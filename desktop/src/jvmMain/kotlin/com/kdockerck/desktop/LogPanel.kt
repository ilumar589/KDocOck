package com.kdockerck.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalScrollbar
import androidx.compose.material3.icons.filled.Clear
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime

data class LogEntry(
    val timestamp: Instant,
    val level: LogLevel,
    val tag: String,
    val message: String
)

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

@Composable
fun LogPanel(
    logs: List<LogEntry>,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = androidx.compose.material3.tokens.Elevation.Level2
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                androidx.compose.material3.Text(
                    text = "Logs",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(Modifier.weight(1f))
                
                androidx.compose.material3.IconButton(
                    onClick = {  },
                    enabled = false
                ) {
                    Icon(Icons.Filled.Clear)
                }
                
                androidx.compose.material3.IconButton(
                    onClick = onDismiss
                ) {
                    Icon(Icons.Filled.Close)
                }
            }
            
            androidx.compose.foundation.layout.HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            if (logs.isEmpty()) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    androidx.compose.material3.Text(
                        text = "No logs yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseScroll = true
                ) {
                    items(logs) { log ->
                        LogEntryItem(log)
                    }
                }
            }
        }
    }
}

@Composable
fun LogEntryItem(
    log: LogEntry
) {
    val localDateTime = log.timestamp.toLocalDateTime()
    val timestampText = "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}:${localDateTime.second.toString().padStart(2, '0')}"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.CenterVertically
    ) {
        androidx.compose.material3.Text(
            text = timestampText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.width(80.dp)
        )
        
        androidx.compose.material3.Text(
            text = log.level.name,
            style = MaterialTheme.typography.bodySmall,
            color = when (log.level) {
                LogLevel.DEBUG -> Color(0x9CA3AF)
                LogLevel.INFO -> Color(0x4CAF50)
                LogLevel.WARN -> Color(0xFFB79400)
                LogLevel.ERROR -> Color(0xFFF44336)
            },
            modifier = Modifier.width(60.dp)
        )
        
        androidx.compose.material3.Text(
            text = log.tag,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7.7f),
            modifier = Modifier.width(120.dp)
        )
        
        androidx.compose.material3.Text(
            text = log.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}