package com.example.trabajointegrador_modulonativo.model

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
    val ownerId: String? = ""
) {
    constructor() : this("", "", "", 0, "", "", "", "", "", "")
}
