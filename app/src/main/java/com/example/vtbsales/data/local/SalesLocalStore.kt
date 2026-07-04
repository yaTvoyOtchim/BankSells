package com.example.vtbsales.data.local

import com.example.vtbsales.model.ClientSession
import com.example.vtbsales.model.Office
import com.example.vtbsales.model.Plan
import com.example.vtbsales.model.ProductType
import com.example.vtbsales.model.ReportExport
import com.example.vtbsales.model.Role
import com.example.vtbsales.model.Sale
import com.example.vtbsales.model.SaleEditHistory
import com.example.vtbsales.model.SaleSnapshot
import com.example.vtbsales.model.SaleStatus
import com.example.vtbsales.model.SalesSeed
import com.example.vtbsales.model.TeamLink
import com.example.vtbsales.model.User
import java.time.LocalDate
import kotlinx.coroutines.runBlocking

class SalesLocalStore(private val dao: SalesDao) {
    fun loadSeedOrSeed(seed: SalesSeed): SalesSeed = runBlocking {
        if (dao.userCount() == 0) {
            seedDatabase(seed)
        } else if (dao.productCount() == 0) {
            dao.upsertProducts(productCatalog())
        }
        loadSeed()
    }

    fun productCount(): Int = runBlocking {
        dao.productCount()
    }

    fun reportExports(): List<ReportExport> = runBlocking {
        dao.reportExports().map { it.toModel() }
    }

    fun upsertUser(user: User) = runBlocking {
        dao.upsertUser(user.toEntity())
    }

    fun upsertTeamLink(link: TeamLink) = runBlocking {
        dao.upsertTeamLink(link.toEntity())
    }

    fun upsertPlan(plan: Plan) = runBlocking {
        dao.upsertPlan(plan.toEntity())
    }

    fun upsertClientSession(session: ClientSession) = runBlocking {
        dao.upsertClientSession(session.toEntity())
    }

    fun upsertSale(sale: Sale) = runBlocking {
        dao.upsertSale(sale.toEntity())
    }

    fun upsertSaleHistory(history: SaleEditHistory) = runBlocking {
        dao.upsertSaleHistory(history.toEntity())
    }

    fun upsertReportExport(report: ReportExport) = runBlocking {
        dao.upsertReportExport(report.toEntity())
    }

    private suspend fun seedDatabase(seed: SalesSeed) {
        dao.upsertProducts(productCatalog())
        dao.upsertOffices(seed.offices.map { it.toEntity() })
        dao.upsertUsers(seed.users.map { it.toEntity() })
        dao.upsertTeamLinks(seed.teamLinks.map { it.toEntity() })
        dao.upsertPlans(seed.plans.map { it.toEntity() })
        dao.upsertClientSessions(seed.clientSessions.map { it.toEntity() })
        dao.upsertSales(seed.sales.map { it.toEntity() })
        dao.upsertSaleHistory(seed.saleHistory.map { it.toEntity() })
        dao.upsertReportExports(seed.reportExports.map { it.toEntity() })
    }

    private suspend fun loadSeed(): SalesSeed =
        SalesSeed(
            users = dao.users().map { it.toModel() },
            sales = dao.sales().map { it.toModel() },
            teamLinks = dao.teamLinks().map { it.toModel() },
            plans = dao.plans().map { it.toModel() },
            clientSessions = dao.clientSessions().map { it.toModel() },
            offices = dao.offices().map { it.toModel() },
            saleHistory = dao.saleHistory().map { it.toModel() },
            reportExports = dao.reportExports().map { it.toModel() }
        )

    private fun productCatalog(): List<ProductEntity> =
        ProductType.entries.map { ProductEntity(it.name, it.title) }

    private fun Office.toEntity(): OfficeEntity =
        OfficeEntity(id, title, city, region)

    private fun OfficeEntity.toModel(): Office =
        Office(id, title, city, region)

