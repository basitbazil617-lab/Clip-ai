package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.example.ui.GtaViClipAiApp
import com.example.ui.theme.GtaViClipAiTheme
import com.example.viewmodel.StudioContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val container = remember(applicationContext) { StudioContainer(applicationContext) }
            GtaViClipAiTheme {
                GtaViClipAiApp(container = container)
            }
        }
    }
}
