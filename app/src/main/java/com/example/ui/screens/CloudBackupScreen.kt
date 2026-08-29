package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.backup.SheetsBackupStatus
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.SnackbarController
import com.example.ui.components.rememberHaptics
import com.example.ui.viewmodel.PoultryViewModel

@Composable
fun CloudBackupScreen(
    viewModel: PoultryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()

    val backupStatus by viewModel.sheetsBackupStatus.collectAsState()
    val lastBackupTime by viewModel.lastSheetsBackupTime.collectAsState()
    val isAutoBackupEnabled by viewModel.isSheetsAutoBackupEnabled.collectAsState()
    val backupFrequency by viewModel.sheetsBackupFrequency.collectAsState()
    val webAppUrl by viewModel.sheetsWebAppUrl.collectAsState()
    val apiToken by viewModel.sheetsApiToken.collectAsState()
    val lastBackupCount by viewModel.lastSheetsBackupCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.isAdmin() == true

    var showConfigDialog by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }

    val isBackingUp = backupStatus is SheetsBackupStatus.InProgress

    LaunchedEffect(Unit) {
        viewModel.refreshSheetsBackupState()
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "ক্লাউড ব্যাকআপ (Google Sheets)",
                isRootScreen = false,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        if (!isAdmin) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
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
                            text = "শুধুমাত্র এডমিনদের জন্য",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ক্লাউড ব্যাকআপ সেটিংস ও নিয়ন্ত্রণ শুধুমাত্র এডমিন অ্যাক্সেস করতে পারবেন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ফিরে যান")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .testTag("cloud_backup_screen"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

            // ══════════════════════════════════════════════════════════════
            // Card 1: Cloud Sync Status & Instant Backup Button
            // ══════════════════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (webAppUrl.isNotBlank()) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (webAppUrl.isNotBlank()) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (webAppUrl.isNotBlank()) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "গুগল শিট ক্লাউড ব্যাকআপ",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (webAppUrl.isNotBlank()) "অনলাইন ব্যাকআপ সক্রিয়" else "Web App URL কনফিগার করা প্রয়োজন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (webAppUrl.isNotBlank()) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                            }
                        }

                        // Web App Config status badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (webAppUrl.isNotBlank()) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = if (webAppUrl.isNotBlank()) "সংযুক্ত" else "অসংযুক্ত",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (webAppUrl.isNotBlank()) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Backup Meta Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "সর্বশেষ ব্যাকআপের সময়:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = viewModel.sheetsBackupManager.formatTimestampBangla(lastBackupTime),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (lastBackupCount > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "সিঙ্ককৃত রেকর্ড:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${BanglaNumberFormatter.formatNumber(lastBackupCount)} টি",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Backup Now Button
                    Button(
                        onClick = {
                            haptics.tap()
                            if (webAppUrl.isBlank()) {
                                SnackbarController.showError("প্রথমে Google Apps Script Web App URL সেট করুন")
                                showConfigDialog = true
                            } else {
                                viewModel.triggerSheetsBackup()
                            }
                        },
                        enabled = !isBackingUp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_backup_now"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isBackingUp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("গুগল শিটে ব্যাকআপ হচ্ছে...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("এখনই ব্যাকআপ করুন", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // Card 2: Auto Backup & Schedule Settings
            // ══════════════════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "স্বয়ংক্রিয় ক্লাউড ব্যাকআপ",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Switch(
                            checked = isAutoBackupEnabled,
                            onCheckedChange = { enabled ->
                                haptics.tap()
                                viewModel.updateSheetsBackupSettings(
                                    webAppUrl = webAppUrl,
                                    apiToken = apiToken,
                                    autoBackupEnabled = enabled,
                                    frequency = backupFrequency
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    Text(
                        text = "স্বয়ংক্রিয় ব্যাকআপ চালু থাকলে অ্যাপ বন্ধ থাকলেও ব্যাকগ্রাউন্ডে আপনার খামারের সকল তথ্য ও রিপোর্ট গুগল শিটে সিঙ্ক হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isAutoBackupEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Text(
                            text = "ব্যাকআপের সময়সূচী (Frequency):",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val frequencies = listOf(
                            "6_HOURS" to "প্রতি ৬ ঘণ্টা পরপর (সুপারিশকৃত)",
                            "12_HOURS" to "প্রতি ১২ ঘণ্টা পরপর",
                            "DAILY" to "প্রতিদিন একবার (২৪ ঘণ্টা)",
                            "MANUAL" to "শুধুমাত্র ম্যানুয়াল (বাটনে চাপলে)"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            frequencies.forEach { (freqKey, freqLabel) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptics.tap()
                                            viewModel.updateSheetsBackupSettings(
                                                webAppUrl = webAppUrl,
                                                apiToken = apiToken,
                                                autoBackupEnabled = isAutoBackupEnabled,
                                                frequency = freqKey
                                            )
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = backupFrequency.equals(freqKey, ignoreCase = true),
                                        onClick = {
                                            haptics.tap()
                                            viewModel.updateSheetsBackupSettings(
                                                webAppUrl = webAppUrl,
                                                apiToken = apiToken,
                                                autoBackupEnabled = isAutoBackupEnabled,
                                                frequency = freqKey
                                            )
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = freqLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // Card 3: Google Apps Script Web App Configuration (Admin Only)
            // ══════════════════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "গুগল শিট সংযোগ কনফিগারেশন",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isAdmin) {
                            IconButton(onClick = { showConfigDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Configuration",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Web App URL display
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Apps Script Web App URL:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (webAppUrl.isNotBlank()) webAppUrl else "ইউআরএল যুক্ত করা হয়নি",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = if (webAppUrl.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(10.dp),
                                maxLines = 2
                            )
                        }
                    }

                    // Setup Guide / Apps Script button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showInstructionsDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("সেটআপ নির্দেশিকা", fontSize = 13.sp)
                        }

                        if (isAdmin) {
                            Button(
                                onClick = { showConfigDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("URL পরিবর্তন", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

    // ══════════════════════════════════════════════════════════════
    // Dialog: Edit Web App URL & API Token (Admin Only)
    // ══════════════════════════════════════════════════════════════
    if (isAdmin && showConfigDialog) {
        var inputUrl by remember { mutableStateOf(webAppUrl) }
        var inputToken by remember { mutableStateOf(apiToken) }
        var isTokenVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Text(
                    text = "Google Sheets Web App সেটিংস",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "গুগল শিটের Apps Script ডিপ্লয় করে প্রাপ্ত Web App URL ও সিক্রেট API Token দিন:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("Web App URL (HTTPS)") },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputToken,
                        onValueChange = { inputToken = it },
                        label = { Text("Secret API Token (নিরাপত্তা টোকেন)") },
                        placeholder = { Text("Apps Script এ সেট করা গোপন টোকেন") },
                        singleLine = true,
                        visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                Icon(
                                    imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isTokenVisible) "Hide token" else "Show token"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "🔒 টোকেনটি Android KeyStore এ AES-256 দিয়ে এনক্রিপ্ট হয়ে সংরক্ষিত থাকে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSheetsBackupSettings(
                            webAppUrl = inputUrl.trim(),
                            apiToken = inputToken.trim(),
                            autoBackupEnabled = isAutoBackupEnabled,
                            frequency = backupFrequency
                        )
                        showConfigDialog = false
                        SnackbarController.showMessage("গুগল শিট কনফিগারেশন সংরক্ষিত হয়েছে")
                    }
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog: Setup Instructions Guide (Admin Only)
    // ══════════════════════════════════════════════════════════════
    if (isAdmin && showInstructionsDialog) {
        AlertDialog(
            onDismissRequest = { showInstructionsDialog = false },
            title = {
                Text(
                    text = "গুগল শিট ব্যাকআপ সেটআপ গাইড",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "সহজ ৪টি ধাপে আপনার গুগল শিট ক্লাউড ব্যাকআপ সংযুক্ত করুন:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Text(
                        text = "১. গুগল শিট (https://sheets.new) এ গিয়ে একটি নতুন স্প্রেডশিট খুলুন।\n" +
                                "২. মেনু থেকে Extensions > Apps Script এ ক্লিক করুন।\n" +
                                "৩. প্রজেক্টের 'GOOGLE_APPS_SCRIPT_BACKUP.gs' ফাইলের কোডটি সেখানে পেস্ট করুন।\n" +
                                "৪. Deploy > New deployment সিলেক্ট করে Type দিন 'Web app'।\n" +
                                "   - Execute as: 'Me'\n" +
                                "   - Who has access: 'Anyone'\n" +
                                "৫. Deploy করে প্রাপ্ত 'Web App URL' টি কপি করে এই স্ক্রিনের 'URL পরিবর্তন' অপশনে পেস্ট করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Apps Script Guide", "https://sheets.new")
                            clipboard.setPrimaryClip(clip)
                            SnackbarController.showMessage("Google Sheets লিংক কপি করা হয়েছে")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google Sheets লিংক কপি করুন")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showInstructionsDialog = false }) {
                    Text("ঠিক আছে")
                }
            }
        )
    }
}

