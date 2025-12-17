package com.example.eprinting.ui.screens.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.eprinting.data.Transaction
import java.util.Locale

@Composable
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ---------- Header: Customer Name + Amount ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.name.ifBlank { "Unknown Customer" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF222222)
                )

                Text(
                    text = "₱${String.format("%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(Modifier.height(8.dp))

            // ---------- Description ----------
            if (transaction.description.isNotBlank()) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))
            }

            // ---------- Status Badge ----------
            TransactionStatusBadge(
                status = transaction.status,
                modifier = Modifier
            )

            Spacer(Modifier.height(8.dp))
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
        }
    }
}

// New Composable for Status Badge
@Composable
fun TransactionStatusBadge(status: String, modifier: Modifier = Modifier) {
    val upperStatus = status.uppercase(Locale.getDefault())

    val green = Color(0xFF4CAF50)   // Completed / Paid
    val orange = Color(0xFFFF9800)  // Pending
    val red = Color(0xFFF44336)     // Cancelled
    val gray = Color(0xFF9E9E9E)    // Unknown / default

    val color = when (upperStatus) {
        "COMPLETED" -> green
        "PENDING" -> orange
        "CANCELLED" -> red
        else -> gray
    }

    Text(
        text = upperStatus.replace("_", " ").replaceFirstChar { it.uppercase() },
        color = color,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
