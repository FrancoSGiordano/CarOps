package com.example.trabajointegrador_modulonativo.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Insurance (
    @DocumentId
    var id: String? = null,
    var insuranceName: String? = "",
    var policyNumber: String? = "",
    var expirationDate: Date? = null,
    var coverage: String? = "",
    var engineNumber: String? = "",
    var chassisNumber: String? = "",
    var policyHolderName: String? = "",
    var policyFileUrl: String? = null,
    var carId : String? = null,
    var userId : String? = null,
    val lastUpdate: String? = "",

) : Parcelable {
    constructor(): this(null, "", "", null, "", "", null, "", "", null, null, "")

}