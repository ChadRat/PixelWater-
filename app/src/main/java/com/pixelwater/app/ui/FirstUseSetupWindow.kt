package com.pixelwater.app.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelwater.app.ui.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstUseSetupWindow(
    viewModel: WaterViewModel
) {
    val firstUseSetupCompleted by viewModel.firstUseSetupCompleted.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    if (!firstUseSetupCompleted) {
        var onboardingStep by remember { mutableStateOf(1) } // Step 1: Legal Disclaimer, Step 2: Personalization
        val isGreek = appLanguage == "el"

        Dialog(
            onDismissRequest = { /* Force completion of setup before dismissal */ },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (onboardingStep == 1) {
                    LegalDisclaimerStep(
                        viewModel = viewModel,
                        appLanguage = appLanguage,
                        onAccept = {
                            viewModel.triggerButtonHaptic()
                            onboardingStep = 2
                        }
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface // Solid, opaque background with no transparency
                    ) {
                        PersonalizationStep(
                            viewModel = viewModel,
                            appLanguage = appLanguage,
                            onBack = {
                                viewModel.triggerButtonHaptic()
                                onboardingStep = 1
                            },
                            onFinish = {
                                viewModel.triggerWoopHaptic()
                                viewModel.acceptLegalDisclaimer()
                                viewModel.completeFirstUseSetup()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegalDisclaimerStep(
    viewModel: WaterViewModel,
    appLanguage: String,
    onAccept: () -> Unit
) {
    val isGreek = appLanguage == "el"
    var hasAcceptedTerms by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Dynamic screen expansion on scroll & interactive dragging!
    var targetFraction by remember { mutableStateOf(1.0f) }
    var isManuallyResized by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(scrollState.value) {
        if (!isManuallyResized && scrollState.value > 15) {
            targetFraction = 1.0f
        }
    }

    val fraction by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "disclaimer_fraction"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxHeight.toPx() }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction),
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 28.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                ),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding() // Keep action buttons perfectly safe and readable above navigation bar
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Interactive drag handle area to resize
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                        .size(width = 120.dp, height = 24.dp) // Generous, comfortable touch target
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    isManuallyResized = true
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val fractionChange = -dragAmount.y / totalHeightPx
                                    targetFraction = (targetFraction + fractionChange).coerceIn(0.55f, 1.0f)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }

            // Android 17 / Pixel Inspired Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isGreek) "Νομική Αποδοχή" else "Legal Agreement",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = if (isGreek) {
                    "Καλώς ήλθατε στο Pixel Water. Για λόγους ασφάλειας και συμμόρφωσης, παρακαλούμε διαβάστε και αποδεχτείτε τις παρακάτω νομικές αποποιήσεις:"
                } else {
                    "Welcome to Pixel Water. For security and regulatory compliance, please carefully review and accept the legal disclaimers below to proceed:"
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Bento-style solid cards (No transparency)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Age limit
                LegalTermsCard(
                    icon = Icons.Rounded.Person,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = if (isGreek) "1. Νόμιμη Ηλικία (18+)" else "1. Legal Age Requirement (18+)",
                    content = if (isGreek) {
                        "Όλοι οι χρήστες πρέπει να είναι 18+ ετών. Οι ανήλικοι απαιτούν ρητή αποδοχή και επίβλεψη από γονείς ή κηδεμόνες."
                    } else {
                        "All users must be 18 years or older. Minor users require explicit acceptance and supervision by parents or legal guardians."
                    }
                )

                // Card 2: Medical Disclaimer
                LegalTermsCard(
                    icon = Icons.Rounded.HealthAndSafety,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = if (isGreek) "2. Ιατρική Αποποίηση" else "2. Medical Disclaimer",
                    content = if (isGreek) {
                        "Η εφαρμογή αυτή αποτελεί βοηθητικό εργαλείο καταγραφής και ενημέρωσης. Δεν παρέχει ιατρικές συμβουλές, διαγνώσεις ή θεραπείες."
                    } else {
                        "This app is a helper and educational tracking resource. It does not provide medical advice, diagnosis, or clinical treatment."
                    }
                )

                // Card 3: Overhydration Danger (Warm warning theme)
                LegalTermsCard(
                    icon = Icons.Rounded.Warning,
                    iconColor = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                    title = if (isGreek) "3. Κίνδυνος Υπερενυδάτωσης & Πίεσης Αίματος" else "3. Overhydration & Blood Pressure Warning",
                    content = if (isGreek) {
                        "Η υπερβολική ή εξαιρετικά γρήγορη κατανάλωση νερού μπορεί να προκαλέσει απότομη αύξηση της αρτηριακής πίεσης (υπερογκαιμική πίεση), αιμοαραίωση και υπονατριαιμία, που είναι επικίνδυνες για την υγεία. Καταναλώνετε υγρά με σύνεση."
                    } else {
                        "Drinking excessive amounts of water too rapidly can cause acute blood pressure surges (hypervolemic pressure), plasma dilution, and hyponatremia, which are dangerous to health. Please monitor your intake responsibly."
                    }
                )

                // Card 4: Limitation of Liability
                LegalTermsCard(
                    icon = Icons.Rounded.Shield,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    title = if (isGreek) "4. Περιορισμός Ευθύνης" else "4. Limitation of Liability",
                    content = if (isGreek) {
                        "Με τη χρήση της εφαρμογής συμφωνείτε ότι ο δημιουργός δεν φέρει καμία ευθύνη για τυχόν ζημίες. Αναλαμβάνετε 100% την προσωπική ευθύνη."
                    } else {
                        "By using this application, you agree that the developer is not liable for any health issues. You assume 100% personal responsibility."
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Android 17 style Chunky Clickable Agree Tile
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 2.dp,
                        color = if (hasAcceptedTerms) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { hasAcceptedTerms = !hasAcceptedTerms },
                colors = CardDefaults.cardColors(
                    containerColor = if (hasAcceptedTerms) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasAcceptedTerms,
                        onCheckedChange = { hasAcceptedTerms = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isGreek) {
                            "Βεβαιώνω ότι είμαι 18+ (ή έχω γονική συναίνεση) και συμφωνώ ρητά με όλους τους παραπάνω αναφερόμενους όρους και προϋποθέσεις ανεπιφύλακτα."
                        } else {
                            "I certify that I am 18+ (or have parental consent) and I explicitly agree with all the above stated terms and conditions unconditionally."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Action Buttons
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.triggerButtonHaptic()
                        // Exit app completely
                        (context as? android.app.Activity)?.finishAffinity()
                        java.lang.System.exit(0)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = if (isGreek) "Ακύρωση" else "Cancel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Button(
                    onClick = onAccept,
                    enabled = hasAcceptedTerms,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isGreek) "Αποδοχή & Συνέχεια" else "Accept & Continue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
}
}

@Composable
fun LegalTermsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    content: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = iconColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PersonalizationStep(
    viewModel: WaterViewModel,
    appLanguage: String,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    var selectedVisualEffects by remember { mutableStateOf("SHAPES") } // AURAGLOW, SHAPES, NONE
    var selectedColorTheme by remember { mutableStateOf("ADAPTIVE") } // ADAPTIVE, STATIC
    var glassThemeEnabled by remember { mutableStateOf(false) }

    val geminiKeyFlow by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    var geminiApiKeyInput by remember { mutableStateOf(geminiKeyFlow) }
    var showApiKey by remember { mutableStateOf(false) }
    val isGreek = appLanguage == "el"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header with Back Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isGreek) "Εξατομίκευση" else "Personalization",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isGreek) "Βήμα 2 από 2 • Ρυθμίσεις Εμφάνισης" else "Step 2 of 2 • Theme Customization",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: VISUAL EFFECTS / BACKDROP STYLE
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isGreek) "1. Στιλ Φόντου" else "1. Backdrop Visual Style",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Option 1: Aura Glow
                BackdropOptionCard(
                    title = if (isGreek) "Aura Glow (Αύρα)" else "Aura Glow",
                    desc = if (isGreek) "Ζωντανό, απαλό φόντο με περιστρεφόμενες λάμψεις." else "Ambient neon background with glowing rotating radial lights.",
                    icon = Icons.Rounded.Flare,
                    isSelected = selectedVisualEffects == "AURAGLOW",
                    onClick = {
                        selectedVisualEffects = "AURAGLOW"
                        viewModel.triggerButtonHaptic()
                    }
                )

                // Option 2: Materia Shapes
                BackdropOptionCard(
                    title = if (isGreek) "Materia Shapes" else "Materia Shapes",
                    desc = if (isGreek) "Διαδικαστικά τρισδιάστατα σχήματα που κινούνται απαλά." else "Beautiful procedural morphing shapes drifting softly.",
                    icon = Icons.Rounded.BubbleChart,
                    isSelected = selectedVisualEffects == "SHAPES",
                    onClick = {
                        selectedVisualEffects = "SHAPES"
                        viewModel.triggerButtonHaptic()
                    }
                )

                // Option 3: None
                BackdropOptionCard(
                    title = if (isGreek) "Κανένα" else "None",
                    desc = if (isGreek) "Καθαρό μονόχρωμο φόντο χωρίς εφέ κίνησης." else "Clean aesthetic dark/light themed solid backdrop.",
                    icon = Icons.Rounded.LayersClear,
                    isSelected = selectedVisualEffects == "NONE",
                    onClick = {
                        selectedVisualEffects = "NONE"
                        viewModel.triggerButtonHaptic()
                    }
                )
            }

            // SECTION 2: COLOR THEME STYLE
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isGreek) "2. Χρωματική Παλέτα" else "2. Color Theme Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Option A: Adaptive Theme
                BackdropOptionCard(
                    title = if (isGreek) "Προσαρμοστικό Χρώμα (Dynamic)" else "Adaptive Color Theme",
                    desc = if (isGreek) "Συγχρονίζεται με τα χρώματα της συσκευής σας (Android 12+)." else "Dynamic theme system that flows with your system wallpaper accent colors.",
                    icon = Icons.Rounded.ColorLens,
                    isSelected = selectedColorTheme == "ADAPTIVE",
                    onClick = {
                        selectedColorTheme = "ADAPTIVE"
                        viewModel.triggerButtonHaptic()
                    }
                )

                // Option B: Static Color Theme
                BackdropOptionCard(
                    title = if (isGreek) "Στατικό Χρώμα (Slate Theme)" else "Static Color Theme",
                    desc = if (isGreek) "Κομψή παλέτα Slate με κλασικά βαθιά ναυτικά χρώματα." else "A polished Slate theme featuring deep classic navy and modern blue accents.",
                    icon = Icons.Rounded.Palette,
                    isSelected = selectedColorTheme == "STATIC",
                    onClick = {
                        selectedColorTheme = "STATIC"
                        viewModel.triggerButtonHaptic()
                    }
                )
            }

            // Independent Glass option Toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = if (glassThemeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        glassThemeEnabled = !glassThemeEnabled
                        viewModel.triggerToggleHaptic()
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (glassThemeEnabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (glassThemeEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BlurOn,
                            contentDescription = null,
                            tint = if (glassThemeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGreek) "+ Εφέ Γυαλιού (Glass Theme)" else "+ Glass Theme Option",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isGreek) "Προσθέτει κομψή ημιδιαφάνεια και θολότητα στα στοιχεία." else "Enable beautiful glassy opacity and translucent blur styles (frosted-glass).",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = glassThemeEnabled,
                        onCheckedChange = {
                            glassThemeEnabled = it
                            viewModel.triggerToggleHaptic()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            // Gemini AI API Key Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = if (geminiApiKeyInput.isNotBlank()) Color(0xFFFF9100) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGreek) "Ρύθμιση Gemini AI" else "Gemini AI API Key Setup",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isGreek) "Ξεκλειδώστε προηγμένη καθοδήγηση & εβδομαδιαία insights" else "Unlock deep scientific Coaching & progress Insights",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        Button(
                            onClick = { uriHandler.openUri("https://aistudio.google.com/app/apikey") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isGreek) "Λήψη" else "Get Key",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = "Get API Key from Google AI Studio",
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = geminiApiKeyInput,
                        onValueChange = {
                            geminiApiKeyInput = it
                            viewModel.updateGeminiApiKey(it.trim())
                        },
                        placeholder = {
                            Text(
                                text = if (isGreek) "Εισάγετε το κλειδί API εδώ..." else "Enter your Gemini API key...",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.VpnKey,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = "Toggle key display",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = if (isGreek) {
                            "Εισαγάγετε ένα κλειδί API Gemini για να λαμβάνετε εξατομικευμένες επιστημονικές συμβουλές ενυδάτωσης από τον AI Coach."
                        } else {
                            "Add your Gemini API Key to enable scientific hydration recommendations from the AI Coach."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Submit Button
        Button(
            onClick = {
                // 1. Apply visual effects background choices
                when (selectedVisualEffects) {
                    "AURAGLOW" -> {
                        viewModel.updateAuraGlowEnabled(true)
                        viewModel.updateMaterialShapesEnabled(false)
                    }
                    "SHAPES" -> {
                        viewModel.updateMaterialShapesEnabled(true)
                        viewModel.updateAuraGlowEnabled(false)
                    }
                    "NONE" -> {
                        viewModel.updateAuraGlowEnabled(false)
                        viewModel.updateMaterialShapesEnabled(false)
                    }
                }

                // 2. Apply theme mode (adaptive dynamic, or static blue)
                when (selectedColorTheme) {
                    "ADAPTIVE" -> {
                        viewModel.updateAppTheme("DYNAMIC")
                    }
                    "STATIC" -> {
                        viewModel.updateAppTheme("STATIC")
                    }
                }

                // 3. Apply frosted glass theme option
                viewModel.updateFrostedGlassEnabled(glassThemeEnabled)

                // 3.5 Save Gemini API Key if entered during onboarding
                viewModel.updateGeminiApiKey(geminiApiKeyInput.trim())

                // 4. Finalize
                onFinish()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = if (isGreek) "Έναρξη Εφαρμογής 🚀" else "Get Started 🚀",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun BackdropOptionCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
