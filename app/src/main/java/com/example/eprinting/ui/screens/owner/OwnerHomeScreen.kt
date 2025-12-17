package com.example.eprinting.ui.screens.owner

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eprinting.data.OrderViewModel
import com.example.eprinting.navigation.AuthScreen
import com.example.eprinting.navigation.Screen
import com.example.eprinting.ui.viewmodels.PaperViewModel
import com.example.eprinting.viewmodels.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerHomeScreen(
    ownerId: String,
    navController: NavController,
    orderViewModel: OrderViewModel,
    paperViewModel: PaperViewModel,
    authViewModel: AuthViewModel
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf("Manage Orders") }

    // 🔹 Firebase shopName state
    var shopName by remember { mutableStateOf("Loading...") }
    val db = FirebaseFirestore.getInstance()

    // 🔹 Snapshot listener for real-time updates
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }

    LaunchedEffect(ownerId) {
        // Remove previous listener if any
        listenerRegistration?.remove()

        listenerRegistration = db.collection("users").document(ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    shopName = "Error loading shop"
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    shopName = snapshot.getString("shopName") ?: "Unknown Shop"
                } else {
                    shopName = "Unknown Shop"
                }
            }
    }

    // Remove listener when Composable leaves the composition
    DisposableEffect(Unit) {
        onDispose { listenerRegistration?.remove() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                // 🔹 Drawer Header
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        shopName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 🔹 Navigation Items
                NavigationDrawerItem(
                    label = { Text("Manage Orders") },
                    icon = { Icon(Icons.Filled.ListAlt, contentDescription = null) },
                    selected = currentScreen == "Manage Orders",
                    onClick = { scope.launch { drawerState.close() }; currentScreen = "Manage Orders" }
                )
                NavigationDrawerItem(
                    label = { Text("Manage Paper") },
                    icon = { Icon(Icons.Filled.Description, contentDescription = null) },
                    selected = currentScreen == "Manage Paper",
                    onClick = { scope.launch { drawerState.close() }; currentScreen = "Manage Paper" }
                )
                NavigationDrawerItem(
                    label = { Text("Transactions") },
                    icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) },
                    selected = currentScreen == "Transactions",
                    onClick = { scope.launch { drawerState.close() }; currentScreen = "Transactions" }
                )
                NavigationDrawerItem(
                    label = { Text("Profile") },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    selected = currentScreen == "Profile",
                    onClick = { scope.launch { drawerState.close() }; currentScreen = "Profile" }
                )

                Spacer(modifier = Modifier.weight(1f))
                Divider()

                // 🔹 Logout
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    icon = { Icon(Icons.Filled.ExitToApp, contentDescription = null) },
                    selected = false,
                    onClick = {
                        authViewModel.logout()
                        navController.navigate(AuthScreen.Login.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentScreen) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { paddingValues ->
            when (currentScreen) {
                "Manage Orders" -> ManageOrdersScreen(ownerId = ownerId, orderViewModel = orderViewModel)
                "Manage Paper" -> ManagePaperScreen(ownerId = ownerId, paperViewModel = paperViewModel)
                "Transactions" -> TransactionsScreen(
                    ownerId = ownerId,
                    drawerState = drawerState,
                    onBackClick = { currentScreen = "Manage Orders" },
                    paddingValues = paddingValues
                )
                "Profile" -> OwnerProfileScreen(
                    ownerId = ownerId,
                    drawerState = drawerState,
                    onBackClick = { currentScreen = "Manage Orders" }
                )
            }
        }
    }
}
