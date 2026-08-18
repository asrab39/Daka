package com.example.ui.markdown

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkCodeBg
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GeminiPink
import com.example.ui.theme.GeminiPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(rawMarkdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawMarkdown.lines()
    var index = 0

    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            val lang = trimmed.removePrefix("```").trim()
            val codeBuilder = StringBuilder()
            index++
            while (index < lines.size && !lines[index].trim().startsWith("```")) {
                codeBuilder.appendLine(lines[index])
                index++
            }
            // Skip the closing ```
            if (index < lines.size) index++
            val code = codeBuilder.toString().trimEnd()
            blocks.add(MarkdownBlock.CodeBlock(language = if (lang.isBlank()) "code" else lang, code = code))
            continue
        }

        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            val headerText = trimmed.removePrefix("#".repeat(level)).trim()
            blocks.add(MarkdownBlock.Header(level = level.coerceIn(1, 4), text = headerText))
            index++
            continue
        }

        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
            val bulletText = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.BulletItem(bulletText))
            index++
            continue
        }

        if (trimmed.startsWith(">")) {
            val quoteText = trimmed.removePrefix(">").trim()
            blocks.add(MarkdownBlock.BlockQuote(quoteText))
            index++
            continue
        }

        if (trimmed.isNotBlank()) {
            blocks.add(MarkdownBlock.Paragraph(trimmed))
        }

        index++
    }

    return blocks
}

@Composable
fun MarkdownMessageView(
    markdownContent: String,
    modifier: Modifier = Modifier,
    onOpenCodeInspector: ((language: String, code: String) -> Unit)? = null
) {
    val blocks = remember(markdownContent) { parseMarkdownBlocks(markdownContent) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Header -> {
                    Text(
                        text = buildFormattedInlineText(block.text),
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildFormattedInlineText(block.text),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(start = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = GeminiBlue,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = buildFormattedInlineText(block.text),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                is MarkdownBlock.BlockQuote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(28.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(GeminiBlue, GeminiPurple, GeminiPink)
                                    ),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = buildFormattedInlineText(block.text),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCard(
                        language = block.language,
                        code = block.code,
                        onInspect = { onOpenCodeInspector?.invoke(block.language, block.code) }
                    )
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    language: String,
    code: String,
    modifier: Modifier = Modifier,
    onInspect: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        color = DarkCodeBg,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = GeminiBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = language.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFFCBD5E1)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Preview / Inspect Button
                    IconButton(
                        onClick = onInspect,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("inspect_code_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Inspect & Run Code",
                            tint = GeminiCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Copy Button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("code", code)
                            clipboard.setPrimaryClip(clip)
                            isCopied = true
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                            coroutineScope.launch {
                                delay(2000)
                                isCopied = false
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("copy_code_button")
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = if (isCopied) Color(0xFF34D399) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Code Content with Horizontal Scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    text = buildSyntaxHighlightedCode(code, language),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                )
            }
        }
    }
}

fun buildFormattedInlineText(rawText: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = rawText.length

        while (cursor < length) {
            val boldStart = rawText.indexOf("**", cursor)
            val inlineCodeStart = rawText.indexOf("`", cursor)

            val nextSpecial = listOf(boldStart, inlineCodeStart)
                .filter { it != -1 }
                .minOrNull() ?: -1

            if (nextSpecial == -1) {
                append(rawText.substring(cursor))
                break
            }

            if (nextSpecial > cursor) {
                append(rawText.substring(cursor, nextSpecial))
                cursor = nextSpecial
            }

            if (cursor == boldStart) {
                val boldEnd = rawText.indexOf("**", cursor + 2)
                if (boldEnd != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(rawText.substring(cursor + 2, boldEnd))
                    }
                    cursor = boldEnd + 2
                } else {
                    append("**")
                    cursor += 2
                }
            } else if (cursor == inlineCodeStart) {
                val codeEnd = rawText.indexOf("`", cursor + 1)
                if (codeEnd != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x334285F4),
                            color = GeminiBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(" ${rawText.substring(cursor + 1, codeEnd)} ")
                    }
                    cursor = codeEnd + 1
                } else {
                    append("`")
                    cursor += 1
                }
            }
        }
    }
}

fun buildSyntaxHighlightedCode(code: String, language: String): AnnotatedString {
    val keywords = setOf(
        "fun", "val", "var", "class", "interface", "data", "override", "import", "package",
        "return", "if", "else", "when", "for", "while", "try", "catch", "finally",
        "def", "import", "from", "as", "class", "return", "const", "let", "function",
        "export", "default", "async", "await", "public", "private", "protected",
        "suspend", "sealed", "object", "companion", "true", "false", "null"
    )

    val types = setOf(
        "String", "Int", "Boolean", "Float", "Double", "Long", "List", "Map", "Set",
        "Composable", "Modifier", "Column", "Row", "Box", "Text", "Button", "Card"
    )

    val lines = code.lines()
    return buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            // Line Number
            withStyle(SpanStyle(color = Color(0xFF64748B), fontSize = 11.sp)) {
                append(String.format("%2d  ", index + 1))
            }

            val tokens = line.split(Regex("(?<=\\b|\\B)(?=[^a-zA-Z0-9_])|(?<=[^a-zA-Z0-9_])(?=\\b|\\B)"))
            var inComment = line.trimStart().startsWith("//") || line.trimStart().startsWith("#")

            if (inComment) {
                withStyle(SpanStyle(color = Color(0xFF6EE7B7), fontStyle = FontStyle.Italic)) {
                    append(line)
                }
            } else {
                for (token in tokens) {
                    when {
                        token in keywords -> {
                            withStyle(SpanStyle(color = Color(0xFFFF79C6), fontWeight = FontWeight.Bold)) {
                                append(token)
                            }
                        }
                        token in types -> {
                            withStyle(SpanStyle(color = Color(0xFF8BE9FD))) {
                                append(token)
                            }
                        }
                        token.startsWith("\"") || token.endsWith("\"") || token.startsWith("'") -> {
                            withStyle(SpanStyle(color = Color(0xFFF1FA8C))) {
                                append(token)
                            }
                        }
                        token.all { it.isDigit() } -> {
                            withStyle(SpanStyle(color = Color(0xFFBD93F9))) {
                                append(token)
                            }
                        }
                        token.startsWith("@") -> {
                            withStyle(SpanStyle(color = Color(0xFFFFB86C), fontWeight = FontWeight.SemiBold)) {
                                append(token)
                            }
                        }
                        else -> {
                            withStyle(SpanStyle(color = Color(0xFFF8F8F2))) {
                                append(token)
                            }
                        }
                    }
                }
            }
            if (index < lines.size - 1) append("\n")
        }
    }
}
