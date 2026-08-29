package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShareholderPaymentEntity
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.SnackbarController
import com.example.ui.components.rememberHaptics
import com.example.ui.viewmodel.PoultryViewModel

@Composable
fun IndividualShareholderHistoryScreen(
    viewModel: PoultryViewModel,
    shareholderIdentifier: String, // ID or Name
    shareholderNameParam: String = "",
    onBack: () -> Unit,
    onNavigateToAddPayment: () -> Unit,
    onNavigateToEditPayment: (String) -> Unit,
    onOpenPdfPreview: (List<ShareholderPaymentEntity>, String) -> Unit
) {
    val haptics = rememberHaptics()
    val allPayments by viewModel.shareholderPayments.collectAsState()
    val shareholders by viewModel.shareholders.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.isAdmin() == true

    var searchQuery by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf("সকল রেকর্ড") }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var deletingPayment by remember { mutableStateOf<ShareholderPaymentEntity?>(null) }

    // Safely decode parameters
    val decodedName = remember(shareholderNameParam) {
        try { android.net.Uri.decode(shareholderNameParam).trim() } catch (e: Exception) { shareholderNameParam.trim() }
    }
    val decodedId = remember(shareholderIdentifier) {
        try { android.net.Uri.decode(shareholderIdentifier).trim() } catch (e: Exception) { shareholderIdentifier.trim() }
    }

    // Resolve shareholder name
    val resolvedName = remember(decodedId, decodedName, shareholders, allPayments) {
        if (decodedName.isNotBlank() && decodedName != "none" && decodedName != "name") decodedName
        else {
            val fromList = shareholders.find { it.id == decodedId }?.name
            val fromPayments = allPayments.find { it.shareholderId == decodedId || it.shareholderName.equals(decodedId, ignoreCase = true) }?.shareholderName
            fromList ?: fromPayments ?: decodedId
        }
    }

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

    // Base filter: All payments strictly for this shareholder
    val baseShareholderPayments = remember(allPayments, decodedId, resolvedName) {
        allPayments.filter { p ->
            (decodedId.isNotBlank() && decodedId != "none" && decodedId != "name" && p.shareholderId == decodedId) ||
            (resolvedName.isNotBlank() && p.shareholderName.trim().equals(resolvedName, ignoreCase = true)) ||
            (decodedId.isNotBlank() && p.shareholderName.trim().equals(decodedId, ignoreCase = true))
        }
    }

    // Available months list for this specific shareholder
    val availableMonths = remember(baseShareholderPayments) {
        val fromPayments = baseShareholderPayments.map { normalizeToYearMonth(it.date) }.filter { it.length == 7 && it.contains("-") }
        val currentM = BanglaNumberFormatter.getCurrentDateFormatted().take(7)
        (fromPayments + currentM).toSortedSet(compareByDescending { it }).toList()
    }

    // Search and Month Filtered payments
    val filteredPayments = remember(baseShareholderPayments, searchQuery, selectedMonth) {
        baseShareholderPayments.filter { payment ->
            val ym = normalizeToYearMonth(payment.date)
            val matchMonth = if (selectedMonth == "সকল রেকর্ড") true else ym == selectedMonth

            val matchSearch = searchQuery.isBlank() ||
                    payment.note.contains(searchQuery, ignoreCase = true) ||
                    payment.paymentMethod.contains(searchQuery, ignoreCase = true) ||
                    payment.amount.toString().contains(searchQuery) ||
                    payment.date.contains(searchQuery)

            matchMonth && matchSearch
        }
    }

    val isFilterActive = selectedMonth != "সকল রেকর্ড" || searchQuery.isNotBlank()

    val totalCount = filteredPayments.size
    val totalAmount = filteredPayments.sumOf { it.amount }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "$resolvedName-এর পেমেন্ট হিস্টোরি",
                isRootScreen = false,
                onBackClick = onBack
            )
        },
        bottomBar = {
            if (filteredPayments.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = {
                                haptics.tap()
                                onOpenPdfPreview(filteredPayments, "$resolvedName-এর পেমেন্ট হিস্টোরি")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_individual_pdf"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PDF সংরক্ষণ করুন",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
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
                .testTag("individual_shareholder_history_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ══════════════════════════════════════════════════════════════
            // Top Summary Section (Bento Grid matching Stitch _3 design)
            // ══════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Total Payments Count
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
                                imageVector = Icons.Default.ReceiptLong,
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
                            text = "${BanglaNumberFormatter.formatNumber(totalCount)} বার",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Card 2: Total Amount Paid
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "মোট দেওয়া হয়েছে",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                        }
                        Text(
                            text = BanglaNumberFormatter.formatCurrency(totalAmount),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                    placeholder = { Text("বিবরণ খুঁজুন...", fontSize = 13.sp) },
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
            // Detailed Chronological Payment List / Table
            // ══════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedMonth != "সকল রেকর্ড") "${BanglaNumberFormatter.formatYearMonth(selectedMonth)}-এর লেনদেন" else "বিস্তারিত লেনদেন",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isFilterActive) {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            selectedMonth = "সকল রেকর্ড"
                        }
                    ) {
                        Text("ফিল্টার পরিষ্কার করুন", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

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
                            text = if (isFilterActive) "এই ফিল্টারে কোনো পেমেন্ট রেকর্ড নেই" else "এই শেয়ারহোল্ডারের কোনো পেমেন্ট রেকর্ড নেই",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (isFilterActive) {
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedMonth = "সকল রেকর্ড"
                                }
                            ) {
                                Text("ফিল্টার পরিষ্কার করুন")
                            }
                        } else {
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
                        Column(modifier = Modifier.width(580.dp)) {
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("তারিখ", modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("পরিমাণ (৳)", modifier = Modifier.weight(1.5f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("মাধ্যম", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("নোট", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (isAdmin) {
                                    Text("অ্যাকশন", modifier = Modifier.weight(1.1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Rows
                            filteredPayments.forEachIndexed { index, payment ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (index % 2 == 1) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f) else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Date
                                    val formattedDateDisplay = if (payment.date.contains("-")) {
                                        BanglaNumberFormatter.formatShortDate(payment.date)
                                    } else {
                                        BanglaNumberFormatter.toBanglaDigits(payment.date)
                                    }
                                    Text(
                                        text = formattedDateDisplay,
                                        modifier = Modifier.weight(1.4f),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Amount
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(payment.amount),
                                        modifier = Modifier.weight(1.5f),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    // Method
                                    val methodLabel = when (payment.paymentMethod.lowercase()) {
                                        "cash" -> "ক্যাশ"
                                        "bank" -> "ব্যাংক"
                                        "bkash" -> "বিকাশ"
                                        else -> payment.paymentMethod
                                    }
                                    Box(
                                        modifier = Modifier.weight(1.2f),
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

                                    // Note
                                    Text(
                                        text = payment.note.ifBlank { "—" },
                                        modifier = Modifier.weight(1.8f),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )

                                    // Admin Actions
                                    if (isAdmin) {
                                        Row(
                                            modifier = Modifier.weight(1.1f),
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

            Spacer(modifier = Modifier.height(80.dp))
        }
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
                    text = "এই পেমেন্ট রেকর্ডটি (${BanglaNumberFormatter.formatCurrency(payment.amount)} - ${payment.date}) মুছে ফেললে তা আর তালিকায় থাকবে না।",
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
                                SnackbarController.showMessage("পেমেন্ট সফলভাবে মুছে ফেলা হয়েছে!")
                            },
                            onError = { err ->
                                isDeleting = false
                                SnackbarController.showError(err)
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

