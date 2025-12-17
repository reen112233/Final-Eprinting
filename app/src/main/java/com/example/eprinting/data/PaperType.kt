package com.example.eprinting.data

data class PaperOption(
    val type: String = "",       // e.g., "Glossy"
    val size: String = "",       // e.g., "A4"
    val priceBW: Double = 0.0,
    val priceColored: Double = 0.0
) {
    val name: String
        get() = if (type.isNotEmpty() && size.isNotEmpty()) "$type $size" else ""
}

