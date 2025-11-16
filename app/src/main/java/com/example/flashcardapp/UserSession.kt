package com.example.flashcardapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Global session holder for currently signed-in user.
 * Avoid heavy mutable state; only store simple identifiers.
 */
object UserSession {
    @Volatile
    var userId: String? = null
        set(value) {
            field = value
        }

    @Volatile
    var username: String? = null
        set(value) {
            field = value
            _usernameFlow.value = value ?: ""
        }

    private val _usernameFlow = MutableStateFlow("")
    val usernameFlow: StateFlow<String> = _usernameFlow
}
