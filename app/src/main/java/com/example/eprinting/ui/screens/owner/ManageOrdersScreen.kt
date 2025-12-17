package com.example.eprinting.ui.screens.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.eprinting.data.Order
import com.example.eprinting.data.OrderViewModel
import com.example.eprinting.ui.screens.customer.StatusBadge
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageOrdersScreen(
    ownerId: String,
    orderViewModel: OrderViewModel
) {
    LaunchedEffect(ownerId) { orderViewModel.listenOwnerOrders(ownerId) }

    val orders: List<Order> by orderViewModel.ownerOrders.collectAsState()

    // Sort orders: first by COMPLETED status last, then by date descending
    val sortedOrders = orders.sortedWith(compareBy<Order> { it.status.uppercase() == "COMPLETED" }.thenByDescending { it.date })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Orders") },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->

        if (sortedOrders.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No orders yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedOrders, key = { it.id }) { order ->
                    OwnerOrderCard(order, orderViewModel)
                }
            }
        }
    }
}



@Composable
fun OwnerOrderCard(order: Order, orderViewModel: OrderViewModel) {
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ---------- Header: File Name + Price ----------
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    order.fileName ?: "Unnamed file",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF222222)
                )
                Text(
                    "₱${String.format("%.2f", order.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }

            Spacer(Modifier.height(8.dp))

            // ---------- Customer & Paper Info ----------
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (order.customerName.isNotEmpty())
                        "Customer: ${order.customerName}" else "Customer: Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF444444),
                    fontWeight = FontWeight.Medium
                )

                order.paper?.let {
                    Text("Paper: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                order.color?.let {
                    Text("Color: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                Text(
                    text = "Copies: ${order.copies}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(Modifier.height(8.dp))

            // ---------- Payment Status & Order Status side by side ----------
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Payment Status
                StatusBadge(order.paymentStatus, isPayment = true)

                // Order Status with inline dropdown
                var expanded by remember { mutableStateOf(false) }
                Box {
                    StatusBadge(
                        status = order.status,
                        isPayment = false,
                        modifier = Modifier.clickable { expanded = true }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        val statusOptions = listOf("PENDING", "PRINTING", "READY", "COMPLETED")
                        statusOptions.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    scope.launch {
                                        orderViewModel.updateOrderStatus(order.id, status) { success ->
                                            if (success) println("Order ${order.id} updated to $status")
                                        }
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------- StatusBadge -------------------
@Composable
fun StatusBadge(status: String, isPayment: Boolean, modifier: Modifier = Modifier) {
    val upperStatus = status.uppercase(Locale.getDefault())

    // Colors same as customer home screen
    val green = Color(0xFF4CAF50)   // Paid / Completed
    val orange = Color(0xFFFF9800)  // Pending
    val red = Color(0xFFF44336)     // Failed / Cancelled
    val blue = Color(0xFF2196F3)    // Printing
    val teal = Color(0xFF009688)    // Ready
    val gray = Color(0xFF9E9E9E)    // Unknown / default

    val color = if (isPayment) {
        when (upperStatus) {
            "PAID" -> green
            "PENDING" -> orange
            "FAILED", "CANCELLED" -> red
            else -> gray
        }
    } else {
        when (upperStatus) {
            "COMPLETED" -> green
            "PRINTING" -> blue
            "READY" -> teal
            "PENDING" -> orange
            "CANCELLED" -> red
            else -> gray
        }
    }

    Text(
        text = status.replace("_", " ").replaceFirstChar { it.uppercase() },
        color = color,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
    )
}


