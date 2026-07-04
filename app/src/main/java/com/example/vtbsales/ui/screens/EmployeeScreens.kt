package com.example.vtbsales.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vtbsales.model.AppScreen
import com.example.vtbsales.model.ProductType
import com.example.vtbsales.ui.VtbAppState
import com.example.vtbsales.ui.components.AppCard
import com.example.vtbsales.ui.components.BottomNav
import com.example.vtbsales.ui.components.GradientHeroCard
import com.example.vtbsales.ui.components.MetricCard
import com.example.vtbsales.ui.components.Pill
import com.example.vtbsales.ui.components.PrimaryButton
import com.example.vtbsales.ui.components.ProductChip
import com.example.vtbsales.ui.components.ProgressLine
import com.example.vtbsales.ui.components.ReportSummaryCard
import com.example.vtbsales.ui.components.SaleRow
import com.example.vtbsales.ui.components.ScreenFrame
import com.example.vtbsales.ui.components.SecondaryButton
import com.example.vtbsales.ui.components.VtbLogoMark
import com.example.vtbsales.ui.components.VtbTextField
import com.example.vtbsales.ui.components.format1
import com.example.vtbsales.ui.components.format2
import com.example.vtbsales.ui.theme.VtbAmber
import com.example.vtbsales.ui.theme.VtbBlue
import com.example.vtbsales.ui.theme.VtbCyan
import com.example.vtbsales.ui.theme.VtbMuted
import com.example.vtbsales.ui.theme.VtbSurface
import com.example.vtbsales.ui.theme.VtbText

private val employeeNav = listOf(
    AppScreen.EmployeeHome to "Главная",
    AppScreen.AddSale to "Продажа",
    AppScreen.EmployeeReports to "Отчеты",
    AppScreen.EmployeeStats to "Детали",
    AppScreen.EmployeeProfile to "Профиль"
)

@Composable
private fun EmployeeScaffold(state: VtbAppState, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        containerColor = VtbSurface,
        bottomBar = {
            BottomNav(employeeNav, state.screen) { state.go(it) }
        },
        content = content
    )
}

@Composable
fun EmployeeHomeScreen(state: VtbAppState) {
    val user = state.currentUser
    val day = state.currentDailyReport()
    val month = state.currentMonthReport()
    EmployeeScaffold(state) { padding ->
        ScreenFrame(
            title = "Главная",
            subtitle = user?.name ?: "Сотрудник ВТБ",
            trailing = { VtbLogoMark() }
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GradientHeroCard(
                    title = "Баллы сегодня",
                    value = day?.points?.format1() ?: "0",
                    subtitle = "${day?.products ?: 0} продуктов · ${day?.clients ?: 0} клиентов",
                    badge = "ВТБ"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Месяц", month?.points?.format1() ?: "0", "баллов", Modifier.weight(1f))
                    MetricCard("Клиенты", month?.clients?.toString() ?: "0", "с начала месяца", Modifier.weight(1f))
                }
                AppCard {
                    Text("Напоминание", style = MaterialTheme.typography.titleMedium, color = VtbText)
                    Text("Внесите продажи до 18:00 — отчет соберется автоматически.", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                }
                AppCard {
                    Text("Прогресс месяца", style = MaterialTheme.typography.titleMedium, color = VtbText)
                    Spacer(Modifier.height(12.dp))
                    ProgressLine("Баллы", month?.points ?: 0.0, 7800.0, VtbBlue)
                    Spacer(Modifier.height(10.dp))
                    ProgressLine("Клиенты", (month?.clients ?: 0).toDouble(), 52.0, VtbCyan)
                }
                AppCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Рейтинг отдела", style = MaterialTheme.typography.titleMedium, color = VtbText)
                        Pill("#2 из 10")
                    }
                    Text("Выше среднего темпа на 18%", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                }
                PrimaryButton("Внести продажу") { state.go(AppScreen.AddSale) }
            }
        }
    }
}

