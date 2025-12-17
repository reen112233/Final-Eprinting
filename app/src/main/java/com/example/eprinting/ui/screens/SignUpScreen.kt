package com.example.eprinting.ui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eprinting.R
import com.example.eprinting.navigation.Screen
import com.example.eprinting.viewmodels.AuthState
import com.example.eprinting.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(navController: NavController, authViewModel: AuthViewModel) {
    val signUpData by authViewModel.signUpData.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Authentication state handler
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar("✅ Sign up successful!")
                }
                authViewModel.resetState()
            }
            is AuthState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar("⚠️ ${(authState as AuthState.Error).message}")
                }
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo and App title
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "E-Printing Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "E-Printing",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Create your account below", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(32.dp))

            // Form card for better visual grouping
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // User type selection
                    Row {
                        UserTypeRadioButton(
                            label = "Customer",
                            selected = signUpData.userType == "CUSTOMER",
                            onSelect = { authViewModel.updateSignUpData(signUpData.copy(userType = "CUSTOMER")) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        UserTypeRadioButton(
                            label = "Shop Owner",
                            selected = signUpData.userType == "OWNER",
                            onSelect = { authViewModel.updateSignUpData(signUpData.copy(userType = "OWNER")) }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Form fields
                    OutlinedTextField(
                        value = signUpData.name,
                        onValueChange = { authViewModel.updateSignUpData(signUpData.copy(name = it)) },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = signUpData.email,
                        onValueChange = { authViewModel.updateSignUpData(signUpData.copy(email = it)) },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (signUpData.userType == "OWNER") {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = signUpData.shopName,
                            onValueChange = { authViewModel.updateSignUpData(signUpData.copy(shopName = it)) },
                            label = { Text("Shop Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = signUpData.password,
                        onValueChange = { authViewModel.updateSignUpData(signUpData.copy(password = it)) },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = signUpData.confirmPassword,
                        onValueChange = { authViewModel.updateSignUpData(signUpData.copy(confirmPassword = it)) },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (signUpData.password != signUpData.confirmPassword) {
                                scope.launch { snackbarHostState.showSnackbar("Passwords don't match") }
                                return@Button
                            }
                            if (signUpData.email.isEmpty() || signUpData.password.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar("Please fill all fields") }
                                return@Button
                            }
                            if (signUpData.userType == "OWNER" && signUpData.shopName.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar("Shop name is required for owners") }
                                return@Button
                            }

                            authViewModel.signUp(
                                email = signUpData.email,
                                password = signUpData.password,
                                userType = signUpData.userType,
                                shopName = signUpData.shopName // ✅ Pass shop name here
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(if (authState is AuthState.Loading) "Creating Account..." else "Sign Up")
                    }


                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Already have an account? Login")
                    }
                }
            }
        }
    }
}

@Composable
private fun UserTypeRadioButton(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label)
    }
}


