package com.example.trabajointegrador_modulonativo.model

import android.R

data class ExpenseType(
    var id: Long,
    val name: String,
    val imageUrl: String
) {
    constructor() : this(0, "", "")
}