package com.example.eprinting.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth_route")
    object CustomerMain : Screen("customer_main_route")
    object OwnerMain : Screen("owner_main")
    object EditProfile : Screen("customer_edit_profile")
}

sealed class AuthScreen(val route: String) {
    object Login : AuthScreen("login")
    object SignUp : AuthScreen("signup")
}

sealed class CustomerScreen(val route: String) {
    object Home : CustomerScreen("home")
    object Orders : CustomerScreen("orders")
    object Profile : CustomerScreen("profile")
}

sealed class OwnerScreen(val route: String) {
    object ManageOrders : OwnerScreen("manage_orders")
    object Transactions : OwnerScreen("transactions")
    object Profile : OwnerScreen("owner_profile")
}
