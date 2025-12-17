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

    val paperOptions by paperViewModel.paperOptions.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var newType by remember { mutableStateOf("") }
    var newSize by remember { mutableStateOf("") }
    var newPriceBW by remember { mutableStateOf("") }
    var newPriceColored by remember { mutableStateOf("") }

    // Edit dialog states
    var editingPaperIndex by remember { mutableStateOf<Int?>(null) }
    var editType by remember { mutableStateOf("") }
    var editSize by remember { mutableStateOf("") }
    var editPriceBW by remember { mutableStateOf("") }
    var editPriceColored by remember { mutableStateOf("") }
    var isEditDialogOpen by remember { mutableStateOf(false) }

    // Delete dialog
    var isDeleteDialogOpen by remember { mutableStateOf(false) }
    var deletingPaperIndex by remember { mutableStateOf<Int?>(null) }
    var recentlyDeletedPaper by remember { mutableStateOf<PaperOption?>(null) }

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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9FAFB))
                .padding(16.dp)
        ) {

            // ---------- Add Paper ----------
            Text(
                "Add New Paper Option",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Please fill all fields correctly"
                            )
                        }
                        return@Button
                    }

                    paperViewModel.addPaperOption(
                        ownerId,
                        PaperOption(newType, newSize, bw, colored)
                    )

                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Paper option added successfully",
                            duration = SnackbarDuration.Short
                        )
                    }

                    newType = ""
                    newSize = ""
                    newPriceBW = ""
                    newPriceColored = ""
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("Add Paper Option", color = Color.White)
            }

            Spacer(Modifier.height(24.dp))

            // ---------- Existing Papers ----------
            Text(
                "Existing Paper Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(paperOptions) { index, paper ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${paper.type} - ${paper.size}", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "₱${paper.priceBW} BW | ₱${paper.priceColored} Colored",
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
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }

                                IconButton(onClick = {
                                    deletingPaperIndex = index
                                    recentlyDeletedPaper = paper
                                    isDeleteDialogOpen = true
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------- Edit Dialog ----------
    if (isEditDialogOpen && editingPaperIndex != null) {
        AlertDialog(
            onDismissRequest = { isEditDialogOpen = false },
            title = { Text("Edit Paper Option") },
            text = {
                Column {
                    OutlinedTextField(editType, { editType = it }, label = { Text("Type") })
                    OutlinedTextField(editSize, { editSize = it }, label = { Text("Size") })
                    OutlinedTextField(editPriceBW, { editPriceBW = it }, label = { Text("Price BW") })
                    OutlinedTextField(editPriceColored, { editPriceColored = it }, label = { Text("Price Colored") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val bw = editPriceBW.toDoubleOrNull()
                    val colored = editPriceColored.toDoubleOrNull()

                    if (editType.isNotBlank() && editSize.isNotBlank() && bw != null && colored != null) {
                        paperViewModel.updatePaperOption(
                            ownerId,
                            editingPaperIndex!!,
                            PaperOption(editType, editSize, bw, colored)
                        )

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Paper option updated",
                                duration = SnackbarDuration.Short
                            )
                        }

                        isEditDialogOpen = false
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

    // ---------- Delete Confirmation + UNDO ----------
    if (isDeleteDialogOpen && deletingPaperIndex != null) {
        AlertDialog(
            onDismissRequest = { isDeleteDialogOpen = false },
            title = { Text("Delete Paper Option") },
            text = { Text("Are you sure you want to delete this paper option?") },
            confirmButton = {
                TextButton(onClick = {
                    val deletedIndex = deletingPaperIndex!!
                    val deletedPaper = recentlyDeletedPaper!!

                    paperViewModel.deletePaperOption(ownerId, deletedIndex)

                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Paper option deleted",
                            actionLabel = "UNDO",
                            duration = SnackbarDuration.Short
                        )

                        if (result == SnackbarResult.ActionPerformed) {
                            paperViewModel.addPaperOption(ownerId, deletedPaper)
                        }
                    }

                    isDeleteDialogOpen = false
                    deletingPaperIndex = null
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isDeleteDialogOpen = false
                    deletingPaperIndex = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
