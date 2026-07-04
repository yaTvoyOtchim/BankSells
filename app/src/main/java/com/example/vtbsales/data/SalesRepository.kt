package com.example.vtbsales.data

import android.content.Context
import com.example.vtbsales.data.local.SalesDatabase
import com.example.vtbsales.data.local.SalesLocalStore
import com.example.vtbsales.model.ProductType
import com.example.vtbsales.model.Plan
import com.example.vtbsales.model.ReportSummary
import com.example.vtbsales.model.ReportExport
import com.example.vtbsales.model.Role
import com.example.vtbsales.model.Sale
import com.example.vtbsales.model.SaleEditHistory
import com.example.vtbsales.model.SaleSnapshot
import com.example.vtbsales.model.SalesSeed
import com.example.vtbsales.model.TeamLink
import com.example.vtbsales.model.TeamMemberSummary
import com.example.vtbsales.model.User
import com.example.vtbsales.model.ClientSession
import com.example.vtbsales.model.ClientSessionDetail
import com.example.vtbsales.model.EmployeeDetail
import com.example.vtbsales.model.Office
import com.example.vtbsales.model.OfficeSummary
import java.time.Clock
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

class SalesRepository(
    seed: SalesSeed,
    private val localStore: SalesLocalStore? = null,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val users = seed.users.toMutableList()
    private val sales = seed.sales.toMutableList()
    private val teamLinks = seed.teamLinks.toMutableList()
    private val plans = seed.plans.toMutableList()
    private val clientSessions = seed.clientSessions.toMutableList()
    private val saleHistory = seed.saleHistory.toMutableList()
    private val reportExports = seed.reportExports.toMutableList()
    private val offices = seed.offices.ifEmpty {
        seed.users
            .filter { it.role != Role.Admin }
            .map { Office(it.officeId, it.office, "", "") }
            .distinctBy { it.id }
    }.toMutableList()
    private val today: LocalDate
        get() = LocalDate.now(clock)

    companion object {
        fun local(
            context: Context,
            seed: SalesSeed = DemoData.seed(),
            clock: Clock = Clock.systemDefaultZone()
        ): SalesRepository =
            fromLocalStore(
                SalesLocalStore(SalesDatabase.get(context).salesDao()),
                seed,
                clock
            )

        fun fromLocalStore(
            localStore: SalesLocalStore,
            seed: SalesSeed,
            clock: Clock = Clock.systemDefaultZone()
        ): SalesRepository =
            SalesRepository(localStore.loadSeedOrSeed(seed), localStore, clock)
    }

    fun users(): List<User> = users.toList()

    fun sales(): List<Sale> = sales.toList()

    fun offices(): List<Office> = offices.toList()

    fun reportExports(): List<ReportExport> = reportExports.toList()

    fun visibleTeamFor(userId: String): List<User> {
        val user = users.firstOrNull { it.id == userId } ?: return emptyList()
        return when (user.role) {
            Role.Employee -> listOf(user)
            Role.Manager -> teamForManager(user.id)
            Role.Admin -> users.filter { it.role == Role.Employee }.sortedWith(compareBy<User> { it.officeId }.thenBy { it.name })
        }
    }

    fun officeSummary(officeId: String): OfficeSummary {
        val office = offices.firstOrNull { it.id == officeId }
            ?: Office(officeId, officeId, "", "")
        val employees = users.filter { it.role == Role.Employee && it.officeId == officeId }
        val daily = employees.sumOf { dailyReport(it.id).points }
        val month = employees.sumOf { monthReport(it.id).points }
        val attention = employees.count { dailyReport(it.id).products == 0 }
        return OfficeSummary(office, employees.size, daily, month, attention)
    }

    fun clientSessionsForDay(userId: String, phoneLast4: String, date: LocalDate = today): List<ClientSession> =
        clientSessions
            .filter { it.userId == userId && it.phoneLast4 == phoneLast4 && it.date == date }
            .sortedBy { it.sequence }

    fun salesForClientSession(clientSessionId: String): List<Sale> =
        sales.filter { it.clientSessionId == clientSessionId }

    fun saleHistoryForUser(userId: String): List<SaleEditHistory> =
        saleHistory.filter { it.userId == userId }.sortedByDescending { it.editedAt }

    fun saleHistoryForSale(saleId: String): List<SaleEditHistory> =
        saleHistory.filter { it.saleId == saleId }.sortedByDescending { it.editedAt }

    fun employeeDetail(userId: String): EmployeeDetail {
        val user = users.first { it.id == userId }
        val daily = dailyReport(userId)
        val month = monthReport(userId)
        val cards = clientSessions
            .filter { it.userId == userId && it.date == today }
            .sortedWith(compareBy<ClientSession> { it.phoneLast4 }.thenBy { it.sequence })
            .map { session ->
                ClientSessionDetail(session, salesForClientSession(session.id))
            }
        return EmployeeDetail(
            user = user,
            dailyReport = daily,
            monthReport = month,
            clientCards = cards,
            editHistory = saleHistoryForUser(userId)
        )
    }

    fun createClientSession(userId: String, phoneLast4: String, date: LocalDate = today): ClientSession {
        val cleanLast4 = phoneLast4.filter { it.isDigit() }.takeLast(4)
        require(cleanLast4.length == 4) { "Нужно ввести последние 4 цифры телефона" }
        val sequence = clientSessionsForDay(userId, cleanLast4, date).size + 1
        val session = ClientSession(
            id = "client-${clientSessions.size + 1}",
            userId = userId,
            phoneLast4 = cleanLast4,
            sequence = sequence,
            date = date
        )
        clientSessions += session
        localStore?.upsertClientSession(session)
        return session
    }

    fun planForOffice(office: String) = plans.firstOrNull { it.office == office }

    fun updatePlan(office: String, pointsTarget: Double, clientsTarget: Int): Plan {
        val current = planForOffice(office)
        val updated = Plan(
            office = office,
            month = current?.month ?: "%04d-%02d".format(today.year, today.monthValue),
            pointsTarget = pointsTarget,
            clientsTarget = clientsTarget
        )
        val index = plans.indexOfFirst { it.office == office }
        if (index >= 0) {
            plans[index] = updated
        } else {
            plans += updated
        }
        localStore?.upsertPlan(updated)
        return updated
    }

    fun loginByPin(pin: String): User =
        loginByPinOrNull(pin) ?: error("Неверный PIN")

    fun loginByPinOrNull(pin: String): User? =
        users.firstOrNull { it.pinHashDemo == pin }

    fun registerEmployee(name: String, office: String, pin: String): User {
        val next = users.count { it.role == Role.Employee } + 1
        val uid = "VTB-${(100 + next * 37)}-${(700 + next * 19)}"
        val resolvedOffice = resolveOffice(office)
        val officeTitle = resolvedOffice?.title ?: office.ifBlank { "Офис ВТБ" }
        val officeId = resolvedOffice?.id ?: officeTitle
        val employee = User(
            id = "employee-new-$next",
            uid = uid,
            name = name,
            role = Role.Employee,
            office = officeTitle,
            pinHashDemo = pin,
            createdAt = today,
            officeId = officeId
        )
        users += employee
        localStore?.upsertUser(employee)
        if (resolvedOffice != null) linkEmployeeToOfficeManagers(employee, resolvedOffice)
        return employee
    }

    fun linkEmployeeToManager(managerId: String, employeeUid: String): Boolean {
        val manager = users.firstOrNull { it.id == managerId && it.role == Role.Manager } ?: return false
        val employee = users.firstOrNull { it.uid == employeeUid && it.role == Role.Employee } ?: return false
        val index = users.indexOfFirst { it.id == employee.id }
        val updatedEmployee = employee.copy(office = manager.office, officeId = manager.officeId)
        users[index] = updatedEmployee
        localStore?.upsertUser(updatedEmployee)
        if (teamLinks.any { it.managerId == manager.id && it.employeeId == employee.id }) return false
        val link = TeamLink(manager.id, employee.id, manager.office)
        teamLinks += link
        localStore?.upsertTeamLink(link)
        return true
    }

    fun bindEmployeeToOffice(userId: String, officeUid: String): User? {
        val office = resolveOffice(officeUid) ?: return null
        val index = users.indexOfFirst { it.id == userId && it.role == Role.Employee }
        if (index < 0) return null
        val updated = users[index].copy(office = office.title, officeId = office.id)
        users[index] = updated
        localStore?.upsertUser(updated)
        linkEmployeeToOfficeManagers(updated, office)
        return updated
    }

    fun officeJoinCodeFor(user: User): String? =
        offices.firstOrNull { it.id == user.officeId }?.id

    fun teamForManager(managerId: String): List<User> {
        val manager = users.firstOrNull { it.id == managerId && it.role == Role.Manager } ?: return emptyList()
        val employeeIds = teamLinks
            .filter { it.managerId == managerId }
            .map { it.employeeId }
            .toSet()
        return users
            .filter { it.id in employeeIds && it.role == Role.Employee && it.officeId == manager.officeId }
            .sortedBy { it.name }
    }

    fun addSale(
        userId: String,
        productType: ProductType,
        format: String,
        count: Int,
        amount: Double,
        points: Double,
        clients: Int,
        date: LocalDate = today
    ): Sale {
        val sale = Sale(
            id = "sale-${sales.size + 1}",
            userId = userId,
            productType = productType,
            format = format,
            count = count,
            amount = amount,
            points = points,
            clients = clients,
            date = date
        )
        sales += sale
        localStore?.upsertSale(sale)
        return sale
    }

    fun addProductToClientSession(
        clientSessionId: String,
        productType: ProductType,
        format: String,
        count: Int,
        amount: Double,
        points: Double
    ): Sale {
        val session = clientSessions.first { it.id == clientSessionId }
        val sale = Sale(
            id = "sale-${sales.size + 1}",
            userId = session.userId,
            productType = productType,
            format = format,
            count = count,
            amount = amount,
            points = points,
            clients = 0,
            date = session.date,
            clientSessionId = session.id
        )
        sales += sale
        localStore?.upsertSale(sale)
        return sale
    }

    fun updateSaleLine(
        saleId: String,
        productType: ProductType,
        format: String,
        count: Int,
        amount: Double,
        points: Double,
        editorId: String = "system",
        reason: String = "Правка продажи"
    ): Sale? {
        val index = sales.indexOfFirst { it.id == saleId }
        if (index < 0) return null
        val current = sales[index]
        val before = current.snapshot()
        val updated = current.copy(
            productType = productType,
            format = format,
            count = count,
            amount = amount,
            points = points
        )
        sales[index] = updated
        val history = SaleEditHistory(
            id = "history-${saleHistory.size + 1}",
            saleId = saleId,
            userId = current.userId,
            editorId = editorId,
            editedAt = today,
            reason = reason.ifBlank { "Правка продажи" },
            before = before,
            after = updated.snapshot()
        )
        saleHistory += history
        localStore?.upsertSale(updated)
        localStore?.upsertSaleHistory(history)
        return updated
    }

    fun dailyReport(userId: String, date: LocalDate = today): ReportSummary =
        report(userId, date, date)

    fun monthReport(userId: String, date: LocalDate = today): ReportSummary =
        report(userId, date.withDayOfMonth(1), date)

    fun report(userId: String, from: LocalDate, to: LocalDate): ReportSummary {
        val user = users.first { it.id == userId }
        val selected = sales.filter { it.userId == userId && !it.date.isBefore(from) && !it.date.isAfter(to) }
        val products = selected.sumOf { it.count }
        val legacyClients = selected.filter { it.clientSessionId == null }.sumOf { it.clients }
        val sessionClients = selected.mapNotNull { it.clientSessionId }.distinct().size
        val clients = legacyClients + sessionClients
        val points = selected.sumOf { it.points }
        val amount = selected.sumOf { it.amount }
        val breakdown = selected
            .filter { it.productType != ProductType.Other }
            .groupBy { it.productType }
            .mapValues { (_, items) -> items.sumOf { it.count } }
        val other = selected
            .filter { it.productType == ProductType.Other }
            .groupBy { it.format }
            .mapValues { (_, items) -> items.sumOf { it.count } }
        return ReportSummary(user, from, to, products, clients, points, amount, breakdown, other)
    }

    fun recordEmployeeReportExport(
        detail: EmployeeDetail,
        kind: String,
        fileName: String
    ): ReportExport {
        val report = ReportExport(
            id = nextReportExportId(),
            scope = "employee",
            ownerId = detail.user.id,
            officeId = detail.user.officeId,
            kind = kind,
            fileName = fileName,
            createdAt = today,
            from = detail.dailyReport.from,
            to = detail.dailyReport.to,
            products = detail.dailyReport.products,
            clients = detail.dailyReport.clients,
            points = detail.dailyReport.points,
            amount = detail.dailyReport.amount
        )
        reportExports += report
        localStore?.upsertReportExport(report)
        return report
    }

    fun recordOfficeReportExport(
        rows: List<OfficeSummary>,
        kind: String,
        fileName: String
    ): ReportExport {
        val officeIds = rows.map { it.office.id }.toSet()
        val reports = users
            .filter { it.role == Role.Employee && it.officeId in officeIds }
            .map { dailyReport(it.id) }
        val report = ReportExport(
            id = nextReportExportId(),
            scope = "office",
            ownerId = null,
            officeId = null,
            kind = kind,
            fileName = fileName,
            createdAt = today,
            from = today,
            to = today,
            products = reports.sumOf { it.products },
            clients = reports.sumOf { it.clients },
            points = reports.sumOf { it.points },
            amount = reports.sumOf { it.amount }
        )
        reportExports += report
        localStore?.upsertReportExport(report)
        return report
    }

    fun teamSummaries(managerId: String): List<TeamMemberSummary> {
        val members = teamForManager(managerId)
        val monthReports = members.associateWith { monthReport(it.id) }
        val ranked = monthReports.entries
            .sortedByDescending { it.value.points }
            .mapIndexed { index, entry -> entry.key.id to index + 1 }
            .toMap()
        return members.map { user ->
            TeamMemberSummary(
                user = user,
                dailyReport = dailyReport(user.id),
                monthReport = monthReports.getValue(user),
                rank = ranked.getValue(user.id)
            )
        }.sortedBy { it.rank }
    }

    fun telegramReportText(userId: String): String {
        val day = dailyReport(userId)
        val month = monthReport(userId)
        return buildString {
            appendLine("📄 Отчет за сегодня")
            appendLine("👤 Продавец: ${day.user.name}")
            appendLine("👥 Клиентов: ${day.clients}")
            appendLine()
            appendLine("Отчет по формату:")
            day.productBreakdown.forEach { (type, count) -> appendLine("📦 ${type.title}: $count шт") }
            if (day.otherProducts.isNotEmpty()) {
                appendLine()
                appendLine("➕ Другие продукты:")
                day.otherProducts.forEach { (name, count) -> appendLine("📦 $name: $count шт") }
            }
            appendLine()
            appendLine("⭐ Баллы: ${day.points.format1()}")
            appendLine("📊 Продуктов на клиента: ${day.productsPerClient.format2()}")
            appendLine("📊 Баллов на клиента: ${day.pointsPerClient.format2()}")
            appendLine()
            appendLine("📄 Итог с начала месяца")
            appendLine("👥 Клиентов: ${month.clients}")
            appendLine("⭐ Баллы: ${month.points.format1()}")
        }
    }

    fun percent(value: Double, target: Double): Int =
        if (target <= 0.0) 0 else ((value / target) * 100).roundToInt().coerceIn(0, 999)

    private fun resolveOffice(rawOfficeUid: String): Office? {
        val key = rawOfficeUid.normalizedKey()
        val digits = rawOfficeUid.digitsOnly()
        if (key.isBlank() && digits.isBlank()) return null
        val byManagerUid = users
            .firstOrNull { it.role == Role.Manager && it.uid.normalizedKey() == key }
            ?.let { manager -> offices.firstOrNull { it.id == manager.officeId } }
        if (byManagerUid != null) return byManagerUid
        return offices.firstOrNull { office ->
            key in office.lookupKeys() || digits.isNotBlank() && digits in office.lookupKeys()
        }
    }

    private fun linkEmployeeToOfficeManagers(employee: User, office: Office) {
        users
            .filter { it.role == Role.Manager && it.officeId == office.id }
            .forEach { manager ->
                if (teamLinks.none { it.managerId == manager.id && it.employeeId == employee.id }) {
                    val link = TeamLink(manager.id, employee.id, office.title)
                    teamLinks += link
                    localStore?.upsertTeamLink(link)
                }
            }
    }

    private fun Office.lookupKeys(): Set<String> =
        setOf(id.normalizedKey(), title.normalizedKey(), id.digitsOnly(), title.digitsOnly())
            .filter { it.isNotBlank() }
            .toSet()

    private fun String.normalizedKey(): String =
        lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

    private fun String.digitsOnly(): String =
        filter { it.isDigit() }

    private fun Double.format1(): String = String.format(Locale.US, "%.1f", this)
    private fun Double.format2(): String = String.format(Locale.US, "%.2f", this)

    private fun nextReportExportId(): String =
        "report-${reportExports.size + 1}"

    private fun Sale.snapshot(): SaleSnapshot =
        SaleSnapshot(productType, format, count, amount, points)
}
