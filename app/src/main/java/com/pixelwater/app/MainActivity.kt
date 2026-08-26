package com.pixelwater.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelwater.app.ui.WaterTrackerScreen
import com.pixelwater.app.ui.WaterViewModel
import com.pixelwater.app.ui.theme.MyApplicationTheme

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager

class MainActivity : ComponentActivity() {
    private val viewModel: WaterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("last_crash_trace").apply()
        val lastCrash = null
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val trace = android.util.Log.getStackTraceString(throwable)
            prefs.edit().putString("last_crash_trace", trace).commit()
            defaultHandler?.uncaughtException(thread, throwable)
        }

        setHighRefreshRate()
        enableEdgeToEdge()
        setContent {
            val crashState = androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(prefs.getString("last_crash_trace", null))
            }

            if (crashState.value != null) {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "App Crash Log",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                            )
                            
                            Text(
                                text = "The application recorded a crash trace from a previous compile/runtime event. You can bypass this screen to load the updated application preview.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .background(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = crashState.value ?: "",
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { 
                                        prefs.edit().remove("last_crash_trace").apply()
                                        crashState.value = null
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Ignore & Relaunch", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { 
                                        prefs.edit().remove("last_crash_trace").apply()
                                        try {
                                            getSharedPreferences("water_tracker_preferences", Context.MODE_PRIVATE).edit().clear().apply()
                                            getSharedPreferences("crash_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                                        } catch (e: Exception) {}
                                        finish()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                ) {
                                    Text("Force Reset", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                val themeType by viewModel.appTheme.collectAsState()
                val oledModeEnabled by viewModel.oledModeEnabled.collectAsState()
                val isFrostedGlassEnabled by viewModel.isFrostedGlassEnabled.collectAsState()
                val paletteIndex by viewModel.appThemePaletteIndex.collectAsState()
                val wallpaperColors by viewModel.wallpaperThemeColors.collectAsState()
                val staticThemeSeed by viewModel.staticThemeSeed.collectAsState()
                val monochromeEnabled by viewModel.monochromeEnabled.collectAsState()
                val monochromeTarget by viewModel.monochromeColorTarget.collectAsState()
                val themeMode by viewModel.themeMode.collectAsState()
                val autoThemeScheduleEnabled by viewModel.autoThemeScheduleEnabled.collectAsState()
                val autoThemeScheduleMode by viewModel.autoThemeScheduleMode.collectAsState()
                val autoThemeLightStartHour by viewModel.autoThemeLightStartHour.collectAsState()
                val autoThemeLightStartMin by viewModel.autoThemeLightStartMin.collectAsState()
                val autoThemeDarkStartHour by viewModel.autoThemeDarkStartHour.collectAsState()
                val autoThemeDarkStartMin by viewModel.autoThemeDarkStartMin.collectAsState()
                
                val textContrastMode by viewModel.textContrastMode.collectAsState()
                val fontSizeMode by viewModel.fontSizeMode.collectAsState()
                val textFontMode by viewModel.textFontMode.collectAsState()
                val colorContrastMode by viewModel.colorContrastMode.collectAsState()
                
                val lightModeDarkTextEnabled by viewModel.lightModeDarkTextEnabled.collectAsState()
                val devFontScale by viewModel.devFontScale.collectAsState()
                val devFontWeight by viewModel.devFontWeight.collectAsState()
                val devSmallestWidth by viewModel.devSmallestWidth.collectAsState()
                
                val isDark = when (themeMode) {
                    "LIGHT" -> false
                    "DARK" -> true
                    else -> {
                        if (autoThemeScheduleEnabled) {
                            viewModel.isDarkBySchedule(
                                mode = autoThemeScheduleMode,
                                lightStartHour = autoThemeLightStartHour,
                                lightStartMin = autoThemeLightStartMin,
                                darkStartHour = autoThemeDarkStartHour,
                                darkStartMin = autoThemeDarkStartMin
                            )
                        } else {
                            androidx.compose.foundation.isSystemInDarkTheme()
                        }
                    }
                }
                
                MyApplicationTheme(
                    darkTheme = isDark,
                    themeType = themeType,
                    oledModeEnabled = oledModeEnabled,
                    isFrostedGlassEnabled = isFrostedGlassEnabled,
                    paletteIndex = paletteIndex,
                    wallpaperColors = wallpaperColors,
                    staticThemeSeed = staticThemeSeed,
                    monochromeEnabled = monochromeEnabled,
                    monochromeTarget = monochromeTarget,
                    textContrastMode = textContrastMode,
                    fontSizeMode = fontSizeMode,
                    textFontMode = textFontMode,
                    colorContrastMode = colorContrastMode,
                    lightModeDarkTextEnabled = lightModeDarkTextEnabled,
                    devFontScale = devFontScale,
                    devFontWeight = devFontWeight,
                    devSmallestWidth = devSmallestWidth
                ) {
                    WaterTrackerScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
        intent?.data?.let { uri ->
            handleEmailLink(uri.toString())
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
            handleEmailLink(uri.toString())
        }
    }

    private fun handleEmailLink(link: String) {
        viewModel.handleIncomingEmailLink(link) { success, msg ->
            runOnUiThread {
                android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val display = display
                val modes = display?.supportedModes
                val highestRefreshRateMode = modes?.maxByOrNull { it.refreshRate }
                if (highestRefreshRateMode != null && highestRefreshRateMode.refreshRate >= 90f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = highestRefreshRateMode.modeId
                    window.attributes = params
                }
            } catch (e: Throwable) {
                // Fallback gracefully
            }
        } else {
            try {
                val params = window.attributes
                params.preferredRefreshRate = 120f
                window.attributes = params
            } catch (e: Throwable) {
                // Fallback gracefully
            }
        }
    }
}
