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

fun Role.canCreateSales(): Boolean = this == Role.EMPLOYEE

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
data class ProductSetting(
    val id: String,
    val productId: String,
    val code: String,
    val title: String,
    val groupName: String = "",
    val active: Boolean = true,
    val points: Double = 1.0,
    val requiresAmount: Boolean = false,
    val countsTowardPlan: Boolean = true,
    val sortOrder: Int = 0
)

@Serializable
data class ProductSettingPatch(
    val active: Boolean? = null,
    val points: Double? = null,
    val requiresAmount: Boolean? = null,
    val countsTowardPlan: Boolean? = null,
    val sortOrder: Int? = null
)

@Serializable
data class ProductCreateRequest(
    val code: String,
    val title: String,
    val groupName: String = "",
    val description: String? = null
)

@Serializable
data class SaleBatchItem(
    val productId: String? = null,
    val category: ProductCategory? = null,
    val amount: Double? = null,
    val quantity: Int = 1,
    val comment: String? = null
)

@Serializable
data class SaleBatchRequest(
    val clientLast4: String,
    val items: List<SaleBatchItem>
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

fun productTitleForCode(code: String): String =
    ProductCategory.entries.firstOrNull { it.name == code }?.title ?: code

@Serializable
data class Sale(
    val id: String? = null,
    val category: String,
    val productId: String? = null,
    val productTitle: String? = null,
    val productGroup: String? = null,
    val points: Double = 0.0,
    val pointsTotal: Double = 0.0,
    val requiresAmount: Boolean = false,
    val countsTowardPlan: Boolean = true,
    val clientLast4: String,
    val saleGroupId: String? = null,
    val amount: Double? = null,
    val quantity: Int = 1,
    val comment: String? = null,
    val createdAt: String? = null,
    val employeeId: String? = null,
    val employeeName: String? = null
)

fun Sale.displayTitle(): String = productTitle ?: productTitleForCode(category)

@Serializable
data class CategoryStat(
    val category: String,
    val productTitle: String? = null,
    val count: Int,
    val totalAmount: Double,
    val totalPoints: Double = 0.0
)

fun CategoryStat.displayTitle(): String = productTitle ?: productTitleForCode(category)

@Serializable
data class DayPoint(val date: String, val count: Int)

@Serializable
data class MyReport(
    val todayCount: Int,
    val todayAmount: Double,
    val todayPoints: Double = 0.0,
    val monthCount: Int,
    val monthAmount: Double,
    val monthPoints: Double = 0.0,
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
    val monthPoints: Double = 0.0,
    val goalProgress: Float? = null
)

@Serializable
data class AdminReport(
    val branchTodayCount: Int,
    val branchTodayPoints: Double = 0.0,
    val branchMonthCount: Int,
    val branchMonthAmount: Double,
    val branchMonthPoints: Double = 0.0,
    val employees: List<EmployeeSummary>,
    val byCategory: List<CategoryStat>
)

@Serializable
data class ApiError(val message: String)
