package com.example.vtbsales.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.vtbsales.MainActivity
import com.example.vtbsales.R
import com.example.vtbsales.data.DemoData
import com.example.vtbsales.data.SalesRepository
import com.example.vtbsales.session.SessionStore
import java.util.Locale

class SalesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, manager, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SalesWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val repository = SalesRepository.local(context, DemoData.seed())
            val userId = SessionStore(context).lastUserId()
            val summary = repository.widgetSummaryForUser(userId)
            val views = RemoteViews(context.packageName, R.layout.widget_sales_summary).apply {
                setTextViewText(R.id.widget_title, "Продажи сегодня")
                setTextViewText(R.id.widget_products, summary.products.toString())
                setTextViewText(R.id.widget_employee, summary.employeeName)
                setTextViewText(R.id.widget_clients, "${summary.clients} клиентов")
                setTextViewText(R.id.widget_points, "${summary.points.format1()} б.")
                setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
            }
            manager.updateAppWidget(appWidgetId, views)
        }

        private fun openAppPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                4200,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun Double.format1(): String =
            String.format(Locale.US, "%.1f", this)
    }
}
