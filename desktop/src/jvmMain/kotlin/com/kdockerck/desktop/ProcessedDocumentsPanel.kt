package com.kdockerck.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ProcessedDocument(
    val id: String,
    val fileName: String,
    val status: String,
    val gherkinContent: String?,
    val processedAt: kotlinx.datetime.Instant?
)

@Composable
fun ProcessedDocumentsPanel(
    documents: androidx.compose.runtime.snapshots.SnapshotStateList<ProcessedDocument>,
    selectedDocumentId: String?,
    onDocumentSelected: (String) -> Unit,
    onDocumentRemoved: (String) -> Unit,
    onCopyToClipboard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = androidx.compose.material3.tokens.Elevation.Level1
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                androidx.compose.material3.Text(
                    text = "Processed Documents",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(Modifier.weight(1f))
                
                androidx.compose.material3.TextButton(
                    onClick = {
                        val allContent = documents.joinToString("\n\n") { doc ->
                            val content = doc.gherkinContent ?: ""
                            "--- ${doc.fileName} ---\n$content\n"
                        }
                        onCopyToClipboard(allContent)
                    }
                ) {
                    androidx.compose.material3.Text("Copy All")
                }
            }
            
            androidx.compose.foundation.layout.HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            if (documents.isEmpty()) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    androidx.compose.material3.Icon(
                        image = Icons.Filled.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    androidx.compose.material3.Text(
                        text = "No processed documents yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    androidx.compose.material3.Text(
                        text = "Process documents to see them here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorSchemeScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseScroll = true
                ) {
                    items(documents) { document ->
                        ProcessedDocumentItem(
                            document = document,
                            isSelected = document.id == selectedDocumentId,
                            onClick = { onDocumentSelected(document.id) },
                            onRemove = { onDocumentRemoved(document.id) },
                            onCopy = { onCopyToClipboard(document.id) }
                        )
                        
                        androidx.compose.foundation.layout.HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessedDocumentItem(
    document: ProcessedDocument,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (isSelected) {
            androidx.compose.material3.tokens.Elevation.Level2
        } else {
            androidx.compose.material3.tokens.Elevation.Level0
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                image = Icons.Filled.Description,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                androidx.compose.material3.Text(
                    text = document.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1
                )
                
                androidx.compose.material3.Text(
                    text = "Status: ${document.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (document.status.lowercase()) {
                        "completed" -> androidx.compose.material3.tokens.Color.Green
                        "failed" -> androidx.compose.material3.tokens.Color.Red
                        "processing" -> androidx.compose.material3.tokens.Color.Blue
                        else -> androidx.compose.material3.tokens.Color.Gray
                    }
                )
                
                if (document.processedAt != null) {
                    val localDateTime = document.processedAt.toLocalDateTime()
                    androidx.compose.material3.Text(
                        text = "Processed: ${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.End
            ) {
                androidx.compose.material3.IconButton(
                    onClick = onCopy
                ) {
                    androidx.compose.material3.Icon(Icons.Filled.ContentCopy)
                }
                
                androidx.compose.material3.IconButton(
                    onClick = onRemove
                ) {
                    androidx.compose.material3.Icon(Icons.Filled.Delete)
                }
            }
        }
    }
}