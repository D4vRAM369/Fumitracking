package com.d4vram.cbdcounter

import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Utilidades para manejar edge-to-edge de forma consistente en toda la app
 */
object EdgeToEdgeUtils {

    /**
     * Habilita edge-to-edge en una Activity.
     * Usa esto cuando el layout ya tiene fitsSystemWindows="true" en el AppBarLayout/Toolbar.
     */
    fun enableEdgeToEdge(activity: AppCompatActivity, lightStatusBar: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.setDecorFitsSystemWindows(false)
            activity.window.insetsController?.setSystemBarsAppearance(
                if (lightStatusBar) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }

    /**
     * Configura edge-to-edge en una Activity aplicando padding programáticamente.
     * Usa esto cuando el layout NO tiene fitsSystemWindows.
     *
     * @param activity La activity a configurar
     * @param rootView La vista raíz del layout
     * @param topView La vista que debe recibir padding top (AppBarLayout o Toolbar)
     * @param lightStatusBar true para iconos oscuros en status bar
     */
    fun setup(
        activity: AppCompatActivity,
        rootView: View,
        topView: View,
        lightStatusBar: Boolean = false
    ) {
        enableEdgeToEdge(activity, lightStatusBar)

        // Aplicar insets al topView
        ViewCompat.setOnApplyWindowInsetsListener(topView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                insets.top,
                view.paddingRight,
                view.paddingBottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }
}
