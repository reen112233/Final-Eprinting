package com.example.eprinting.ui.screens.customer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eprinting.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerEditProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val userData by authViewModel.currentUserData.collectAsState()

    var name by remember { mutableStateOf(userData.name) }
    var gcash by remember { mutableStateOf(userData.gcash) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ✅ Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Edit Profile") })
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // -------- BASIC INFO --------
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = gcash,
                onValueChange = { gcash = it },
                label = { Text("GCash Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            // -------- CHANGE PASSWORD --------
            Text(
                "Change Password (optional)",
                style = MaterialTheme.typography.labelLarge
            )

            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Current Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm New Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true

                    // ✅ Save profile info
                    authViewModel.updateCustomerData(name, gcash)

                    val passwordFieldsFilled =
                        currentPassword.isNotBlank() &&
                                newPassword.isNotBlank() &&
                                confirmPassword.isNotBlank()

                    if (passwordFieldsFilled) {
                        if (newPassword != confirmPassword) {
                            errorMessage = "Passwords do not match"
                            isLoading = false
                            return@Button
                        }

                        // 🔐 Change password
                        authViewModel.changePassword(
                            currentPassword,
                            newPassword
                        ) { success, error ->
                            isLoading = false
                            if (success) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Profile updated and password changed successfully"
                                    )
                                }
                                navController.popBackStack()
                            } else {
                                errorMessage = error
                            }
                        }
                    } else {
                        isLoading = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Profile updated successfully"
                            )
                        }
                        navController.popBackStack()
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Save Changes")
                }
            }
        }
    }
}

