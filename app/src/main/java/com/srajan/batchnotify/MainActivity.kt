package com.srajan.batchnotify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.srajan.batchnotify.ui.App
import com.srajan.batchnotify.ui.BatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Scheduler.schedule(this, false)
        setContent { BatchTheme { App() } }
    }
}
