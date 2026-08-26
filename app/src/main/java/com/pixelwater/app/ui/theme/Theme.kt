package com.pixelwater.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkVibrantBlue,
    onPrimary = DarkOnVibrantBlue,
    primaryContainer = DarkVibrantContainer,
    onPrimaryContainer = DarkOnVibrantContainer,
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = CharcoalQuiet,
    onSurfaceVariant = DarkOnVibrantContainer,
    outline = CoolGrayBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFFE65100), // Sunset Coral main accent
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD2C1), // Vivid, rich coral container
    onPrimaryContainer = Color(0xFF1C1B18),
    secondary = Color(0xFF26282B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCE), // Vivid warm container
    onSecondaryContainer = Color(0xFF1C1B18),
    tertiary = Color(0xFFFFE0D6),
    onTertiary = CharcoalQuiet,
    background = Color(0xFFFFFBF8),
    onBackground = Color(0xFF1C1B18),
    surface = Color(0xFFF5F5F8), // Standard neutral light surface
    onSurface = Color(0xFF1C1B18),
    surfaceVariant = Color(0xFFECEEF2), // Standard neutral light surface variant
    onSurfaceVariant = Color(0xFF2B2823),
    outline = Color(0xFFE5D8D0)
  )

private val OledColorScheme =
  darkColorScheme(
    primary = DarkVibrantBlue,
    onPrimary = DarkOnVibrantBlue,
    primaryContainer = DarkVibrantContainer,
    onPrimaryContainer = DarkOnVibrantContainer,
    background = Color.Black,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = CharcoalQuiet,
    onSurfaceVariant = DarkOnVibrantContainer,
    outline = CoolGrayBorder
  )


fun getLightThemeBoxColor(mainColor: Color, lightnessTarget: Float = 0.85f, saturationFactor: Float = 0.85f): Color {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(mainColor.toArgb(), hsl)
    val hue = hsl[0]
    val sat = hsl[1]
    
    val newSat = if (sat < 0.05f) sat else (sat * saturationFactor).coerceIn(0.35f, 0.95f)
    val newLit = lightnessTarget.coerceIn(0.72f, 0.90f)
    
    return Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, newSat, newLit)))
}

fun ensureTextContrast(color: Color, isDark: Boolean): Color {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(color.toArgb(), hsl)
    val hue = hsl[0]
    val sat = hsl[1]
    val lit = hsl[2]

    return if (!isDark) {
        // Light mode: background surfaces are light (~0.90-0.98 lightness).
        // Text needs a rich, sufficiently dark shade (max lightness ~0.36f) to preserve high contrast and readability.
        val targetSat = if (sat < 0.10f) sat else sat.coerceAtLeast(0.55f)
        val targetLit = if (lit > 0.38f) 0.34f else lit
        Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, targetSat, targetLit)))
    } else {
        // Dark mode: background surfaces are dark (~0.10-0.16 lightness).
        // Text needs a bright, vibrant shade (min lightness ~0.65f) to pop against dark backgrounds.
        val targetSat = if (sat < 0.10f) sat else sat.coerceAtLeast(0.55f)
        val targetLit = if (lit < 0.60f) 0.70f else lit
        Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, targetSat, targetLit)))
    }
}