@Composable
fun AddSaleScreen(state: VtbAppState) {
    EmployeeScaffold(state) { padding ->
        ScreenFrame(title = "Продажа", subtitle = "4 цифры клиента и продукты") {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppCard {
                    Text("Клиент", style = MaterialTheme.typography.titleMedium, color = VtbText)
                    Text(
                        "Введите последние 4 цифры телефона. Если цифры совпали, можно выбрать существующую карточку или создать новую.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VtbMuted
                    )
                    Spacer(Modifier.height(12.dp))
                    VtbTextField(state.clientLast4, "Последние 4 цифры") { value ->
                        state.clientLast4 = value.filter { it.isDigit() }.take(4)
                        state.activeClientSessionId = null
                    }
                }

                val clientMatches = state.matchingClientSessions()
                if (clientMatches.isNotEmpty()) {
                    AppCard {
                        Text("Такие цифры уже есть сегодня", style = MaterialTheme.typography.titleMedium, color = VtbText)
                        Text("Это тот же клиент или новый человек с такими же цифрами?", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                        Spacer(Modifier.height(10.dp))
                        clientMatches.forEach { session ->
                            SecondaryButton(
                                if (session.sequence == 1) {
                                    "Выбрать клиента ${session.phoneLast4}"
                                } else {
                                    "Выбрать клиента ${session.phoneLast4} #${session.sequence}"
                                }
                            ) { state.selectClientSession(session.id) }
                            Spacer(Modifier.height(8.dp))
                        }
                        SecondaryButton("Создать нового клиента ${state.clientLast4} #${clientMatches.size + 1}") {
                            state.createNewClientCard()
                        }
                    }
                }

                if (state.activeClientSessionId != null) {
                    AppCard {
                        Text("Текущая карточка", style = MaterialTheme.typography.titleMedium, color = VtbText)
                        Text("Клиент ${state.activeClientLabel()}", style = MaterialTheme.typography.bodyMedium, color = VtbBlue)
                        val lines = state.activeClientSales()
                        if (lines.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            lines.forEach { sale ->
                                SaleRow(sale.productType.title, sale.format, "${sale.count} шт · ${sale.points.format1()} б.")
                                SecondaryButton("Редактировать") {
                                    state.startEditingSale(sale.id)
                                }
                            }
                        }
                    }
                }

                ProductType.values().toList().chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { product ->
                            Column(Modifier.weight(1f)) {
                                ProductChip(
                                    product = product,
                                    selected = state.selectedProduct == product,
                                    onClick = { state.selectProduct(product) }
                                )
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                AppCard {
                    VtbTextField(state.saleFormat, "Формат") { state.saleFormat = it }
                    Spacer(Modifier.height(10.dp))
                    VtbTextField(state.saleCount, "Количество") { state.saleCount = it }
                    Spacer(Modifier.height(10.dp))
                    VtbTextField(state.saleAmount, "Сумма") { state.saleAmount = it }
                    Spacer(Modifier.height(10.dp))
                    VtbTextField(state.salePoints, "Баллы") { state.salePoints = it }
                    if (state.editingSaleId != null) {
                        Spacer(Modifier.height(10.dp))
                        VtbTextField(state.editReason, "Причина правки") { state.editReason = it }
                    }
                }
                AppCard {
                    Text("Проверка записи", style = MaterialTheme.typography.titleMedium, color = VtbText)
                    Text("Клиент считается один раз, даже если внутри карточки несколько продуктов.", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                }
                PrimaryButton(if (state.editingSaleId == null) "Добавить продукт клиенту" else "Сохранить правку") {
                    if (state.editingSaleId == null) state.saveSale() else state.saveEditedSale()
                }
                state.toastMessage?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge, color = VtbBlue)
                }
            }
        }
    }
}

@Composable
fun EmployeeReportsScreen(state: VtbAppState) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val day = state.currentDailyReport()
    val month = state.currentMonthReport()
    EmployeeScaffold(state) { padding ->
        ScreenFrame(title = "Отчеты", subtitle = "Сегодня и с начала месяца") {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (day != null) ReportSummaryCard(day, "Отчет за сегодня")
                if (month != null) ReportSummaryCard(month, "Итог с начала месяца")
                PrimaryButton("Скопировать отчет") {
                    clipboard.setText(AnnotatedString(state.reportCopyText()))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Excel", Modifier.weight(1f)) { state.exportCurrentEmployeeExcel(context) }
                    SecondaryButton("PDF", Modifier.weight(1f)) { state.exportCurrentEmployeePdf(context) }
                }
                state.exportMessage?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge, color = VtbBlue)
                }
            }
        }
    }
}

