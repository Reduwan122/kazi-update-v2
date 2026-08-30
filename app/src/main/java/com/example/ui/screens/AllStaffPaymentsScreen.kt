package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.local.StaffEntity
import com.example.data.local.StaffPaymentEntity
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.SnackbarController
import com.example.ui.components.rememberHaptics
import com.example.ui.viewmodel.PoultryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AllStaffPaymentsScreen(
    viewModel: PoultryViewModel,
    onBack: () -> Unit,
    onNavigateToAddPayment: () -> Unit,
    onNavigateToEditPayment: (String) -> Unit,
    onNavigateToStaffHistory: (String, String) -> Unit, // staffId, staffName
    onOpenPdfPreview: (List<StaffPaymentEntity>, String) -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val allPayments by viewModel.staffPayments.collectAsState()
    val staffList by viewModel.staff.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.isAdmin() == true

    var searchQuery by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf("সকল রেকর্ড") }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    var selectedStaffFilter by remember { mutableStateOf<String?>(null) }
    var selectedMethodFilter by remember { mutableStateOf("ALL") }
    var fromDateFilter by remember { mutableStateOf("") }
    var toDateFilter by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var deletingPayment by remember { mutableStateOf<StaffPaymentEntity?>(null) }

    // Helper functions for universal date handling
    fun normalizeToYearMonth(dateStr: String): String {
        return try {
            if (dateStr.contains("-")) {
                val parts = dateStr.split("-")
                if (parts.size >= 2) "${parts[0]}-${parts[1].padStart(2, '0')}" else dateStr
            } else if (dateStr.contains("/")) {
                val parts = dateStr.split("/")
                if (parts.size == 3) "${parts[2]}-${parts[1].padStart(2, '0')}" else dateStr
            } else {
                dateStr.take(7)
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    fun parseAnyDate(dateStr: String): Date? {
        return try {
            if (dateStr.contains("-")) {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
            } else if (dateStr.contains("/")) {
                SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(dateStr)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // Available months list (newest first)
    val availableMonths = remember(allPayments) {
        val fromPayments = allPayments.map { normalizeToYearMonth(it.date) }.filter { it.length == 7 && it.contains("-") }
        val currentM = BanglaNumberFormatter.getCurrentDateFormatted().take(7)
        (fromPayments + currentM).toSortedSet(compareByDescending { it }).toList()
    }

    val filteredPayments = remember(
        allPayments,
        searchQuery,
        selectedMonth,
        selectedStaffFilter,
        selectedMethodFilter,
        fromDateFilter,
        toDateFilter
    ) {
        allPayments.filter { payment ->
            val ym = normalizeToYearMonth(payment.date)
            val matchMonth = if (selectedMonth == "সকল রেকর্ড") true else ym == selectedMonth

            val matchSearch = searchQuery.isBlank() ||
                    payment.staffName.contains(searchQuery, ignoreCase = true) ||
                    payment.note.contains(searchQuery, ignoreCase = true) ||
                    payment.paymentMethod.contains(searchQuery, ignoreCase = true) ||
                    payment.amount.toString().contains(searchQuery) ||
                    payment.date.contains(searchQuery)

            val matchStaff = selectedStaffFilter.isNullOrBlank() ||
                    payment.staffId == selectedStaffFilter ||
                    payment.staffName.equals(selectedStaffFilter, ignoreCase = true)

            val matchMethod = selectedMethodFilter == "ALL" ||
                    payment.paymentMethod.equals(selectedMethodFilter, ignoreCase = true)

            val matchDate = try {
                if (fromDateFilter.isNotBlank() || toDateFilter.isNotBlank()) {
                    val pDate = parseAnyDate(payment.date)
                    val fromD = if (fromDateFilter.isNotBlank()) parseAnyDate(fromDateFilter) else null
                    val toD = if (toDateFilter.isNotBlank()) parseAnyDate(toDateFilter) else null

                    val afterFrom = fromD == null || (pDate != null && !pDate.before(fromD))
                    val beforeTo = toD == null || (pDate != null && !pDate.after(toD))
                    afterFrom && beforeTo
                } else true
            } catch (e: Exception) { true }

            matchMonth && matchSearch && matchStaff && matchMethod && matchDate
        }.sortedByDescending { it.date }
    }

    val totalPaid = remember(filteredPayments) { filteredPayments.sumOf { it.amount } }
    val uniqueStaffCount = remember(filteredPayments) { filteredPayments.map { it.staffName }.distinct().size }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "স্টাফ পেমেন্ট রিপোর্ট",
                isRootScreen = false,
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptics.tap()
                    val reportTitle = if (selectedMonth == "সকল রেকর্ড") "স্টাফ পেমেন্ট রিপোর্ট" else "স্টাফ পেমেন্ট রিপোর্ট (${BanglaNumberFormatter.formatYearMonth(selectedMonth)})"
                    onOpenPdfPreview(filteredPayments, reportTitle)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_export_staff_payments_pdf")
            ) {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF Preview")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
                .testTag("all_staff_payments_screen"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // ══════════════════════════════════════════════════════════════
            // Top Controls: Month Selector & Filter
            // ══════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month Selector Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable { monthMenuExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (selectedMonth == "সকল রেকর্ড") "সকল রেকর্ড" else BanglaNumberFormatter.formatYearMonth(selectedMonth),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = monthMenuExpanded,
                        onDismissRequest = { monthMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("সকল রেকর্ড", fontWeight = if (selectedMonth == "সকল রেকর্ড") FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                selectedMonth = "সকল রেকর্ড"
                                monthMenuExpanded = false
                            }
                        )
                        availableMonths.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(BanglaNumberFormatter.formatYearMonth(m), fontWeight = if (selectedMonth == m) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedMonth = m
                                    monthMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Filter Dialog Trigger Button
                IconButton(
                    onClick = {
                        haptics.tap()
                        showFilterDialog = true
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selectedStaffFilter != null || selectedMethodFilter != "ALL" || fromDateFilter.isNotBlank())
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (selectedStaffFilter != null || selectedMethodFilter != "ALL" || fromDateFilter.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isAdmin) {
                    Button(
                        onClick = {
                            haptics.tap()
                            onNavigateToAddPayment()
                        },
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পেমেন্ট যোগ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("স্টাফের নাম, নোট বা মাধ্যম খুঁজুন...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                singleLine = true
            )

            // ══════════════════════════════════════════════════════════════
            // Bento Grid Summary Cards: Total Staff, Total Payments, Total Paid
            // ══════════════════════════════════════════════════════════════
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card 1: মোট স্টাফ
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Text("মোট স্টাফ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${BanglaNumberFormatter.formatNumber(if (staffList.isNotEmpty()) staffList.size else uniqueStaffCount)} জন",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }

                    // Card 2: মোট পেমেন্ট
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Text("মোট পেমেন্ট", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${BanglaNumberFormatter.formatNumber(filteredPayments.size)} বার",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }

                // Card 3: মোট দেওয়া হয়েছে (Full Width)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                            Text("মোট দেওয়া হয়েছে", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = BanglaNumberFormatter.formatCurrency(totalPaid),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        )
                    }
                }
            }

            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "পেমেন্ট হিস্ট্রি",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = if (selectedMonth == "সকল রেকর্ড") "সকল রেকর্ড" else BanglaNumberFormatter.formatYearMonth(selectedMonth),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // ══════════════════════════════════════════════════════════════
            // Staff Payment Table / Sheet
            // ══════════════════════════════════════════════════════════════
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                if (filteredPayments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedStaffFilter != null) "এই ফিল্টারে কোনো পেমেন্ট পাওয়া যায়নি" else "এখনও কোনো স্টাফ পেমেন্ট রেকর্ড নেই",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (isAdmin && allPayments.isEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = onNavigateToAddPayment,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("স্টাফ পেমেন্ট যোগ করুন")
                                }
                            }
                        }
                    }
                } else {
                    val horizScroll = rememberScrollState()
                    val vertScroll = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(vertScroll)
                            .horizontalScroll(horizScroll)
                    ) {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "স্টাফের নাম",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.width(130.dp)
                            )
                            Text(
                                text = "তারিখ",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.width(90.dp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "পরিমাণ",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.width(105.dp),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "পেমেন্ট মাধ্যম",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.width(100.dp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "নোট",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.width(130.dp)
                            )
                            if (isAdmin) {
                                Text(
                                    text = "অ্যাকশন",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.width(80.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Table Rows
                        filteredPayments.forEachIndexed { index, payment ->
                            val rowBg = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLowest

                            Row(
                                modifier = Modifier
                                    .background(rowBg)
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Clickable Staff Name
                                Text(
                                    text = payment.staffName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable {
                                            haptics.tap()
                                            onNavigateToStaffHistory(payment.staffId, payment.staffName)
                                        }
                                )

                                Text(
                                    text = BanglaNumberFormatter.formatShortDate(payment.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(90.dp),
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = BanglaNumberFormatter.formatCurrency(payment.amount),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0D631B)
                                    ),
                                    modifier = Modifier.width(105.dp),
                                    textAlign = TextAlign.End
                                )

                                Box(
                                    modifier = Modifier.width(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (payment.paymentMethod) {
                                                    "Cash" -> Color(0xFFE8F5E9)
                                                    "Bank" -> Color(0xFFE3F2FD)
                                                    "bKash" -> Color(0xFFFCE4EC)
                                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = payment.paymentMethod,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            ),
                                            color = when (payment.paymentMethod) {
                                                "Cash" -> Color(0xFF2E7D32)
                                                "Bank" -> Color(0xFF1565C0)
                                                "bKash" -> Color(0xFFC2185B)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }

                                Text(
                                    text = payment.note.ifBlank { "—" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(130.dp)
                                )

                                if (isAdmin) {
                                    Row(
                                        modifier = Modifier.width(80.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        IconButton(
                                            onClick = {
                                                haptics.tap()
                                                onNavigateToEditPayment(payment.id)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                haptics.tap()
                                                deletingPayment = payment
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog: Delete Payment Confirmation
    // ══════════════════════════════════════════════════════════════
    deletingPayment?.let { payment ->
        var isDeleting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isDeleting) deletingPayment = null },
            title = {
                Text(
                    text = "পেমেন্ট মুছে ফেলবেন?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "\"${payment.staffName}\"-এর ${BanglaNumberFormatter.formatCurrency(payment.amount)} টাকার পেমেন্ট রেকর্ডটি মুছে ফেলতে চান?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.tap()
                        isDeleting = true
                        viewModel.deleteStaffPayment(
                            id = payment.id,
                            onSuccess = {
                                isDeleting = false
                                deletingPayment = null
                                SnackbarController.showMessage("স্টাফ পেমেন্ট মুছে ফেলা হয়েছে")
                            },
                            onError = { err ->
                                isDeleting = false
                                SnackbarController.showError(err)
                            }
                        )
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(if (isDeleting) "মুছে ফেলা হচ্ছে..." else "মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deletingPayment = null },
                    enabled = !isDeleting
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Filter Modal Dialog
    // ══════════════════════════════════════════════════════════════
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = {
                Text(
                    text = "পেমেন্ট ফিল্টার",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Staff Filter
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "স্টাফ নির্বাচন",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        var staffFilterExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = staffList.find { it.id == selectedStaffFilter || it.name == selectedStaffFilter }?.name ?: (selectedStaffFilter ?: "সব স্টাফ"),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth().clickable { staffFilterExpanded = true },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { staffFilterExpanded = true })

                            DropdownMenu(
                                expanded = staffFilterExpanded,
                                onDismissRequest = { staffFilterExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("সব স্টাফ") },
                                    onClick = {
                                        selectedStaffFilter = null
                                        staffFilterExpanded = false
                                    }
                                )
                                staffList.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name) },
                                        onClick = {
                                            selectedStaffFilter = s.name
                                            staffFilterExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Payment Method Filter
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "পেমেন্ট মাধ্যম",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("ALL" to "সব", "Cash" to "Cash", "Bank" to "Bank", "bKash" to "bKash").forEach { (methodKey, label) ->
                                val isSel = selectedMethodFilter == methodKey
                                OutlinedButton(
                                    onClick = { selectedMethodFilter = methodKey },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Reset Filters Button
                    TextButton(
                        onClick = {
                            selectedStaffFilter = null
                            selectedMethodFilter = "ALL"
                            fromDateFilter = ""
                            toDateFilter = ""
                            selectedMonth = "সকল রেকর্ড"
                            showFilterDialog = false
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("ফিল্টার রিসেট করুন", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFilterDialog = false }) {
                    Text("প্রয়োগ করুন")
                }
            }
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = color
                ),
                maxLines = 1
            )
        }
    }
}

