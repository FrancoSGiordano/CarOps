package com.example.trabajointegrador_modulonativo.model

import com.google.firebase.Timestamp

data class Reminder (
    var id: String? = "",
    val userId: String? = "",
    var carId: String? = "",
    val title: String = "",
    val notifyAt: Timestamp? = null,
    var state: String = ReminderState.EN_ESPERA.name,
    var notificationSent: Boolean = false,
    var createdAt: Timestamp? = null,
    val done: Boolean = false
){constructor() : this("", "", "", "", null, ReminderState.EN_ESPERA.name, false, null, false)}
enum class ReminderState { EN_ESPERA, PENDIENTE, REALIZADO }

