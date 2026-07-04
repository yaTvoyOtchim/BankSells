package com.example.vtbsales.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SalesDao {
    @Query("SELECT COUNT(*) FROM users")
    suspend fun userCount(): Int

    @Query("SELECT COUNT(*) FROM products")
    suspend fun productCount(): Int

    @Query("SELECT * FROM offices ORDER BY title")
    suspend fun offices(): List<OfficeEntity>

    @Query("SELECT * FROM users ORDER BY name")
    suspend fun users(): List<UserEntity>

    @Query("SELECT * FROM team_links ORDER BY managerId, employeeId")
    suspend fun teamLinks(): List<TeamLinkEntity>

    @Query("SELECT * FROM plans ORDER BY office")
    suspend fun plans(): List<PlanEntity>

    @Query("SELECT * FROM client_sessions ORDER BY date, userId, phoneLast4, sequence")
    suspend fun clientSessions(): List<ClientSessionEntity>

    @Query("SELECT * FROM sales ORDER BY date, id")
    suspend fun sales(): List<SaleEntity>

    @Query("SELECT * FROM sale_edit_history ORDER BY editedAt DESC, id DESC")
    suspend fun saleHistory(): List<SaleEditHistoryEntity>

    @Query("SELECT * FROM report_exports ORDER BY createdAt DESC, id DESC")
    suspend fun reportExports(): List<ReportExportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOffices(items: List<OfficeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsers(items: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeamLinks(items: List<TeamLinkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlans(items: List<PlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(items: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClientSessions(items: List<ClientSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSales(items: List<SaleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSaleHistory(items: List<SaleEditHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReportExports(items: List<ReportExportEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOffice(item: OfficeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(item: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeamLink(item: TeamLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(item: PlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClientSession(item: ClientSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSale(item: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSaleHistory(item: SaleEditHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReportExport(item: ReportExportEntity)
}
