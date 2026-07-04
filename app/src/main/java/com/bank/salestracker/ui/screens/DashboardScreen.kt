@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bank.salestracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
fun DashboardScreen(onAddSale: () -> Unit, onLogout: () -> Unit, vm: DashboardVm = viewModel()) {
    val user = ServiceLocator.authRepo.currentUser()
    val pending by vm.pendingCount.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(user?.fullName ?: "", style = MaterialTheme.typography.titleMedium)
                        Text(user?.branch ?: "", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch { ServiceLocator.authRepo.logout(); onLogout() }
                    }) { Icon(Icons.AutoMirrored.Filled.Logout, "Выйти") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddSale) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Продажа")
            }
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (pending > 0) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudOff, null)
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
                    // Hero-карточка с градиентом: главное число дня
                    Card(shape = MaterialTheme.shapes.large) {
                        Column(
                            Modifier.background(com.bank.salestracker.ui.theme.HeroGradient)
                                .fillMaxWidth().padding(20.dp)
                        ) {
                            Text("СЕГОДНЯ", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .8f))
                            Text("${r.todayCount}", style = MaterialTheme.typography.displayMedium, color = Color.White)
                            Text("продаж на ${money.format(r.todayAmount)}", color = Color.White.copy(alpha = .9f))
                            Spacer(Modifier.height(16.dp))
                            Row {
                                Column(Modifier.weight(1f)) {
                                    Text("ЗА МЕСЯЦ", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .8f))
                                    Text("${r.monthCount}", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("СУММА", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .8f))
                                    Text(money.format(r.monthAmount), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                                }
                            }
                        }
                    }

                    r.monthlyGoal?.let { goal ->
                        val progress = (r.monthCount.toFloat() / goal).coerceIn(0f, 1f)
                        Card {
                            Column(Modifier.padding(16.dp)) {
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
                                    Text("План выполнен 🎉", color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }

                    Card {
                        Column(Modifier.padding(16.dp)) {
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

@Composable
fun MiniBarChart(values: List<Int>, modifier: Modifier = Modifier, barColor: Color = MaterialTheme.colorScheme.primary) {
    if (values.isEmpty()) { Text("Нет данных"); return }
    val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
    Canvas(modifier.fillMaxWidth().height(100.dp)) {
        val gap = 6f
        val barW = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val h = size.height * (v.toFloat() / max)
            drawLine(
                color = barColor,
                start = Offset(i * (barW + gap) + barW / 2, size.height),
                end = Offset(i * (barW + gap) + barW / 2, size.height - h.coerceAtLeast(2f)),
                strokeWidth = barW,
                cap = StrokeCap.Round
            )
        }
    }
}
