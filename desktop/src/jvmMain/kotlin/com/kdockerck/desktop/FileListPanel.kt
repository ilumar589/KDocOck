package com.kdockerck.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Document
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

data class DocumentInfo(
    val id: String,
    val name: String,
    val type: String,
    val status: String,
    val fileSize: Long
)

@Composable
fun FileListPanel(
    documents: SnapshotStateList<DocumentInfo>,
    selectedDocumentId: String?,
    onDocumentSelected: (String) -> Unit,
    onAddFiles: () -> Unit,
    onRemoveDocument: (String) -> Unit,
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
                Text(
                    text = "Documents",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(Modifier.weight(1f))
                
                TextButton(
                    onClick = onAddFiles
                ) {
                    Icon(Icons.Filled.Add)
                    Spacer(Modifier.weight(0).1f))
                    Text("Add Files")
                }
            }
            
            androidx.compose.foundation.layout.HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            if (documents.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        image = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = "No documents selected",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = "Click 'Add Files' to select documents",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(documents) { document ->
                        DocumentItem(
                            document = document,
                            isSelected = document.id == selectedDocumentId,
                            onClick = { onDocumentSelected(document.id) },
                            onRemove = { onRemoveDocument(document.id) }
                        )
                        
                        androidx.compose.foundation.layout.HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentItem(
    document: DocumentInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
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
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                image = Icons.Filled.Document,
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
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1
                )
                
                Text(
                    text = "${document.type} • ${formatFileSize(document.fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Text(
                text = document.status,
                style = MaterialTheme.typography.bodySmall,
                color = when (document.status.lowercase()) {
                    "completed" -> androidx.compose.material3.tokens.Color.Green
                    "failed" -> androidx.compose.material3.tokens.Color.Red
                    "processing" -> androidx.compose.material3.tokens.Color.Blue
                    else -> androidx.compose.material3.tokens.Color.Gray
                }
            )
            
            IconButton(
                onClick = onRemove
            ) {
                Icon(Icons.Filled.Delete)
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