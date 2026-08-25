package com.example.utils

object Validators {
    
    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
    private val DISPLAY_NAME_REGEX = "^[A-Za-z0-9 ]{3,20}\$".toRegex()
    private val FRIEND_CODE_REGEX = "^[A-Z0-9]{6}\$".toRegex()

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && EMAIL_REGEX.matches(email)
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isValidDisplayName(name: String): Boolean {
        return DISPLAY_NAME_REGEX.matches(name)
    }

    fun isValidFriendCode(code: String): Boolean {
        return FRIEND_CODE_REGEX.matches(code.uppercase())
    }
}
