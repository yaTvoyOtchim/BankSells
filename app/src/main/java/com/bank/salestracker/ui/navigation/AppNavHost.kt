package com.bank.salestracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bank.salestracker.data.model.RegistrationStatus
import com.bank.salestracker.data.model.canCreateSales
import com.bank.salestracker.data.model.isManager
import com.bank.salestracker.di.ServiceLocator
import com.bank.salestracker.ui.screens.*
import com.bank.salestracker.ui.theme.GlassSurface

sealed class Dest(val route: String, val label: String, val icon: ImageVector? = null) {
    data object Login : Dest("login", "Вход")
    data object Register : Dest("register", "Регистрация")
    data object Waiting : Dest("waiting", "Ожидание")
    data object ChangePassword : Dest("change_password", "Смена пароля")
    data object Home : Dest("home", "Главная", Icons.Default.Home)
    data object AddSale : Dest("add_sale", "Продажа", Icons.Default.AddCircle)
    data object Reports : Dest("reports", "Отчёты", Icons.Default.BarChart)
    data object Admin : Dest("admin", "Команда", Icons.Default.SupervisorAccount)
    data object Products : Dest("products", "Продукты", Icons.Default.Settings)
    data object EmployeeDetail : Dest("employee/{id}", "Сотрудник")
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val auth = ServiceLocator.authRepo
    val currentUser = auth.currentUser()
    val start = when {
        !auth.isLoggedIn -> Dest.Login.route
        currentUser?.registrationStatus == RegistrationStatus.PENDING_ASSIGNMENT -> Dest.Waiting.route
        else -> Dest.Home.route
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val activeUser = auth.currentUser()
    val canWork = activeUser?.registrationStatus == RegistrationStatus.ACTIVE
    val isManager = activeUser?.role?.isManager() == true
    val canCreateSales = activeUser?.role?.canCreateSales() == true

    val tabs = buildList {
        if (canWork) {
            add(Dest.Home)
            if (canCreateSales) add(Dest.AddSale)
            add(Dest.Reports)
            if (isManager) add(Dest.Admin)
        }
    }
    val showBar = currentRoute in tabs.map { it.route }

    fun routeAfterAuth(mustChangePassword: Boolean): String {
        val user = auth.currentUser()
        return when {
            mustChangePassword -> Dest.ChangePassword.route
            user?.registrationStatus == RegistrationStatus.PENDING_ASSIGNMENT -> Dest.Waiting.route
            else -> Dest.Home.route
        }
    }

    Scaffold(
        bottomBar = {
            if (showBar) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                            tabs.forEach { dest ->
                                NavigationBarItem(
                                    selected = currentRoute == dest.route,
                                    onClick = {
                                        nav.navigate(dest.route) {
                                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { dest.icon?.let { Icon(it, dest.label) } },
                                    label = { Text(dest.label) }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = start, modifier = Modifier.padding(padding)) {
            composable(Dest.Login.route) {
                LoginScreen(
                    onLoggedIn = { mustChange ->
                        nav.navigate(routeAfterAuth(mustChange)) {
                            popUpTo(Dest.Login.route) { inclusive = true }
                        }
                    },
                    onRegisterClick = { nav.navigate(Dest.Register.route) }
                )
            }
            composable(Dest.Register.route) {
                RegisterScreen(
                    onRegistered = {
                        nav.navigate(Dest.Waiting.route) {
                            popUpTo(Dest.Login.route) { inclusive = true }
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
            composable(Dest.Waiting.route) {
                WaitingScreen(onLogout = {
                    nav.navigate(Dest.Login.route) { popUpTo(0) }
                })
            }
            composable(Dest.ChangePassword.route) {
                ChangePasswordScreen(onDone = {
                    nav.navigate(routeAfterAuth(false)) {
                        popUpTo(Dest.ChangePassword.route) { inclusive = true }
                    }
                })
            }
            composable(Dest.Home.route) {
                DashboardScreen(
                    onAddSale = { nav.navigate(Dest.AddSale.route) },
                    onLogout = {
                        nav.navigate(Dest.Login.route) { popUpTo(0) }
                    },
                    canCreateSales = canCreateSales
                )
            }
            composable(Dest.AddSale.route) {
                if (canCreateSales) {
                    AddSaleScreen(onSaved = { nav.popBackStack(Dest.Home.route, false) })
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Продажи может вносить только сотрудник")
                    }
                }
            }
            composable(Dest.Reports.route) { ReportsScreen() }
            composable(Dest.Admin.route) {
                AdminScreen(
                    onEmployeeClick = { id -> nav.navigate("employee/$id") },
                    onProductsClick = { nav.navigate(Dest.Products.route) }
                )
            }
            composable(Dest.Products.route) { ProductManagementScreen() }
            composable(Dest.EmployeeDetail.route) { entry ->
                EmployeeDetailScreen(employeeId = entry.arguments?.getString("id") ?: "")
            }
        }
    }
}
