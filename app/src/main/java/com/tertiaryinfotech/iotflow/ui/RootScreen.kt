package com.tertiaryinfotech.iotflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tertiaryinfotech.iotflow.AuthState
import com.tertiaryinfotech.iotflow.SessionViewModel

@Composable
fun RootScreen(session: SessionViewModel) {
    val state by session.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (state is AuthState.Loading) session.restore()
    }

    when (state) {
        is AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is AuthState.SignedOut -> LoginScreen(session)
        is AuthState.SignedIn -> MainTabs(session)
    }
}

private data class Tab(val label: String, val icon: ImageVector)

@Composable
private fun MainTabs(session: SessionViewModel) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tabs = remember {
        listOf(
            Tab("Dashboard", Icons.Filled.GridView),
            Tab("Devices", Icons.Filled.Memory),
            Tab("Settings", Icons.Filled.Settings),
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                0 -> DashboardScreen()
                1 -> DevicesScreen()
                else -> SettingsScreen(session)
            }
        }
    }
}
