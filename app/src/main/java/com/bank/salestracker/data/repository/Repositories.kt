package com.bank.salestracker.data.repository

import com.bank.salestracker.data.api.ApiService
import com.bank.salestracker.data.local.PendingSale
import com.bank.salestracker.data.local.PendingSaleDao
import com.bank.salestracker.data.local.TokenStore
import com.bank.salestracker.data.model.*
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.util.UUID

class AuthRepository(private val api: ApiService, private val store: TokenStore) {

    suspend fun login(employeeId: String, password: String): TokenResponse {
        val resp = api.login(LoginRequest(employeeId.trim().lowercase(), password, store.deviceId))
        store.accessToken = resp.accessToken
        store.refreshToken = resp.refreshToken
        store.saveUser(resp.user)
        return resp
    }

    suspend fun register(employeeId: String, fullName: String, password: String): TokenResponse {
        val resp = api.register(RegisterRequest(employeeId.trim().lowercase(), fullName.trim(), password, store.deviceId))
        store.accessToken = resp.accessToken
        store.refreshToken = resp.refreshToken
        store.saveUser(resp.user)
        return resp
    }

    suspend fun changePassword(old: String, new: String) = api.changePassword(ChangePasswordRequest(old, new))

    suspend fun logout() {
        runCatching { api.logout() }
        store.clear()
    }

    fun currentUser(): User? = store.user()
    val isLoggedIn get() = store.isLoggedIn
}

class SalesRepository(
    private val api: ApiService,
    private val pendingDao: PendingSaleDao
) {
    val pendingCount: Flow<Int> = pendingDao.countFlow()

    /**
     * Пытаемся отправить продажу на сервер.
     * При сетевой ошибке кладём в офлайн-очередь — данные не теряются.
     * @return true, если ушло на сервер сразу; false, если в очереди.
     */
    suspend fun addSale(sale: Sale): Boolean = try {
        syncPending()
        api.addSale(sale)
        true
    } catch (e: IOException) {
        pendingDao.insert(
            PendingSale(
                saleGroupId = sale.saleGroupId ?: UUID.randomUUID().toString(),
                category = sale.category.name,
                clientLast4 = sale.clientLast4,
                amount = sale.amount,
                quantity = sale.quantity,
                comment = sale.comment
            )
        )
        false
    }

    suspend fun addSalesBatch(clientLast4: String, items: List<SaleBatchItem>): Boolean = try {
        syncPending()
        api.addSalesBatch(SaleBatchRequest(clientLast4, items))
        true
    } catch (e: IOException) {
        val groupId = UUID.randomUUID().toString()
        for (item in items) {
            pendingDao.insert(
                PendingSale(
                    saleGroupId = groupId,
                    category = item.category.name,
                    clientLast4 = clientLast4,
                    amount = item.amount,
                    quantity = item.quantity,
                    comment = item.comment
                )
            )
        }
        false
    }

    /** Отправляет всё, что накопилось офлайн. */
    suspend fun syncPending() {
        val pending = pendingDao.all()
        val groups = pending.groupBy { it.saleGroupId ?: it.localId.toString() }
        for ((_, group) in groups) {
            val clientLast4 = group.first().clientLast4 ?: "0000"
            api.addSalesBatch(SaleBatchRequest(clientLast4, group.map { it.toBatchItem() }))
            for (p in group) pendingDao.delete(p)
        }
    }

    suspend fun mySales(from: String? = null, to: String? = null) = api.mySales(from, to)
    suspend fun deleteSale(id: String) = api.deleteSale(id)
    suspend fun myReport(): MyReport {
        runCatching { syncPending() }
        return api.myReport()
    }

    suspend fun adminReport(period: String) = api.adminReport(period)
    suspend fun employeeSales(id: String) = api.employeeSales(id)
    suspend fun setGoal(employeeId: String, goal: Int) = api.setGoal(employeeId, goal)
    suspend fun exportCsv(period: String) = api.exportCsv(period)
    suspend fun orgUnits() = api.orgUnits()
    suspend fun pendingUsers() = api.pendingUsers()
    suspend fun managementUser(employeeId: String) = api.managementUser(employeeId.trim().lowercase())
    suspend fun assignUser(employeeId: String, orgUnitId: String? = null) =
        api.assignUser(employeeId.trim().lowercase(), AssignmentRequest(orgUnitId = orgUnitId))
}
