package com.example.eprinting.data

sealed class UserRole {
    object CUSTOMER : UserRole()
    object OWNER : UserRole()

    companion object {
        fun fromString(value: String): UserRole = when(value.uppercase()) {
            "CUSTOMER" -> CUSTOMER
            "OWNER" -> OWNER
            else -> CUSTOMER // fallback to CUSTOMER
        }
    }
}
