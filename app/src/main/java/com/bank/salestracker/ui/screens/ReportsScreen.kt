@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bank.salestracker.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bank.salestracker.data.model.MyReport
import com.bank.salestracker.data.model.Sale
import com.bank.salestracker.data.model.displayTitle
import com.bank.salestracker.di.ServiceLocator
import com.bank.salestracker.ui.theme.AppBackground
import com.bank.salestracker.ui.theme.GlassSurface
import com.bank.salestracker.ui.theme.GlassTopBar
import com.bank.salestracker.ui.theme.MetricGlassCard
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReportsVm : ViewModel() {
    var report by mutableStateOf<MyReport?>(null)
    var todaySales by mutableStateOf<List<Sale>>(emptyList())
    var loading by mutableStateOf(true)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        loading = true
        runCatching {
            report = ServiceLocator.salesRepo.myReport()
            val today = LocalDate.now().toString()
            todaySales = ServiceLocator.salesRepo.mySales(from = today, to = today)
        }
        loading = false
    }

    fun delete(sale: Sale) = viewModelScope.launch {
        sale.id?.let {
            runCatching { ServiceLocator.salesRepo.deleteSale(it) }
            refresh()
        }
    }

    fun buildShareText(): String {
        val r = report ?: return ""
        val user = ServiceLocator.authRepo.currentUser()
        return buildString {
            appendLine("Отчет по продажам - ${LocalDate.now()}")
            appendLine("Сотрудник: ${user?.fullName}")
            appendLine("Сегодня: ${r.todayCount} шт. на ${"%,.0f".format(r.todayAmount)} ₽ · ${formatReportPoints(r.todayPoints)} б.")
            appendLine("За месяц: ${r.monthCount} шт. на ${"%,.0f".format(r.monthAmount)} ₽ · ${formatReportPoints(r.monthPoints)} б.")
            appendLine()
            appendLine("По продуктам за месяц:")
            r.byCategory.sortedByDescending { it.count }.forEach {
                appendLine("- ${it.displayTitle()}: ${it.count} · ${formatReportPoints(it.totalPoints)} б.")
            }
        }
    }
}

@Composable
fun ReportsScreen(vm: ReportsVm = viewModel()) {
    val ctx = LocalContext.current

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    GlassTopBar(
                        title = "Отчеты",
                        subtitle = "Сегодня, месяц и продукты",
                        trailing = {
                            IconButton(onClick = {
                                val text = vm.buildShareText()
                                if (text.isNotBlank()) {
                                    ctx.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, text)
                                            },
                                            "Отправить отчет"
                                        )
                                    )
                                }
                            }) {
                                Icon(Icons.Default.Share, "Поделиться отчетом")
                            }
                        }
                    )
                }
            }
        ) { pad ->
            if (vm.loading) {
                Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            LazyColumn(
                Modifier.padding(pad).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                vm.report?.let { r ->
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricGlassCard("${r.todayCount}", "сегодня", Modifier.weight(1f), "${formatReportPoints(r.todayPoints)} б.")
                            MetricGlassCard("${r.monthCount}", "месяц", Modifier.weight(1f), "${formatReportPoints(r.monthPoints)} б.")
                            MetricGlassCard("%,.0f ₽".format(r.monthAmount), "сумма", Modifier.weight(1f))
                        }
                    }

                    item {
                        GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("По продуктам за месяц", style = MaterialTheme.typography.titleMedium)
                                r.byCategory.sortedByDescending { it.count }.forEachIndexed { index, category ->
                                    ReportProductRow(
                                        title = category.displayTitle(),
                                        count = "${category.count} шт.",
                                        points = "${formatReportPoints(category.totalPoints)} б.",
                                        amount = category.totalAmount.takeIf { it > 0 }?.let { "%,.0f ₽".format(it) }
                                    )
                                    if (index != r.byCategory.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Продажи за сегодня", style = MaterialTheme.typography.titleMedium)
                            if (vm.todaySales.isEmpty()) {
                                Text("Сегодня продаж пока нет", color = MaterialTheme.colorScheme.outline)
                            } else {
                                vm.todaySales.forEachIndexed { index, sale ->
                                    TodaySaleRow(sale = sale, onDelete = { vm.delete(sale) })
                                    if (index != vm.todaySales.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.padding(bottom = 8.dp)) }
            }
        }
    }
}

@Composable
private fun ReportProductRow(title: String, count: String, points: String, amount: String?) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            amount?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(count, style = MaterialTheme.typography.titleSmall)
            Text(points, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TodaySaleRow(sale: Sale, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(sale.displayTitle(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            sale.amount?.let { Text("%,.0f ₽".format(it), style = MaterialTheme.typography.bodySmall) }
            sale.comment?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
        }
    }
}

private fun formatReportPoints(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
