package com.bank.salestracker.ui.navigation

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesRefreshSignalTest {
    @Test
    fun markChangedWritesNewTimestampIntoSavedState() {
        val handle = SavedStateHandle()

        SalesRefreshSignal.markChanged(handle) { 42L }

        assertEquals(42L, handle[SalesRefreshSignal.KEY])
    }

    @Test
    fun dashboardRefreshesOnlyAfterRealSignal() {
        assertTrue(!SalesRefreshSignal.shouldRefresh(0L))
        assertTrue(SalesRefreshSignal.shouldRefresh(42L))
    }
}
