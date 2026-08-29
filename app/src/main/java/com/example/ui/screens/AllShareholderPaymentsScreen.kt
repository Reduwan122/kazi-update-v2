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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import com.example.data.local.ShareholderEntity
import com.example.data.local.ShareholderPaymentEntity
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
fun AllShareholderPaymentsScreen(
    viewModel: PoultryViewModel,
    onBack: () -> Unit,
    onNavigateToAddPayment: () -> Unit,
    onNavigateToEditPayment: (String) -> Unit,
    onNavigateToShareholderHistory: (String, String) -> Unit, // shareholderId, shareholderName
    onOpenPdfPreview: (List<ShareholderPaymentEntity>, String) -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val allPayments by viewModel.shareholderPayments.collectAsState()
    val shareholders by viewModel.shareholders.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.isAdmin() == true

    var searchQuery by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf("সকল রেকর্ড") }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    var selectedShareholderFilter by remember { mutableStateOf<String?>(null) }
    var selectedMethodFilter by remember { mutableStateOf("ALL") }
    var fromDateFilter by remember { mutableStateOf("") }
    var toDateFilter by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var deletingPayment by remember { mutableStateOf<ShareholderPaymentEntity?>(null) }

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

    // Filter payments without 4-item limitation
    val filteredPayments = remember(
        allPayments,
        searchQuery,
        selectedMonth,
        selectedShareholderFilter,
        selectedMethodFilter,
        fromDateFilter,
        toDateFilter
    ) {
        allPayments.filter { payment ->
            val ym = normalizeToYearMonth(payment.date)
            val matchMonth = if (selectedMonth == "সকল রেকর্ড") true else ym == selectedMonth

            val matchSearch = searchQuery.isBlank() ||
                    payment.shareholderName.contains(searchQuery, ignoreCase = true) ||
                    payment.note.contains(searchQuery, ignoreCase = true) ||
                    payment.paymentMethod.contains(searchQuery, ignoreCase = true) ||
                    payment.amount.toString().contains(searchQuery) ||
                    payment.date.contains(searchQuery)

            val matchShareholder = selectedShareholderFilter.isNullOrBlank() ||
                    payment.shareholderId == selectedShareholderFilter ||
                    payment.shareholderName.equals(selectedShareholderFilter, ignoreCase = true)

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
            } catch (e: Exception) {
                true
            }

            matchMonth && matchSearch && matchShareholder && matchMethod && matchDate
        }
    }

    val isFilterActive = !selectedShareholderFilter.isNullOrBlank() ||
            selectedMethodFilter != "ALL" ||
            fromDateFilter.isNotBlank() ||
            toDateFilter.isNotBlank() ||
            selectedMonth != "সকল রেকর্ড" ||
            searchQuery.isNotBlank()

    // Summary calculations
    val totalShareholdersCount = remember(shareholders, allPayments, filteredPayments) {
        val uniqueInFilter = filteredPayments.map { it.shareholderName }.filter { it.isNotBlank() }.distinct().size
        if (selectedMonth == "সকল রেকর্ড" && !isFilterActive) {
            maxOf(shareholders.size, uniqueInFilter)
        } else {
            uniqueInFilter
        }
    }
    val totalPaymentsCount = filteredPayments.size
    val totalAmountPaid = filteredPayments.sumOf { it.amount }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "সকল শেয়ারহোল্ডার পেমেন্ট",
                isRootScreen = false,
                onBackClick = onBack,
                actions = {
                    if (isAdmin) {
                        IconButton(
                            onClick = {
                                haptics.tap()
                                onNavigateToAddPayment()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "পেমেন্ট যোগ করুন",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            haptics.tap()
                            showFilterDialog = true
                        }
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isFilterActive) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (filteredPayments.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        haptics.tap()
                        val reportTitle = if (selectedMonth != "সকল রেকর্ড") {
                            "${BanglaNumberFormatter.formatYearMonth(selectedMonth)}-এর শেয়ারহোল্ডার পেমেন্ট রিপোর্ট"
                        } else {
                            "সকল শেয়ারহোল্ডার পেমেন্ট রিপোর্ট"
                        }
                        onOpenPdfPreview(filteredPayments, reportTitle)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_shareholder_pdf")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF সংরক্ষণ করুন"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("all_shareholder_payments_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ══════════════════════════════════════════════════════════════
            // Top Summary Section (Bento Grid matching Stitch design)
            // ══════════════════════════════════════════════════════════════
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 1: Total Shareholders
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "মোট শেয়ারহোল্ডার",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${BanglaNumberFormatter.formatNumber(totalShareholdersCount)} জন",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Card 2: Total Payments Count
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "মোট পেমেন্ট",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${BanglaNumberFormatter.formatNumber(totalPaymentsCount)} বার",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Card 3: Total Amount Paid (Full Width Banner)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "মোট দেওয়া হয়েছে",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                        }
                        Text(
                            text = BanglaNumberFormatter.formatCurrency(totalAmountPaid),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // Search & Month Selector Filter Row
            // ══════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("শেয়ারহোল্ডার বা বিবরণ খুঁজুন...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("shareholder_search_field")
                )

                // Month Dropdown
                Box {
                    OutlinedButton(
                        onClick = { monthMenuExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = if (selectedMonth == "সকল রেকর্ড") "সকল রেকর্ড" else BanglaNumberFormatter.formatYearMonth(selectedMonth),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Month",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = monthMenuExpanded,
                        onDismissRequest = { monthMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("সকল রেকর্ড") },
                            onClick = {
                                selectedMonth = "সকল রেকর্ড"
                                monthMenuExpanded = false
                            }
                        )
                        availableMonths.forEach { monthStr ->
                            DropdownMenuItem(
                                text = { Text(BanglaNumberFormatter.formatYearMonth(monthStr)) },
                                onClick = {
                                    selectedMonth = monthStr
                                    monthMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // Active Filter Badge & Header
            // ══════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedMonth != "সকল রেকর্ড") "${BanglaNumberFormatter.formatYearMonth(selectedMonth)}-এর পেমেন্ট তালিকা" else "পেমেন্ট তালিকা",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isFilterActive) {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            selectedMonth = "সকল রেকর্ড"
                            selectedShareholderFilter = null
                            selectedMethodFilter = "ALL"
                            fromDateFilter = ""
                            toDateFilter = ""
                        }
                    ) {
                        Text("ফিল্টার পরিষ্কার করুন", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // Payment Records Table / Sheet View (Matching Stitch design)
            // ══════════════════════════════════════════════════════════════
            if (filteredPayments.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isFilterActive) "এই ফিল্টারে কোনো পেমেন্ট পাওয়া যায়নি" else "এখনও কোনো পেমেন্ট রেকর্ড নেই",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (isFilterActive) {
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedMonth = "সকল রেকর্ড"
                                    selectedShareholderFilter = null
                                    selectedMethodFilter = "ALL"
                                    fromDateFilter = ""
                                    toDateFilter = ""
                                }
                            ) {
                                Text("ফিল্টার পরিষ্কার করুন")
                            }
                        } else if (isAdmin) {
                            Button(
                                onClick = onNavigateToAddPayment,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("পেমেন্ট যোগ করুন")
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    val hScrollState = rememberScrollState()
                    Box(modifier = Modifier.fillMaxWidth().horizontalScroll(hScrollState)) {
                        Column(modifier = Modifier.width(680.dp)) {
                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("শেয়ারহোল্ডার", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("তারিখ", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("পরিমাণ (৳)", modifier = Modifier.weight(1.4f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("মাধ্যম", modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("নোট", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (isAdmin) {
                                    Text("অ্যাকশন", modifier = Modifier.weight(1.0f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Table Rows
                            filteredPayments.forEachIndexed { index, payment ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (index % 2 == 1) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f) else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Shareholder Name (CLICKABLE -> Navigates to individual history)
                                    Text(
                                        text = payment.shareholderName,
                                        modifier = Modifier
                                            .weight(1.8f)
                                            .clickable {
                                                haptics.tap()
                                                onNavigateToShareholderHistory(payment.shareholderId, payment.shareholderName)
                                            },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    // 2. Date
                                    val formattedDateDisplay = if (payment.date.contains("-")) {
                                        BanglaNumberFormatter.formatShortDate(payment.date)
                                    } else {
                                        BanglaNumberFormatter.toBanglaDigits(payment.date)
                                    }
                                    Text(
                                        text = formattedDateDisplay,
                                        modifier = Modifier.weight(1.2f),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // 3. Amount
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(payment.amount),
                                        modifier = Modifier.weight(1.4f),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // 4. Method Chip
                                    val methodLabel = when (payment.paymentMethod.lowercase()) {
                                        "cash" -> "ক্যাশ"
                                        "bank" -> "ব্যাংক"
                                        "bkash" -> "বিকাশ"
                                        else -> payment.paymentMethod
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = methodLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // 5. Note
                                    Text(
                                        text = payment.note.ifBlank { "—" },
                                        modifier = Modifier.weight(1.5f),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )

                                    // 6. Admin Actions (Edit/Delete)
                                    if (isAdmin) {
                                        Row(
                                            modifier = Modifier.weight(1.0f),
                                            horizontalArrangement = Arrangement.End
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

                                if (index < filteredPayments.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Filter Modal Dialog / Sheet
    // ══════════════════════════════════════════════════════════════
    if (showFilterDialog) {
        var tempShareholder by remember { mutableStateOf(selectedShareholderFilter) }
        var tempMethod by remember { mutableStateOf(selectedMethodFilter) }
        var tempFromDate by remember { mutableStateOf(fromDateFilter) }
        var tempToDate by remember { mutableStateOf(toDateFilter) }
        var shDropdownOpen by remember { mutableStateOf(false) }

        val cal = Calendar.getInstance()
        val fromPicker = DatePickerDialog(
            context,
            { _, year, month, day ->
                tempFromDate = String.format(Locale.US, "%02d/%02d/%04d", day, month + 1, year)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )
        val toPicker = DatePickerDialog(
            context,
            { _, year, month, day ->
                tempToDate = String.format(Locale.US, "%02d/%02d/%04d", day, month + 1, year)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )

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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Shareholder Filter
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("শেয়ারহোল্ডার", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val shName = if (tempShareholder.isNullOrBlank()) "সব শেয়ারহোল্ডার"
                            else (shareholders.find { it.id == tempShareholder || it.name == tempShareholder }?.name ?: tempShareholder ?: "সব শেয়ারহোল্ডার")

                            OutlinedTextField(
                                value = shName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            Box(modifier = Modifier.matchParentSize().clickable { shDropdownOpen = true })

                            DropdownMenu(
                                expanded = shDropdownOpen,
                                onDismissRequest = { shDropdownOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("সব শেয়ারহোল্ডার") },
                                    onClick = {
                                        tempShareholder = null
                                        shDropdownOpen = false
                                    }
                                )
                                shareholders.forEach { sh ->
                                    DropdownMenuItem(
                                        text = { Text(sh.name) },
                                        onClick = {
                                            tempShareholder = sh.name
                                            shDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Date Range Filter
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("তারিখের সীমা", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { fromPicker.show() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(tempFromDate.ifBlank { "শুরুর তারিখ" }, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { toPicker.show() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(tempToDate.ifBlank { "শেষ তারিখ" }, fontSize = 12.sp)
                            }
                        }
                    }

                    // Payment Method Filter
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("পেমেন্ট মাধ্যম", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val methods = listOf(
                            "ALL" to "সব মাধ্যম",
                            "Cash" to "ক্যাশ",
                            "Bank" to "ব্যাংক",
                            "bKash" to "বিকাশ",
                            "Other" to "অন্যান্য"
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            methods.forEach { (mKey, mLabel) ->
                                val isSel = tempMethod.equals(mKey, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable { tempMethod = mKey }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = mLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedShareholderFilter = tempShareholder
                        selectedMethodFilter = tempMethod
                        fromDateFilter = tempFromDate
                        toDateFilter = tempToDate
                        showFilterDialog = false
                    }
                ) {
                    Text("প্রয়োগ করুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedShareholderFilter = null
                        selectedMethodFilter = "ALL"
                        fromDateFilter = ""
                        toDateFilter = ""
                        showFilterDialog = false
                    }
                ) {
                    Text("রিসেট")
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Delete Payment Confirmation Dialog
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
                    text = "এই পেমেন্ট রেকর্ডটি (${payment.shareholderName} - ${BanglaNumberFormatter.formatCurrency(payment.amount)}) মুছে ফেললে তা আর তালিকায় থাকবে না।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.tap()
                        isDeleting = true
                        viewModel.deleteShareholderPayment(
                            id = payment.id,
                            onSuccess = {
                                isDeleting = false
                                deletingPayment = null
                            },
                            onError = {
                                isDeleting = false
                            }
                        )
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
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
}

