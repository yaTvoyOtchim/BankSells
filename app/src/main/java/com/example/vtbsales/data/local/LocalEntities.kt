package com.example.vtbsales.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "offices")
data class OfficeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val city: String,
    val region: String
)

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["uid"], unique = true),
        Index(value = ["pinHashDemo"]),
        Index(value = ["officeId"])
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val name: String,
    val role: String,
    val office: String,
    val pinHashDemo: String,
    val createdAt: String,
    val officeId: String
)

@Entity(tableName = "team_links", primaryKeys = ["managerId", "employeeId"])
data class TeamLinkEntity(
    val managerId: String,
    val employeeId: String,
    val office: String
)

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey val office: String,
    val month: String,
    val pointsTarget: Double,
    val clientsTarget: Int
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val type: String,
    val title: String
)

@Entity(
    tableName = "client_sessions",
    indices = [
        Index(value = ["userId", "date", "phoneLast4"]),
        Index(value = ["userId"])
    ]
)
data class ClientSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val phoneLast4: String,
    val sequence: Int,
    val date: String
)

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["userId", "date"]),
        Index(value = ["clientSessionId"])
    ]
)
data class SaleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val productType: String,
    val format: String,
    val count: Int,
    val amount: Double,
    val points: Double,
    val clients: Int,
    val date: String,
    val clientSessionId: String?,
    val status: String
)

@Entity(
    tableName = "sale_edit_history",
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["userId"])
    ]
)
data class SaleEditHistoryEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val userId: String,
    val editorId: String,
    val editedAt: String,
    val reason: String,
    val beforeProductType: String,
    val beforeFormat: String,
    val beforeCount: Int,
    val beforeAmount: Double,
    val beforePoints: Double,
    val afterProductType: String,
    val afterFormat: String,
    val afterCount: Int,
    val afterAmount: Double,
    val afterPoints: Double
)

@Entity(
    tableName = "report_exports",
    indices = [
        Index(value = ["scope"]),
        Index(value = ["ownerId"]),
        Index(value = ["officeId"])
    ]
)
data class ReportExportEntity(
    @PrimaryKey val id: String,
    val scope: String,
    val ownerId: String?,
    val officeId: String?,
    val kind: String,
    val fileName: String,
    val createdAt: String,
    val fromDate: String,
    val toDate: String,
    val products: Int,
    val clients: Int,
    val points: Double,
    val amount: Double
)
