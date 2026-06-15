package com.warungdata.pos.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.warungdata.pos.core.datastore.SettingsRepository
import com.warungdata.pos.features.auth.PinScreen
import com.warungdata.pos.features.onboarding.OnboardingScreen
import com.warungdata.pos.features.cashier.CashierScreen
import com.warungdata.pos.features.customers.CustomersScreen
import com.warungdata.pos.features.dashboard.DashboardScreen
import com.warungdata.pos.features.debts.DebtScreen
import com.warungdata.pos.features.expenses.ExpenseScreen
import com.warungdata.pos.features.categories.CategoryScreen
import com.warungdata.pos.features.cash_session.CashSessionScreen
import com.warungdata.pos.features.transactions.TransactionHistoryScreen
import com.warungdata.pos.features.backup.BackupScreen
import com.warungdata.pos.features.export_features.ExportScreen
import com.warungdata.pos.features.products.ProductsScreen
import com.warungdata.pos.features.reports.ReportScreen
import com.warungdata.pos.features.settings.SettingsScreen
import com.warungdata.pos.features.stock.StockScreen
import com.warungdata.pos.features.suppliers.SupplierScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val bottomNavItems = listOf(
    Screen("dashboard", "Dashboard", Icons.Default.Dashboard),
    Screen("cashier", "Kasir", Icons.Default.PointOfSale),
    Screen("products", "Produk", Icons.Default.Inventory),
    Screen("stock", "Stok", Icons.Default.ShoppingCart),
    Screen("debts", "Utang", Icons.Default.AccountBalance),
    Screen("reports", "Laporan", Icons.Default.Receipt),
)

class Screen(val route: String, val title: String, val icon: ImageVector)

@Composable
fun NavGraph() {
    var startDest by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val repo = SettingsRepository(context)
        startDest = if (repo.isOnboardingCompleted()) "main" else "onboarding"
    }

    when (startDest) {
        null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            AppNavHost(startDest = startDest!!)
        }
    }
}

@Composable
fun AppNavHost(startDest: String) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            val pinRepo = SettingsRepository(LocalContext.current)
            var needsPin by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                needsPin = pinRepo.isPinSet()
            }

            when (needsPin) {
                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                true -> PinScreen(
                    onVerified = {
                        navController.navigate("main_inner") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                )
                false -> MainScaffold()
            }
        }

        composable("main_inner") {
            MainScaffold()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold() {
    val innerNavController = rememberNavController()
    val innerNavBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val innerDestination = innerNavBackStackEntry?.destination

    fun showBottomBar(): Boolean {
        val route = innerDestination?.route
        return bottomNavItems.any { it.route == route }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar()) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = innerDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                innerNavController.navigate(screen.route) {
                                    popUpTo(innerNavController.graph.findStartDestination().id) {
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
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onSettings = { innerNavController.navigate("settings") },
                    onExport = { innerNavController.navigate("export") },
                    onBackup = { innerNavController.navigate("backup") },
                    onCategories = { innerNavController.navigate("categories") },
                    onTransactions = { innerNavController.navigate("transactions") },
                    onCashSession = { innerNavController.navigate("cash_session") }
                )
            }
            composable("cashier") { CashierScreen() }
            composable("products") { ProductsScreen() }
            composable("stock") { StockScreen() }
            composable("debts") { DebtScreen() }
            composable("reports") { ReportScreen() }
            composable("settings") {
                SettingsScreen(onBack = { innerNavController.popBackStack() })
            }
            composable("export") {
                ExportScreen(onBack = { innerNavController.popBackStack() })
            }
            composable("backup") {
                BackupScreen(onBack = { innerNavController.popBackStack() })
            }
            composable("categories") {
                CategoryScreen(onBack = { innerNavController.popBackStack() })
            }
            composable("cash_session") {
                CashSessionScreen(onBack = { innerNavController.popBackStack() })
            }
            composable("transactions") {
                TransactionHistoryScreen(onBack = { innerNavController.popBackStack() })
            }
        }
    }
}
