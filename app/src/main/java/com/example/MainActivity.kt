package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.DailyReportEntity
import com.example.data.local.MonthlyExpenseEntity
import com.example.data.local.ShareholderPaymentEntity
import com.example.ui.components.AppBottomNavBar
import com.example.ui.components.AppSnackbarHost
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.BottomNavTab
import com.example.ui.components.FarmNotificationDialog
import com.example.ui.components.PdfPreviewModalDialog
import com.example.ui.components.ShareholderPdfPreviewModalDialog
import com.example.ui.components.SnackbarBottomInset
import com.example.ui.screens.AddEditDailyReportScreen
import com.example.ui.screens.AddEditMonthlyExpenseScreen
import com.example.ui.screens.AddEditShareholderPaymentScreen
import com.example.ui.screens.AdminUserManagementScreen
import com.example.ui.screens.AllShareholderPaymentsScreen
import com.example.ui.screens.CloudBackupScreen
import com.example.ui.screens.DailyReportDetailScreen
import com.example.ui.screens.DailyReportScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.IndividualShareholderHistoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MonthlyExpenseDetailScreen
import com.example.ui.screens.MonthlyExpenseScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.RolePermissionEditorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShareholderSettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.KaziAgrotechTheme
import com.example.ui.viewmodel.PoultryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PoultryViewModel = viewModel()
            val farmProfile by viewModel.farmProfile.collectAsState()
            val updateState by viewModel.updateState.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current

            // Check for updates automatically in background on startup (subject to 6h cooldown)
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.checkForUpdates(isManual = false)
            }

            KaziAgrotechTheme(darkTheme = farmProfile.isDarkMode) {
                // One snackbar host for the whole app: several screens (login, splash) are not
                // built on a Scaffold, and messages also come from the ViewModel and dialogs.
                Box(modifier = Modifier.fillMaxSize()) {
                    MainAppNavigation(viewModel = viewModel)

                    AppUpdateDialog(
                        updateState = updateState,
                        onUpdateClick = { info -> viewModel.downloadAndInstallUpdate(context, info) },
                        onInstallClick = { info ->
                            val downloadedState = updateState as? com.example.data.update.UpdateState.Downloaded
                            if (downloadedState != null) {
                                viewModel.installDownloadedApk(downloadedState.apkFile, info)
                            } else {
                                viewModel.downloadAndInstallUpdate(context, info)
                            }
                        },
                        onDismiss = { viewModel.dismissUpdateDialog() },
                        onCancelDownload = { viewModel.cancelUpdateDownload() }
                    )

                    AppSnackbarHost(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .testTag("app_snackbar_host")
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppNavigation(viewModel: PoultryViewModel) {
    val navController = rememberNavController()
    val farmProfile by viewModel.farmProfile.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                logoUri = farmProfile.logoUri,
                logoEmoji = farmProfile.logoEmoji,
                onFinished = {
                    val destination = if (viewModel.isUserLoggedInAndApproved()) "main" else "login"
                    navController.navigate(destination) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainContainerScreen(
                viewModel = viewModel,
                onNavigateToAddDailyReport = { navController.navigate("add_edit_daily_report/0") },
                onNavigateToEditDailyReport = { id -> navController.navigate("add_edit_daily_report/$id") },
                onNavigateToDailyReportDetail = { id -> navController.navigate("daily_report_detail/$id") },
                onNavigateToAddExpense = { navController.navigate("add_edit_expense/0") },
                onNavigateToEditExpense = { id -> navController.navigate("add_edit_expense/$id") },
                onNavigateToExpenseDetail = { id -> navController.navigate("expense_detail/$id") },
                onNavigateToAddShareholderPayment = { navController.navigate("add_shareholder_payment") },
                onNavigateToShareholderSettings = { navController.navigate("shareholder_settings") },
                onNavigateToShareholderPayments = { navController.navigate("all_shareholder_payments") },
                onNavigateToAdmin = { navController.navigate("admin_management") },
                onNavigateToRolePermissions = { role -> navController.navigate("role_permissions/$role") },
                onNavigateToProfile = { navController.navigate("user_profile") },
                onNavigateToBackupRestore = { navController.navigate("backup_restore") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "add_edit_daily_report/{reportId}",
            arguments = listOf(navArgument("reportId") { type = NavType.LongType; defaultValue = 0L })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getLong("reportId") ?: 0L
            AddEditDailyReportScreen(
                reportId = reportId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "daily_report_detail/{reportId}",
            arguments = listOf(navArgument("reportId") { type = NavType.LongType })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getLong("reportId") ?: 0L
            var pdfReportToPreview by remember { mutableStateOf<DailyReportEntity?>(null) }
            val farmProfile by viewModel.farmProfile.collectAsState()

            DailyReportDetailScreen(
                reportId = reportId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("add_edit_daily_report/$id") },
                onPdfPreview = { report -> pdfReportToPreview = report }
            )

            if (pdfReportToPreview != null) {
                PdfPreviewModalDialog(
                    title = "দৈনিক প্রতিবেদন (${pdfReportToPreview!!.date})",
                    farmProfile = farmProfile,
                    dailyReports = listOf(pdfReportToPreview!!),
                    onDismiss = { pdfReportToPreview = null }
                )
            }
        }

        composable(
            route = "add_edit_expense/{expenseId}",
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType; defaultValue = 0L })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: 0L
            AddEditMonthlyExpenseScreen(
                expenseId = expenseId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "expense_detail/{expenseId}",
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: 0L
            var pdfExpenseToPreview by remember { mutableStateOf<MonthlyExpenseEntity?>(null) }
            val farmProfile by viewModel.farmProfile.collectAsState()

            MonthlyExpenseDetailScreen(
                expenseId = expenseId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("add_edit_expense/$id") },
                onPdfPreview = { expense -> pdfExpenseToPreview = expense }
            )

            if (pdfExpenseToPreview != null) {
                PdfPreviewModalDialog(
                    title = "মাসিক ব্যয় (${pdfExpenseToPreview!!.date})",
                    farmProfile = farmProfile,
                    expenses = listOf(pdfExpenseToPreview!!),
                    onDismiss = { pdfExpenseToPreview = null }
                )
            }
        }

        composable("admin_management") {
            AdminUserManagementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToRolePermissions = { role ->
                    navController.navigate("role_permissions/$role")
                }
            )
        }

        composable(
            route = "role_permissions/{roleKey}",
            arguments = listOf(navArgument("roleKey") { type = NavType.StringType; defaultValue = "MANAGER" })
        ) { backStackEntry ->
            val roleKey = backStackEntry.arguments?.getString("roleKey") ?: "MANAGER"
            RolePermissionEditorScreen(
                viewModel = viewModel,
                initialRole = roleKey,
                onBack = { navController.popBackStack() }
            )
        }

        composable("role_permissions") {
            RolePermissionEditorScreen(
                viewModel = viewModel,
                initialRole = "MANAGER",
                onBack = { navController.popBackStack() }
            )
        }

        composable("user_profile") {
            UserProfileScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        composable("backup_restore") {
            CloudBackupScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("shareholder_settings") {
            ShareholderSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("add_shareholder_payment") {
            AddEditShareholderPaymentScreen(
                viewModel = viewModel,
                paymentId = null,
                onBack = { navController.popBackStack() },
                onNavigateToShareholderSettings = { navController.navigate("shareholder_settings") }
            )
        }

        composable(
            route = "edit_shareholder_payment/{paymentId}",
            arguments = listOf(navArgument("paymentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val paymentId = backStackEntry.arguments?.getString("paymentId") ?: ""
            AddEditShareholderPaymentScreen(
                viewModel = viewModel,
                paymentId = paymentId,
                onBack = { navController.popBackStack() },
                onNavigateToShareholderSettings = { navController.navigate("shareholder_settings") }
            )
        }

        composable("all_shareholder_payments") {
            var pdfPaymentsToPreview by remember { mutableStateOf<Pair<List<ShareholderPaymentEntity>, String>?>(null) }
            val profile by viewModel.farmProfile.collectAsState()

            AllShareholderPaymentsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAddPayment = { navController.navigate("add_shareholder_payment") },
                onNavigateToEditPayment = { pid -> navController.navigate("edit_shareholder_payment/$pid") },
                onNavigateToShareholderHistory = { id, name -> navController.navigate("shareholder_history/$id?name=$name") },
                onOpenPdfPreview = { list, title -> pdfPaymentsToPreview = Pair(list, title) }
            )

            if (pdfPaymentsToPreview != null) {
                ShareholderPdfPreviewModalDialog(
                    title = pdfPaymentsToPreview!!.second,
                    farmProfile = profile,
                    payments = pdfPaymentsToPreview!!.first,
                    onDismiss = { pdfPaymentsToPreview = null }
                )
            }
        }

        composable(
            route = "shareholder_history/{shareholderId}?name={name}",
            arguments = listOf(
                navArgument("shareholderId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val shareholderId = backStackEntry.arguments?.getString("shareholderId") ?: ""
            val nameParam = backStackEntry.arguments?.getString("name") ?: ""
            var pdfPaymentsToPreview by remember { mutableStateOf<Pair<List<ShareholderPaymentEntity>, String>?>(null) }
            val profile by viewModel.farmProfile.collectAsState()

            IndividualShareholderHistoryScreen(
                viewModel = viewModel,
                shareholderIdentifier = shareholderId,
                shareholderNameParam = nameParam,
                onBack = { navController.popBackStack() },
                onNavigateToAddPayment = { navController.navigate("add_shareholder_payment") },
                onNavigateToEditPayment = { pid -> navController.navigate("edit_shareholder_payment/$pid") },
                onOpenPdfPreview = { list, title -> pdfPaymentsToPreview = Pair(list, title) }
            )

            if (pdfPaymentsToPreview != null) {
                ShareholderPdfPreviewModalDialog(
                    title = pdfPaymentsToPreview!!.second,
                    farmProfile = profile,
                    payments = pdfPaymentsToPreview!!.first,
                    onDismiss = { pdfPaymentsToPreview = null }
                )
            }
        }
    }
}

@Composable
fun MainContainerScreen(
    viewModel: PoultryViewModel,
    onNavigateToAddDailyReport: () -> Unit,
    onNavigateToEditDailyReport: (Long) -> Unit,
    onNavigateToDailyReportDetail: (Long) -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToEditExpense: (Long) -> Unit,
    onNavigateToExpenseDetail: (Long) -> Unit,
    onNavigateToAddShareholderPayment: () -> Unit = {},
    onNavigateToShareholderSettings: () -> Unit = {},
    onNavigateToShareholderPayments: () -> Unit = {},
    onNavigateToAdmin: () -> Unit,
    onNavigateToRolePermissions: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit,
    onNavigateToBackupRestore: () -> Unit = {},
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableStateOf(BottomNavTab.DASHBOARD) }
    var showFarmNotifications by remember { mutableStateOf(false) }
    var pdfPreviewDailyReports by remember { mutableStateOf<List<DailyReportEntity>?>(null) }
    var pdfPreviewExpenses by remember { mutableStateOf<List<MonthlyExpenseEntity>?>(null) }
    val farmProfile by viewModel.farmProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    androidx.compose.runtime.LaunchedEffect(currentUser) {
        if (currentUser != null && !currentUser!!.isApprovedUser()) {
            onLogout()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AppBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        // Keep the app-wide snackbar above the bottom navigation bar while this tabbed screen is shown.
        SnackbarBottomInset(innerPadding.calculateBottomPadding())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (currentTab) {
                BottomNavTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddReport = onNavigateToAddDailyReport,
                    onNavigateToAddExpense = onNavigateToAddExpense,
                    onNavigateToAddShareholderPayment = onNavigateToAddShareholderPayment,
                    onNavigateToReports = { currentTab = BottomNavTab.REPORTS },
                    onNavigateToDailyReport = { currentTab = BottomNavTab.DAILY_REPORT },
                    onNavigateToExpense = { currentTab = BottomNavTab.EXPENSE },
                    onOpenNotifications = { showFarmNotifications = true },
                    onNavigateToProfile = onNavigateToProfile
                )

                BottomNavTab.DAILY_REPORT -> DailyReportScreen(
                    viewModel = viewModel,
                    onNavigateToAddReport = onNavigateToAddDailyReport,
                    onNavigateToEditReport = onNavigateToEditDailyReport,
                    onNavigateToDetail = onNavigateToDailyReportDetail,
                    onPreviewPdf = { list -> pdfPreviewDailyReports = list },
                    onOpenNotifications = { showFarmNotifications = true },
                    onNavigateToProfile = onNavigateToProfile
                )

                BottomNavTab.EXPENSE -> MonthlyExpenseScreen(
                    viewModel = viewModel,
                    onNavigateToAddExpense = onNavigateToAddExpense,
                    onNavigateToEditExpense = onNavigateToEditExpense,
                    onNavigateToDetail = onNavigateToExpenseDetail,
                    onPreviewExpensePdf = { list -> pdfPreviewExpenses = list },
                    onOpenNotifications = { showFarmNotifications = true },
                    onNavigateToProfile = onNavigateToProfile
                )

                BottomNavTab.REPORTS -> ReportsScreen(
                    viewModel = viewModel,
                    onOpenNotifications = { showFarmNotifications = true },
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToShareholderPayments = onNavigateToShareholderPayments
                )

                BottomNavTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToAdmin = onNavigateToAdmin,
                    onNavigateToRolePermissions = onNavigateToRolePermissions,
                    onNavigateToShareholderSettings = onNavigateToShareholderSettings,
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToBackupRestore = onNavigateToBackupRestore,
                    onOpenNotifications = { showFarmNotifications = true },
                    onLogout = onLogout
                )
            }

            if (showFarmNotifications) {
                FarmNotificationDialog(
                    viewModel = viewModel,
                    onDismiss = {
                        showFarmNotifications = false
                        viewModel.markNotificationsRead()
                    },
                    onNavigateToAddDailyReport = {
                        viewModel.markNotificationsRead()
                        onNavigateToAddDailyReport()
                    },
                    onNavigateToDailyReportList = {
                        viewModel.markNotificationsRead()
                        currentTab = BottomNavTab.DAILY_REPORT
                    }
                )
            }

            if (pdfPreviewDailyReports != null) {
                PdfPreviewModalDialog(
                    title = "দৈনিক প্রতিবেদন রেজিস্টার",
                    farmProfile = farmProfile,
                    dailyReports = pdfPreviewDailyReports!!,
                    onDismiss = { pdfPreviewDailyReports = null }
                )
            }

            if (pdfPreviewExpenses != null) {
                PdfPreviewModalDialog(
                    title = "মাসিক ব্যয় রেজিস্টার",
                    farmProfile = farmProfile,
                    expenses = pdfPreviewExpenses!!,
                    onDismiss = { pdfPreviewExpenses = null }
                )
            }
        }
    }
}
