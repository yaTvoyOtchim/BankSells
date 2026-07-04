package com.bank.salestracker.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val employeeId: String,
    val password: String,
    val deviceId: String
)

@Serializable
data class RegisterRequest(
    val employeeId: String,
    val fullName: String,
    val password: String,
    val deviceId: String
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val mustChangePassword: Boolean = false,
    val user: User
)

@Serializable
data class RefreshRequest(val refreshToken: String, val deviceId: String)

@Serializable
data class ChangePasswordRequest(val oldPassword: String, val newPassword: String)

@Serializable
data class User(
    val id: String,
    val employeeId: String,
    val fullName: String,
    val branch: String = "",
    val role: Role,
    val registrationStatus: RegistrationStatus = RegistrationStatus.ACTIVE,
    val orgUnitId: String? = null
)

@Serializable
enum class RegistrationStatus { PENDING_ASSIGNMENT, ACTIVE, BLOCKED, DEACTIVATED }

@Serializable
enum class Role {
    EMPLOYEE,
    TEAM_LEAD,
    OFFICE_MANAGER,
    REGIONAL_MANAGER,
    DIRECTOR,
    ADMIN
}

fun Role.isManager(): Boolean = this in setOf(
    Role.TEAM_LEAD,
    Role.OFFICE_MANAGER,
    Role.REGIONAL_MANAGER,
    Role.DIRECTOR,
    Role.ADMIN
)

@Serializable
data class OrgUnit(
    val id: String,
    val name: String,
    val type: String,
    val parentId: String? = null,
    val code: String? = null
)

@Serializable
data class AssignmentRequest(
    val orgUnitId: String? = null,
    val role: String = "EMPLOYEE",
    val comment: String? = null
)

@Serializable
enum class ProductCategory(val title: String) {
    DEBIT_CARD_STICKER_APPLICATION("ДК/стик(по заявке)"),
    CREDIT_CARD_SALE("КК(продажа)"),
    CREDIT_CARD_APPLICATION("КК(по заявке)"),
    PDS("ПДС"),
    CREDIT_CARD_INSURANCE("Страховка КК"),
    SOM("СОМ"),
    KSP("КСП"),
    STICKER("СТИК"),
    PENSION("Пенсия"),
    SALARY_PROJECT("ИЗП"),
    CASH_LOAN_APPLICATION("КН Заявка"),
    CASH_LOAN_SALE("КН продажа"),
    SUBSCRIPTION("Подписка"),
    CARD_PLUS("Карта+"),
    SOCIAL_PAYOUTS("Соц. Выплаты"),
    MASS_ISSUE("Масс. выдача"),
    OPIF("ОПИФ"),
    AUTO_PAYMENTS("Автоплатежи"),
    SAVINGS_ACCOUNT("Накопительный счет"),
    AUTO_PULLING("Автостягивание"),
    FAMILY_BANK("Сем. Банк"),
    SALARY_CARD_ISSUE("Выдача ЗП карт"),
    DEBIT_CARD_ADDITIONAL("ДК(доп.карта)"),
    PRIVILEGE("Привилегия"),
    SALARY_LIGHT("ЗП лайт")
}

@Serializable
data class Sale(
    val id: String? = null,
    val category: ProductCategory,
    val clientLast4: String,
    val amount: Double? = null,
    val quantity: Int = 1,
    val comment: String? = null,
    val createdAt: String? = null,
    val employeeId: String? = null,
    val employeeName: String? = null
)

@Serializable
data class CategoryStat(
    val category: ProductCategory,
    val count: Int,
    val totalAmount: Double
)

@Serializable
data class DayPoint(val date: String, val count: Int)

@Serializable
data class MyReport(
    val todayCount: Int,
    val todayAmount: Double,
    val monthCount: Int,
    val monthAmount: Double,
    val totalCount: Int,
    val byCategory: List<CategoryStat>,
    val last14Days: List<DayPoint>,
    val monthlyGoal: Int? = null
)

@Serializable
data class EmployeeSummary(
    val user: User,
    val todayCount: Int,
    val monthCount: Int,
    val monthAmount: Double,
    val goalProgress: Float? = null
)

@Serializable
data class AdminReport(
    val branchTodayCount: Int,
    val branchMonthCount: Int,
    val branchMonthAmount: Double,
    val employees: List<EmployeeSummary>,
    val byCategory: List<CategoryStat>
)

@Serializable
data class ApiError(val message: String)
