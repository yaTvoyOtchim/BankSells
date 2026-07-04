package com.example.vtbsales

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.vtbsales.data.DemoData
import com.example.vtbsales.data.SalesRepository
import com.example.vtbsales.model.ProductType
import com.example.vtbsales.notifications.ReminderSchedule
import com.example.vtbsales.session.SessionStore
import com.example.vtbsales.ui.VtbAppState
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EngagementFeatureTest {
    @Test
    fun reminderScheduleUsesTodayAt1730WhenCurrentTimeIsEarlier() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.of(LocalDate.of(2026, 7, 4), LocalTime.of(12, 0), zone)

        val next = ReminderSchedule.nextTriggerAfter(now)

        assertEquals(LocalDate.of(2026, 7, 4), next.toLocalDate())
        assertEquals(LocalTime.of(17, 30), next.toLocalTime())
    }

    @Test
    fun reminderScheduleUsesTomorrowAt1730WhenCurrentTimeAlreadyPassed() {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.of(LocalDate.of(2026, 7, 4), LocalTime.of(18, 1), zone)

        val next = ReminderSchedule.nextTriggerAfter(now)

        assertEquals(LocalDate.of(2026, 7, 5), next.toLocalDate())
        assertEquals(LocalTime.of(17, 30), next.toLocalTime())
    }

    @Test
    fun widgetSummaryUsesSelectedEmployeeDailyReport() {
        val repo = SalesRepository(DemoData.seed())
        val employee = repo.registerEmployee("Widget User", "office-8617-0290", "8642")
        val client = repo.createClientSession(employee.id, "2468")
        repo.addProductToClientSession(client.id, ProductType.CreditCard, "КК", 2, 0.0, 60.0)
        repo.addProductToClientSession(client.id, ProductType.Deposit, "Вклад", 1, 100000.0, 30.0)

        val summary = repo.widgetSummaryForUser(employee.id)

        assertEquals(employee.name, summary.employeeName)
        assertEquals(3, summary.products)
        assertEquals(1, summary.clients)
        assertEquals(90.0, summary.points, 0.01)
    }

    @Test
    fun loginStoresLastEmployeeForPersonalWidget() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SessionStore(context, "engagement-feature-test")
        store.clear()
        val state = VtbAppState(
            repository = SalesRepository(DemoData.seed()),
            appContext = context,
            sessionStore = store
        )

        assertTrue(state.loginWithPin("1111"))

        assertEquals(state.currentUser!!.id, store.lastUserId())
    }
}
