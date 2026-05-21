package com.example.ui

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.speech.RecognizerIntent
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.data.models.SundayModelType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Cozy Latte & Terracotta Color Palette
val CozyTerracotta = Color(0xFFC05C3E)
val CozyAmber = Color(0xFFE69A43)
val CozyCoffeeDark = Color(0xFF2C2523)
val CozySandLight = Color(0xFFFBF8F4)
val CozyCreamCard = Color(0xFFFAF2EB)
val CozyTextDark = Color(0xFF3F3735)
val CozyTextLight = Color(0xFFF3EFE9)
val CozyBrownMedium = Color(0xFF8B6B5E)

// Pro Gold / Premium Aesthetic Styling Palette
val ProGoldBorder = Color(0xFFFFD700)
val ProGoldBg = Color(0x1BFFD700)

// Frosted Glass Aesthetic Styling Palette
val GlassPurplePrimary = Color(0xFF6750A4)
val GlassPurpleContainer = Color(0xFFEADDFF)
val GlassPurpleText = Color(0xFF21005D)
val GlassTextDark = Color(0xFF1C1B1F)
val GlassTextMedium = Color(0xFF49454F)

// Background Mesh Colors
val FrostedMeshBlue = Color(0xFFD8E5FF)
val FrostedMeshPurple = Color(0xFFF3E8FF)
val FrostedMeshPink = Color(0xFFFFE8EC)

val FrostedBackgroundBrush = Brush.linearGradient(
    colors = listOf(
        FrostedMeshBlue,
        FrostedMeshPurple,
        FrostedMeshPink
    )
)

