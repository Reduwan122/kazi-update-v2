package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.domain.StockCalculationService
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyReportEntity
import com.example.data.local.MonthlyExpenseEntity
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.MonthlySummaryShareDialog
import com.example.ui.components.PdfPreviewModalDialog
import com.example.ui.components.rememberHaptics
import com.example.ui.components.scaleClickable
import com.example.ui.viewmodel.PoultryViewModel

import androidx.compose.material.icons.filled.Groups

enum class ReportCategory(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DAILY("দৈনিক রিপোর্ট", Icons.Default.DateRange),
    MONTHLY("মাসিক রিপোর্ট", Icons.Default.CalendarMonth),
    SALES("বিক্রয় রিপোর্ট", Icons.Default.Paid),
    PRODUCTION("উৎপাদন রিপোর্ট", Icons.Default.Egg),
    EXPENSE("ব্যয় রিপোর্ট", Icons.Default.AccountBalanceWallet),
    PROFIT_LOSS("লাভ-ক্ষতি বিবরণী", Icons.Default.ShowChart)
}

@Composable
fun ReportsScreen(
    viewModel: PoultryViewModel,
    onOpenNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToShareholderPayments: () -> Unit = {},
    onNavigateToStaffPayments: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val dailyReports by viewModel.dailyReports.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val farmProfile by viewModel.farmProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val rolePermissionsMap by viewModel.rolePermissions.collectAsState()

    val todayDate = remember { BanglaNumberFormatter.getCurrentDateFormatted() }
    val hasTodayReport = remember(dailyReports, todayDate) { dailyReports.any { it.date == todayDate } }
    val notifDismissedDate by viewModel.notificationDismissedDate.collectAsState()
    val hasUnreadNotification = !hasTodayReport && notifDismissedDate != todayDate

    val userPerms = currentUser?.let { rolePermissionsMap[it.role.uppercase()] }
    val canViewReports = currentUser?.canViewReportsAndAnalytics(userPerms) ?: false
    val canDownload = currentUser?.canDownloadReports(userPerms) ?: false

    var selectedCategory by remember { mutableStateOf(ReportCategory.MONTHLY) }
    var selectedMonthFilter by remember { mutableStateOf(BanglaNumberFormatter.getCurrentDateFormatted().take(7)) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    var showPdfPreviewModal by remember { mutableStateOf(false) }
    var showShareCardModal by remember { mutableStateOf(false) }

    // Months that actually have data (from either reports or expenses), plus the current month, newest first
    val availableMonths = remember(dailyReports, expenses) {
        (dailyReports.map { it.date.take(7) } + expenses.map { it.date.take(7) } + BanglaNumberFormatter.getCurrentDateFormatted().take(7))
            .toSortedSet(compareByDescending { it })
            .toList()
    }

    // Computed for selected month
    val filteredDaily = remember(dailyReports, selectedMonthFilter) {
        if (selectedMonthFilter == "সকল রেকর্ড") dailyReports
        else dailyReports.filter { it.date.startsWith(selectedMonthFilter) }
    }

    val filteredExpenses = remember(expenses, selectedMonthFilter) {
        if (selectedMonthFilter == "সকল রেকর্ড") expenses
        else expenses.filter { it.date.startsWith(selectedMonthFilter) }
    }

    val totalProduction = filteredDaily.sumOf { it.eggProduction }
    val totalSold = filteredDaily.sumOf { it.eggSold }
    val totalSale = filteredDaily.sumOf { it.totalSale }
    val totalMedicine = filteredDaily.sumOf { it.medicineCost }
    val totalMortality = filteredDaily.sumOf { it.deadBirds }
    val currentBirds = filteredDaily.lastOrNull()?.currentBirds ?: 0
    val totalExpense = filteredExpenses.sumOf { it.totalExpense }
    val netProfit = totalSale - totalExpense

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "রিপোর্ট ও বিশ্লেষণ",
                isRootScreen = true,
                logoUri = farmProfile.logoUri,
                logoEmoji = farmProfile.logoEmoji,
                userProfileImageUri = currentUser?.profileImageUri ?: "",
                username = currentUser?.username ?: "",
                hasUnreadNotification = hasUnreadNotification,
                onNotificationClick = onOpenNotifications,
                onProfileClick = onNavigateToProfile
            )
        }
    ) { innerPadding ->
        if (!canViewReports) {
            com.example.ui.components.AccessDeniedView(
                title = "রিপোর্ট ও বিশ্লেষণ সংরক্ষিত",
                message = "আপনার রোলে রিপোর্ট ও বিশ্লেষণ দেখার অনুমতি সক্রিয় করা নেই।",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("reports_screen"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Month Filter & Actions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "রিপোর্টের সময়কাল",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Box {
                            OutlinedButton(
                                onClick = { monthDropdownExpanded = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (selectedMonthFilter == "সকল রেকর্ড") "সকল রেকর্ড" else BanglaNumberFormatter.formatYearMonth(selectedMonthFilter),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Month filter",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = monthDropdownExpanded,
                                onDismissRequest = { monthDropdownExpanded = false }
                            ) {
                                availableMonths.forEach { month ->
                                    val currentMonth = BanglaNumberFormatter.getCurrentDateFormatted().take(7)
                                    val label = BanglaNumberFormatter.formatYearMonth(month) +
                                        if (month == currentMonth) " (চলতি মাস)" else ""
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedMonthFilter = month
                                            monthDropdownExpanded = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("সকল রেকর্ড") },
                                    onClick = {
                                        selectedMonthFilter = "সকল রেকর্ড"
                                        monthDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Export / Print Action Buttons
                    if (canDownload) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    haptics.tap()
                                    showShareCardModal = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("btn_reports_share_card")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "শেয়ার",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    showPdfPreviewModal = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("btn_reports_pdf")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "পিডিএফ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    if (selectedCategory == ReportCategory.EXPENSE) {
                                        viewModel.exportExpensesCsv(context)
                                    } else {
                                        viewModel.exportDailyReportsCsv(context)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("btn_reports_excel")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableView,
                                    contentDescription = "Excel",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "এক্সেল",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Summary Bento Cards for Filter
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "নির্বাচিত মাসের পরিসংখ্যান",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportStatItem(
                        title = "মোট ডিম উৎপাদন",
                        value = "${BanglaNumberFormatter.formatNumber(totalProduction)} পিস",
                        icon = Icons.Default.Egg,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    ReportStatItem(
                        title = "মোট বিক্রয়",
                        value = BanglaNumberFormatter.formatCurrency(totalSale),
                        icon = Icons.Default.Paid,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportStatItem(
                        title = "মোট খামার ব্যয়",
                        value = BanglaNumberFormatter.formatCurrency(totalExpense),
                        icon = Icons.Default.AccountBalanceWallet,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )

                    ReportStatItem(
                        title = "নিট লাভ / উদ্বৃত্ত",
                        value = BanglaNumberFormatter.formatCurrency(netProfit),
                        icon = Icons.Default.ShowChart,
                        color = if (netProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Report Categories Grid (Bento style)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "রিপোর্টের ধরণ নির্বাচন করুন",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                val categories = ReportCategory.values()
                for (i in categories.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportCategoryCard(
                            category = categories[i],
                            isSelected = selectedCategory == categories[i],
                            onClick = {
                                selectedCategory = categories[i]
                                showPdfPreviewModal = true
                            },
                            modifier = Modifier.weight(1f)
                        )

                        if (i + 1 < categories.size) {
                            ReportCategoryCard(
                                category = categories[i + 1],
                                isSelected = selectedCategory == categories[i + 1],
                                onClick = {
                                    selectedCategory = categories[i + 1]
                                    showPdfPreviewModal = true
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                // Shareholder Payments Report Entry
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            haptics.tap()
                            onNavigateToShareholderPayments()
                        }
                        .testTag("card_shareholder_payments_report"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "সকল শেয়ারহোল্ডার পেমেন্ট",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "শেয়ারহোল্ডার পেমেন্ট হিস্টোরি ও রিপোর্ট",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Staff Payments Report Entry
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            haptics.tap()
                            onNavigateToStaffPayments()
                        }
                        .testTag("card_staff_payments_report"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = Color(0xFF0D631B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "সকল স্টাফ পেমেন্ট",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "স্টাফ পেমেন্ট হিস্টোরি ও রিপোর্ট",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

    if (showShareCardModal) {
        val monthLabel = if (selectedMonthFilter == "সকল রেকর্ড") {
            "সকল রেকর্ড সারাংশ"
        } else {
            BanglaNumberFormatter.formatYearMonth(selectedMonthFilter)
        }

        MonthlySummaryShareDialog(
            monthLabel = monthLabel,
            totalBirds = currentBirds,
            totalProduction = totalProduction,
            totalSold = totalSold,
            totalSale = totalSale,
            totalMedicine = totalMedicine,
            totalExpense = totalExpense,
            totalMortality = totalMortality,
            farmProfile = farmProfile,
            onDismiss = { showShareCardModal = false }
        )
    }

    if (showPdfPreviewModal) {
        val title = when (selectedCategory) {
            ReportCategory.DAILY -> "দৈনিক খামার প্রতিবেদন"
            ReportCategory.MONTHLY -> "মাসিক সামগ্রিক প্রতিবেদন"
            ReportCategory.SALES -> "ডিম বিক্রয় প্রতিবেদন"
            ReportCategory.PRODUCTION -> "ডিম উৎপাদন ও ফ্লক স্বাস্থ্য প্রতিবেদন"
            ReportCategory.EXPENSE -> "খামার ব্যয় রেজিস্টার"
            ReportCategory.PROFIT_LOSS -> "আর্থিক লাভ-ক্ষতি বিবরণী"
        }

        PdfPreviewModalDialog(
            title = title,
            farmProfile = farmProfile,
            dailyReports = filteredDaily,
            allReports = dailyReports,
            baselineStock = farmProfile.initialOpeningStock,
            expenses = filteredExpenses,
            reportCategory = selectedCategory.name,
            onDismiss = { showPdfPreviewModal = false }
        )
    }
}

@Composable
fun ReportStatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 16.sp
                )
            )
        }
    }
}

@Composable
fun ReportCategoryCard(
    category: ReportCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .scaleClickable { onClick() }
            .testTag("report_card_${category.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
            )
        }
    }
}
