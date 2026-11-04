package org.kinera.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.kinera.app.data.settings.KineraSettings
import org.kinera.app.data.settings.SettingsRepository
import org.kinera.app.ui.navigation.KineraApp
import org.kinera.app.ui.theme.KineraTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings
                .collectAsState(initial = KineraSettings())
            KineraTheme(
                themeMode = settings.themeMode,
                dynamicColors = settings.dynamicColors,
            ) {
                KineraApp()
            }
        }
    }
}
