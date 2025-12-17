    package com.example.eprinting.ui.screens.customer

    import android.net.Uri
    import android.util.Log
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Add
    import androidx.compose.material.icons.filled.Remove
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.unit.dp
    import androidx.navigation.NavController
    import com.example.eprinting.viewmodels.AuthViewModel
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import okhttp3.*
    import okhttp3.MediaType.Companion.toMediaType
    import okhttp3.RequestBody.Companion.asRequestBody
    import org.json.JSONObject
    import java.io.File
    import java.util.concurrent.TimeUnit

    // -----------------------------
    // DATA CLASSES
    data class PaperOption(val name: String, val priceBW: Double, val priceColored: Double)
    data class Shop(
        val id: String,
        val shopName: String,
        val shopLocation: String,
        val ownerId: String,
        val paperOptions: List<PaperOption>
    )

    // -----------------------------
    // CLOUDINARY UPLOAD
    suspend fun uploadFileToCloudinary(
        uri: Uri,
        cloudName: String,
        uploadPreset: String,
        resolver: android.content.ContentResolver
    ): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = resolver.openInputStream(uri) ?: return@withContext null
            val tempFile = File.createTempFile("upload", ".tmp")
            tempFile.outputStream().use { inputStream.copyTo(it) }

            val mime = "application/octet-stream"

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    tempFile.name,
                    tempFile.asRequestBody(mime.toMediaType())
                )
                .addFormDataPart("upload_preset", uploadPreset)
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/auto/upload")
                .post(body)
                .build()

            val response = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
                .newCall(request)
                .execute()

            if (!response.isSuccessful) throw Exception("Upload failed: ${response.message}")
            JSONObject(response.body!!.string()).getString("secure_url")
        } catch (e: Exception) {
            Log.e("Cloudinary", "Upload failed", e)
            null
        }
    }

    // -----------------------------
    // DROPDOWN MENU COMPONENT
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DropdownMenuBox(label: String, items: List<String>, onItemSelected: (Int) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        var selectedText by remember { mutableStateOf(label) }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selectedText,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            selectedText = item
                            expanded = false
                            onItemSelected(index)
                        }
                    )
                }
            }
        }
    }

    // -----------------------------
    // FETCH SHOPS
    fun fetchPrinterShops(onResult: (List<Shop>) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("shops")
            .get()
            .addOnSuccessListener { snap ->
                val shops = snap.documents.mapNotNull { doc ->
                    val shopName = doc.getString("shopName")
                    val ownerId = doc.getString("ownerId")
                    if (shopName != null && ownerId != null) {
                        val paperOptions = (doc.get("paperOption") as? List<Map<String, Any>>)?.map {
                            PaperOption(
                                name = "${it["type"]} ${it["size"]}",
                                priceBW = (it["priceBW"] as Number).toDouble(),
                                priceColored = (it["priceColored"] as Number).toDouble()
                            )
                        } ?: emptyList()
                        Shop(
                            id = doc.id,
                            shopName = shopName,
                            shopLocation = doc.getString("shopLocation") ?: "",
                            ownerId = ownerId,
                            paperOptions = paperOptions
                        )
                    } else null
                }
                onResult(shops)
            }
            .addOnFailureListener {
                Log.e("HomeScreen", "Failed to fetch shops", it)
                onResult(emptyList())
            }
    }

    // -----------------------------
    // SEND ORDER (Updated with paymentStatus)
    fun sendOrder(
        selectedShop: Shop?,
        selectedPaper: PaperOption?,
        selectedColor: String,
        quantity: Int,
        uploadedFileUrl: String?,
        totalPrice: Double,
        paymentDone: Boolean,
        snackbar: SnackbarHostState,
        scope: CoroutineScope
    ) {
        val db = FirebaseFirestore.getInstance()

        // Validate inputs
        val shop = selectedShop ?: run {
            scope.launch { snackbar.showSnackbar("Please select a shop") }
            return
        }
        val paper = selectedPaper ?: run {
            scope.launch { snackbar.showSnackbar("Please select paper") }
            return
        }
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            scope.launch { snackbar.showSnackbar("User not logged in") }
            return
        }
        val uploadedFile = uploadedFileUrl ?: run {
            scope.launch { snackbar.showSnackbar("Please upload a file") }
            return
        }

        // Get customer name from FirebaseAuth displayName or fallback
        val customerName = currentUser.displayName ?: "Unknown"

        val orderData = hashMapOf(
            "userId" to currentUser.uid,
            "shopId" to shop.id,
            "ownerId" to shop.ownerId,
            "customerName" to customerName,           // ✅ Added field
            "fileUrl" to uploadedFile,
            "copies" to quantity,
            "price" to totalPrice,
            "status" to "PENDING",
            "paymentStatus" to if (paymentDone) "PAID" else "UNPAID",
            "date" to System.currentTimeMillis(),
            "paper" to paper.name,
            "color" to selectedColor,
            "shopName" to shop.shopName
        )

        db.collection("orders").add(orderData)
            .addOnSuccessListener {
                scope.launch { snackbar.showSnackbar("Print request sent successfully ✅") }
            }
            .addOnFailureListener { e ->
                scope.launch { snackbar.showSnackbar("Failed to send request: ${e.message}") }
            }
    }

    // -----------------------------
    // HOME SCREEN
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HomeScreen(navController: NavController, authViewModel: AuthViewModel) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }

        var uploadedFileUrl by remember { mutableStateOf<String?>(null) }
        var isUploading by remember { mutableStateOf(false) }

        var shops by remember { mutableStateOf<List<Shop>>(emptyList()) }
        var selectedShop by remember { mutableStateOf<Shop?>(null) }
        var selectedPaper by remember { mutableStateOf<PaperOption?>(null) }
        var selectedColor by remember { mutableStateOf("Black & White") }
        var quantity by remember { mutableStateOf(1) }
        var totalPrice by remember { mutableStateOf(0.0) }

        var paymentDone by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { fetchPrinterShops { shops = it } }

        LaunchedEffect(selectedPaper, selectedColor, quantity) {
            selectedPaper?.let {
                totalPrice = if (selectedColor == "Colored") it.priceColored * quantity else it.priceBW * quantity
            }
        }

        val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                scope.launch {
                    isUploading = true
                    try {
                        val url = uploadFileToCloudinary(uri, "dhmz4js82", "mobile_unsigned", context.contentResolver)
                        if (url == null) throw Exception("Upload failed")
                        uploadedFileUrl = url
                        scope.launch { snackbar.showSnackbar("File uploaded successfully ✅") }
                    } catch (e: Exception) {
                        scope.launch { snackbar.showSnackbar("Upload failed: ${e.message}") }
                        Log.e("Cloudinary", "Upload error", e)
                    } finally {
                        isUploading = false
                    }
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = { CenterAlignedTopAppBar(title = { Text("Create Print Request") }) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ---------------- FILE UPLOAD ----------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally // Center everything inside
                ) {
                    Text("Document Upload", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { picker.launch("*/*") },
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth(0.6f) // optional: make button narrower and centered
                    ) {
                        if (isUploading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else Text(if (uploadedFileUrl == null) "📤 Upload File" else "🔄 Change & Upload File")
                    }
                    uploadedFileUrl?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Uploaded URL: $it",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.widthIn(max = 300.dp) // optional: limit width for long URLs
                        )
                    }
                }


                Spacer(Modifier.height(16.dp))

                // ---------------- SHOP SELECTION ----------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Shop Selection", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    DropdownMenuBox(
                        label = if (shops.isEmpty()) "Loading shops..." else "Select Shop",
                        items = shops.map { "${it.shopName} (${it.shopLocation})" },
                        onItemSelected = { index ->
                            selectedShop = shops[index]
                            selectedPaper = selectedShop!!.paperOptions.firstOrNull()
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ---------------- PRINT CONFIG ----------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Print Configuration", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    DropdownMenuBox(label = "Select Color", items = listOf("Black & White", "Colored")) { index ->
                        selectedColor = if (index == 0) "Black & White" else "Colored"
                    }
                    Spacer(Modifier.height(12.dp))
                    selectedShop?.let { shop ->
                        DropdownMenuBox(label = "Select Paper", items = shop.paperOptions.map { it.name }) { index ->
                            selectedPaper = shop.paperOptions[index]
                        }
                    } ?: OutlinedTextField(
                        value = "Select a shop first",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Paper") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { if (quantity > 1) quantity-- }, enabled = quantity > 1) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(quantity.toString(), style = MaterialTheme.typography.headlineMedium)
                        IconButton(onClick = { quantity++ }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ---------------- TOTAL PRICE ----------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Amount", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "₱${String.format("%.2f", totalPrice)}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ---------------- PAYMENT BUTTON ----------------
                if (uploadedFileUrl != null && !paymentDone) {
                    Button(
                        onClick = { paymentDone = true; scope.launch { snackbar.showSnackbar("Payment successful ✅") } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedShop != null && selectedPaper != null && totalPrice > 0
                    ) {
                        Text("Pay ₱${String.format("%.2f", totalPrice)}")
                    }
                }

                // ---------------- SEND REQUEST BUTTON ----------------
                if (paymentDone && uploadedFileUrl != null) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            sendOrder(selectedShop, selectedPaper, selectedColor, quantity, uploadedFileUrl, totalPrice, paymentDone, snackbar, scope)
                            // Reset selections
                            uploadedFileUrl = null
                            paymentDone = false
                            quantity = 1
                            selectedPaper = null
                            selectedShop = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedShop != null && selectedPaper != null
                    ) {
                        Text("Send Print Request to ${selectedShop?.shopName ?: "Shop"}")
                    }
                }
            }
        }
    }
