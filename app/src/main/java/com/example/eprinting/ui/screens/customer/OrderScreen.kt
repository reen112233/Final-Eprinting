package com.example.eprinting.ui.screens.customer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eprinting.data.Order
import com.example.eprinting.data.OrderViewModel
import java.util.*

@Composable
fun CustomerOrderScreen(
    userId: String,
    navController: NavController,
    orderViewModel: OrderViewModel
) {
    LaunchedEffect(userId) {
        orderViewModel.listenCustomerOrders(userId)
    }

    val orders by orderViewModel.customerOrders.collectAsState()
    val sortedOrders = orders.sortedByDescending { it.date }

    if (sortedOrders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No print orders yet.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sortedOrders, key = { it.id }) { order ->
                CustomerOrderCard(order)
            }
        }
    }
}

@Composable
fun CustomerOrderCard(order: Order) {
    // Optional: Card background color based on status
    val cardColor = when (order.status.uppercase()) {
        "COMPLETED" -> Color(0xFFE8F5E9)
        "PENDING" -> Color(0xFFFFF3E0)
        "CANCELLED" -> Color(0xFFFFEBEE)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ---------------- TOP INFO ----------------
            Text(
                text = order.fileName ?: "Unnamed file",
                style = MaterialTheme.typography.titleMedium
            )
            order.shopName?.let {
                Text(
                    text = "Shop: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // ---------------- PAPER / COLOR / DATE ----------------
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                order.paper?.let { Text("Paper: $it", style = MaterialTheme.typography.bodySmall) }
                order.color?.let { Text("Color: $it", style = MaterialTheme.typography.bodySmall) }
                val formattedDate =
                    java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(order.date)
                Text("Date: $formattedDate", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---------------- STATUS & AMOUNT ----------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(order.paymentStatus, isPayment = true)
                    StatusBadge(order.status, isPayment = false)
                }

                AmountBadge(order.price)
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, isPayment: Boolean) {
    val upperStatus = status.uppercase(Locale.getDefault())
    val color = when {
        isPayment && upperStatus == "PAID" -> Color(0xFF4CAF50)
        !isPayment && upperStatus == "COMPLETED" -> Color(0xFF4CAF50)
        upperStatus == "PENDING" -> Color(0xFFFF9800)
        upperStatus == "FAILED" || upperStatus == "CANCELLED" -> Color(0xFFF44336)
        upperStatus == "PRINTING" -> Color(0xFF2196F3)
        upperStatus == "READY" -> Color(0xFF009688)
        else -> Color(0xFF9E9E9E)
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = status.replace("_", " ").replaceFirstChar { it.uppercase() },
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AmountBadge(amount: Double) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "₱${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
