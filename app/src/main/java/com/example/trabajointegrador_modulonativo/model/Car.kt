package com.example.trabajointegrador_modulonativo.model


import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize



@Parcelize
data class Car (
    var id: String? = "",
    val brand: String? = "",
    val model: String? = "",
    val anio: Int? = 0,
    val licensePlate: String? = "",
    val engine: String? = "",
    val transmission: String? = "",
    val lastUpdate: Timestamp? = null,
    val imageUrl: String? = "",
    var userId: String = "",
    val insuranceId: String? = "",
    @Transient
    var parked: Boolean = false,
    var parkedLat: Double? = null,
    var parkedLng: Double? = null,
    var parkedDate: Timestamp? = null,
) : Parcelable {
   constructor() : this("", "", "", 0, "", "", "", null, "", "","", false, null, null, null )

    override fun toString(): String {
        return "${this.brand} ${this.model}"
    }
    
}
