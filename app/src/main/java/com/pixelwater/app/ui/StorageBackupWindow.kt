package com.pixelwater.app.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
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
    val isFrostedGlassEnabled by viewModel.isFrostedGlassEnabled.collectAsStateWithLifecycle()
    val autoLocalBackup by viewModel.autoLocalBackupEnabled.collectAsStateWithLifecycle()
    val autoGoogleBackup by viewModel.autoGoogleBackupEnabled.collectAsStateWithLifecycle()
    var foundBackups by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    var hasScanned by remember { mutableStateOf(false) }
    var showManualInputArea by remember { mutableStateOf(false) }
    var manualJsonText by remember { mutableStateOf("") }
    var customBackupName by remember { mutableStateOf("pixel_water_backup") }

    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background.luminance() < 0.5f

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
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header (Appearance Settings Aesthetic)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text(
                        text = if (appLanguage == "el") "Κέντρο Αντιγράφων Ασφαλείας" else "Storage & Backup Center",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (appLanguage == "el") "Διαχειριστείτε τοπικά αντίγραφα, cloud συγχρονισμό & όρια δεδομένων" else "Manage local device, Google Drive backups & storage caps",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 1. Google Account Connection Card
                StorageWindowCard(isDark = isDark, isFrosted = isFrostedGlassEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(
                                            color = if (isGoogleLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isGoogleLoggedIn) {
                                        Text(
                                            text = googleInitials,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.CloudOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (isGoogleLoggedIn) googleAccountName else (if (appLanguage == "el") "Μη συνδεδεμένος λογαριασμός" else "Not Linked"),
                                        fontSize = 14.sp,
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
                                    onClick = {
                                        viewModel.triggerButtonHaptic()
                                        onSignOutGoogle()
                                    },
                                    shape = RoundedCornerShape(100),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
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
                                    onClick = {
                                        viewModel.triggerButtonHaptic()
                                        onOpenGoogleLoginDialog()
                                    },
                                    shape = RoundedCornerShape(100),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Rounded.Cloud, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (appLanguage == "el") "Google Drive" else "Link Google Drive",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.triggerButtonHaptic()
                                        onOpenFirebaseLoginDialog()
                                    },
                                    shape = RoundedCornerShape(100),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFDD2C00),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    FirebaseLogoIcon(modifier = Modifier.size(13.dp))
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

                // 2. Automated Backup Settings Header
                StorageSectionHeader(
                    title = if (appLanguage == "el") "ΑΥΤΟΜΑΤΟ ΑΝΤΙΓΡΑΦΟ ΑΣΦΑΛΕΙΑΣ" else "AUTOMATED BACKUP SETTINGS",
                    icon = Icons.Rounded.CloudDone
                )

                // 2a. Auto Local Storage Backup Card
                StorageWindowCard(
                    isDark = isDark,
                    isFrosted = isFrostedGlassEnabled,
                    onClick = {
                        viewModel.triggerButtonHaptic()
                        viewModel.updateAutoLocalBackupEnabled(!autoLocalBackup)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appLanguage == "el") "Αυτόματη αποθήκευση στο 'PixelWaterBackup' σε κάθε καταγραφή" else "Automatically save backup to 'PixelWaterBackup' folder on log updates",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                        ChunkySettingSwitch(
                            checked = autoLocalBackup,
                            onCheckedChange = {
                                viewModel.triggerButtonHaptic()
                                viewModel.updateAutoLocalBackupEnabled(it)
                            }
                        )
                    }
                }

                // 2b. Auto Cloud Backup & Sync Card
                StorageWindowCard(
                    isDark = isDark,
                    isFrosted = isFrostedGlassEnabled,
                    onClick = {
                        viewModel.triggerButtonHaptic()
                        viewModel.updateAutoGoogleBackupEnabled(!autoGoogleBackup)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appLanguage == "el") "Αυτόματος συγχρονισμός και αντίγραφο ασφαλείας στο Google Drive" else "Automatically sync and backup database to Google Drive cloud storage",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                        ChunkySettingSwitch(
                            checked = autoGoogleBackup,
                            onCheckedChange = {
                                viewModel.triggerButtonHaptic()
                                viewModel.updateAutoGoogleBackupEnabled(it)
                            }
                        )
                    }
                }

                // 2c. App User Data Limit & Storage Management Card (Redesigned)
                AppUserDataLimitCard(
                    viewModel = viewModel,
                    appLanguage = appLanguage
                )

                // 3. Manual Backup Export Section
                StorageSectionHeader(
                    title = if (appLanguage == "el") "ΧΕΙΡΟΚΙΝΗΤΗ ΕΞΑΓΩΓΗ" else "MANUAL EXPORT BACKUP",
                    icon = Icons.Rounded.Save
                )

                StorageWindowCard(isDark = isDark, isFrosted = isFrostedGlassEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = customBackupName,
                            onValueChange = { customBackupName = it },
                            label = { Text(if (appLanguage == "el") "Όνομα αρχείου (.json)" else "Backup File Name (.json)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.triggerButtonHaptic()
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
                                shape = RoundedCornerShape(100),
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
                                    viewModel.triggerButtonHaptic()
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
                                shape = RoundedCornerShape(100),
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

                // 4. Restoration Section
                StorageSectionHeader(
                    title = if (appLanguage == "el") "ΕΠΑΝΑΦΟΡΑ ΔΕΔΟΜΕΝΩΝ" else "RESTORE & RECOVER DATA",
                    icon = Icons.Rounded.SettingsBackupRestore
                )

                StorageWindowCard(isDark = isDark, isFrosted = isFrostedGlassEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.triggerButtonHaptic()
                                    restoreLauncher.launch("application/json")
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(100),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(imageVector = Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (appLanguage == "el") "Επιλογή Αρχείου" else "Browse File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.triggerButtonHaptic()
                                    foundBackups = viewModel.searchLocalBackups()
                                    hasScanned = true
                                    Toast.makeText(hContext, if (appLanguage == "el") "Βρέθηκαν ${foundBackups.size} αρχεία αντιγράφων!" else "Found ${foundBackups.size} backup files!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(100),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            ) {
                                Icon(imageVector = Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (appLanguage == "el") "Αυτόματη Αναζήτηση" else "Auto Search", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (hasScanned) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (foundBackups.isEmpty()) {
                                        Text(
                                            text = if (appLanguage == "el") "Δεν βρέθηκαν αρχεία .json στον φάκελο 'PixelWaterBackup'." else "No backup .json files found in 'PixelWaterBackup' directory.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    } else {
                                        Text(
                                            text = if (appLanguage == "el") "Αντίγραφα στο PixelWaterBackup:" else "Backups in PixelWaterBackup:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        foundBackups.sortedByDescending { it.lastModified() }.forEach { file ->
                                            val formattedDate = remember(file) {
                                                val df = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                                df.format(java.util.Date(file.lastModified()))
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                                border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else null,
                                                onClick = {
                                                    viewModel.triggerButtonHaptic()
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
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.InsertDriveFile,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Column {
                                                            Text(file.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                            Text(formattedDate, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Rounded.SettingsBackupRestore,
                                                        contentDescription = "Restore",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Expandable Advanced Input Section
                        val manualChevronRotation by animateFloatAsState(
                            targetValue = if (showManualInputArea) 180f else 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "manual_chevron"
                        )

                        Surface(
                            onClick = {
                                viewModel.triggerButtonHaptic()
                                showManualInputArea = !showManualInputArea
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Code,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (appLanguage == "el") "Εισαγωγή JSON χειροκίνητα (Advanced)" else "Input JSON backup manually (Advanced)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .rotate(manualChevronRotation)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = showManualInputArea,
                            enter = fadeIn() + expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)),
                            exit = fadeOut() + shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = manualJsonText,
                                    onValueChange = { manualJsonText = it },
                                    modifier = Modifier.fillMaxWidth().height(110.dp),
                                    placeholder = { Text("Paste raw backup JSON text here...", fontSize = 11.sp) },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Button(
                                        onClick = {
                                            viewModel.triggerButtonHaptic()
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
                                        modifier = Modifier.height(36.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        Text(if (appLanguage == "el") "Εφαρμογή JSON" else "Apply JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Data Privacy & Security On-Device Information Card
                StorageWindowCard(isDark = isDark, isFrosted = isFrostedGlassEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = if (appLanguage == "el") "Ιδιωτικότητα & Ασφάλεια Δεδομένων" else "Data Privacy & Local Sovereignty",
                                fontSize = 13.sp,
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
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun StorageSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun StorageWindowCard(
    isDark: Boolean,
    isFrosted: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardCornerShape = RoundedCornerShape(24.dp)
    val cardBackground by animateColorAsState(
        targetValue = if (isFrosted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "window_card_bg"
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

    val clickableModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
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
            .then(clickableModifier)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}
