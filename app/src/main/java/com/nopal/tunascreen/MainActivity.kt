package com.nopal.tunascreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.rememberNavController
import com.nopal.tunascreen.core.auth.TokenStore
import com.nopal.tunascreen.navigation.TunaDestinations
import com.nopal.tunascreen.navigation.TunaNavGraph
import com.nopal.tunascreen.ui.theme.TunaScreenTheme
import com.nopal.tunascreen.ui.theme.AppearanceStore
import com.nopal.tunascreen.ui.theme.TunaThemeMode
import com.nopal.tunascreen.ui.theme.DisplayModeOverride
import com.nopal.tunascreen.ui.theme.ProvideDisplayProfile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var appearanceStore: AppearanceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()
        setContent {
            val themeMode by appearanceStore.themeMode.collectAsState(initial = TunaThemeMode.NOPAL)
            val displayMode by appearanceStore.displayMode.collectAsState(initial = DisplayModeOverride.AUTO)
            ProvideDisplayProfile(override = displayMode) {
                TunaScreenTheme(themeMode = themeMode) {
                val session by tokenStore.session.collectAsState(initial = null)
                var showIntro by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(INTRO_DURATION_MS)
                    showIntro = false
                }

                if (showIntro) {
                    AppIntroScreen()
                } else {
                    Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    // null = todavía no llegó la primera lectura de DataStore --
                    // se espera antes de decidir la pantalla inicial (Discovery
                    // vs. Taller) para no arrancar siempre en Discovery y
                    // "flashear" a Taller un instante después en cada apertura.
                    when (val current = session) {
                        null -> Box(modifier = androidx.compose.ui.Modifier.fillMaxSize())
                        else -> {
                            val navController = rememberNavController()
                            val start = if (current.isPaired) TunaDestinations.WORKSHOP else TunaDestinations.DISCOVERY
                            TunaNavGraph(navController = navController, startDestination = start)
                        }
                    }
                    }
                }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        private const val INTRO_DURATION_MS = 1_800L
    }
}

@androidx.compose.runtime.Composable
private fun AppIntroScreen() {
    Surface(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        color = Color(0xFF070707)
    ) {
        Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.nopal_logo),
                contentDescription = "NOPAL",
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(0.5f),
                contentScale = ContentScale.Fit
            )
        }
    }
}
