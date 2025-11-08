package com.example.project_for_vtb.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.project_for_vtb.ui.screens.accounts.AccountsScreen
import com.example.project_for_vtb.ui.screens.analytics.AnalyticsScreen
import com.example.project_for_vtb.ui.screens.banks.BankConnectionScreen
import com.example.project_for_vtb.ui.screens.dashboard.DashboardScreen
import com.example.project_for_vtb.ui.screens.login.LoginScreen
import com.example.project_for_vtb.ui.screens.register.RegisterScreen
import com.example.project_for_vtb.ui.screens.settings.SettingsScreen
import com.example.project_for_vtb.ui.screens.transactions.TransactionsScreen
import com.example.project_for_vtb.ui.screens.welcome.WelcomeScreen

// ИМПОРТ ДЛЯ РАБОТЫ С PNG ИЗОБРАЖЕНИЯМИ
import androidx.compose.ui.res.painterResource
import com.example.project_for_vtb.R // Убедитесь, что этот импорт есть

/**
 * Граф навигации приложения
 */
sealed class Screen(val route: String, val title: String, val iconResId: Int? = null) { // Изменено на iconResId
    object Welcome : Screen("welcome", "Добро пожаловать")
    object Login : Screen("login", "Авторизация")
    object Register : Screen("register", "Регистрация")

    // Основные экраны с bottom navigation
    // ЗАМЕНИТЕ НАЗВАНИЯ НА СВОИ PNG ФАЙЛЫ:
    object Dashboard : Screen("dashboard", "Главная", R.drawable.home_icon) // Ваш PNG для главной
    object Accounts : Screen("accounts", "Счета", R.drawable.baseline_account_balance_24) // Ваш PNG для счетов
    object Transactions : Screen("transactions", "Транзакции", R.drawable.transfers_icon) // Ваш PNG для транзакций
    object Analytics : Screen("analytics", "Аналитика", R.drawable.outline_bar_chart_4_bars_24) // Ваш PNG для аналитики
    object Settings : Screen("settings", "Настройки", R.drawable.filter_btn) // Ваш PNG для настроек

    // Дополнительные экраны
    object BankConnection : Screen("bank_connection", "Подключение банков")
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Accounts,
    Screen.Transactions,
    Screen.Analytics,
)

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Welcome.route
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                // ИЗМЕНЕНО: Используем painterResource для PNG вместо ImageVector
                                screen.iconResId?.let { resId ->
                                    Icon(
                                        painter = painterResource(id = resId), // Используем painterResource
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
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
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        // После успешной регистрации переходим на экран входа
                        // так как токены не выдаются при регистрации
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToAccounts = {
                        navController.navigate(Screen.Accounts.route)
                    },
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route)
                    },
                    onNavigateToAnalytics = {
                        navController.navigate(Screen.Analytics.route)
                    },
                    onNavigateToBanks = {
                        navController.navigate(Screen.BankConnection.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(Screen.Accounts.route) {
                AccountsScreen(
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    },
                    onNavigateToAddBank = {
                        navController.navigate(Screen.BankConnection.route)
                    }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    },
                    onLogout = {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.BankConnection.route) {
                BankConnectionScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onConnectBank = { bank ->
                        // TODO: Обработать подключение банка через OAuth2
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}