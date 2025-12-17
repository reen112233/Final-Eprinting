package com.example.eprinting.ui.screens.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.eprinting.data.PaperOption
import com.example.eprinting.ui.viewmodels.PaperViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePaperScreen(ownerId: String, paperViewModel: PaperViewModel) {
    val scope = rememberCoroutineScope()
    val paperOptions by paperViewModel.paperOptions.collectAsState()

    var newType by remember { mutableStateOf("") }
    var newSize by remember { mutableStateOf("") }
    var newPriceBW by remember { mutableStateOf("") }
    var newPriceColored by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }

    // For edit dialog
    var editingPaperIndex by remember { mutableStateOf<Int?>(null) }
    var editType by remember { mutableStateOf("") }
    var editSize by remember { mutableStateOf("") }
    var editPriceBW by remember { mutableStateOf("") }
    var editPriceColored by remember { mutableStateOf("") }
    var isEditDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(ownerId) {
        paperViewModel.loadPaperOptions(ownerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Paper") },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9FAFB))
                .padding(16.dp)
        ) {
            // ---------------- Add New Paper Section ----------------
            Text(
                "Add New Paper Option",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newType,
                    onValueChange = { newType = it },
                    label = { Text("Type") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = newSize,
                    onValueChange = { newSize = it },
                    label = { Text("Size") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newPriceBW,
                    onValueChange = { newPriceBW = it },
                    label = { Text("Price BW") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = newPriceColored,
                    onValueChange = { newPriceColored = it },
                    label = { Text("Price Colored") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    val bw = newPriceBW.toDoubleOrNull()
                    val colored = newPriceColored.toDoubleOrNull()
                    if (newType.isBlank() || newSize.isBlank() || bw == null || colored == null) {
                        scope.launch { snackbar.showSnackbar("Please fill all fields correctly") }
                        return@Button
                    }
                    val paperOption = PaperOption(newType, newSize, bw, colored)
                    paperViewModel.addPaperOption(ownerId, paperOption)
                    newType = ""; newSize = ""; newPriceBW = ""; newPriceColored = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("Add Paper Option", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(24.dp))

            // ---------------- Existing Paper Options ----------------
            Text(
                "Existing Paper Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(paperOptions) { index, paper ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
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
                                Text(
                                    "${paper.type} - ${paper.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "₱${paper.priceBW} BW | ₱${paper.priceColored} Colored",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    editingPaperIndex = index
                                    editType = paper.type
                                    editSize = paper.size
                                    editPriceBW = paper.priceBW.toString()
                                    editPriceColored = paper.priceColored.toString()
                                    isEditDialogOpen = true
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color(0xFF2196F3))
                                }
                                IconButton(onClick = {
                                    paperViewModel.deletePaperOption(ownerId, index)
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------- Edit Paper Dialog ----------------
    if (isEditDialogOpen && editingPaperIndex != null) {
        AlertDialog(
            onDismissRequest = { isEditDialogOpen = false },
            title = { Text("Edit Paper Option") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editType, onValueChange = { editType = it }, label = { Text("Type") })
                    OutlinedTextField(value = editSize, onValueChange = { editSize = it }, label = { Text("Size") })
                    OutlinedTextField(value = editPriceBW, onValueChange = { editPriceBW = it }, label = { Text("Price BW") })
                    OutlinedTextField(value = editPriceColored, onValueChange = { editPriceColored = it }, label = { Text("Price Colored") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val bw = editPriceBW.toDoubleOrNull()
                    val colored = editPriceColored.toDoubleOrNull()
                    if (editType.isNotBlank() && editSize.isNotBlank() && bw != null && colored != null) {
                        val updatedPaper = PaperOption(editType, editSize, bw, colored)
                        paperViewModel.updatePaperOption(ownerId, editingPaperIndex!!, updatedPaper)
                        isEditDialogOpen = false
                    } else {
                        scope.launch { snackbar.showSnackbar("Please fill all fields correctly") }
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


