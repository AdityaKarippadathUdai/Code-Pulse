package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.db.CodePulseDatabase
import com.example.data.pref.CodePulsePrefs
import com.example.data.repository.CodePulseRepository
import com.example.ui.screens.MainLayout
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core singletons setup
        val prefs = CodePulsePrefs(applicationContext)
        val database = CodePulseDatabase.getDatabase(applicationContext)
        val repository = CodePulseRepository(applicationContext, database, prefs)

        setContent {
            val themeMode by prefs.themeMode.collectAsState()

            val isDarkThemeActive = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkThemeActive) {
                // Smooth background fade animation when switching dark/light themes
                val animatedBgColor by animateColorAsState(
                    targetValue = MaterialTheme.colorScheme.background,
                    animationSpec = tween(durationMillis = 400),
                    label = "ThemeBgAnim"
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = animatedBgColor
                ) {
                    MainLayout(
                        repository = repository,
                        prefs = prefs,
                        themeMode = themeMode,
                        onThemeChange = { newMode ->
                            prefs.setThemeMode(newMode)
                        }
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
