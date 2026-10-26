package com.d4vram.fumitracking

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class CBDWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_ADD_CBD = "com.d4vram.fumitracking.ADD_CBD"
        const val ACTION_ADD_WEED = "com.d4vram.fumitracking.ADD_WEED"
        const val ACTION_ADD_POLEM = "com.d4vram.fumitracking.ADD_POLEM"
        private const val ACTION_SCHEDULED_REFRESH = "com.d4vram.fumitracking.SCHEDULED_REFRESH"
        private const val MIDNIGHT_REQUEST_CODE = 420

        // Claves compartidas con Prefs.kt si fuera necesario, o centralizadas aquí
        private const val PREFS_NAME = "CBDCounter"
        private const val KEY_COUNT_PREFIX = "count_"

        // Método estático para actualizar widgets desde MainActivity y desde los botones.
        // Pinta directamente con AppWidgetManager (sin mandarse un broadcast a sí mismo).
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CBDWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) {
                cancelMidnightUpdate(context)
                return
            }
            for (id in appWidgetIds) render(context, appWidgetManager, id)
            scheduleMidnightUpdate(context)
        }

        private fun render(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val today = currentDateKey()
            val cbdCount = Prefs.getCbdCount(context, today)
            val thcCount = Prefs.getThcCount(context, today)
            val totalCount = cbdCount + thcCount
            val isThc = Prefs.getSubstanceType(context) == "THC"

            // Ancho real del widget en dp; si el launcher no lo informa (0), layout compacto.
            val widthDp = appWidgetManager.getAppWidgetOptions(appWidgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val wide = WidgetLayout.isWide(widthDp)
            val views = RemoteViews(
                context.packageName,
                if (wide) R.layout.cbd_widget_wide else R.layout.cbd_widget
            )

            // Metas máximas; 0 = sin meta (RingProgress nunca divide entre ella).
            val goals = context.getSharedPreferences(GoalsActivity.PREFS_NAME, Context.MODE_PRIVATE)
            views.setImageViewBitmap(
                R.id.widget_rings,
                WidgetRingsRenderer.render(
                    context, cbdCount, thcCount,
                    goals.getInt(GoalsActivity.KEY_CBD_MAX, 0),
                    goals.getInt(GoalsActivity.KEY_THC_MAX, 0)
                )
            )
            views.setTextViewText(R.id.widget_counter, totalCount.toString())
            views.setTextViewText(R.id.widget_date, SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date()))
            views.setTextViewText(R.id.widget_emoji, EmojiUtils.emojiForCount(totalCount, context))
            // La etiqueta del +1 dice a qué sustancia suma (modo activo).
            views.setTextViewText(
                R.id.widget_add_button,
                context.getString(if (isThc) R.string.widget_add_thc else R.string.widget_add_cbd)
            )
            if (wide) views.setTextViewText(R.id.widget_sub_counts, "CBD $cbdCount · THC $thcCount")

            views.setOnClickPendingIntent(R.id.widget_add_button, actionIntent(context, ACTION_ADD_CBD, 0))
            views.setOnClickPendingIntent(R.id.widget_btn_weed, actionIntent(context, ACTION_ADD_WEED, 2))
            views.setOnClickPendingIntent(R.id.widget_btn_polem, actionIntent(context, ACTION_ADD_POLEM, 3))
            views.setOnClickPendingIntent(
                R.id.widget_container,
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context, requestCode,
                Intent(context, CBDWidgetActionReceiver::class.java).apply { this.action = action },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        private fun currentDateKey(): String =
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        private fun scheduleMidnightUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                MIDNIGHT_REQUEST_CODE,
                Intent(context, CBDWidgetProvider::class.java).apply { action = ACTION_SCHEDULED_REFRESH },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAt = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 5)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()

            if (canUseExact) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } catch (security: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            } else {
                // Sin permiso para alarmas exactas en Android 12+, usamos una alarma inexacta.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }

        private fun cancelMidnightUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                MIDNIGHT_REQUEST_CODE,
                Intent(context, CBDWidgetProvider::class.java).apply { action = ACTION_SCHEDULED_REFRESH },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) render(context, appWidgetManager, appWidgetId)
        scheduleMidnightUpdate(context)
    }

    // Al redimensionar el widget cambia el ancho: puede tocar pasar de 2×2 a 4×2 o al revés.
    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle
    ) {
        render(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        scheduleMidnightUpdate(context)
    }

    override fun onDisabled(context: Context) {
        cancelMidnightUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        runCatching {
            when (intent.action) {
                // APPWIDGET_UPDATE ya lo despacha super.onReceive hacia onUpdate: no repetirlo aquí.
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_BOOT_COMPLETED,
                ACTION_SCHEDULED_REFRESH -> {
                    updateAllWidgets(context)
                }
            }
        }.onFailure { e ->
            // Log error or show a Toast if critical, but prevent crash
            e.printStackTrace()
        }
    }
}

class CBDWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            when (intent.action) {
                CBDWidgetProvider.ACTION_ADD_CBD -> WidgetActionHandler.addStandardCBD(context)
                CBDWidgetProvider.ACTION_ADD_WEED -> WidgetActionHandler.addWeed(context)
                CBDWidgetProvider.ACTION_ADD_POLEM -> WidgetActionHandler.addPolem(context)
                else -> return
            }
            CBDWidgetProvider.updateAllWidgets(context)
        }.onFailure { e ->
            e.printStackTrace()
        }
    }
}

private object WidgetActionHandler {
    fun addStandardCBD(context: Context) {
        val today = currentDateKey()
        val isThc = Prefs.getSubstanceType(context) == "THC"
        val entry = if (isThc) "${IntakeController.ICON_THC} ${currentTimestamp()}" else "${IntakeController.ICON_CBD} ${currentTimestamp()}"
        IntakeController.registerActive(context, today, entry)
    }

    fun addWeed(context: Context) {
        // Weed siempre suma a THC, independiente del modo activo
        val today = currentDateKey()
        val entry = "${IntakeController.ICON_WEED} ${currentTimestamp()} (aliñado con weed)"
        IntakeController.registerThc(context, today, entry)
    }

    fun addPolem(context: Context) {
        // Polen siempre suma a THC, independiente del modo activo
        val today = currentDateKey()
        val entry = "${IntakeController.ICON_POLEN} ${currentTimestamp()} (aliñado con polen)"
        IntakeController.registerThc(context, today, entry)
    }

    private fun currentDateKey(): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    private fun currentTimestamp(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
