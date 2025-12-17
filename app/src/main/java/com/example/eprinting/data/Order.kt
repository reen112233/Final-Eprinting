package com.example.eprinting.data


data class Order(
    val id: String = "",
    val userId: String = "",
    val shopId: String = "",
    val ownerId: String = "",
    val fileUrl: String? = null,
    val fileName: String? = null,
    val copies: Int = 1,
    val price: Double = 0.0,
    val status: String = "PENDING",       // order status
    val paymentStatus: String = "PENDING", // payment status
    val date: Long = System.currentTimeMillis(),
    val paper: String? = null,
    val color: String? = null,
    val shopName: String? = null,
    val customerName: String = "", // ✅ STORED DIRECTLY
)


