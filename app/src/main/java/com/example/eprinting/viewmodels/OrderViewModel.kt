package com.example.eprinting.data

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OrderViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _customerOrders = MutableStateFlow<List<Order>>(emptyList())
    val customerOrders: StateFlow<List<Order>> = _customerOrders

    private val _ownerOrders = MutableStateFlow<List<Order>>(emptyList())
    val ownerOrders: StateFlow<List<Order>> = _ownerOrders

    private var customerListener: ListenerRegistration? = null
    private var ownerListener: ListenerRegistration? = null

    // ---------------- UPDATE ORDER STATUS ----------------
    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val updates: Map<String, Any> = mapOf("status" to newStatus)
        firestore.collection("orders")
            .document(orderId)
            .update(updates)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e("OrderViewModel", "Failed to update order status", e)
                onComplete(false)
            }
    }


    // ---------------- LOAD ORDERS (ONE TIME) ----------------
    fun loadCustomerOrders(userId: String) {
        firestore.collection("orders")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                _customerOrders.value = snapshot.documents.mapNotNull { it.toOrder() }
            }
            .addOnFailureListener {
                Log.e("OrderViewModel", "Failed to load customer orders", it)
            }
    }

    fun loadOwnerOrders(ownerId: String) {
        firestore.collection("orders")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                _ownerOrders.value = snapshot.documents.mapNotNull { it.toOrder() }
            }
            .addOnFailureListener {
                Log.e("OrderViewModel", "Failed to load owner orders", it)
            }
    }

    // ---------------- REAL-TIME LISTENERS ----------------
    fun listenCustomerOrders(userId: String) {
        customerListener?.remove()
        customerListener = firestore.collection("orders")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("OrderViewModel", "Customer orders listen failed", e)
                    return@addSnapshotListener
                }
                _customerOrders.value =
                    snapshot?.documents?.mapNotNull { it.toOrder() } ?: emptyList()
            }
    }

    fun listenOwnerOrders(ownerId: String) {
        ownerListener?.remove()
        ownerListener = firestore.collection("orders")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("OrderViewModel", "Owner orders listen failed", e)
                    return@addSnapshotListener
                }
                _ownerOrders.value =
                    snapshot?.documents?.mapNotNull { it.toOrder() } ?: emptyList()
            }
    }

    override fun onCleared() {
        customerListener?.remove()
        ownerListener?.remove()
        super.onCleared()
    }
}


// ---------------- Extension to map Firestore doc to Order ----------------
fun com.google.firebase.firestore.DocumentSnapshot.toOrder(): Order? {
    return try {
        Order(
            id = id,
            userId = getString("userId") ?: "",
            ownerId = getString("ownerId") ?: "",
            customerName = getString("customerName") ?: "", // ✅ IMPORTANT
            shopName = getString("shopName") ?: "",
            fileUrl = getString("fileUrl"),
            fileName = getString("fileName")
                ?: getString("fileUrl")?.substringAfterLast("/"),
            copies = (getLong("copies") ?: 1L).toInt(),
            price = getDouble("price") ?: 0.0,
            status = getString("status") ?: "PENDING",
            paymentStatus = getString("paymentStatus") ?: "PENDING",
            date = getLong("date") ?: System.currentTimeMillis(),
            paper = getString("paper"),
            color = getString("color")
        )
    } catch (e: Exception) {
        Log.e("OrderViewModel", "Error parsing order", e)
        null
    }
}

