// CBDWidgetProvider.kt - CORREGIDO
package com.d4vram.cbdcounter

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class CBDWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_ADD_CBD = "com.d4vram.cbdcounter.ADD_CBD"
        const val ACTION_ADD_WEED = "com.d4vram.cbdcounter.ADD_WEED"
        const val ACTION_ADD_POLEM = "com.d4vram.cbdcounter.ADD_POLEM"
        const val ACTION_RESET_CBD = "com.d4vram.cbdcounter.RESET_CBD"
        private const val ACTION_SCHEDULED_REFRESH = "com.d4vram.cbdcounter.SCHEDULED_REFRESH"
        private const val MIDNIGHT_REQUEST_CODE = 420

        // Claves compartidas con Prefs.kt si fuera necesario, o centralizadas aquí
        private const val PREFS_NAME = "CBDCounter"
        private const val KEY_COUNT_PREFIX = "count_"

        // Método estático para actualizar widgets desde MainActivity
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, CBDWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CBDWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) {
                cancelMidnightUpdate(context)
                return
            }
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.sendBroadcast(intent)
            scheduleMidnightUpdate(context)
        }

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
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleMidnightUpdate(context)
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
                AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    if (appWidgetIds != null) {
                        onUpdate(context, appWidgetManager, appWidgetIds)
                    }
                }
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

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.cbd_widget)

        // Obtener contadores actuales
        val today = getCurrentDateKey()
        val cbdCount = Prefs.getCbdCount(context, today)
        val thcCount = Prefs.getThcCount(context, today)
        val totalCount = cbdCount + thcCount
        val date = getCurrentDateDisplay()
        val emoji = EmojiUtils.emojiForCount(totalCount, context)
        val isThc = Prefs.getSubstanceType(context) == "THC"

        // Actualizar vistas principales
        views.setTextViewText(R.id.widget_counter, totalCount.toString())
        views.setTextViewText(R.id.widget_date, date)
        views.setTextViewText(R.id.widget_emoji, emoji)

        // Sub-línea CBD · THC (desglose)
        views.setTextViewText(R.id.widget_sub_counts, "CBD $cbdCount · THC $thcCount")

        // Badge modo activo
        views.setTextViewText(R.id.widget_mode_label, if (isThc) "THC" else "CBD")

        // Color del botón +1 según modo: texto púrpura en light (blanco bg), blanco en night (verde bg)
        // No podemos detectar modo noche en RemoteViews; el color del texto se fija para modo claro
        // ya que el bg del botón cambia vía drawable-night automáticamente.
        // Para noche: el bg verde oscuro contrasta mejor con texto blanco.
        val addTextColor = if (isThc) android.graphics.Color.parseColor("#7B1FA2")
                           else android.graphics.Color.parseColor("#6750A4")
        views.setTextColor(R.id.widget_add_button, addTextColor)

        // Configurar botón +1
        val addIntent = Intent(context, CBDWidgetActionReceiver::class.java).apply {
            action = ACTION_ADD_CBD
        }
        val addPendingIntent = PendingIntent.getBroadcast(
            context, 0, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_add_button, addPendingIntent)

        // Configurar botón Weed
        val weedIntent = Intent(context, CBDWidgetActionReceiver::class.java).apply {
            action = ACTION_ADD_WEED
        }
        val weedPendingIntent = PendingIntent.getBroadcast(
            context, 2, weedIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_weed, weedPendingIntent)

        // Configurar botón Polem
        val polemIntent = Intent(context, CBDWidgetActionReceiver::class.java).apply {
            action = ACTION_ADD_POLEM
        }
        val polemPendingIntent = PendingIntent.getBroadcast(
            context, 3, polemIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_polem, polemPendingIntent)

        // Configurar botón reset
        val resetIntent = Intent(context, CBDWidgetActionReceiver::class.java).apply {
            action = ACTION_RESET_CBD
        }
        val resetPendingIntent = PendingIntent.getBroadcast(
            context, 1, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_reset_button, resetPendingIntent)

        // Configurar tap para abrir la app
        val mainIntent = Intent(context, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, mainPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getCurrentDateKey(): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun getCurrentDateDisplay(): String {
        val formatter = SimpleDateFormat("dd/MM", Locale.getDefault())
        return formatter.format(Date())
    }
}

class CBDWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            when (intent.action) {
                CBDWidgetProvider.ACTION_ADD_CBD -> WidgetActionHandler.addStandardCBD(context)
                CBDWidgetProvider.ACTION_ADD_WEED -> WidgetActionHandler.addWeed(context)
                CBDWidgetProvider.ACTION_ADD_POLEM -> WidgetActionHandler.addPolem(context)
                CBDWidgetProvider.ACTION_RESET_CBD -> WidgetActionHandler.resetActiveCounter(context)
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

    fun resetActiveCounter(context: Context) {
        IntakeController.resetActive(context, currentDateKey())
    }

    private fun currentDateKey(): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    private fun currentTimestamp(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
