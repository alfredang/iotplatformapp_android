package com.tertiaryinfotech.iotflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tertiaryinfotech.iotflow.ui.IoTFlowTheme
import com.tertiaryinfotech.iotflow.ui.RootScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(applicationContext)
        ApiClient.init()
        enableEdgeToEdge()
        setContent {
            IoTFlowTheme {
                val session: SessionViewModel = viewModel()
                RootScreen(session)
            }
        }
    }
}
