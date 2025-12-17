package com.example.eprinting.ui.screens.owner

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.example.eprinting.R
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
    var ownerName by remember { mutableStateOf("") }
    var ownerEmail by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher to pick profile image
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) profileImageUri = uri
    }

    // Fetch existing shop and user info
    LaunchedEffect(ownerId) {
        // Fetch USER info
        db.collection("users")
            .document(ownerId)
            .get()
            .addOnSuccessListener { doc ->
                ownerName = doc.getString("name") ?: ""
                ownerEmail = doc.getString("email") ?: ""
            }

        // Fetch SHOP info
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
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profileImageUri),
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                } else {
                    // Placeholder icon
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Placeholder",
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )

                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Owner Name
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Owner Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Owner Email
                    OutlinedTextField(
                        value = ownerEmail,
                        onValueChange = { ownerEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Shop Name
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("Shop Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Shop Location
                    OutlinedTextField(
                        value = shopLocation,
                        onValueChange = { shopLocation = it },
                        label = { Text("Shop Location") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEditing
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Edit / Save Button
                    Button(
                        onClick = {
                            if (isEditing) {
                                // Save changes
                                if (ownerName.isNotBlank()
                                    && shopName.isNotBlank() && shopLocation.isNotBlank()
                                ) {
                                    // Update USERS collection
                                    db.collection("users").document(ownerId)
                                        .update(
                                            "name", ownerName,
                                            "email", ownerEmail
                                        )

                                    // Update SHOPS collection
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

        }
    }
}
