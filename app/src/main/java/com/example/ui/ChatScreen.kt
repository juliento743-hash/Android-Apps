package com.example.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
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

    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val activeMessages by viewModel.activeMessages.collectAsState()
    val currentModelType by viewModel.currentModelType.collectAsState()
    val isSending by viewModel.isSending.collectAsState()

    var showRenameDialog by remember { mutableStateOf<ChatSession?>(null) }
    var renameTitleText by remember { mutableStateOf("") }
    var showModelInfoDialog by remember { mutableStateOf(false) }

    // Navigation Drawer with History
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xEEF5F6FA),
                modifier = Modifier
                    .width(310.dp)
                    .border(width = 1.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Historial de charlas",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(16.dp))

                // New Session Actions Header
                Text(
                    text = "Nueva charla con:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            onClick = {
                                viewModel.startNewSession(model)
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (model) {
                                    SundayModelType.COZY -> Color(0xFFE9C46A)
                                    SundayModelType.DEEP -> Color(0xFF2A9D8F)
                                    SundayModelType.ARTIST -> Color(0xFFF4A261)
                                },
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
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
                                    modifier = Modifier.size(14.dp)
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
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(12.dp))

                // Saved Sessions list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
                                    text = "No hay salas previas.\n¡Inicia una nueva arriba!",
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.8f
                                        ) else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.selectSession(session.id)
                                        coroutineScope.launch { drawerState.close() }
                                    }
                                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Bubble indicating active model in conversation
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

                                // Session Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = sessionModel.displayName,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Interactive action buttons
                                IconButton(
                                    onClick = {
                                        renameTitleText = session.title
                                        showRenameDialog = session
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Rename Session",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteSession(session.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete Session",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        // Main Screen Interface with Frosted Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FrostedBackgroundBrush)
        ) {
            Scaffold(
                containerColor = Color.Transparent, // fully transparent so our gorgeous gradient is always present
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
                            IconButton(
                                onClick = { viewModel.startNewSession(currentModelType) },
                                modifier = Modifier
                                    .padding(end = 8.dp)
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
                        // Beautiful Frosted Glass Landing Hub Dashboard
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // App Logo Header (exactly modeled after mockup brand showcase!)
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

                            // Active Model Card (Glassmorphic featured)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                                text = "ACTIVE MODEL",
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
                                                text = "Pro",
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

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Dynamic indicators from HTML mock
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .height(6.dp)
                                                .weight(1f)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(GlassPurplePrimary)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .height(6.dp)
                                                .weight(1f)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(GlassPurplePrimary.copy(alpha = 0.2f))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .height(6.dp)
                                                .weight(1f)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(GlassPurplePrimary.copy(alpha = 0.2f))
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Model Selection Title
                            Text(
                                text = "Elige tu Mood de Domingo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = GlassTextDark,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(bottom = 12.dp)
                            )

                            // 2x2 Model Selection Grid elements
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Sunday Cozy Card (Canvas type)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .frostedGlassCard(
                                            shape = RoundedCornerShape(24.dp),
                                            bgColor = Color(0x66FFFFFF),
                                            borderColor = Color(0x4DFFFFFF),
                                            shadowElevation = 1f // use simple float
                                        )
                                        .clickable { viewModel.startNewSession(SundayModelType.COZY) }
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

                                // Sunday Deep Card (Script type)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .frostedGlassCard(
                                            shape = RoundedCornerShape(24.dp),
                                            bgColor = Color(0x66FFFFFF),
                                            borderColor = Color(0x4DFFFFFF),
                                            shadowElevation = 1f
                                        )
                                        .clickable { viewModel.startNewSession(SundayModelType.DEEP) }
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

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                              ) {
                                // Sunday Artist Card (Echo type)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .frostedGlassCard(
                                            shape = RoundedCornerShape(24.dp),
                                            bgColor = Color(0x66FFFFFF),
                                            borderColor = Color(0x4DFFFFFF),
                                            shadowElevation = 1f
                                        )
                                        .clickable { viewModel.startNewSession(SundayModelType.ARTIST) }
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

                                // Placeholder Info Card (Flash/Static details)
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
                                            text = "Más pronto...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GlassTextMedium.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    } else {
                        // Regular Conversation Window with Glass background showing through
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
    val coroutineScope = rememberCoroutineScope()

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

                // Mandatory Key security text warnings for proto
                Text(
                    text = "Security Warning: Las respuestas son generadas usando tu clave local. Evita compartir capturas confidenciales o publicar la APK si tus claves personales están expuestas.",
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
                // If it's a Sunday Artist response and has encoded image, we decode and present it
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

                // Text body content
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
            "💡 Consejos para evitar el estrés del lunes",
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
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
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

// Utility Base64 Image decoder
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

// Simple time formatter utility
fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
