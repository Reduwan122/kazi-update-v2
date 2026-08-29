package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.DashboardStatCard
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.ProductionChartCard
import com.example.ui.components.rememberHaptics
import com.example.ui.components.scaleClickable
import com.example.ui.viewmodel.PoultryViewModel

@Composable
fun DashboardScreen(
    viewModel: PoultryViewModel,
    onNavigateToAddReport: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToAddShareholderPayment: () -> Unit = {},
    onNavigateToReports: () -> Unit,
    onNavigateToDailyReport: () -> Unit,
    onNavigateToExpense: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val haptics = rememberHaptics()
    val dailyReports by viewModel.dailyReports.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val farmProfile by viewModel.farmProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val rolePermissionsMap by viewModel.rolePermissions.collectAsState()

    val todayDate = remember { BanglaNumberFormatter.getCurrentDateFormatted() }
    val hasTodayReport = remember(dailyReports, todayDate) { dailyReports.any { it.date == todayDate } }
    val notifDismissedDate by viewModel.notificationDismissedDate.collectAsState()
    val hasUnreadNotification = !hasTodayReport && notifDismissedDate != todayDate

    val userPerms = currentUser?.let { rolePermissionsMap[it.role.uppercase()] }
    val canAddReport = currentUser?.canAddReport(userPerms) == true

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = farmProfile.farmName,
                isRootScreen = true,
                logoUri = farmProfile.logoUri,
                logoEmoji = farmProfile.logoEmoji,
                userProfileImageUri = currentUser?.profileImageUri ?: "",
                username = currentUser?.username ?: "",
                hasUnreadNotification = hasUnreadNotification,
                onNotificationClick = onOpenNotifications,
                onProfileClick = onNavigateToProfile
            )
        },
        floatingActionButton = {
            if (canAddReport) {
                FloatingActionButton(
                    onClick = {
                        haptics.tap()
                        onNavigateToAddReport()
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("dashboard_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New Report",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("dashboard_screen"),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Bento Grid Summary Cards (2x2)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardStatCard(
                        title = "বর্তমান মুরগী",
                        value = BanglaNumberFormatter.formatNumber(stats.currentBirds),
                        icon = Icons.Default.Pets,
                        iconTint = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = onNavigateToDailyReport,
                        modifier = Modifier.weight(1f)
                    )

                    DashboardStatCard(
                        title = "আজকের ডিম উৎপাদন",
                        value = BanglaNumberFormatter.formatNumber(stats.todayEggProduction),
                        icon = Icons.Default.Egg,
                        isPrimaryHighlight = true,
                        onClick = onNavigateToDailyReport,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardStatCard(
                        title = "আজকের মোট বিক্রয়",
                        value = BanglaNumberFormatter.formatCurrency(stats.todayTotalSale),
                        icon = Icons.Default.Payments,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToDailyReport,
                        modifier = Modifier.weight(1f)
                    )

                    DashboardStatCard(
                        title = "আজকের মোট ব্যয়",
                        value = BanglaNumberFormatter.formatCurrency(stats.todayTotalExpense),
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = onNavigateToExpense,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Total Stock Card (মোট স্টক)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .scaleClickable { onNavigateToDailyReport() }
                    .testTag("stat_card_total_stock"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = "মোট স্টক",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "মোট স্টক",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = BanglaNumberFormatter.formatNumber(stats.currentEggStock) + " টি",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // Quick Actions (কুইক একশন)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "কুইক একশন",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "নতুন দৈনিক রিপোর্ট",
                        icon = Icons.Default.AddCircle,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToAddReport,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_new_report"
                    )

                    QuickActionButton(
                        title = "নতুন মাসিক ব্যয়",
                        icon = Icons.Default.MoneyOff,
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = onNavigateToAddExpense,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_new_expense"
                    )
                }

                val isAdmin = currentUser?.isAdmin() == true
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isAdmin) {
                        QuickActionButton(
                            title = "পেমেন্ট যোগ করুন",
                            icon = Icons.Default.Payments,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToAddShareholderPayment,
                            modifier = Modifier.weight(1f),
                            testTag = "quick_action_new_shareholder_payment"
                        )
                    }

                    QuickActionButton(
                        title = "রিপোর্ট ও বিশ্লেষণ",
                        icon = Icons.Default.Analytics,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        onClick = onNavigateToReports,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_reports"
                    )
                }
            }

            // Interactive Production Chart
            ProductionChartCard(reports = dailyReports)

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .scaleClickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 6.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
