
package com.example.eprinting.navigation

import ProfileScreen
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.eprinting.data.OrderViewModel
import com.example.eprinting.data.UserRole
import com.example.eprinting.ui.screens.LoginScreen
import com.example.eprinting.ui.screens.SignUpScreen
import com.example.eprinting.ui.screens.customer.CustomerEditProfileScreen
import com.example.eprinting.ui.screens.customer.CustomerOrderScreen
import com.example.eprinting.ui.screens.customer.HomeScreen

import com.example.eprinting.ui.screens.customer.PayMongoPaymentScreen
import com.example.eprinting.ui.screens.owner.OwnerHomeScreen
import com.example.eprinting.ui.viewmodels.PaperViewModel
import com.example.eprinting.viewmodels.AuthViewModel


@Composable
fun AppNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    orderViewModel: OrderViewModel = viewModel()
) {
    NavHost(navController = navController, startDestination = Screen.Auth.route) {

        // ---------------- AUTH GRAPH ----------------
        navigation(startDestination = AuthScreen.Login.route, route = Screen.Auth.route) {
            composable(AuthScreen.Login.route) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = { role ->
                        val destination = when (role) {
                            UserRole.CUSTOMER -> Screen.CustomerMain.route
                            UserRole.OWNER -> Screen.OwnerMain.route
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = { navController.navigate(AuthScreen.SignUp.route) }
                )
            }

            composable(AuthScreen.SignUp.route) {
                SignUpScreen(navController = navController, authViewModel = authViewModel)
            }
        }

        // ---------------- CUSTOMER GRAPH ----------------
        navigation(
            startDestination = CustomerScreen.Home.route,
            route = Screen.CustomerMain.route
        ) {
            composable(CustomerScreen.Home.route) {
                HomeScreen(navController = navController, authViewModel = authViewModel)
            }
            composable(CustomerScreen.Orders.route) {
                val userId = authViewModel.currentUserUid ?: ""
                CustomerOrderScreen(
                    userId = userId,
                    navController = navController,
                    orderViewModel = orderViewModel
                )
            }
            composable(CustomerScreen.Profile.route) {
                ProfileScreen(navController = navController, authViewModel = authViewModel)
            }
            composable(Screen.EditProfile.route) {
                CustomerEditProfileScreen(navController = navController, authViewModel = authViewModel)
            }



            // ---------------- OWNER GRAPH ----------------
            navigation(startDestination = "OwnerHome", route = Screen.OwnerMain.route) {
                composable("OwnerHome") {
                    val ownerId = authViewModel.currentUserUid ?: ""
                    val paperViewModel: PaperViewModel = viewModel()
                    OwnerHomeScreen(
                        ownerId = ownerId,
                        navController = navController,
                        orderViewModel = orderViewModel,
                        paperViewModel = paperViewModel,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun EditProfileScreen(navController: NavHostController, authViewModel: AuthViewModel) {
    TODO("Not yet implemented")
}
