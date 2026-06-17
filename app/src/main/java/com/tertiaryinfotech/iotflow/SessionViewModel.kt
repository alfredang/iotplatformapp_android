package com.tertiaryinfotech.iotflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** App-wide authentication state, mirroring the iOS `SessionStore`. */
sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: SessionUser) : AuthState
}

class SessionViewModel : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    val user: SessionUser?
        get() = (_state.value as? AuthState.SignedIn)?.user

    fun enterDemo() {
        Store.demoMode = true
        _state.value = AuthState.SignedIn(DemoData.user)
    }

    fun restore() {
        viewModelScope.launch {
            if (Store.demoMode) {
                _state.value = AuthState.SignedIn(DemoData.user)
                return@launch
            }
            val user = runCatching { ApiClient.currentUser() }.getOrNull()
            _state.value = if (user != null) AuthState.SignedIn(user) else AuthState.SignedOut
        }
    }

    suspend fun login(email: String, password: String) {
        ApiClient.login(email, password)
        val user = runCatching { ApiClient.currentUser() }.getOrNull()
        _state.value = AuthState.SignedIn(user ?: SessionUser(email = email))
    }

    suspend fun register(name: String, email: String, password: String) {
        ApiClient.register(name, email, password)
        login(email, password)
    }

    fun logout() {
        Store.demoMode = false
        ApiClient.logout()
        _state.value = AuthState.SignedOut
    }
}
