package com.example.eprinting.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PrintingShop(
    val id: String = "",
    val name: String = "",
    val location: String = ""
)

data class PaperType(
    val id: String = "",
    val name: String = "",
    val pricePerCopy: Double = 0.0
)

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _shops = MutableStateFlow<List<PrintingShop>>(emptyList())
    val shops: StateFlow<List<PrintingShop>> = _shops

    private val _paperTypes = MutableStateFlow<List<PaperType>>(emptyList())
    val paperTypes: StateFlow<List<PaperType>> = _paperTypes

    fun loadShops() {
        viewModelScope.launch {
            db.collection("shops").get().addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    PrintingShop(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        location = doc.getString("location") ?: ""
                    )
                }
                _shops.value = list
            }
        }
    }

    fun loadPaperTypes() {
        viewModelScope.launch {
            db.collection("paper_types").get().addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    PaperType(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        pricePerCopy = doc.getDouble("pricePerCopy") ?: 0.0
                    )
                }
                _paperTypes.value = list
            }
        }
    }
}
