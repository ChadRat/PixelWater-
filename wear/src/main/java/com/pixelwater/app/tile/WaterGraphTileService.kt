package com.pixelwater.app.tile

import android.content.ComponentName
import android.content.Context
import androidx.core.graphics.ColorUtils
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.pixelwater.app.MainActivity

class WaterGraphTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val prefs = getSharedPreferences("wear_prefs", Context.MODE_PRIVATE)
        val todayIntake = prefs.getInt("total_intake", 0)
        val todayGoal = prefs.getInt("goal", 2000)
        val themeColorResolved = prefs.getInt("theme_color_resolved", 0xFF00BFA5.toInt())
        val appLanguage = prefs.getString("app_language", "en") ?: "en"
        val progressCircleThickness = prefs.getString("progress_circle_thickness", "THIN") ?: "THIN"
        val customThickness = prefs.getFloat("custom_thickness", 13f)
        val graphPillarThickness = prefs.getFloat("graph_pillar_thickness", 24f)

        val effectiveBarWidth = when (progressCircleThickness) {
            "THIN" -> (graphPillarThickness * (14.4f / 24f)).coerceIn(8f, 30f)
            "THICK" -> graphPillarThickness.coerceIn(10f, 36f)
            "CUSTOM" -> (graphPillarThickness * (customThickness / 24f)).coerceIn(6f, 36f)
            else -> graphPillarThickness.coerceIn(10f, 36f)
        }

        val daysPrior = prefs.getInt("graph_days_prior", 3)
        val maxForward = prefs.getInt("graph_max_forward", 1)

        val pastIntakesCsv = prefs.getString("past_intakes_csv", "") ?: ""
        val pastGoalsCsv = prefs.getString("past_goals_csv", "") ?: ""

        val intakesList = if (pastIntakesCsv.isNotBlank()) {
            pastIntakesCsv.split(",").mapNotNull { it.toIntOrNull() }
        } else {
            listOf(
                prefs.getInt("intake_minus_3", 1500),
                prefs.getInt("intake_minus_2", 1800),
                prefs.getInt("intake_minus_1", 2100),
                todayIntake,
                prefs.getInt("intake_plus_1", 0)
            )
        }

        val goalsList = if (pastGoalsCsv.isNotBlank()) {
            pastGoalsCsv.split(",").mapNotNull { it.toIntOrNull() }
        } else {
            listOf(
                prefs.getInt("goal_minus_3", 2000),
                prefs.getInt("goal_minus_2", 2000),
                prefs.getInt("goal_minus_1", 2000),
                todayGoal,
                prefs.getInt("goal_plus_1", 2000)
            )
        }

        // Chart Data map matching MainActivity exactly
        val wearChartData = (-daysPrior..maxForward).map { offset ->
            val label = when (offset) {
                0 -> if (appLanguage == "el") "ΣΗΜ" else "TOD"
                -1 -> if (appLanguage == "el") "ΧΘΕ" else "YEST"
                1 -> if (appLanguage == "el") "ΑΥΡ" else "TOM"
                else -> {
                    if (offset < 0) {
                        if (appLanguage == "el") "ΠΡ${-offset}" else "${offset}D"
                    } else {
                        if (appLanguage == "el") "ΣΕ$offset" else "+${offset}D"
                    }
                }
            }
            val originalIdx = offset + daysPrior
            val intake = intakesList.getOrNull(originalIdx) ?: 0
            val goal = goalsList.getOrNull(originalIdx) ?: 2000
            Triple(label, intake, goal)
        }

        val maxAmount = wearChartData.maxOfOrNull { it.second } ?: 1000
        val maxGoal = wearChartData.maxOfOrNull { it.third } ?: 1000
        val maxVal = maxOf(maxAmount, maxGoal, 1000)

        // Palette generation matching MainActivity exactly
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(themeColorResolved, hsl)
        val h = hsl[0]
        val s = hsl[1]
        val l = hsl[2]

        val activePillarColor = ColorUtils.HSLToColor(
            floatArrayOf(h, (s * 1.1f).coerceIn(0.6f, 1f), (l * 0.95f).coerceIn(0.40f, 0.65f))
        )
        val inProgressPillarColor = ColorUtils.HSLToColor(
            floatArrayOf(h, (s * 0.85f).coerceIn(0.4f, 0.9f), (l * 1.25f).coerceIn(0.60f, 0.85f))
        )
        val zeroBarColor = (activePillarColor and 0x00FFFFFF) or 0x38000000

        // Launch app action (tap graph)
        val launchIntent = ComponentName(this, MainActivity::class.java)
        val launchClickable = ModifiersBuilders.Clickable.Builder()
            .setId("launch_app_from_graph")
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

        val headerText = Text.Builder(this, if (appLanguage == "el") "ΙΣΤΟΡΙΚΟ ΝΕΡΟΥ" else "WATER INTAKE HISTORY")
            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
            .setColor(ColorBuilders.argb(0xCCFFFFFF.toInt()))
            .build()

        val subheaderText = Text.Builder(this, "${if (appLanguage == "el") "Σήμερα" else "Today"}: $todayIntake / $todayGoal ml")
            .setTypography(Typography.TYPOGRAPHY_BODY2)
            .setColor(ColorBuilders.argb(themeColorResolved))
            .build()

        // Build Graph Columns Row
        val graphRowBuilder = LayoutElementBuilders.Row.Builder()

        wearChartData.forEachIndexed { i, (label, valMl, valGoal) ->
            val activeBar = valMl >= valGoal
            val barHeightRatio = (valMl.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f) * 0.72f + 0.12f
            val barHeightDp = barHeightRatio * 56f

            val barColor = when {
                valMl == 0 -> zeroBarColor
                activeBar -> activePillarColor
                else -> inProgressPillarColor
            }

            val barBox = LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.dp(effectiveBarWidth))
                .setHeight(DimensionBuilders.dp(if (valMl == 0) (effectiveBarWidth * 0.12f).coerceIn(2f, 4f) else barHeightDp))
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setBackground(
                            ModifiersBuilders.Background.Builder()
                                .setColor(ColorBuilders.argb(barColor))
                                .setCorner(ModifiersBuilders.Corner.Builder().setRadius(DimensionBuilders.dp(effectiveBarWidth / 2f)).build())
                                .build()
                        )
                        .build()
                )
                .build()

            val isToday = label == "TOD" || label == "ΣΗΜ"
            val labelText = Text.Builder(this, label)
                .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                .setColor(ColorBuilders.argb(if (isToday) themeColorResolved else 0xFF9EAAB6.toInt()))
                .build()

            val col = LayoutElementBuilders.Column.Builder()
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(barBox)
                .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())
                .addContent(labelText)
                .build()

            graphRowBuilder.addContent(col)
            if (i < wearChartData.size - 1) {
                graphRowBuilder.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(6f)).build())
            }
        }

        val mainColumn = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(headerText)
            .addContent(subheaderText)
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(10f)).build())
            .addContent(graphRowBuilder.build())
            .build()

        val rootBox = LayoutElementBuilders.Box.Builder()
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(launchClickable).build())
            .addContent(mainColumn)
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
        private const val RESOURCES_VERSION = "1"
    }
}
