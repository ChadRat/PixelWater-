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

fun androidx.compose.foundation.lazy.LazyListScope.renderAICoachSection(
    viewModel: WaterViewModel,
    chatMessages: List<WaterViewModel.ChatMessage>,
    chatLoading: Boolean,
    chatError: String?,
    transparentComponentsEnabled: Boolean,
    componentsTransparency: Float
) {
    item {
        val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
        val isFrostedGlassEnabled by viewModel.isFrostedGlassEnabled.collectAsStateWithLifecycle()
        val frostedGlassTransparency by viewModel.frostedGlassTransparency.collectAsStateWithLifecycle()
        
        val isDark = androidx.compose.foundation.isSystemInDarkTheme() || MaterialTheme.colorScheme.background.luminance() < 0.5f
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pixel_color_shift_coach")
        val animatedOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(10000, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "pixel_gradient_offset_coach"
        )
        
        val primaryColor = MaterialTheme.colorScheme.primary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        
        val pixelBrush = Brush.linearGradient(
            colors = listOf(
                primaryColor,
                tertiaryColor,
                secondaryColor,
                primaryColor
            ),
            start = androidx.compose.ui.geometry.Offset(animatedOffset, 0f),
            end = androidx.compose.ui.geometry.Offset(animatedOffset + 400f, 400f)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("nero_ai_coach_card")
                .then(
                    if (isFrostedGlassEnabled) {
                        Modifier
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = frostedGlassTransparency),
                                        MaterialTheme.colorScheme.surface.copy(alpha = frostedGlassTransparency * 0.45f)
                                    )
                                ),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .border(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        Color.Transparent,
                                        Color.Transparent
                                    ),
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                ),
                                shape = RoundedCornerShape(32.dp)
                            )
                    } else Modifier
                ),
            shape = RoundedCornerShape(32.dp), // More rounded for modern feel
            colors = CardDefaults.cardColors(
                containerColor = if (isFrostedGlassEnabled) {
                    Color.Transparent
                } else if (transparentComponentsEnabled) {
                    MaterialTheme.colorScheme.surface.copy(alpha = componentsTransparency)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header of AI Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "AI Coach Logo",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (appLanguage == "el") "Σύμβουλος AI Nero" else "Nero AI Hydration Coach",
                            fontWeight = FontWeight.Black, // Extra bold
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val providerText = viewModel.aiProvider.collectAsStateWithLifecycle().value
                        val modelText = if (providerText == "Gemini") viewModel.geminiModel.collectAsStateWithLifecycle().value else ""
                        Text(
                            text = if (appLanguage == "el") 
                                "Υποστηρίζεται από $providerText ${if (modelText.isNotBlank()) "($modelText)" else ""}".trim()
                                else "Powered by $providerText ${if (modelText.isNotBlank()) "($modelText)" else ""}".trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // Clear chat button
                    IconButton(
                        onClick = { 
                            viewModel.clearChat()
                            viewModel.triggerButtonHaptic()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Clear Chat history",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Chat Messages Scroll Box
                val scrollState = rememberScrollState()
                
                LaunchedEffect(chatMessages.size) {
                    if (chatMessages.isNotEmpty()) {
                        scrollState.animateScrollTo(
                            value = scrollState.maxValue,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(
                            color = if (isFrostedGlassEnabled) {
                                MaterialTheme.colorScheme.surface.copy(alpha = frostedGlassTransparency * 0.4f)
                            } else if (transparentComponentsEnabled) {
                                MaterialTheme.colorScheme.surface.copy(alpha = componentsTransparency)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            if (isFrostedGlassEnabled) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (chatMessages.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = if (appLanguage == "el") "Πείτε γεια στον Nero!" else "Say hello to Nero!",
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        chatMessages.forEach { msg ->
                            val isUser = msg.isUser
                            val bubbleColor = if (isUser) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                            val textColor = if (isUser) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            
                            val alignment = if (isUser) Alignment.End else Alignment.Start
                            val bubbleShape = if (isUser) {
                                RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                            } else {
                                RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = alignment
                            ) {
                                Box(
                                    modifier = Modifier
                                        .shadow(elevation = 2.dp, shape = bubbleShape)
                                        .background(color = bubbleColor, shape = bubbleShape)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .widthIn(max = 260.dp)
                                ) {
                                    Text(
                                        text = parseMarkdown(msg.text),
                                        color = textColor,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                    
                    if (chatLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Card(
                                modifier = Modifier.padding(bottom = 8.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = if (appLanguage == "el") "Ο Nero προετοιμάζεται..." else "Nero is thinking...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Suggestions quick chips list
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (appLanguage == "el") "ΠΡΟΤΕΙΝΟΜΕΝΑ ΘΕΜΑΤΑ" else "SUGGESTED TOPICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        val suggestions = if (appLanguage == "el") {
                             listOf(
                                Triple("Εβδομαδιαίες Τάσεις", Icons.Rounded.Timeline, "Αναλύστε το ιστορικό των καταγραφών μου για την τελευταία εβδομάδα, εξηγήστε τις τάσεις και εντοπίστε τομείς βελτίωσης."),
                                Triple("Ενυδάτωση & Προπόνηση", Icons.Rounded.FitnessCenter, "Ποιες είναι οι βέλτιστες πρακτικές ενυδάτωσης πριν, κατά τη διάρκεια και μετά την άσκηση;"),
                                Triple("Καφές vs Νερό", Icons.Rounded.Coffee, "Πώς συγκρίνεται ο καφές και το τσάι με το νερό όσον αφορά την ενυδάτωση;"),
                                Triple("Πρωινή Ρουτίνα", Icons.Rounded.WbSunny, "Γιατί είναι σημαντικό να πίνουμε νερό αμέσως μόλις ξυπνάμε;"),
                                Triple("Υγεία Δέρματος", Icons.Rounded.Face, "Πώς επηρεάζει η ενυδάτωση την υγεία και την εμφάνιση του δέρματος;"),
                                Triple("Ηλεκτρολύτες", Icons.Rounded.FlashOn, "Πότε πρέπει να προσθέτω ηλεκτρολύτες στο νερό μου;")
                            )
                        } else {
                            listOf(
                                Triple("Weekly Trends", Icons.Rounded.Timeline, "Please analyze my water logs history for the past week, explain trends, highlight strengths, and identify areas to improve."),
                                Triple("Workout Fuel", Icons.Rounded.FitnessCenter, "What are the hydration best practices before, during, and after a vigorous exercise session to maximize energy?"),
                                Triple("Coffee vs Water", Icons.Rounded.Coffee, "Tell me how coffee, tea, and soda compare to water in terms of actual body hydration efficiency."),
                                Triple("Morning Routine", Icons.Rounded.WbSunny, "Why is it critical to rehydrate immediately upon waking up after sleep?"),
                                Triple("Skin Vitality", Icons.Rounded.Face, "Explain the scientific link between high hydration levels and skin elasticity and health."),
                                Triple("Electrolyte Balance", Icons.Rounded.FlashOn, "Under what conditions should I supplement my water with electrolytes or minerals?")
                            )
                        }

                        suggestions.forEach { triple ->
                            item {
                                var isActive by remember { mutableStateOf(false) }
                                val scope = rememberCoroutineScope()
                                var resetJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
                                val scaleAnim = remember { androidx.compose.animation.core.Animatable(1f) }

                                val cornerRadius by animateDpAsState(
                                    targetValue = if (isActive) 100.dp else 24.dp,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "suggested_radius_coach"
                                )

                                val baseCardColor = LocalBaseCardColor.current ?: MaterialTheme.colorScheme.surfaceVariant
                                val containerColor by animateColorAsState(
                                    targetValue = if (isActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        if (isFrostedGlassEnabled) {
                                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                                        } else {
                                            baseCardColor
                                        }
                                    },
                                    animationSpec = tween(250),
                                    label = "suggested_container_color_coach"
                                )

                                val contentColor by animateColorAsState(
                                    targetValue = if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                    animationSpec = tween(250),
                                    label = "suggested_content_color_coach"
                                )

                                val iconTint by animateColorAsState(
                                    targetValue = if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                    animationSpec = tween(250),
                                    label = "suggested_icon_color_coach"
                                )

                                val iconBg by animateColorAsState(
                                    targetValue = if (isDark) Color.Black.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                    animationSpec = tween(250),
                                    label = "suggested_icon_bg_coach"
                                )

                                Column(
                                    modifier = Modifier
                                        .width(125.dp)
                                        .height(60.dp)
                                        .scale(scaleAnim.value)
                                        .clip(RoundedCornerShape(maxOf(0f, cornerRadius.value).dp))
                                        .background(color = MaterialTheme.colorScheme.primary)
                                        .clickable {
                                            viewModel.triggerButtonHaptic()
                                            viewModel.sendChatMessage(triple.third)
                                            isActive = true
                                            scope.launch {
                                                scaleAnim.animateTo(0.85f, animationSpec = tween(50))
                                                scaleAnim.animateTo(1.08f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 300f))
                                                scaleAnim.animateTo(1.0f, animationSpec = spring())
                                            }
                                            resetJob?.cancel()
                                            resetJob = scope.launch {
                                                delay(1500)
                                                isActive = false
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(iconBg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = triple.second,
                                            contentDescription = triple.first,
                                            tint = iconTint,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = triple.first,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 1.dp)

                // Send Message Input Area
                var userTextState by remember { mutableStateOf("") }
                
                val baseCardColor = LocalBaseCardColor.current ?: MaterialTheme.colorScheme.surfaceVariant
                val dialogBoxColor = if (isFrostedGlassEnabled) {
                    if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                } else {
                    baseCardColor
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Input Bar (Pill shaped)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = dialogBoxColor,
                                shape = RoundedCornerShape(28.dp)
                            )
                            .border(
                                width = 1.dp,
                                brush = if (isFrostedGlassEnabled) {
                                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))
                                } else {
                                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)))
                                },
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = userTextState,
                            onValueChange = { userTextState = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gemini_chat_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = if (isDark) Color.White else Color.Black,
                                fontSize = 15.sp
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    val trimmed = userTextState.trim()
                                    if (trimmed.isNotEmpty() && !chatLoading) {
                                        viewModel.sendChatMessage(trimmed)
                                        userTextState = ""
                                        viewModel.triggerButtonHaptic()
                                    }
                                }
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(if (isDark) Color.White else Color.Black),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (userTextState.isEmpty()) {
                                        Text(
                                            text = if (appLanguage == "el") "Ρωτήστε τον Nero..." else "Ask Nero AI Coach...",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontSize = 15.sp,
                                            maxLines = 1
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        if (userTextState.isNotEmpty()) {
                            IconButton(
                                onClick = { 
                                    userTextState = "" 
                                    viewModel.triggerButtonHaptic()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear text",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Separate Send Button (Pill shaped with color-shifting Pixel gradient)
                    val isSendEnabled = userTextState.trim().isNotEmpty() && !chatLoading
                    
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .width(64.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .then(
                                if (isSendEnabled) {
                                    Modifier.background(brush = pixelBrush)
                                } else {
                                    Modifier
                                        .background(color = dialogBoxColor)
                                        .border(
                                            width = 1.dp,
                                            brush = if (isFrostedGlassEnabled) {
                                                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))
                                            } else {
                                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)))
                                            },
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                }
                            )
                            .clickable(enabled = isSendEnabled) {
                                val trimmed = userTextState.trim()
                                if (trimmed.isNotEmpty() && !chatLoading) {
                                    viewModel.sendChatMessage(trimmed)
                                    userTextState = ""
                                    viewModel.triggerButtonHaptic()
                                }
                            }
                            .testTag("gemini_send_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send prompt",
                            tint = if (isSendEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatDateShort(cal: java.util.Calendar, lang: String): String {
    val sdf = if (lang == "el") {
        java.text.SimpleDateFormat("d MMM", java.util.Locale("el"))
    } else {
        java.text.SimpleDateFormat("d MMM", java.util.Locale.US)
    }
    return sdf.format(cal.time)
}

fun getWeekdayLabel(dayOfWeek: Int, lang: String): String {
    return if (lang == "el") {
        when (dayOfWeek) {
            java.util.Calendar.MONDAY -> "Δ"
            java.util.Calendar.TUESDAY -> "Τ"
            java.util.Calendar.WEDNESDAY -> "Τ"
            java.util.Calendar.THURSDAY -> "Π"
            java.util.Calendar.FRIDAY -> "Π"
            java.util.Calendar.SATURDAY -> "Σ"
            java.util.Calendar.SUNDAY -> "Κ"
            else -> ""
        }
    } else {
        when (dayOfWeek) {
            java.util.Calendar.MONDAY -> "M"
            java.util.Calendar.TUESDAY -> "T"
            java.util.Calendar.WEDNESDAY -> "W"
            java.util.Calendar.THURSDAY -> "T"
            java.util.Calendar.FRIDAY -> "F"
            java.util.Calendar.SATURDAY -> "S"
            java.util.Calendar.SUNDAY -> "S"
            else -> ""
        }
    }
}

fun formatVolume(amountMl: Int, lang: String): String {
    return "$amountMl"
}

fun formatProposalItem(type: String, params: Map<String, String>, isGreek: Boolean): String {
    return when (type) {
        "setting" -> {
            val key = params["key"] ?: ""
            val rawVal = params["value"] ?: ""
            val keyLabel = when (key.lowercase()) {
                "daily_goal" -> if (isGreek) "Ημερήσιος Στόχος" else "Daily Goal"
                "quick_add" -> if (isGreek) "Γρήγορη Προσθήκη" else "Quick Add Amount"
                "fluid_ounces" -> if (isGreek) "Μονάδες Oz" else "Fluid Ounces"
                "reminders" -> if (isGreek) "Υπενθυμίσεις" else "Reminders"
                "reminder_interval" -> if (isGreek) "Διάστημα Υπενθυμίσεων" else "Reminder Interval"
                "theme_mode" -> if (isGreek) "Θέμα Εφαρμογής" else "Theme Mode"
                "oled_mode" -> if (isGreek) "Λειτουργία OLED" else "OLED Mode"
                "app_language" -> if (isGreek) "Γλώσσα Εφαρμογής" else "App Language"
                "fart_mode" -> if (isGreek) "Λειτουργία Φαρσών" else "Fart Mode"
                "blur_effect" -> if (isGreek) "Εφέ Θολώματος" else "Blur Effect"
                "rainbow_border" -> if (isGreek) "Ουράνιο Τόξο Περίγραμμα" else "Rainbow Border"
                "voice_mode" -> if (isGreek) "Φωνητική Καθοδήγηση" else "Voice Mode"
                "water_remaining" -> if (isGreek) "Εμφάνιση Υπολειπόμενου Νερού" else "Show Remaining Water"
                "haptic_mode" -> if (isGreek) "Λειτουργία Δόνησης" else "Haptics Level"
                else -> key
            }
            if (isGreek) "🔧 Αλλαγή ρύθμισης '$keyLabel' σε '$rawVal'" else "🔧 Change setting '$keyLabel' to '$rawVal'"
        }
        "add_water" -> {
            val amount = params["amount"] ?: "250"
            val beverage = params["beverage"] ?: "Water"
            if (isGreek) "💧 Προσθήκη $amount ml ($beverage)" else "💧 Add $amount ml ($beverage)"
        }
        "subtract_water" -> {
            val amount = params["amount"] ?: "250"
            if (isGreek) "🗑️ Αφαίρεση $amount ml" else "🗑️ Subtract $amount ml"
        }
        "create_drink" -> {
            val name = params["name"] ?: "Custom Drink"
            val factor = params["factor"] ?: "1.0"
            if (isGreek) "🍹 Δημιουργία ποτού '$name' (Συντελεστής: $factor)" else "🍹 Create saved drink '$name' (Factor: $factor)"
        }
        "change_color" -> {
            val element = params["element"] ?: ""
            val colorStr = params["color"] ?: "Custom"
            val elementLabel = when (element.lowercase()) {
                "static_theme_seed" -> if (isGreek) "Κύριο Θέμα" else "Primary Theme Color"
                "progress_circle" -> if (isGreek) "Κύκλος Προόδου" else "Progress Circle Indicator"
                "shape_a" -> if (isGreek) "Σχήμα Φόντου Α" else "Background Shape A"
                "shape_b" -> if (isGreek) "Σχήμα Φόντου Β" else "Background Shape B"
                "shape_c" -> if (isGreek) "Σχήμα Φόντου Γ" else "Background Shape C"
                "shape_d" -> if (isGreek) "Σχήμα Φόντου Δ" else "Background Shape D"
                else -> element
            }
            if (isGreek) "🎨 Αλλαγή χρώματος '$elementLabel' σε '$colorStr'" else "🎨 Change color of '$elementLabel' to '$colorStr'"
        }
        else -> type
    }
}

@Composable
fun AskGeminiScreen(
    viewModel: WaterViewModel,
    chatMessages: List<WaterViewModel.ChatMessage>,
    chatLoading: Boolean,
    chatError: String?,
    isDark: Boolean,
    isOledActive: Boolean,
    appLanguage: String
) {
    val emptyScrollState = rememberScrollState()
    val activeScrollState = rememberScrollState()
    val hasChats = chatMessages.size > 1 || (chatMessages.isNotEmpty() && chatMessages[0].isUser)
    val context = androidx.compose.ui.platform.LocalContext.current
    val pendingProposal by viewModel.pendingProposal.collectAsStateWithLifecycle()
    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    val isFrostedGlassEnabled = LocalFrostedGlassEnabled.current
    val frostedTransparency = LocalFrostedGlassTransparency.current

    // Photo picker launcher
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val resolver = context.contentResolver
            try {
                resolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    viewModel.setSelectedImage(uri.toString(), base64Str)
                }
            } catch (e: Exception) {
                Log.e("AskGeminiScreen", "Failed to load visual media", e)
            }
        }
    }

    LaunchedEffect(chatMessages.size, chatLoading) {
        if (hasChats) {
            kotlinx.coroutines.delay(100)
            activeScrollState.animateScrollTo(
                value = activeScrollState.maxValue,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // (Glow removed per request)

        Column(modifier = Modifier.fillMaxSize()) {
            // Elegant Matching Tab Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = if (appLanguage == "el") "Συζήτηση με τον Nero" else "Chat with Nero",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // 2. MAIN SCROLLABLE BODY
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (!hasChats) {
                    // Empty state layout modeled exactly off the second screenshot
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(emptyScrollState)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                } else {
                    // Chat flow screen when messages are loaded
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(activeScrollState)
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        chatMessages.forEachIndexed { index, msg ->
                            val isUser = msg.isUser
                            val bubbleShape = if (isUser) {
                                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
                            } else {
                                RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                            }

                            val bubbleColor = if (isFrostedGlassEnabled) {
                                if (isUser) {
                                    (if (isDark) Color(0xFF1E3A5F) else MaterialTheme.colorScheme.primaryContainer).copy(alpha = frostedTransparency)
                                } else {
                                    (if (isDark) Color(0xFF1F2023) else MaterialTheme.colorScheme.surfaceVariant).copy(alpha = frostedTransparency)
                                }
                            } else {
                                if (isUser) {
                                    if (isDark) Color(0xFF1E3A5F) else Color(0xFFD3E3FD)
                                } else {
                                    if (isDark) Color(0xFF232528) else Color(0xFFF1F3F4)
                                }
                            }

                            val bubbleBorderColor = if (isUser) {
                                if (isDark) Color(0xFF2B4C7E) else Color(0xFFB8D3FF)
                            } else {
                                if (isDark) Color(0xFF333538) else Color(0xFFE0E3E7)
                            }

                            val textColor = if (isUser) {
                                if (isDark) Color(0xFFE1F0FF) else Color(0xFF041E49)
                            } else {
                                if (isDark) Color(0xFFE3E2E6) else Color(0xFF1D1B20)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                if (!isUser) {
                                    NeroAvatar(
                                        isDark = isDark,
                                        sizeDp = 32.dp,
                                        modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = bubbleColor,
                                            shape = bubbleShape
                                        )
                                        .then(
                                            if (isFrostedGlassEnabled) {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                        colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
                                                    ),
                                                    shape = bubbleShape
                                                )
                                            } else {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    color = bubbleBorderColor,
                                                    shape = bubbleShape
                                                )
                                            }
                                        )
                                        .padding(
                                            horizontal = 16.dp,
                                            vertical = 12.dp
                                        )
                                        .widthIn(max = 280.dp)
                                ) {
                                    Text(
                                        text = if (index == 0 && !isUser) {
                                            parseMarkdown(if (appLanguage == "el") "Ρωτήστε τον βοηθό ενυδάτωσης" else "Ask hydration coach")
                                        } else {
                                            parseMarkdown(msg.text)
                                        },
                                        color = textColor,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                        fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        if (chatLoading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeroAvatar(
                                    isDark = isDark,
                                    sizeDp = 32.dp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (appLanguage == "el") "Ο Nero γράφει..." else "Nero is writing...",
                                    color = if (isDark) Color.LightGray else Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        if (chatError != null) {
                            Text(
                                text = if (appLanguage == "el") "Σφάλμα: $chatError" else "Error: $chatError",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Attached image preview area inside coach interface
            if (selectedImageUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .background(
                            color = if (isDark) Color(0xFF1E1E20).copy(alpha = 0.85f) else Color(0xFFE2E8F0).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (appLanguage == "el") "Συνημμένη εικόνα έτοιμη για ανάλυση" else "Image attached & ready to analyze",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.clearSelectedImage() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear image",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Conditionally display Accept/Decline action buttons OR topic suggestions
            if (pendingProposal != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("proposal_pending_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E1F22) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (appLanguage == "el") "Ο βοηθός Nero ζητάει έγκριση:" else "Nero requires permission:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            pendingProposal?.forEach { action ->
                                Text(
                                    text = "• " + formatProposalItem(action.type, action.params, appLanguage == "el"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.9f)
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { viewModel.declinePendingProposal() },
                                modifier = Modifier.weight(1f).testTag("proposal_decline_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(100)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                )
                                Text(text = if (appLanguage == "el") "Απόρριψη" else "Decline")
                            }
                            
                            Button(
                                onClick = { viewModel.acceptPendingProposal() },
                                modifier = Modifier.weight(1.5f).testTag("proposal_accept_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(100)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                )
                                Text(text = if (appLanguage == "el") "Αποδοχή" else "Accept")
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val helpers = listOf(
                            Triple(if (appLanguage == "el") "Εβδομαδιαίες Τάσεις" else "Weekly Trends", Icons.Rounded.DateRange, if (appLanguage == "el") "Ανέλυσε το ιστορικό ενυδάτωσής μου για την τελευταία εβδομάδα." else "Please analyze my water logs history for the past week, explain trends, highlight strengths, and identify areas to improve."),
                            Triple(if (appLanguage == "el") "Ενυδάτωση στην Άσκηση" else "Workout Hydration", Icons.Rounded.Favorite, if (appLanguage == "el") "Ποιες είναι οι βέλτιστες πρακτικές ενυδάτωσης για τη γυμναστική;" else "What are the hydration best practices before, during, and after a vigorous exercise session to maximize energy and muscle recovery?"),
                            Triple(if (appLanguage == "el") "Καφές vs Νερό" else "Coffee vs Water", Icons.Rounded.Info, if (appLanguage == "el") "Πώς συγκρίνεται ο καφές και το τσάι με το νερό;" else "Tell me how coffee, tea, and soda compare to water in terms of actual body hydration efficiency and water equivalency coefficients."),
                            Triple(if (appLanguage == "el") "Ενυδάτωση & Βάρος" else "Hydration & Weight", Icons.Rounded.Star, if (appLanguage == "el") "Πώς βοηθά το νερό στην απώλεια βάρους;" else "How does proper daily water intake affect metabolism, fat burning, and healthy weight management support?"),
                            Triple(if (appLanguage == "el") "Υπολογισμός Ορίου" else "Calculated Limit", Icons.Rounded.Settings, if (appLanguage == "el") "Πώς υπολογίζεται ο βέλτιστος στόχος ενυδάτωσης;" else "How do age, weight, climate, and daily activity level mathematically scale my optimal hydration targets?"),
                            Triple(if (appLanguage == "el") "Νεφρά & Αποτοξίνωση" else "Kidney & Detox", Icons.Rounded.Warning, if (appLanguage == "el") "Εξήγησε τη λειτουργία του νερού στην αποτοξίνωση." else "Explain the physiological function of water in kidney filtration and cellular detoxification processes."),
                            Triple(if (appLanguage == "el") "Δέρμα & Λάμψη" else "Skin & Glow", Icons.Rounded.Face, if (appLanguage == "el") "Ποιες είναι οι επιπτώσεις της ενυδάτωσης στο δέρμα;" else "What are the dermatological impacts of consistent micro-hydration on skin elasticity, barrier health, and glow?")
                        )
                        helpers.forEach { (text, icon, prompt) ->
                            SuggestedTopicCard(
                                title = text,
                                icon = icon,
                                isDark = isDark,
                                onClick = {
                                    viewModel.sendChatMessage(prompt)
                                    viewModel.triggerButtonHaptic()
                                }
                            )
                        }
                    }
                }
            }

            // 3. PILL-SHAPED INPUT BAR WITH PHOTO SELECTOR, TEXT FIELD AND SEND BUTTON
            val isFrostedGlassEnabled = LocalFrostedGlassEnabled.current
            val baseCardColor = LocalBaseCardColor.current ?: MaterialTheme.colorScheme.surfaceVariant
            val boxColor = if (isFrostedGlassEnabled) {
                if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
            } else {
                baseCardColor
            }
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pixel_color_shift_gemini")
            val animatedOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(10000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "pixel_gradient_offset_gemini"
            )
            
            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            
            val pixelBrush = Brush.linearGradient(
                colors = listOf(
                    primaryColor,
                    tertiaryColor,
                    secondaryColor,
                    primaryColor
                ),
                start = androidx.compose.ui.geometry.Offset(animatedOffset, 0f),
                end = androidx.compose.ui.geometry.Offset(animatedOffset + 400f, 400f)
            )

            var userTextState by remember { mutableStateOf("") }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Input Bar (Pill shaped)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = boxColor,
                            shape = RoundedCornerShape(28.dp)
                        )
                        .then(
                            if (!isDark) {
                                Modifier.border(
                                    width = 1.dp,
                                    brush = if (isFrostedGlassEnabled) {
                                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))
                                    } else {
                                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)))
                                    },
                                    shape = RoundedCornerShape(28.dp)
                                )
                            } else Modifier
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = "Attach image",
                            tint = if (selectedImageUri != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    androidx.compose.foundation.text.BasicTextField(
                        value = userTextState,
                        onValueChange = { userTextState = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gemini_chat_input"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 15.sp
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSend = {
                                val trimmed = userTextState.trim()
                                if ((trimmed.isNotEmpty() || selectedImageUri != null) && !chatLoading) {
                                    viewModel.sendChatMessage(trimmed)
                                    userTextState = ""
                                    viewModel.triggerButtonHaptic()
                                }
                            }
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(if (isDark) Color.White else Color.Black),
                        decorationBox = { innerTextField ->
                            Box {
                                if (userTextState.isEmpty()) {
                                    Text(
                                        text = if (appLanguage == "el") "Ρωτήστε κάτι άλλο..." else "Ask something else...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 15.sp,
                                        maxLines = 1
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (userTextState.isNotEmpty()) {
                        IconButton(
                            onClick = { 
                                userTextState = "" 
                                viewModel.triggerButtonHaptic()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear text",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Separate Send Button (Pill shaped with color-shifting Pixel gradient)
                val isSendEnabled = (userTextState.trim().isNotEmpty() || selectedImageUri != null) && !chatLoading
                
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(64.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .then(
                            if (isSendEnabled) {
                                Modifier.background(brush = pixelBrush)
                            } else {
                                Modifier
                                    .background(color = boxColor)
                                    .border(
                                        width = 1.dp,
                                        brush = if (isFrostedGlassEnabled) {
                                            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))
                                        } else {
                                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)))
                                        },
                                        shape = RoundedCornerShape(24.dp)
                                    )
                            }
                        )
                        .clickable(enabled = isSendEnabled) {
                            val trimmed = userTextState.trim()
                            if ((trimmed.isNotEmpty() || selectedImageUri != null) && !chatLoading) {
                                viewModel.sendChatMessage(trimmed)
                                userTextState = ""
                                viewModel.triggerButtonHaptic()
                            }
                        }
                        .testTag("gemini_send_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = if (appLanguage == "el") "Αποστολή μηνύματος" else "Send message",
                        tint = if (isSendEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestedTopicCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isDark: Boolean
) {
    val isFrostedGlassEnabled = LocalFrostedGlassEnabled.current
    var isActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var resetJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val scaleAnim = remember { androidx.compose.animation.core.Animatable(1f) }
    
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pixel_shift_suggested")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(8000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "offset"
    )
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    
    val pixelBrush = Brush.linearGradient(
        colors = listOf(
            primaryColor,
            tertiaryColor,
            secondaryColor,
            primaryColor
        ),
        start = androidx.compose.ui.geometry.Offset(animatedOffset, 0f),
        end = androidx.compose.ui.geometry.Offset(animatedOffset + 400f, 400f)
    )
    
    val cornerRadius by animateDpAsState(
        targetValue = if (isActive) 100.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "suggested_radius"
    )
    
    val baseCardColor = LocalBaseCardColor.current ?: MaterialTheme.colorScheme.surfaceVariant
    val containerColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            if (isFrostedGlassEnabled) {
                if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
            } else {
                baseCardColor
            }
        },
        animationSpec = tween(250),
        label = "suggested_container_color"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isDark) Color.Black else MaterialTheme.colorScheme.onPrimary,
        animationSpec = tween(250),
        label = "suggested_content_color"
    )

    Box(
        modifier = Modifier
            .width(125.dp)
            .height(60.dp)
            .scale(scaleAnim.value)
            .clip(RoundedCornerShape(maxOf(0f, cornerRadius.value).dp))
            .background(color = MaterialTheme.colorScheme.primary)
            .clickable {
                onClick()
                isActive = true
                scope.launch {
                    scaleAnim.animateTo(0.85f, animationSpec = tween(50))
                    scaleAnim.animateTo(1.08f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 300f))
                    scaleAnim.animateTo(1.0f, animationSpec = spring())
                }
                resetJob?.cancel()
                resetJob = scope.launch {
                    delay(1500)
                    isActive = false
                }
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
fun NeroAvatar(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    sizeDp: androidx.compose.ui.unit.Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(sizeDp)
            .background(
                color = if (isDark) Color(0xFF1E1E22) else Color(0xFFE5E5EA),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color(0xFF3A3A3C) else Color(0xFFCBD5E1),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "App Icon",
            modifier = Modifier.size(sizeDp)
        )
    }
}

