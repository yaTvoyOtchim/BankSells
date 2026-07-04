package com.example.vtbsales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.vtbsales.data.SalesRepository
import com.example.vtbsales.model.AppScreen
import com.example.vtbsales.ui.VtbAppState
import com.example.vtbsales.ui.screens.AdminHomeScreen
import com.example.vtbsales.ui.screens.AddSaleScreen
import com.example.vtbsales.ui.screens.EmployeeHomeScreen
import com.example.vtbsales.ui.screens.EmployeeProfileScreen
import com.example.vtbsales.ui.screens.EmployeeRankingScreen
import com.example.vtbsales.ui.screens.EmployeeReportsScreen
import com.example.vtbsales.ui.screens.EmployeeStatsScreen
import com.example.vtbsales.ui.screens.ManagerDashboardLiveScreen
import com.example.vtbsales.ui.screens.ManagerEmployeeCardScreen
import com.example.vtbsales.ui.screens.ManagerPlansEditorScreen
import com.example.vtbsales.ui.screens.ManagerProfileScreen
import com.example.vtbsales.ui.screens.ManagerTeamScreen
import com.example.vtbsales.ui.screens.PinLoginScreen
import com.example.vtbsales.ui.screens.RegisterScreen
import com.example.vtbsales.ui.screens.UidCreatedScreen
import com.example.vtbsales.ui.screens.WelcomeScreen
import com.example.vtbsales.ui.theme.VtbSalesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = SalesRepository.local(applicationContext)
        setContent {
            val appState = remember { VtbAppState(repository) }
            VtbSalesTheme {
                VtbSalesApp(appState)
            }
        }
    }
}

@Composable
fun VtbSalesApp(state: VtbAppState) {
    when (state.screen) {
        AppScreen.Welcome -> WelcomeScreen(state)
        AppScreen.Register -> RegisterScreen(state)
        AppScreen.UidCreated -> UidCreatedScreen(state)
        AppScreen.PinLogin -> PinLoginScreen(state)
        AppScreen.EmployeeHome -> EmployeeHomeScreen(state)
        AppScreen.AddSale -> AddSaleScreen(state)
        AppScreen.EmployeeReports -> EmployeeReportsScreen(state)
        AppScreen.EmployeeStats -> EmployeeStatsScreen(state)
        AppScreen.EmployeeRanking -> EmployeeRankingScreen(state)
        AppScreen.EmployeeProfile -> EmployeeProfileScreen(state)
        AppScreen.ManagerHome -> ManagerDashboardLiveScreen(state)
        AppScreen.ManagerTeam -> ManagerTeamScreen(state)
        AppScreen.ManagerEmployeeCard -> ManagerEmployeeCardScreen(state)
        AppScreen.ManagerPlans -> ManagerPlansEditorScreen(state)
        AppScreen.ManagerProfile -> ManagerProfileScreen(state)
        AppScreen.AdminHome -> AdminHomeScreen(state)
    }
}
