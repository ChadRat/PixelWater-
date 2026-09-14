package com.pixelwater.app.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private const val GITHUB_REPO_URL = "https://github.com/ChadRat/PixelWater-"
private const val GITHUB_ISSUES_URL = "https://github.com/ChadRat/PixelWater-/issues/new"
private const val GNU_LICENSE_WEB_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/**
 * Loads the complete application license text from the assets folder.
 */
fun loadAppLicenseText(context: Context): String {
    return try {
        context.assets.open("LICENSE").bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        FALLBACK_GPL_LICENSE_SNIPPET
    }
}

/**
 * Renders the About section in Settings.
 * Displays app metadata, source code link, issue tracker link, and full GNU GPLv3 license viewer.
 * Follows the requirement: boxes contain NO transparency unless glass theme is enabled.
 */
fun androidx.compose.foundation.lazy.LazyListScope.renderAboutSection(
    viewModel: WaterViewModel,
    appLanguage: String,
    onBack: () -> Unit
) {
    item {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val isFrostedGlassEnabled = LocalFrostedGlassEnabled.current
        val frostedTransparency = LocalFrostedGlassTransparency.current
        val baseCardColor = LocalBaseCardColor.current ?: MaterialTheme.colorScheme.surfaceVariant
        val generalCornerRadius = LocalGeneralCornerRadius.current

        var showLicenseDialog by remember { mutableStateOf(false) }

        val dynamicVersionName = remember(context) {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.4.3"
            } catch (e: Exception) {
                "1.4.3"
            }
        }

        val licenseText = remember { loadAppLicenseText(context) }

        // Background brush: Solid opaque color unless Frosted Glass is enabled
        val cardBrush = if (isFrostedGlassEnabled) {
            GlassTheme.getCardBackgroundBrush(isDark, frostedTransparency)
        } else {
            SolidColor(baseCardColor)
        }

        // Outline: 1 dp outline in light mode, no outline in dark mode (unless frosted glass is active)
        val cardBorder = if (isDark) {
            if (isFrostedGlassEnabled) BorderStroke(1.dp, brush = GlassTheme.getCardBorderBrush(isDark)) else null
        } else {
            if (isFrostedGlassEnabled) {
                BorderStroke(1.dp, brush = GlassTheme.getCardBorderBrush(isDark))
            } else {
                BorderStroke(1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }

        val cardShape = RoundedCornerShape(generalCornerRadius.coerceAtLeast(24).dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // App Hero Banner
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(
                        color = if (isFrostedGlassEnabled) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WaterDrop,
                    contentDescription = "PixelWater App Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "PixelWater",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (appLanguage == "el") "Έκδοση $dynamicVersionName" else "Version $dynamicVersionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }

            // Grouped 3-Item Card matching Screenshot_20260914-114308.png
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .then(
                        if (cardBorder != null) Modifier.border(cardBorder, cardShape) else Modifier
                    )
                    .background(
                        brush = cardBrush,
                        shape = cardShape
                    )
                    .testTag("about_links_card")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Item 1: Source Code (Πηγαίος Κώδικας)
                    AboutCardRow(
                        iconBadge = {
                            // Circular background with dark pill containing "Code" text
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        color = if (isDark) Color(0xFF223651) else Color(0xFFC7DEFC),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isDark) Color(0xFF0F2643) else Color(0xFF0B57D0),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Code",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 11.sp
                                    )
                                }
                            }
                        },
                        title = if (appLanguage == "el") "Πηγαίος Κώδικας" else "Source Code",
                        subtitle = "github.com/ChadRat/PixelWater-",
                        onClick = {
                            viewModel.triggerButtonHaptic()
                            try {
                                uriHandler.openUri(GITHUB_REPO_URL)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 1.dp
                    )

                    // Item 2: Issue Tracker (Αναφορά Προβλήματος)
                    AboutCardRow(
                        iconBadge = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        color = if (isDark) Color(0xFF223651) else Color(0xFFC7DEFC),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BugReport,
                                    contentDescription = "Report issue icon",
                                    tint = if (isDark) Color(0xFFA8C7FA) else Color(0xFF0B57D0),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        title = if (appLanguage == "el") "Αναφορά Προβλήματος" else "Report an Issue",
                        subtitle = "issues.new",
                        onClick = {
                            viewModel.triggerButtonHaptic()
                            try {
                                uriHandler.openUri(GITHUB_ISSUES_URL)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 1.dp
                    )

                    // Item 3: License (Άδεια Χρήσης)
                    AboutCardRow(
                        iconBadge = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        color = if (isDark) Color(0xFF223651) else Color(0xFFC7DEFC),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Balance,
                                    contentDescription = "License icon",
                                    tint = if (isDark) Color(0xFFA8C7FA) else Color(0xFF0B57D0),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        title = if (appLanguage == "el") "Άδεια Χρήσης" else "License",
                        subtitle = "GNU GPLv3 License",
                        onClick = {
                            viewModel.triggerButtonHaptic()
                            showLicenseDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Full License Dialog
        if (showLicenseDialog) {
            AppLicenseDialog(
                licenseText = licenseText,
                appLanguage = appLanguage,
                isDark = isDark,
                isFrostedGlassEnabled = isFrostedGlassEnabled,
                frostedTransparency = frostedTransparency,
                baseCardColor = baseCardColor,
                generalCornerRadius = generalCornerRadius,
                onDismiss = {
                    showLicenseDialog = false
                    viewModel.triggerButtonHaptic()
                }
            )
        }
    }
}

/**
 * A single row in the About grouped card matching Screenshot_20260914-114308.png
 */
@Composable
private fun AboutCardRow(
    iconBadge: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            iconBadge()

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Full Dialog displaying the complete application GNU GPLv3 license.
 * Boxes adhere to: No transparency unless glass theme is on.
 */
@Composable
fun AppLicenseDialog(
    licenseText: String,
    appLanguage: String,
    isDark: Boolean,
    isFrostedGlassEnabled: Boolean,
    frostedTransparency: Float,
    baseCardColor: Color,
    generalCornerRadius: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    val dialogShape = RoundedCornerShape(generalCornerRadius.coerceAtLeast(24).dp)

    // Solid opaque background brush unless Frosted Glass is enabled
    val dialogBrush = if (isFrostedGlassEnabled) {
        GlassTheme.getCardBackgroundBrush(isDark, frostedTransparency)
    } else {
        SolidColor(baseCardColor)
    }

    val dialogBorder = if (isDark) {
        if (isFrostedGlassEnabled) BorderStroke(1.dp, brush = GlassTheme.getCardBorderBrush(isDark)) else null
    } else {
        if (isFrostedGlassEnabled) {
            BorderStroke(1.dp, brush = GlassTheme.getCardBorderBrush(isDark))
        } else {
            BorderStroke(1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }

    val innerBoxColor = if (isFrostedGlassEnabled) {
        GlassTheme.getSubCardColor(isDark)
    } else {
        if (isDark) Color(0xFF1B1D22) else Color(0xFFE9ECF0) // Solid opaque, no transparency
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(dialogShape)
                    .then(
                        if (dialogBorder != null) Modifier.border(dialogBorder, dialogShape) else Modifier
                    ),
                shape = dialogShape,
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(dialogBrush, dialogShape)
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = if (isDark) Color(0xFF223651) else Color(0xFFC7DEFC),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Balance,
                                        contentDescription = "License",
                                        tint = if (isDark) Color(0xFFA8C7FA) else Color(0xFF0B57D0),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = if (appLanguage == "el") "Άδεια Χρήσης" else "Application License",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "GNU General Public License v3.0",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (isDark) Color(0xFF2A2B2F) else Color(0xFFE2E6EC),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Action Buttons Bar (Copy / Web)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(licenseText))
                                    Toast.makeText(
                                        context,
                                        if (appLanguage == "el") "Η άδεια αντιγράφηκε επιτυχώς!" else "License copied to clipboard!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(100)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (appLanguage == "el") "Αντιγραφή" else "Copy",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        uriHandler.openUri(GNU_LICENSE_WEB_URL)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(100),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (appLanguage == "el") "GNU.org" else "GNU.org",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Complete License Scrollable Container (Solid, No Transparency)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(innerBoxColor)
                                .then(
                                    if (!isDark) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                    else Modifier
                                )
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            SelectionContainer {
                                Text(
                                    text = licenseText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    lineHeight = 17.sp,
                                    color = if (isDark) Color(0xFFD6DAE2) else Color(0xFF1C2026)
                                )
                            }
                        }

                        // Close Button
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(100),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = if (appLanguage == "el") "Κλείσιμο" else "Close",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val FALLBACK_GPL_LICENSE_SNIPPET = """                    GNU GENERAL PUBLIC LICENSE
                       Version 3, 29 June 2007

 Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.

                            Preamble

  The GNU General Public License is a free, copyleft license for
software and other kinds of works.

  The licenses for most software and other practical works are designed
to take away your freedom to share and change the works.  By contrast,
the GNU General Public License is intended to guarantee your freedom to
share and change all versions of a program--to make sure it remains free
software for all its users.  We, the Free Software Foundation, use the
GNU General Public License for most of our software; it applies also to
any other work released this way by its authors.  You can apply it to
your programs, too.

  When we speak of free software, we are referring to freedom, not
price.  Our General Public Licenses are designed to make sure that you
have the freedom to distribute copies of free software (and charge for
them if you wish), that you receive source code or can get it if you
want it, that you can change the software or use pieces of it in new
free programs, and that you know you can do these things.

[View full license at https://www.gnu.org/licenses/gpl-3.0.html]"""
