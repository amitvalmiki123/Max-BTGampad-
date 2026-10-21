package com.gamepadpro.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gamepadpro.app.hid.GamepadState
import com.gamepadpro.app.hid.HidGamepad
import com.gamepadpro.app.layout.LayoutRepository
import com.gamepadpro.app.layout.SettingsRepository
import com.gamepadpro.app.nav.AppNav
import com.gamepadpro.app.ui.theme.GamePadProTheme

class MainActivity : ComponentActivity() {

    private lateinit var hid: HidGamepad
    private lateinit var gamepadState: GamepadState
    private lateinit var layoutRepo: LayoutRepository
    private lateinit var settingsRepo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full-screen landscape, screen stays on while gaming
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applyImmersive()

        hid = HidGamepad(applicationContext)
        gamepadState = GamepadState()
        layoutRepo = LayoutRepository(applicationContext)
        settingsRepo = SettingsRepository(applicationContext)

        setContent {
            GamePadProTheme {
                AppNav(
                    hid = hid,
                    gamepadState = gamepadState,
                    layoutRepo = layoutRepo,
                    settingsRepo = settingsRepo
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersive()
    }

    override fun onPause() {
        super.onPause()
        // Never leave a button stuck on the PC when the app goes to background
        if (::hid.isInitialized) {
            gamepadState.reset()
            hid.sendNeutral()
        }
    }

    override fun onDestroy() {
        if (::hid.isInitialized) hid.close()
        super.onDestroy()
    }

    private fun applyImmersive() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
