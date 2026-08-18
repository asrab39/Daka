package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ChatInputBar
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.CodeInspectorModal
import com.example.ui.components.DrawerChatList
import com.example.ui.components.GeminiThinkingIndicator
import com.example.ui.components.GeminiTopBar
import com.example.ui.components.SettingsDialog
import com.example.ui.components.WelcomeHeroSection
import kotlinx.coroutines.launch

@Composable
fun GeminiAppScreen(
    viewModel: GeminiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val attachedImageBase64 by viewModel.attachedImageBase64.collectAsStateWithLifecycle()
    val activeModel by viewModel.activeModel.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val systemPrompt by viewModel.systemPrompt.collectAsStateWithLifecycle()
    val temperature by viewModel.temperature.collectAsStateWithLifecycle()
    val codeInspectorTarget by viewModel.codeInspectorTarget.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    // Auto-scroll when messages change
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.attachImageUri(context, uri)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerChatList(
                    conversations = conversations,
                    activeConversationId = activeConversationId,
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onSelectConversation = { id ->
                        viewModel.selectConversation(id)
                        scope.launch { drawerState.close() }
                    },
                    onNewChat = {
                        viewModel.startNewChat()
                        scope.launch { drawerState.close() }
                    },
                    onDeleteConversation = viewModel::deleteConversation,
                    onClearAll = viewModel::clearAllConversations
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            topBar = {
                GeminiTopBar(
                    activeModel = activeModel,
                    onModelSelected = viewModel::onModelSelected,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenSettings = viewModel::openSettings,
                    onNewChat = viewModel::startNewChat,
                    onClearChat = viewModel::clearCurrentChat
                )
            },
            bottomBar = {
                ChatInputBar(
                    inputText = inputText,
                    onInputChange = viewModel::onInputTextChange,
                    onSend = { viewModel.sendMessage() },
                    onAttachImage = { imagePickerLauncher.launch("image/*") },
                    attachedImageBase64 = attachedImageBase64,
                    onRemoveImage = viewModel::removeAttachedImage,
                    isLoading = isLoading
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (messages.isEmpty() && !isLoading) {
                    WelcomeHeroSection(
                        onPromptSelected = { prompt ->
                            viewModel.sendMessage(prompt)
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChatMessageItem(
                                message = msg,
                                onSpeak = viewModel::speakText,
                                onOpenCodeInspector = viewModel::openCodeInspector
                            )
                        }

                        if (isLoading) {
                            item {
                                GeminiThinkingIndicator()
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }

    // Code Inspector Dialog
    if (codeInspectorTarget != null) {
        val (lang, code) = codeInspectorTarget!!
        CodeInspectorModal(
            language = lang,
            code = code,
            onDismiss = viewModel::closeCodeInspector
        )
    }

    // Settings & Persona Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            customApiKey = customApiKey,
            onApiKeyChange = viewModel::onApiKeyChange,
            systemPrompt = systemPrompt,
            onSystemPromptChange = viewModel::onSystemPromptChange,
            temperature = temperature,
            onTemperatureChange = viewModel::onTemperatureChange,
            onDismiss = viewModel::closeSettings
        )
    }
}
