package com.example.trabajointegrador_modulonativo.model

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Expense (
    var id: String? = "",
    val description: String? = "",
    val amount: Double? = 0.0,
    val date: Date? = Date(),
    @get:PropertyName("expense_type_id") @set:PropertyName("expense_type_id")
    var expenseTypeId: Long? = 0,
    var carId: String? = "",
    var carName: String? = "",
    var userId: String? = ""
) : Parcelable {
    constructor() : this("", "", 0.0, Date(), 0, "", "")
}
