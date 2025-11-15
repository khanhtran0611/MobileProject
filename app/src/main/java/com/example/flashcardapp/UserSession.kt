package com.example.flashcardapp

/**
 * Global session holder for currently signed-in user.
 * Avoid heavy mutable state; only store simple identifiers.
 */
object UserSession {
    @Volatile
    var userId: String? = null

    @Volatile
    var username: String? = null
}

