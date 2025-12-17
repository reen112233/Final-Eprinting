package com.example.eprinting.ui.screens.owner

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun OwnerAppDrawer(
    currentScreen: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                // Header
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.Start) {
                    Icon(Icons.Filled.Menu, contentDescription = "Logo", modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("E-Print Owner", style = MaterialTheme.typography.titleLarge)
                    Text("Manage your shop", style = MaterialTheme.typography.labelMedium)
                }
                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                // Menu items
                NavigationDrawerItem(
                    label = { Text("Manage Orders") },
                    icon = { Icon(Icons.Filled.ListAlt, contentDescription = null) },
                    selected = currentScreen == "Manage Orders",
                    onClick = { scope.launch { drawerState.close() }; onNavigate("Manage Orders") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Manage Papers") },
                    icon = { Icon(Icons.Filled.ListAlt, contentDescription = null) },
                    selected = currentScreen == "Manage Papers",
                    onClick = { scope.launch { drawerState.close() }; onNavigate("Manage Papers") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Profile") },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    selected = currentScreen == "Profile",
                    onClick = { scope.launch { drawerState.close() }; onNavigate("Profile") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.weight(1f))
                Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                NavigationDrawerItem(
                    label = { Text("Logout") },
                    icon = { Icon(Icons.Filled.ExitToApp, contentDescription = null) },
                    selected = false,
                    onClick = onLogout,
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedTextColor = MaterialTheme.colorScheme.error,
                        unselectedIconColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        },
        content = content
    )
}
