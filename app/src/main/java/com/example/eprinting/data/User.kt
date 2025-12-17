package com.example.eprinting.data

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val gcash: String = "",
    val role: String = "CUSTOMER",
    val contactNumber: String = "",
    val shopName: String = "",
    val shopLocation: String = ""
)
