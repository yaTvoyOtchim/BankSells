package com.example.vtbsales.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vtbsales.model.AppScreen
import com.example.vtbsales.ui.VtbAppState
import com.example.vtbsales.ui.components.AppCard
import com.example.vtbsales.ui.components.BottomNav
import com.example.vtbsales.ui.components.EmployeeRow
import com.example.vtbsales.ui.components.GradientHeroCard
import com.example.vtbsales.ui.components.MetricCard
import com.example.vtbsales.ui.components.Pill
import com.example.vtbsales.ui.components.PrimaryButton
import com.example.vtbsales.ui.components.ProgressLine
import com.example.vtbsales.ui.components.ReportSummaryCard
import com.example.vtbsales.ui.components.SaleRow
import com.example.vtbsales.ui.components.ScreenFrame
import com.example.vtbsales.ui.components.SecondaryButton
import com.example.vtbsales.ui.components.VtbLogoMark
import com.example.vtbsales.ui.components.VtbTextField
import com.example.vtbsales.ui.components.format1
import com.example.vtbsales.ui.theme.VtbAmber
import com.example.vtbsales.ui.theme.VtbBlue
import com.example.vtbsales.ui.theme.VtbCyan
import com.example.vtbsales.ui.theme.VtbDanger
import com.example.vtbsales.ui.theme.VtbMuted
import com.example.vtbsales.ui.theme.VtbSurface
import com.example.vtbsales.ui.theme.VtbText

private val managerNav = listOf(
    AppScreen.ManagerHome to "Сводка",
    AppScreen.ManagerTeam to "Команда",
    AppScreen.ManagerPlans to "Планы",
    AppScreen.ManagerProfile to "Профиль"
)

@Composable
private fun ManagerScaffold(state: VtbAppState, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        containerColor = VtbSurface,
        bottomBar = {
            BottomNav(managerNav, state.screen) { state.go(it) }
        },
        content = content
    )
}

