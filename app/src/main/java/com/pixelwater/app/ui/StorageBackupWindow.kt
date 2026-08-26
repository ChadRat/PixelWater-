package com.pixelwater.app.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageBackupWindow(
    viewModel: WaterViewModel,
    appLanguage: String,
    isGoogleLoggedIn: Boolean,
    googleAccountEmail: String,
    googleAccountName: String,
    onOpenGoogleLoginDialog: () -> Unit,
    onOpenFirebaseLoginDialog: () -> Unit,
    onSignOutGoogle: () -> Unit,
    onBackupSuccess: (Int, Int, String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val hContext = LocalContext.current
    val autoLocalBackup by viewModel.autoLocalBackupEnabled.collectAsStateWithLifecycle()
    val autoGoogleBackup by viewModel.autoGoogleBackupEnabled.collectAsStateWithLifecycle()
    var foundBackups by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    var hasScanned by remember { mutableStateOf(false) }
    var showManualInputArea by remember { mutableStateOf(false) }
    var manualJsonText by remember { mutableStateOf("") }
    var customBackupName by remember { mutableStateOf("pixel_water_backup") }

    val googleInitials = remember(googleAccountName) {
        if (googleAccountName.isBlank()) "G"
        else {
            val parts = googleAccountName.trim().split("\\s+".toRegex())
            if (parts.size >= 2) {
                "${parts[0].take(1).uppercase()}${parts[1].take(1).uppercase()}"
            } else {
                googleAccountName.take(2).uppercase()
            }
        }
    }

    val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val content = hContext.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
                if (content != null) {
                    viewModel.restoreDataFromJson(
                        content,
                        onSuccess = { Toast.makeText(hContext, if (appLanguage == "el") "Επιτυχής επαναφορά δεδομένων!" else "Successfully restored data!", Toast.LENGTH_SHORT).show() },
                        onError = { err -> Toast.makeText(hContext, if (appLanguage == "el") "Η επαναφορά απέτυχε: $err" else "Restore failed: $err", Toast.LENGTH_LONG).show() }
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(hContext, "Could not open file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and Header Action Row (Expressive Pixel Header Box - NO close button)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (appLanguage == "el") "Κέντρο Αντιγράφων Ασφαλείας" else "Storage & Backup Center",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appLanguage == "el") "Διαχειριστείτε τοπικά αντίγραφα & Google Drive" else "Manage local device and Google Drive backups",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 1. Google Account Connection Box (Shape morphs rounder when connected)
                val googleAccountRadius by animateDpAsState(
                    targetValue = if (isGoogleLoggedIn) 28.dp else 18.dp,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "google_account_shape"
                )
                val googleAccountBg by animateColorAsState(
                    targetValue = if (isGoogleLoggedIn) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "google_account_bg"
                )
                val googleAccountBorder by animateColorAsState(
                    targetValue = if (isGoogleLoggedIn) {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "google_account_border"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(googleAccountBg, RoundedCornerShape(googleAccountRadius))
                        .border(1.dp, googleAccountBorder, RoundedCornerShape(googleAccountRadius))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                        .size(38.dp)
                                        .background(
                                            color = if (isGoogleLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isGoogleLoggedIn) {
                                        Text(
                                            text = googleInitials,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.CloudOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (isGoogleLoggedIn) googleAccountName else (if (appLanguage == "el") "Μη συνδεδεμένος λογαριασμός" else "Not Linked"),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isGoogleLoggedIn) googleAccountEmail else (if (appLanguage == "el") "Συνδεθείτε για αυτόματο cloud backup" else "Sign in for cloud backup"),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isGoogleLoggedIn) {
                                Button(
                                    onClick = onSignOutGoogle,
                                    shape = RoundedCornerShape(100),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Αποσύνδεση" else "Disconnect",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (!isGoogleLoggedIn) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                Button(
                                    onClick = onOpenGoogleLoginDialog,
                                    shape = RoundedCornerShape(100),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Rounded.Cloud, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (appLanguage == "el") "Σύνδεση Google Drive" else "Link Google Drive",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = onOpenFirebaseLoginDialog,
                                    shape = RoundedCornerShape(100),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFDD2C00),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    FirebaseLogoIcon(modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (appLanguage == "el") "Firestore" else "Firestore Login",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Automated Backup Settings Cards (Shape morphs rounder when selected/active)
                Text(
                    text = if (appLanguage == "el") "Αυτόματο Αντίγραφο Ασφαλείας" else "Automated Backup Settings",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                )

                // 2a. Auto Local Storage Backup Card
                val localBackupRadius by animateDpAsState(
                    targetValue = if (autoLocalBackup) 28.dp else 16.dp,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "local_backup_shape"
                )
                val localBackupBg by animateColorAsState(
                    targetValue = if (autoLocalBackup) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "local_backup_bg"
                )
                val localBackupBorder by animateColorAsState(
                    targetValue = if (autoLocalBackup) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "local_backup_border"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(localBackupBg, RoundedCornerShape(localBackupRadius))
                        .border(1.dp, localBackupBorder, RoundedCornerShape(localBackupRadius))
                        .clickable { viewModel.updateAutoLocalBackupEnabled(!autoLocalBackup) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (autoLocalBackup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SdCard,
                                contentDescription = null,
                                tint = if (autoLocalBackup) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "el") "Αυτόματο Τοπικό Αντίγραφο" else "Auto Local Storage Backup",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (autoLocalBackup) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appLanguage == "el") "Αυτόματη αποθήκευση στο 'PixelWaterBackup' σε κάθε αλλαγή" else "Automatically backup data to 'PixelWaterBackup' directory on log changes",
                                fontSize = 11.sp,
                                color = if (autoLocalBackup) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                        ChunkySettingSwitch(
                            checked = autoLocalBackup,
                            onCheckedChange = { viewModel.updateAutoLocalBackupEnabled(it) }
                        )
                    }
                }

                // 2b. Auto Cloud Backup & Sync Card
                val cloudBackupRadius by animateDpAsState(
                    targetValue = if (autoGoogleBackup) 28.dp else 16.dp,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "cloud_backup_shape"
                )
                val cloudBackupBg by animateColorAsState(
                    targetValue = if (autoGoogleBackup) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "cloud_backup_bg"
                )
                val cloudBackupBorder by animateColorAsState(
                    targetValue = if (autoGoogleBackup) {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "cloud_backup_border"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cloudBackupBg, RoundedCornerShape(cloudBackupRadius))
                        .border(1.dp, cloudBackupBorder, RoundedCornerShape(cloudBackupRadius))
                        .clickable { viewModel.updateAutoGoogleBackupEnabled(!autoGoogleBackup) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (autoGoogleBackup) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = null,
                                tint = if (autoGoogleBackup) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (appLanguage == "el") "Αυτόματο Cloud Backup & Sync" else "Auto Cloud Backup & Sync",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (autoGoogleBackup) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appLanguage == "el") "Αυτόματος συγχρονισμός και δημιουργία αντιγράφων ασφαλείας στο Google Drive" else "Automatically sync and backup database to Google Drive / Cloud storage",
                                fontSize = 11.sp,
                                color = if (autoGoogleBackup) MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                        ChunkySettingSwitch(
                            checked = autoGoogleBackup,
                            onCheckedChange = { viewModel.updateAutoGoogleBackupEnabled(it) }
                        )
                    }
                }

                // 3. Manual Backup Export
                Text(
                    text = if (appLanguage == "el") "Χειροκίνητο Αντίγραφο (Manual Export)" else "Manual Backup Export",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = customBackupName,
                            onValueChange = { customBackupName = it },
                            label = { Text(if (appLanguage == "el") "Όνομα αρχείου (.json)" else "Backup File Name (.json)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.backupToDeviceStorage(
                                        fileName = if (customBackupName.endsWith(".json")) customBackupName else "$customBackupName.json",
                                        onSuccess = { file ->
                                            Toast.makeText(hContext, if (appLanguage == "el") "Αποθηκεύτηκε στο: ${file.absolutePath}" else "Saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                                        },
                                        onError = { err ->
                                            Toast.makeText(hContext, "Failed: $err", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(imageVector = Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (appLanguage == "el") "Στη Συσκευή" else "To Local", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (isGoogleLoggedIn) {
                                        viewModel.backupToGoogleDrive(
                                            fileName = if (customBackupName.endsWith(".json")) customBackupName else "$customBackupName.json",
                                            onSuccess = { logCount, prefCount, file ->
                                                onBackupSuccess(logCount, prefCount, file)
                                            },
                                            onError = { error ->
                                                Toast.makeText(hContext, "Cloud backup failed: $error", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        Toast.makeText(hContext, if (appLanguage == "el") "Παρακαλώ συνδεθείτε πρώτα με Google Drive!" else "Please link Google Drive account first!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Icon(imageVector = Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (appLanguage == "el") "Στο Drive" else "To Google Drive", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 4. Restoration section
                Text(
                    text = if (appLanguage == "el") "Επαναφορά Δεδομένων (Restore)" else "Restore & Recover Data",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { restoreLauncher.launch("application/json") },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(imageVector = Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (appLanguage == "el") "Επιλογή Αρχείου" else "Browse File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    foundBackups = viewModel.searchLocalBackups()
                                    hasScanned = true
                                    Toast.makeText(hContext, if (appLanguage == "el") "Βρέθηκαν ${foundBackups.size} αρχεία αντιγράφων!" else "Found ${foundBackups.size} backup files!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            ) {
                                Icon(imageVector = Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (appLanguage == "el") "Αυτόματη Αναζήτηση" else "Auto Search", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (hasScanned) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                if (foundBackups.isEmpty()) {
                                    Text(
                                        text = if (appLanguage == "el") "Δεν βρέθηκαν αρχεία .json στον φάκελο 'PixelWaterBackup'." else "No backup .json files found in 'PixelWaterBackup' directory.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (appLanguage == "el") "Αντίγραφα στο PixelWaterBackup:" else "Backups in PixelWaterBackup:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        foundBackups.sortedByDescending { it.lastModified() }.forEach { file ->
                                            val formattedDate = remember(file) {
                                                val df = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                                df.format(java.util.Date(file.lastModified()))
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        try {
                                                            val content = file.readText()
                                                            viewModel.restoreDataFromJson(
                                                                content,
                                                                onSuccess = { Toast.makeText(hContext, if (appLanguage == "el") "Επαναφορά επιτυχής!" else "Successfully restored database!", Toast.LENGTH_SHORT).show() },
                                                                onError = { err -> Toast.makeText(hContext, "Failed: $err", Toast.LENGTH_LONG).show() }
                                                            )
                                                        } catch (e: Exception) {
                                                            Toast.makeText(hContext, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(imageVector = Icons.Rounded.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    Column {
                                                        Text(file.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                        Text(formattedDate, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                                Icon(imageVector = Icons.Rounded.SettingsBackupRestore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Expandable Advanced Input Section
                        TextButton(
                            onClick = { showManualInputArea = !showManualInputArea },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = if (showManualInputArea) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (appLanguage == "el") "Εισαγωγή JSON χειροκίνητα (Advanced)" else "Input JSON backup manually (Advanced)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (showManualInputArea) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualJsonText,
                                    onValueChange = { manualJsonText = it },
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    placeholder = { Text("Paste raw backup JSON text here...", fontSize = 11.sp) },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Button(
                                        onClick = {
                                            if (manualJsonText.isNotBlank()) {
                                                viewModel.restoreDataFromJson(
                                                    manualJsonText,
                                                    onSuccess = {
                                                        Toast.makeText(hContext, if (appLanguage == "el") "Επιτυχής επαναφορά!" else "Restore Succeeded!", Toast.LENGTH_SHORT).show()
                                                        manualJsonText = ""
                                                        showManualInputArea = false
                                                    },
                                                    onError = { err ->
                                                        Toast.makeText(hContext, "Failed: $err", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            } else {
                                                Toast.makeText(hContext, "Please enter some JSON content first", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(100),
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp)
                                    ) {
                                        Text(if (appLanguage == "el") "Εφαρμογή JSON" else "Apply JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Data Privacy & Security On-Device Information Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = if (appLanguage == "el") "Ιδιωτικότητα & Ασφάλεια Δεδομένων" else "Data Privacy & Local Sovereignty",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = if (appLanguage == "el") {
                                "Τα δεδομένα σας αποθηκεύονται τοπικά στη συσκευή σας. Τα αντίγραφα ασφαλείας Google Drive αποθηκεύονται αποκλειστικά στον δικό σας προσωπικό χώρο Google Drive."
                            } else {
                                "All water intake logs and personal metrics are stored securely on-device. Google Drive backups reside exclusively in your personal cloud storage folder."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
