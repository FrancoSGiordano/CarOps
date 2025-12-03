package com.example.trabajointegrador_modulonativo.model

import android.R

data class ExpenseType(
    var id: String,
    val name: String,
) {
    constructor() : this("0", "")
}