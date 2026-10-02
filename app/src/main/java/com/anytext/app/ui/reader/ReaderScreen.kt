@file:OptIn(ExperimentalMaterial3Api::class)

package com.anytext.app.ui.reader

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anytext.app.logic.Encodings
import com.anytext.app.logic.Fmt
import com.anytext.app.logic.LoadedFile
import com.anytext.app.logic.Syntax
import com.anytext.app.logic.lineOfOffset
import com.anytext.app.ui.theme.fontFamilyOf
import com.anytext.app.viewmodel.AppViewModel
import com.anytext.app.viewmodel.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val KwColor = Color(0xFF9B8CFF)
private val StrColor = Color(0xFF7BE39A)
private val ComColor = Color(0xFF8A93A8)
private val NumColor = Color(0xFF54D6E8)
private val FnColor = Color(0xFFFF8FB8)

private fun codeStyleN(kind: Syntax.Kind): SpanStyle = when (kind) {
    Syntax.Kind.KEYWORD -> SpanStyle(color = KwColor, fontWeight = FontWeight.SemiBold)
    Syntax.Kind.STRING -> SpanStyle(color = StrColor)
    Syntax.Kind.COMMENT -> SpanStyle(color = ComColor, fontStyle = FontStyle.Italic)
    Syntax.Kind.NUMBER -> SpanStyle(color = NumColor)
    Syntax.Kind.FUNC -> SpanStyle(color = FnColor)
}

private data class HighlightData(val full: AnnotatedString, val lines: List<AnnotatedString>)

private fun buildHighlight(text: String, lang: Syntax.Lang): HighlightData {
    val lines = text.split('\n')
    val h1 = Syntax.highlighter(lang)
    val full = buildAnnotatedString {
        lines.forEachIndexed { i, line ->
            if (i > 0) append('\n')
            val base = length
            append(line)
            h1.lineSpans(line).forEach { s ->
                if (s.e > s.s) addStyle(codeStyleN(s.kind), base + s.s, base + s.e)
            }
        }
    }
    val h2 = Syntax.highlighter(lang)
    val perLine = lines.map { line ->
        buildAnnotatedString {
            append(line)
            h2.lineSpans(line).forEach { s ->
                if (s.e > s.s) addStyle(codeStyleN(s.kind), s.s, s.e)
            }
        }
    }
    return HighlightData(full, perLine)
}

private fun annotateCode(text: String, lang: Syntax.Lang): AnnotatedString = buildAnnotatedString {
    append(text)
    val hl = Syntax.highlighter(lang)
    var base = 0
    text.split('\n').forEach { line ->
        hl.lineSpans(line).forEach { s ->
            if (s.e > s.s) addStyle(codeStyleN(s.kind), base + s.s, base + s.e)
        }
        base += line.length + 1
    }
}

