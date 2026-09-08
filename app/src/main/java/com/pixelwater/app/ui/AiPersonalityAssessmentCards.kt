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
fun AiMbtiProfileCard(
    viewModel: WaterViewModel,
    appLanguage: String,
    modifier: Modifier = Modifier
) {
    val userMbti by viewModel.userMbti.collectAsStateWithLifecycle()
    var manualText by remember { mutableStateOf(userMbti) }
    var showTestDialog by remember { mutableStateOf(false) }
    var showExtendedTestDialog by remember { mutableStateOf(false) }
    var isListExpanded by remember { mutableStateOf(false) }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    LaunchedEffect(userMbti) {
        manualText = userMbti
    }

    ChunkySettingCard {
        Column(
            modifier = modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (appLanguage == "el") "Τύπος Προσωπικότητας MBTI" else "MBTI Personality Type",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = if (appLanguage == "el")
                    "Προσαρμόστε τις απαντήσεις του AI Coach και των Hydration Insights βάσει του χαρακτήρα σας. Εισάγετε τον τύπο σας χειροκίνητα ή κάντε το ειδικό τεστ 5 λεπτών."
                    else "Tailor the AI Coach & Hydration Insights to match your exact psychological behavior. Enter your MBTI code below or complete the 5-minute personality test.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualText,
                    onValueChange = { input ->
                        val uppercaseInput = input.uppercase()
                        val filtered = uppercaseInput.filter { c -> c.isLetter() || c == '-' }.take(7)
                        manualText = filtered
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(if (appLanguage == "el") "Τύπος MBTI (π.χ. INFJ-OC)" else "MBTI Type (e.g. INFJ-OC)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (manualText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    viewModel.triggerButtonHaptic()
                                    if (manualText.length >= 4) {
                                        viewModel.updateUserMbti(manualText)
                                    }
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                },
                                enabled = (manualText.length >= 4)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Done,
                                    contentDescription = "Save MBTI",
                                    tint = if (manualText.length >= 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                )

                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.triggerButtonHaptic()
                        showTestDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Face,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (appLanguage == "el") "Τεστ MBTI" else "Take Test",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Expandable selector list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { 
                        viewModel.triggerButtonHaptic()
                        isListExpanded = !isListExpanded 
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (appLanguage == "el") "Επιλογή από τη λίστα (16 χαρακτήρες)" else "Choose from the 16 types directly",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (isListExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (isListExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    val groups = listOf(
                        Triple(
                            if (appLanguage == "el") "ΑΝΑΛΥΤΕΣ (Intellectual & Precise)" else "ANALYSTS (Intellectual & Precise)", 
                            listOf("INTJ", "INTP", "ENTJ", "ENTP"),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        Triple(
                            if (appLanguage == "el") "ΔΙΠΛΩΜΑΤΕΣ (Warm & Empathetic)" else "DIPLOMATS (Warm & Empathetic)", 
                            listOf("INFJ", "INFP", "ENFJ", "ENFP"),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                        ),
                        Triple(
                            if (appLanguage == "el") "ΦΥΛΑΚΕΣ (Practical & Structured)" else "SENTINELS (Practical & Structured)", 
                            listOf("ISTJ", "ISFJ", "ESTJ", "ESFJ"),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        ),
                        Triple(
                            if (appLanguage == "el") "ΕΞΕΡΕΥΝΗΤΕΣ (Action-oriented & Energetic)" else "EXPLORERS (Action-oriented & Energetic)", 
                            listOf("ISTP", "ISFP", "ESTP", "ESFP"),
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    groups.forEach { (catName, types, containerColor) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = catName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                types.forEach { t ->
                                    val isSelected = userMbti.startsWith(t)
                                    val cornerShape = RoundedCornerShape(if (isSelected) 24.dp else 10.dp)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(cornerShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else containerColor)
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = cornerShape
                                            )
                                            .clickable {
                                                viewModel.triggerButtonHaptic()
                                                val suffix = if (userMbti.contains("-") && userMbti.substringAfter("-").length == 2) {
                                                    "-" + userMbti.substringAfter("-")
                                                } else {
                                                    "" // Let it be just 4 if they don't have a 6-axis setup
                                                }
                                                val finalVal = t + suffix
                                                manualText = finalVal
                                                viewModel.updateUserMbti(finalVal)
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = t,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Optional 6-Axis Extractor
                    Spacer(modifier = Modifier.height(4.dp))
                    var isExtendedAxesExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.triggerButtonHaptic()
                                isExtendedAxesExpanded = !isExtendedAxesExpanded
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (appLanguage == "el") "Προαιρετικό: Community 6-Axis MBTI" else "Optional: Community 6-Axis MBTI",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = if (isExtendedAxesExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (isExtendedAxesExpanded) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 8.dp, end = 8.dp)
                        ) {
                            Text(
                                text = if (appLanguage == "el") "Άξονας 5 (Προσαρμογή):" else "Axis 5 (Adaptation):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val currentA5 = if (userMbti.contains("-") && userMbti.substringAfter("-").isNotEmpty()) userMbti.substringAfter("-")[0] else ' '
                                listOf('A' to (if (appLanguage == "el") "Assertive (Σταθερός)" else "Assertive"), 'O' to (if (appLanguage == "el") "Oscillating (Ευέλικτος)" else "Oscillating")).forEach { (char, label) ->
                                    val isSelected = currentA5 == char
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.3f))
                                            .clickable {
                                                viewModel.triggerButtonHaptic()
                                                val base = if (userMbti.length >= 4) userMbti.take(4) else ""
                                                if (base.isNotEmpty()) {
                                                    val currentSuffix = if (userMbti.contains("-")) userMbti.substringAfter("-") else ""
                                                    val c6 = if (currentSuffix.length > 1) currentSuffix[1] else 'H'
                                                    val newVal = "$base-$char$c6"
                                                    manualText = newVal
                                                    viewModel.updateUserMbti(newVal)
                                                }
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Text(
                                text = if (appLanguage == "el") "Άξονας 6 (Συναισθηματικός Πυρήνας):" else "Axis 6 (Emotional Core):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val currentA6 = if (userMbti.contains("-") && userMbti.substringAfter("-").length > 1) userMbti.substringAfter("-")[1] else ' '
                                listOf('H' to (if (appLanguage == "el") "Harmony (Αρμονία)" else "Harmony"), 'C' to (if (appLanguage == "el") "Calm (Ήρεμος)" else "Calm")).forEach { (char, label) ->
                                    val isSelected = currentA6 == char
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.3f))
                                            .clickable {
                                                viewModel.triggerButtonHaptic()
                                                val base = if (userMbti.length >= 4) userMbti.take(4) else ""
                                                if (base.isNotEmpty()) {
                                                    val currentSuffix = if (userMbti.contains("-")) userMbti.substringAfter("-") else ""
                                                    val c5 = if (currentSuffix.isNotEmpty()) currentSuffix[0] else 'A'
                                                    val newVal = "$base-$c5$char"
                                                    manualText = newVal
                                                    viewModel.updateUserMbti(newVal)
                                                }
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.material3.Button(
                                onClick = { showExtendedTestDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (appLanguage == "el") "Μίνι Τεστ Προέκτασης (2 άξονες)" else "Take Mini 2-Axis Extension Test",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            // Expandable info section for MBTI
            var isInfoExpanded by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.triggerButtonHaptic(); isInfoExpanded = !isInfoExpanded }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (appLanguage == "el") "Τι είναι το MBTI και οι 6 Άξονες;" else "How MBTI 6-Axis AI Coaching works",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (isInfoExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                if (isInfoExpanded) {
                    Text(
                        text = if (appLanguage == "el")
                            "Το στάνταρ MBTI (4 άξονες) οργανώνει την προσωπικότητά σας σε 16 τύπους βάσει του πώς λαμβάνετε αποφάσεις και επεξεργάζεστε πληροφορίες. Ο AI Coach το χρησιμοποιεί για να προσαρμόσει τον τόνο και τη γλώσσα των επιστημονικών insights υδάτωσης.\n\n" +
                            "Η κοινότητα (community) έχει εισάγει 2 επιπλέον άξονες (6-Axis MBTI Extension):\n" +
                            "• Άξονας 5 (Προσαρμογή): Assertive (κλειδωμένος στη ρουτίνα) ή Oscillating (δυναμικός και ευέλικτος).\n" +
                            "• Άξονας 6 (Συναισθηματικός Πυρήνας): Harmony (ενσυναίσθηση) ή Calm (αποστασιοποιημένος και απόλυτα λογικός).\n\n" +
                            "Μέσω αυτών, το AI δεν σας μιλάει απλά ως 'ένα άτομο', αλλά σαν συνεργάτης προσαρμοσμένος ακριβώς στην ψυχολογία σας!"
                            else 
                            "Standard MBTI (4 axes) classifies your core personality choices into 16 types, dictating how you prefer to process incoming information and make decisions. The AI uses this to alter the tone and delivery style of daily hydration insights.\n\n" +
                            "The community leverages a 6-Axis Expansion:\n" +
                            "• Axis 5 (Adaptation): Assertive (confident/structured) vs Oscillating (fluid/adaptive).\n" +
                            "• Axis 6 (Emotional Core): Harmony (seeks deep connection) vs Calm (detached/stoic logic).\n\n" +
                            "This empowers the AI Coach to form an incredibly profound and accurate psychological bond perfectly suited to your habits and mindset.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    if (showTestDialog) {
        MbtiAssessmentDialog(
            appLanguage = appLanguage,
            onDismiss = { showTestDialog = false },
            onApplyMbti = { assessmentResult ->
                val suffix = if (userMbti.contains("-")) "-" + userMbti.substringAfter("-") else "-AH"
                val finalVal = assessmentResult + suffix
                viewModel.updateUserMbti(finalVal)
                showTestDialog = false
            }
        )
    }

    if (showExtendedTestDialog) {
        ExtendedMbtiAssessmentDialog(
            appLanguage = appLanguage,
            onDismiss = { showExtendedTestDialog = false },
            onApplyMbti = { assessmentResult ->
                val base = if (userMbti.length >= 4) userMbti.take(4) else "INTJ"
                viewModel.updateUserMbti("$base-$assessmentResult")
                showExtendedTestDialog = false
            }
        )
    }
}

data class BigFiveTestQuestion(
    val id: Int,
    val textEn: String,
    val textEl: String,
    val dimension: String, // "O", "C", "E", "A", "N"
    val weight: Int // 1 or -1
)

@Composable
fun AiBigFiveProfileCard(
    viewModel: WaterViewModel,
    appLanguage: String,
    modifier: Modifier = Modifier
) {
    val oVal by viewModel.userBigFiveO.collectAsStateWithLifecycle()
    val cVal by viewModel.userBigFiveC.collectAsStateWithLifecycle()
    val eVal by viewModel.userBigFiveE.collectAsStateWithLifecycle()
    val aVal by viewModel.userBigFiveA.collectAsStateWithLifecycle()
    val nVal by viewModel.userBigFiveN.collectAsStateWithLifecycle()

    var showTestDialog by remember { mutableStateOf(false) }
    var isEditingScores by remember { mutableStateOf(false) }

    ChunkySettingCard {
        Column(
            modifier = modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (appLanguage == "el") "Χαρακτηριστικά Προσωπικότητας Big Five (OCEAN)" else "Big Five Personality Traits (OCEAN)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = if (appLanguage == "el")
                    "Συνδυάστε τα MBTI με τα χαρακτηριστικά Big Five (Δεκτικότητα, Συνειδητότητα, Εξωστρέφεια, Συνεργατικότητα, Νευρωτισμός) για ακόμα πιο ακριβή insights υδάτωσης."
                    else "Synergize MBTI with Big Five traits (Openness, Conscientiousness, Extraversion, Agreeableness, Neuroticism) to generate hyper-personalized coaching insights.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )

            if (oVal == -1 || cVal == -1 || eVal == -1 || aVal == -1 || nVal == -1) {
                // Not specified state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f))
                        .padding(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (appLanguage == "el") "Δεν έχει οριστεί προφίλ Big Five" else "Big Five Profile Unspecified",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = if (appLanguage == "el") "Κάντε το τεστ ή ορίστε τις τιμές για να ξεκλειδώσετε συνδυαστικά insights." else "Complete the 5-minute personality quiz or adjust sliders to enable deep synergized AI feedback.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Button(
                                onClick = {
                                    viewModel.triggerButtonHaptic()
                                    showTestDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                )
                            ) {
                                Text(if (appLanguage == "el") "Τεστ Big Five" else "Take Quiz", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    viewModel.triggerButtonHaptic()
                                    viewModel.updateBigFive(50, 50, 50, 50, 50)
                                    isEditingScores = true
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (appLanguage == "el") "Μη αυτόματα" else "Manual Set", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Specified state
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BigFiveProgressBarRow(
                        label = if (appLanguage == "el") "Δεκτικότητα (Openness)" else "Openness (O)",
                        value = oVal,
                        isEditing = isEditingScores,
                        onValueChange = { viewModel.updateBigFive(it, cVal, eVal, aVal, nVal) }
                    )
                    BigFiveProgressBarRow(
                        label = if (appLanguage == "el") "Συνειδητότητα (Conscientiousness)" else "Conscientiousness (C)",
                        value = cVal,
                        isEditing = isEditingScores,
                        onValueChange = { viewModel.updateBigFive(oVal, it, eVal, aVal, nVal) }
                    )
                    BigFiveProgressBarRow(
                        label = if (appLanguage == "el") "Εξωστρέφεια (Extraversion)" else "Extraversion (E)",
                        value = eVal,
                        isEditing = isEditingScores,
                        onValueChange = { viewModel.updateBigFive(oVal, cVal, it, aVal, nVal) }
                    )
                    BigFiveProgressBarRow(
                        label = if (appLanguage == "el") "Συνεργατικότητα (Agreeableness)" else "Agreeableness (A)",
                        value = aVal,
                        isEditing = isEditingScores,
                        onValueChange = { viewModel.updateBigFive(oVal, cVal, eVal, it, nVal) }
                    )
                    BigFiveProgressBarRow(
                        label = if (appLanguage == "el") "Νευρωτισμός / Ευαισθησία (Neuroticism)" else "Neuroticism (N)",
                        value = nVal,
                        isEditing = isEditingScores,
                        onValueChange = { viewModel.updateBigFive(oVal, cVal, eVal, aVal, it) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                viewModel.triggerButtonHaptic()
                                isEditingScores = !isEditingScores
                            }
                        ) {
                            Text(
                                text = if (isEditingScores) {
                                    if (appLanguage == "el") "Αποθήκευση" else "Save Adjustments"
                                } else {
                                    if (appLanguage == "el") "Προσαρμογή" else "Adjust Manually"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        androidx.compose.material3.TextButton(
                            onClick = {
                                viewModel.triggerButtonHaptic()
                                showTestDialog = true
                            }
                        ) {
                            Text(
                                text = if (appLanguage == "el") "Επανάληψη Τεστ" else "Retake Test",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))

                var isInfoExpanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                        .animateContentSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.triggerButtonHaptic(); isInfoExpanded = !isInfoExpanded }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (appLanguage == "el") "Τι είναι το Big Five / OCEAN;" else "How Big Five Synergy works",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (isInfoExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    if (isInfoExpanded) {
                        Text(
                            text = if (appLanguage == "el")
                                "Το μοντέλο Big Five (OCEAN) μετράει 5 βασικές διαστάσεις της προσωπικότητας σε μια κλίμακα 0-100:\n\n" +
                                "• Openness (Δεκτικότητα αποκλίσεων)\n" +
                                "• Conscientiousness (Συνειδητότητα)\n" +
                                "• Extraversion (Εξωστρέφεια)\n" +
                                "• Agreeableness (Συνεργατικότητα)\n" +
                                "• Neuroticism (Νευρωτισμός/Ευαισθησία)\n\n" +
                                "Συνδυάζεται με το 6-Axis MBTI για να υπολογίσει ο AI Coach ακριβώς πόσο αυστηρός, υποστηρικτικός ή αναλυτικός πρέπει να είναι!"
                                else 
                                "The Big Five (OCEAN) model measures 5 core personality scales from 0 to 100:\n\n" +
                                "• Openness (Idea tolerance)\n" +
                                "• Conscientiousness (Rule strictness)\n" +
                                "• Extraversion (Social energy)\n" +
                                "• Agreeableness (Empathy)\n" +
                                "• Neuroticism (Emotional sensitivity)\n\n" +
                                "It synergizes directly with the 6-Axis MBTI to allow the AI Coach to dynamically calculate the perfect emotional depth, toughness, and analytical strictness for you.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (showTestDialog) {
        BigFiveAssessmentDialog(
            appLanguage = appLanguage,
            onDismiss = { showTestDialog = false },
            onApply = { o, c, e, a, n ->
                viewModel.updateBigFive(o, c, e, a, n)
                showTestDialog = false
            }
        )
    }
}

@Composable
fun BigFiveProgressBarRow(
    label: String,
    value: Int,
    isEditing: Boolean,
    onValueChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text("$value%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
        }

        if (isEditing) {
            androidx.compose.material3.Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..100f,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.tertiary,
                    activeTrackColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            )
        } else {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { value / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun BigFiveAssessmentDialog(
    appLanguage: String,
    onDismiss: () -> Unit,
    onApply: (Int, Int, Int, Int, Int) -> Unit
) {
    val qList = remember {
        listOf(
            BigFiveTestQuestion(1, "I have a rich imagination and appreciate beauty.", "Έχω πλούσια φαντασία και εκτιμώ την ομορφιά.", "O", 1),
            BigFiveTestQuestion(2, "I am always fully prepared and highly organized.", "Είμαι πάντα πλήρως προετοιμασμένος και εξαιρετικά οργανωμένος.", "C", 1),
            BigFiveTestQuestion(3, "I feel comfortable, energetic, and talkative around others.", "Νιώθω άνετα, δραστήριος και ομιλητικός γύρω από άλλους.", "E", 1),
            BigFiveTestQuestion(4, "I feel a great deal of deep sympathy and concern for others.", "Νιώθω βαθιά συμπόνια και ενδιαφέρον για τους άλλους ανθρώπους.", "A", 1),
            BigFiveTestQuestion(5, "I get stressed, anxious, or overwhelmed easily under pressure.", "Αγχώνομαι, ανησυχώ ή πνίγομαι εύκολα υπό πίεση.", "N", 1),
            
            BigFiveTestQuestion(6, "I am deeply interested in abstract or theoretical concepts.", "Ενδιαφέρομαι βαθιά για αφηρημένες ή θεωρητικές έννοιες.", "O", 1),
            BigFiveTestQuestion(7, "I sometimes neglect tasks or leave chores unfinished.", "Μερικές φορές παραμελώ εργασίες ή αφήνω δουλειές ημιτελείς.", "C", -1),
            BigFiveTestQuestion(8, "I active enjoy being the center of attention in groups.", "Απολαμβάνω δραστήρια να είμαι το επίκεντρο της προσοχής σε ομάδες.", "E", 1),
            BigFiveTestQuestion(9, "I treat other folks with polite courtesy, respect, and trust.", "Συμπεριφέρομαι στους άλλους με ευγένεια, σεβασμό και εμπιστοσύνη.", "A", 1),
            BigFiveTestQuestion(10, "I am incredibly relaxed and handle stress exceptionally well.", "Είμαι απίστευτα χαλαρός και διαχειρίζομαι το άγχος εξαιρετικά καλά.", "N", -1),
            
            BigFiveTestQuestion(11, "I prefer standard, familiar routines over new experiences.", "Προτιμώ τη σταθερή, γνώριμη ρουτίνα από τις νέες εμπειρίες.", "O", -1),
            BigFiveTestQuestion(12, "I pay extreme attention to small details and love checklists.", "Δίνω τεράστια προσοχή στις μικρές λεπτομέρειες και λατρεύω τις λίστες.", "C", 1),
            BigFiveTestQuestion(13, "I prefer quiet, introspective, and solitary contemplation.", "Προτιμώ την ήσυχη, εσωστρεφή και μοναχική σκέψη.", "E", -1),
            BigFiveTestQuestion(14, "I sometimes prioritize competition and my goals over cooperation.", "Μερικές φορές δίνω προτεραιότητα στον ανταγωνισμό έναντι της συνεργασίας.", "A", -1),
            BigFiveTestQuestion(15, "My mood changes very frequently and my emotions run high.", "Η διάθεσή μου αλλάζει πολύ συχνά και τα συναισθήματά μου είναι έντονα.", "N", 1)
        )
    }

    var currentIdx by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, Int>() }
    var showsResultStage by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (appLanguage == "el") "Τεστ Big Five 5 Λεπτών 🧠" else "5-Minute Big Five Quiz 🧠",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!showsResultStage) {
                    // Question stage
                    val progress = (currentIdx + 1).toFloat() / qList.size
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (appLanguage == "el") "Ερώτηση ${currentIdx + 1} από ${qList.size}" else "Question ${currentIdx + 1} of ${qList.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val currentQ = qList[currentIdx]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (appLanguage == "el") currentQ.textEl else currentQ.textEn,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 28.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 1-5 scales
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val activeSelection = answers[currentQ.id]

                        val choices = listOf(
                            1 to (if (appLanguage == "el") "Διαφωνώ Απόλυτα" else "Strongly Disagree"),
                            2 to (if (appLanguage == "el") "Μάλλον Διαφωνώ" else "Disagree"),
                            3 to (if (appLanguage == "el") "Ουδέτερο" else "Neutral"),
                            4 to (if (appLanguage == "el") "Μάλλον Συμφωνώ" else "Agree"),
                            5 to (if (appLanguage == "el") "Συμφωνώ Απόλυτα" else "Strongly Agree")
                        )

                        choices.forEach { (optionVal, labelText) ->
                            val isSelected = activeSelection == optionVal
                            androidx.compose.material3.Button(
                                onClick = {
                                    answers[currentQ.id] = optionVal
                                    if (currentIdx < qList.size - 1) {
                                        currentIdx++
                                    } else {
                                        showsResultStage = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(text = labelText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                if (currentIdx > 0) {
                                    currentIdx--
                                }
                            },
                            enabled = currentIdx > 0
                        ) {
                            Text(if (appLanguage == "el") "Προηγούμενο" else "Back")
                        }

                        androidx.compose.material3.TextButton(onClick = onDismiss) {
                            Text(if (appLanguage == "el") "Ακύρωση" else "Cancel", color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    // Result stage: Calculate & Display results
                    val oSum = remember { mutableStateOf(0) }
                    val cSum = remember { mutableStateOf(0) }
                    val eSum = remember { mutableStateOf(0) }
                    val aSum = remember { mutableStateOf(0) }
                    val nSum = remember { mutableStateOf(0) }

                    LaunchedEffect(Unit) {
                        var oScore = 0
                        var cScore = 0
                        var eScore = 0
                        var aScore = 0
                        var nScore = 0

                        qList.forEach { q ->
                            val ansValue = answers[q.id] ?: 3
                            val finalScore = if (q.weight == 1) ansValue else (6 - ansValue)
                            when (q.dimension) {
                                "O" -> oScore += finalScore
                                "C" -> cScore += finalScore
                                "E" -> eScore += finalScore
                                "A" -> aScore += finalScore
                                "N" -> nScore += finalScore
                            }
                        }

                        // Map sum (which is 3 to 15) to percentage (0 to 100)
                        oSum.value = (((oScore - 3) / 12.0) * 100).toInt()
                        cSum.value = (((cScore - 3) / 12.0) * 100).toInt()
                        eSum.value = (((eScore - 3) / 12.0) * 100).toInt()
                        aSum.value = (((aScore - 3) / 12.0) * 100).toInt()
                        nSum.value = (((nScore - 3) / 12.0) * 100).toInt()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (appLanguage == "el") "Τα Αποτελέσματά σας 🎉" else "Your Personality Overview 🎉",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        BigFiveResultRow(
                            label = if (appLanguage == "el") "Δεκτικότητα (Openness)" else "Openness (O)",
                            value = oSum.value,
                            desc = if (appLanguage == "el") "Εκτίμηση νέων ιδεών, φαντασία & περιπέτεια" else "Intellectual curiosity, creative imagination, and appreciation for art/novelty"
                        )
                        BigFiveResultRow(
                            label = if (appLanguage == "el") "Συνειδητότητα (Conscientiousness)" else "Conscientiousness (C)",
                            value = cSum.value,
                            desc = if (appLanguage == "el") "Αυτοπειθαρχία, οργάνωση & στόχοι" else "Goal-driven discipline, meticulous reliability, and structural habit compliance"
                        )
                        BigFiveResultRow(
                            label = if (appLanguage == "el") "Εξωστρέφεια (Extraversion)" else "Extraversion (E)",
                            value = eSum.value,
                            desc = if (appLanguage == "el") "Κοινωνικότητα, ενέργεια & ενθουσιασμός" else "Sociability, social assertiveness, outer-world energy, and chatty style"
                        )
                        BigFiveResultRow(
                            label = if (appLanguage == "el") "Συνεργατικότητα (Agreeableness)" else "Agreeableness (A)",
                            value = aSum.value,
                            desc = if (appLanguage == "el") "Ενσυναίσθηση, ευγένεια & εμπιστοσύνη" else "Warm interpersonal trust, empathetic support, and peaceful team spirit"
                        )
                        BigFiveResultRow(
                            label = if (appLanguage == "el") "Νευρωτισμός (Neuroticism)" else "Neuroticism (N)",
                            value = nSum.value,
                            desc = if (appLanguage == "el") "Συναισθηματική ευαισθησία & αντίδραση στο άγχος" else "Emotional stress sensitivity, anxiety response, and moody variance"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    androidx.compose.material3.Button(
                        onClick = {
                            onApply(oSum.value, cSum.value, eSum.value, aSum.value, nSum.value)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (appLanguage == "el") "Αποδοχή και Αποθήκευση" else "Apply and Save Results", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BigFiveResultRow(
    label: String,
    value: Int,
    desc: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("$value%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
        }
        androidx.compose.material3.LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        )
        Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
    }
}

@Composable
fun MbtiAssessmentDialog(
    appLanguage: String,
    onDismiss: () -> Unit,
    onApplyMbti: (String) -> Unit
) {
    val qList = remember {
        listOf(
            MbtiTestQuestion(1, "You feel energized after spending time with active people.", "Νιώθετε γεμάτοι ενέργεια αφού περάσετε χρόνο με ομάδα δραστήριων ανθρώπων.", "EI", 1),
            MbtiTestQuestion(2, "You prefer spending your free time alone reading or contemplating.", "Προτιμάτε να περνάτε τον ελεύθερο χρόνο σας μόνοι για διάβασμα ή σκέψη.", "EI", -1),
            MbtiTestQuestion(3, "You love being the center of attention in social gatherings.", "Σας αρέσει να είστε το επίκεντρο της προσοχής σε κοινωνικές εκδηλώσεις.", "EI", 1),
            MbtiTestQuestion(4, "You prefer deep one-on-one conversations over group settings.", "Προτιμάτε βαθιές κατ' ιδίαν συζητήσεις αντί για ομαδικές συγκεντρώσεις.", "EI", -1),
            MbtiTestQuestion(5, "You tend to express yourself out loud before finishing processing.", "Τείνετε να εκφράζεστε φωναχτά προτού ολοκληρώσετε τη σκέψη σας.", "EI", 1),
            
            MbtiTestQuestion(6, "You look for facts and concrete reality rather than abstract theories.", "Αναζητάτε γεγονότα και χειροπιαστή πραγματικότητα παρά αφηρημένες θεωρίες.", "NS", -1),
            MbtiTestQuestion(7, "You focus heavily on future ideas and creative possibilities.", "Εστιάζετε έντονα σε μελλοντικές ιδέες και δημιουργικές δυνατότητες.", "NS", 1),
            MbtiTestQuestion(8, "You prefer standard step-by-step practical methods.", "Προτιμάτε τυποποιημένες, πρακτικές μεθόδους βήμα-προς-βήμα.", "NS", -1),
            MbtiTestQuestion(9, "You easily notice underlying symbols and patterns in life.", "Παρατηρείτε εύκολα υποκείμενα σύμβολα και μοτίβα στη ζωή.", "NS", 1),
            MbtiTestQuestion(10, "You value empirical common sense over theoretical concepts.", "Εκτιμάτε την εμπειρική κοινή λογική περισσότερο από θεωρητικές έννοιες.", "NS", -1),
            
            MbtiTestQuestion(11, "Logical analyzer decisions matter more to you than empathy.", "Οι αποφάσεις λογικής ανάλυσης μετράνε περισσότερο για εσάς από την ενσυναίσθηση.", "TF", 1),
            MbtiTestQuestion(12, "You strive for emotional harmony and validating others.", "Προσπαθείτε για συναισθηματική αρμονία και επιβεβαίωση των άλλων.", "TF", -1),
            MbtiTestQuestion(13, "Being completely honest is more important than comfort.", "Το να είστε απόλυτα ειλικρινείς είναι πιο σημαντικό από την προσωρινή άνεση.", "TF", 1),
            MbtiTestQuestion(14, "If friends get upset, you focus on feelings first before facts.", "Αν φίλοι αναστατωθούν, εστιάζετε στα συναισθήματα πριν από τα γεγονότα.", "TF", -1),
            MbtiTestQuestion(15, "In disagreements, you prioritize finding truth over harmony.", "Στις διαφωνίες, δίνετε προτεραιότητα στην εύρεση της αλήθειας παρά στην αρμονία.", "TF", 1),
            
            MbtiTestQuestion(16, "Having a detailed plan is essential before any trip or project.", "Το να έχετε ένα λεπτομερές σχέδιο είναι απαραίτητο πριν από κάθε ταξίδι ή έργο.", "JP", 1),
            MbtiTestQuestion(17, "You leave things open-ended and adapt spontaneously.", "Αφήνετε τα πράγματα ανοιχτά και προσαρμόζεστε αυθόρμητα.", "JP", -1),
            MbtiTestQuestion(18, "Strict schedules inspire you to complete milestones early.", "Τα αυστηρά χρονοδιαγράμματα σας εμπνέουν να ολοκληρώνετε στόχους νωρίτερα.", "JP", 1),
            MbtiTestQuestion(19, "You find structured, rigid planning to be highly restrictive.", "Θεωρείτε ότι ο πολύ δομημένος σχεδιασμός είναι εξαιρετικά περιοριστικός.", "JP", -1),
            MbtiTestQuestion(20, "Keeping your environment neat, decided, and closed gives peace.", "Το να διατηρείτε το περιβάλλον σας τακτοποιημένο και αποφασισμένο σας δίνει ειρήνη.", "JP", 1)
        )
    }

    var currentIdx by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, Int>() }
    var showsResultStage by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (appLanguage == "el") "Τεστ MBTI 5 Λεπτών 🧠" else "5-Minute MBTI Test 🧠",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!showsResultStage) {
                    val progress = (currentIdx) / qList.size.toFloat()
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (appLanguage == "el") "Ερώτηση ${currentIdx + 1} από ${qList.size}" else "Question ${currentIdx + 1} of ${qList.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val currentQuestion = qList[currentIdx]

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (appLanguage == "el") currentQuestion.textEl else currentQuestion.textEn,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val selectedValue = answers[currentQuestion.id] ?: 3
                    val scaleOptions = remember {
                        listOf(
                            1 to ("Strongly Disagree" to "Διαφωνώ Απόλυτα"),
                            2 to ("Disagree" to "Διαφωνώ"),
                            3 to ("Neutral" to "Ουδέτερο"),
                            4 to ("Agree" to "Συμφωνώ"),
                            5 to ("Strongly Agree" to "Συμφωνώ Απόλυτα")
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        scaleOptions.forEach { (optionVal, labels) ->
                            val isSelected = selectedValue == optionVal
                            val labelText = if (appLanguage == "el") labels.second else labels.first
                            
                            val containerCol = when (optionVal) {
                                1 -> if (isSelected) Color(0xFFE57373) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                2 -> if (isSelected) Color(0xFFFFB74D) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                3 -> if (isSelected) Color(0xFFB0BEC5) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                4 -> if (isSelected) Color(0xFF81C784) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                5 -> if (isSelected) Color(0xFF4DB6AC) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }

                            val contentCol = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        answers[currentQuestion.id] = optionVal
                                        if (currentIdx < qList.size - 1) {
                                            currentIdx++
                                        } else {
                                            showsResultStage = true
                                        }
                                    },
                                color = containerCol,
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = labelText,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = contentCol
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (currentIdx > 0) {
                                    currentIdx--
                                }
                            },
                            enabled = currentIdx > 0,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (appLanguage == "el") "Πίσω" else "Back")
                        }

                        if (currentIdx == qList.size - 1) {
                            androidx.compose.material3.Button(
                                onClick = { showsResultStage = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (appLanguage == "el") "Αποτέλεσμα" else "Done")
                            }
                        } else {
                            androidx.compose.material3.Button(
                                onClick = { currentIdx++ },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (appLanguage == "el") "Επόμενο" else "Next")
                            }
                        }
                    }
                } else {
                    var totalEI = 0
                    var totalNS = 0
                    var totalTF = 0
                    var totalJP = 0

                    qList.forEach { q ->
                        val ansScore = answers[q.id] ?: 3
                        val scaleWeight = ansScore - 3
                        val scoreContributed = scaleWeight * q.agreeWeight

                        when (q.dimension) {
                            "EI" -> totalEI += scoreContributed
                            "NS" -> totalNS += scoreContributed
                            "TF" -> totalTF += scoreContributed
                            "JP" -> totalJP += scoreContributed
                        }
                    }

                    val typeEorI = if (totalEI >= 0) "E" else "I"
                    val typeNorS = if (totalNS >= 0) "N" else "S"
                    val typeTorF = if (totalTF >= 0) "T" else "F"
                    val typeJorP = if (totalJP >= 0) "J" else "P"
                    
                    val calculatedMbtiBase = "$typeEorI$typeNorS$typeTorF$typeJorP"
                    val calculatedMbti = calculatedMbtiBase

                    val roleTitle = when (calculatedMbtiBase) {
                        "INTJ" -> "The Architect / Ο Αρχιτέκτονας"
                        "INTP" -> "The Logician / Ο Λογικός"
                        "ENTJ" -> "The Commander / Ο Αρχηγός"
                        "ENTP" -> "The Debater / Ο Ερευνητής"
                        "INFJ" -> "The Advocate / Ο Υπερασπιστής"
                        "INFP" -> "The Mediator / Ο Μεσολαβητής"
                        "ENFJ" -> "The Protagonist / Ο Πρωταγωνιστής"
                        "ENFP" -> "The Campaigner / Ο Ιδεολόγος"
                        "ISTJ" -> "The Logistician / Ο Επιθεωρητής"
                        "ISFJ" -> "The Defender / Ο Προστάτης"
                        "ESTJ" -> "The Executive / Ο Διοικητικός"
                        "ESFJ" -> "The Consul / Ο Πρόξενος"
                        "ISTP" -> "The Virtuoso / Ο Τεχνίτης"
                        "ISFP" -> "The Adventurer / Ο Περιπετειώδης"
                        "ESTP" -> "The Entrepreneur / Ο Επιχειρηματίας"
                        "ESFP" -> "The Entertainer / Ο Διασκεδαστής"
                        else -> "The Explorer"
                    }

                    val description = when (calculatedMbtiBase) {
                        "INTJ" -> "Strategic, logical, private, highly analytical. Enjoys systems, long-term plans, and cognitive efficiency."
                        "INTP" -> "Intellectual, objective, clinical, theoretical. Loves analyzing complex theories and abstract principles."
                        "ENTJ" -> "Assertive, decisive, strategic leader. Loves goal-setting, planning, efficiency, and driving results."
                        "ENTP" -> "Inventive, curious, intellectual challenger. Loves analyzing ideas, debating, and discovering new pathways."
                        "INFJ" -> "Idealistic, warm, intuitive advocate. Guides others with deep empathy, integrity, and profound insights."
                        "INFP" -> "Creative, poetic, values-driven mediator. Gentle and quiet, seeking beauty and deep authentic connections."
                        "ENFJ" -> "Charismatic, empathetic, and inspiring leader. Builds deep emotional consensus and values-based motivation."
                        "ENFP" -> "Vibrant, creative, idealistic enthusiast. Highly conversational, holistic, and deeply passionate."
                        "ISTJ" -> "Reliable, practical, detailed, precise Sentinel. Emphasizes duty, order, logical consistency, and routine."
                        "ISFJ" -> "Warm, dedicated, practical defender. Ensures steady routine, safety, harmony, and faithful service."
                        "ESTJ" -> "Practical, administrative executive. Loves discipline, hard work, steady habits, and clear organization."
                        "ESFJ" -> "Nurturing, practical consul. Creates absolute harmony, steady structures, and deeply social routines."
                        "ISTP" -> "Practical virtuoso. Spontaneous, tactical, highly active, and analytical tool-builder."
                        "ISFP" -> "Sensitive, quiet adventurer. Expresses themselves actionably, valuing organic beauty and spontaneous choices."
                        "ESTP" -> "Active element. Sharp, direct, funny, extremely fast, action-oriented, and dynamic."
                        "ESFP" -> "Joyous entertainer. Lives for the present moment, delivering action, humor, and energetic fun."
                        else -> "Unique personality type with exceptional capabilities."
                    }

                    val customGreekDesc = when (calculatedMbtiBase) {
                        "INTJ" -> "Στρατηγικός, λογικός, ιδιωτικός, εξαιρετικά αναλυτικός. Αγαπά τα συστήματα, τα μακροπρόθεσμα σχέδια και τη γνωστική αποτελεσματικότητα."
                        "INTP" -> "Διανοητικός, αντικειμενικός, θεωρητικός. Του αρέσει να αναλύει περίπλοκες θεωρίες και αφηρημένες αρχές."
                        "ENTJ" -> "Δυναμικός, αποφασιστικός, στρατηγικός ηγέτης. Αγαπά τον καθορισμό στόχων, τον σχεδιασμό και την απόδοση."
                        "ENTP" -> "Εφευρετικός, περίεργος, πνευματικός προκλητής. Του αρέσει να αναλύει ιδέες, να συζητά και να ανακαλύπτει νέες μεθόδους."
                        "INFJ" -> "Ιδεαλιστής, ζεστός, διαισθητικός υποστηρικτής. Καθοδηγεί με βαθιά ενσυναίσθηση, ακεραιότητα και βαθιά διορατικότητα."
                        "INFP" -> "Δημιουργικός, ποιητικός, καθοδηγούμενος από αξίες μεσολαβητής. Ευγενικός και ήσυχος, αναζητά ομορφιά και αυθεντικούς δεσμούς."
                        "ENFJ" -> "Χαρισματικός, ενσυναίσθητος και εμπνευσμένος ηγέτης. Δημιουργεί βαθιά συναίνεση και κίνητρα βασισμένα σε αξίες."
                        "ENFP" -> "Ζωντανός, δημιουργικός, ιδεαλιστής ενθουσιώδης. Εξαιρετικά επικοινωνιακός, ολιστικός και βαθιά παθιασμένος."
                        "ISTJ" -> "Αξιόπιστος, πρακτικός, λεπτομερής, ακριβής. Δίνει έμφαση στο καθήκον, την τάξη, τη συνέπεια και τη ρουτίνα."
                        "ISFJ" -> "Ζεστός, αφοσιωμένος, πρακτικός προστάτης. Διασφαλίζει σταθερή ρουτίνα, ασφάλεια, αρμονία και πιστή υπηρεσία."
                        "ESTJ" -> "Πρακτικός, οργανωτικός, διοικητικός. Αγαπά την πειθαρχία, τη σκληρή δουλειά, τις σταθερές συνήθειες και την καθαρή δομή."
                        "ESFJ" -> "Στοργικός, πρακτικός. Δημιουργεί απόλυτη αρμονία, σταθερές δομές και βαθιά κοινωνικές ρουτίνες."
                        "ISTP" -> "Πρακτικός, αυθόρμητος, εξαιρετικά δραστήριος και αναλυτικός επιλυτής προβλημάτων."
                        "ISFP" -> "Ευαίσθητος, ήσυχος περιπετειώδης. Εκφράζεται με πράξεις, εκτιμώντας την οργανική ομορφιά και τις αυθόρμητες επιλογές."
                        "ESTP" -> "Δραστήριος, άμεσος, αστείος, εξαιρετικά γρήγορος, προσανατολισμένος στη δράση και δυναμικός."
                        "ESFP" -> "Χαρούμενος διασκεδαστής. Ζει για την παρούσα στιγμή, προσφέροντας δράση, χιούμορ και ενέργεια."
                        else -> "Μοναδικός τύπος προσωπικότητας με εξαιρετικές ικανότητες."
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Face,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (appLanguage == "el") "Το Προφίλ σου είναι:" else "Your MBTI Type is:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = calculatedMbti,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )

                        Text(
                            text = roleTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (appLanguage == "el") customGreekDesc else description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        MbtiBar(label = if (appLanguage == "el") "Extraversion (E) vs Introversion (I)" else "Extraversion (E) vs Introversion (I)", value = (totalEI + 10) / 20.0f)
                        MbtiBar(label = if (appLanguage == "el") "Intuition (N) vs Sensing (S)" else "Intuition (N) vs Sensing (S)", value = (totalNS + 10) / 20.0f)
                        MbtiBar(label = if (appLanguage == "el") "Thinking (T) vs Feeling (F)" else "Thinking (T) vs Feeling (F)", value = (totalTF + 10) / 20.0f)
                        MbtiBar(label = if (appLanguage == "el") "Judging (J) vs Perceiving (P)" else "Judging (J) vs Perceiving (P)", value = (totalJP + 10) / 20.0f)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showsResultStage = false
                                currentIdx = 0
                                answers.clear()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (appLanguage == "el") "Επανάληψη" else "Retake")
                        }

                        androidx.compose.material3.Button(
                            onClick = { onApplyMbti(calculatedMbti) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (appLanguage == "el") "Εφαρμογή" else "Apply")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtendedMbtiAssessmentDialog(
    appLanguage: String,
    onDismiss: () -> Unit,
    onApplyMbti: (String) -> Unit
) {
    val qList = remember {
        listOf(
            MbtiTestQuestion(1, "You assert your opinions firmly and rarely change your mind constantly.", "Υποστηρίζετε σθεναρά τις απόψεις σας και σπάνια αλλάζετε γνώμη.", "AO", 1),
            MbtiTestQuestion(2, "You tend to change your mind frequently and adapt as you go.", "Τείνετε να αλλάζετε συχνά γνώμη και να προσαρμόζεστε στην πορεία.", "AO", -1),
            MbtiTestQuestion(3, "You prioritize harmony and emotional connection above all.", "Δίνετε προτεραιότητα στην αρμονία και τη συναισθηματική σύνδεση πάνω απ' όλα.", "HC", 1),
            MbtiTestQuestion(4, "You remain calm and detached even in highly emotional situations.", "Παραμένετε ήρεμοι και αποστασιοποιημένοι ακόμα και σε έντονα συναισθηματικές καταστάσεις.", "HC", -1)
        )
    }

    var currentIdx by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, Int>() }
    var showsResultStage by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (appLanguage == "el") "2-Axis Extensions" else "2-Axis Extensions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, null)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (!showsResultStage) {
                    val currentQ = qList[currentIdx]
                    
                    Text(
                        text = "${currentIdx + 1} / ${qList.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (appLanguage == "el") currentQ.textEl else currentQ.textEn,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 28.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (appLanguage == "el") "Διαφωνώ" else "Disagree", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        Text(if (appLanguage == "el") "Συμφωνώ" else "Agree", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val selectedValue = answers[currentQ.id] ?: 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { scaleVal ->
                            val isSelected = selectedValue == scaleVal
                            val size = when(scaleVal) {
                                1, 5 -> 48.dp
                                2, 4 -> 36.dp
                                else -> 24.dp
                            }
                            val baseColor = when(scaleVal) {
                                1, 2 -> MaterialTheme.colorScheme.error
                                4, 5 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(size)
                                    .background(if (isSelected) baseColor else Color.Transparent, CircleShape)
                                    .border(
                                        2.dp,
                                        if (isSelected) baseColor else baseColor.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                                    .clickable {
                                        answers[currentQ.id] = scaleVal
                                        if (currentIdx < qList.size - 1) {
                                            currentIdx++
                                        } else {
                                            showsResultStage = true
                                        }
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (currentIdx > 0) {
                                    currentIdx--
                                }
                            },
                            enabled = currentIdx > 0,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (appLanguage == "el") "Πίσω" else "Back")
                        }

                        if (currentIdx == qList.size - 1) {
                            androidx.compose.material3.Button(
                                onClick = { showsResultStage = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (appLanguage == "el") "Αποτέλεσμα" else "Done")
                            }
                        } else {
                            androidx.compose.material3.Button(
                                onClick = { currentIdx++ },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (appLanguage == "el") "Επόμενο" else "Next")
                            }
                        }
                    }
                } else {
                    var totalAO = 0
                    var totalHC = 0

                    qList.forEach { q ->
                        val ansScore = answers[q.id] ?: 3
                        val scaleWeight = ansScore - 3
                        val scoreContributed = scaleWeight * q.agreeWeight

                        when (q.dimension) {
                            "AO" -> totalAO += scoreContributed
                            "HC" -> totalHC += scoreContributed
                        }
                    }

                    val typeAorO = if (totalAO >= 0) "A" else "O"
                    val typeHorC = if (totalHC >= 0) "H" else "C"
                    
                    val calculatedSuffix = "$typeAorO$typeHorC"

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (appLanguage == "el") "Η Προέκταση σου είναι:" else "Your Extension is:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = calculatedSuffix,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        MbtiBar(label = if (appLanguage == "el") "Assertive (A) vs Oscillating (O)" else "Assertive (A) vs Oscillating (O)", value = (totalAO + 4) / 8.0f)
                        MbtiBar(label = if (appLanguage == "el") "Harmony (H) vs Calm (C)" else "Harmony (H) vs Calm (C)", value = (totalHC + 4) / 8.0f)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showsResultStage = false
                                currentIdx = 0
                                answers.clear()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (appLanguage == "el") "Επανάληψη" else "Retake")
                        }

                        androidx.compose.material3.Button(
                            onClick = { onApplyMbti(calculatedSuffix) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (appLanguage == "el") "Εφαρμογή" else "Apply")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MbtiBar(label: String, value: Float) {
    val clamped = value.coerceIn(0f, 1f)
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label.substringBefore(" vs"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label.substringAfter("vs "), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = clamped,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.secondaryContainer
        )
    }
}
