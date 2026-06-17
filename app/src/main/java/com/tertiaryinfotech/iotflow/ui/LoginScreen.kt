package com.tertiaryinfotech.iotflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.iotflow.SessionViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(session: SessionViewModel) {
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showServer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isValid = email.isNotEmpty() && password.length >= 8 && (!isRegister || name.isNotEmpty())

    fun submit() {
        error = null
        busy = true
        scope.launch {
            try {
                if (isRegister) session.register(name, email, password)
                else session.login(email, password)
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Something went wrong."
            }
            busy = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1226), Color(0xFF1A2147))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))
            Icon(
                Icons.Filled.SettingsInputAntenna,
                contentDescription = null,
                tint = TealLight,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text("IoTFlow", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (isRegister) "Create your account" else "Sign in to your dashboard",
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(24.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (isRegister) {
                    LoginField("Name", name, { name = it }, Icons.Filled.Person)
                }
                LoginField("Email", email, { email = it }, Icons.Filled.Email, KeyboardType.Email)
                LoginField("Password", password, { password = it }, Icons.Filled.Lock, KeyboardType.Password, secure = true)

                error?.let {
                    Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp)
                }

                androidx.compose.material3.Button(
                    onClick = { submit() },
                    enabled = isValid && !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(
                        if (isRegister) "Create account" else "Sign in",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }

                TextButton(
                    onClick = { isRegister = !isRegister; error = null },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (isRegister) "Already have an account? Sign in"
                        else "New here? Create an account",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { session.enterDemo() }) {
                Icon(Icons.Filled.PlayCircleOutline, null, tint = Color.White.copy(alpha = 0.9f))
                Spacer(Modifier.size(8.dp))
                Text("Explore demo", color = Color.White.copy(alpha = 0.9f))
            }
            TextButton(onClick = { showServer = true }) {
                Icon(Icons.Filled.Dns, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Server settings", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showServer) {
        ServerSettingsDialog(onDismiss = { showServer = false })
    }
}

@Composable
private fun LoginField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboard: KeyboardType = KeyboardType.Text,
    secure: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.5f)) },
        leadingIcon = { Icon(icon, null, tint = Color.White.copy(alpha = 0.6f)) },
        singleLine = true,
        visualTransformation = if (secure) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedIndicatorColor = TealLight,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = TealLight,
        ),
        shape = RoundedCornerShape(12.dp),
    )
}
