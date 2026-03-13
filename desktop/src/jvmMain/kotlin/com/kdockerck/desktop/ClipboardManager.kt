package com.kdockerck.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kdockerck.shared.logging.Logger
import com.kdockerck.shared.logging.LoggerFactory
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard

class ClipboardManager(
    private val logger = LoggerFactory.getLogger("ClipboardManager")
) {
    private val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    
    fun copyToClipboard(text: String): Boolean {
        return try {
            clipboard.setContents(text)
            logger.info("Copied ${text.take(50)}... to clipboard")
            true
        } catch (e: Exception) {
            logger.error("Failed to copy to clipboard: ${e.message}")
            false
        }
    }
    
    fun copyToClipboardWithRetry(text: String, maxRetries: Int = 3): Boolean {
        var attempt = 0
        
        while (attempt < maxRetries) {
            if (copyToClipboard(text)) {
                return true
            }
            
            attempt++
            kotlinx.coroutines.delay(100)
        }
        
        return false
    }
    
    fun getClipboardContents(): String? {
        return try {
            clipboard.getContents?.trim()
        } catch (e: Exception) {
            logger.error("Failed to get clipboard contents: ${e.message}")
            null
        }
    }
}

@Composable
fun CopyToClipboardButton(
    text: String,
    onCopySuccess: () -> Unit,
    onCopyFailure: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = remember { ClipboardManager() }
    var showSuccessMessage by mutableStateOf(false)
    
    androidx.compose.material3.Button(
        onClick = {
            if (clipboardManager.copyToClipboard(text)) {
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
        Text("Copy")
    }
    
    androidx.compose.material3.Text(
        text = "Copied to clipboard!",
        modifier = Modifier
            .alpha(if (showSuccessMessage) 1f else 0f)
            .padding(top = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodySmall
    )
    
    kotlinx.coroutines.LaunchedEffect(showSuccessMessage) {
        kotlinx.coroutines.delay(2000)
        showSuccessMessage = false
    }
}