package com.example.trabajointegrador_modulonativo.model

import android.os.Parcelable
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
) : Parcelable {
    constructor() : this("", "", "", 0, "", "", "", "", "", "")
}
