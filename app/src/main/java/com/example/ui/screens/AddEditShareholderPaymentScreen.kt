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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
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
fun AddEditShareholderPaymentScreen(
    viewModel: PoultryViewModel,
    paymentId: String? = null,
    onBack: () -> Unit,
    onNavigateToShareholderSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val shareholders by viewModel.shareholders.collectAsState()
    val allPayments by viewModel.shareholderPayments.collectAsState()

    val isEditing = !paymentId.isNullOrBlank()
    val existingPayment = remember(paymentId, allPayments) {
        if (isEditing) allPayments.find { it.id == paymentId } else null
    }

    var selectedShareholder by remember(existingPayment, shareholders) {
        mutableStateOf(
            if (existingPayment != null) {
                shareholders.find { it.id == existingPayment.shareholderId || it.name == existingPayment.shareholderName }
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

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = if (isEditing) "পেমেন্ট পরিবর্তন করুন" else "পেমেন্ট যোগ করুন",
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
                .testTag("add_edit_shareholder_payment_screen"),
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
                    // 1. Shareholder Selection Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "শেয়ারহোল্ডারের নাম",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (shareholders.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
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
                                            text = "কোনো শেয়ারহোল্ডার যুক্ত নেই। সেটিংস থেকে যোগ করুন।",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    TextButton(onClick = onNavigateToShareholderSettings) {
                                        Text("সেটিংস", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedShareholder?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = { Text("শেয়ারহোল্ডার নির্বাচন করুন") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { dropdownExpanded = true }
                                        .testTag("dropdown_shareholder_select"),
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { dropdownExpanded = true }
                                )

                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    shareholders.forEach { sh ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = sh.name,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = if (selectedShareholder?.id == sh.id) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                            },
                                            onClick = {
                                                selectedShareholder = sh
                                                dropdownExpanded = false
                                                errorMessage = null
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Date Field
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
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Select Date",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
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
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    amountText = input
                                    errorMessage = null
                                }
                            },
                            placeholder = { Text("0.00") },
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_payment_amount")
                        )
                    }

                    // 4. Payment Method Selection (Stitch 2x2 Grid design)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "পেমেন্ট মাধ্যম",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val methods = listOf(
                            PaymentMethodItem("Cash", "ক্যাশ", Icons.Default.Payments),
                            PaymentMethodItem("Bank", "ব্যাংক", Icons.Default.AccountBalance),
                            PaymentMethodItem("bKash", "বিকাশ", Icons.Default.PhoneAndroid),
                            PaymentMethodItem("Other", "অন্যান্য", Icons.Default.Wallet)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PaymentMethodCard(
                                    item = methods[0],
                                    isSelected = selectedMethod.equals(methods[0].key, ignoreCase = true),
                                    onClick = { selectedMethod = methods[0].key },
                                    modifier = Modifier.weight(1f)
                                )
                                PaymentMethodCard(
                                    item = methods[1],
                                    isSelected = selectedMethod.equals(methods[1].key, ignoreCase = true),
                                    onClick = { selectedMethod = methods[1].key },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PaymentMethodCard(
                                    item = methods[2],
                                    isSelected = selectedMethod.equals(methods[2].key, ignoreCase = true),
                                    onClick = { selectedMethod = methods[2].key },
                                    modifier = Modifier.weight(1f)
                                )
                                PaymentMethodCard(
                                    item = methods[3],
                                    isSelected = selectedMethod.equals(methods[3].key, ignoreCase = true),
                                    onClick = { selectedMethod = methods[3].key },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 5. Note Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "নোট (ঐচ্ছিক)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text("প্রয়োজনে নোট লিখুন") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_payment_note")
                        )
                    }

                    // Validation Error Message
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Bottom Save Button
            Button(
                onClick = {
                    haptics.tap()
                    val shareholder = selectedShareholder
                    if (shareholder == null) {
                        errorMessage = "শেয়ারহোল্ডারের নাম নির্বাচন করুন"
                        return@Button
                    }
                    val amountVal = amountText.toDoubleOrNull()
                    if (amountVal == null || amountVal <= 0) {
                        errorMessage = "সঠিক টাকার পরিমাণ দিন"
                        return@Button
                    }
                    if (selectedDate.isBlank()) {
                        errorMessage = "তারিখ নির্বাচন করুন"
                        return@Button
                    }

                    isSaving = true
                    val paymentRecord = ShareholderPaymentEntity(
                        id = existingPayment?.id ?: "",
                        shareholderId = shareholder.id,
                        shareholderName = shareholder.name,
                        date = selectedDate,
                        amount = amountVal,
                        paymentMethod = selectedMethod,
                        note = noteText.trim(),
                        createdAt = existingPayment?.createdAt ?: System.currentTimeMillis()
                    )

                    if (isEditing) {
                        viewModel.updateShareholderPayment(
                            payment = paymentRecord,
                            onSuccess = {
                                isSaving = false
                                SnackbarController.showMessage("পেমেন্ট সফলভাবে পরিবর্তন করা হয়েছে!")
                                onBack()
                            },
                            onError = { err ->
                                isSaving = false
                                errorMessage = err
                                SnackbarController.showError(err)
                            }
                        )
                    } else {
                        viewModel.addShareholderPayment(
                            payment = paymentRecord,
                            onSuccess = {
                                isSaving = false
                                SnackbarController.showMessage("পেমেন্ট সফলভাবে সংরক্ষণ করা হয়েছে!")
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
                enabled = !isSaving && (shareholders.isNotEmpty() || selectedShareholder != null),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_payment"),
                shape = RoundedCornerShape(12.dp),
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
                    text = if (isSaving) "সংরক্ষণ হচ্ছে..." else if (isEditing) "পরিবর্তন সংরক্ষণ করুন" else "পেমেন্ট সংরক্ষণ করুন",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class PaymentMethodItem(
    val key: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun PaymentMethodCard(
    item: PaymentMethodItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
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
            .padding(vertical = 12.dp, horizontal = 12.dp),
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
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor
            )
        }
    }
}