@Composable
fun ReaderScreen(spec: String, appVm: AppViewModel, onBack: () -> Unit) {
    val vm: ReaderViewModel = viewModel()
    val ui by vm.ui.collectAsState()
    val settings by appVm.settings.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(spec) { vm.load(spec) }

    val flowScroll = rememberScrollState()
    val listState = rememberLazyListState()
    val hexHScroll = rememberScrollState()

    var searchOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showEncoding by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showJump by remember { mutableStateOf(false) }
    var showFont by remember { mutableStateOf(false) }
    var binaryBannerDismissed by remember { mutableStateOf(false) }

    val fontFamily = fontFamilyOf(settings.fontFamily)

    val saveAndBack: () -> Unit = {
        val ratio = when {
            ui.hexMode -> 0f
            settings.lineNumbers -> {
                val t = listState.layoutInfo.totalItemsCount
                if (t == 0) 0f else (listState.firstVisibleItemIndex.toFloat() / t).coerceIn(0f, 1f)
            }
            flowScroll.maxValue <= 0 -> 0f
            else -> (flowScroll.value.toFloat() / flowScroll.maxValue).coerceIn(0f, 1f)
        }
        if (ui.file != null) vm.saveProgress(ratio)
        onBack()
    }
    BackHandler(enabled = !ui.editing) { saveAndBack() }

    val flowP by remember {
        derivedStateOf {
            if (flowScroll.maxValue <= 0) 0f
            else (flowScroll.value.toFloat() / flowScroll.maxValue).coerceIn(0f, 1f)
        }
    }
    val listP by remember {
        derivedStateOf {
            val t = listState.layoutInfo.totalItemsCount
            if (t == 0) 0f else (listState.firstVisibleItemIndex.toFloat() / t).coerceIn(0f, 1f)
        }
    }
    val progress = if (ui.hexMode) 0f else if (settings.lineNumbers) listP else flowP

    // восстановление сохранённой позиции чтения
    LaunchedEffect(ui.file?.uri) {
        val p = ui.savedProgress
        if (p > 0.005f) {
            if (settings.lineNumbers) {
                val t = listState.layoutInfo.totalItemsCount
                if (t > 0) listState.scrollToItem((p * t).toInt().coerceIn(0, t - 1))
            } else {
                snapshotFlow { flowScroll.maxValue }.first { it > 0 }
                flowScroll.scrollTo((p * flowScroll.maxValue).toInt())
            }
        }
    }

    // автоскролл к текущему совпадению в режиме строк
    LaunchedEffect(ui.currentMatch, settings.lineNumbers) {
        val m = ui.currentMatch
        if (m >= 0 && !ui.hexMode && settings.lineNumbers && ui.matches.isNotEmpty()) {
            val line = lineOfOffset(ui.lineStarts, ui.matches[m])
            listState.animateScrollToItem(line.coerceIn(0, maxOf(0, ui.lines.size - 1)))
        }
    }

    // подсветка синтаксиса (считается в фоне, у больших файлов отключается)
    val lang = ui.lang
    val canHighlight = settings.syntaxHighlight && lang != null &&
        ui.text.length <= 600_000 && !ui.editing && !ui.hexMode
    val hl by produceState<HighlightData?>(initialValue = null, ui.text, lang, canHighlight) {
        value = if (canHighlight && lang != null) {
            withContext(Dispatchers.Default) { buildHighlight(ui.text, lang) }
        } else {
            null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReaderTopBar(
            file = ui.file,
            hexMode = ui.hexMode,
            wrap = settings.wrap,
            lineNumbers = settings.lineNumbers,
            syntaxHighlight = settings.syntaxHighlight,
            lang = ui.lang,
            units = settings.units,
            menuOpen = menuOpen,
            onBack = saveAndBack,
            onToggleSearch = {
                searchOpen = !searchOpen
                if (!searchOpen) vm.setQuery("")
            },
            onMenu = { menuOpen = true },
            onDismissMenu = { menuOpen = false },
            onToggleHex = { vm.setHexMode(!ui.hexMode) },
            onBigger = { appVm.updateSettings { it.copy(fontSize = (it.fontSize + 1).coerceAtMost(32f)) } },
            onSmaller = { appVm.updateSettings { it.copy(fontSize = (it.fontSize - 1).coerceAtLeast(10f)) } },
            onFont = { showFont = true },
            onToggleWrap = { appVm.updateSettings { it.copy(wrap = !it.wrap) } },
            onToggleLineNumbers = { appVm.updateSettings { it.copy(lineNumbers = !it.lineNumbers) } },
            onToggleSyntax = { appVm.updateSettings { it.copy(syntaxHighlight = !it.syntaxHighlight) } },
            onStats = { showStats = true },
            onCopy = {
                if (ui.text.isEmpty()) {
                    toast(context, "Пусто")
                } else {
                    clipboard.setText(AnnotatedString(ui.text))
                    toast(context, "Скопировано ${Fmt.num(ui.text.length)} символов")
                }
            },
            onShare = { shareText(context, ui.text) }
        )

        AnimatedVisibility(visible = searchOpen && !ui.editing) {
            SearchRow(
                query = ui.query,
                matchCount = ui.matches.size,
                currentIndex = ui.currentMatch,
                ignoreCase = ui.ignoreCase,
                onQuery = vm::setQuery,
                onIgnoreCase = vm::setIgnoreCase,
                onNext = vm::nextMatch,
                onPrev = vm::prevMatch,
                onClose = {
                    vm.setQuery("")
                    searchOpen = false
                }
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )

        ModeRow(
            hexMode = ui.hexMode,
            onSelectMode = vm::setHexMode,
            showJump = settings.lineNumbers && !ui.hexMode && !ui.loading && ui.error == null,
            onJump = { showJump = true },
            editable = ui.editablePath != null && !ui.loading && ui.error == null && !ui.hexMode,
            editing = ui.editing,
            saving = ui.saving,
            onStartEdit = { vm.startEdit() },
            onCancelEdit = { vm.cancelEdit() },
            onSave = { vm.saveEdit() }
        )

        val f = ui.file
        if (f != null && !ui.hexMode && f.isBinary && !binaryBannerDismissed && !ui.editing) {
            Banner(
                text = "Похоже, это двоичный файл — в виде текста он будет «кашей».",
                actionLabel = "Открыть HEX",
                onAction = { vm.setHexMode(true) },
                onDismiss = { binaryBannerDismissed = true }
            )
        }
        if (f != null && f.truncated && !ui.busy && f.realSize > f.loadedSize && !ui.editing) {
            Banner(
                text = "Загружено ${Fmt.size(f.loadedSize, settings.units)} из ${Fmt.size(f.realSize, settings.units)}.",
                actionLabel = "Догрузить",
                onAction = { vm.loadMore() },
                onDismiss = null
            )
        }
        ui.notice?.let {
            Banner(text = it, actionLabel = null, onAction = null, onDismiss = { vm.dismissNotice() })
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                ui.loading -> Loader()
                ui.error != null -> ErrorView(
                    message = ui.error!!,
                    onRetry = { vm.load(spec) },
                    onBack = saveAndBack
                )
                ui.editing -> EditView(
                    draft = ui.draft,
                    lang = lang,
                    liveHl = settings.syntaxHighlight && ui.draft.length <= 128_000,
                    fontFamily = fontFamily,
                    fontSize = settings.fontSize,
                    onDraft = vm::setDraft
                )
                f != null -> AnimatedContent(
                    targetState = ui.hexMode,
                    transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(160)) },
                    label = "mode"
                ) { hex ->
                    if (hex) {
                        HexView(file = f, hScroll = hexHScroll)
                    } else {
                        TextView(
                            ui = ui,
                            wrap = settings.wrap,
                            lineNumbers = settings.lineNumbers,
                            fontFamily = fontFamily,
                            fontSize = settings.fontSize,
                            flowScroll = flowScroll,
                            listState = listState,
                            hl = hl
                        )
                    }
                }
            }
        }

        if (f != null && !ui.loading && ui.error == null) {
            BottomStatsBar(
                file = f,
                ui = ui,
                progress = progress,
                showPosition = !ui.hexMode,
                units = settings.units,
                onEncoding = { showEncoding = true }
            )
        }
    }

    if (showEncoding && ui.file != null) {
        EncodingDialog(
            ui = ui,
            onSelect = {
                vm.setEncoding(it)
                showEncoding = false
            },
            onDismiss = { showEncoding = false }
        )
    }
    if (showStats && ui.file != null) {
        StatsDialog(f = ui.file!!, ui = ui, units = settings.units, onDismiss = { showStats = false })
    }
    if (showJump) {
        JumpDialog(
            totalLines = ui.lines.size,
            onJump = { n ->
                scope.launch {
                    listState.scrollToItem((n - 1).coerceIn(0, maxOf(0, ui.lines.size - 1)))
                }
            },
            onDismiss = { showJump = false }
        )
    }
    if (showFont) {
        FontDialog(
            current = settings.fontFamily,
            onSelect = { value ->
                appVm.updateSettings { it.copy(fontFamily = value) }
            },
            onDismiss = { showFont = false }
        )
    }
}

