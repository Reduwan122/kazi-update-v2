package com.example.ui.screens

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
import androidx.compose.material.icons.filled.PictureAsPdf
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
import com.example.data.local.StaffPaymentEntity
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.SnackbarController
import com.example.ui.components.rememberHaptics
import com.example.ui.viewmodel.PoultryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IndividualStaffHistoryScreen(
    viewModel: PoultryViewModel,
    staffIdentifier: String, // ID or Name
    staffNameParam: String = "",
    onBack: () -> Unit,
    onNavigateToAddPayment: () -> Unit,
    onNavigateToEditPayment: (String) -> Unit,
    onOpenPdfPreview: (List<StaffPaymentEntity>, String) -> Unit
) {
    val haptics = rememberHaptics()
    val allPayments by viewModel.staffPayments.collectAsState()
    val staffList by viewModel.staff.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.isAdmin() == true

    var searchQuery by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf("সকল রেকর্ড") }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var deletingPayment by remember { mutableStateOf<StaffPaymentEntity?>(null) }

    // Safely decode parameters
    val decodedName = remember(staffNameParam) {
        try { android.net.Uri.decode(staffNameParam).trim() } catch (e: Exception) { staffNameParam.trim() }
    }
    val decodedId = remember(staffIdentifier) {
        try { android.net.Uri.decode(staffIdentifier).trim() } catch (e: Exception) { staffIdentifier.trim() }
    }

    // Resolve staff name
    val resolvedName = remember(decodedId, decodedName, staffList, allPayments) {
        if (decodedName.isNotBlank() && decodedName != "none" && decodedName != "name") decodedName
        else {
            val fromList = staffList.find { it.id == decodedId }?.name
            val fromPayments = allPayments.find { it.staffId == decodedId || it.staffName.equals(decodedId, ignoreCase = true) }?.staffName
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

    // Base filter: All payments strictly for this staff
    val baseStaffPayments = remember(allPayments, decodedId, resolvedName) {
        allPayments.filter { p ->
            (decodedId.isNotBlank() && decodedId != "none" && decodedId != "name" && p.staffId == decodedId) ||
            (resolvedName.isNotBlank() && p.staffName.trim().equals(resolvedName, ignoreCase = true)) ||
            (decodedId.isNotBlank() && p.staffName.trim().equals(decodedId, ignoreCase = true))
        }
    }

    // Available months list for this specific staff
    val availableMonths = remember(baseStaffPayments) {
        val fromPayments = baseStaffPayments.map { normalizeToYearMonth(it.date) }.filter { it.length == 7 && it.contains("-") }
        val currentM = BanglaNumberFormatter.getCurrentDateFormatted().take(7)
        (fromPayments + currentM).toSortedSet(compareByDescending { it }).toList()
    }

    // Search and Month Filtered payments
    val filteredPayments = remember(baseStaffPayments, searchQuery, selectedMonth) {
        baseStaffPayments.filter { payment ->
            val ym = normalizeToYearMonth(payment.date)
            val matchMonth = if (selectedMonth == "সকল রেকর্ড") true else ym == selectedMonth

            val matchSearch = searchQuery.isBlank() ||
                    payment.note.contains(searchQuery, ignoreCase = true) ||
                    payment.paymentMethod.contains(searchQuery, ignoreCase = true) ||
                    payment.amount.toString().contains(searchQuery) ||
                    payment.date.contains(searchQuery)

            matchMonth && matchSearch
        }.sortedByDescending { it.date }
    }

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
                                val reportTitle = if (selectedMonth != "সকল রেকর্ড") {
                                    "$resolvedName-এর পেমেন্ট হিস্টোরি (${BanglaNumberFormatter.formatYearMonth(selectedMonth)})"
                                } else {
                                    "$resolvedName-এর পেমেন্ট হিস্টোরি"
                                }
                                onOpenPdfPreview(filteredPayments, reportTitle)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_staff_individual_pdf"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
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
                .padding(horizontal = 14.dp)
                .testTag("individual_staff_history_screen"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // ══════════════════════════════════════════════════════════════
            // Top Controls: Month Selector & Search
            // ══════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month Selector
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
                placeholder = { Text("নোট, মাধ্যম বা তারিখ খুঁজুন...", fontSize = 13.sp) },
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
            // Summary Cards: Total Payments, Total Paid
            // ══════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(14.dp))
                            Text("মোট পেমেন্ট", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${BanglaNumberFormatter.formatNumber(totalCount)} বার",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF0D631B), modifier = Modifier.size(14.dp))
                            Text("মোট দেওয়া হয়েছে", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = BanglaNumberFormatter.formatCurrency(totalAmount),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0D631B))
                        )
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // History Table
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
                                text = if (searchQuery.isNotBlank() || selectedMonth != "সকল রেকর্ড") "এই ফিল্টারে কোনো পেমেন্ট হিস্টোরি পাওয়া যায়নি" else "এই স্টাফের কোনো পেমেন্ট হিস্টোরি নেই",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
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
                        // Header
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                modifier = Modifier.width(110.dp),
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
                                modifier = Modifier.width(140.dp)
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

                        filteredPayments.forEachIndexed { index, payment ->
                            val rowBg = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLowest

                            Row(
                                modifier = Modifier
                                    .background(rowBg)
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                    modifier = Modifier.width(110.dp),
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
                                    modifier = Modifier.width(140.dp)
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
                    text = "${BanglaNumberFormatter.formatCurrency(payment.amount)} টাকার এই পেমেন্ট রেকর্ডটি মুছে ফেলতে চান?",
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
}
