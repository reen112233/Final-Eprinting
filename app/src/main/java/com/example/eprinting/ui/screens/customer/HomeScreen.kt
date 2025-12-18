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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
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

        val body = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart(
                "file",
                tempFile.name,
                tempFile.asRequestBody("application/octet-stream".toMediaType())
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

        if (!response.isSuccessful) return@withContext null

        JSONObject(response.body!!.string()).getString("secure_url")
    } catch (e: Exception) {
        Log.e("Cloudinary", "Upload failed", e)
        null
    }
}

// -----------------------------
// DROPDOWN MENU
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuBox(label: String, items: List<String>, onItemSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(label) }

    ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
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
    FirebaseFirestore.getInstance().collection("shops").get()
        .addOnSuccessListener { snap ->
            val shops = snap.documents.mapNotNull { doc ->
                val shopName = doc.getString("shopName")
                val ownerId = doc.getString("ownerId")
                if (shopName != null && ownerId != null) {
                    val paperOptions =
                        (doc.get("paperOption") as? List<Map<String, Any>>)?.map {
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
            onResult(emptyList())
        }
}

// -----------------------------
// SEND ORDER
fun sendOrder(
    selectedShop: Shop?,
    selectedPaper: PaperOption?,
    selectedColor: String,
    quantity: Int,
    uploadedFileUrl: String?,
    totalPrice: Double,
    snackbar: SnackbarHostState,
    scope: CoroutineScope
) {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    val shop = selectedShop ?: return
    val paper = selectedPaper ?: return
    val fileUrl = uploadedFileUrl ?: return

    val order = hashMapOf(
        "userId" to user.uid,
        "shopId" to shop.id,
        "ownerId" to shop.ownerId,
        "customerName" to (user.displayName ?: "Unknown"),
        "fileUrl" to fileUrl,
        "copies" to quantity,
        "price" to totalPrice,
        "status" to "PENDING",
        "paymentStatus" to "PAID",
        "paper" to paper.name,
        "color" to selectedColor,
        "date" to System.currentTimeMillis(),
        "shopName" to shop.shopName
    )

    FirebaseFirestore.getInstance().collection("orders").add(order)
        .addOnSuccessListener {
            scope.launch { snackbar.showSnackbar("Print request sent ✅") }
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
    var paymentStatus by remember { mutableStateOf("UNPAID") }

    LaunchedEffect(Unit) { fetchPrinterShops { shops = it } }

    LaunchedEffect(selectedPaper, selectedColor, quantity) {
        selectedPaper?.let {
            totalPrice =
                if (selectedColor == "Colored") it.priceColored * quantity
                else it.priceBW * quantity
        }
    }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                scope.launch {
                    isUploading = true
                    uploadedFileUrl =
                        uploadFileToCloudinary(uri, "dhmz4js82", "mobile_unsigned", context.contentResolver)
                    isUploading = false
                }
            }
        }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { CenterAlignedTopAppBar(title = { Text("Create Print Request") }) }
    ) { padding ->

        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {

            Button(onClick = { picker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (uploadedFileUrl == null) "Upload File" else "Change File")
            }

            Spacer(Modifier.height(12.dp))

            DropdownMenuBox("Select Shop", shops.map { it.shopName }) {
                selectedShop = shops[it]
                selectedPaper = shops[it].paperOptions.firstOrNull()
            }

            Spacer(Modifier.height(12.dp))

            DropdownMenuBox("Select Color", listOf("Black & White", "Colored")) {
                selectedColor = if (it == 0) "Black & White" else "Colored"
            }

            Spacer(Modifier.height(12.dp))

            selectedShop?.let {
                DropdownMenuBox("Select Paper", it.paperOptions.map { p -> p.name }) { i ->
                    selectedPaper = it.paperOptions[i]
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (quantity > 1) quantity-- }) {
                    Icon(Icons.Default.Remove, null)
                }
                Text(quantity.toString())
                IconButton(onClick = { quantity++ }) {
                    Icon(Icons.Default.Add, null)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Total: ₱${String.format("%.2f", totalPrice)}")

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    paymentStatus = "PAID"
                    scope.launch { snackbar.showSnackbar("Payment successful ✅") }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uploadedFileUrl != null
            ) {
                Text("Pay")
            }

            if (paymentStatus == "PAID") {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        sendOrder(
                            selectedShop,
                            selectedPaper,
                            selectedColor,
                            quantity,
                            uploadedFileUrl,
                            totalPrice,
                            snackbar,
                            scope
                        )
                        paymentStatus = "UNPAID"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Print Request")
                }
            }
        }
    }
}
