package com.keeply.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.keeply.app.ui.KeeplyApp
import com.keeply.app.ui.theme.KeeplyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeeplyTheme {
                KeeplyApp()
            }
        }
    }
}
