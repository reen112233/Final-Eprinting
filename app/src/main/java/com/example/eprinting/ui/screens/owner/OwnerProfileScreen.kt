package com.example.eprinting.ui.screens.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerProfileScreen(ownerId: String, onBackClick: () -> Unit, drawerState: DrawerState) {
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    // State variables
    var shopName by remember { mutableStateOf("") }
    var shopLocation by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Fetch existing shop info
    LaunchedEffect(ownerId) {
        db.collection("shops")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    val doc = snapshot.documents[0]
                    shopName = doc.getString("shopName") ?: ""
                    shopLocation = doc.getString("shopLocation") ?: ""
                }
            }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Owner Profile") },
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
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF9FAFB))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("Shop Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = shopLocation,
                        onValueChange = { shopLocation = it },
                        label = { Text("Shop Location") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isEditing) {
                                // Save changes
                                if (shopName.isNotBlank() && shopLocation.isNotBlank()) {
                                    db.collection("shops")
                                        .whereEqualTo("ownerId", ownerId)
                                        .get()
                                        .addOnSuccessListener { snapshot ->
                                            if (snapshot.documents.isNotEmpty()) {
                                                val docId = snapshot.documents[0].id
                                                db.collection("shops").document(docId)
                                                    .update(
                                                        "shopName", shopName,
                                                        "shopLocation", shopLocation
                                                    )
                                                    .addOnSuccessListener {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Profile Updated!")
                                                            isEditing = false
                                                        }
                                                    }
                                            }
                                        }
                                }
                            } else {
                                // Enable edit mode
                                isEditing = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text(if (isEditing) "Save Changes" else "Edit Profile", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SnackbarHost(snackbarHostState)
        }
    }
}
