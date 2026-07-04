@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bank.salestracker.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bank.salestracker.data.model.MyReport
import com.bank.salestracker.data.model.Sale
import com.bank.salestracker.di.ServiceLocator
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

    /** Текстовый отчёт — как раньше выгружали из телеграм-бота, можно отправить в чат. */
    fun buildShareText(): String {
        val r = report ?: return ""
        val user = ServiceLocator.authRepo.currentUser()
        return buildString {
            appendLine("📊 Отчёт по продажам — ${LocalDate.now()}")
            appendLine("Сотрудник: ${user?.fullName}")
            appendLine("Сегодня: ${r.todayCount} шт. на ${"%,.0f".format(r.todayAmount)} ₽")
            appendLine("За месяц: ${r.monthCount} шт. на ${"%,.0f".format(r.monthAmount)} ₽")
            appendLine()
            appendLine("По продуктам (месяц):")
            r.byCategory.sortedByDescending { it.count }.forEach {
                appendLine("• ${it.category.title}: ${it.count}")
            }
        }
    }
}

@Composable
fun ReportsScreen(vm: ReportsVm = viewModel()) {
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои отчёты") },
                actions = {
                    IconButton(onClick = {
                        val text = vm.buildShareText()
                        if (text.isNotBlank()) ctx.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                                }, "Отправить отчёт"
                            )
                        )
                    }) { Icon(Icons.Default.Share, "Поделиться отчётом") }
                }
            )
        }
    ) { pad ->
        if (vm.loading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            Modifier.padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            vm.report?.let { r ->
                item {
                    Text("По продуктам за месяц", style = MaterialTheme.typography.titleMedium)
                }
                items(r.byCategory.sortedByDescending { it.count }) { c ->
                    ListItem(
                        headlineContent = { Text(c.category.title) },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${c.count} шт.", style = MaterialTheme.typography.titleSmall)
                                if (c.totalAmount > 0)
                                    Text("%,.0f ₽".format(c.totalAmount), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Продажи за сегодня", style = MaterialTheme.typography.titleMedium)
            }
            if (vm.todaySales.isEmpty()) {
                item { Text("Сегодня продаж пока нет", color = MaterialTheme.colorScheme.outline) }
            }
            items(vm.todaySales, key = { it.id ?: it.hashCode() }) { s ->
                Card {
                    ListItem(
                        headlineContent = { Text(s.category.title) },
                        supportingContent = {
                            Column {
                                s.amount?.let { Text("%,.0f ₽".format(it)) }
                                s.comment?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { vm.delete(s) }) {
                                Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }
        }
    }
}
