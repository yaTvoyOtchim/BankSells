package com.example.vtbsales.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.content.Context
import com.example.vtbsales.data.ExportFileStore
import com.example.vtbsales.data.ReportExporter
import com.example.vtbsales.data.SalesRepository
import com.example.vtbsales.model.AppScreen
import com.example.vtbsales.model.ClientSession
import com.example.vtbsales.model.EmployeeDetail
import com.example.vtbsales.model.OfficeSummary
import com.example.vtbsales.model.ProductType
import com.example.vtbsales.model.ReportSummary
import com.example.vtbsales.model.Role
import com.example.vtbsales.model.Sale
import com.example.vtbsales.model.SaleEditHistory
import com.example.vtbsales.model.TeamMemberSummary
import com.example.vtbsales.model.User

class VtbAppState(
    val repository: SalesRepository
) {
    var screen by mutableStateOf(AppScreen.Welcome)
    var currentUser by mutableStateOf<User?>(null)
    var lastCreatedUid by mutableStateOf<String?>(null)
    var selectedProduct by mutableStateOf(ProductType.CreditCard)
    var saleFormat by mutableStateOf("КК по заявке")
    var saleCount by mutableStateOf("1")
    var saleAmount by mutableStateOf("0")
    var salePoints by mutableStateOf("30")
    var saleClients by mutableStateOf("0")
    var clientLast4 by mutableStateOf("")
    var activeClientSessionId by mutableStateOf<String?>(null)
    var editingSaleId by mutableStateOf<String?>(null)
    var editReason by mutableStateOf("")
    var selectedEmployeeId by mutableStateOf<String?>(null)
    var managerQuery by mutableStateOf("")
    var planPointsTarget by mutableStateOf("7800")
    var planClientsTarget by mutableStateOf("52")
    var planStatus by mutableStateOf<String?>(null)
    var exportMessage by mutableStateOf<String?>(null)
    var toastMessage by mutableStateOf<String?>(null)

    fun go(target: AppScreen) {
        if (isManagerScreen(target) && currentUser?.role != Role.Manager) return
        if (isAdminScreen(target) && currentUser?.role != Role.Admin) return
        if (isEmployeeScreen(target) && currentUser?.role != Role.Employee) return
        if (target != AppScreen.ManagerTeam) managerQuery = ""
        screen = target
    }

    fun loginWithPin(pin: String): Boolean {
        val user = repository.loginByPinOrNull(pin) ?: return false
        currentUser = user
        when (user.role) {
            Role.Manager -> {
                loadManagerPlanInputs()
                screen = AppScreen.ManagerHome
            }
            Role.Admin -> {
                screen = AppScreen.AdminHome
            }
            Role.Employee -> {
                screen = AppScreen.EmployeeHome
            }
        }
        return true
    }

    fun registerEmployee(name: String, office: String, pin: String) {
        val user = repository.registerEmployee(name, office, pin)
        currentUser = user
        lastCreatedUid = user.uid
        screen = AppScreen.UidCreated
    }

    fun openManagerHome() {
        go(AppScreen.ManagerHome)
    }

    fun selectProduct(productType: ProductType) {
        selectedProduct = productType
        saleFormat = when (productType) {
            ProductType.CreditCard -> "КК по заявке"
            ProductType.DebitCard -> "ДК/стик по заявке"
            ProductType.Loan -> "Кредит наличными"
            ProductType.Deposit -> "Накопительный счет"
            ProductType.Insurance -> "Страхование"
            ProductType.Investment -> "ИИС/ИЗП"
            ProductType.SimCard -> "Карта + СМС"
            ProductType.SalaryProject -> "ЗП Лайт"
            ProductType.Other -> "Другой продукт"
        }
        salePoints = defaultPoints(productType)
    }

    fun saveSale(): Boolean {
        val user = currentUser ?: return false
        if (user.role != Role.Employee) return false
        val cleanLast4 = clientLast4.filter { it.isDigit() }.take(4)
        if (cleanLast4.length != 4) {
            toastMessage = "Введите последние 4 цифры телефона"
            return false
        }
        val count = saleCount.toIntOrNull()?.coerceAtLeast(1) ?: return false
        val amount = saleAmount.toDoubleOrNull() ?: 0.0
        val points = salePoints.toDoubleOrNull() ?: return false
        val sessionId = activeClientSessionId
            ?: repository.createClientSession(user.id, cleanLast4).also { activeClientSessionId = it.id }.id
        repository.addProductToClientSession(
            clientSessionId = sessionId,
            productType = selectedProduct,
            format = saleFormat.ifBlank { selectedProduct.title },
            count = count,
            amount = amount,
            points = points
        )
        toastMessage = "Продукт добавлен к клиенту ${activeClientLabel()}"
        saleCount = "1"
        saleAmount = "0"
        saleClients = "0"
        return true
    }

    fun matchingClientSessions(): List<ClientSession> {
        val user = currentUser?.takeIf { it.role == Role.Employee } ?: return emptyList()
        val cleanLast4 = clientLast4.filter { it.isDigit() }.take(4)
        if (cleanLast4.length != 4) return emptyList()
        return repository.clientSessionsForDay(user.id, cleanLast4)
    }

    fun selectClientSession(sessionId: String) {
        activeClientSessionId = sessionId
        val session = matchingClientSessions().firstOrNull { it.id == sessionId }
        if (session != null) clientLast4 = session.phoneLast4
    }

    fun createNewClientCard(): Boolean {
        val user = currentUser?.takeIf { it.role == Role.Employee } ?: return false
        val cleanLast4 = clientLast4.filter { it.isDigit() }.take(4)
        if (cleanLast4.length != 4) {
            toastMessage = "Введите последние 4 цифры телефона"
            return false
        }
        activeClientSessionId = repository.createClientSession(user.id, cleanLast4).id
        toastMessage = "Создан клиент ${activeClientLabel()}"
        return true
    }

    fun activeClientSales(): List<Sale> =
        activeClientSessionId?.let { repository.salesForClientSession(it) }.orEmpty()

    fun startEditingSale(saleId: String): Boolean {
        val sale = repository.sales().firstOrNull { it.id == saleId } ?: return false
        editingSaleId = sale.id
        selectedProduct = sale.productType
        saleFormat = sale.format
        saleCount = sale.count.toString()
        saleAmount = sale.amount.formatPlanInput()
        salePoints = sale.points.formatPlanInput()
        editReason = ""
        return true
    }

    fun saveEditedSale(): Boolean {
        val saleId = editingSaleId ?: return false
        val editor = currentUser ?: return false
        val count = saleCount.toIntOrNull()?.coerceAtLeast(1) ?: return false
        val amount = saleAmount.toDoubleOrNull() ?: 0.0
        val points = salePoints.toDoubleOrNull() ?: return false
        repository.updateSaleLine(
            saleId = saleId,
            productType = selectedProduct,
            format = saleFormat.ifBlank { selectedProduct.title },
            count = count,
            amount = amount,
            points = points,
            editorId = editor.id,
            reason = editReason.ifBlank { "Исправление продажи" }
        )
        toastMessage = "Продажа исправлена"
        editingSaleId = null
        editReason = ""
        saleCount = "1"
        saleAmount = "0"
        salePoints = defaultPoints(selectedProduct)
        return true
    }

    fun currentEditHistory(): List<SaleEditHistory> =
        currentUser?.let { repository.saleHistoryForUser(it.id) }.orEmpty()

    fun currentEmployeeDetail(): EmployeeDetail? =
        currentUser?.takeIf { it.role == Role.Employee }?.let { repository.employeeDetail(it.id) }

    fun selectedEmployeeDetail(): EmployeeDetail? =
        selectedEmployeeId?.let { repository.employeeDetail(it) }

    fun openEmployeeCard(employeeId: String) {
        selectedEmployeeId = employeeId
        screen = AppScreen.ManagerEmployeeCard
    }

    fun exportCurrentEmployeeExcel(): Boolean {
        val detail = currentEmployeeDetail() ?: return false
        val bytes = ReportExporter.employeeXlsx(detail)
        repository.recordEmployeeReportExport(detail, "xlsx", "employee-preview.xlsx")
        exportMessage = "Excel сформирован: ${bytes.size / 1024 + 1} КБ"
        return true
    }

    fun exportCurrentEmployeeExcel(context: Context): Boolean {
        val detail = currentEmployeeDetail() ?: return false
        val file = ExportFileStore.saveEmployeeXlsx(context, detail)
        repository.recordEmployeeReportExport(detail, "xlsx", file.name)
        exportMessage = "Excel сохранен: ${file.name}"
        return true
    }

    fun exportCurrentEmployeePdf(): Boolean {
        val detail = currentEmployeeDetail() ?: return false
        val bytes = ReportExporter.employeePdf(detail)
        repository.recordEmployeeReportExport(detail, "pdf", "employee-preview.pdf")
        exportMessage = "PDF сформирован: ${bytes.size / 1024 + 1} КБ"
        return true
    }

    fun exportCurrentEmployeePdf(context: Context): Boolean {
        val detail = currentEmployeeDetail() ?: return false
        val file = ExportFileStore.saveEmployeePdf(context, detail)
        repository.recordEmployeeReportExport(detail, "pdf", file.name)
        exportMessage = "PDF сохранен: ${file.name}"
        return true
    }

    fun exportSelectedEmployeeExcel(): Boolean {
        val detail = selectedEmployeeDetail() ?: return false
        val bytes = ReportExporter.employeeXlsx(detail)
        repository.recordEmployeeReportExport(detail, "xlsx", "selected-employee-preview.xlsx")
        exportMessage = "Excel по сотруднику: ${bytes.size / 1024 + 1} КБ"
        return true
    }

    fun exportSelectedEmployeeExcel(context: Context): Boolean {
        val detail = selectedEmployeeDetail() ?: return false
        val file = ExportFileStore.saveEmployeeXlsx(context, detail)
        repository.recordEmployeeReportExport(detail, "xlsx", file.name)
        exportMessage = "Excel по сотруднику сохранен: ${file.name}"
        return true
    }

    fun exportSelectedEmployeePdf(): Boolean {
        val detail = selectedEmployeeDetail() ?: return false
        val bytes = ReportExporter.employeePdf(detail)
        repository.recordEmployeeReportExport(detail, "pdf", "selected-employee-preview.pdf")
        exportMessage = "PDF по сотруднику: ${bytes.size / 1024 + 1} КБ"
        return true
    }

    fun exportSelectedEmployeePdf(context: Context): Boolean {
        val detail = selectedEmployeeDetail() ?: return false
        val file = ExportFileStore.saveEmployeePdf(context, detail)
        repository.recordEmployeeReportExport(detail, "pdf", file.name)
        exportMessage = "PDF по сотруднику сохранен: ${file.name}"
        return true
    }

    fun adminOfficeSummaries(): List<OfficeSummary> {
        if (currentUser?.role != Role.Admin) return emptyList()
        return repository.offices().map { repository.officeSummary(it.id) }
    }

    fun exportOfficeExcel(context: Context): Boolean {
        val rows = adminOfficeSummaries()
        if (rows.isEmpty()) return false
        val file = ExportFileStore.saveOfficeXlsx(context, rows)
        repository.recordOfficeReportExport(rows, "xlsx", file.name)
        exportMessage = "Excel по офисам сохранен: ${file.name}"
        return true
    }

    fun exportOfficePdf(context: Context): Boolean {
        val rows = adminOfficeSummaries()
        if (rows.isEmpty()) return false
        val file = ExportFileStore.saveOfficePdf(context, rows)
        repository.recordOfficeReportExport(rows, "pdf", file.name)
        exportMessage = "PDF по офисам сохранен: ${file.name}"
        return true
    }

    fun activeClientLabel(): String {
        val sessionId = activeClientSessionId ?: return clientLast4
        val session = matchingClientSessions().firstOrNull { it.id == sessionId }
            ?: return clientLast4
        return if (session.sequence == 1) session.phoneLast4 else "${session.phoneLast4} #${session.sequence}"
    }

    fun currentDailyReport(): ReportSummary? =
        currentUser?.takeIf { it.role == Role.Employee }?.let { repository.dailyReport(it.id) }

    fun currentMonthReport(): ReportSummary? =
        currentUser?.takeIf { it.role == Role.Employee }?.let { repository.monthReport(it.id) }

    fun reportCopyText(): String =
        currentUser?.takeIf { it.role == Role.Employee }?.let { repository.telegramReportText(it.id) }.orEmpty()

    fun managerTeam(): List<TeamMemberSummary> {
        val query = managerQuery.trim().lowercase()
        return managerAllTeam()
            .filter { query.isBlank() || it.user.name.lowercase().contains(query) }
    }

    fun managerDailyPoints(): Double =
        managerAllTeam().sumOf { it.dailyReport.points }

    fun managerMonthPoints(): Double =
        managerAllTeam().sumOf { it.monthReport.points }

    fun activeEmployeesToday(): Int =
        managerAllTeam().count { it.dailyReport.products > 0 }

    fun attentionEmployees(): List<TeamMemberSummary> =
        managerAllTeam().filter { it.dailyReport.products == 0 || it.dailyReport.points < 40.0 }

    fun managerPlanPercent(): Int {
        val manager = currentUser?.takeIf { it.role == Role.Manager } ?: return 0
        val plan = repository.planForOffice(manager.office)
        val target = (plan?.pointsTarget ?: 7800.0) * managerAllTeam().size.coerceAtLeast(1)
        return repository.percent(managerMonthPoints(), target)
    }

    fun loadManagerPlanInputs() {
        val manager = currentUser?.takeIf { it.role == Role.Manager } ?: return
        val plan = repository.planForOffice(manager.office) ?: return
        planPointsTarget = plan.pointsTarget.formatPlanInput()
        planClientsTarget = plan.clientsTarget.toString()
        planStatus = null
    }

    fun saveManagerPlan(): Boolean {
        val manager = currentUser?.takeIf { it.role == Role.Manager } ?: return false
        val points = planPointsTarget.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: return false
        val clients = planClientsTarget.toIntOrNull()?.coerceAtLeast(0) ?: return false
        repository.updatePlan(manager.office, points, clients)
        planPointsTarget = points.formatPlanInput()
        planClientsTarget = clients.toString()
        planStatus = "Планы сохранены"
        return true
    }

    private fun defaultPoints(productType: ProductType): String =
        when (productType) {
            ProductType.CreditCard -> "30"
            ProductType.DebitCard -> "25"
            ProductType.Loan -> "80"
            ProductType.Deposit -> "30"
            ProductType.Insurance -> "35"
            ProductType.Investment -> "55"
            ProductType.SimCard -> "20"
            ProductType.SalaryProject -> "30"
            ProductType.Other -> "10"
        }

    private fun Double.formatPlanInput(): String =
        if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()

    private fun managerAllTeam(): List<TeamMemberSummary> {
        val manager = currentUser?.takeIf { it.role == Role.Manager } ?: return emptyList()
        return repository.teamSummaries(manager.id)
    }

    private fun isManagerScreen(target: AppScreen): Boolean =
        target in setOf(
            AppScreen.ManagerHome,
            AppScreen.ManagerTeam,
            AppScreen.ManagerEmployeeCard,
            AppScreen.ManagerPlans,
            AppScreen.ManagerProfile
        )

    private fun isAdminScreen(target: AppScreen): Boolean =
        target == AppScreen.AdminHome

    private fun isEmployeeScreen(target: AppScreen): Boolean =
        target in setOf(
            AppScreen.EmployeeHome,
            AppScreen.AddSale,
            AppScreen.EmployeeReports,
            AppScreen.EmployeeStats,
            AppScreen.EmployeeRanking,
            AppScreen.EmployeeProfile
        )
}
