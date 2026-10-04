package com.yoorme.squadsignup.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// 轻量 Markdown 渲染：标题/加粗/斜体/行内代码/代码块/引用/列表/链接/图片
// 覆盖网站公告常用语法；复杂表格等请在网页端查看

private val IMAGE_RE = Regex("""!\[([^\]]*)]\(([^)]+)\)""")

/** 提取正文里引用的图片路径（原样，通常是 /uploads/... 相对路径），按出现顺序去重 */
fun extractImageUrls(markdown: String): List<String> =
    IMAGE_RE.findAll(markdown).map { it.groupValues[2] }.distinct().toList()
private val LINK_RE = Regex("""\[([^\]]+)]\(([^)]+)\)""")
private val BOLD_RE = Regex("""\*\*(.+?)\*\*""")
private val ITALIC_RE = Regex("""\*(.+?)\*""")
private val CODE_RE = Regex("""`([^`]+)`""")

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineAnnotated(text: String, color: Color) {
    // 粗体 / 斜体 / 行内代码 / 链接 依次扫描
    val tokens = mutableListOf<Triple<IntRange, String, String>>() // range, kind, payload
    LINK_RE.findAll(text).forEach { tokens += Triple(it.range, "link", "${it.groupValues[1]}\u0000${it.groupValues[2]}") }
    BOLD_RE.findAll(text).forEach { tokens += Triple(it.range, "bold", it.groupValues[1]) }
    ITALIC_RE.findAll(text).forEach { tokens += Triple(it.range, "italic", it.groupValues[1]) }
    CODE_RE.findAll(text).forEach { tokens += Triple(it.range, "code", it.groupValues[1]) }
    if (tokens.isEmpty()) {
        append(text)
        return
    }
    tokens.sortBy { it.first.first }
    var cursor = 0
    for ((range, kind, payload) in tokens) {
        if (range.first < cursor) continue // 跳过嵌套重叠的简单场景
        if (range.first > cursor) append(text.substring(cursor, range.first))
        when (kind) {
            "bold" -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(payload) }
            "italic" -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(payload) }
            "code" -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = color.copy(alpha = 0.08f))) {
                append(payload)
            }
            "link" -> {
                val (label, _) = payload.split("\u0000")
                withStyle(SpanStyle(color = color, textDecoration = TextDecoration.Underline)) { append(label) }
            }
        }
        cursor = range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}

@Composable
fun MarkdownText(
    markdown: String,
    baseUrl: String,
    modifier: Modifier = Modifier,
    // 图片单击回调（用于进入全屏查看）；不传则图片不可点击
    onImageClick: ((String) -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        val lines = markdown.replace("\r\n", "\n").split("\n")
        var inCodeBlock = false
        val codeBuffer = StringBuilder()
        for (raw in lines) {
            val line = raw
            if (line.trimStart().startsWith("```")) {
                if (inCodeBlock) {
                    Text(
                        codeBuffer.toString().trimEnd('\n'),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    codeBuffer.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }
            if (inCodeBlock) {
                codeBuffer.appendLine(line)
                continue
            }
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> Spacer(Modifier.height(6.dp))
                trimmed.startsWith("### ") -> Heading(trimmed.removePrefix("### "), 18)
                trimmed.startsWith("## ") -> Heading(trimmed.removePrefix("## "), 20)
                trimmed.startsWith("# ") -> Heading(trimmed.removePrefix("# "), 22)
                trimmed.startsWith("> ") -> Text(
                    trimmed.removePrefix("> "),
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> Row(Modifier.padding(start = 8.dp, bottom = 2.dp)) {
                    Text("•  ")
                    Text(renderAnnotatedInline(trimmed.substring(2), MaterialTheme.colorScheme.onSurface))
                }
                IMAGE_RE.matches(trimmed) -> {
                    val m = IMAGE_RE.find(trimmed)!!
                    MarkdownImage(
                        url = m.groupValues[2],
                        description = m.groupValues[1],
                        baseUrl = baseUrl,
                        onClick = onImageClick,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    )
                }
                else -> {
                    // 行内图片混排：先渲染文本部分（图片占位剔除），再补图片
                    val images = IMAGE_RE.findAll(trimmed).toList()
                    if (images.isEmpty()) {
                        Text(renderAnnotatedInline(trimmed, MaterialTheme.colorScheme.onSurface), modifier = Modifier.padding(bottom = 2.dp))
                    } else {
                        val textOnly = IMAGE_RE.replace(trimmed, "").trim()
                        if (textOnly.isNotEmpty()) {
                            Text(renderAnnotatedInline(textOnly, MaterialTheme.colorScheme.onSurface), modifier = Modifier.padding(bottom = 2.dp))
                        }
                        for (m in images) {
                            MarkdownImage(
                                url = m.groupValues[2],
                                description = m.groupValues[1],
                                baseUrl = baseUrl,
                                onClick = onImageClick,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 正文图片：单击回调（全屏查看），相对路径自动拼服务器地址 */
@Composable
private fun MarkdownImage(
    url: String,
    description: String,
    baseUrl: String,
    onClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val full = if (url.startsWith("/")) baseUrl.trimEnd('/') + url else url
    // 图片点击不加波纹（照片上叠波纹观感差），保留点击区域即可
    val interaction = if (onClick != null) {
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { onClick(full) }
    } else {
        modifier
    }
    AsyncImage(
        model = full,
        contentDescription = description,
        modifier = interaction,
    )
}

@Composable
private fun Heading(text: String, size: Int) {
    Text(
        renderAnnotatedInline(text, MaterialTheme.colorScheme.onSurface),
        fontSize = size.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun renderAnnotatedInline(text: String, color: Color): AnnotatedString =
    buildAnnotatedString {
        appendInlineAnnotated(stripImages(text), color)
    }

private fun stripImages(text: String): String = IMAGE_RE.replace(text, "").trim()
