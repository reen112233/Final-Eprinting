package com.example.eprinting.ui.screens.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eprinting.viewmodels.OwnerTransactionViewModel
import com.example.eprinting.viewmodels.Transaction
import kotlinx.coroutines.launch

@Composable
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF222222)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Paper: ${transaction.paper}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "Color: ${transaction.color}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "Copies: ${transaction.copies}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }

                Text(
                    text = "₱${String.format("%.2f", transaction.price)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    ownerId: String,
    onBackClick: () -> Unit,
    drawerState: DrawerState,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: OwnerTransactionViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val todayTransactions by viewModel.todayTransactions.collectAsState()
    val totalToday by viewModel.totalEarningToday.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(ownerId) { viewModel.loadTransactions(ownerId) }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg -> snackbarHostState.showSnackbar(msg) }
    }

    val sortedTransactions = todayTransactions.sortedByDescending { it.date }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch { drawerState.close() }
                        onBackClick()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9FAFB))
        ) {
            // ---------- Today's Earnings Card ----------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Today's Earnings", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                        Text(
                            "₱${String.format("%.2f", totalToday)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE3F2FD), shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = Color(0xFF2196F3))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---------- Transaction List Card ----------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Today's Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )

                    when {
                        loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(36.dp), color = Color(0xFF2196F3))
                            }
                        }
                        todayTransactions.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No transactions today", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(sortedTransactions) { transaction ->
                                    TransactionItem(transaction)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

