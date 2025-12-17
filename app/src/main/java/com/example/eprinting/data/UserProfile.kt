package com.example.eprinting.data

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val gcash: String = "",
    val contactNumber: String = "",
    val shopName: String = "",
    val shopLocation: String = "",
    val role: String = "CUSTOMER"
)
