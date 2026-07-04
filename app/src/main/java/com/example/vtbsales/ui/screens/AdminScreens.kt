package com.example.vtbsales.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vtbsales.model.AppScreen
import com.example.vtbsales.ui.VtbAppState
import com.example.vtbsales.ui.components.AppCard
import com.example.vtbsales.ui.components.PrimaryButton
import com.example.vtbsales.ui.components.ProgressLine
import com.example.vtbsales.ui.components.ScreenFrame
import com.example.vtbsales.ui.components.SecondaryButton
import com.example.vtbsales.ui.components.VtbLogoMark
import com.example.vtbsales.ui.components.format1
import com.example.vtbsales.ui.theme.VtbBlue
import com.example.vtbsales.ui.theme.VtbCyan
import com.example.vtbsales.ui.theme.VtbMuted
import com.example.vtbsales.ui.theme.VtbSurface
import com.example.vtbsales.ui.theme.VtbText

@Composable
fun AdminHomeScreen(state: VtbAppState) {
    val summaries = state.adminOfficeSummaries()
    val context = LocalContext.current
    Scaffold(containerColor = VtbSurface) { padding ->
        ScreenFrame(
            title = "Админ",
            subtitle = "Все офисы и отделения",
            trailing = { VtbLogoMark("АД") }
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminMetric("Офисы", summaries.size.toString(), "в сети", Modifier.weight(1f))
                    AdminMetric("Сотрудники", summaries.sumOf { it.employees }.toString(), "активные", Modifier.weight(1f))
                }
                summaries.forEach { summary ->
                    AppCard {
                        Text(summary.office.title, style = MaterialTheme.typography.titleMedium, color = VtbText)
                        Text("${summary.office.city} · ${summary.office.region}", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminMetric("Сегодня", summary.dailyPoints.format1(), "баллов", Modifier.weight(1f))
                            AdminMetric("Внимание", summary.attention.toString(), "сотрудников", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        ProgressLine("Месяц", summary.monthPoints, (summary.employees.coerceAtLeast(1) * 7800).toDouble(), VtbCyan)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Excel", Modifier.weight(1f)) { state.exportOfficeExcel(context) }
                    SecondaryButton("PDF", Modifier.weight(1f)) { state.exportOfficePdf(context) }
                }
                PrimaryButton("Обновить сводку") { state.exportMessage = "Сводка обновлена локально" }
                state.exportMessage?.let { Text(it, color = VtbBlue) }
                SecondaryButton("Выйти") { state.screen = AppScreen.Welcome }
            }
        }
    }
}

@Composable
private fun AdminMetric(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = VtbText)
        Text(caption, style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
    }
}
