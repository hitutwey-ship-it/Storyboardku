package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainScreen
import com.example.ui.theme.StoryboardKuTheme
import com.example.ui.viewmodel.StoryboardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: StoryboardViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            StoryboardKuTheme(
                themeMode = settings.themeMode,
                textScale = settings.textScale
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
