package com.example.trabajointegrador_modulonativo.model


import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize



@Parcelize
data class Car (
    var id: String? = "",
    val brand: String? = "",
    val model: String? = "",
    val year: Int? = 0,
    val licensePlate: String? = "",
    val engine: String? = "",
    val transmission: String? = "",
    val lastUpdate: String? = "",
    val imageUrl: String? = "",

    var userId: String = "",
    var parked: Boolean = false,
    var parkedLat: Double? = null,
    var parkedLng: Double? = null,
    var parkedDate: Timestamp? = null,
    ) : Parcelable {
    constructor() : this("", "", "", 0, "", "", "", "", "", "",false, null, null, null  )
}