fun generateColorSchemeFromSeed(
    seedColorInt: Int,
    isDark: Boolean,
    isMonochrome: Boolean = false,
    monochromeTarget: Int = 0,
    secondarySeedInt: Int? = null,
    tertiarySeedInt: Int? = null
): ColorScheme {
  val hsl = FloatArray(3)
  androidx.core.graphics.ColorUtils.colorToHSL(seedColorInt, hsl)
  var hue = hsl[0]
  val sat = hsl[1]
  val lit = hsl[2]

  if (isMonochrome) {
    hue = when (monochromeTarget) {
      1 -> (hue + 30f) % 360f
      2 -> (hue + 120f) % 360f
      else -> hue
    }
  }

  val secHue: Float
  val secSat: Float
  val secLit: Float
  if (!isMonochrome && secondarySeedInt != null) {
      val secHsl = FloatArray(3)
      androidx.core.graphics.ColorUtils.colorToHSL(secondarySeedInt, secHsl)
      secHue = secHsl[0]
      secSat = secHsl[1]
      secLit = secHsl[2]
  } else {
      secHue = if (isMonochrome) hue else (hue + 30f) % 360f
      secSat = sat
      secLit = lit
  }

  val tertHue: Float
  val tertSat: Float
  val tertLit: Float
  if (!isMonochrome && tertiarySeedInt != null) {
      val tertHsl = FloatArray(3)
      androidx.core.graphics.ColorUtils.colorToHSL(tertiarySeedInt, tertHsl)
      tertHue = tertHsl[0]
      tertSat = tertHsl[1]
      tertLit = tertHsl[2]
  } else {
      tertHue = if (isMonochrome) hue else (hue + 120f) % 360f
      tertSat = sat
      tertLit = lit
  }

  if (isDark) {
    val rawPrimary = if (isMonochrome && monochromeTarget != 0) Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat, lit))) else Color(seedColorInt)
    val primaryColor = ensureTextContrast(rawPrimary, isDark = true)
    val onPrimaryColor = if (lit > 0.5f) Color.Black else Color.White
    
    val primaryContainerColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, (sat * 0.5f).coerceIn(0f, 1f), (lit * 0.3f).coerceIn(0f, 1f))))
    val onPrimaryContainerColor = if (lit * 0.3f > 0.5f) Color.Black else Color.White
    
    val rawSecondary = if (!isMonochrome && secondarySeedInt != null) Color(secondarySeedInt) else Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(secHue, (secSat * 0.8f).coerceIn(0f, 1f), secLit.coerceIn(0f, 1f))))
    val secondaryColor = ensureTextContrast(rawSecondary, isDark = true)
    val onSecondaryColor = if (secLit > 0.5f) Color.Black else Color.White
    val secondaryContainerColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(secHue, (secSat * 0.5f).coerceIn(0f, 1f), (secLit * 0.35f).coerceIn(0f, 1f))))
    val onSecondaryContainerColor = if (secLit * 0.35f > 0.5f) Color.Black else Color.White
    
    val rawTertiary = if (!isMonochrome && tertiarySeedInt != null) Color(tertiarySeedInt) else Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(tertHue, (tertSat * 0.8f).coerceIn(0f, 1f), tertLit.coerceIn(0f, 1f))))
    val tertiaryColor = ensureTextContrast(rawTertiary, isDark = true)
    val onTertiaryColor = if (tertLit > 0.5f) Color.Black else Color.White
    val tertiaryContainerColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(tertHue, (tertSat * 0.5f).coerceIn(0f, 1f), (tertLit * 0.35f).coerceIn(0f, 1f))))
    val onTertiaryContainerColor = if (tertLit * 0.35f > 0.5f) Color.Black else Color.White
    
    val bgColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, (sat * 0.2f).coerceIn(0f, 1f), (lit * 0.12f).coerceIn(0f, 1f))))
    val surfaceColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, (sat * 0.15f).coerceIn(0f, 1f), (lit * 0.16f).coerceIn(0f, 1f))))
    val onSurfaceColor = if ((lit * 0.16f) > 0.5f) Color.Black else Color(0xFFE2E2E6)
    
    val surfaceVariantColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, (sat * 0.15f).coerceIn(0f, 1f), (lit * 0.14f).coerceIn(0f, 1f))))
    val onSurfaceVariantColor = if ((lit * 0.14f) > 0.5f) Color.Black else Color(0xFFC4C6D0)
    val outlineColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, (sat * 0.2f).coerceIn(0f, 1f), (lit * 0.35f).coerceIn(0f, 1f))))

    return darkColorScheme(
      primary = primaryColor,
      onPrimary = onPrimaryColor,
      primaryContainer = primaryContainerColor,
      onPrimaryContainer = onPrimaryContainerColor,
      secondary = secondaryColor,
      onSecondary = onSecondaryColor,
      secondaryContainer = secondaryContainerColor,
      onSecondaryContainer = onSecondaryContainerColor,
      tertiary = tertiaryColor,
      onTertiary = onTertiaryColor,
      tertiaryContainer = tertiaryContainerColor,
      onTertiaryContainer = onTertiaryContainerColor,
      background = bgColor,
      onBackground = onSurfaceColor,
      surface = surfaceColor,
      onSurface = onSurfaceColor,
      surfaceVariant = surfaceVariantColor,
      onSurfaceVariant = onSurfaceVariantColor,
      outline = outlineColor
    )
  } else {
    val rawPrimary = if (isMonochrome && monochromeTarget != 0) Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat, lit))) else Color(seedColorInt)
    val primaryColor = ensureTextContrast(rawPrimary, isDark = false)
    val onPrimaryColor = if (lit > 0.5f) Color.Black else Color.White
    
    val rawSecondary = if (!isMonochrome && secondarySeedInt != null) Color(secondarySeedInt) else Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(secHue, (secSat * 0.7f).coerceIn(0f, 1f), secLit.coerceIn(0f, 1f))))
    val secondaryColor = ensureTextContrast(rawSecondary, isDark = false)
    val onSecondaryColor = if (secLit > 0.5f) Color.Black else Color.White
    
    val rawTertiary = if (!isMonochrome && tertiarySeedInt != null) Color(tertiarySeedInt) else Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(tertHue, (tertSat * 0.7f).coerceIn(0f, 1f), tertLit.coerceIn(0f, 1f))))
    val tertiaryColor = ensureTextContrast(rawTertiary, isDark = false)
    val onTertiaryColor = if (tertLit > 0.5f) Color.Black else Color.White
    
    val surfaceColor = getLightThemeBoxColor(primaryColor, 0.86f, 0.80f)
    val surfaceVariantColor = getLightThemeBoxColor(primaryColor, 0.82f, 0.85f)
    val bgColor = getLightThemeBoxColor(primaryColor, 0.97f, 0.25f)
    val primaryContainerColor = getLightThemeBoxColor(primaryColor, 0.82f, 0.90f)
    val secondaryContainerColor = getLightThemeBoxColor(secondaryColor, 0.84f, 0.85f)
    val tertiaryContainerColor = getLightThemeBoxColor(tertiaryColor, 0.86f, 0.85f)
    val onSurfaceColor = Color(0xFF1C1B18)
    val onSurfaceVariantColor = Color(0xFF2B2823)
    val outlineColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, (sat * 0.2f).coerceIn(0f, 1f), 0.82f)))

    return lightColorScheme(
      primary = primaryColor,
      onPrimary = onPrimaryColor,
      primaryContainer = primaryContainerColor,
      onPrimaryContainer = onSurfaceColor,
      secondary = secondaryColor,
      onSecondary = onSecondaryColor,
      secondaryContainer = secondaryContainerColor,
      onSecondaryContainer = onSurfaceColor,
      tertiary = tertiaryColor,
      onTertiary = onTertiaryColor,
      tertiaryContainer = tertiaryContainerColor,
      onTertiaryContainer = onSurfaceColor,
      background = bgColor,
      onBackground = onSurfaceColor,
      surface = surfaceColor,
      onSurface = onSurfaceColor,
      surfaceVariant = surfaceVariantColor,
      onSurfaceVariant = onSurfaceVariantColor,
      outline = outlineColor
    )
  }
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  themeType: String = "DYNAMIC",
  oledModeEnabled: Boolean = false,
  isFrostedGlassEnabled: Boolean = false,
  paletteIndex: Int = 0,
  wallpaperColors: List<Int> = emptyList(),
  staticThemeSeed: Int = 0xFF1D5AAB.toInt(),
  monochromeEnabled: Boolean = false,
  monochromeTarget: Int = 0,
  textContrastMode: Int = 0,
  fontSizeMode: Int = 0,
  textFontMode: Int = 0,
  colorContrastMode: Int = 0,
  lightModeDarkTextEnabled: Boolean = false,
  devFontScale: Float = 1.0f,
  devFontWeight: Float = 500f,
  devSmallestWidth: Float = 411f,
  content: @Composable () -> Unit,
) {
  var colorScheme =
    when {
      themeType == "STATIC" -> {
        generateColorSchemeFromSeed(staticThemeSeed, darkTheme)
      }
      themeType == "DYNAMIC" -> {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && paletteIndex == 0 && wallpaperColors.isEmpty()) {
          val context = LocalContext.current
          val dynamicScheme = if (darkTheme) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
          if (monochromeEnabled) {
            val seedColor = when (monochromeTarget) {
              1 -> dynamicScheme.secondary.toArgb()
              2 -> dynamicScheme.tertiary.toArgb()
              else -> dynamicScheme.primary.toArgb()
            }
            generateColorSchemeFromSeed(seedColor, darkTheme, true, 0)
          } else {
            dynamicScheme
          }
        } else {
          val seeds = if (wallpaperColors.isNotEmpty()) {
            wallpaperColors
          } else {
            listOf(
              0xFF1D5AAB.toInt(), // Primary: Sky Blue
              0xFF4B5563.toInt(), // Secondary: Slate Gray
              0xFFD97706.toInt()  // Tertiary: Warm Amber
            )
          }
          val seedColorInt = seeds[paletteIndex % seeds.size]
          val secSeedInt = if (!monochromeEnabled) seeds[(paletteIndex + 1) % seeds.size] else null
          val tertSeedInt = if (!monochromeEnabled) seeds[(paletteIndex + 2) % seeds.size] else null

          generateColorSchemeFromSeed(
            seedColorInt = seedColorInt,
            isDark = darkTheme,
            isMonochrome = monochromeEnabled,
            monochromeTarget = monochromeTarget,
            secondarySeedInt = secSeedInt,
            tertiarySeedInt = tertSeedInt
          )
        }
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  // Guarantee high text contrast for theme accent colors (primary, secondary, tertiary) across all themes
  val textPrimary = ensureTextContrast(colorScheme.primary, darkTheme)
  val textSecondary = ensureTextContrast(colorScheme.secondary, darkTheme)
  val textTertiary = ensureTextContrast(colorScheme.tertiary, darkTheme)

  colorScheme = colorScheme.copy(
      primary = textPrimary,
      secondary = textSecondary,
      tertiary = textTertiary
  )

  if (!darkTheme && colorContrastMode == 0) {
    val mainColor = colorScheme.primary
    val secColor = colorScheme.secondary
    val tertColor = colorScheme.tertiary
    val boxColor = getLightThemeBoxColor(mainColor, 0.94f)
    val boxVariantColor = getLightThemeBoxColor(mainColor, 0.91f)
    val boxBgColor = getLightThemeBoxColor(mainColor, 0.98f)
    
    val primaryContainerBox = getLightThemeBoxColor(mainColor, 0.88f, 0.90f)
    val secondaryContainerBox = getLightThemeBoxColor(secColor, 0.90f, 0.85f)
    val tertiaryContainerBox = getLightThemeBoxColor(tertColor, 0.91f, 0.85f)

    colorScheme = colorScheme.copy(
      surface = boxColor,
      surfaceVariant = boxVariantColor,
      background = boxBgColor,
      primaryContainer = primaryContainerBox,
      secondaryContainer = secondaryContainerBox,
      tertiaryContainer = tertiaryContainerBox,
      onSurface = Color(0xFF1C1B18),
      onSurfaceVariant = Color(0xFF2B2823),
      onBackground = Color(0xFF1C1B18),
      onPrimaryContainer = Color(0xFF1C1B18),
      onSecondaryContainer = Color(0xFF1C1B18),
      onTertiaryContainer = Color(0xFF1C1B18)
    )
  }

  if (isFrostedGlassEnabled) {
      val glassPrimary = if (darkTheme) {
          ensureTextContrast(colorScheme.primary, isDark = true)
      } else {
          val hsl = FloatArray(3)
          androidx.core.graphics.ColorUtils.colorToHSL(colorScheme.primary.toArgb(), hsl)
          hsl[1] = hsl[1].coerceAtLeast(0.70f)
          hsl[2] = 0.72f
          Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
      }

      colorScheme = colorScheme.copy(
        background = Color.Transparent,
        surface = if (darkTheme) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.22f),
        surfaceVariant = if (darkTheme) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.16f),
        primary = glassPrimary,
        onPrimary = if (androidx.core.graphics.ColorUtils.calculateLuminance(glassPrimary.toArgb()) > 0.5) Color(0xFF101828) else Color.White,
        primaryContainer = if (darkTheme) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.26f),
        onPrimaryContainer = Color.White,
        secondaryContainer = if (darkTheme) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.20f),
        onSecondaryContainer = Color.White,
        tertiaryContainer = if (darkTheme) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.20f),
        onTertiaryContainer = Color.White,
        onSurface = Color(0xFFF9FAFB),
        onSurfaceVariant = Color(0xFFE2E8F0),
        onBackground = Color(0xFFF9FAFB),
        outline = if (darkTheme) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.55f),
        outlineVariant = if (darkTheme) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.30f),
      )
  }

  if (oledModeEnabled && darkTheme) {
    colorScheme = colorScheme.copy(
      background = Color.Black
    )
  }

  if (colorContrastMode > 0) {
      if (darkTheme) {
          when (colorContrastMode) {
              1 -> { // Medium
                  colorScheme = colorScheme.copy(
                      surface = Color(0xFF1E1E1E),
                      background = Color(0xFF0C0A0F),
                      surfaceVariant = Color(0xFF2E2E2E)
                  )
              }
              2 -> { // High
                  colorScheme = colorScheme.copy(
                      surface = Color(0xFF111111),
                      background = Color(0xFF000000),
                      surfaceVariant = Color(0xFF222222)
                  )
              }
              else -> { // Extreme (3)
                  colorScheme = colorScheme.copy(
                      surface = Color(0xFF050505),
                      background = Color.Black,
                      surfaceVariant = Color(0xFF151515)
                  )
              }
          }
      } else {
          when (colorContrastMode) {
              1 -> { // Medium
                  colorScheme = colorScheme.copy(
                      surface = Color(0xFFF9F9F9),
                      background = Color(0xFFF0F0F0),
                      surfaceVariant = Color(0xFFE8E8E8)
                  )
              }
              2 -> { // High
                  colorScheme = colorScheme.copy(
                      surface = Color.White,
                      background = Color.White,
                      surfaceVariant = Color(0xFFDEDEDE)
                  )
              }
              else -> { // Extreme (3)
                  colorScheme = colorScheme.copy(
                      surface = Color.White,
                      background = Color.White,
                      surfaceVariant = Color(0xFFCCCCCC)
                  )
              }
          }
      }
  }

  if (textContrastMode > 0) {
      val defaultTextColor = if (darkTheme) Color.White else Color.Black
      when (textContrastMode) {
          1 -> { // Medium
              colorScheme = colorScheme.copy(
                  onSurface = if (darkTheme) Color(0xFFEBEBEB) else Color(0xFF151515),
                  onBackground = if (darkTheme) Color(0xFFEBEBEB) else Color(0xFF151515),
                  onSurfaceVariant = if (darkTheme) Color(0xFFCCCCCC) else Color(0xFF2F2F2F),
                  onPrimaryContainer = if (darkTheme) Color(0xFFE0E0E0) else Color(0xFF202020),
                  onSecondaryContainer = if (darkTheme) Color(0xFFE0E0E0) else Color(0xFF202020)
              )
          }
          2 -> { // High
              colorScheme = colorScheme.copy(
                  onSurface = defaultTextColor,
                  onBackground = defaultTextColor,
                  onSurfaceVariant = if (darkTheme) Color(0xFFE5E5E5) else Color(0xFF1A1A1A),
                  onPrimaryContainer = defaultTextColor,
                  onSecondaryContainer = defaultTextColor,
                  onTertiaryContainer = defaultTextColor
              )
          }
          else -> { // Extreme (3)
              colorScheme = colorScheme.copy(
                  onSurface = defaultTextColor,
                  onBackground = defaultTextColor,
                  onSurfaceVariant = defaultTextColor,
                  onPrimaryContainer = defaultTextColor,
                  onSecondaryContainer = defaultTextColor,
                  onTertiaryContainer = defaultTextColor,
                  outline = defaultTextColor,
                  outlineVariant = defaultTextColor
              )
          }
      }
  }

  if (lightModeDarkTextEnabled && !darkTheme) {
      colorScheme = colorScheme.copy(
          onPrimary = Color.Black,
          onSecondary = Color.Black,
          onTertiary = Color.Black,
          onPrimaryContainer = Color.Black,
          onSecondaryContainer = Color.Black,
          onTertiaryContainer = Color.Black,
          onSurface = Color.Black,
          onBackground = Color.Black,
          onSurfaceVariant = Color.Black
      )
  }

  val fontScaleVal = when (fontSizeMode) {
      1 -> 1.10f // Medium
      2 -> 1.20f // Big
      3 -> 1.35f // Enormous
      else -> 1.0f
  } * devFontScale

  val selectedFontFamily = when (textFontMode) {
      1 -> androidx.compose.ui.text.font.FontFamily.Monospace
      2 -> androidx.compose.ui.text.font.FontFamily.Serif
      3 -> androidx.compose.ui.text.font.FontFamily.SansSerif
      else -> androidx.compose.ui.text.font.FontFamily.Default
  }

  val baseTypo = Typography
  val customTypography = androidx.compose.material3.Typography(
      displayLarge = baseTypo.displayLarge.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 0.8f).toInt().coerceIn(100, 950))),
      displayMedium = baseTypo.displayMedium.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 0.8f).toInt().coerceIn(100, 950))),
      displaySmall = baseTypo.displaySmall.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 0.8f).toInt().coerceIn(100, 950))),
      headlineLarge = baseTypo.headlineLarge.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 1.1f).toInt().coerceIn(100, 950))),
      headlineMedium = baseTypo.headlineMedium.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 1.1f).toInt().coerceIn(100, 950))),
      headlineSmall = baseTypo.headlineSmall.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 1.1f).toInt().coerceIn(100, 950))),
      titleLarge = baseTypo.titleLarge.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 1.1f).toInt().coerceIn(100, 950))),
      titleMedium = baseTypo.titleMedium.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 1.0f).toInt().coerceIn(100, 950))),
      titleSmall = baseTypo.titleSmall.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 1.0f).toInt().coerceIn(100, 950))),
      bodyLarge = baseTypo.bodyLarge.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 1.0f).toInt().coerceIn(100, 950))),
      bodyMedium = baseTypo.bodyMedium.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 1.0f).toInt().coerceIn(100, 950))),
      bodySmall = baseTypo.bodySmall.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 1.0f).toInt().coerceIn(100, 950))),
      labelLarge = baseTypo.labelLarge.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 1.0f).toInt().coerceIn(100, 950))),
      labelMedium = baseTypo.labelMedium.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 1.0f).toInt().coerceIn(100, 950))),
      labelSmall = baseTypo.labelSmall.copy(fontFamily = selectedFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight((devFontWeight * 1.0f).toInt().coerceIn(100, 950)))
  )

  val originalDensity = androidx.compose.ui.platform.LocalDensity.current.density
  val originalScreenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()
  val actualDensityFactor = originalScreenWidth / devSmallestWidth
  val finalDensity = originalDensity * actualDensityFactor

  androidx.compose.runtime.CompositionLocalProvider(
    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
      density = finalDensity,
      fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale * fontScaleVal
    )
  ) {
      MaterialTheme(colorScheme = colorScheme, typography = customTypography, content = content)
  }
}
