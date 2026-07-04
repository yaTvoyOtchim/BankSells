package com.bank.salestracker.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.bank.salestracker.ui.theme.HeroGradient
import com.bank.salestracker.ui.theme.appBackgroundBrush

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
            if (isManager) add(Dest.Products)
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
        modifier = Modifier.background(appBackgroundBrush()),
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBar) {
                VtbBottomMenu(
                    tabs = tabs,
                    currentRoute = currentRoute,
                    onNavigate = { dest ->
                        if (currentRoute == dest.route) return@VtbBottomMenu
                        if (dest == Dest.AddSale) {
                            nav.navigate(dest.route) { launchSingleTop = true }
                        } else {
                            nav.navigate(dest.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
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
            composable(Dest.Home.route) { entry ->
                val salesChangedAt by entry.savedStateHandle
                    .getStateFlow(SalesRefreshSignal.KEY, 0L)
                    .collectAsState()
                DashboardScreen(
                    onLogout = {
                        nav.navigate(Dest.Login.route) { popUpTo(0) }
                    },
                    refreshSignal = salesChangedAt
                )
            }
            composable(Dest.AddSale.route) {
                if (canCreateSales) {
                    AddSaleScreen(onSaved = {
                        val homeHandle = runCatching {
                            nav.getBackStackEntry(Dest.Home.route).savedStateHandle
                        }.getOrNull()
                        homeHandle?.let(SalesRefreshSignal::markChanged)
                        nav.popBackStack(Dest.Home.route, false)
                    })
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

@Composable
private fun VtbBottomMenu(
    tabs: List<Dest>,
    currentRoute: String?,
    onNavigate: (Dest) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(34.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            elevation = if (isSystemInDarkTheme()) 0.dp else 18.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { dest ->
                    val selected = currentRoute == dest.route
                    if (dest == Dest.AddSale) {
                        AddMenuItem(
                            selected = selected,
                            onClick = { onNavigate(dest) },
                            modifier = Modifier.weight(1.18f)
                        )
                    } else {
                        BottomMenuItem(
                            label = dest.label,
                            icon = dest.icon,
                            selected = selected,
                            onClick = { onNavigate(dest) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomMenuItem(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val itemBrush = if (selected) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f)
            )
        )
    } else {
        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    Column(
        modifier = modifier
            .height(62.dp)
            .padding(horizontal = 3.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(itemBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
        }
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun AddMenuItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val shadowColor = if (darkTheme) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
    val brush = if (selected) {
        HeroGradient
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary,
                Color(0xFF00B7D8)
            )
        )
    }
    Column(
        modifier = modifier
            .height(68.dp)
            .padding(horizontal = 4.dp)
            .shadow(
                elevation = if (darkTheme) 0.dp else 16.dp,
                shape = RoundedCornerShape(25.dp),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(RoundedCornerShape(25.dp))
            .background(brush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.AddCircle,
            "Добавить продажу",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = "Продажа",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .widthIn(max = 82.dp)
        )
    }
}