@Composable
private fun ReaderTopBar(
    file: LoadedFile?,
    hexMode: Boolean,
    wrap: Boolean,
    lineNumbers: Boolean,
    syntaxHighlight: Boolean,
    lang: Syntax.Lang?,
    units: String,
    menuOpen: Boolean,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onToggleHex: () -> Unit,
    onBigger: () -> Unit,
    onSmaller: () -> Unit,
    onFont: () -> Unit,
    onToggleWrap: () -> Unit,
    onToggleLineNumbers: () -> Unit,
    onToggleSyntax: () -> Unit,
    onStats: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = file?.name ?: "Читалка",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file?.let {
                        val extLabel = if (it.ext.isEmpty()) "без расш." else ".${it.ext}"
                        val langLabel = lang?.let { l -> " · ${Syntax.label(l)}" } ?: ""
                        "$extLabel · ${Fmt.size(it.realSize, units)}$langLabel"
                    } ?: "открываем…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
        },
        actions = {
            IconButton(onClick = onToggleSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Поиск")
            }
            IconButton(onClick = onMenu) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Меню")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(
                    text = { Text(if (hexMode) "Показать как текст" else "Показать как HEX") },
                    leadingIcon = {
                        Icon(if (hexMode) Icons.Filled.TextFields else Icons.Filled.Code, null)
                    },
                    onClick = {
                        onToggleHex()
                        onDismissMenu()
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Крупнее") },
                    leadingIcon = { Icon(Icons.Filled.Add, null) },
                    onClick = { onBigger(); onDismissMenu() }
                )
                DropdownMenuItem(
                    text = { Text("Мельче") },
                    leadingIcon = { Icon(Icons.Filled.Remove, null) },
                    onClick = { onSmaller(); onDismissMenu() }
                )
                DropdownMenuItem(
                    text = { Text("Гарнитура…") },
                    leadingIcon = { Icon(Icons.Filled.FontDownload, null) },
                    onClick = { onFont(); onDismissMenu() }
                )
                DropdownMenuItem(
                    text = { Text("Перенос строк") },
                    trailingIcon = { Checkbox(checked = wrap, onCheckedChange = null) },
                    onClick = onToggleWrap
                )
                DropdownMenuItem(
                    text = { Text("Номера строк") },
                    trailingIcon = { Checkbox(checked = lineNumbers, onCheckedChange = null) },
                    onClick = onToggleLineNumbers
                )
                DropdownMenuItem(
                    text = { Text("Подсветка синтаксиса") },
                    trailingIcon = { Checkbox(checked = syntaxHighlight, onCheckedChange = null) },
                    onClick = onToggleSyntax
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Статистика") },
                    leadingIcon = { Icon(Icons.Filled.QueryStats, null) },
                    onClick = { onStats(); onDismissMenu() }
                )
                DropdownMenuItem(
                    text = { Text("Копировать всё") },
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                    onClick = { onCopy(); onDismissMenu() }
                )
                DropdownMenuItem(
                    text = { Text("Поделиться") },
                    leadingIcon = { Icon(Icons.Filled.Share, null) },
                    onClick = { onShare(); onDismissMenu() }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SearchRow(
    query: String,
    matchCount: Int,
    currentIndex: Int,
    ignoreCase: Boolean,
    onQuery: (String) -> Unit,
    onIgnoreCase: (Boolean) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("Поиск по файлу…", style = MaterialTheme.typography.bodyMedium)
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            trailingIcon = {
                if (matchCount > 0) {
                    Text(
                        text = "${currentIndex + 1}/$matchCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        )
        Spacer(Modifier.width(6.dp))
        FilterChip(
            selected = ignoreCase,
            onClick = { onIgnoreCase(!ignoreCase) },
            label = { Text("Aa") }
        )
        IconButton(onClick = onPrev, enabled = matchCount > 0) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Предыдущее совпадение")
        }
        IconButton(onClick = onNext, enabled = matchCount > 0) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Следующее совпадение")
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Закрыть поиск")
        }
    }
}

@Composable
private fun ModeRow(
    hexMode: Boolean,
    onSelectMode: (Boolean) -> Unit,
    showJump: Boolean,
    onJump: () -> Unit,
    editable: Boolean,
    editing: Boolean,
    saving: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (editing) {
            TextButton(onClick = onSave, enabled = !saving) {
                Icon(
                    Icons.Filled.Save,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (saving) "Сохранение…" else "Сохранить")
            }
            TextButton(onClick = onCancelEdit) { Text("Отмена") }
        } else {
            ModeToggle(hexMode, onSelectMode)
            Spacer(Modifier.weight(1f))
            if (editable) {
                IconButton(onClick = onStartEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
                }
            }
            if (showJump) {
                TextButton(onClick = onJump) {
                    Icon(
                        Icons.Filled.FormatListNumbered,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("К строке", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(hex: Boolean, onSelect: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(Modifier.padding(3.dp)) {
            ToggleItem("Текст", !hex) { onSelect(false) }
            ToggleItem("HEX", hex) { onSelect(true) }
        }
    }
}

@Composable
private fun ToggleItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "toggleBg"
    )
    Box(
        Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun Banner(
    text: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    onDismiss: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Скрыть",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun TextView(
    ui: ReaderViewModel.Ui,
    wrap: Boolean,
    lineNumbers: Boolean,
    fontFamily: FontFamily,
    fontSize: Float,
    flowScroll: ScrollState,
    listState: LazyListState,
    hl: HighlightData?
) {
    if (lineNumbers) {
        LinesView(ui, wrap, fontFamily, fontSize, listState, hl)
    } else {
        FlowView(ui, wrap, fontFamily, fontSize, flowScroll, hl)
    }
}

@Composable
private fun FlowView(
    ui: ReaderViewModel.Ui,
    wrap: Boolean,
    fontFamily: FontFamily,
    fontSize: Float,
    flowScroll: ScrollState,
    hl: HighlightData?
) {
    val matchColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)
    val currentColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
    SelectionContainer(Modifier.fillMaxSize()) {
        val annotated = remember(
            ui.text, ui.matches, ui.currentMatch, ui.query, matchColor, currentColor, hl
        ) {
            if (hl != null) {
                mergeFull(hl.full, ui.text, ui.matches, ui.currentMatch, ui.query, matchColor, currentColor)
            } else {
                buildHighlighted(ui.text, ui.matches, ui.currentMatch, ui.query.length, matchColor, currentColor)
            }
        }
        val m = if (wrap) {
            Modifier
                .fillMaxSize()
                .verticalScroll(flowScroll)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        } else {
            Modifier
                .fillMaxSize()
                .verticalScroll(flowScroll)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp)
        }
        Text(
            text = annotated,
            modifier = m,
            fontFamily = fontFamily,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.5f).sp,
            softWrap = wrap,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LinesView(
    ui: ReaderViewModel.Ui,
    wrap: Boolean,
    fontFamily: FontFamily,
    fontSize: Float,
    listState: LazyListState,
    hl: HighlightData?
) {
    val matchColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)
    val currentColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
    val query = ui.query
    val ignoreCase = ui.ignoreCase
    val currentAbs = if (ui.currentMatch in ui.matches.indices) ui.matches[ui.currentMatch] else -1

    val rows: LazyListScope.() -> Unit = {
        items(ui.lines.size) { i ->
            val lineText = ui.lines[i]
            val ls = if (i < ui.lineStarts.size) ui.lineStarts[i] else 0
            val curLocal =
                if (currentAbs >= ls && currentAbs < ls + lineText.length) currentAbs - ls else -1
            val base = hl?.lines?.getOrNull(i)
            val annotated = remember(lineText, base, query, ignoreCase, curLocal, matchColor, currentColor) {
                mergedLine(base, lineText, query, ignoreCase, curLocal, matchColor, currentColor)
            }
            Row(Modifier.padding(horizontal = 10.dp, vertical = 1.dp)) {
                Text(
                    text = "${i + 1}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .width(50.dp)
                        .padding(end = 10.dp)
                )
                Text(
                    text = annotated,
                    fontFamily = fontFamily,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.45f).sp,
                    softWrap = wrap,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (wrap) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) { rows() }
    } else {
        Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxHeight()) { rows() }
        }
    }
}

@Composable
private fun EditView(
    draft: String,
    lang: Syntax.Lang?,
    liveHl: Boolean,
    fontFamily: FontFamily,
    fontSize: Float,
    onDraft: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            text = "Редактирование" + (lang?.let { " · ${Syntax.label(it)}" } ?: ""),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BasicTextField(
            value = draft,
            onValueChange = onDraft,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            textStyle = TextStyle(
                fontFamily = fontFamily,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.5f).sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = if (liveHl && lang != null) {
                VisualTransformation { t ->
                    TransformedText(annotateCode(t.text, lang), OffsetMapping.Identity)
                }
            } else {
                VisualTransformation.None
            }
        )
    }
}

@Composable
private fun Loader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                "Открываем файл…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Row {
                Button(onClick = onRetry) { Text("Повторить") }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(onClick = onBack) { Text("Назад") }
            }
        }
    }
}

