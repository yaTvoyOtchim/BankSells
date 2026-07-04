package com.bank.salestracker.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.bank.salestracker.ui.screens.AddSaleBottomSheet
import com.bank.salestracker.ui.screens.AddSaleScreen
import com.bank.salestracker.ui.screens.AdminScreen
import com.bank.salestracker.ui.screens.ChangePasswordScreen
import com.bank.salestracker.ui.screens.DashboardScreen
import com.bank.salestracker.ui.screens.EmployeeDetailScreen
import com.bank.salestracker.ui.screens.EmployeeRatingScreen
import com.bank.salestracker.ui.screens.LoginScreen
import com.bank.salestracker.ui.screens.ProductManagementScreen
import com.bank.salestracker.ui.screens.ProfileScreen
import com.bank.salestracker.ui.screens.RegisterScreen
import com.bank.salestracker.ui.screens.ReportsScreen
import com.bank.salestracker.ui.screens.WaitingScreen
import com.bank.salestracker.ui.theme.appBackgroundBrush

sealed class Dest(val route: String, val label: String, val icon: ImageVector? = null) {
    data object Login : Dest("login", "Вход")
    data object Register : Dest("register", "Регистрация")
    data object Waiting : Dest("waiting", "Ожидание")
    data object ChangePassword : Dest("change_password", "Смена пароля")
    data object Home : Dest("home", "Главная", Icons.Default.Home)
    data object AddSale : Dest("add_sale", "Продажа", Icons.Default.Add)
    data object Reports : Dest("reports", "Отчёты", Icons.Default.Description)
    data object Rating : Dest("rating", "Рейтинг", Icons.Default.EmojiEvents)
    data object Profile : Dest("profile", "Профиль", Icons.Default.Person)
    data object Admin : Dest("admin", "Команда", Icons.Default.EmojiEvents)
    data object Products : Dest("products", "Продукты", Icons.Default.Settings)
    data object EmployeeDetail : Dest("employee/{id}", "Сотрудник")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val auth = ServiceLocator.authRepo
    var showAddSheet by remember { mutableStateOf(false) }
    var addSheetSession by remember { mutableStateOf(0) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

    val menuRoutes = buildList {
        if (canWork) {
            add(Dest.Home)
            add(Dest.Reports)
            add(Dest.Rating)
            add(Dest.Profile)
            if (isManager) add(Dest.Products)
        }
    }
    val showBar = currentRoute in menuRoutes.map { it.route }

    fun routeAfterAuth(mustChangePassword: Boolean): String {
        val user = auth.currentUser()
        return when {
            mustChangePassword -> Dest.ChangePassword.route
            user?.registrationStatus == RegistrationStatus.PENDING_ASSIGNMENT -> Dest.Waiting.route
            else -> Dest.Home.route
        }
    }

    androidx.compose.material3.Scaffold(
        modifier = Modifier.background(appBackgroundBrush()),
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBar) {
                VtbBottomMenu(
                    currentRoute = currentRoute,
                    canAddSale = canCreateSales,
                    onNavigate = { dest ->
                        if (currentRoute == dest.route) return@VtbBottomMenu
                        nav.navigate(dest.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddClick = {
                        if (canCreateSales) {
                            addSheetSession += 1
                            showAddSheet = true
                        } else if (isManager) {
                            nav.navigate(Dest.Products.route) { launchSingleTop = true }
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
            composable(Dest.Rating.route) {
                if (isManager) {
                    AdminScreen(
                        onEmployeeClick = { id -> nav.navigate("employee/$id") },
                        onProductsClick = { nav.navigate(Dest.Products.route) }
                    )
                } else {
                    EmployeeRatingScreen()
                }
            }
            composable(Dest.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        nav.navigate(Dest.Login.route) { popUpTo(0) }
                    },
                    onProductsClick = { nav.navigate(Dest.Products.route) },
                    canManageProducts = isManager
                )
            }
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

    if (showAddSheet && canCreateSales) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = addSheetState,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            containerColor = if (isSystemInDarkTheme()) Color(0xFF141A2B) else Color(0xFFF7F7FC),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            AddSaleBottomSheet(
                sessionKey = addSheetSession,
                onDismiss = { showAddSheet = false },
                onSaved = {
                    showAddSheet = false
                    val homeHandle = runCatching {
                        nav.getBackStackEntry(Dest.Home.route).savedStateHandle
                    }.getOrNull()
                    homeHandle?.let(SalesRefreshSignal::markChanged)
                    nav.navigate(Dest.Home.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
private fun VtbBottomMenu(
    currentRoute: String?,
    canAddSale: Boolean,
    onNavigate: (Dest) -> Unit,
    onAddClick: () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val purple = Color(0xFF6246FF)
    val inactive = if (darkTheme) Color(0xFFA7ACC3) else Color(0xFF9EA2B6)
    val barColor = if (darkTheme) Color(0xF01A2134) else Color(0xFFFDFDFF)
    val borderColor = if (darkTheme) Color.White.copy(alpha = 0.12f) else Color(0xFFE5E8F4)

    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(92.dp)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(76.dp)
                .border(1.dp, borderColor, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = barColor,
            shadowElevation = if (darkTheme) 0.dp else 20.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomMenuItem(
                    label = "Главная",
                    icon = Icons.Default.Home,
                    selected = currentRoute == Dest.Home.route,
                    activeColor = purple,
                    inactiveColor = inactive,
                    onClick = { onNavigate(Dest.Home) },
                    modifier = Modifier.weight(1f)
                )
                BottomMenuItem(
                    label = "Отчёт",
                    icon = Icons.Default.Description,
                    selected = currentRoute == Dest.Reports.route,
                    activeColor = purple,
                    inactiveColor = inactive,
                    onClick = { onNavigate(Dest.Reports) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.weight(1f))
                BottomMenuItem(
                    label = "Рейтинг",
                    icon = Icons.Default.EmojiEvents,
                    selected = currentRoute == Dest.Rating.route,
                    activeColor = purple,
                    inactiveColor = inactive,
                    onClick = { onNavigate(Dest.Rating) },
                    modifier = Modifier.weight(1f)
                )
                BottomMenuItem(
                    label = "Профиль",
                    icon = Icons.Default.Person,
                    selected = currentRoute == Dest.Profile.route,
                    activeColor = purple,
                    inactiveColor = inactive,
                    onClick = { onNavigate(Dest.Profile) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-2).dp)
                .size(64.dp)
                .shadow(
                    elevation = if (darkTheme) 0.dp else 22.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = purple.copy(alpha = 0.34f),
                    spotColor = purple.copy(alpha = 0.34f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(if (canAddSale) purple else inactive.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAddClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                if (canAddSale) "Новая продажа" else "Добавить",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun BottomMenuItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) activeColor else inactiveColor
    Column(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(23.dp))
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 3.dp)
                .fillMaxWidth()
        )
    }
}
