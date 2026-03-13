package com.kdockerck.desktop

import androidx.compose.desktop.LocalFileDialog
import androidx.compose.desktop.WindowDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Add
import androidx.compose.material3.icons.filled.Close
import androidx.compose.material3.icons.filled.FolderOpen
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.io.File

@Composable
fun FileSelectionDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onFilesSelected: (List<File>) -> Unit,
    supportedExtensions: List<String> = listOf(".docx", ".xlsx", ".vsdx")
) {
    val files = mutableStateOf<List<File>>(emptyList())
    
    androidx.compose.desktop.ui.window.Dialog(
        visible = visible,
        onCloseRequest = { onDismiss() },
        title = "Select Files",
        resizable = true,
        state = rememberFileDialogState()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Select documents to process",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            val selectedFiles = LocalFileDialog.currentDirectory.chooseFiles(
                                title = "Select Documents",
                                description = "Supported files: ${supportedExtensions.joinToString(", ")}",
                                filters = listOf(
                                    androidx.compose.desktop.ui.window.FileFilter(
                                        description = "Supported Documents",
                                        extensions = supportedExtensions
                                    )
                                )
                            )
                            
                            if (selectedFiles.isNotEmpty()) {
                                files.value = selectedFiles
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FolderOpen)
                        Spacer(Modifier.weight(0.1f))
                        Text("Browse Files")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val selectedDirectory = LocalFileDialog.currentDirectory.chooseDirectory(
                                title = "Select Output Directory"
                            )
                            
                            if (selectedDirectory != null) {
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FolderOpen)
                        Spacer(Modifier.weight(0.1f))
                        Text("Output Directory")
                    }
                }
                
                if (files.value.isNotEmpty()) {
                    androidx.compose.foundation.layout.VerticalDivider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Text(
                        text = "Selected Files (${files.value.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    files.value.forEach { file ->
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
                
                androidx.compose.foundation.layout.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onDismiss() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(Modifier.weight(1f))
                    
                    Button(
                        onClick = {
                            if (files.value.isNotEmpty()) {
                                onFilesSelected(files.value)
                                onDismiss()
                            }
                        },
                        enabled = files.value.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Add)
                        Spacer(Modifier.weight(0.1f))
                        Text("Add Files")
                    }
                }
            }
        }
    }
}

@Composable
fun rememberFileDialogState(): androidx.compose.ui.window.FileDialogState {
    return androidx.compose.ui.window.rememberFileDialogState()
}