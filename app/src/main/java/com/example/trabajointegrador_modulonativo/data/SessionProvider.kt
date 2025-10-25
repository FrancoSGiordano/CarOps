package com.example.trabajointegrador_modulonativo.data

import com.google.firebase.auth.FirebaseAuth

class SessionProvider {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}