package com.kdockerck.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CurrentFileDisplay(
    fileName: String?,
    fileSize: Long?,
    fileType: String?,
    status: String?,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = androidx.compose.material3.tokens.Elevation.Level1
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    image = Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    androidx.compose.material3.Text(
                        text = fileName ?: "No file selected",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (fileSize != null && fileType != null) {
                        androidx.compose.material3.Text(
                            text = "$fileType • ${formatFileSize(fileSize)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (status != null) {
                androidx.compose.material3.Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (status.lowercase()) {
                        "completed" -> androidx.compose.material3.tokens.Color.Green
                        "failed" -> androidx.compose.material3.tokens.Color.Red
                        "processing" -> androidx.compose.material3.tokens.Color.Blue
                        else -> androidx.compose.material3.tokens.Color.Gray
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}