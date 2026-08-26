package com.pixelwater.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun DevDeleteDayLogsCard(
    viewModel: WaterViewModel,
    appLanguage: String,
    modifier: Modifier = Modifier
) {
    var daysAgoOffset by remember { mutableStateOf(0) }
    var customDateStr by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }

    val resolvedDateStr = remember(daysAgoOffset) {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgoOffset)
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.time)
    }

    LaunchedEffect(resolvedDateStr) {
        customDateStr = resolvedDateStr
    }

    ChunkySettingCard {
        Column(
            modifier = modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (appLanguage == "el") "Διαγραφή Καταγραφών Ημέρας" else "Delete Day Hydration Logs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = if (appLanguage == "el")
                    "Διαγράψτε εντελώς όλες τις καταγραφές νερού για μια συγκεκριμένη ημερομηνία χωρίς να επηρεαστούν οι άλλες ημέρες."
                    else "Completely delete all water log entries for a specific date without affecting other days.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (appLanguage == "el") "Ημέρες Πριν:" else "Days Offset:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(75.dp)
                )
                CapsulePatternSlider(
                    value = daysAgoOffset.toFloat(),
                    onValueChange = { daysAgoOffset = it.roundToInt() },
                    valueRange = 0f..14f,
                    triggerHaptic = { viewModel.triggerSliderHaptic() },
                    activeColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (daysAgoOffset == 0) "Today" else "-$daysAgoOffset d",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.End
                )
            }

            OutlinedTextField(
                value = customDateStr,
                onValueChange = {
                    customDateStr = it
                    showConfirm = false
                },
                label = { Text(if (appLanguage == "el") "Ημερομηνία (YYYY-MM-DD)" else "Date (YYYY-MM-DD)", fontSize = 10.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.error,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            if (showConfirm) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (appLanguage == "el") {
                            "Είστε σίγουροι ότι θέλετε να διαγράψετε ΟΛΕΣ τις καταγραφές για την ημερομηνία $customDateStr; Αυτή η ενέργεια δεν αναιρείται!"
                        } else {
                            "Are you sure you want to delete ALL water log entries for $customDateStr? This action cannot be undone!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showConfirm = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(if (appLanguage == "el") "Ακύρωση" else "Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.deleteWaterLogsForDate(customDateStr)
                                showConfirm = false
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                contentColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(if (appLanguage == "el") "Διαγραφή" else "Delete")
                        }
                    }
                }
            } else {
                Button(
                    onClick = { showConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (appLanguage == "el") "Επιβεβαίωση Διαγραφής" else "Confirm Delete")
                }
            }
        }
    }
}
