package com.example.trabajointegrador_modulonativo.model

import com.google.firebase.Timestamp

data class Reminder (
    var id: String? = "",
    val userId: String? = "",
    var carId: String? = "",
    val title: String = "",
    val notifyAt: Timestamp? = null,
    var notificationSent: Boolean = false,
    var createdAt: Timestamp? = null,
    var pending: Boolean = false
){constructor() : this("", "", "", "", null, false, null,false)}

