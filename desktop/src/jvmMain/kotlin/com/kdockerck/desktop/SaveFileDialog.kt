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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Material
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.materialariant3.TextButton
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Check
import androidx.compose.material3.icons.filled.Close
import androidx.compose.material3.icons.filled.FolderOpen
import androidx.compose.material3.icons.filled.Save
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextField
import androidx.compose.material3.icons.filled.Warning
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.io.File

@Composable
fun SaveFileDialog(
    visible: Boolean,
    content: String,
    fileName: String,
    onDismiss: () -> Unit,
    onSave: (File) -> Unit
) {
    var selectedDirectory by mutableStateOf<File?>(null)
    var fileNameValue by mutableStateOf(fileName)
    var showDirectoryError by mutableStateOf(false)
    
    androidx.compose.ui.window.Dialog(
        visible = visible,
        onCloseRequest = { onDismiss() },
        title = "Save Gherkin File",
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
                    text = "Save Gherkin file",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                androidx.compose.material3.OutlinedTextField(
                    value = fileNameValue,
                    onValueChange = { fileNameValue = it },
                    label = "File name",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        val directory = LocalFileDialog.currentDirectory.chooseDirectory(
                            title = "Select Output Directory"
                        )
                        
                        if (directory != null) {
                            selectedDirectory = directory
                            showDirectoryError = false
                        }
) {
                    Icon(Icons.Filled.FolderOpen)
                    Spacer(Modifier.weight(0).1f))
                    Text("Browse")
                }
                
                if (selectedDirectory != null) {
                    Text(
                        text = "Output directory: ${selectedDirectory.absolutePath}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                if (showDirectoryError) {
                    Row(
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            image = Icons.Filled.Warning,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Please select a valid directory",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                androidx.compose.foundation.layout.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                Text(
                    text = "Preview:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = androidx.compose.material3.tokens.Elevation.Level1
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = content,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = androidx.compose.ui.text.FontFamily.Monospace,
                                fontSize = 14.sp.sp
                            )
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
                            val directory = selectedDirectory
                            
                            if (directory != null) {
                                val file = File(directory, fileNameValue)
                                
                                try {
                                    file.writeText(content)
                                    onSave(file)
                                    onDismiss()
                                } catch (e: Exception) {
                                    showDirectoryError = true
                                }
                            }
                        },
                        enabled = selectedDirectory != null && fileNameValue.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Save)
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun rememberFileDialogState(): androidx.compose.ui.window.DialogState {
    return androidx.compose.ui.window.rememberFileDialogState()
}