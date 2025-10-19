package com.example.trabajointegrador_modulonativo.placeholder

import java.util.ArrayList
import java.util.HashMap

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 *
 * TODO: Replace all uses of this class before publishing your app.
 */
object PlaceholderContent {

    /**
     * An array of sample (placeholder) items.
     */
    val ITEMS: MutableList<PlaceholderItem> = ArrayList()

    /**
     * A map of sample (placeholder) items, by ID.
     */
    val ITEM_MAP: MutableMap<String, PlaceholderItem> = HashMap()


    init {
        addItem(PlaceholderItem("1", "Clio", "https://www.mercadolibre.com.ar", "https://cdn.motor1.com/images/mgl/pb9jJV/s1/renault-clio-2026.jpg", "16/10/2025", "ABC123", 120000))
        addItem(PlaceholderItem("2", "Gol",  "https://www.mercadolibre.com.ar", "https://cdn.motor1.com/images/mgl/pb9jJV/s1/renault-clio-2026.jpg", "10/09/2025", "XYZ987", 87000))
        addItem(PlaceholderItem("3", "207",  "https://www.mercadolibre.com.ar", "https://cdn.motor1.com/images/mgl/pb9jJV/s1/renault-clio-2026.jpg", "02/08/2025", "LMN456", 56000))
    }

    private fun addItem(item: PlaceholderItem) {
        ITEMS.add(item)
        ITEM_MAP.put(item.id, item)
    }



    /**
     * A placeholder item representing a piece of content.
     */
    data class PlaceholderItem(val id: String, val content: String, val details: String, val imageUrl:String, val lastUpdate: String, val patente: String, val kilometraje: Int) {
        override fun toString(): String = content
    }
}