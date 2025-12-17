package com.example.eprinting.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.eprinting.data.PaperOption
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PaperViewModel : ViewModel() {

    private val _paperOptions = MutableStateFlow<List<PaperOption>>(emptyList())
    val paperOptions: StateFlow<List<PaperOption>> = _paperOptions

    private val db = FirebaseFirestore.getInstance()

    fun loadPaperOptions(ownerId: String) {
        db.collection("shops")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    val doc = snapshot.documents[0]
                    val options = (doc.get("paperOptions") as? List<Map<String, Any>>)?.map {
                        PaperOption(
                            type = it["type"] as? String ?: "",
                            size = it["size"] as? String ?: "",
                            priceBW = (it["priceBW"] as? Number)?.toDouble() ?: 0.0,
                            priceColored = (it["priceColored"] as? Number)?.toDouble() ?: 0.0
                        )
                    } ?: emptyList()
                    _paperOptions.value = options
                }
            }
    }

    fun updatePaperOption(ownerId: String, index: Int, updatedPaper: PaperOption) {
        val currentList = _paperOptions.value.toMutableList() // _paperOptions is your StateFlow or LiveData
        if (index in currentList.indices) {
            currentList[index] = updatedPaper
            _paperOptions.value = currentList
            // Also update in Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("shops")
                .document(ownerId)
                .update("paperOptions", currentList.map {
                    mapOf(
                        "type" to it.type,
                        "size" to it.size,
                        "priceBW" to it.priceBW,
                        "priceColored" to it.priceColored
                    )
                })
        }
    }

    fun addPaperOption(ownerId: String, option: PaperOption) {
        db.collection("shops")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    val docRef = snapshot.documents[0].reference
                    val updatedList = _paperOptions.value + option
                    docRef.update("paperOptions", updatedList.map {
                        mapOf(
                            "type" to it.type,
                            "size" to it.size,
                            "priceBW" to it.priceBW,
                            "priceColored" to it.priceColored
                        )
                    })
                    _paperOptions.value = updatedList
                }
            }
    }

    fun deletePaperOption(ownerId: String, index: Int) {
        db.collection("shops")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    val docRef = snapshot.documents[0].reference
                    val updatedList = _paperOptions.value.toMutableList().apply { removeAt(index) }
                    docRef.update("paperOptions", updatedList.map {
                        mapOf(
                            "type" to it.type,
                            "size" to it.size,
                            "priceBW" to it.priceBW,
                            "priceColored" to it.priceColored
                        )
                    })
                    _paperOptions.value = updatedList
                }
            }
    }
}
