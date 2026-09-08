package com.pixelwater.app.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@Composable
fun AppUserDataLimitCard(
    viewModel: WaterViewModel,
    appLanguage: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFrostedGlassEnabled by viewModel.isFrostedGlassEnabled.collectAsStateWithLifecycle()
    val userDataLimitMb by viewModel.appUserDataLimitMb.collectAsStateWithLifecycle()
    val stats by viewModel.currentUserDataStats.collectAsStateWithLifecycle()

    var isPurging by remember { mutableStateOf(false) }
    var purgeResultDialogMessage by remember { mutableStateOf<String?>(null) }
    var isInjectingTestData by remember { mutableStateOf(false) }
    var showDeleteSelectionDialog by remember { mutableStateOf(false) }
    var selectedDeleteOption by remember { mutableStateOf(0) } // 0: >30 days, 1: <=30 days, 2: all

    val allLogsList by viewModel.allLogs.collectAsStateWithLifecycle()
    val cal30 = remember {
        java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -30)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
    }
    val cutoffDateStr = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal30.time)
    }
    val cutoffTimestamp = cal30.timeInMillis

    val olderCount = remember(allLogsList) {
        allLogsList.count { it.dateString < cutoffDateStr || it.timestamp < cutoffTimestamp }
    }
    val recentCount = remember(allLogsList) {
        allLogsList.count { it.dateString >= cutoffDateStr && it.timestamp >= cutoffTimestamp }
    }
    val totalCount = allLogsList.size

    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background.luminance() < 0.5f

    val limitFormatted = remember(userDataLimitMb) {
        when {
            userDataLimitMb >= 1024 && userDataLimitMb % 1024 == 0 -> "${userDataLimitMb / 1024} GB"
            userDataLimitMb >= 1024 -> String.format(java.util.Locale.US, "%.1f GB", userDataLimitMb / 1024.0)
            else -> "$userDataLimitMb MB"
        }
    }

    val limitBytes = userDataLimitMb.toLong() * 1024L * 1024L
    val usageFraction = if (limitBytes > 0L) {
        (stats.totalBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedUsageFraction by animateFloatAsState(
        targetValue = usageFraction,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "animated_usage"
    )
    val usagePercent = (usageFraction * 100f).roundToInt()

    val cardCornerShape = RoundedCornerShape(24.dp)
    val cardBackground by animateColorAsState(
        targetValue = if (isFrostedGlassEnabled) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "card_bg"
    )

    // Outline rule: 1dp ONLY in light mode; NO outline in dark mode
    val cardBorderModifier = if (!isDark) {
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            shape = cardCornerShape
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(cardBorderModifier)
            .background(
                color = cardBackground,
                shape = cardCornerShape
            )
            .clip(cardCornerShape)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header: Storage Icon Capsule + Title + Current / Limit Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (appLanguage == "el") "Όριο Δεδομένων Χρήστη" else "User Data Limit",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (appLanguage == "el") "Ακριβής μέτρηση & διαχείριση χώρου" else "Accurate storage metering & cap",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Storage usage pill badge
                Surface(
                    shape = RoundedCornerShape(100),
                    color = if (isFrostedGlassEnabled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else if (isDark) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = if (!isDark) {
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    } else null
                ) {
                    Text(
                        text = "${stats.formattedTotal} / $limitFormatted",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Visual Progress Meter
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (appLanguage == "el") "Χρήση Δεδομένων: $usagePercent%" else "User Data Used: $usagePercent%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (appLanguage == "el") "Μέγιστο: $limitFormatted" else "Cap: $limitFormatted",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedUsageFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(100)),
                    color = if (usageFraction > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = if (isFrostedGlassEnabled) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            }

            // Slider to choose preferred User Data size (10 MB - 5 GB / 5120 MB)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (appLanguage == "el") "Επιθυμητό Όριο Δεδομένων" else "Preferred User Data Limit",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
                    ) {
                        Text(
                            text = limitFormatted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                CapsulePatternSlider(
                    value = userDataLimitMb.toFloat(),
                    onValueChange = { newValue ->
                        val intVal = when {
                            newValue >= 5100f -> 5120
                            newValue >= 1000f -> ((newValue / 50f).roundToInt() * 50).coerceAtMost(5120)
                            newValue >= 250f -> (newValue / 25f).roundToInt() * 25
                            else -> ((newValue / 10f).roundToInt() * 10).coerceAtLeast(10)
                        }
                        if (intVal != userDataLimitMb) {
                            viewModel.updateAppUserDataLimitMb(intVal)
                        }
                    },
                    valueRange = 10f..5120f,
                    triggerHaptic = { viewModel.triggerSliderHaptic() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Action Row: Circular Arrow Refresh Button + Delete Data > 1 Month Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular arrow refresh button
                Surface(
                    onClick = {
                        viewModel.triggerButtonHaptic()
                        viewModel.refreshCurrentUserDataSize()
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = if (!isDark) {
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    } else null,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = if (appLanguage == "el") "Ανανέωση" else "Refresh",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Delete data button (opens confirmation dialog to delete user data while preserving goal streak)
                Button(
                    onClick = {
                        viewModel.triggerButtonHaptic()
                        if (!isPurging && !isInjectingTestData) {
                            selectedDeleteOption = 0
                            showDeleteSelectionDialog = true
                        }
                    },
                    enabled = !isPurging && !isInjectingTestData,
                    shape = RoundedCornerShape(100),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (isPurging) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (appLanguage == "el") "Εκκαθάριση..." else "Deleting...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (appLanguage == "el") "Διαγραφή Δεδομένων" else "Delete Data",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            // Streak preservation hint
            Text(
                text = if (appLanguage == "el")
                    "• Η διαγραφή απελευθερώνει gigabytes δεδομένων και διατηρεί μόνο το σερί στόχου"
                else
                    "• Deletion actively frees gigabytes of data and only maintains your goal streak",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 15.sp
            )
        }
    }

    // Selection Dialog to choose deletion scope
    if (showDeleteSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (appLanguage == "el") "Διαγραφή Δεδομένων Χρήστη" else "Delete User Data",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (appLanguage == "el")
                            "Επιλέξτε τρόπο διαγραφής. Η προτεινόμενη επιλογή απελευθερώνει gigabytes χώρου και διατηρεί μόνο το σερί στόχου:"
                        else
                            "Select deletion mode. The recommended option frees gigabytes of data and only maintains your goal streak:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option 0: Actively delete gigabytes of data while strictly maintaining goal streak
                    Surface(
                        onClick = { selectedDeleteOption = 0 },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedDeleteOption == 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedDeleteOption == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDeleteOption == 0,
                                onClick = { selectedDeleteOption = 0 }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (appLanguage == "el") "Διαγραφή Δεδομένων (Διατήρηση Σερί)" else "Delete User Data (Maintain Streak)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (appLanguage == "el")
                                        "Διαγράφει οριστικά gigabytes δεδομένων, παλιές καταγραφές & αντίγραφα. Διατηρεί μόνο το σερί στόχου!"
                                    else
                                        "Actively deletes gigabytes of historical logs, local backups & cache. Only maintains your goal streak!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 1: Past 30 days
                    Surface(
                        onClick = { selectedDeleteOption = 1 },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedDeleteOption == 1) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedDeleteOption == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDeleteOption == 1,
                                onClick = { selectedDeleteOption = 1 }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (appLanguage == "el") "Τελευταίες 30 ημέρες (≤ 30d)" else "Past 30 days (≤ 30d)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (appLanguage == "el")
                                        "$recentCount καταγραφές των τελευταίων 30 ημερών"
                                    else
                                        "$recentCount logs from past 30 days",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 2: All Data
                    Surface(
                        onClick = { selectedDeleteOption = 2 },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedDeleteOption == 2) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedDeleteOption == 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDeleteOption == 2,
                                onClick = { selectedDeleteOption = 2 },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (appLanguage == "el") "Όλα τα δεδομένα (Ολική διαγραφή)" else "All data (Full deletion)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (selectedDeleteOption == 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (appLanguage == "el")
                                        "Διαγραφή όλων των $totalCount καταγραφών & εκκαθάριση μνήμης"
                                    else
                                        "Delete all $totalCount records & clear cache",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteSelectionDialog = false
                        viewModel.triggerButtonHaptic()
                        if (!isPurging) {
                            isPurging = true
                            when (selectedDeleteOption) {
                                0 -> {
                                    viewModel.deleteUserDataAndMaintainStreak { _, message ->
                                        isPurging = false
                                        purgeResultDialogMessage = message
                                    }
                                }
                                1 -> {
                                    viewModel.deleteUserDataRecent30Days { _, message ->
                                        isPurging = false
                                        purgeResultDialogMessage = message
                                    }
                                }
                                2 -> {
                                    viewModel.clearAllHydrationData { _, message ->
                                        isPurging = false
                                        purgeResultDialogMessage = message
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedDeleteOption == 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = if (selectedDeleteOption == 2) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (selectedDeleteOption == 0) {
                            if (appLanguage == "el") "Διαγραφή & Διατήρηση Σερί" else "Delete & Keep Streak"
                        } else {
                            if (appLanguage == "el") "Διαγραφή" else "Delete"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteSelectionDialog = false }
                ) {
                    Text(
                        text = if (appLanguage == "el") "Άκυρο" else "Cancel"
                    )
                }
            }
        )
    }

    // Result & Explanation Dialog for Data Cleanup
    purgeResultDialogMessage?.let { dialogMsg ->
        val isZeroFound = dialogMsg.contains("No data older", ignoreCase = true) || dialogMsg.contains("Δεν βρέθηκαν", ignoreCase = true)
        AlertDialog(
            onDismissRequest = { purgeResultDialogMessage = null },
            icon = {
                Icon(
                    imageVector = if (isZeroFound) Icons.Rounded.Info else Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = if (isZeroFound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (appLanguage == "el") "Εκκαθάριση Δεδομένων" else "User Data Cleanup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = dialogMsg,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { purgeResultDialogMessage = null }
                ) {
                    Text(
                        text = if (appLanguage == "el") "Εντάξει" else "OK",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                if (isZeroFound) {
                    TextButton(
                        onClick = {
                            purgeResultDialogMessage = null
                            isInjectingTestData = true
                            viewModel.generateTestPastData60Days {
                                isInjectingTestData = false
                                Toast.makeText(
                                    context,
                                    if (appLanguage == "el") "Προστέθηκαν δοκιμαστικές καταγραφές 60 ημερών. Πατήστε ξανά τη διαγραφή για δοκιμή!" else "Added 60-day test data. Tap Delete Data > 30 Days to test!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        Text(
                            text = if (appLanguage == "el") "Προσθήκη δεδομένων (>30 ημ.) για δοκιμή" else "Add >30d data to test",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun AppCacheLimitCard(
    viewModel: WaterViewModel,
    appLanguage: String,
    modifier: Modifier = Modifier
) {
    // Delegated to AppUserDataLimitCard
    AppUserDataLimitCard(
        viewModel = viewModel,
        appLanguage = appLanguage,
        modifier = modifier
    )
}
