package com.example.vtbsales

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.vtbsales.data.DemoData
import com.example.vtbsales.data.ReportExporter
import com.example.vtbsales.data.SalesRepository
import com.example.vtbsales.data.local.SalesDatabase
import com.example.vtbsales.data.local.SalesLocalStore
import com.example.vtbsales.model.ProductType
import com.example.vtbsales.model.Role
import com.example.vtbsales.model.AppScreen
import com.example.vtbsales.ui.VtbAppState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SalesRepositoryTest {
    private var database: SalesDatabase? = null
    private var localStore: SalesLocalStore? = null

    @Before
    fun openRoomDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SalesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        localStore = SalesLocalStore(database!!.salesDao())
    }

    @After
    fun closeRoomDatabase() {
        database?.close()
        database = null
        localStore = null
    }

    @Test
    fun employeeReportTotalsMatchDemoTelegramStyleData() {
        val repo = SalesRepository(DemoData.seed())
        val employee = repo.users().first { it.role == Role.Employee }

        val report = repo.dailyReport(employee.id)

        assertEquals(7, report.products)
        assertEquals(2, report.clients)
        assertEquals(195.0, report.points, 0.01)
        assertEquals(3.5, report.productsPerClient, 0.01)
        assertEquals(97.5, report.pointsPerClient, 0.01)
    }

    @Test
    fun addingSaleUpdatesDailyReport() {
        val repo = SalesRepository(DemoData.seed())
        val employee = repo.users().first { it.role == Role.Employee }

        repo.addSale(employee.id, ProductType.CreditCard, "КК по заявке", 1, 0.0, 30.0, 0)
        val report = repo.dailyReport(employee.id)

        assertEquals(8, report.products)
        assertEquals(225.0, report.points, 0.01)
    }

    @Test
    fun managerCanSeeLinkedEmployeesOnly() {
        val repo = SalesRepository(DemoData.seed())
        val manager = repo.users().first { it.role == Role.Manager }

        val team = repo.teamForManager(manager.id)

        assertTrue(team.isNotEmpty())
        assertTrue(team.all { it.role == Role.Employee })
        assertFalse(team.any { it.role == Role.Manager })
    }

    @Test
    fun manualUidLinkingStillAddsUnassignedEmployeeToManagerTeam() {
        val repo = SalesRepository(DemoData.seed())
        val manager = repo.users().first { it.role == Role.Manager }
        val unlinked = repo.registerEmployee("Новый сотрудник", "Временный офис", "2468")

        assertFalse(repo.teamForManager(manager.id).any { it.uid == unlinked.uid })

        val linked = repo.linkEmployeeToManager(manager.id, unlinked.uid)

        assertTrue(linked)
        assertTrue(repo.teamForManager(manager.id).any { it.uid == unlinked.uid })
    }

    @Test
    fun newSalesUseActualCurrentDate() {
        val repo = SalesRepository(DemoData.seed())
        val expectedToday = LocalDate.now()
        val employee = repo.registerEmployee("Новый сотрудник", "office-8617-0290", "2468")

        val client = repo.createClientSession(employee.id, "1357")
        val sale = repo.addProductToClientSession(client.id, ProductType.CreditCard, "КК", 1, 0.0, 30.0)

        assertEquals(expectedToday, employee.createdAt)
        assertEquals(expectedToday, client.date)
        assertEquals(expectedToday, sale.date)
        assertEquals(1, repo.dailyReport(employee.id, expectedToday).products)
    }

    @Test
    fun officeReportExportUsesRealDailySalesTotals() {
        val repo = SalesRepository(DemoData.seed())
        val admin = repo.users().first { it.role == Role.Admin }
        val rows = repo.offices().map { repo.officeSummary(it.id) }
        val expectedReports = repo.visibleTeamFor(admin.id).map { repo.dailyReport(it.id) }

        val export = repo.recordOfficeReportExport(rows, "xlsx", "offices.xlsx")

        assertEquals(expectedReports.sumOf { it.products }, export.products)
        assertEquals(expectedReports.sumOf { it.clients }, export.clients)
        assertEquals(expectedReports.sumOf { it.points }, export.points, 0.01)
        assertEquals(expectedReports.sumOf { it.amount }, export.amount, 0.01)
    }

    @Test
    fun officeUidRegistrationBindsEmployeeToOfficeAndManagerTeam() {
        val repo = SalesRepository(DemoData.seed())
        val manager = repo.users().first { it.role == Role.Manager }

        val employee = repo.registerEmployee("Новый сотрудник", manager.officeId, "2468")

        assertEquals(manager.officeId, employee.officeId)
        assertEquals(manager.office, employee.office)
        assertTrue(repo.teamForManager(manager.id).any { it.id == employee.id })
        assertTrue(repo.officeSummary(manager.officeId).employees >= 4)
    }

    @Test
    fun officeUidRegistrationAcceptsShortBranchCode() {
        val repo = SalesRepository(DemoData.seed())
        val manager = repo.users().first { it.role == Role.Manager }

        val employee = repo.registerEmployee("Новый сотрудник", "8617/0290", "2468")

        assertEquals(manager.officeId, employee.officeId)
        assertEquals(manager.office, employee.office)
        assertTrue(repo.teamForManager(manager.id).any { it.id == employee.id })
    }

    @Test
    fun appStateEmployeeCanBindOfficeUidFromProfile() {
        val repo = SalesRepository(DemoData.seed())
        val manager = repo.users().first { it.role == Role.Manager }
        repo.registerEmployee("Новый сотрудник", "Временный офис", "2468")
        val state = VtbAppState(repo)

        assertTrue(state.loginWithPin("2468"))
        state.officeUidInput = manager.officeId

        assertTrue(state.bindCurrentEmployeeToOffice())
        assertEquals(manager.officeId, state.currentUser!!.officeId)
        assertTrue(repo.teamForManager(manager.id).any { it.id == state.currentUser!!.id })
    }

    @Test
    fun pinLoginReturnsOnlyMatchingUser() {
        val repo = SalesRepository(DemoData.seed())
        val employee = repo.users().first { it.role == Role.Employee }

        assertEquals(employee.id, repo.loginByPin(employee.pinHashDemo).id)
        assertNotNull(repo.loginByPin(employee.pinHashDemo))
    }

    @Test
    fun appStatePinLoginOpensEmployeeHome() {
        val state = VtbAppState(SalesRepository(DemoData.seed()))

        val ok = state.loginWithPin("1111")

        assertTrue(ok)
        assertEquals(Role.Employee, state.currentUser!!.role)
        assertEquals(AppScreen.EmployeeHome, state.screen)
    }

    @Test
    fun employeeCannotOpenManagerAreaDirectly() {
        val state = VtbAppState(SalesRepository(DemoData.seed()))
        state.loginWithPin("1111")

        state.openManagerHome()

        assertEquals(AppScreen.EmployeeHome, state.screen)
    }

    @Test
    fun appStateSavingSaleUpdatesCurrentReport() {
        val state = VtbAppState(SalesRepository(DemoData.seed()))
        state.loginWithPin("1111")

        state.selectProduct(ProductType.CreditCard)
        state.saleFormat = "КК по заявке"
        state.saleCount = "1"
        state.saleAmount = "0"
        state.salePoints = "30"
        state.saleClients = "0"
        state.clientLast4 = "1234"
        state.saveSale()

        assertEquals(8, state.currentDailyReport()!!.products)
        assertEquals(225.0, state.currentDailyReport()!!.points, 0.01)
    }

    @Test
    fun managerPlanCanBeUpdated() {
        val repo = SalesRepository(DemoData.seed())
        val manager = repo.users().first { it.role == Role.Manager }

        repo.updatePlan(manager.office, pointsTarget = 9000.0, clientsTarget = 60)

        val plan = repo.planForOffice(manager.office)!!
        assertEquals(9000.0, plan.pointsTarget, 0.01)
        assertEquals(60, plan.clientsTarget)
    }

    @Test
    fun appStateSavingManagerPlanUpdatesRepository() {
        val repo = SalesRepository(DemoData.seed())
        val state = VtbAppState(repo)

        state.loginWithPin("0000")
        state.planPointsTarget = "9000"
        state.planClientsTarget = "60"

        assertTrue(state.saveManagerPlan())

        val plan = repo.planForOffice(state.currentUser!!.office)!!
        assertEquals(9000.0, plan.pointsTarget, 0.01)
        assertEquals(60, plan.clientsTarget)
    }

    @Test
    fun managerPlanPercentUsesCurrentTeamPlan() {
        val date = LocalDate.of(2026, 7, 3)
        val zone = ZoneId.systemDefault()
        val clock = Clock.fixed(date.atStartOfDay(zone).toInstant(), zone)
        val state = VtbAppState(SalesRepository(DemoData.seed(date), clock = clock))

        state.loginWithPin("0000")

        assertEquals(3, state.managerPlanPercent())
    }

    @Test
    fun managerSearchDoesNotLeakIntoOtherManagerScreens() {
        val state = VtbAppState(SalesRepository(DemoData.seed()))
        state.loginWithPin("0000")
        val fullMonthPoints = state.managerMonthPoints()

        state.managerQuery = "not-found"
        state.go(AppScreen.ManagerHome)

        assertEquals("", state.managerQuery)
        assertEquals(fullMonthPoints, state.managerMonthPoints(), 0.01)
    }

    @Test
    fun multipleProductsInOneClientCardCountAsOneClient() {
        val repo = SalesRepository(DemoData.seed())
        val employee = repo.registerEmployee("Новый сотрудник", "Доп. офис №8617/0290", "2468")
        val client = repo.createClientSession(employee.id, "1234")

        repo.addProductToClientSession(client.id, ProductType.CreditCard, "КК по заявке", 1, 0.0, 30.0)
        repo.addProductToClientSession(client.id, ProductType.Deposit, "Накопительный счет", 1, 0.0, 30.0)

        val report = repo.dailyReport(employee.id)
        assertEquals(1, report.clients)
        assertEquals(2, report.products)
        assertEquals(60.0, report.points, 0.01)
    }

    @Test
    fun sameLastFourDigitsCanCreateSeparateClientCards() {
        val repo = SalesRepository(DemoData.seed())
        val employee = repo.registerEmployee("Новый сотрудник", "Доп. офис №8617/0290", "2468")

        val first = repo.createClientSession(employee.id, "1234")
        val second = repo.createClientSession(employee.id, "1234")

        repo.addProductToClientSession(first.id, ProductType.CreditCard, "КК по заявке", 1, 0.0, 30.0)
        repo.addProductToClientSession(second.id, ProductType.SimCard, "Карта + СМС", 1, 0.0, 20.0)

        val report = repo.dailyReport(employee.id)
        assertEquals(2, report.clients)
        assertEquals(2, report.products)
        assertEquals(2, repo.clientSessionsForDay(employee.id, "1234").size)
    }

    @Test
    fun appStateAddsProductsToSelectedClientCard() {
        val state = VtbAppState(SalesRepository(DemoData.seed()))
        state.loginWithPin("1111")
        state.clientLast4 = "7777"

        assertTrue(state.saveSale())
        state.selectProduct(ProductType.Deposit)
        assertTrue(state.saveSale())

        val addedClientSales = state.activeClientSales()
        assertEquals(2, addedClientSales.size)
        assertEquals("7777", state.activeClientLabel())
    }

    @Test
    fun saleLineCanBeEditedWithoutChangingClientCount() {
        val repo = SalesRepository(DemoData.seed())
        val employee = repo.registerEmployee("Новый сотрудник", "Доп. офис №8617/0290", "2468")
        val client = repo.createClientSession(employee.id, "9876")
        val sale = repo.addProductToClientSession(client.id, ProductType.CreditCard, "КК", 1, 0.0, 30.0)

        repo.updateSaleLine(sale.id, ProductType.CreditCard, "КК исправлено", 2, 0.0, 60.0)

        val report = repo.dailyReport(employee.id)
        assertEquals(1, report.clients)
        assertEquals(2, report.products)
        assertEquals(60.0, report.points, 0.01)
    }

    @Test
    fun managerSeesOnlyOwnOfficeEmployeesInLocalV2() {
        val repo = SalesRepository(DemoData.seed())
        val manager = repo.users().first { it.pinHashDemo == "0000" }

        val visible = repo.visibleTeamFor(manager.id)

        assertTrue(visible.isNotEmpty())
        assertTrue(visible.all { it.officeId == manager.officeId })
        assertFalse(visible.any { it.officeId != manager.officeId })
    }

    @Test
    fun adminSeesEveryOfficeAndOfficeSummaries() {
        val repo = SalesRepository(DemoData.seed())
        val admin = repo.users().first { it.role == Role.Admin }

        val visible = repo.visibleTeamFor(admin.id)
        val offices = repo.offices()

        assertTrue(offices.size >= 2)
        assertTrue(visible.map { it.officeId }.toSet().containsAll(offices.map { it.id }))
        assertTrue(repo.officeSummary(offices.first().id).employees > 0)
    }

    @Test
    fun editingSaleLineRecordsHistoryAndRecalculatesReport() {
        val repo = SalesRepository(DemoData.seed())
        val employee = repo.registerEmployee("Новый сотрудник", "Доп. офис №8617/0290", "2468")
        val client = repo.createClientSession(employee.id, "2222")
        val sale = repo.addProductToClientSession(client.id, ProductType.CreditCard, "КК", 1, 0.0, 30.0)

        repo.updateSaleLine(
            saleId = sale.id,
            productType = ProductType.CreditCard,
            format = "КК по заявке",
            count = 2,
            amount = 0.0,
            points = 60.0,
            editorId = employee.id,
            reason = "Исправлено количество"
        )

        val history = repo.saleHistoryForUser(employee.id)
        assertEquals(1, history.size)
        assertEquals(1, history.first().before.count)
        assertEquals(2, history.first().after.count)
        assertEquals("Исправлено количество", history.first().reason)
        assertEquals(2, repo.dailyReport(employee.id).products)
        assertEquals(60.0, repo.dailyReport(employee.id).points, 0.01)
    }

    @Test
    fun appStateCanEditActiveClientSaleLine() {
        val state = VtbAppState(SalesRepository(DemoData.seed()))
        state.loginWithPin("1111")
        state.clientLast4 = "4321"
        state.saveSale()
        val sale = state.activeClientSales().first()

        state.startEditingSale(sale.id)
        state.saleCount = "2"
        state.salePoints = "60"
        state.editReason = "Исправление после сверки"

        assertTrue(state.saveEditedSale())
        assertEquals(60.0, state.activeClientSales().first().points, 0.01)
        assertEquals(1, state.currentEditHistory().size)
    }

    @Test
    fun employeeDetailIncludesReportsClientCardsAndHistory() {
        val repo = SalesRepository(DemoData.seed())
        val employee = repo.registerEmployee("Новый сотрудник", "Доп. офис №8617/0290", "2468")
        val client = repo.createClientSession(employee.id, "6789")
        val sale = repo.addProductToClientSession(client.id, ProductType.Deposit, "Вклад", 1, 100000.0, 30.0)
        repo.updateSaleLine(sale.id, ProductType.Deposit, "Вклад", 2, 200000.0, 60.0, employee.id, "Дубль продукта")

        val detail = repo.employeeDetail(employee.id)

        assertEquals(employee.id, detail.user.id)
        assertEquals(2, detail.dailyReport.products)
        assertEquals(1, detail.clientCards.size)
        assertEquals("6789", detail.clientCards.first().session.phoneLast4)
        assertEquals(1, detail.clientCards.first().sales.size)
        assertEquals(1, detail.editHistory.size)
    }

    @Test
    fun appStateProvidesCurrentEmployeeDetail() {
        val state = VtbAppState(SalesRepository(DemoData.seed()))
        state.loginWithPin("1111")

        val detail = state.currentEmployeeDetail()

        assertNotNull(detail)
        assertEquals(state.currentUser!!.id, detail!!.user.id)
        assertEquals(state.currentDailyReport()!!.points, detail.dailyReport.points, 0.01)
    }

    @Test
    fun exporterCreatesExcelAndPdfBytesForEmployeeDetail() {
        val repo = SalesRepository(DemoData.seed())
        val employee = repo.users().first { it.role == Role.Employee }
        val detail = repo.employeeDetail(employee.id)

        val xlsx = ReportExporter.employeeXlsx(detail)
        val pdf = ReportExporter.employeePdf(detail)

        assertTrue(xlsx.size > 100)
        assertEquals('P'.code.toByte(), xlsx[0])
        assertEquals('K'.code.toByte(), xlsx[1])
        assertTrue(pdf.size > 100)
        assertEquals("%PDF", pdf.copyOfRange(0, 4).toString(Charsets.UTF_8))
    }

    @Test
    fun exporterCreatesExcelAndPdfBytesForOfficeSummary() {
        val repo = SalesRepository(DemoData.seed())
        val rows = repo.offices().map { repo.officeSummary(it.id) }

        val xlsx = ReportExporter.officeXlsx(rows)
        val pdf = ReportExporter.officePdf(rows)

        assertTrue(xlsx.size > 100)
        assertEquals('P'.code.toByte(), xlsx[0])
        assertEquals('K'.code.toByte(), xlsx[1])
        assertTrue(pdf.size > 100)
        assertEquals("%PDF", pdf.copyOfRange(0, 4).toString(Charsets.UTF_8))
    }

    @Test
    fun appStateAdminLoginOpensAdminHome() {
        val state = VtbAppState(SalesRepository(DemoData.seed()))

        assertTrue(state.loginWithPin("9999"))

        assertEquals(Role.Admin, state.currentUser!!.role)
        assertEquals(AppScreen.AdminHome, state.screen)
        assertTrue(state.adminOfficeSummaries().size >= 2)
    }

    @Test
    fun roomStoreSeedsLocalDatabaseWithCoreTables() {
        val seed = DemoData.seed()

        val loaded = localStore!!.loadSeedOrSeed(seed)

        assertEquals(seed.users.size, loaded.users.size)
        assertEquals(seed.offices.size, loaded.offices.size)
        assertEquals(seed.sales.size, loaded.sales.size)
        assertEquals(seed.clientSessions.size, loaded.clientSessions.size)
        assertEquals(ProductType.entries.size, localStore!!.productCount())
    }

    @Test
    fun roomBackedRepositoryAutosavesSalesAndEditHistory() {
        val firstRun = SalesRepository.fromLocalStore(localStore!!, DemoData.seed())
        val employee = firstRun.registerEmployee("Local Room User", "Room Office", "8642")
        val client = firstRun.createClientSession(employee.id, "9090")
        val sale = firstRun.addProductToClientSession(
            clientSessionId = client.id,
            productType = ProductType.CreditCard,
            format = "Card by request",
            count = 1,
            amount = 0.0,
            points = 30.0
        )

        firstRun.updateSaleLine(
            saleId = sale.id,
            productType = ProductType.CreditCard,
            format = "Card by request",
            count = 2,
            amount = 0.0,
            points = 60.0,
            editorId = employee.id,
            reason = "Audit correction"
        )

        val reloaded = SalesRepository.fromLocalStore(localStore!!, DemoData.seed())
        val detail = reloaded.employeeDetail(employee.id)

        assertEquals(1, detail.clientCards.size)
        assertEquals("9090", detail.clientCards.first().session.phoneLast4)
        assertEquals(2, detail.clientCards.first().sales.first().count)
        assertEquals(1, detail.editHistory.size)
        assertEquals("Audit correction", detail.editHistory.first().reason)
    }

    @Test
    fun reportExportsArePersistedInRoom() {
        val repo = SalesRepository.fromLocalStore(localStore!!, DemoData.seed())
        val employee = repo.users().first { it.role == Role.Employee }
        val detail = repo.employeeDetail(employee.id)

        repo.recordEmployeeReportExport(detail, "xlsx", "employee.xlsx")
        repo.recordOfficeReportExport(repo.offices().map { repo.officeSummary(it.id) }, "pdf", "offices.pdf")

        val exports = SalesRepository.fromLocalStore(localStore!!, DemoData.seed()).reportExports()

        assertEquals(2, exports.size)
        assertTrue(exports.any { it.scope == "employee" && it.fileName == "employee.xlsx" })
        assertTrue(exports.any { it.scope == "office" && it.fileName == "offices.pdf" })
    }

    @Test
    fun appStateEmployeeExportWritesReportRecordToRoom() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SalesRepository.fromLocalStore(localStore!!, DemoData.seed())
        val state = VtbAppState(repo)

        state.loginWithPin("1111")

        assertTrue(state.exportCurrentEmployeeExcel(context))
        val exports = SalesRepository.fromLocalStore(localStore!!, DemoData.seed()).reportExports()

        assertTrue(exports.any { it.scope == "employee" && it.kind == "xlsx" })
    }
}
