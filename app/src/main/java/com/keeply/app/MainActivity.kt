package com.keeply.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeply.app.ui.KeeplyApp
import com.keeply.app.ui.KeeplyViewModel
import com.keeply.app.ui.theme.KeeplyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeeplyTheme {
                val keeplyViewModel: KeeplyViewModel = viewModel(
                    factory = KeeplyViewModel.factory(
                        (application as KeeplyApplication).thingRepository
                    )
                )
                KeeplyApp(viewModel = keeplyViewModel)
            }
        }
    }
}