@Composable
fun ManagerDashboardScreen(state: VtbAppState) {
    val user = state.currentUser
    val team = state.managerTeam()
    ManagerScaffold(state) { padding ->
        ScreenFrame(
            title = "Руководитель",
            subtitle = user?.office ?: "Доп. офис №8617/0290",
            trailing = { VtbLogoMark("АД") }
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GradientHeroCard(
                    title = "Команда сегодня",
                    value = state.managerDailyPoints().format1(),
                    subtitle = "${state.activeEmployeesToday()} активны · ${team.size} сотрудников",
                    badge = "91% плана"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Месяц", state.managerMonthPoints().format1(), "баллов", Modifier.weight(1f))
                    MetricCard("Внимание", state.attentionEmployees().size.toString(), "сотрудников", Modifier.weight(1f))
                }
                AppCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Лидеры месяца", style = MaterialTheme.typography.titleMedium, color = VtbText)
                        Pill("Все", color = VtbBlue.copy(alpha = 0.12f))
                    }
                    Spacer(Modifier.height(8.dp))
                    team.take(3).forEach { EmployeeRow(it) }
                }
                AppCard {
                    Text("Требуют внимания", style = MaterialTheme.typography.titleMedium, color = VtbText)
                    Spacer(Modifier.height(8.dp))
                    val attention = state.attentionEmployees()
                    if (attention.isEmpty()) {
                        Text("Все сотрудники активны сегодня.", color = VtbMuted)
                    } else {
                        attention.forEach {
                            SaleRow(it.user.name, "нет продаж или низкий темп", "${it.dailyReport.points.format1()} б.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManagerDashboardLiveScreen(state: VtbAppState) {
    val user = state.currentUser
    val team = state.managerTeam()

    ManagerScaffold(state) { padding ->
        ScreenFrame(
            title = "Руководитель",
            subtitle = user?.office ?: "Доп. офис №8617/0290",
            trailing = { VtbLogoMark("АД") }
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GradientHeroCard(
                    title = "Команда сегодня",
                    value = state.managerDailyPoints().format1(),
                    subtitle = "${state.activeEmployeesToday()} активны · ${team.size} сотрудников",
                    badge = "${state.managerPlanPercent()}% плана"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Месяц", state.managerMonthPoints().format1(), "баллов", Modifier.weight(1f))
                    MetricCard("Внимание", state.attentionEmployees().size.toString(), "сотрудников", Modifier.weight(1f))
                }
                AppCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Лидеры месяца", style = MaterialTheme.typography.titleMedium, color = VtbText)
                        Pill("Все", color = VtbBlue.copy(alpha = 0.12f))
                    }
                    Spacer(Modifier.height(8.dp))
                    team.take(3).forEach { EmployeeRow(it) }
                }
                AppCard {
                    Text("Требуют внимания", style = MaterialTheme.typography.titleMedium, color = VtbText)
                    Spacer(Modifier.height(8.dp))
                    val attention = state.attentionEmployees()
                    if (attention.isEmpty()) {
                        Text("Все сотрудники активны сегодня.", color = VtbMuted)
                    } else {
                        attention.forEach {
                            SaleRow(it.user.name, "нет продаж или низкий темп", "${it.dailyReport.points.format1()} б.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManagerTeamScreen(state: VtbAppState) {
    val team = state.managerTeam()
    ManagerScaffold(state) { padding ->
        ScreenFrame(title = "Команда", subtitle = "${team.size} сотрудников · поиск и сводки") {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VtbTextField(state.managerQuery, "Поиск сотрудника") { state.managerQuery = it }
                AppCard {
                    if (team.isEmpty()) {
                        Text("Сотрудники не найдены", color = VtbMuted)
                    } else {
                        team.forEach { summary ->
                            EmployeeRow(summary) { state.openEmployeeCard(summary.user.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManagerEmployeeCardScreen(state: VtbAppState) {
    val detail = state.selectedEmployeeDetail()
    val context = LocalContext.current
    ManagerScaffold(state) { padding ->
        ScreenFrame(
            title = detail?.user?.name ?: "Карточка сотрудника",
            subtitle = detail?.user?.office ?: "Продажи и история"
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (detail == null) {
                    AppCard {
                        Text("Сотрудник не выбран", color = VtbMuted)
                        SecondaryButton("Вернуться к команде") { state.go(AppScreen.ManagerTeam) }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard("Сегодня", detail.dailyReport.points.format1(), "баллов", Modifier.weight(1f))
                        MetricCard("Клиенты", detail.dailyReport.clients.toString(), "за день", Modifier.weight(1f))
                    }
                    ReportSummaryCard(detail.dailyReport, "Отчет за сегодня")
                    AppCard {
                        Text("Клиенты сегодня", style = MaterialTheme.typography.titleMedium, color = VtbText)
                        Spacer(Modifier.height(8.dp))
                        if (detail.clientCards.isEmpty()) {
                            Text("Нет карточек по последним 4 цифрам.", color = VtbMuted)
                        } else {
                            detail.clientCards.forEach { card ->
                                Text(
                                    "Клиент ${card.session.phoneLast4}${if (card.session.sequence > 1) " #${card.session.sequence}" else ""}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VtbBlue
                                )
                                card.sales.forEach { sale ->
                                    SaleRow(sale.productType.title, sale.format, "${sale.count} шт · ${sale.points.format1()} б.")
                                }
                            }
                        }
                    }
                    AppCard {
                        Text("История правок", style = MaterialTheme.typography.titleMedium, color = VtbText)
                        Spacer(Modifier.height(8.dp))
                        if (detail.editHistory.isEmpty()) {
                            Text("Правок пока нет.", color = VtbMuted)
                        } else {
                            detail.editHistory.take(8).forEach { item ->
                                SaleRow(item.reason, "${item.before.count} шт → ${item.after.count} шт", "${item.after.points.format1()} б.")
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("Excel", Modifier.weight(1f)) { state.exportSelectedEmployeeExcel(context) }
                        SecondaryButton("PDF", Modifier.weight(1f)) { state.exportSelectedEmployeePdf(context) }
                    }
                    state.exportMessage?.let {
                        Text(it, style = MaterialTheme.typography.labelLarge, color = VtbBlue)
                    }
                    SecondaryButton("Назад к команде") { state.go(AppScreen.ManagerTeam) }
                }
            }
        }
    }
}

@Composable
fun ManagerPlansScreen(state: VtbAppState) {
    val manager = state.currentUser
    val plan = manager?.office?.let { state.repository.planForOffice(it) }
    ManagerScaffold(state) { padding ->
        ScreenFrame(title = "Планы", subtitle = "Цели применяются ко всем сотрудникам") {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppCard {
                    Text("Баллы в месяц", style = MaterialTheme.typography.titleMedium, color = VtbText)
                    Text("на сотрудника", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                    Spacer(Modifier.height(12.dp))
                    Text((plan?.pointsTarget ?: 7800.0).format1(), style = MaterialTheme.typography.headlineMedium, color = VtbBlue)
                    ProgressLine("Средний прогресс команды", state.managerMonthPoints() / state.managerTeam().size.coerceAtLeast(1), plan?.pointsTarget ?: 7800.0, VtbBlue)
                }
                AppCard {
                    Text("Клиенты в месяц", style = MaterialTheme.typography.titleMedium, color = VtbText)
                    Text("на сотрудника", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                    Spacer(Modifier.height(12.dp))
                    Text("${plan?.clientsTarget ?: 52}", style = MaterialTheme.typography.headlineMedium, color = VtbCyan)
                }
                PrimaryButton("Сохранить планы") { }
            }
        }
    }
}

@Composable
fun ManagerPlansEditorScreen(state: VtbAppState) {
    val manager = state.currentUser
    val plan = manager?.office?.let { state.repository.planForOffice(it) }
    val pointsTarget = state.planPointsTarget.replace(',', '.').toDoubleOrNull()
        ?: plan?.pointsTarget
        ?: 7800.0
    val clientsTarget = state.planClientsTarget.toIntOrNull()
        ?: plan?.clientsTarget
        ?: 52

    ManagerScaffold(state) { padding ->
        ScreenFrame(title = "Планы", subtitle = "Цели применяются ко всем сотрудникам") {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppCard {
                    Text("Баллы в месяц", style = MaterialTheme.typography.titleMedium, color = VtbText)
                    Text("на сотрудника", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                    Spacer(Modifier.height(12.dp))
                    VtbTextField(
                        value = state.planPointsTarget,
                        label = "Цель по баллам"
                    ) { value ->
                        state.planPointsTarget = value.filter { it.isDigit() || it == '.' || it == ',' }
                    }
                    Spacer(Modifier.height(12.dp))
                    ProgressLine(
                        "Средний прогресс команды",
                        state.managerMonthPoints() / state.managerTeam().size.coerceAtLeast(1),
                        pointsTarget,
                        VtbBlue
                    )
                }

                AppCard {
                    Text("Клиенты в месяц", style = MaterialTheme.typography.titleMedium, color = VtbText)
                    Text("на сотрудника", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                    Spacer(Modifier.height(12.dp))
                    VtbTextField(
                        value = state.planClientsTarget,
                        label = "Цель по клиентам"
                    ) { value ->
                        state.planClientsTarget = value.filter { it.isDigit() }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Текущая цель: $clientsTarget клиентов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VtbCyan
                    )
                }

                state.planStatus?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = VtbBlue)
                }
                PrimaryButton("Сохранить планы") { state.saveManagerPlan() }
            }
        }
    }
}

@Composable
fun ManagerProfileScreen(state: VtbAppState) {
    val user = state.currentUser
    ManagerScaffold(state) { padding ->
        ScreenFrame(title = "Профиль", subtitle = "Настройки руководителя", trailing = { VtbLogoMark("АД") }) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppCard {
                    Text(user?.name ?: "Руководитель ВТБ", style = MaterialTheme.typography.titleLarge, color = VtbText)
                    Text("Руководитель · ${state.managerTeam().size} сотрудников", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                }
                AppCard {
                    Text("Как добавить сотрудника", style = MaterialTheme.typography.titleMedium, color = VtbText)
                    Text(
                        "Попросите сотрудника зарегистрироваться и прислать UID. После привязки он появится в разделе «Команда».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VtbMuted
                    )
                }
                AppCard {
                    SaleRow("Сводка по отделу", "каждый день в 18:30", "Вкл")
                    SaleRow("Ошибки и пустые дни", "показывать в зоне внимания", "Вкл")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Статус", color = VtbMuted)
                        Text("Демо MVP", color = VtbBlue, fontWeight = FontWeight.ExtraBold)
                    }
                }
                SecondaryButton("Выйти") { state.screen = AppScreen.Welcome }
            }
        }
    }
}
