package com.kdockerck.desktop

import androidx.compose.desktop.WindowState
import androidx.compose.desktop.darkThemeColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.kdockerck.shared.config.AppConfig
import com.kdockerck.shared.config.ConfigManager
import com.kdockerck.shared.config.StringConfigLoader
import com.kdockerck.shared.config.StringConfigSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

fun main() = androidx.compose.desktop.application {
    val scope = CoroutineScope(Dispatchers.Default)
    val configManager = remember { createConfigManager(scope) }
    val windowState = rememberWindowState()
    
    val config = remember { mutableStateOf<AppConfig?>(null) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            when (val result = configManager.loadConfig()) {
                is arrow.core.Either.Right -> {
                    config.value = result.value
                }
                is arrow.core.Either.Left -> {
                    config.value = configManager.getDefaultConfig()
                }
            }
        }
    }
    
    MaterialTheme(
        colorScheme = darkColorScheme,
        darkThemeColorScheme = darkThemeColorScheme
    ) {
        MainWindow(
            config = config.value ?: configManager.getDefaultConfig(),
            onConfigChange = { newConfig ->
                scope.launch {
                    when (val result = configManager.saveConfig(newConfig)) {
                        is arrow.core.Either.Right -> {
                            config.value = newConfig
                        }
                        is arrow.core.Either.Left -> {
                        }
                    }
                }
            },
            onClose = {
                scope.launch {
                    configManager.saveConfig(config.value ?: configManager.getDefaultConfig())
                }
            }
        )
    }
}

private fun createConfigManager(scope: CoroutineScope): ConfigManager {
    val configFile = File(System.getProperty("user.home"), ".kdockerck/config.json")
    
    val configLoader = StringConfigLoader(
        configLoader = {
            if (configFile.exists()) {
                configFile.readText()
            } else {
                "{}"
            }
        }
    )
    
    val configSaver = StringConfigSaver(
        configSaver = { configJson ->
            configFile.parentFile?.mkdirs()
            configFile.writeText(configJson)
        }
    )
    
    return ConfigManager(configLoader, configSaver)
}

@Composable
fun MainWindow(
    config: AppConfig,
    onConfigChange: (AppConfig) -> Unit,
    onClose: () -> Unit
) {
    val windowState = rememberWindowState(
        placement = WindowPlacement.Floating,
        position = WindowPosition.Absolute(100.dp, 100.dp),
        size = androidx.compose.ui.unit.IntSize(
            width = config.ui.windowWidth,
            height = config.ui.windowHeight
        )
    )
    
    androidx.compose.ui.window.Window(
        onCloseRequest = {
            onClose()
            androidx.compose.ui.window.ExitApplication
        },
        state = windowState,
        title = "KDocOck - Document to Gherkin Converter",
        icon = null,
        resizable = true,
        undecorated = false
    ) {
        DesktopApp(
            config = config,
            onConfigChange = onConfigChange
        )
    }
}