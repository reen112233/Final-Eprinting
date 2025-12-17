package com.example.eprinting.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

data class Transaction(
    val id: String,
    val date: Long,
    val price: Double,
    val customerName: String,
    val paper: String,
    val color: String,
    val copies: Int
)

class OwnerTransactionViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val allTransactions: StateFlow<List<Transaction>> = _allTransactions

    private val _todayTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val todayTransactions: StateFlow<List<Transaction>> = _todayTransactions

    private val _totalEarningToday = MutableStateFlow(0.0)
    val totalEarningToday: StateFlow<Double> = _totalEarningToday

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadTransactions(ownerId: String) {
        _loading.value = true
        db.collection("orders")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { doc ->
                    val date = doc.getLong("date") ?: return@mapNotNull null
                    val price = doc.getDouble("price") ?: 0.0
                    val customerName = doc.getString("customerName") ?: "Unknown"
                    val paper = doc.getString("paper") ?: ""
                    val color = doc.getString("color") ?: ""
                    val copies = (doc.getLong("copies") ?: 1L).toInt()

                    Transaction(
                        id = doc.id,
                        date = date,
                        price = price,
                        customerName = customerName,
                        paper = paper,
                        color = color,
                        copies = copies
                    )
                }
                _allTransactions.value = list
                filterTodayTransactions(list)
                _loading.value = false
            }
            .addOnFailureListener { e ->
                _errorMessage.value = e.message
                _loading.value = false
            }
    }

    private fun filterTodayTransactions(transactions: List<Transaction>) {
        val calendarStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val calendarEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val todayList = transactions.filter { it.date in calendarStart..calendarEnd }
        _todayTransactions.value = todayList
        _totalEarningToday.value = todayList.sumOf { it.price }
    }
}
