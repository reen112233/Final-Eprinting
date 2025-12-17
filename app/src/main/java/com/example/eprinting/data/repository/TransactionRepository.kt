package com.example.eprinting.data.repository

import com.example.eprinting.data.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TransactionRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getOwnerTransactions(ownerId: String): List<Transaction> {
        val snapshot = db.collection("transactions")
            .whereEqualTo("ownerId", ownerId)
            .orderBy("createdAt")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Transaction::class.java)?.copy(id = doc.id)
        }
    }
}
