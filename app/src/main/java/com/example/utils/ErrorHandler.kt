package com.example.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

object ErrorHandler {
    fun getMessage(error: Throwable): String {
        return when (error) {
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
            is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists."
            is FirebaseNetworkException -> "Network error. Please check your internet connection."
            is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
            else -> error.localizedMessage ?: "Something went wrong. Please try again."
        }
    }
}
