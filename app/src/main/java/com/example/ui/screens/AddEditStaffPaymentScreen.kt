package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
fun AddEditStaffPaymentScreen(
    viewModel: PoultryViewModel,
    paymentId: String? = null,
    onBack: () -> Unit,
    onNavigateToStaffSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val staffList by viewModel.staff.collectAsState()
    val allPayments by viewModel.staffPayments.collectAsState()

    val isEditing = !paymentId.isNullOrBlank()
    val existingPayment = remember(paymentId, allPayments) {
        if (isEditing) allPayments.find { it.id == paymentId } else null
    }

    var selectedStaff by remember(existingPayment, staffList) {
        mutableStateOf(
            if (existingPayment != null) {
                staffList.find { it.id == existingPayment.staffId || it.name == existingPayment.staffName }
            } else null
        )
    }

    var selectedDate by remember(existingPayment) {
        val rawDate = existingPayment?.date?.ifBlank { null }
        val normalized = if (rawDate != null && rawDate.contains("/")) {
            try {
                val parts = rawDate.split("/")
                if (parts.size == 3) "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"
                else rawDate
            } catch (e: Exception) { rawDate }
        } else rawDate
        mutableStateOf(normalized ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }

    var amountText by remember(existingPayment) {
        mutableStateOf(
            if (existingPayment != null && existingPayment.amount > 0) {
                if (existingPayment.amount % 1.0 == 0.0) existingPayment.amount.toLong().toString()
                else existingPayment.amount.toString()
            } else ""
        )
    }

    var selectedMethod by remember(existingPayment) {
        mutableStateOf(existingPayment?.paymentMethod ?: "Cash")
    }

    var noteText by remember(existingPayment) {
        mutableStateOf(existingPayment?.note ?: "")
    }

    var isSaving by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // DatePicker
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            selectedDate = formatted
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.isAdmin() == true

    if (!isAdmin) {
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = "স্টাফ পেমেন্ট",
                    isRootScreen = false,
                    onBackClick = onBack
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "অননুমোদিত প্রবেশাধিকার",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "শুধুমাত্র এডমিন স্টাফ পেমেন্ট যোগ ও সম্পাদন করতে পারেন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onBack, shape = RoundedCornerShape(8.dp)) {
                            Text("ফিরে যান")
                        }
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = if (isEditing) "স্টাফ পেমেন্ট পরিবর্তন করুন" else "স্টাফ পেমেন্ট যোগ করুন",
                isRootScreen = false,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("add_edit_staff_payment_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Staff Selection Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "স্টাফের নাম",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedStaff?.name ?: (existingPayment?.staffName ?: ""),
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("স্টাফ নির্বাচন করুন") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Staff",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dropdownExpanded = true }
                                    .testTag("input_select_staff"),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            // Invisible full clickable overlay
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { dropdownExpanded = true }
                            )

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                if (staffList.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("কোনো স্টাফ নেই। সেটিংস থেকে যোগ করুন") },
                                        onClick = {
                                            dropdownExpanded = false
                                            onNavigateToStaffSettings()
                                        }
                                    )
                                } else {
                                    staffList.forEach { staff ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = staff.name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (staff.phone.isNotBlank()) {
                                                        Text(
                                                            text = BanglaNumberFormatter.toBanglaDigits(staff.phone),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            onClick = {
                                                selectedStaff = staff
                                                dropdownExpanded = false
                                                errorMessage = null
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (staffList.isEmpty()) {
                            TextButton(
                                onClick = onNavigateToStaffSettings,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "+ নতুন স্টাফ যোগ করুন",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // 2. Date Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "তারিখ",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = BanglaNumberFormatter.formatBanglaDate(selectedDate),
                                onValueChange = {},
                                readOnly = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_staff_payment_date"),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { datePickerDialog.show() }
                            )
                        }
                    }

                    // 3. Amount Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "কত টাকা দেওয়া হয়েছে",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                val banglaConverted = BanglaNumberFormatter.convertBanglaToEnglishDigits(it)
                                if (banglaConverted.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    amountText = banglaConverted
                                    errorMessage = null
                                }
                            },
                            placeholder = { Text("৳ Amount (যেমন: ২০০০০)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_staff_payment_amount")
                        )
                    }

                    // 4. Payment Method Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "পেমেন্ট মাধ্যম",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PaymentMethodChip(
                                label = "Cash",
                                icon = Icons.Default.Wallet,
                                isSelected = selectedMethod == "Cash",
                                onClick = { selectedMethod = "Cash" },
                                modifier = Modifier.weight(1f)
                            )
                            PaymentMethodChip(
                                label = "Bank",
                                icon = Icons.Default.AccountBalance,
                                isSelected = selectedMethod == "Bank",
                                onClick = { selectedMethod = "Bank" },
                                modifier = Modifier.weight(1f)
                            )
                            PaymentMethodChip(
                                label = "bKash",
                                icon = Icons.Default.PhoneAndroid,
                                isSelected = selectedMethod == "bKash",
                                onClick = { selectedMethod = "bKash" },
                                modifier = Modifier.weight(1f)
                            )
                            PaymentMethodChip(
                                label = "Other",
                                icon = Icons.Default.Payments,
                                isSelected = selectedMethod == "Other",
                                onClick = { selectedMethod = "Other" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 5. Note Field (Optional)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "নোট (ঐচ্ছিক)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text("পেমেন্ট সংক্রান্ত কোনো বিবরণ থাকলে লিখুন") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .testTag("input_staff_payment_note"),
                            maxLines = 3
                        )
                    }

                    // Error Message display
                    errorMessage?.let { err ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    haptics.tap()

                    val finalStaff = selectedStaff ?: (
                        if (existingPayment != null) StaffEntity(id = existingPayment.staffId, name = existingPayment.staffName)
                        else null
                    )

                    if (finalStaff == null || finalStaff.name.isBlank()) {
                        errorMessage = "স্টাফের নাম নির্বাচন করুন"
                        SnackbarController.showError("স্টাফের নাম নির্বাচন করুন")
                        return@Button
                    }

                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0.0) {
                        errorMessage = "সঠিক টাকার পরিমাণ দিন"
                        SnackbarController.showError("সঠিক টাকার পরিমাণ দিন")
                        return@Button
                    }

                    isSaving = true
                    val paymentToSave = StaffPaymentEntity(
                        id = existingPayment?.id ?: "",
                        staffId = finalStaff.id,
                        staffName = finalStaff.name,
                        date = selectedDate,
                        amount = amount,
                        paymentMethod = selectedMethod,
                        note = noteText.trim(),
                        createdAt = existingPayment?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    if (isEditing) {
                        viewModel.updateStaffPayment(
                            payment = paymentToSave,
                            onSuccess = {
                                isSaving = false
                                SnackbarController.showMessage("স্টাফ পেমেন্ট সফলভাবে পরিবর্তন করা হয়েছে")
                                onBack()
                            },
                            onError = { err ->
                                isSaving = false
                                errorMessage = err
                                SnackbarController.showError(err)
                            }
                        )
                    } else {
                        viewModel.addStaffPayment(
                            payment = paymentToSave,
                            onSuccess = {
                                isSaving = false
                                SnackbarController.showMessage("স্টাফ পেমেন্ট সফলভাবে সংরক্ষণ করা হয়েছে")
                                onBack()
                            },
                            onError = { err ->
                                isSaving = false
                                errorMessage = err
                                SnackbarController.showError(err)
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_staff_payment"),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaving) "সংরক্ষণ হচ্ছে..." else "পেমেন্ট সংরক্ষণ করুন",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable {
                haptics.tap()
                onClick()
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                ),
                color = contentColor
            )
        }
    }
}
