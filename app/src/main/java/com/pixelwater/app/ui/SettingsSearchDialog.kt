package com.pixelwater.app.ui
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.luminance

import kotlin.math.roundToInt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.pixelwater.app.ui.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.pixelwater.app.R
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelwater.app.data.WaterLog
import com.pixelwater.app.data.HealthConnectManager
import androidx.health.connect.client.PermissionController
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource



import com.pixelwater.app.data.*


@Composable
fun SettingsSearchDialog(
    viewModel: WaterViewModel,
    onDismiss: () -> Unit,
    onNavigate: (String?) -> Unit,
    onOpenGoalDialog: () -> Unit,
    isDark: Boolean,
    isOledActive: Boolean,
    appLanguage: String
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var aiQuery by remember { mutableStateOf("") }
    
    var aiLoading by remember { mutableStateOf(false) }
    var aiResponseExplanation by remember { mutableStateOf<String?>(null) }
    var aiProposedActions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var aiError by remember { mutableStateOf<String?>(null) }

    // Static settings list for regular search
    val settingsItems = remember(appLanguage) {
        listOf(
            Triple(
                if (appLanguage == "el") "Θέμα & Εμφάνιση" else "Theme & Appearance",
                if (appLanguage == "el") "Αλλαγή σκοτεινού/φωτεινού θέματος, OLED, σχήματα και χρώματα" else "Toggle dark/light theme, OLED mode, shape options and colors",
                "appearance"
            ),
            Triple(
                if (appLanguage == "el") "Απτική Ανάδραση (Haptics)" else "Haptic Feedback (Haptics)",
                if (appLanguage == "el") "Προσαρμόστε τις δονήσεις για κουμπιά, sliders και στόχους" else "Customize sliders, buttons, and target-reached vibrations",
                "haptics"
            ),
            Triple(
                if (appLanguage == "el") "Μικροεφαρμογή (Widget Settings)" else "Widget Settings",
                if (appLanguage == "el") "Ρυθμίσεις εμφάνισης widget στην αρχική οθόνη" else "Configure the widget layout on your device home screen",
                "widget_settings"
            ),
            Triple(
                if (appLanguage == "el") "Γρήγορες Ρυθμίσεις (Quick Settings Tile)" else "Quick Settings Tile",
                if (appLanguage == "el") "Προσαρμόστε το πλακίδιο γρήγορης προσθήκης νερού, εικονίδιο και ml" else "Customize the quick add tile, its icon and logged water amount",
                "tile_settings"
            ),
            Triple(
                if (appLanguage == "el") "Ρυθμίσεις Συμβούλου AI" else "AI Integration Coach Settings",
                if (appLanguage == "el") "Δείτε ή αλλάξτε το κλειδί API Gemini, το μοντέλο AI και το prompt" else "Manage your Gemini API key, AI model, and custom system context",
                "ai_integration"
            ),
            Triple(
                if (appLanguage == "el") "Καθημερινός Στόχος Ενυδάτωσης" else "Daily Hydration Target",
                if (appLanguage == "el") "Ρυθμίστε τον στόχο σας σε ml ή oz" else "Set your daily base and upper water intake goal target",
                "goal_dialog"
            ),
            Triple(
                if (appLanguage == "el") "Αυτόματο Cloud Backup" else "Auto Cloud Backup",
                if (appLanguage == "el") "Συγχρονισμός και δημιουργία αντιγράφων ασφαλείας στο Google Drive" else "Sync app logs and preferences securely to Google Drive",
                "backup"
            ),
            Triple(
                if (appLanguage == "el") "Γλώσσα Εφαρμογής" else "App Language Preference",
                if (appLanguage == "el") "Αλλαγή γλώσσας μεταξύ Ελληνικών και Αγγλικών" else "Switch default application language (English or Greek)",
                "language"
            ),
            Triple(
                if (appLanguage == "el") "Επικοινωνία & Πληροφορίες" else "About the Developer",
                if (appLanguage == "el") "Λεπτομέρειες για την εφαρμογή και επικοινωνία με τον δημιουργό" else "Contact the creator, view details and developer info",
                "contact_me"
            )
        )
    }

    val filteredSettings = remember(searchQuery, settingsItems) {
        if (searchQuery.isBlank()) emptyList()
        else {
            settingsItems.filter { item ->
                item.first.contains(searchQuery, ignoreCase = true) ||
                item.second.contains(searchQuery, ignoreCase = true) ||
                item.third.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 40.dp, horizontal = 24.dp),
            shape = RoundedCornerShape(32.dp),
            color = if (isDark) {
                if (isOledActive) Color.Black else Color(0xFF0F1115)
            } else {
                MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header with colorful glowing G-logo/AI-star icon and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF4285F4),
                                            Color(0xFF34A853),
                                            Color(0xFFFBBC05),
                                            Color(0xFFEA4335)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .padding(2.dp)
                                .background(
                                    color = if (isDark) (if (isOledActive) Color.Black else Color(0xFF0F1115)) else Color.White,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "AI logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (appLanguage == "el") "Αναζήτηση ρυθμίσεων" else "Search settings",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = if (isDark) Color.White else Color.Black,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = if (appLanguage == "el") "Έξυπνη διαμόρφωση συσκευής" else "Smart device configuration",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.triggerButtonHaptic()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isDark) Color(0xFF22242B) else Color(0xFFE0E0E0),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close search",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content area
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Regular Text Search Section
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (appLanguage == "el") "ΑΝΑΖΗΤΗΣΗ" else "SEARCH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.2.sp
                            )
                            
                            androidx.compose.material3.TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("settings_search_input")
                                    .border(
                                        width = 1.dp,
                                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(28.dp)
                                    ),
                                placeholder = {
                                    Text(
                                        text = if (appLanguage == "el") "Αναζήτηση στις ρυθμίσεις..." else "Search settings...",
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search Settings icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Clear,
                                                contentDescription = "Clear search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(28.dp),
                                singleLine = true,
                                colors = androidx.compose.material3.TextFieldDefaults.colors(
                                    focusedContainerColor = if (isDark) Color(0xFF1B1D23) else Color(0xFFEBE0C8),
                                    unfocusedContainerColor = if (isDark) Color(0xFF13151A) else MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }

                    // Static Search Results
                    if (searchQuery.isNotBlank()) {
                        val qLower = searchQuery.lowercase()
                        val oledQueryMatch = "oled" in qLower || "μαύρο" in qLower || "black" in qLower
                        val hapticsQueryMatch = "haptic" in qLower || "δονή" in qLower || "vibrat" in qLower
                        
                        if (oledQueryMatch || hapticsQueryMatch) {
                            item {
                                Text(
                                    text = if (appLanguage == "el") "ΑΚΡΙΒΕΙΣ ΡΥΘΜΙΣΕΙΣ" else "DIRECT SETTING OPTIONS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        if (oledQueryMatch) {
                            item {
                                val oledModeEnabled by viewModel.oledModeEnabled.collectAsStateWithLifecycle()
                                ChunkySettingCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (appLanguage == "el") "Λειτουργία OLED" else "AMOLED True Black Mode",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isDark) Color.White else Color.Black
                                            )
                                            Text(
                                                text = if (appLanguage == "el") "Καθαρό μαύρο χρώμα." else "True pitch black background.",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        ChunkySettingSwitch(
                                            checked = oledModeEnabled,
                                            onCheckedChange = { 
                                                viewModel.triggerButtonHaptic()
                                                viewModel.updateOledMode(it)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (hapticsQueryMatch) {
                            item {
                                val hapticsButtonsEnabled by viewModel.hapticsButtonsEnabled.collectAsStateWithLifecycle()
                                ChunkySettingCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (appLanguage == "el") "Δονήσεις για Κουμπιά" else "Haptics for Buttons",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isDark) Color.White else Color.Black
                                            )
                                            Text(
                                                text = if (appLanguage == "el") "Δονήσεις κατά το πάτημα." else "Crisp pulse when tapping.",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        ChunkySettingSwitch(
                                            checked = hapticsButtonsEnabled,
                                            onCheckedChange = { 
                                                viewModel.triggerButtonHaptic()
                                                viewModel.updateHapticsButtonsEnabled(it)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredSettings.isEmpty() && !oledQueryMatch && !hapticsQueryMatch) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Δεν βρέθηκαν αποτελέσματα για \"$searchQuery\"" else "No matching settings found for \"$searchQuery\"",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (filteredSettings.isNotEmpty()) {
                            item {
                                Text(
                                    text = if (appLanguage == "el") "ΚΑΤΗΓΟΡΙΕΣ" else "CATEGORIES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(filteredSettings) { item ->
                                ChunkySettingCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.triggerButtonHaptic()
                                            when (item.third) {
                                                "goal_dialog" -> {
                                                    onDismiss()
                                                    onOpenGoalDialog()
                                                }
                                                "language" -> {
                                                    onNavigate(null)
                                                    onDismiss()
                                                }
                                                "backup" -> {
                                                    onNavigate(null)
                                                    onDismiss()
                                                }
                                                else -> {
                                                    onNavigate(item.third)
                                                    onDismiss()
                                                }
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        val icon = when(item.third) {
                                            "appearance" -> Icons.Rounded.Palette
                                            "haptics" -> Icons.Rounded.TouchApp
                                            "widget_settings" -> Icons.Rounded.Widgets
                                            "ai_integration" -> Icons.Rounded.Psychology
                                            "goal_dialog" -> Icons.Rounded.Adjust
                                            "backup" -> Icons.Rounded.CloudUpload
                                            "language" -> Icons.Rounded.Language
                                            else -> Icons.Rounded.Info
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.first,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isDark) Color.White else Color.Black
                                            )
                                            Text(
                                                text = item.second,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Settings AI Assistant Coach Section
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = if (appLanguage == "el") "ΕΞΥΠΝΟΣ ΣΥΜΒΟΥΛΟΣ AI" else "INTELLIGENT AI COACH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            androidx.compose.material3.TextField(
                                value = aiQuery,
                                onValueChange = { aiQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_settings_coach_input")
                                    .border(
                                        width = 1.dp,
                                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                placeholder = {
                                    Text(
                                        text = if (appLanguage == "el") "π.χ., Κάνε την εφαρμογή Forest και σκοτεινή" else "e.g., Make my app Forest and dark mode",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Psychology,
                                        contentDescription = "AI Coach psychology icon",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                minLines = 2,
                                maxLines = 4,
                                colors = androidx.compose.material3.TextFieldDefaults.colors(
                                    focusedContainerColor = if (isDark) Color(0xFF1B1D23) else Color(0xFFEBE0C8),
                                    unfocusedContainerColor = if (isDark) Color(0xFF13151A) else MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )

                            val prompts = remember(appLanguage) {
                                if (appLanguage == "el") {
                                    listOf(
                                        "Πού είναι τα haptics;",
                                        "Βάλε θέμα Forest και σκοτεινή",
                                        "Βάλε ημερήσιο στόχο 3000ml",
                                        "Ενεργοποίησε το εφέ γυαλιού"
                                    )
                                } else {
                                    listOf(
                                        "Where are the haptics?",
                                        "Make app dark & Forest theme",
                                        "Set daily target to 3000ml",
                                        "Enable frosted glass effect"
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    contentPadding = PaddingValues(end = 16.dp)
                                ) {
                                    items(prompts) { prompt ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFFE2E7F0),
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .clickable {
                                                    viewModel.triggerButtonHaptic()
                                                    aiQuery = prompt
                                                }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color.Black else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = prompt,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDark) Color.Black else Color.Black.copy(alpha = 0.9f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    if (aiQuery.isNotBlank() && !aiLoading) {
                                        viewModel.triggerButtonHaptic()
                                        aiLoading = true
                                        aiError = null
                                        aiResponseExplanation = null
                                        aiProposedActions = emptyList()
                                        
                                        viewModel.querySettingsAiCoach(
                                            userQuery = aiQuery,
                                            currentLanguage = appLanguage,
                                            onSuccess = { explanation, actions ->
                                                aiResponseExplanation = explanation
                                                aiProposedActions = actions
                                                aiLoading = false
                                            },
                                            onError = { err ->
                                                aiError = err
                                                aiLoading = false
                                            }
                                        )
                                    }
                                },
                                enabled = aiQuery.isNotBlank() && !aiLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("ai_settings_ask_btn"),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (aiLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (appLanguage == "el") "Ρώτησε τον AI Coach" else "Ask AI Coach",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (aiError != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = aiError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (aiResponseExplanation != null) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ChunkySettingCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Psychology,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = if (appLanguage == "el") "Απάντηση από AI Coach" else "AI Coach Assistant",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Text(
                                            text = aiResponseExplanation ?: "",
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            color = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.9f)
                                        )
                                    }
                                }

                                if (aiProposedActions.isNotEmpty()) {
                                    ChunkySettingCard(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.VerifiedUser,
                                                    contentDescription = "Permission Required",
                                                    tint = Color(0xFFFBBC05),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = if (appLanguage == "el") "Απαιτείται Έγκριση!" else "Requesting Permission!",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFFFBBC05)
                                                )
                                            }

                                            Text(
                                                text = if (appLanguage == "el") {
                                                    "Επιτρέπετε στον AI Coach να πραγματοποιήσει τις παρακάτω αλλαγές;"
                                                } else {
                                                    "Do you authorize the AI Coach to apply the following system changes?"
                                                },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else Color.Black
                                            )

                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                aiProposedActions.forEach { action ->
                                                    val visualName = when (action.first) {
                                                        "theme_mode" -> if (appLanguage == "el") "Λειτουργία Θέματος (theme_mode)" else "Theme Mode (theme_mode)"
                                                        "app_theme" -> if (appLanguage == "el") "Παλέτα Εμφάνισης (app_theme)" else "App Art Theme Palette (app_theme)"
                                                        "daily_goal_ml" -> if (appLanguage == "el") "Καθημερινός Στόχος (daily_goal_ml)" else "Daily Hydration Goal (daily_goal_ml)"
                                                        "oled_mode" -> if (appLanguage == "el") "Λειτουργία OLED (oled_mode)" else "OLED Mode (oled_mode)"
                                                        "frosted_glass_enabled" -> if (appLanguage == "el") "Εφέ Γυαλιού (frosted_glass_enabled)" else "Frosted Glass Effect (frosted_glass_enabled)"
                                                        "general_corner_radius" -> if (appLanguage == "el") "Καμπυλότητα (general_corner_radius)" else "General Corner Radius (general_corner_radius)"
                                                        "reminders_enabled" -> if (appLanguage == "el") "Ειδοποιήσεις (reminders_enabled)" else "Reminders Alert (reminders_enabled)"
                                                        "reminder_interval" -> if (appLanguage == "el") "Μεσοδιάστημα Ειδοποιήσεων (reminder_interval)" else "Reminders Interval (reminder_interval)"
                                                        "quick_add_amount" -> if (appLanguage == "el") "Ποσότητα Γρήγορης Προσθήκης" else "Quick Add Water Amount"
                                                        "app_language" -> if (appLanguage == "el") "Γλώσσα Εφαρμογής" else "Application Language"
                                                        "navigate" -> if (appLanguage == "el") "Πλοήγηση στην Ενότητα" else "Navigate to Section"
                                                        else -> action.first
                                                    }

                                                    val visualVal = when (action.second) {
                                                        "SYSTEM" -> if (appLanguage == "el") "Σύστημα" else "System Default"
                                                        "LIGHT" -> if (appLanguage == "el") "Φωτεινό" else "Light Theme"
                                                        "DARK" -> if (appLanguage == "el") "Σκοτεινό" else "Dark Theme"
                                                        "true" -> if (appLanguage == "el") "Ναι (Ενεργό)" else "Enabled"
                                                        "false" -> if (appLanguage == "el") "Όχι (Ανενεργό)" else "Disabled"
                                                        else -> action.second
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(text = "•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            text = "$visualName -> ",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = visualVal,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.triggerButtonHaptic()
                                                        aiProposedActions = emptyList()
                                                        aiResponseExplanation = null
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(50),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                                ) {
                                                    Text(text = if (appLanguage == "el") "Απόρριψη" else "Cancel")
                                                }

                                                Button(
                                                    onClick = {
                                                        viewModel.triggerButtonHaptic()
                                                        
                                                        aiProposedActions.forEach { action ->
                                                            val id = action.first.trim()
                                                            val value = action.second.trim()
                                                            try {
                                                                when (id) {
                                                                    "theme_mode" -> viewModel.updateThemeMode(value)
                                                                    "app_theme" -> viewModel.updateAppTheme(value)
                                                                    "daily_goal_ml" -> value.toIntOrNull()?.let { viewModel.updateDailyGoal(it) }
                                                                    "oled_mode" -> viewModel.updateOledMode(value.toBoolean())
                                                                    "frosted_glass_enabled" -> viewModel.updateFrostedGlassEnabled(value.toBoolean())
                                                                    "general_corner_radius" -> value.toIntOrNull()?.let { viewModel.updateGeneralCornerRadius(it) }
                                                                    "navbar_corner_radius" -> value.toIntOrNull()?.let { viewModel.updateNavbarCornerRadius(it) }
                                                                    "reminders_enabled" -> viewModel.updateReminders(value.toBoolean())
                                                                    "reminder_interval" -> value.toIntOrNull()?.let { viewModel.updateReminderInterval(it) }
                                                                    "quick_add_amount" -> value.toIntOrNull()?.let { viewModel.updateQuickAddAmount(it) }
                                                                    "app_language" -> viewModel.updateAppLanguage(value)
                                                                    "navigate" -> {
                                                                        val page = if (value == "null") null else value
                                                                        onNavigate(page)
                                                                    }
                                                                }
                                                            } catch (ex: Exception) {
                                                                Log.e("SettingsSearchDialog", "Failed to apply action $id with val $value", ex)
                                                            }
                                                        }

                                                        val successToast = if (appLanguage == "el") "Ο Σύμβουλος AI εφάρμοσε επιτυχώς τις αλλαγές!" else "AI Coach successfully applied all requested changes!"
                                                        Toast.makeText(context, successToast, Toast.LENGTH_SHORT).show()
                                                        
                                                        aiProposedActions = emptyList()
                                                        aiResponseExplanation = null
                                                        onDismiss()
                                                    },
                                                    modifier = Modifier.weight(1f).testTag("ai_settings_approve_btn"),
                                                    shape = RoundedCornerShape(50),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF34A853),
                                                        contentColor = Color.White
                                                    )
                                                ) {
                                                    Text(text = if (appLanguage == "el") "Έγκριση" else "Approve")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun androidx.compose.foundation.lazy.LazyListScope.renderNotificationSettingsSection(
    viewModel: WaterViewModel,
    appLanguage: String,
    isScrollInProgress: Boolean = false
) {
    item {
        val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()
        val defaultSilent by viewModel.notificationDefaultSilent.collectAsStateWithLifecycle()
        val bannerEnabled by viewModel.notificationBannerEnabled.collectAsStateWithLifecycle()
        val soundMode by viewModel.notificationSoundMode.collectAsStateWithLifecycle()
        val vibrationEnabled by viewModel.notificationVibrationEnabled.collectAsStateWithLifecycle()
        val bypassDnd by viewModel.notificationBypassDnd.collectAsStateWithLifecycle()
        val dndNoSound by viewModel.notificationDndNoSound.collectAsStateWithLifecycle()
        val isGenerating by viewModel.isGeneratingAiSound.collectAsStateWithLifecycle()
        val audioGeminiModel by viewModel.audioGeminiModel.collectAsStateWithLifecycle()
        
        val context = androidx.compose.ui.platform.LocalContext.current
        var isExpandedSound by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        var aiJinglePrompt by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
        var isPreviewExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

        androidx.compose.runtime.LaunchedEffect(isScrollInProgress) {
            if (isScrollInProgress) {
                isPreviewExpanded = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Show Notifications Main Card (supports double-tap to toggle preview)
            ChunkySettingCard(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { _ ->
                            viewModel.triggerButtonHaptic()
                            isPreviewExpanded = !isPreviewExpanded
                        }
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (remindersEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                            contentDescription = null,
                            tint = if (remindersEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = if (appLanguage == "el") "Εμφάνιση ειδοποιήσεων" else "Show notifications",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appLanguage == "el") "Να επιτρέπονται οι υπενθυμίσεις ενυδάτωσης" else "Allow water drinking alerts",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    androidx.compose.material3.Switch(
                        checked = remindersEnabled,
                        onCheckedChange = {
                            viewModel.updateReminders(it)
                            viewModel.triggerToggleHaptic(reversed = !it)
                        },
                        modifier = Modifier.testTag("notifications_master_switch")
                    )
                }
            }



            if (remindersEnabled) {
                // 2. Behavior Selection: Default vs Silent (Pixel style radio selection block)
                Text(
                    text = if (appLanguage == "el") "ΣΥΜΠΕΡΙΦΟΡΑ ΕΙΔΟΠΟΙΗΣΗΣ" else "ALERT BEHAVIOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )

                ChunkySettingCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Default choice row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateNotificationDefaultSilent("DEFAULT")
                                    viewModel.triggerButtonHaptic()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (defaultSilent == "DEFAULT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (appLanguage == "el") "Προεπιλογή" else "Default",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (defaultSilent == "DEFAULT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (appLanguage == "el") "Ενδέχεται να κουδουνίζει ή να δονείται βάσει των ρυθμίσεων συσκευής" else "May ring or vibrate based on device settings",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            androidx.compose.material3.RadioButton(
                                selected = (defaultSilent == "DEFAULT"),
                                onClick = {
                                    viewModel.updateNotificationDefaultSilent("DEFAULT")
                                    viewModel.triggerButtonHaptic()
                                }
                            )
                        }

                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            thickness = 1.dp
                        )

                        // Silent choice row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateNotificationDefaultSilent("SILENT")
                                    viewModel.triggerButtonHaptic()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (defaultSilent == "SILENT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (appLanguage == "el") "Σίγαση" else "Mute",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (defaultSilent == "SILENT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (appLanguage == "el") "Κανονική ειδοποίηση αλλά χωρίς ήχο" else "Deliver normally without any sound",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            androidx.compose.material3.RadioButton(
                                selected = (defaultSilent == "SILENT"),
                                onClick = {
                                    viewModel.updateNotificationDefaultSilent("SILENT")
                                    viewModel.triggerButtonHaptic()
                                }
                            )
                        }
                    }
                }

                // 3. Audio & Banner Toggles Group
                Text(
                    text = if (appLanguage == "el") "ΠΡΟΣΘΕΤΕΣ ΕΠΙΛΟΓΕΣ" else "DELIVERY & SOUND",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )

                ChunkySettingCard {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        // pop as banner switch (Heads Up style)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Smartphone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = if (appLanguage == "el") "Εμφάνιση στην οθόνη" else "Pop on screen / Banner",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (appLanguage == "el") "Όταν η συσκευή είναι ξεκλειδωμένη, οι ειδοποιήσεις εμφανίζονται ως banner επάνω στην οθόνη" else "Show notifications as high-importance drop-down banner heads-up",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            androidx.compose.material3.Switch(
                                checked = bannerEnabled,
                                onCheckedChange = {
                                    viewModel.updateNotificationBannerEnabled(it)
                                    viewModel.triggerToggleHaptic(reversed = !it)
                                },
                                modifier = Modifier.testTag("banner_toggle_switch")
                            )
                        }

                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            thickness = 1.dp
                        )

                        // Sound Choice Accordion Card
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isExpandedSound = !isExpandedSound
                                        viewModel.triggerButtonHaptic()
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (appLanguage == "el") "Ήχος ειδοποίησης" else "Notification Sound",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val activeSoundName = when (soundMode) {
                                            "droplet" -> if (appLanguage == "el") "Σταγόνα νερού (Drop)" else "Water Droplet (Droplet)"
                                            "ai" -> if (appLanguage == "el") "AI Coach Jingle" else "AI Coach Custom Jingle"
                                            else -> if (appLanguage == "el") "Προεπιλεγμένος ήχος" else "Device Default Reminder"
                                        }
                                        Text(
                                            text = activeSoundName,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isExpandedSound) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            androidx.compose.animation.AnimatedVisibility(visible = isExpandedSound) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp, start = 12.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Option A: Default Android sound
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.updateNotificationSoundMode("device")
                                                viewModel.triggerButtonHaptic()
                                            },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (appLanguage == "el") "Ήχος συσκευής" else "Device default sound",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (appLanguage == "el") "Χρήση του προεπιλεγμένου ήχου Android" else "Play the standard device notification alert",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        androidx.compose.material3.RadioButton(
                                            selected = (soundMode == "device"),
                                            onClick = {
                                                viewModel.updateNotificationSoundMode("device")
                                                viewModel.triggerButtonHaptic()
                                            }
                                        )
                                    }

                                    androidx.compose.material3.HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                    )

                                    // Option B: Splashing Water droplet sound
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.updateNotificationSoundMode("droplet")
                                                viewModel.triggerButtonHaptic()
                                            },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = if (appLanguage == "el") "Ήχος σταγόνας" else "Water droplet sound",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                // Preview Button
                                                androidx.compose.material3.IconButton(
                                                    onClick = {
                                                        try {
                                                            val sf = com.pixelwater.app.notifications.SoundGenerator.generateWaterDropletWav(context)
                                                            val mp = android.media.MediaPlayer().apply {
                                                                setDataSource(sf.absolutePath)
                                                                prepare()
                                                                start()
                                                            }
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.PlayCircleFilled,
                                                        contentDescription = "Preview sound",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (appLanguage == "el") "Ένας φυσικός, αναζωογονητικός ήχος σταγόνας" else "A clean, custom synthesized splash droplet sound",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        androidx.compose.material3.RadioButton(
                                            selected = (soundMode == "droplet"),
                                            onClick = {
                                                viewModel.updateNotificationSoundMode("droplet")
                                                viewModel.triggerButtonHaptic()
                                            }
                                        )
                                    }

                                    androidx.compose.material3.HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                    )

                                    // Option C: AI Coach custom jingle sound
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.updateNotificationSoundMode("ai")
                                                    viewModel.triggerButtonHaptic()
                                                },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = if (appLanguage == "el") "Σύνθεση ήχου AI" else "AI Coach generated sound",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    )
                                                    // Preview if exists
                                                    val soundFile = java.io.File(context.filesDir, "ai_notification.wav")
                                                    if (soundFile.exists()) {
                                                        androidx.compose.material3.IconButton(
                                                            onClick = {
                                                                try {
                                                                    val mp = android.media.MediaPlayer().apply {
                                                                        setDataSource(soundFile.absolutePath)
                                                                        prepare()
                                                                        start()
                                                                    }
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                }
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.PlayCircleFilled,
                                                                contentDescription = "Preview sound",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = if (appLanguage == "el") "Μια μελωδία συντεθειμένη από τον AI Coach σας" else "A melodic ambient chord sequence composed by Gemini",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            androidx.compose.material3.RadioButton(
                                                selected = (soundMode == "ai"),
                                                onClick = {
                                                    viewModel.updateNotificationSoundMode("ai")
                                                    viewModel.triggerButtonHaptic()
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        if (soundMode == "ai") {
                                            var isAudioModelMenuExpanded by remember { mutableStateOf(false) }
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = if (appLanguage == "el") "Μοντέλο Ήχου AI:" else "AI Audio Model:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Box {
                                                    androidx.compose.material3.TextButton(onClick = { isAudioModelMenuExpanded = true }) {
                                                        Text(text = audioGeminiModel, fontSize = 13.sp)
                                                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                                                    }
                                                    androidx.compose.material3.DropdownMenu(
                                                        expanded = isAudioModelMenuExpanded,
                                                        onDismissRequest = { isAudioModelMenuExpanded = false }
                                                    ) {
                                                        androidx.compose.material3.DropdownMenuItem(
                                                            text = { Text("lyria-3-clip-preview (Music/Short)") },
                                                            onClick = {
                                                                viewModel.updateAudioGeminiModel("lyria-3-clip-preview")
                                                                isAudioModelMenuExpanded = false
                                                            }
                                                        )
                                                        androidx.compose.material3.DropdownMenuItem(
                                                            text = { Text("lyria-3-pro-preview (Music/Full)") },
                                                            onClick = {
                                                                viewModel.updateAudioGeminiModel("lyria-3-pro-preview")
                                                                isAudioModelMenuExpanded = false
                                                            }
                                                        )
                                                        androidx.compose.material3.DropdownMenuItem(
                                                            text = { Text("gemini-2.5-flash-native-audio-preview-12-2025 (Native Audio)") },
                                                            onClick = {
                                                                viewModel.updateAudioGeminiModel("gemini-2.5-flash-native-audio-preview-12-2025")
                                                                isAudioModelMenuExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            androidx.compose.material3.OutlinedTextField(
                                                value = aiJinglePrompt,
                                                onValueChange = { aiJinglePrompt = it },
                                                label = { Text(text = if (appLanguage == "el") "Περιγράψτε τον ήχο σας" else "Describe your custom sound") },
                                                placeholder = { Text(text = if (appLanguage == "el") "π.χ. χαλαρωτικό αρπέτζιο" else "e.g. relaxing bell arpeggio") },
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }

                                        // AI Generation Button
                                        if (isGenerating) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                androidx.compose.material3.CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = if (appLanguage == "el") "Ο Σύμβουλος AI συνθέτει μελωδία..." else "AI Coach composing melodic pattern...",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        } else {
                                            androidx.compose.material3.OutlinedButton(
                                                onClick = {
                                                    viewModel.triggerWoopHaptic()
                                                    viewModel.generateAiSoundTrack(
                                                        customInstruction = aiJinglePrompt,
                                                        onSuccess = {
                                                            viewModel.updateNotificationSoundMode("ai")
                                                            Toast.makeText(context, if (appLanguage == "el") "Η μελωδία AI δημιουργήθηκε επιτυχώς!" else "Beautiful AI Jingle is ready!", Toast.LENGTH_SHORT).show()
                                                            try {
                                                                val mp = android.media.MediaPlayer().apply {
                                                                    setDataSource(java.io.File(context.filesDir, "ai_notification.wav").absolutePath)
                                                                    prepare()
                                                                    start()
                                                                }
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            }
                                                        },
                                                        onError = { msg ->
                                                            Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                                                        }
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Text(text = if (appLanguage == "el") "Δημιουργία νέου ήχου" else "Generate or refresh AI Sound")
                                                }
                                            }

                                            // Save section
                                            val aiFile = java.io.File(context.filesDir, "ai_notification.wav")
                                            if (aiFile.exists()) {
                                                var showSaveDialog by remember { mutableStateOf(false) }
                                                var saveName by remember { mutableStateOf("") }
                                                var savedSounds by remember { mutableStateOf(viewModel.getSavedAiSounds()) }

                                                androidx.compose.material3.TextButton(
                                                    onClick = { showSaveDialog = true },
                                                    modifier = Modifier.align(Alignment.End),
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (appLanguage == "el") "Αποθήκευση στη Συλλογή" else "Save to Collection", fontSize = 12.sp)
                                                }

                                                if (showSaveDialog) {
                                                    androidx.compose.material3.AlertDialog(
                                                        onDismissRequest = { showSaveDialog = false },
                                                        title = { Text(if (appLanguage == "el") "Αποθήκευση Ήχου AI" else "Save AI Sound") },
                                                        text = {
                                                            androidx.compose.material3.OutlinedTextField(
                                                                value = saveName,
                                                                onValueChange = { saveName = it },
                                                                label = { Text(if (appLanguage == "el") "Όνομα ήχου" else "Sound Name") },
                                                                singleLine = true
                                                            )
                                                        },
                                                        confirmButton = {
                                                            androidx.compose.material3.TextButton(onClick = {
                                                                if (saveName.isNotBlank()) {
                                                                    viewModel.saveCurrentAiSound(saveName) {
                                                                        showSaveDialog = false
                                                                        saveName = ""
                                                                        savedSounds = viewModel.getSavedAiSounds()
                                                                        Toast.makeText(context, if (appLanguage == "el") "Αποθηκεύτηκε!" else "Saved successfully!", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            }) { Text(if (appLanguage == "el") "Αποθήκευση" else "Save") }
                                                        },
                                                        dismissButton = {
                                                            androidx.compose.material3.TextButton(onClick = { showSaveDialog = false }) { Text(if (appLanguage == "el") "Ακύρωση" else "Cancel") }
                                                        }
                                                    )
                                                }

                                                if (savedSounds.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(if (appLanguage == "el") "Συλλογή AI Ήχων" else "AI Sound Collection", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    savedSounds.forEach { savedAudioFile ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    viewModel.updateNotificationSoundMode(savedAudioFile)
                                                                    viewModel.triggerButtonHaptic()
                                                                },
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                androidx.compose.material3.IconButton(
                                                                    onClick = {
                                                                        try {
                                                                            val mp = android.media.MediaPlayer().apply {
                                                                                setDataSource(java.io.File(context.filesDir, savedAudioFile).absolutePath)
                                                                                prepare()
                                                                                start()
                                                                            }
                                                                        } catch (e: Exception) {
                                                                            e.printStackTrace()
                                                                        }
                                                                    },
                                                                    modifier = Modifier.size(24.dp)
                                                                ) {
                                                                    Icon(imageVector = Icons.Rounded.PlayCircleFilled, contentDescription = "Preview", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                                }
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(
                                                                    text = savedAudioFile.removePrefix("ai_notification_saved_").substringBeforeLast("_"),
                                                                    fontSize = 12.sp
                                                                )
                                                            }
                                                            androidx.compose.material3.RadioButton(
                                                                selected = (soundMode == savedAudioFile),
                                                                onClick = {
                                                                    viewModel.updateNotificationSoundMode(savedAudioFile)
                                                                    viewModel.triggerButtonHaptic()
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            thickness = 1.dp
                        )

                        // Vibration switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Vibration,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = if (appLanguage == "el") "Δόνηση" else "Vibration",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (appLanguage == "el") "Ενεργοποίηση δόνησης συσκευής για ειδοποιήσεις" else "Trigger vibration sequences for reminders",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            androidx.compose.material3.Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = {
                                    viewModel.updateNotificationVibrationEnabled(it)
                                    viewModel.triggerToggleHaptic(reversed = !it)
                                },
                                modifier = Modifier.testTag("vibration_toggle_switch")
                            )
                        }

                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            thickness = 1.dp
                        )


                        // Do not disturb bypass switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RemoveCircleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = if (appLanguage == "el") "Παράκαμψη λειτουργίας Μην ενοχλείτε" else "Bypass Do Not Disturb",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (appLanguage == "el") "Να επιτραπεί σε αυτές τις ειδοποιήσεις να εξακολουθούν να διακόπτουν όταν η λειτουργία \"Μην ενοχλείτε\" είναι ενεργή" else "Allow water notifications to bypass DND mode and wake up screen",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            androidx.compose.material3.Switch(
                                checked = bypassDnd,
                                onCheckedChange = {
                                    viewModel.updateNotificationBypassDnd(it)
                                    viewModel.triggerToggleHaptic(reversed = !it)
                                },
                                modifier = Modifier.testTag("dnd_bypass_switch")
                            )
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(start = 54.dp, top = 8.dp, bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 54.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (appLanguage == "el") "Χωρίς ήχο κατά τη λειτουργία \"Μην ενοχλείτε\"" else "No sound while DND is on",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (appLanguage == "el") "Η ειδοποίηση θα εμφανίζεται αλλά χωρίς ήχο/δόνηση" else "Notification will appear but without sound or vibration",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                androidx.compose.material3.Switch(
                                    checked = dndNoSound,
                                    onCheckedChange = {
                                        viewModel.updateNotificationDndNoSound(it)
                                        viewModel.triggerToggleHaptic(reversed = !it)
                                    },
                                    modifier = Modifier.scale(0.85f).testTag("dnd_no_sound_switch")
                                )
                            }
                        }
                    }
                }
            }

            // 4. Button to open system app notifications settings (Pixel layout match exactly)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Test Notification Button
            androidx.compose.material3.Button(
                onClick = {
                    viewModel.triggerButtonHaptic()
                    com.pixelwater.app.notifications.NotificationHelper.showCustomNotification(
                        context, 
                        if (appLanguage == "el") "Δοκιμαστική Ειδοποίηση" else "Test Notification", 
                        if (appLanguage == "el") "Ώρα για ενυδάτωση!" else "Time to hydrate!"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("test_notification_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (appLanguage == "el") "Δοκιμαστική Ειδοποίηση" else "Send Test Notification",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.Button(
                onClick = {
                    viewModel.triggerButtonHaptic()
                    val intent = android.content.Intent().apply {
                        action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Settings could not be opened: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("system_notification_settings_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (appLanguage == "el") "Άνοιγμα ρυθμίσεων συστήματος" else "System Notification Settings",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
