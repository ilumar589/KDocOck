package com.kdockerck.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kdockerck.shared.config.AppConfig

@Composable
fun DesktopApp(
    config: AppConfig,
    onConfigChange: (AppConfig) -> Unit
) {
    var showSettingsDialog by mutableStateOf(false)
    var selectedDocumentId by mutableStateOf<String?>(null)
    
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = "KDocOck",
                navigationIcon = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {  }
                    ) {
                        Text("Exit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
            
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                FileListPanel(
                    selectedDocumentId = selectedDocumentId,
                    onDocumentSelected = { documentId ->
                        selectedDocumentId = documentId
                    },
                    onAddFiles = { showFileSelectionDialog = true }
                )
                
                androidx.compose.foundation.layout.VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp)
                )
                
                GherkinContentPanel(
                    documentId = selectedDocumentId,
                    config = config
                )
            }
            
            LogPanel(
                visible = config.ui.logPanelVisible
            )
        }
    }
    
    if (showSettingsDialog) {
        SettingsDialog(
            config = config,
            onDismiss = { showSettingsDialog = false },
            onConfigChange = { newConfig ->
                onConfigChange(newConfig)
                showSettingsDialog = false
            }
        )
    }
}