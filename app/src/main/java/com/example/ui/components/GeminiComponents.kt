package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.ui.markdown.MarkdownMessageView
import com.example.ui.theme.DarkCodeBg
import com.example.ui.theme.GeminiAmber
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GeminiPink
import com.example.ui.theme.GeminiPurple

@Composable
fun GeminiSparkleGradientBrush(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            GeminiBlue,
            GeminiPurple,
            GeminiPink,
            GeminiCyan
        )
    )
}

@Composable
fun GeminiAvatar(
    modifier: Modifier = Modifier,
    size: Int = 32
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(GeminiSparkleGradientBrush()),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Gemini AI",
            tint = Color.White,
            modifier = Modifier.size((size * 0.6).dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiTopBar(
    activeModel: String,
    onModelSelected: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onNewChat: () -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var actionMenuExpanded by remember { mutableStateOf(false) }

    val models = listOf(
        "gemini-3.5-flash" to "Gemini 3.5 Flash",
        "gemini-3.1-pro-preview" to "Gemini 3.1 Pro",
        "gemini-3.1-flash-lite-preview" to "Gemini Flash Lite"
    )

    TopAppBar(
        modifier = modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier.testTag("drawer_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Chat History",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        title = {
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { modelMenuExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("model_selector_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GeminiAvatar(size = 20)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = models.find { it.first == activeModel }?.second ?: "Gemini 3.5 Flash",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select Model",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = modelMenuExpanded,
                    onDismissRequest = { modelMenuExpanded = false }
                ) {
                    models.forEach { (modelId, displayName) ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = when (modelId) {
                                            "gemini-3.5-flash" -> "General intelligence & speed"
                                            "gemini-3.1-pro-preview" -> "Advanced reasoning & coding"
                                            else -> "Lightweight & responsive"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            trailingIcon = {
                                if (activeModel == modelId) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = GeminiBlue)
                                }
                            },
                            onClick = {
                                onModelSelected(modelId)
                                modelMenuExpanded = false
                            }
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(
                onClick = onNewChat,
                modifier = Modifier.testTag("new_chat_top_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Box {
                IconButton(
                    onClick = { actionMenuExpanded = true },
                    modifier = Modifier.testTag("more_options_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = actionMenuExpanded,
                    onDismissRequest = { actionMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Settings & Persona") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            actionMenuExpanded = false
                            onOpenSettings()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear Current Chat") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            actionMenuExpanded = false
                            onClearChat()
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun WelcomeHeroSection(
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickStarters = listOf(
        QuickStarter(
            title = "Code an App",
            prompt = "Write a complete Jetpack Compose modern dashboard with interactive analytics cards and clean Kotlin architecture.",
            icon = Icons.Default.Code,
            color = GeminiBlue
        ),
        QuickStarter(
            title = "Deep Reasoning",
            prompt = "Explain how transformer neural networks and multi-head attention mechanisms work step by step with clear analogies.",
            icon = Icons.Default.Psychology,
            color = GeminiPurple
        ),
        QuickStarter(
            title = "Debug & Refactor",
            prompt = "How do I optimize Room database queries and coroutine flows to prevent UI recomposition jank in Android?",
            icon = Icons.Default.BugReport,
            color = GeminiPink
        ),
        QuickStarter(
            title = "Creative Spark",
            prompt = "Write a captivating sci-fi opening paragraph about an AI developing self-awareness inside a quantum computer.",
            icon = Icons.Default.AutoAwesome,
            color = GeminiAmber
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Large glowing Gemini Sparkle
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(GeminiSparkleGradientBrush())
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Google Gemini Assistant",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                brush = GeminiSparkleGradientBrush()
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Your exact AI Studio replica & coding companion.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Quick Starter Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            quickStarters.chunked(2).forEach { rowStarters ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowStarters.forEach { starter ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onPromptSelected(starter.prompt) }
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(starter.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = starter.icon,
                                        contentDescription = null,
                                        tint = starter.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = starter.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = starter.prompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class QuickStarter(
    val title: String,
    val prompt: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
fun ChatMessageItem(
    message: MessageEntity,
    onSpeak: (String) -> Unit,
    onOpenCodeInspector: (language: String, code: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val isError = message.role == "error"
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            GeminiAvatar(size = 32, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // User Attached Image if present
            if (!message.imageBase64.isNullOrBlank()) {
                val bitmap = remember(message.imageBase64) {
                    try {
                        val decoded = Base64.decode(message.imageBase64, Base64.NO_WRAP)
                        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(bottom = 6.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Message Bubble
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = when {
                    isUser -> MaterialTheme.colorScheme.primary
                    isError -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                tonalElevation = if (isUser) 0.dp else 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (isUser) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        MarkdownMessageView(
                            markdownContent = message.content,
                            onOpenCodeInspector = onOpenCodeInspector
                        )
                    }

                    // Metadata chips (Tokens & Latency)
                    if (!isUser && !isError && (message.tokens != null || message.latencyMs != null)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (message.latencyMs != null) {
                                Text(
                                    text = "⚡ ${(message.latencyMs.toFloat() / 1000f).let { String.format("%.1fs", it) }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (message.tokens != null) {
                                Text(
                                    text = "• ${message.tokens} tokens",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Action Toolbar for Gemini responses
            if (!isUser && !isError) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("gemini_reply", message.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Message",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = { onSpeak(message.content) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Read Aloud",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message.content)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Gemini Response"))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeminiThinkingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GeminiAvatar(size = 32)
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(GeminiSparkleGradientBrush())
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Gemini is thinking...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachImage: () -> Unit,
    attachedImageBase64: String?,
    onRemoveImage: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Attached image thumbnail preview
            if (!attachedImageBase64.isNullOrBlank()) {
                val bitmap = remember(attachedImageBase64) {
                    try {
                        val bytes = Base64.decode(attachedImageBase64, Base64.NO_WRAP)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .size(72.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Attached thumbnail",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, GeminiBlue, RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = onRemoveImage,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove attached image",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach Image Button
                IconButton(
                    onClick = onAttachImage,
                    modifier = Modifier.testTag("attach_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Attach Image",
                        tint = GeminiBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Text Input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    placeholder = {
                        Text(
                            text = "Ask Gemini anything, write code...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Send Button
                val canSend = (inputText.isNotBlank() || attachedImageBase64 != null) && !isLoading
                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) GeminiSparkleGradientBrush()
                            else Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f)))
                        )
                        .testTag("send_message_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerChatList(
    conversations: List<ConversationEntity>,
    activeConversationId: Long?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectConversation: (Long) -> Unit,
    onNewChat: () -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App Identity in Drawer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                GeminiAvatar(size = 36)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Google Gemini",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "AI Studio Companion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // New Chat Button
            Button(
                onClick = onNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_new_chat_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start New Chat", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_chats_input"),
                placeholder = { Text("Search chats...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recent Conversations",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Conversation Items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No chats yet. Start asking questions!" else "No matching chats found.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    conversations.forEach { conv ->
                        val isSelected = conv.id == activeConversationId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectConversation(conv.id) }
                                .border(
                                    if (isSelected) 1.dp else 0.dp,
                                    if (isSelected) GeminiBlue else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                ),
                            color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = conv.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = conv.model,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteConversation(conv.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Conversation",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer Clear All
            if (conversations.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Chats", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    customApiKey: String,
    onApiKeyChange: (String) -> Unit,
    systemPrompt: String,
    onSystemPromptChange: (String) -> Unit,
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = GeminiBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Studio Settings",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // API Key Field
                Column {
                    Text(
                        text = "Gemini API Key (Optional Override)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customApiKey,
                        onValueChange = onApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Uses BuildConfig.GEMINI_API_KEY by default") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Temperature Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Temperature (Creativity)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = String.format("%.1f", temperature),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = GeminiBlue
                        )
                    }
                    Slider(
                        value = temperature,
                        onValueChange = onTemperatureChange,
                        valueRange = 0.0f..1.0f,
                        steps = 9
                    )
                }

                // System Persona Instruction
                Column {
                    Text(
                        text = "System Instructions (Persona / Identity)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = onSystemPromptChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save & Close")
                }
            }
        }
    }
}

@Composable
fun CodeInspectorModal(
    language: String,
    code: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkCodeBg,
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = GeminiCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Code Inspector (${language.uppercase()})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Code Viewer
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    MarkdownMessageView(
                        markdownContent = "```$language\n$code\n```"
                    )
                }

                // Bottom actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${code.lines().size} lines • ${code.length} chars",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("code", code)
                            clipboard.setPrimaryClip(clip)
                            isCopied = true
                            Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GeminiBlue
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isCopied) "Copied!" else "Copy Code")
                    }
                }
            }
        }
    }
}
