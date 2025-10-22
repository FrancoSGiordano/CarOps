package com.example.trabajointegrador_modulonativo

import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings


object FirebaseClient {

    val db: FirebaseFirestore by lazy {
        val firestore = Firebase.firestore

        val settings = firestoreSettings {
            isPersistenceEnabled = true
        }
        firestore.firestoreSettings = settings
        firestore
    }
}