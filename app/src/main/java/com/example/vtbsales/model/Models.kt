package com.example.vtbsales.model

import java.time.LocalDate

enum class Role {
    Employee,
    Manager,
    Admin
}

enum class ProductType(val title: String) {
    CreditCard("Кредитная карта"),
    DebitCard("Дебетовая карта"),
    Loan("Кредит"),
    Deposit("Вклад"),
    Insurance("Страхование"),
    Investment("Инвестиции"),
    SimCard("Сим-карта"),
    SalaryProject("Зарплатный проект"),
    Other("Другой продукт")
}

enum class SaleStatus(val title: String) {
    Saved("Сохранено"),
    Draft("Черновик"),
    NeedsReview("Проверить")
}

enum class AppScreen {
    Welcome,
    Register,
    UidCreated,
    PinLogin,
    EmployeeHome,
    AddSale,
    EmployeeReports,
    EmployeeStats,
    EmployeeRanking,
    EmployeeProfile,
    ManagerHome,
    ManagerTeam,
    ManagerEmployeeCard,
    ManagerPlans,
    ManagerProfile,
    AdminHome
}

data class User(
    val id: String,
    val uid: String,
    val name: String,
    val role: Role,
    val office: String,
    val pinHashDemo: String,
    val createdAt: LocalDate,
    val officeId: String = office
)

data class Office(
    val id: String,
    val title: String,
    val city: String,
    val region: String
)

data class OfficeSummary(
    val office: Office,
    val employees: Int,
    val dailyPoints: Double,
    val monthPoints: Double,
    val attention: Int
)

data class Sale(
    val id: String,
    val userId: String,
    val productType: ProductType,
    val format: String,
    val count: Int,
    val amount: Double,
    val points: Double,
    val clients: Int,
    val date: LocalDate,
    val clientSessionId: String? = null,
    val status: SaleStatus = SaleStatus.Saved
)

data class SaleSnapshot(
    val productType: ProductType,
    val format: String,
    val count: Int,
    val amount: Double,
    val points: Double
)

data class SaleEditHistory(
    val id: String,
    val saleId: String,
    val userId: String,
    val editorId: String,
    val editedAt: LocalDate,
    val reason: String,
    val before: SaleSnapshot,
    val after: SaleSnapshot
)

data class ClientSession(
    val id: String,
    val userId: String,
    val phoneLast4: String,
    val sequence: Int,
    val date: LocalDate
)

data class TeamLink(
    val managerId: String,
    val employeeId: String,
    val office: String
)

data class Plan(
    val office: String,
    val month: String,
    val pointsTarget: Double,
    val clientsTarget: Int
)

data class SalesSeed(
    val users: List<User>,
    val sales: List<Sale>,
    val teamLinks: List<TeamLink>,
    val plans: List<Plan>,
    val clientSessions: List<ClientSession> = emptyList(),
    val offices: List<Office> = emptyList(),
    val saleHistory: List<SaleEditHistory> = emptyList(),
    val reportExports: List<ReportExport> = emptyList()
)

data class ReportSummary(
    val user: User,
    val from: LocalDate,
    val to: LocalDate,
    val products: Int,
    val clients: Int,
    val points: Double,
    val amount: Double,
    val productBreakdown: Map<ProductType, Int>,
    val otherProducts: Map<String, Int>
) {
    val productsPerClient: Double =
        if (clients == 0) 0.0 else products.toDouble() / clients

    val pointsPerClient: Double =
        if (clients == 0) 0.0 else points / clients
}

data class ClientSessionDetail(
    val session: ClientSession,
    val sales: List<Sale>
)

data class EmployeeDetail(
    val user: User,
    val dailyReport: ReportSummary,
    val monthReport: ReportSummary,
    val clientCards: List<ClientSessionDetail>,
    val editHistory: List<SaleEditHistory>
)

data class TeamMemberSummary(
    val user: User,
    val dailyReport: ReportSummary,
    val monthReport: ReportSummary,
    val rank: Int
)

data class ReportExport(
    val id: String,
    val scope: String,
    val ownerId: String?,
    val officeId: String?,
    val kind: String,
    val fileName: String,
    val createdAt: LocalDate,
    val from: LocalDate,
    val to: LocalDate,
    val products: Int,
    val clients: Int,
    val points: Double,
    val amount: Double
)
