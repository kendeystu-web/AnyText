package com.anytext.app.ui.reader

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anytext.app.logic.HexDump
import com.anytext.app.logic.LoadedFile

@Composable
fun HexView(file: LoadedFile, hScroll: ScrollState) {
    val bytes = file.bytes
    val rows = HexDump.rowCount(bytes)

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "СМЕЩЕНИЕ   16-РИЧНЫЕ БАЙТЫ (16 на строку)        ASCII",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.outline
        )
        if (rows == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Файл пуст",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).horizontalScroll(hScroll)) {
                items(rows) { r ->
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            ) {
                                append(HexDump.offset(r))
                            }
                            append("  ")
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append(HexDump.hex(bytes, r))
                            }
                            append("  ")
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append(HexDump.ascii(bytes, r))
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
