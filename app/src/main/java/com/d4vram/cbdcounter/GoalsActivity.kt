package com.d4vram.cbdcounter

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

class GoalsActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "goals_prefs"
        const val KEY_CBD_MAX = "goal_cbd_max"
        const val KEY_THC_MAX = "goal_thc_max"
        const val KEY_NOTIFICATIONS = "goal_notifications_enabled"
        const val CHANNEL_ID = "goals_channel"
        const val NOTIF_ID = 1001

        fun notifyIfGoalExceeded(context: Context, cbdCount: Int, thcCount: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_NOTIFICATIONS, false)) return

            val cbdGoal = prefs.getInt(KEY_CBD_MAX, 0)
            val thcGoal = prefs.getInt(KEY_THC_MAX, 0)
            if (cbdGoal == 0 && thcGoal == 0) return

            val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val notifKey = "notified_$today"
            if (prefs.getBoolean(notifKey, false)) return

            val cbdExceeded = cbdGoal > 0 && cbdCount > cbdGoal
            val thcExceeded = thcGoal > 0 && thcCount > thcGoal
            if (!cbdExceeded && !thcExceeded) return

            val msg = buildString {
                if (cbdExceeded) append("CBD: $cbdCount/$cbdGoal ")
                if (thcExceeded) append("THC: $thcCount/$thcGoal")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return

            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Meta superada")
                .setContentText(msg.trim())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
            prefs.edit().putBoolean(notifKey, true).apply()
        }
    }

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private var cbdGoal = 0
    private var thcGoal = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)
        supportActionBar?.title = "Metas Diarias"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        createNotificationChannel()
        loadGoals()
        setupButtons()
        updateStreakDisplay()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadGoals() {
        cbdGoal = prefs.getInt(KEY_CBD_MAX, 0)
        thcGoal = prefs.getInt(KEY_THC_MAX, 0)
        val notifEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, false)
        findViewById<TextView>(R.id.tvCbdGoal).text = cbdGoal.toString()
        findViewById<TextView>(R.id.tvThcGoal).text = thcGoal.toString()
        findViewById<SwitchMaterial>(R.id.switchGoalNotification).isChecked = notifEnabled
    }

    private fun setupButtons() {
        val tvCbd = findViewById<TextView>(R.id.tvCbdGoal)
        val tvThc = findViewById<TextView>(R.id.tvThcGoal)
        val switchNotif = findViewById<SwitchMaterial>(R.id.switchGoalNotification)

        findViewById<MaterialButton>(R.id.btnCbdMinus).setOnClickListener {
            if (cbdGoal > 0) cbdGoal--
            tvCbd.text = cbdGoal.toString()
            saveGoals()
        }
        findViewById<MaterialButton>(R.id.btnCbdPlus).setOnClickListener {
            cbdGoal++
            tvCbd.text = cbdGoal.toString()
            saveGoals()
        }
        findViewById<MaterialButton>(R.id.btnThcMinus).setOnClickListener {
            if (thcGoal > 0) thcGoal--
            tvThc.text = thcGoal.toString()
            saveGoals()
        }
        findViewById<MaterialButton>(R.id.btnThcPlus).setOnClickListener {
            thcGoal++
            tvThc.text = thcGoal.toString()
            saveGoals()
        }
        switchNotif.setOnCheckedChangeListener { _, checked ->
            if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
                }
            }
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, checked).apply()
        }
    }

    private fun saveGoals() {
        prefs.edit()
            .putInt(KEY_CBD_MAX, cbdGoal)
            .putInt(KEY_THC_MAX, thcGoal)
            .apply()
        updateStreakDisplay()
    }

    private fun updateStreakDisplay() {
        val streak = calculateStreak()
        val label = if (streak == 1) "día" else "días"
        findViewById<TextView>(R.id.tvStreak).text = "🔥 $streak $label"
    }

    private fun calculateStreak(): Int {
        if (cbdGoal == 0 && thcGoal == 0) return 0
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        var streak = 0
        repeat(365) {
            val dateKey = fmt.format(cal.time)
            val cbd = Prefs.getCbdCount(this, dateKey)
            val thc = Prefs.getThcCount(this, dateKey)
            val cbdOk = cbdGoal == 0 || cbd <= cbdGoal
            val thcOk = thcGoal == 0 || thc <= thcGoal
            if (cbdOk && thcOk) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                return streak
            }
        }
        return streak
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Metas de consumo",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Alertas al superar meta diaria" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
