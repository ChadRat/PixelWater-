package com.pixelwater.app.tile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.pixelwater.app.MainActivity
import com.pixelwater.app.complication.QuickAddComplicationReceiver
import java.util.Locale

class WaterProgressTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val clickId = requestParams.currentState.lastClickableId
        if (clickId == ID_QUICK_ADD) {
            val intent = Intent(this, QuickAddComplicationReceiver::class.java).apply {
                action = QuickAddComplicationReceiver.ACTION_QUICK_ADD
            }
            sendBroadcast(intent)
        }

        val prefs = getSharedPreferences("wear_prefs", Context.MODE_PRIVATE)
        val totalIntake = prefs.getInt("total_intake", 0)
        val goal = prefs.getInt("goal", 2000)
        val quickAdd = prefs.getInt("quick_add", 250)
        val themeColorResolved = prefs.getInt("theme_color_resolved", 0xFF00BFA5.toInt())
        val textColorResolved = prefs.getInt("text_color_resolved", 0xFFFFFFFF.toInt())
        val showRemaining = prefs.getBoolean("show_remaining", false)
        val removeInsideCircle = prefs.getBoolean("remove_inside_circle", false)
        val progressCircleThickness = prefs.getString("progress_circle_thickness", "THIN") ?: "THIN"
        val customThickness = prefs.getFloat("custom_thickness", 13f)
        val appLanguage = prefs.getString("app_language", "en") ?: "en"

        val thicknessDp = when (progressCircleThickness) {
            "THICK" -> 24f
            "CUSTOM" -> customThickness
            else -> 14.4f
        }

        val ratio = (totalIntake.toFloat() / goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        val sweepAngle = ratio * 360f

        // Launch app action (tap anywhere)
        val launchIntent = ComponentName(this, MainActivity::class.java)
        val launchClickable = ModifiersBuilders.Clickable.Builder()
            .setId("launch_app")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(packageName)
                            .setClassName(launchIntent.className)
                            .build()
                    )
                    .build()
            )
            .build()

        // Quick Add Clickable
        val quickAddClickable = ModifiersBuilders.Clickable.Builder()
            .setId(ID_QUICK_ADD)
            .setOnClick(ActionBuilders.LoadAction.Builder().build())
            .build()

        // Background Track Arc
        val bgArcLine = LayoutElementBuilders.ArcLine.Builder()
            .setLength(DimensionBuilders.degrees(360f))
            .setThickness(DimensionBuilders.dp(thicknessDp))
            .setColor(ColorBuilders.argb(0xFF2C2D2F.toInt()))
            .build()

        // Progress Arc
        val progressArcLine = LayoutElementBuilders.ArcLine.Builder()
            .setLength(DimensionBuilders.degrees(sweepAngle))
            .setThickness(DimensionBuilders.dp(thicknessDp))
            .setColor(ColorBuilders.argb(themeColorResolved))
            .build()

        val arcLayout = LayoutElementBuilders.Arc.Builder()
            .addContent(bgArcLine)
            .addContent(progressArcLine)
            .build()

        // Center Intake Formatting exactly matching MainActivity
        val intakeToFormat = if (showRemaining) (goal - totalIntake).coerceAtLeast(0) else totalIntake
        val formattedIntakeStr = if (intakeToFormat >= 1000) {
            val liters = intakeToFormat / 1000
            val remaining = intakeToFormat % 1000
            String.format(Locale.US, "%d.%03d", liters, remaining)
        } else {
            intakeToFormat.toString()
        }

        val topTextStr = if (appLanguage == "el") "ΣΗΜΕΡΑ" else "TODAY"
        val bottomTextStr = if (showRemaining) {
            if (appLanguage == "el") "ml απομένουν" else "ml remaining"
        } else {
            "ml / $goal"
        }

        // Title and Value Text
        val titleText = Text.Builder(this, topTextStr)
            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
            .setColor(ColorBuilders.argb(themeColorResolved))
            .build()

        val intakeText = Text.Builder(this, formattedIntakeStr)
            .setTypography(Typography.TYPOGRAPHY_DISPLAY3)
            .setColor(ColorBuilders.argb(textColorResolved))
            .build()

        val goalText = Text.Builder(this, bottomTextStr)
            .setTypography(Typography.TYPOGRAPHY_BODY2)
            .setColor(ColorBuilders.argb(0x99FFFFFF.toInt()))
            .build()

        // Quick Add Chip
        val quickAddChip = CompactChip.Builder(this, "+$quickAdd ml", quickAddClickable, requestParams.deviceConfiguration)
            .build()

        // Content Column
        val column = LayoutElementBuilders.Column.Builder()
            .addContent(titleText)
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(2f)).build())
            .addContent(intakeText)
            .addContent(goalText)
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())
            .addContent(quickAddChip)
            .build()

        val rootBoxBuilder = LayoutElementBuilders.Box.Builder()
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(launchClickable).build())

        if (!removeInsideCircle) {
            val centerBgBox = LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.dp(120f))
                .setHeight(DimensionBuilders.dp(120f))
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setBackground(
                            ModifiersBuilders.Background.Builder()
                                .setColor(ColorBuilders.argb(0xFF111213.toInt()))
                                .setCorner(ModifiersBuilders.Corner.Builder().setRadius(DimensionBuilders.dp(60f)).build())
                                .build()
                        )
                        .build()
                )
                .build()
            rootBoxBuilder.addContent(centerBgBox)
        }

        val rootBox = rootBoxBuilder
            .addContent(arcLayout)
            .addContent(column)
            .build()

        val layout = LayoutElementBuilders.Layout.Builder()
            .setRoot(rootBox)
            .build()

        val timelineEntry = TimelineBuilders.TimelineEntry.Builder()
            .setLayout(layout)
            .build()

        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(timelineEntry)
            .build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(timeline)
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
        return Futures.immediateFuture(resources)
    }

    companion object {
        private const val ID_QUICK_ADD = "quick_add_tile_click"
        private const val RESOURCES_VERSION = "1"
    }
}
