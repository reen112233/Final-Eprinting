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

    private fun paperMap(list: List<PaperOption>) =
        list.map {
            mapOf(
                "type" to it.type,
                "size" to it.size,
                "priceBW" to it.priceBW,
                "priceColored" to it.priceColored
            )
        }

    fun loadPaperOptions(ownerId: String) {
        db.collection("shops")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    val doc = snapshot.documents[0]
                    val options = (doc.get("paperOption") as? List<Map<String, Any>>)?.map {
                        PaperOption(
                            it["type"] as? String ?: "",
                            it["size"] as? String ?: "",
                            (it["priceBW"] as? Number)?.toDouble() ?: 0.0,
                            (it["priceColored"] as? Number)?.toDouble() ?: 0.0
                        )
                    } ?: emptyList()
                    _paperOptions.value = options
                }
            }
    }

    fun addPaperOption(ownerId: String, option: PaperOption) {
        db.collection("shops")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    val docRef = snapshot.documents[0].reference
                    val updated = _paperOptions.value + option
                    docRef.update("paperOption", paperMap(updated))
                    _paperOptions.value = updated
                }
            }
    }

    fun updatePaperOption(ownerId: String, index: Int, updatedPaper: PaperOption) {
        val list = _paperOptions.value.toMutableList()
        if (index !in list.indices) return
        list[index] = updatedPaper
        _paperOptions.value = list

        db.collection("shops")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener {
                if (it.documents.isNotEmpty()) {
                    it.documents[0].reference
                        .update("paperOption", paperMap(list))
                }
            }
    }

    fun deletePaperOption(ownerId: String, index: Int) {
        val list = _paperOptions.value.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        _paperOptions.value = list

        db.collection("shops")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener {
                if (it.documents.isNotEmpty()) {
                    it.documents[0].reference
                        .update("paperOption", paperMap(list))
                }
            }
    }
}

