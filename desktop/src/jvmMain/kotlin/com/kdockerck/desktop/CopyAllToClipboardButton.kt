package com.kdockerck.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kdockerck.desktop.ClipboardManager

@Composable
fun CopyAllToClipboardButton(
    texts: List<String>,
    onCopySuccess: () -> Unit,
    onCopyFailure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = remember { ClipboardManager() }
    var showSuccessMessage by mutableStateOf(false)
    
    androidx.compose.material3.Button(
        onClick = {
            val allText = texts.joinToString("\n\n")
            
            if (clipboardManager.copyToClipboard(allText)) {
                onCopySuccess()
                showSuccessMessage = true
            } else {
                onCopyFailure()
            }
        },
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(Icons.Filled.ContentCopy)
        Spacer(Modifier.width(8.dp))
        Text("Copy All")
        
        androidx.compose.material3.Text(
            text = "Copied all to clipboard!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .alpha(if (showSuccessMessage) 1f else 0f)
                .padding(top = 4.dp)
        )
        
        kotlinx.coroutines.LaunchedEffect(showSuccessMessage) {
            kotlinx.coroutines.delay(2000)
            showSuccessMessage = false
        }
    }
}