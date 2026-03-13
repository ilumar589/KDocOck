package com.kdockerck.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    showPercentage: Boolean = true
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        tonalElevation = androidx.compose.material3.tokens.Elevation.Level1
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (label != null) {
                androidx.compose.material3.Text(
                    text = label,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                track = false,
                colors = androidx.compose.material3.ProgressIndicatorDefaults.colors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    trackColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            )
            
            if (showPercentage) {
                androidx.compose.material3.Text(
                    text = "${(progress * 100).toInt()}%",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CircularProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 4.dp,
    color: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(size)
            .padding(strokeWidth / 2),
        contentAlignment = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .size(size)
        ) {
            val stroke = Stroke(
                width = strokeWidth,
                color = color,
                pathEffect = androidx.compose.ui.graphics.PathEffect.Stroke
            )
            
            val sweepAngle = 360f * progress
            val startAngle = -90f
            
            drawArc(
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                style = stroke
            )
        }
    }
}

@Composable
fun MultiProgressBar(
    tasks: List<ProgressTask>,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        tonalElevation = androidx.compose.material3.tokens.Elevation.Level1
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            androidx.compose.material3.Text(
                text = "Task Progress",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            tasks.forEach { task ->
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    androidx.compose.material3.Text(
                        text = task.name,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { task.progress },
                        modifier = Modifier.weight(2f),
                        track = false,
                        colors = androidx.compose.material3.ProgressIndicatorDefaults.colors(
                            containerColor = when (task.status) {
                                ProgressTaskStatus.COMPLETED -> androidx.compose.material3.tokens.Color.Green
                                ProgressTaskStatus.FAILED -> androidx.compose.material3.tokens.Color.Red
                                ProgressTaskStatus.IN_PROGRESS -> androidx.compose.material3.tokens.Color.Blue
                                else -> androidx.compose.material3.tokens.Color.Gray
                            },
                            trackColor = when (task.status) {
                                ProgressTaskStatus.COMPLETED -> androidx.compose.material3.tokens.Color.Green
                                ProgressTaskStatus.FAILED -> androidx.compose.material3.tokens.Color.Red
                                ProgressTaskStatus.IN_PROGRESS -> androidx.compose.material3.tokens.Color.Blue
                                else -> androidx.compose.material3.tokens.Color.Gray
                            }
                        )
                    )
                    
                    androidx.compose.material3.Text(
                        text = "${(task.progress * 100).toInt()}%",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

data class ProgressTask(
    val name: String,
    val progress: Float,
    val status: ProgressTaskStatus
)

enum class ProgressTaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}