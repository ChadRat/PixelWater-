package com.pixelwater.app.complication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.pixelwater.wear.R

class QuickAddComplicationService : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return createComplicationData(type, 1250, 2000, 250)
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        val prefs = getSharedPreferences("wear_prefs", Context.MODE_PRIVATE)
        val totalIntake = prefs.getInt("total_intake", 0)
        val goal = prefs.getInt("goal", 2000)
        val quickAdd = prefs.getInt("quick_add", 250)

        val complicationData = createComplicationData(
            request.complicationType,
            totalIntake,
            goal,
            quickAdd
        )
        listener.onComplicationData(complicationData)
    }

    private fun createComplicationData(
        type: ComplicationType,
        totalIntake: Int,
        goal: Int,
        quickAdd: Int
    ): ComplicationData? {
        val prefs = getSharedPreferences("wear_prefs", Context.MODE_PRIVATE)
        val styleKey = (prefs.getString("complication_icon_style", "DROPLET") ?: "DROPLET").uppercase()

        val tapIntent = Intent(this, QuickAddComplicationReceiver::class.java).apply {
            action = QuickAddComplicationReceiver.ACTION_QUICK_ADD
            putExtra(QuickAddComplicationReceiver.EXTRA_AMOUNT, quickAdd)
        }
        val tapPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isAppIcon = styleKey == "APP_ICON"
        val isInvisible = styleKey == "INVISIBLE" || styleKey == "TRANSPARENT"
        val isProgressCircle = styleKey == "PROGRESS_CIRCLE"

        val iconRes = when {
            isAppIcon -> R.mipmap.ic_launcher
            isInvisible -> R.drawable.ic_transparent
            else -> R.drawable.ic_water_drop
        }

        val icon = Icon.createWithResource(this, iconRes)
        val monoImage = MonochromaticImage.Builder(icon).build()
        val smallImageType = if (isAppIcon) SmallImageType.PHOTO else SmallImageType.ICON
        val smallImage = SmallImage.Builder(icon, smallImageType).build()

        val textContent = when {
            isInvisible -> PlainComplicationText.Builder(" ").build()
            isProgressCircle -> PlainComplicationText.Builder(" ").build()
            else -> PlainComplicationText.Builder("+$quickAdd").build()
        }
        val titleContent = if (isInvisible) null else PlainComplicationText.Builder("${totalIntake}ml").build()
        val descContent = PlainComplicationText.Builder("Quick add $quickAdd ml water").build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                val builder = ShortTextComplicationData.Builder(
                    text = textContent,
                    contentDescription = descContent
                )
                if (titleContent != null) builder.setTitle(titleContent)
                builder.setMonochromaticImage(monoImage)
                builder.setSmallImage(smallImage)
                builder.setTapAction(tapPendingIntent).build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    monochromaticImage = monoImage,
                    contentDescription = descContent
                )
                    .setTapAction(tapPendingIntent)
                    .build()
            }
            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    smallImage = smallImage,
                    contentDescription = descContent
                )
                    .setTapAction(tapPendingIntent)
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                val builder = RangedValueComplicationData.Builder(
                    value = if (isInvisible) 0f else totalIntake.coerceAtLeast(0).toFloat(),
                    min = 0f,
                    max = if (isInvisible) 1f else goal.coerceAtLeast(1).toFloat(),
                    contentDescription = descContent
                )
                builder.setText(textContent)
                if (titleContent != null) {
                    builder.setTitle(titleContent)
                }
                builder.setMonochromaticImage(monoImage)
                builder.setSmallImage(smallImage)
                builder.setTapAction(tapPendingIntent).build()
            }
            ComplicationType.LONG_TEXT -> {
                val builder = LongTextComplicationData.Builder(
                    text = if (isInvisible) PlainComplicationText.Builder(" ").build() else PlainComplicationText.Builder("Add $quickAdd ml ($totalIntake / $goal ml)").build(),
                    contentDescription = descContent
                )
                if (!isInvisible) {
                    builder.setTitle(PlainComplicationText.Builder("Pixel Water").build())
                }
                builder.setMonochromaticImage(monoImage)
                builder.setSmallImage(smallImage)
                builder.setTapAction(tapPendingIntent).build()
            }
            else -> null
        }
    }
}
