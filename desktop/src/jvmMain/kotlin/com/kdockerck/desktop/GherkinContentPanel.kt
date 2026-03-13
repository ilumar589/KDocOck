package com.kdockerck.desktop

import androidx.compose.foundation.background.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Tokens
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.isWindowDark

@Composable
fun GherkinContentPanel(
    gherkinContent: String,
    modifier: Modifier = Modifier
) {
    val isDark = isWindowDark()
    val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        color = colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            SelectionContainer {
                Text(
                    text = gherkinContent,
                    modifier = Modifier.fillMaxSize(),
                    style = androidx.compose.ui.text.TextStyle(
                        color = colorScheme.onBackground,
                        fontFamily = androidx.compose.ui.text.FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

@Composable
fun GherkinSyntaxHighlighter(
    content: String,
    modifier: Modifier = Modifier
) {
    val isDark = isWindowDark()
    val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
    
    val lines = content.lines()
    
    LazyColumn(
        modifier = modifier
    ) {
        items(lines) { line ->
            val (keyword, rest) = parseGherkinLine(line)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                if (keyword != null) {
                    Text(
                        text = keyword,
                        color = getKeywordColor(keyword, colorScheme),
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.FontFamily.Monospace,
                            fontWeight = androidx.compose.ui.text.FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
                
                if (rest.isNotEmpty()) {
                    Text(
                        text = rest,
                        color = colorScheme.onBackground,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}

private fun parseGherkinLine(line: String): Pair<String?, String> {
    val trimmedLine = line.trim()
    
    val keywordRegex = Regex("""^(Feature|Scenario|Given|When|Then|And|But|Background|Examples):?\s+""", RegexOption.IGNORE_CASE)
    val match = keywordRegex.find(trimmedLine)
    
    if (match != null) {
        val keyword = match.value.trim()
        val rest = trimmedLine.substringAfter(match.value).trim()
        return Pair(keyword, rest)
    }
    
    return Pair(null, trimmedLine)
}

private fun getKeywordColor(keyword: String, colorScheme: androidx.compose.material3.ColorScheme): androidx.compose.ui.graphics.Color {
    return when (keyword.lowercase()) {
        "feature" -> colorScheme.primary
        "scenario" -> colorScheme.secondary
        "given", "when", "then" -> colorScheme.tertiary
        "and", "but" -> colorScheme.secondary
        "background" -> colorScheme.error
        "examples" -> colorScheme.primary
        else -> colorScheme.onBackground
    }
}