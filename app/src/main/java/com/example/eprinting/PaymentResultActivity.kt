package com.example.eprinting

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PaymentResultActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: Uri? = intent?.data
        if (data == null) {
            Toast.makeText(this, "No payment data received", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Example redirect URL:
        // https://eprinting.app/payment-success?orderId=abc123
        val path = data.path ?: ""
        val orderId = data.getQueryParameter("orderId")

        if (orderId == null) {
            Toast.makeText(this, "Order ID missing", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        when (path) {
            "/payment-success" -> {
                markOrderPaid(orderId)
            }
            "/payment-failed" -> {
                Toast.makeText(this, "Payment failed ❌", Toast.LENGTH_LONG).show()
                finish()
            }
            else -> {
                Toast.makeText(this, "Unknown payment result", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun markOrderPaid(orderId: String) {
        scope.launch {
            db.collection("orders").document(orderId)
                .update("paymentStatus", "PAID")
                .addOnSuccessListener {
                    Toast.makeText(this@PaymentResultActivity, "Payment confirmed ✅", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this@PaymentResultActivity, "Failed to confirm payment: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
        }
    }
}
