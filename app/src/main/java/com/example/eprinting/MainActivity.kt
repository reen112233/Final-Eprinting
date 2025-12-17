package com.example.eprinting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.eprinting.navigation.*
import com.example.eprinting.ui.theme.EPrintingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EPrintingTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentGraphRoute = navBackStackEntry?.destination?.parent?.route

                when (currentGraphRoute) {
                    Screen.CustomerMain.route -> CustomerScaffold(navController)
                    Screen.OwnerMain.route -> OwnerScaffold(navController)
                    else -> { // Auth graph or other
                        AppNavHost(navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerScaffold(navController: NavHostController) {
    Scaffold(
        bottomBar = { CustomerBottomNavigationBar(navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavHost(navController = navController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerScaffold(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: OwnerScreen.ManageOrders.route
    val currentTitle = when(currentRoute) {
        OwnerScreen.ManageOrders.route -> "Manage Orders"
        OwnerScreen.Transactions.route -> "Transactions"
        OwnerScreen.Profile.route -> "Shop Profile"
        else -> "E-Print Hub Owner"
    }

    // We are simulating a Scaffold for the owner's dashboard
    Scaffold(
        topBar = { TopAppBar(title = { Text(currentTitle) }) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavHost(navController = navController)
        }
    }
}


@Composable
fun CustomerBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Home", Icons.Default.Home, CustomerScreen.Home.route),
        BottomNavItem("Orders", Icons.Default.List, CustomerScreen.Orders.route),
        BottomNavItem("Profile", Icons.Default.Person, CustomerScreen.Profile.route)
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(val title: String, val icon: ImageVector, val route: String)