@Composable
fun EmployeeStatsScreen(state: VtbAppState) {
    val detail = state.currentEmployeeDetail()
    val month = detail?.monthReport
    EmployeeScaffold(state) { padding ->
        ScreenFrame(title = "Детали", subtitle = "Клиенты, продукты и правки") {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (detail != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard("Сегодня", detail.dailyReport.points.format1(), "баллов", Modifier.weight(1f))
                        MetricCard("Клиенты", detail.dailyReport.clients.toString(), "за день", Modifier.weight(1f))
                    }
                    AppCard {
                        Text("Прогресс месяца", style = MaterialTheme.typography.titleMedium, color = VtbText)
                        Spacer(Modifier.height(12.dp))
                        ProgressLine("Баллы", month?.points ?: 0.0, 7800.0, VtbBlue)
                        Spacer(Modifier.height(8.dp))
                        ProgressLine("Клиенты", (month?.clients ?: 0).toDouble(), 52.0, VtbCyan)
                    }
                    AppCard {
                        Text("Клиентские карточки сегодня", style = MaterialTheme.typography.titleMedium, color = VtbText)
                        Spacer(Modifier.height(8.dp))
                        if (detail.clientCards.isEmpty()) {
                            Text("Сегодня пока нет карточек с последними 4 цифрами.", color = VtbMuted)
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
                            detail.editHistory.take(5).forEach { item ->
                                SaleRow(item.reason, "${item.before.count} шт → ${item.after.count} шт", "${item.after.points.format1()} б.")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeRankingScreen(state: VtbAppState) {
    val manager = state.repository.users().firstOrNull { it.role.name == "Manager" }
    val team = manager?.let { state.repository.teamSummaries(it.id) }.orEmpty()
    EmployeeScaffold(state) { padding ->
        ScreenFrame(title = "Рейтинг", subtitle = "Доп. офис №8617/0290") {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                team.forEach { summary ->
                    AppCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(summary.user.name, style = MaterialTheme.typography.titleMedium, color = VtbText)
                                Text("сегодня +${summary.dailyReport.points.format1()}", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                            }
                            Pill("#${summary.rank}", color = if (summary.rank == 1) VtbAmber.copy(alpha = 0.18f) else VtbBlue.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeProfileScreen(state: VtbAppState) {
    val user = state.currentUser
    EmployeeScaffold(state) { padding ->
        ScreenFrame(title = "Профиль", subtitle = "Настройки и UID", trailing = { VtbLogoMark() }) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppCard {
                    Text(user?.name ?: "Сотрудник ВТБ", style = MaterialTheme.typography.titleLarge, color = VtbText)
                    Text(user?.office ?: "Офис ВТБ", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                    Spacer(Modifier.height(12.dp))
                    Text("Ваш UID", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                    Text(user?.uid.orEmpty(), style = MaterialTheme.typography.headlineMedium, color = VtbBlue)
                }
                AppCard {
                    Text("Привязка к офису", style = MaterialTheme.typography.titleMedium, color = VtbText)
                    Text("Введите UID офиса от руководителя", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
                    Spacer(Modifier.height(12.dp))
                    VtbTextField(state.officeUidInput, "UID офиса") { state.officeUidInput = it }
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton("Сохранить офис") { state.bindCurrentEmployeeToOffice() }
                    state.officeBindStatus?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = VtbBlue)
                    }
                }
                AppCard {
                    SaleRow("Напоминать о продажах", "каждый день в 18:00", "Вкл")
                    SaleRow("Итоги дня от руководителя", "место в рейтинге", "Вкл")
                    SaleRow("Язык", "интерфейс приложения", "Русский")
                }
                SecondaryButton("Выйти") { state.screen = AppScreen.Welcome }
            }
        }
    }
}
