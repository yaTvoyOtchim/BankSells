package com.bank.salestracker.ui.navigation

import androidx.lifecycle.SavedStateHandle

object SalesRefreshSignal {
    const val KEY = "sales_changed_at"

    fun markChanged(handle: SavedStateHandle, nowMillis: () -> Long = System::currentTimeMillis) {
        handle[KEY] = nowMillis()
    }

    fun shouldRefresh(value: Long): Boolean = value > 0L
}
