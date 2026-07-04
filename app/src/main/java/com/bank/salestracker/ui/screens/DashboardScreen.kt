@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bank.salestracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bank.salestracker.data.model.MyReport
import com.bank.salestracker.di.ServiceLocator
import com.bank.salestracker.ui.navigation.SalesRefreshSignal
import com.bank.salestracker.ui.theme.AppBackground
import com.bank.salestracker.ui.theme.GlassSurface
import com.bank.salestracker.ui.theme.GlassTopBar
import com.bank.salestracker.ui.theme.MetricGlassCard
import com.bank.salestracker.ui.theme.Ocean
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DashboardVm : ViewModel() {
    var report by mutableStateOf<MyReport?>(null)
    var loading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)

    val pendingCount = ServiceLocator.salesRepo.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        loading = true; error = null
        try { report = ServiceLocator.salesRepo.myReport() }
        catch (e: Exception) { error = "Не удалось загрузить данные" }
        finally { loading = false }
    }
}

private val money: NumberFormat = NumberFormat.getCurrencyInstance(Locale("ru", "RU"))

@Composable
fun DashboardScreen(
    onAddSale: () -> Unit,
    onLogout: () -> Unit,
    canCreateSales: Boolean = true,
    refreshSignal: Long = 0L,
    vm: DashboardVm = viewModel()
) {
    val user = ServiceLocator.authRepo.currentUser()
    val pending by vm.pendingCount.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshSignal) {
        if (SalesRefreshSignal.shouldRefresh(refreshSignal)) vm.refresh()
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    GlassTopBar(
                        title = user?.fullName ?: "",
                        subtitle = user?.branch ?: "",
                        trailing = {
                            IconButton(onClick = {
                                scope.launch { ServiceLocator.authRepo.logout(); onLogout() }
                            }) { Icon(Icons.AutoMirrored.Filled.Logout, "Выйти") }
                        }
                    )
                }
            },
            floatingActionButton = {
                if (canCreateSales) {
                    ExtendedFloatingActionButton(
                        onClick = onAddSale,
                        containerColor = Ocean,
                        contentColor = Color.White,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Продажа")
                    }
                }
            }
        ) { pad ->
            Column(
                Modifier.padding(pad).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (pending > 0) {
                    GlassSurface(contentPadding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("$pending продаж(и) ждут отправки — нет связи с сервером")
                        }
                    }
                }

                when {
                    vm.loading -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    vm.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(vm.error!!)
                        TextButton(onClick = vm::refresh) { Text("Повторить") }
                    }
                    else -> vm.report?.let { r ->
                        GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                            Column {
                                Text("СЕГОДНЯ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${r.todayCount}", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "продаж на ${money.format(r.todayAmount)} · ${formatPoints(r.todayPoints)} б.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MetricGlassCard("${r.monthCount}", "за месяц", Modifier.weight(1f))
                                    MetricGlassCard(money.format(r.monthAmount), "сумма", Modifier.weight(1f))
                                    MetricGlassCard(formatPoints(r.monthPoints), "баллы", Modifier.weight(1f), accent = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }

                        r.monthlyGoal?.let { goal ->
                            val progress = (r.monthCount.toFloat() / goal).coerceIn(0f, 1f)
                            GlassSurface(contentPadding = PaddingValues(16.dp)) {
                                Column {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("План на месяц", style = MaterialTheme.typography.titleSmall)
                                        Text("${r.monthCount} / $goal")
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(10.dp),
                                        strokeCap = StrokeCap.Round
                                    )
                                    if (progress >= 1f) {
                                        Spacer(Modifier.height(4.dp))
                                        Text("План выполнен", color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }

                        GlassSurface(contentPadding = PaddingValues(16.dp)) {
                            Column {
                                Text("Динамика за 14 дней", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(12.dp))
                                MiniBarChart(r.last14Days.map { it.count })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatPoints(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

@Composable
fun MiniBarChart(values: List<Int>, modifier: Modifier = Modifier, barColor: Color = MaterialTheme.colorScheme.primary) {
    if (values.isEmpty()) { Text("Нет данных"); return }
    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    val trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
    Canvas(modifier.fillMaxWidth().height(100.dp)) {
        val gap = 6f
        val barW = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val h = size.height * (v.toFloat() / max)
            val x = i * (barW + gap) + barW / 2
            drawLine(
                color = trackColor,
                start = Offset(x, size.height),
                end = Offset(x, 0f),
                strokeWidth = barW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = barColor,
                start = Offset(x, size.height),
                end = Offset(x, size.height - h.coerceAtLeast(4f)),
                strokeWidth = barW,
                cap = StrokeCap.Round
            )
        }
    }
}
