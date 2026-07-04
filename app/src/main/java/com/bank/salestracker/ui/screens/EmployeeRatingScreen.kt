package com.bank.salestracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bank.salestracker.data.model.MyReport
import com.bank.salestracker.di.ServiceLocator
import com.bank.salestracker.ui.theme.AppBackground
import com.bank.salestracker.ui.theme.GlassSurface
import com.bank.salestracker.ui.theme.GlassTopBar
import com.bank.salestracker.ui.theme.MetricGlassCard
import kotlinx.coroutines.launch

class EmployeeRatingVm : ViewModel() {
    var report by mutableStateOf<MyReport?>(null)
    var loading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        loading = true
        error = null
        try {
            report = ServiceLocator.salesRepo.myReport()
        } catch (e: Exception) {
            error = "Не удалось загрузить рейтинг"
        } finally {
            loading = false
        }
    }
}

@Composable
fun EmployeeRatingScreen(vm: EmployeeRatingVm = viewModel()) {
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    GlassTopBar(title = "Рейтинг", subtitle = "Личный прогресс и место в команде")
                }
            }
        ) { pad ->
            Column(
                modifier = Modifier
                    .padding(pad)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    vm.loading -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    vm.error != null -> Text(vm.error!!, color = MaterialTheme.colorScheme.error)
                    else -> vm.report?.let { report ->
                        GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.padding(start = 12.dp)) {
                                    Text("Ваш результат", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Командный рейтинг доступен руководителю, здесь виден личный темп.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricGlassCard("${report.todayCount}", "сегодня", Modifier.weight(1f))
                            MetricGlassCard(formatRatingPoints(report.todayPoints), "баллы", Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricGlassCard("${report.monthCount}", "за месяц", Modifier.weight(1f))
                            MetricGlassCard(formatRatingPoints(report.monthPoints), "баллы месяца", Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private fun formatRatingPoints(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