    private fun User.toEntity(): UserEntity =
        UserEntity(
            id = id,
            uid = uid,
            name = name,
            role = role.name,
            office = office,
            pinHashDemo = pinHashDemo,
            createdAt = createdAt.toString(),
            officeId = officeId
        )

    private fun UserEntity.toModel(): User =
        User(
            id = id,
            uid = uid,
            name = name,
            role = Role.valueOf(role),
            office = office,
            pinHashDemo = pinHashDemo,
            createdAt = LocalDate.parse(createdAt),
            officeId = officeId
        )

    private fun TeamLink.toEntity(): TeamLinkEntity =
        TeamLinkEntity(managerId, employeeId, office)

    private fun TeamLinkEntity.toModel(): TeamLink =
        TeamLink(managerId, employeeId, office)

    private fun Plan.toEntity(): PlanEntity =
        PlanEntity(office, month, pointsTarget, clientsTarget)

    private fun PlanEntity.toModel(): Plan =
        Plan(office, month, pointsTarget, clientsTarget)

    private fun ClientSession.toEntity(): ClientSessionEntity =
        ClientSessionEntity(id, userId, phoneLast4, sequence, date.toString())

    private fun ClientSessionEntity.toModel(): ClientSession =
        ClientSession(id, userId, phoneLast4, sequence, LocalDate.parse(date))

    private fun Sale.toEntity(): SaleEntity =
        SaleEntity(
            id = id,
            userId = userId,
            productType = productType.name,
            format = format,
            count = count,
            amount = amount,
            points = points,
            clients = clients,
            date = date.toString(),
            clientSessionId = clientSessionId,
            status = status.name
        )

    private fun SaleEntity.toModel(): Sale =
        Sale(
            id = id,
            userId = userId,
            productType = ProductType.valueOf(productType),
            format = format,
            count = count,
            amount = amount,
            points = points,
            clients = clients,
            date = LocalDate.parse(date),
            clientSessionId = clientSessionId,
            status = SaleStatus.valueOf(status)
        )

    private fun SaleEditHistory.toEntity(): SaleEditHistoryEntity =
        SaleEditHistoryEntity(
            id = id,
            saleId = saleId,
            userId = userId,
            editorId = editorId,
            editedAt = editedAt.toString(),
            reason = reason,
            beforeProductType = before.productType.name,
            beforeFormat = before.format,
            beforeCount = before.count,
            beforeAmount = before.amount,
            beforePoints = before.points,
            afterProductType = after.productType.name,
            afterFormat = after.format,
            afterCount = after.count,
            afterAmount = after.amount,
            afterPoints = after.points
        )

    private fun SaleEditHistoryEntity.toModel(): SaleEditHistory =
        SaleEditHistory(
            id = id,
            saleId = saleId,
            userId = userId,
            editorId = editorId,
            editedAt = LocalDate.parse(editedAt),
            reason = reason,
            before = SaleSnapshot(
                productType = ProductType.valueOf(beforeProductType),
                format = beforeFormat,
                count = beforeCount,
                amount = beforeAmount,
                points = beforePoints
            ),
            after = SaleSnapshot(
                productType = ProductType.valueOf(afterProductType),
                format = afterFormat,
                count = afterCount,
                amount = afterAmount,
                points = afterPoints
            )
        )

    private fun ReportExport.toEntity(): ReportExportEntity =
        ReportExportEntity(
            id = id,
            scope = scope,
            ownerId = ownerId,
            officeId = officeId,
            kind = kind,
            fileName = fileName,
            createdAt = createdAt.toString(),
            fromDate = from.toString(),
            toDate = to.toString(),
            products = products,
            clients = clients,
            points = points,
            amount = amount
        )

    private fun ReportExportEntity.toModel(): ReportExport =
        ReportExport(
            id = id,
            scope = scope,
            ownerId = ownerId,
            officeId = officeId,
            kind = kind,
            fileName = fileName,
            createdAt = LocalDate.parse(createdAt),
            from = LocalDate.parse(fromDate),
            to = LocalDate.parse(toDate),
            products = products,
            clients = clients,
            points = points,
            amount = amount
        )
}
