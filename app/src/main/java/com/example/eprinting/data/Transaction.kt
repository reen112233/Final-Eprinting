package com.example.eprinting.data

import com.google.firebase.Timestamp

enum class TransactionStatus {
    COMPLETED,
    PENDING,
    CANCELLED
}

data class Transaction(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val customerId: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val status: String = "",
    val createdAt: Timestamp? = null
)

