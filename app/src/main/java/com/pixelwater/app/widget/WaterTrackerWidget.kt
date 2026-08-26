package com.pixelwater.app.widget

import android.content.Context
import android.widget.Toast
import android.content.ComponentName
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import com.pixelwater.app.MainActivity
import com.pixelwater.app.data.WaterDatabase
import com.pixelwater.app.data.WaterLog
import com.pixelwater.app.data.WaterRepository
import com.pixelwater.app.R
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WaterTrackerWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        val appLanguage = prefs.getString("app_language", "en") ?: "en"
        val bgOption = prefs.getString("widget_bg_option", "opaque") ?: "opaque"
        
        // Load Selected Date and Goal
        val activeDateStr = prefs.getString("widget_selected_date", null) ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val dailyGoal = prefs.getInt("daily_goal", 2000)

        // Load Current Hydration Aggregate value
        val db = WaterDatabase.getDatabase(context)
        val dao = db.waterLogDao()
        
        val optimisticIntake = prefs.getInt("optimistic_total_$activeDateStr", -1)
        val totalIntake = if (optimisticIntake >= 0) {
            optimisticIntake
        } else {
            try {
                dao.getTotalIntakeForDateSync(activeDateStr) ?: 0
            } catch (e: Exception) {
                0
            }
        }
        
        // Cache this total so we can do fast optimistic updates on tap
        prefs.edit().putInt("cached_total_$activeDateStr", totalIntake).apply()
        
        val fillRatio = if (dailyGoal > 0) (totalIntake.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f) else 1f

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("select_tab", 0) // Explicitly open to Track tab
        }

        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val updatedString = if (appLanguage == "el") "Ενημερώθηκε ${formatter.format(Date())}" else "Updated ${formatter.format(Date())}"

        provideContent {
            val size = LocalSize.current
            val isWide = size.width >= 240.dp
            val isOneByOne = !isWide && (size.width < 160.dp || size.height < 160.dp)
            val showTopHeader = size.height >= 160.dp
            val isTall = size.height >= 200.dp

            val isNightMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            
            val bgResource = when (bgOption) {
                "translucent" -> if (isNightMode) R.drawable.widget_bg_translucent_dark else R.drawable.widget_bg_translucent_light
                "oled" -> R.drawable.widget_bg_oled
                else -> if (isNightMode) R.drawable.widget_bg_opaque_dark else R.drawable.widget_bg_opaque_light
            }

            // Determine if background is light-colored so we can adjust text colors dynamically
            val isBgLight = when (bgOption) {
                "translucent" -> !isNightMode
                "oled" -> false
                else -> !isNightMode
            }
            val textBaseColor = if (isBgLight) Color(0xFF1E1F22) else Color.White

            // Render Layout Base wrapper - One unified root Box
            val translucentInvisible = bgOption == "translucent" && prefs.getBoolean("widget_translucent_invisible", false)
            val boxModifier = if (translucentInvisible) {
                GlanceModifier.fillMaxSize()
            } else {
                GlanceModifier.fillMaxSize().background(ImageProvider(bgResource))
            }

            Box(
                modifier = boxModifier,
                contentAlignment = Alignment.Center
            ) {
                if (isOneByOne) {
                    // 1x1 or Small LAYOUT - Tapping adds quick add amount set in app settings
                    val diameterDp = minOf(size.width.value - 20f, size.height.value - 20f).coerceIn(45f, 110f)
                    CircularProgressColumn(
                        context = LocalContext.current,
                        totalIntake = totalIntake, 
                        dailyGoal = dailyGoal, 
                        fillRatio = fillRatio, 
                        appLanguage = appLanguage,
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .clickable(actionRunCallback<QuickAddWaterAction>(actionParametersOf(QuickAddWaterAction.AmountKey to -1))),
                        diameterDp = diameterDp,
                        isDark = !isBgLight
                    )
                } else if (isWide) {
                    // WIDE LAYOUT (e.g., 4x2 or 4x3)
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        // Top Header Bar - Show only if we have enough height
                        if (showTopHeader) {
                            Row(modifier = GlanceModifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Left-hand mini unicolored light gray representation of the apps icon, clickable to open app to stats/track tab
                                androidx.glance.Image(
                                    provider = ImageProvider(R.drawable.ic_mini_app_logo_widget), 
                                    contentDescription = "App Icon Logo Mini", 
                                    modifier = GlanceModifier.size(20.dp).clickable(actionStartActivity(intent))
                                )
                                Spacer(modifier = GlanceModifier.defaultWeight())
                                Text(updatedString, style = TextStyle(color = ColorProvider(textBaseColor), fontSize = 14.sp, fontWeight = FontWeight.Medium))
                                Spacer(modifier = GlanceModifier.defaultWeight())
                                // Circular arrow manual sync arrow
                                androidx.glance.Image(
                                    provider = ImageProvider(R.drawable.ic_refresh_widget), 
                                    contentDescription = "Manual Synchronize", 
                                    modifier = GlanceModifier.size(24.dp).clickable(actionRunCallback<QuickAddWaterAction>(actionParametersOf(QuickAddWaterAction.AmountKey to 0))) // amount to 0 triggers manual database sync update
                                )
                            }
                        } else {
                            Spacer(modifier = GlanceModifier.height(16.dp))
                        }
                        
                        val availableHeight = size.height.value - (if (showTopHeader) 54f else 24f)
                        val availableWidth = (size.width.value - 40f) * 0.5f
                        val diameterDp = minOf(availableWidth, availableHeight).coerceIn(45f, 150f)
                        
                        val scale = (diameterDp / 130f).coerceIn(0.5f, 1.1f)
                        val startEndPadding = (16f * scale).coerceAtLeast(6f).dp
                        val bottomPadding = (16f * scale).coerceAtLeast(6f).dp
                        val midSpacerWidth = (16f * scale).coerceAtLeast(6f).dp
                        
                        Row(
                            modifier = GlanceModifier.fillMaxSize().padding(start = startEndPadding, end = startEndPadding, bottom = bottomPadding), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Side (Circle) - Clicking inside adds quick add amount set by slider
                            Column(
                                modifier = GlanceModifier.defaultWeight().clickable(actionRunCallback<QuickAddWaterAction>(actionParametersOf(QuickAddWaterAction.AmountKey to -1))),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressColumn(LocalContext.current, totalIntake, dailyGoal, fillRatio, appLanguage, diameterDp = diameterDp, isDark = !isBgLight)
                            }
                            
                            Spacer(modifier = GlanceModifier.width(midSpacerWidth))
                            
                            // Right Side (Pills with exact matching custom drawables)
                            Column(
                                modifier = GlanceModifier.defaultWeight(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                QuickAddPill(amount = 150, bgRes = R.drawable.widget_bg_pill_1, bgLeftRes = R.drawable.widget_bg_pill_left_1, iconRes = R.drawable.ic_glass_widget, accentColor = Color(0xFF00E676), diameterDp = diameterDp)
                                Spacer(modifier = GlanceModifier.height((8f * scale).coerceAtLeast(4f).dp))
                                QuickAddPill(amount = 250, bgRes = R.drawable.widget_bg_pill_2, bgLeftRes = R.drawable.widget_bg_pill_left_2, iconRes = R.drawable.ic_glass_widget, accentColor = Color(0xFF81D4FA), diameterDp = diameterDp)
                                if (isTall) {
                                    Spacer(modifier = GlanceModifier.height((8f * scale).coerceAtLeast(4f).dp))
                                    QuickAddPill(amount = 500, bgRes = R.drawable.widget_bg_pill_3, bgLeftRes = R.drawable.widget_bg_pill_left_3, iconRes = R.drawable.ic_bottle_widget, accentColor = Color(0xFFFFFFFF), diameterDp = diameterDp)
                                }
                            }
                        }
                    }
                } else {
                    // 2x2 LAYOUT (Medium) - Tapping inside adds quick add amount set by slider
                    val diameterDp = minOf(size.width.value - 32f, size.height.value - 32f).coerceIn(45f, 150f)
                    CircularProgressColumn(
                        context = LocalContext.current,
                        totalIntake = totalIntake, 
                        dailyGoal = dailyGoal, 
                        fillRatio = fillRatio, 
                        appLanguage = appLanguage,
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .clickable(actionRunCallback<QuickAddWaterAction>(actionParametersOf(QuickAddWaterAction.AmountKey to -1))),
                        diameterDp = diameterDp,
                        isDark = !isBgLight
                    )
                }
            }
        }
    }

    private fun formatDisplayDate(dateStr: String, language: String): String {
        val todayStr = getCurrentDateString()
        if (dateStr == todayStr) {
            return if (language == "el") "Σήμερα" else "Today"
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
        if (dateStr == yesterdayStr) {
            return if (language == "el") "Χθες" else "Yesterday"
        }

        return try {
            val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = inputSdf.parse(dateStr) ?: Date()
            val locale = if (language == "el") Locale("el") else Locale.US
            val outputSdf = SimpleDateFormat("EEE, MMM d", locale)
            outputSdf.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}

@androidx.compose.runtime.Composable
fun DateNavRow(displayDate: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "◀",
            style = TextStyle(color = ColorProvider(Color(0xFF81D4FA)), fontSize = 15.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.padding(vertical = 4.dp, horizontal = 12.dp).clickable(actionRunCallback<NavigateDateAction>(actionParametersOf(NavigateDateAction.DirectionKey to -1)))
        )
        Text(
            text = displayDate,
            maxLines = 1,
            style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.clickable(actionRunCallback<NavigateDateAction>(actionParametersOf(NavigateDateAction.DirectionKey to 99))).padding(horizontal = 6.dp)
        )
        Text(
            text = "▶",
            style = TextStyle(color = ColorProvider(Color(0xFF81D4FA)), fontSize = 15.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.padding(vertical = 4.dp, horizontal = 12.dp).clickable(actionRunCallback<NavigateDateAction>(actionParametersOf(NavigateDateAction.DirectionKey to 1)))
        )
    }
}

@androidx.compose.runtime.Composable
fun CircularProgressColumn(
    context: Context,
    totalIntake: Int,
    dailyGoal: Int,
    fillRatio: Float,
    appLanguage: String,
    modifier: GlanceModifier = GlanceModifier,
    diameterDp: Float = 160f,
    isDark: Boolean = true
) {
    val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
    val displayMode = prefs.getString("widget_display_mode", "left") ?: "left"

    val bitmap = CircularProgressGenerator.generateCircularProgressBitmap(
        context = context,
        totalIntake = totalIntake,
        dailyGoal = dailyGoal,
        fillRatio = fillRatio,
        appLanguage = appLanguage,
        isDark = isDark,
        diameterDp = diameterDp,
        displayMode = displayMode
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.glance.Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Hydration Progress",
            modifier = GlanceModifier.size(diameterDp.dp)
        )
    }
}

@androidx.compose.runtime.Composable
fun QuickAddPill(
    amount: Int,
    bgRes: Int,
    bgLeftRes: Int,
    iconRes: Int,
    accentColor: Color,
    diameterDp: Float
) {
    val scale = (diameterDp / 130f).coerceIn(0.5f, 1.1f)
    val pillHeight = (54f * scale).coerceAtLeast(28f).dp
    val leftBoxWidth = (48f * scale).coerceAtLeast(24f).dp
    val iconSize = (20f * scale).coerceAtLeast(12f).dp
    val amountTextSize = (18f * scale).coerceAtLeast(12f).sp

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(pillHeight)
            .background(ImageProvider(bgRes))
            .clickable(actionRunCallback<QuickAddWaterAction>(actionParametersOf(QuickAddWaterAction.AmountKey to amount))),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = GlanceModifier.width(leftBoxWidth).fillMaxHeight().background(ImageProvider(bgLeftRes)),
                contentAlignment = Alignment.Center
            ) {
                androidx.glance.Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = "Drop Icon",
                    modifier = GlanceModifier.size(iconSize)
                )
            }
            Spacer(modifier = GlanceModifier.width((12f * scale).coerceAtLeast(4f).dp))
            Text(
                text = String.format("%,d", amount).replace(',', '.'),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = amountTextSize,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class WaterTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WaterTrackerWidget()
}

class NavigateDateAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val direction = parameters[DirectionKey] ?: 0
        val widgetPrefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        val activeDateStr = widgetPrefs.getString("widget_selected_date", null) ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        // 99 is standard override for Today reset
        if (direction == 99) {
            widgetPrefs.edit().remove("widget_selected_date").apply()
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(activeDateStr) ?: Date()
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.DAY_OF_YEAR, direction)
            val newDateStr = sdf.format(calendar.time)
            widgetPrefs.edit().putString("widget_selected_date", newDateStr).apply()
        }

        WaterTrackerWidget().updateAll(context)
    }

    companion object {
        val DirectionKey = ActionParameters.Key<Int>("direction")
    }
}

class QuickAddWaterAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val amountParam = parameters[AmountKey] ?: 250
        val widgetPrefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)

        if (amountParam == 0) {
            // Manual sync/refresh trigger - reset target date to today's date
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            widgetPrefs.edit().putString("widget_selected_date", todayStr).apply()

            WaterTrackerWidget().updateAll(context)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                val appLanguage = widgetPrefs.getString("app_language", "en") ?: "en"
                val msg = if (appLanguage == "el") "Συγχρονισμός ολοκληρώθηκε!" else "Data Synced!"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            return
        }

        // -1 represents the tapping added from the dynamic settings' quick add slider
        val amount = if (amountParam < 0) {
            widgetPrefs.getInt("quick_add_amount", 250)
        } else {
            amountParam
        }

        val activeDateStr = widgetPrefs.getString("widget_selected_date", null) ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        // OPTIMISTIC INSTANT UI UPDATE Accumulator
        val optimisticIntake = widgetPrefs.getInt("optimistic_total_$activeDateStr", -1)
        val currentTotal = if (optimisticIntake >= 0) optimisticIntake else widgetPrefs.getInt("cached_total_$activeDateStr", 0)
        val optimisticTotal = currentTotal + amount
        widgetPrefs.edit().putInt("optimistic_total_$activeDateStr", optimisticTotal).apply()
        
        // Update the tapped widget instantly!
        WaterTrackerWidget().update(context, glanceId)

        // Perform DB transaction in the same coroutine scope (onAction is suspendable and scoped appropriately by Glance) to prevent process death
        val db = WaterDatabase.getDatabase(context)
        val repository = WaterRepository(db.waterLogDao())
        val waterLog = WaterLog(
            amountMl = amount,
            timestamp = System.currentTimeMillis(),
            dateString = activeDateStr,
            beverageType = "Water",
            waterEquivalency = 1.0f,
            waterEquivalentMl = amount
        )
        repository.insertLog(waterLog)
        
        // Remove optimistic lock now that DB is accurate
        widgetPrefs.edit().remove("optimistic_total_$activeDateStr").apply()
        
        // Update all active glance widgets 
        WaterTrackerWidget().updateAll(context)

        // Localized feedback to user
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            val appLanguage = widgetPrefs.getString("app_language", "en") ?: "en"
            val msg = if (appLanguage == "el") "Προστέθηκαν ${amount}ml!" else "Added ${amount}ml!"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        val AmountKey = ActionParameters.Key<Int>("amount")
    }
}
