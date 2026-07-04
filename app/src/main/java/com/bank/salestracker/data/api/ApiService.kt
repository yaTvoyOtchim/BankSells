package com.bank.salestracker.data.api

import com.bank.salestracker.data.model.*
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): TokenResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)

    @POST("auth/logout")
    suspend fun logout()

    @POST("sales")
    suspend fun addSale(@Body sale: Sale): Sale

    @GET("sales/my")
    suspend fun mySales(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): List<Sale>

    @DELETE("sales/{id}")
    suspend fun deleteSale(@Path("id") id: String)

    @GET("reports/my")
    suspend fun myReport(): MyReport

    @GET("admin/report")
    suspend fun adminReport(@Query("period") period: String = "month"): AdminReport

    @GET("admin/employee/{id}/sales")
    suspend fun employeeSales(
        @Path("id") employeeId: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): List<Sale>

    @POST("admin/employee/{id}/goal")
    suspend fun setGoal(@Path("id") employeeId: String, @Query("goal") goal: Int)

    @GET("admin/export")
    suspend fun exportCsv(@Query("period") period: String): String

    @GET("org/units")
    suspend fun orgUnits(): List<OrgUnit>

    @GET("management/pending-users")
    suspend fun pendingUsers(): List<User>

    @GET("management/users/{employeeId}")
    suspend fun managementUser(@Path("employeeId") employeeId: String): User

    @POST("management/users/{employeeId}/assign")
    suspend fun assignUser(
        @Path("employeeId") employeeId: String,
        @Body body: AssignmentRequest
    ): User
}
