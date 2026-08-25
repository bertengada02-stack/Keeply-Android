package com.keeply.app.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class ThingCreationTestHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(LOG_TAG, "checkpoint: Activity launched")
    }

    fun detachAppComposition() {
        setContent {}
        viewModelStore.clear()
    }

    private companion object {
        const val LOG_TAG = "ThingCreationE2E"
    }
}