fun Modifier.frostedGlassCard(
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    bgColor: Color = Color(0x66FFFFFF),
    borderColor: Color = Color(0x4DFFFFFF),
    shadowElevation: Float = 2f
): Modifier = this
    .shadow(elevation = shadowElevation.dp, shape = shape, clip = false)
    .background(color = bgColor, shape = shape)
    .border(width = 1.dp, color = borderColor, shape = shape)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe State from ViewModel
    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val activeMessages by viewModel.activeMessages.collectAsState()
    val currentModelType by viewModel.currentModelType.collectAsState()
    val isSending by viewModel.isSending.collectAsState()

    // Accounts / Settings States
    val userName by viewModel.userName.collectAsState()
    val userEmoji by viewModel.userEmoji.collectAsState()
    val isProMember by viewModel.isProMember.collectAsState()
    val userApiKey by viewModel.userApiKey.collectAsState()

    // Live Mode variables
    val isLiveActive by viewModel.isLiveActive.collectAsState()
    val liveSpeakingStatus by viewModel.liveSpeakingStatus.collectAsState()
    val lastLiveText by viewModel.lastLiveText.collectAsState()
    val liveVoiceSoundActive by viewModel.liveVoiceSoundActive.collectAsState()
    val selectedVoiceGender by viewModel.selectedVoiceGender.collectAsState()

    // Triggers / Local screen triggers
    var showRenameDialog by remember { mutableStateOf<ChatSession?>(null) }
    var renameTitleText by remember { mutableStateOf("") }
    var showModelInfoDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }

    // Register Google Speech recognition activity launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                viewModel.sendLiveVoiceInput(spokenText)
            }
        }
    }

    fun launchSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Cuéntale hoy algo a Sunday AI...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "El dictado no está disponible, introduce el texto en la barra inferior.", Toast.LENGTH_LONG).show()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            // TABLET / DESKTOP SPLIT CANONICAL LAYOUT
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FrostedBackgroundBrush)
            ) {
                // Fixed glass sidebar context on the left
                SidebarContent(
                    sessions = sessions,
                    activeSessionId = activeSessionId,
                    currentModelType = currentModelType,
                    onSessionSelected = { id -> viewModel.selectSession(id) },
                    onStartNewSession = { model -> viewModel.startNewSession(model) },
                    onRenameClick = { session ->
                        renameTitleText = session.title
                        showRenameDialog = session
                    },
                    onDeleteClick = { id -> viewModel.deleteSession(id) },
                    userName = userName,
                    userEmoji = userEmoji,
                    isProMember = isProMember,
                    onProfileClick = { showAccountDialog = true },
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .border(
                            width = 1.dp,
                            color = Color(0x33FFFFFF),
                            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                        )
                )

                // Main Chat Panel (Right Panel)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(30.dp))
                                            .background(Color(0x33FFFFFF))
                                            .border(width = 1.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(30.dp))
                                            .clickable { showModelInfoDialog = true }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = currentModelType.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = GlassTextDark
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Model Info",
                                            tint = GlassPurplePrimary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                },
                                navigationIcon = {
                                    // Left User Account Profile Status
                                    Row(
                                        modifier = Modifier
                                            .padding(start = 16.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0x2AFFFFFF))
                                            .clickable { showAccountDialog = true }
                                            .border(
                                                width = if (isProMember) 1.5.dp else 1.dp,
                                                color = if (isProMember) ProGoldBorder else Color(0x33FFFFFF),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = userEmoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = userName.take(12),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GlassTextDark
                                        )
                                        if (isProMember) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Stars,
                                                contentDescription = "Pro",
                                                tint = Color(0xFFD4AF37),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    // Gemini Live quick launcher trigger
                                    IconButton(
                                        onClick = { viewModel.startLiveSession(currentModelType) },
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(CozyTerracotta.copy(alpha = 0.12f))
                                            .border(width = 1.dp, color = CozyTerracotta.copy(alpha = 0.3f), shape = RoundedCornerShape(20.dp))
                                    ) {
                                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "Modo Live", tint = CozyTerracotta)
                                    }

                                    IconButton(
                                        onClick = { viewModel.startNewSession(currentModelType) },
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0x33FFFFFF))
                                            .border(width = 1.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(20.dp))
                                    ) {
                                        Icon(imageVector = Icons.Default.AddComment, contentDescription = "Nueva sala", tint = GlassTextMedium)
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (activeSessionId == null) {
                                LandingHubDashboard(
                                    currentModelType = currentModelType,
                                    onStartSession = { model -> viewModel.startNewSession(model) },
                                    onStartLive = { model -> viewModel.startLiveSession(model) }
                                )
                            } else {
                                ChatContentLayout(
                                    viewModel = viewModel,
                                    messages = activeMessages,
                                    isSending = isSending,
                                    currentModel = currentModelType
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // MOBILE AND PORTRAIT SLIDING DRAWER VIEW
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = Color(0xEEF5F6FA),
                        modifier = Modifier
                            .width(310.dp)
                            .border(width = 1.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                    ) {
                        SidebarContent(
                            sessions = sessions,
                            activeSessionId = activeSessionId,
                            currentModelType = currentModelType,
                            onSessionSelected = { id ->
                                viewModel.selectSession(id)
                                coroutineScope.launch { drawerState.close() }
                            },
                            onStartNewSession = { model ->
                                viewModel.startNewSession(model)
                                coroutineScope.launch { drawerState.close() }
                            },
                            onRenameClick = { session ->
                                renameTitleText = session.title
                                showRenameDialog = session
                            },
                            onDeleteClick = { id -> viewModel.deleteSession(id) },
                            userName = userName,
                            userEmoji = userEmoji,
                            isProMember = isProMember,
                            onProfileClick = {
                                showAccountDialog = true
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            ) {
                // Main Mobile View
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FrostedBackgroundBrush)
                ) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(30.dp))
                                            .background(Color(0x33FFFFFF))
                                            .border(width = 1.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(30.dp))
                                            .clickable { showModelInfoDialog = true }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = currentModelType.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = GlassTextDark
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = GlassPurplePrimary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { coroutineScope.launch { drawerState.open() } },
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0x33FFFFFF))
                                            .border(width = 1.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(20.dp))
                                    ) {
                                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = GlassTextMedium)
                                    }
                                },
                                actions = {
                                    // Live and account action triggers
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.startLiveSession(currentModelType) },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(CozyTerracotta.copy(alpha = 0.12f))
                                                .border(width = 1.dp, color = CozyTerracotta.copy(alpha = 0.3f), shape = RoundedCornerShape(20.dp))
                                        ) {
                                            Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "Live voice", tint = CozyTerracotta, modifier = Modifier.size(18.dp))
                                        }
                                        
                                        Spacer(modifier = Modifier.width(6.dp))

                                        Box(
                                            modifier = Modifier
                                                .padding(end = 12.dp)
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color(0x22FFFFFF))
                                                .clickable { showAccountDialog = true }
                                                .border(
                                                    width = if (isProMember) 1.5.dp else 1.dp,
                                                    color = if (isProMember) ProGoldBorder else Color(0x22FFFFFF),
                                                    shape = RoundedCornerShape(20.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = userEmoji, fontSize = 18.sp)
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (activeSessionId == null) {
                                LandingHubDashboard(
                                    currentModelType = currentModelType,
                                    onStartSession = { model -> viewModel.startNewSession(model) },
                                    onStartLive = { model -> viewModel.startLiveSession(model) }
                                )
                            } else {
                                ChatContentLayout(
                                    viewModel = viewModel,
                                    messages = activeMessages,
                                    isSending = isSending,
                                    currentModel = currentModelType
                                )
                            }
                        }
                    }
                }
            }
        }

        // IMMERSIVE GEMINI LIVE MOOD OVERLAY SCREEN
        AnimatedVisibility(
            visible = isLiveActive,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            GeminiLiveOverlay(
                isSending = isSending,
                speakingStatus = liveSpeakingStatus,
                lastSpokenText = lastLiveText,
                liveVoiceSoundActive = liveVoiceSoundActive,
                onSoundToggle = { viewModel.updateVoiceConfig(it, selectedVoiceGender) },
                onSendSpeech = { text -> viewModel.sendLiveVoiceInput(text) },
                onMicTrigger = { launchSpeechToText() },
                onCloseLive = { viewModel.setLiveActive(false) },
                currentModel = currentModelType
            )
        }
    }

    // Rename Session Title Dialog
    if (showRenameDialog != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Renombrar Charla") },
            text = {
                OutlinedTextField(
                    value = renameTitleText,
                    onValueChange = { renameTitleText = it },
                    label = { Text("Nuevo Título") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRenameDialog?.let { session ->
                            if (renameTitleText.trim().isNotEmpty()) {
                                viewModel.renameSession(session.id, renameTitleText.trim())
                            }
                        }
                        showRenameDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CozyTerracotta)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modern Accounts/Settings Dialog Panel
    if (showAccountDialog) {
        AccountSettingsDialog(
            userName = userName,
            userEmoji = userEmoji,
            isProMember = isProMember,
            userApiKey = userApiKey,
            onSaveProfile = { name, emoji -> viewModel.updateProfile(name, emoji) },
            onTogglePro = { viewModel.updateProStatus(it) },
            onSaveApiKey = { viewModel.updateApiKey(it) },
            onDismiss = { showAccountDialog = false }
        )
    }

    // Show Model Explanation Dialog
    if (showModelInfoDialog) {
        AlertDialog(
            onDismissRequest = { showModelInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = CozyTerracotta,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Modelos de Sunday AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SundayModelType.values().forEach { m ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = 1.dp,
                                    color = if (m == currentModelType) CozyTerracotta else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val modelIcon = when (m) {
                                    SundayModelType.COZY -> Icons.Default.Coffee
                                    SundayModelType.DEEP -> Icons.Default.Psychology
                                    SundayModelType.ARTIST -> Icons.Default.Palette
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CozyTerracotta.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = modelIcon,
                                        contentDescription = null,
                                        tint = CozyTerracotta,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = m.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (m == currentModelType) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        "Activo",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = CozyTerracotta,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(30.dp))
                                            .background(CozyTerracotta.copy(alpha = 0.1f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = m.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showModelInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CozyTerracotta)
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun ChatContentLayout(
    viewModel: ChatViewModel,
    messages: List<ChatMessage>,
    isSending: Boolean,
    currentModel: SundayModelType
) {
    val scrollState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var inputMessageText by remember { mutableStateOf("") }

    // Smooth autoscroll down when message list increments
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Message Thread Listing
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }

            // Typing preview indicator
            if (isSending) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        AnimatedTypingCard(modelName = currentModel.displayName)
                    }
                }
            }
        }

        // Horizontal scrolling preset suggestions
        PresetsList(
            model = currentModel,
            onPresetClick = { presetText ->
                inputMessageText = presetText
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Input controls panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xE6F7F2FA))
                        .border(width = 1.dp, color = Color(0x336750A4), shape = RoundedCornerShape(28.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chat Input Field
                    OutlinedTextField(
                        value = inputMessageText,
                        onValueChange = { inputMessageText = it },
                        placeholder = {
                            Text(
                                text = when (currentModel) {
                                    SundayModelType.COZY -> "Charlemos de domingo..."
                                    SundayModelType.DEEP -> "Escribe tus planes o dudas..."
                                    SundayModelType.ARTIST -> "Describe tu ilustración..."
                                },
                                fontSize = 14.sp,
                                color = GlassTextMedium.copy(alpha = 0.6f)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputMessageText.trim().isNotEmpty() && !isSending) {
                                    viewModel.sendMessage(inputMessageText)
                                    inputMessageText = ""
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = GlassTextDark,
                            unfocusedTextColor = GlassTextDark
                        ),
                        maxLines = 4,
                        trailingIcon = {
                            if (inputMessageText.trim().isNotEmpty()) {
                                IconButton(onClick = { inputMessageText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar texto",
                                        tint = GlassTextMedium.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    )

                    // Sending Trigger Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (inputMessageText.trim().isEmpty() || isSending) Color(0xFFEADDFF)
                                else GlassPurplePrimary
                            )
                            .clickable(enabled = inputMessageText.trim().isNotEmpty() && !isSending) {
                                viewModel.sendMessage(inputMessageText)
                                inputMessageText = ""
                                keyboardController?.hide()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Enviar",
                            tint = if (inputMessageText.trim().isEmpty() || isSending) Color(0xFF21005D).copy(alpha = 0.4f)
                            else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Security text warnings for proto
                Text(
                    text = "Sunday AI: Respuestas cifradas en tu almacenamiento local.",
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        // Soft model title headers
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = if (isUser) Icons.Default.Face else Icons.Default.Android,
                contentDescription = null,
                tint = if (isUser) GlassPurplePrimary else Color(0xFF6750A4),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isUser) "Tú" else "Sunday AI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUser) GlassPurplePrimary else Color(0xFF6750A4)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formatTime(message.timestamp),
                fontSize = 10.sp,
                color = GlassTextMedium.copy(alpha = 0.6f)
            )
        }

        // Bubble shape and container styles
        val bubbleShape = if (isUser) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        } else {
            RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        }

        val bgColor = if (isUser) GlassPurplePrimary.copy(alpha = 0.85f) else Color(0xCCFFFFFF)
        val borderColor = if (isUser) GlassPurplePrimary.copy(alpha = 0.2f) else Color(0x66FFFFFF)

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(elevation = 1.dp, shape = bubbleShape)
                .background(
                    color = bgColor,
                    shape = bubbleShape
                )
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = bubbleShape
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                if (message.imageB64 != null) {
                    val decodedImage = rememberBase64Image(message.imageB64)
                    if (decodedImage != null) {
                        Image(
                            bitmap = decodedImage,
                            contentDescription = "Sunday Illustration",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = if (isUser) Color.White else GlassTextDark
                )
            }
        }
    }
}

@Composable
fun PresetsList(
    model: SundayModelType,
    onPresetClick: (String) -> Unit
) {
    val presets = when (model) {
        SundayModelType.COZY -> listOf(
            "☕ Ideas de relajación para hoy",
            "📚 Recomiéndame un buen libro",
            "🎬 Una peli acogedora de domingo",
            "✍️ Reflexión corta del finde"
        )
        SundayModelType.DEEP -> listOf(
            "📅 Planificador para mi semana",
            "💡 Consejos para evitar el estrés de lunes",
            "🧩 Resume mis metas organizadas",
            "💻 ¿Cómo optimizo esta rutina?"
        )
        SundayModelType.ARTIST -> listOf(
            "🎨 Un gato nevado frente a la chimenea",
            "🏡 Un café en el bosque lluvioso medieval",
            "🥞 Desayuno campestre en acuarela",
            "🍁 Bosque otoñal con luz dorada"
        )
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets) { preset ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(30.dp)
                    )
                    .clickable { onPresetClick(preset.substring(2)) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = preset,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AnimatedTypingCard(modelName: String) {
    val transition = rememberInfiniteTransition(label = "dots")
    val dotCountFloat by transition.animateFloat(
        initialValue = 1f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotsAnimation"
    )

    val dots = ".".repeat(dotCountFloat.toInt())

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(CozyCreamCard)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Pensando$dots",
            fontSize = 13.sp,
            color = CozyBrownMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun rememberBase64Image(base64Str: String?): ImageBitmap? {
    return remember(base64Str) {
        if (base64Str == null) return@remember null
        try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}

fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Composable
fun SidebarContent(
    sessions: List<ChatSession>,
    activeSessionId: String?,
    currentModelType: SundayModelType,
    onSessionSelected: (String) -> Unit,
    onStartNewSession: (SundayModelType) -> Unit,
    onRenameClick: (ChatSession) -> Unit,
    onDeleteClick: (String) -> Unit,
    userName: String,
    userEmoji: String,
    isProMember: Boolean,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xEEF5F6FA))
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        // Drawer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassPurplePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Sunday AI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GlassTextDark
                )
                Text(
                    text = "Historial de charlas",
                    fontSize = 12.sp,
                    color = GlassTextMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0x1A000000))
        Spacer(modifier = Modifier.height(16.dp))

        // New Session Actions Header
        Text(
            text = "Nueva charla con:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = GlassTextMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        // Quick Launch Model Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SundayModelType.values().forEach { model ->
                Button(
                    onClick = { onStartNewSession(model) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (model) {
                            SundayModelType.COZY -> Color(0xFFE9C46A)
                            SundayModelType.DEEP -> Color(0xFF2A9D8F)
                            SundayModelType.ARTIST -> Color(0xFFF4A261)
                        },
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val modelIcon = when (model) {
                            SundayModelType.COZY -> Icons.Default.Coffee
                            SundayModelType.DEEP -> Icons.Default.Psychology
                            SundayModelType.ARTIST -> Icons.Default.Palette
                        }
                        Icon(
                            imageVector = modelIcon,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = model.displayName.substringBefore(" ").take(6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0x1A000000))
        Spacer(modifier = Modifier.height(12.dp))

        // Saved Sessions list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (sessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay salas de chat.\n¡Crea una eligiendo tu mood!",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = GlassTextMedium
                        )
                    }
                }
            } else {
                items(sessions, key = { it.id }) { session ->
                    val isSelected = session.id == activeSessionId
                    val sessionModel = SundayModelType.fromId(session.modelId)
                    val modelColor = when (sessionModel) {
                        SundayModelType.COZY -> Color(0xFFE9C46A)
                        SundayModelType.DEEP -> Color(0xFF2A9D8F)
                        SundayModelType.ARTIST -> Color(0xFFF4A261)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) GlassPurpleContainer.copy(alpha = 0.8f) else Color.Transparent
                            )
                            .clickable { onSessionSelected(session.id) }
                            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(modelColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = when (sessionModel) {
                                SundayModelType.COZY -> Icons.Default.Coffee
                                SundayModelType.DEEP -> Icons.Default.Psychology
                                SundayModelType.ARTIST -> Icons.Default.Palette
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = modelColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) GlassPurpleText else GlassTextDark
                            )
                            Text(
                                text = sessionModel.displayName,
                                fontSize = 11.sp,
                                color = GlassTextMedium
                            )
                        }

                        IconButton(
                            onClick = { onRenameClick(session) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Renombrar",
                                tint = GlassTextMedium.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { onDeleteClick(session.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Eliminar",
                                tint = Color(0xFFBA1A1A).copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // FOOTER ACCOUNT BUTTON
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0x1F000000))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onProfileClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(
                        if (isProMember) ProGoldBg else GlassPurplePrimary.copy(alpha = 0.15f)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isProMember) ProGoldBorder else Color.Transparent,
                        shape = RoundedCornerShape(21.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = userEmoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GlassTextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isProMember) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "PRO Member",
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Text(
                    text = if (isProMember) "Sunday Pro Member" else "Cuenta de Domingo",
                    fontSize = 11.sp,
                    color = if (isProMember) Color(0xFFB8860B) else GlassTextMedium
                )
            }
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configuración",
                tint = GlassTextMedium,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun LandingHubDashboard(
    currentModelType: SundayModelType,
    onStartSession: (SundayModelType) -> Unit,
    onStartLive: (SundayModelType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Branding Title Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassPurplePrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Sunday AI",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = GlassTextDark
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active Theme Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .frostedGlassCard(
                    shape = RoundedCornerShape(32.dp),
                    bgColor = Color(0x80FFFFFF),
                    borderColor = Color(0x99FFFFFF),
                    shadowElevation = 2f
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ESTADO DEL ENTORNO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassPurplePrimary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = currentModelType.displayName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GlassTextDark
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassPurpleContainer)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Sunday AI Pro",
                            color = GlassPurpleText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentModelType.description,
                    fontSize = 14.sp,
                    color = GlassTextMedium,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action to speak in real-time mode directly
                Button(
                    onClick = { onStartLive(currentModelType) },
                    colors = ButtonDefaults.buttonColors(containerColor = CozyTerracotta),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Iniciar Conversación Live",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Model Selection Title
        Text(
            text = "Elige tu Mood para Conversar",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = GlassTextDark,
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        // Mood Selection Grid elements
        Column(
            modifier = Modifier.widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sunday Cozy Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .frostedGlassCard(
                            shape = RoundedCornerShape(24.dp),
                            bgColor = Color(0x66FFFFFF),
                            borderColor = Color(0x4DFFFFFF),
                            shadowElevation = 1f
                        )
                        .clickable { onStartSession(SundayModelType.COZY) }
                        .padding(16.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFFEEBA))
                                .align(Alignment.Start),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Coffee,
                                contentDescription = null,
                                tint = Color(0xFF8A6D3B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sunday Cozy",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GlassTextDark
                        )
                        Text(
                            text = "Charla chill de café",
                            fontSize = 11.sp,
                            color = GlassTextMedium
                        )
                    }
                }

                // Sunday Deep Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .frostedGlassCard(
                            shape = RoundedCornerShape(24.dp),
                            bgColor = Color(0x66FFFFFF),
                            borderColor = Color(0x4DFFFFFF),
                            shadowElevation = 1f
                        )
                        .clickable { onStartSession(SundayModelType.DEEP) }
                        .padding(16.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFB2EEB5))
                                .align(Alignment.Start),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = Color(0xFF00390A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sunday Deep",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GlassTextDark
                        )
                        Text(
                            text = "Pensamiento y código",
                            fontSize = 11.sp,
                            color = GlassTextMedium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sunday Artist Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .frostedGlassCard(
                            shape = RoundedCornerShape(24.dp),
                            bgColor = Color(0x66FFFFFF),
                            borderColor = Color(0x4DFFFFFF),
                            shadowElevation = 1f
                        )
                        .clickable { onStartSession(SundayModelType.ARTIST) }
                        .padding(16.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFFD8E4))
                                .align(Alignment.Start),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color(0xFF31111D),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sunday Artist",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GlassTextDark
                        )
                        Text(
                            text = "Dibuja un finde creativo",
                            fontSize = 11.sp,
                            color = GlassTextMedium
                        )
                    }
                }

                // Immersive info Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .frostedGlassCard(
                            shape = RoundedCornerShape(24.dp),
                            bgColor = Color(0x33FFFFFF),
                            borderColor = Color(0x13FFFFFF),
                            shadowElevation = 0f
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GlassPurplePrimary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cuenta Conectada",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlassTextMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// IMMERSIVE GEMINI LIVE SIMULATION SCREENS OVERLAY
@Composable
fun GeminiLiveOverlay(
    isSending: Boolean,
    speakingStatus: String,
    lastSpokenText: String,
    liveVoiceSoundActive: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    onSendSpeech: (String) -> Unit,
    onMicTrigger: () -> Unit,
    onCloseLive: () -> Unit,
    currentModel: SundayModelType
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_waves")

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale2"
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.76f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale3"
    )

    var liveTextInput by remember { mutableStateOf("") }

    val themeColor = when (speakingStatus) {
        "Escuchando" -> Color(0xFF34A853) // Green listening
        "Pensando" -> Color(0xFFFBBC05)  // Yellow thinking
        "Hablando" -> Color(0xFF4285F4)  // Blue talking
        else -> CozyTerracotta          // Standby cozy
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0E11),
                        Color(0xFF1E1A22)
                    )
                )
            )
            .clickable(enabled = false) {} // block clicks
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Live Status Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(themeColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sunday Live Voice Mode",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }

                IconButton(
                    onClick = onCloseLive,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar Live",
                        tint = Color.White
                    )
                }
            }

            // Visualizer Orbs
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                if (speakingStatus != "Silencioso") {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .scale(if (speakingStatus == "Hablando" || speakingStatus == "Escuchando") scale3 else 1f)
                            .clip(RoundedCornerShape(90.dp))
                            .background(themeColor.copy(alpha = 0.08f))
                            .border(width = 1.dp, color = themeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(90.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(if (speakingStatus == "Hablando" || speakingStatus == "Escuchando") scale2 else 1f)
                            .clip(RoundedCornerShape(70.dp))
                            .background(themeColor.copy(alpha = 0.12f))
                            .border(width = 1.2.dp, color = themeColor.copy(alpha = 0.25f), shape = RoundedCornerShape(70.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(if (speakingStatus == "Hablando" || speakingStatus == "Escuchando") scale1 else 1f)
                            .clip(RoundedCornerShape(50.dp))
                            .background(themeColor.copy(alpha = 0.18f))
                            .border(width = 1.5.dp, color = themeColor.copy(alpha = 0.45f), shape = RoundedCornerShape(50.dp))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(38.dp))
                        .shadow(elevation = 10.dp, shape = RoundedCornerShape(38.dp))
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(themeColor, themeColor.copy(alpha = 0.6f), themeColor)
                            )
                        )
                        .clickable { onMicTrigger() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (speakingStatus == "Escuchando") Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = "Tap to talk",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // Audio Response Subtitle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when (speakingStatus) {
                        "Escuchando" -> "ESCUCHANDO TU VOZ..."
                        "Pensando" -> "PROCESANDO RESPUESTA DE SUNDAY..."
                        "Hablando" -> "SUNDAY AI ESTÁ HABLANDO..."
                        else -> "TOCA EL MICRÓFONO CENTRAL PARA DICTAR"
                    },
                    color = themeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lastSpokenText.isNotEmpty()) lastSpokenText else "¡Hola! Háblame como si de una llamada real se tratase. Cuéntame tu fin de semana de domingo.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Bottom controls & keyboard fallbacks
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = liveTextInput,
                        onValueChange = { liveTextInput = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        decorationBox = { innerTextField ->
                            if (liveTextInput.isEmpty()) {
                                Text("O escribe discretamente aquí...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    )

                    IconButton(
                        onClick = {
                            if (liveTextInput.trim().isNotEmpty() && !isSending) {
                                onSendSpeech(liveTextInput.trim())
                                liveTextInput = ""
                            }
                        },
                        enabled = liveTextInput.trim().isNotEmpty() && !isSending
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar",
                            tint = if (liveTextInput.trim().isNotEmpty()) CozyAmber else Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSoundToggle(!liveVoiceSoundActive) },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (liveVoiceSoundActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Mute",
                            tint = Color.White
                        )
                    }

                    Button(
                        onClick = onCloseLive,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CallEnd, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Terminar Live", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// PROFILE AND ACCOUNTS SETTINGS DIALOG (ROOM AND LOCAL STATE OVERRIDE)
@Composable
fun AccountSettingsDialog(
    userName: String,
    userEmoji: String,
    isProMember: Boolean,
    userApiKey: String,
    onSaveProfile: (String, String) -> Unit,
    onTogglePro: (Boolean) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameState by remember { mutableStateOf(userName) }
    var emojiState by remember { mutableStateOf(userEmoji) }
    var apiKeyState by remember { mutableStateOf(userApiKey) }

    val emojis = listOf("☕", "🎨", "🧠", "🍂", "🥞", "🐈", "🦉", "🌲", "🏠", "🧘")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = CozyTerracotta
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Perfil de Cuenta",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Name Input
                OutlinedTextField(
                    value = nameState,
                    onValueChange = { nameState = it },
                    label = { Text("Nombre de Usuario") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Avatar Emojis list
                Text(
                    "Icono de Avatar:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (emojiState == emoji) CozyTerracotta.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (emojiState == emoji) CozyTerracotta else Color.LightGray,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { emojiState = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                // Sunday Premium active Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProMember) Color(0xFFFFFDF0) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isProMember) ProGoldBorder else Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Membresía Sunday Pro",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isProMember) Color(0xFF856404) else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = Color(0xFFD4AF37),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = "El brillo visual dorado se reflejará en tu perfil activamente.",
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = if (isProMember) Color(0xFF856404).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isProMember,
                            onCheckedChange = { onTogglePro(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFD4AF37),
                                checkedTrackColor = Color(0xFFFFF2CC)
                            )
                        )
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                // Optional local Gemini Key configuration
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Clave de API Gemini Local",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = GlassPurplePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "Sobrescribe la clave por defecto de forma privada y segura en tu almacenamiento local.",
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = apiKeyState,
                        onValueChange = { apiKeyState = it },
                        placeholder = { Text("AIzaSy...", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CozyTerracotta,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveProfile(nameState.trim(), emojiState)
                    onSaveApiKey(apiKeyState.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CozyTerracotta)
            ) {
                Text("Guardar Perfil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
