package com.pixelwater.app.ui

import androidx.compose.ui.text.font.FontWeight as ComposeFontWeight

object FontWeight {
    @Volatile
    var globalDevFontWeight: Float = 500f

    private fun dynamic(base: Int): ComposeFontWeight {
        val shift = globalDevFontWeight - 500f
        val target = (base + shift).toInt().coerceIn(100, 950)
        return ComposeFontWeight(target)
    }

    val Thin: ComposeFontWeight get() = dynamic(100)
    val ExtraLight: ComposeFontWeight get() = dynamic(200)
    val Light: ComposeFontWeight get() = dynamic(300)
    val Normal: ComposeFontWeight get() = dynamic(400)
    val Medium: ComposeFontWeight get() = dynamic(500)
    val SemiBold: ComposeFontWeight get() = dynamic(600)
    val Bold: ComposeFontWeight get() = dynamic(700)
    val ExtraBold: ComposeFontWeight get() = dynamic(800)
    val Black: ComposeFontWeight get() = dynamic(900)

    val W100: ComposeFontWeight get() = dynamic(100)
    val W200: ComposeFontWeight get() = dynamic(200)
    val W300: ComposeFontWeight get() = dynamic(300)
    val W400: ComposeFontWeight get() = dynamic(400)
    val W500: ComposeFontWeight get() = dynamic(500)
    val W600: ComposeFontWeight get() = dynamic(600)
    val W700: ComposeFontWeight get() = dynamic(700)
    val W800: ComposeFontWeight get() = dynamic(800)
    val W900: ComposeFontWeight get() = dynamic(900)
}
