package com.example.weanimals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.weanimals.screens.MainScreen
import com.example.weanimals.theme.WeAnimalsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeAnimalsTheme {
                MainScreen()
            }
        }
    }
}
