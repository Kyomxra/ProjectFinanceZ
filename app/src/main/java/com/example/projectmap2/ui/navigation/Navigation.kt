package com.example.projectmap2.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.projectmap2.ui.screens.*

// Navigation routes
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Profile : Screen("profile")
    object Dashboard : Screen("dashboard/{userId}") {
        fun createRoute(userId: String) = "dashboard/$userId"
    }
    object AddIncome : Screen("add_income/{userId}") {
        fun createRoute(userId: String) = "add_income/$userId"
    }
    object AddExpense : Screen("add_expense/{userId}") {
        fun createRoute(userId: String) = "add_expense/$userId"
    }
    object AddSaving : Screen("add_saving/{userId}") {
        fun createRoute(userId: String) = "add_saving/$userId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = { userId ->
                    navController.navigate(Screen.Dashboard.createRoute(userId)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Register Screen
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.popBackStack()
                }
            )
        }

        // Dashboard Screen
        composable(Screen.Dashboard.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            DashboardScreen(
                userId = userId,
                navController = navController,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Profile Screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}