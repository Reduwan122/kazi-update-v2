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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StaffEntity
import com.example.data.local.StaffPaymentEntity
import com.example.ui.components.AccessDeniedView
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

    // DatePicker Dialog
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
            AccessDeniedView(
                title = "পেমেন্ট ব্যবস্থাপনা সংরক্ষিত",
                message = "শুধুমাত্র এডমিন স্টাফ পেমেন্ট যোগ ও সম্পাদন করতে পারেন।",
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    val liveAmount = amountText.toDoubleOrNull() ?: 0.0

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = if (isEditing) "স্টাফ পেমেন্ট সম্পাদন" else "স্টাফ পেমেন্ট এন্ট্রি",
                isRootScreen = false,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("add_edit_staff_payment_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Staff Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "স্টাফের নাম",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                if (staffList.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "কোনো স্টাফ যুক্ত নেই। সেটিংস থেকে যোগ করুন।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            TextButton(onClick = onNavigateToStaffSettings) {
                                Text("সেটিংস", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedStaff?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("স্টাফ নির্বাচন করুন") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dropdownExpanded = true }
                                .testTag("dropdown_staff_select"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            staffList.forEach { staff ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (staff.designation.isNotBlank()) "${staff.name} (${staff.designation})" else staff.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (selectedStaff?.id == staff.id) FontWeight.Bold else FontWeight.Normal
                                            )
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
            }

            // 2. Date Picker Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "তারিখ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                OutlinedTextField(
                    value = "${BanglaNumberFormatter.formatBanglaDate(selectedDate)} ($selectedDate)",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Select Date",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                        .testTag("field_date"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // 3. Amount Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "কত টাকা দেওয়া হয়েছে",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val cleaned = BanglaNumberFormatter.toEnglishDigits(input)
                        if (cleaned.isEmpty() || cleaned.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = cleaned
                            errorMessage = null
                        }
                    },
                    placeholder = { Text("০.০০") },
                    leadingIcon = {
                        Text(
                            text = "৳",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_staff_payment_amount"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // 4. Live Amount Summary Card
            if (liveAmount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Paid,
                                contentDescription = "Total Amount",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "পরিশোধিত অর্থ (টাকা)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }

                        Text(
                            text = BanglaNumberFormatter.formatCurrency(liveAmount),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 18.sp
                            )
                        )
                    }
                }
            }

            // 5. Payment Method Selection (2x2 Bento Grid)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "পেমেন্ট মাধ্যম",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                val methodList = listOf(
                    StaffPaymentMethodItem("Cash", "ক্যাশ", Icons.Default.Payments),
                    StaffPaymentMethodItem("Bank", "ব্যাংক", Icons.Default.AccountBalance),
                    StaffPaymentMethodItem("bKash", "বিকাশ", Icons.Default.PhoneAndroid),
                    StaffPaymentMethodItem("Other", "অন্যান্য", Icons.Default.Wallet)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StaffPaymentMethodCard(
                            item = methodList[0],
                            isSelected = selectedMethod.equals(methodList[0].key, ignoreCase = true),
                            onClick = {
                                haptics.tap()
                                selectedMethod = methodList[0].key
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StaffPaymentMethodCard(
                            item = methodList[1],
                            isSelected = selectedMethod.equals(methodList[1].key, ignoreCase = true),
                            onClick = {
                                haptics.tap()
                                selectedMethod = methodList[1].key
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StaffPaymentMethodCard(
                            item = methodList[2],
                            isSelected = selectedMethod.equals(methodList[2].key, ignoreCase = true),
                            onClick = {
                                haptics.tap()
                                selectedMethod = methodList[2].key
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StaffPaymentMethodCard(
                            item = methodList[3],
                            isSelected = selectedMethod.equals(methodList[3].key, ignoreCase = true),
                            onClick = {
                                haptics.tap()
                                selectedMethod = methodList[3].key
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6. Note Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "নোট (ঐচ্ছিক)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("পেমেন্টের বিবরণ লিখুন...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_staff_payment_note"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Full-width Save Button
            Button(
                onClick = {
                    val parsed = amountText.toDoubleOrNull()
                    if (selectedStaff == null) {
                        errorMessage = "অনুগ্রহ করে একজন স্টাফ নির্বাচন করুন"
                        return@Button
                    }
                    if (parsed == null || parsed <= 0) {
                        errorMessage = "অনুগ্রহ করে সঠিক টাকার পরিমাণ লিখুন"
                        return@Button
                    }

                    haptics.tap()
                    isSaving = true
                    errorMessage = null

                    val finalStaff = selectedStaff!!
                    val paymentToSave = StaffPaymentEntity(
                        id = existingPayment?.id ?: "",
                        staffId = finalStaff.id,
                        staffName = finalStaff.name,
                        date = selectedDate,
                        amount = parsed,
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
                                SnackbarController.showMessage("স্টাফ পেমেন্ট সফলভাবে আপডেট হয়েছে!")
                                onBack()
                            },
                            onError = { err ->
                                isSaving = false
                                errorMessage = "আপডেট ব্যর্থ: $err"
                                SnackbarController.showError(errorMessage ?: "")
                            }
                        )
                    } else {
                        viewModel.addStaffPayment(
                            payment = paymentToSave,
                            onSuccess = {
                                isSaving = false
                                SnackbarController.showMessage("স্টাফ পেমেন্ট সফলভাবে সংরক্ষিত হয়েছে!")
                                onBack()
                            },
                            onError = { err ->
                                isSaving = false
                                errorMessage = "সংরক্ষণ ব্যর্থ: $err"
                                SnackbarController.showError(errorMessage ?: "")
                            }
                        )
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_staff_payment"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaving) "সংরক্ষণ হচ্ছে..." else if (isEditing) "পেমেন্ট আপডেট করুন" else "পেমেন্ট সংরক্ষণ করুন",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class StaffPaymentMethodItem(
    val key: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun StaffPaymentMethodCard(
    item: StaffPaymentMethodItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                ),
                color = contentColor
            )
        }
    }
}