@Composable
private fun BottomStatsBar(
    file: LoadedFile,
    ui: ReaderViewModel.Ui,
    progress: Float,
    showPosition: Boolean,
    units: String,
    onEncoding: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatChip(
                    text = ui.effectiveEncodingLabel.ifEmpty { "кодировка" },
                    onClick = onEncoding,
                    accent = true
                )
                val st = ui.stats
                if (st != null) {
                    StatChip("${Fmt.num(st.lines)} стр.")
                    StatChip("${Fmt.num(st.words)} слов")
                    StatChip("${Fmt.num(st.chars)} симв.")
                }
                StatChip(Fmt.size(file.loadedSize, units))
                if (showPosition) {
                    StatChip("${(progress * 100).roundToInt()} %")
                }
            }
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun StatChip(text: String, onClick: (() -> Unit)? = null, accent: Boolean = false) {
    val bg = if (accent) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface
    val fg = if (accent) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = fg
        )
    }
}

@Composable
private fun EncodingDialog(
    ui: ReaderViewModel.Ui,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Кодировка") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Сейчас применяется: ${ui.effectiveEncodingLabel.ifEmpty { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Encodings.available().forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(value) }
                            .padding(horizontal = 4.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = ui.encodingName == value, onClick = null)
                        Spacer(Modifier.width(10.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
}

@Composable
private fun StatsDialog(f: LoadedFile, ui: ReaderViewModel.Ui, units: String, onDismiss: () -> Unit) {
    val st = ui.stats
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Статистика") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatRow("Файл", f.name)
                StatRow("Расширение", f.ext.ifEmpty { "— (открыт как текст)" })
                StatRow("Размер", Fmt.size(f.realSize, units))
                StatRow(
                    "Загружено",
                    Fmt.size(f.loadedSize, units) + if (f.truncated) " (обрезано)" else ""
                )
                StatRow("Кодировка", ui.effectiveEncodingLabel)
                ui.lang?.let { StatRow("Синтаксис", Syntax.label(it)) }
                if (st != null) {
                    StatRow("Строки", Fmt.num(st.lines))
                    StatRow("Слова", Fmt.num(st.words))
                    StatRow("Символы", Fmt.num(st.chars))
                }
                StatRow("Двоичный", if (f.isBinary) "да" else "нет")
                StatRow("Редактирование", if (ui.editablePath != null) "доступно" else "только чтение")
                if (ui.query.isNotEmpty()) StatRow("Совпадений", "${ui.matches.size}")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.width(110.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun JumpDialog(totalLines: Int, onJump: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Перейти к строке") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() } },
                placeholder = { Text(if (totalLines > 0) "1…$totalLines" else "—") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                text.toIntOrNull()?.let(onJump)
                onDismiss()
            }) { Text("Перейти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun FontDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        "sans" to "Стандартный (Sans)",
        "serif" to "Книжный (Serif)",
        "mono" to "Моноширинный (Mono)"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Гарнитура") },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(value) }
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = current == value, onClick = null)
                        Spacer(Modifier.width(10.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
}

private fun buildHighlighted(
    text: String,
    matches: List<Int>,
    current: Int,
    queryLen: Int,
    matchColor: Color,
    currentColor: Color
): AnnotatedString = buildAnnotatedString {
    append(text)
    if (queryLen <= 0) return@buildAnnotatedString
    matches.forEachIndexed { i, s ->
        val e = minOf(s + queryLen, text.length)
        if (s < e) {
            addStyle(
                SpanStyle(background = if (i == current) currentColor else matchColor),
                s,
                e
            )
        }
    }
}

private fun mergeFull(
    base: AnnotatedString,
    text: String,
    matches: List<Int>,
    current: Int,
    query: String,
    matchColor: Color,
    currentColor: Color
): AnnotatedString = buildAnnotatedString {
    append(base.text)
    base.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
    if (query.isEmpty()) return@buildAnnotatedString
    matches.forEachIndexed { i, s ->
        val e = minOf(s + query.length, text.length)
        if (s < e) {
            addStyle(
                SpanStyle(background = if (i == current) currentColor else matchColor),
                s,
                e
            )
        }
    }
}

private fun mergedLine(
    base: AnnotatedString?,
    line: String,
    query: String,
    ignoreCase: Boolean,
    currentLocal: Int,
    matchColor: Color,
    currentColor: Color
): AnnotatedString = buildAnnotatedString {
    if (base != null) {
        append(base.text)
        base.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
    } else {
        append(line)
    }
    if (query.isEmpty()) return@buildAnnotatedString
    val hay = if (ignoreCase) line.lowercase() else line
    val needle = if (ignoreCase) query.lowercase() else query
    if (needle.isEmpty()) return@buildAnnotatedString
    var idx = hay.indexOf(needle)
    while (idx >= 0) {
        addStyle(
            SpanStyle(background = if (idx == currentLocal) currentColor else matchColor),
            idx,
            idx + needle.length
        )
        idx = hay.indexOf(needle, idx + needle.length)
    }
}

private fun toast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    if (text.isEmpty()) {
        toast(context, "Пусто")
        return
    }
    val body = if (text.length > 500_000) {
        text.substring(0, 500_000) + "\n…(текст обрезан для отправки)"
    } else {
        text
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(send, "Поделиться текстом"))
}
