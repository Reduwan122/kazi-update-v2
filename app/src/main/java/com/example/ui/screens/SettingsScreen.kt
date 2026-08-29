package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.FarmLogoDisplay
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.UserProfileAvatar
import com.example.ui.viewmodel.PoultryViewModel
import androidx.compose.material3.SnackbarDuration
import com.example.ui.components.SnackbarController
import com.example.ui.components.rememberHaptics

@Composable
fun SettingsScreen(
    viewModel: PoultryViewModel,
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToRolePermissions: (String) -> Unit = {},
    onNavigateToShareholderSettings: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val farmProfile by viewModel.farmProfile.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val dailyReports by viewModel.dailyReports.collectAsState()

    val todayDate = remember { BanglaNumberFormatter.getCurrentDateFormatted() }
    val hasTodayReport = remember(dailyReports, todayDate) { dailyReports.any { it.date == todayDate } }
    val notifDismissedDate by viewModel.notificationDismissedDate.collectAsState()
    val hasUnreadNotification = !hasTodayReport && notifDismissedDate != todayDate

    val isAdmin = currentUser?.isAdmin() == true
    val pendingCount = allUsers.count { !it.isApproved && !it.isAdmin() }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoSelectionDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showInitialStockDialog by remember { mutableStateOf(false) }
    var isUploadingLogo by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploadingLogo = true
            viewModel.uploadFarmLogoFromUri(
                context = context,
                imageUri = uri,
                onSuccess = {
                    isUploadingLogo = false
                    showLogoSelectionDialog = false
                    SnackbarController.showMessage("লোগো ছবি সফলভাবে আপলোড ও আপডেট করা হয়েছে!", SnackbarDuration.Long)
                },
                onError = { error ->
                    isUploadingLogo = false
                    SnackbarController.showError("লোগো আপলোড সমস্যা: $error")
                }
            )
        }
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "সেটিংস",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("settings_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // User Profile Row (Quick Access)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigateToProfile() }
                    .testTag("card_user_profile"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        UserProfileAvatar(
                            profileImageUri = currentUser?.profileImageUri ?: "",
                            username = currentUser?.username ?: "",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.username ?: "ব্যবহারকারী প্রোফাইল",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "${currentUser?.email} • ${currentUser?.role ?: "WORKER"}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "View Profile",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Farm Profile Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (isAdmin) {
                            showEditProfileDialog = true
                        } else {
                            SnackbarController.showError("খামার প্রোফাইল ও সেটিংস শুধুমাত্র এডমিন পরিবর্তন করতে পারেন")
                        }
                    }
                    .testTag("card_farm_profile"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(enabled = isAdmin) { showLogoSelectionDialog = true }
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FarmLogoDisplay(
                            logoUri = farmProfile.logoUri,
                            logoEmoji = farmProfile.logoEmoji,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = farmProfile.farmName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            if (isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("এডমিন কন্ট্রোল", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Text(
                            text = "প্রোঃ ${farmProfile.ownerName}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                        )
                        Text(
                            text = "মোবাইলঃ ${farmProfile.mobileNumber}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        )
                        Text(
                            text = "ঠিকানাঃ ${farmProfile.address}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        )
                    }

                    if (isAdmin) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ADMIN CONTROL SECTION (Visible only if user is Admin or prompt to Admin)
            if (isAdmin) {
                Text(
                    text = "প্রশাসনিক এক্সেস কন্ট্রোল (Admin Only)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigateToAdmin() }
                        .testTag("admin_approval_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ইউজার ও রোল ম্যানেজমেন্ট",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "ব্যবহারকারীদের অ্যাক্সেস ও ভূমিকা নিয়ন্ত্রণ করুন",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        if (pendingCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pendingCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Go",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Role Permission Editor Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigateToRolePermissions("MANAGER") }
                        .testTag("admin_role_permissions_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Role Permissions",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "রোল পারমিশন এডিটর",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "ম্যানেজার, সুপারভাইজার ও কর্মীর এক্সেস কনফিগারেশন",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Go",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Initial Baseline Stock Setup Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showInitialStockDialog = true }
                        .testTag("admin_initial_stock_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = "Initial Stock",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "প্রারম্ভিক স্টক কনফিগারেশন",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                val stockDisplay = if (farmProfile.initialOpeningStock > 0) {
                                    "${BanglaNumberFormatter.formatNumber(farmProfile.initialOpeningStock)} ডিম (${if (farmProfile.initialOpeningDate.isNotBlank()) BanglaNumberFormatter.formatShortDate(farmProfile.initialOpeningDate) else "পূর্ববর্তী"})"
                                } else {
                                    "সেট করা নেই (০ ডিম)"
                                }
                                Text(
                                    text = "বর্তমান প্রারম্ভিক স্টক: $stockDisplay",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Go",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sync Status Indicator
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = syncStatus,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        )
                    }

                    TextButton(
                        onClick = { viewModel.manualBackup(context) }
                    ) {
                        Text("এখনই সিঙ্ক করুন", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // System Settings Section
            Text(
                text = "সিস্টেম সেটিংস",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Farm Profile Edit (Admin only)
                    SettingsRowItem(
                        icon = Icons.Default.Agriculture,
                        title = "ফার্ম প্রোফাইল তথ্য",
                        subtitle = if (isAdmin) "নাম, মালিকের নাম, মোবাইল ও ঠিকানা" else "শুধুমাত্র এডমিন পরিবর্তন করতে পারেন",
                        onClick = {
                            if (isAdmin) {
                                showEditProfileDialog = true
                            } else {
                                SnackbarController.showError("খামার প্রোফাইল ও তথ্য শুধুমাত্র এডমিন পরিবর্তন করতে পারেন")
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Change Farm Logo (Admin only)
                    SettingsRowItem(
                        icon = Icons.Default.Image,
                        title = "খামার লোগো পরিবর্তন (PDF ও অ্যাপ)",
                        subtitle = "রিপোর্ট ও পিডিএফে প্রদর্শিত লোগো নির্বাচন করুন",
                        onClick = {
                            if (isAdmin) {
                                showLogoSelectionDialog = true
                            } else {
                                SnackbarController.showError("খামার লোগো পরিবর্তন শুধুমাত্র এডমিন করতে পারেন")
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Google Sheets Cloud Backup
                    val lastSheetsBackupTime by viewModel.lastSheetsBackupTime.collectAsState()
                    val webAppUrl by viewModel.sheetsWebAppUrl.collectAsState()

                    SettingsRowItem(
                        icon = Icons.Default.CloudUpload,
                        title = "ক্লাউড ব্যাকআপ (Google Sheets)",
                        subtitle = if (webAppUrl.isNotBlank()) {
                            if (lastSheetsBackupTime > 0) "সর্বশেষ: " + viewModel.sheetsBackupManager.formatTimestampBangla(lastSheetsBackupTime)
                            else "অনলাইন ব্যাকআপ সক্রিয় • কোনো ব্যাকআপ নেই"
                        } else {
                            "গুগল শিট ক্লাউড ব্যাকআপ কনফিগার করুন"
                        },
                        onClick = onNavigateToBackupRestore
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Shareholder Settings (Admin only)
                    if (isAdmin) {
                        SettingsRowItem(
                            icon = Icons.Default.Groups,
                            title = "শেয়ারহোল্ডার সেটিংস",
                            subtitle = "শেয়ারহোল্ডার তালিকা ও ব্যবস্থাপনা",
                            onClick = onNavigateToShareholderSettings
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }

                    // Security
                    SettingsRowItem(
                        icon = Icons.Default.Lock,
                        title = "নিরাপত্তা ও পাসওয়ার্ড",
                        subtitle = "পাসওয়ার্ড পরিবর্তন ও এক্সেস সেটিংস",
                        onClick = { showChangePasswordDialog = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = "Dark Mode",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ডার্ক থিম (নাইট মোড)",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "চোখের সুরক্ষায় ডার্ক কালার ব্যবহার",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = farmProfile.isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // About App
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = "অ্যাপ পরিচিতি",
                        subtitle = "কাজী এগ্রোটেক সংস্করণ ১.০.০",
                        onClick = { showAboutDialog = true }
                    )
                }
            }

            // Logout Button
            Button(
                onClick = { showLogoutConfirm = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_logout")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "লগআউট করুন",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Logo Selection & Upload Dialog
    if (showLogoSelectionDialog) {
        val logos = listOf(
            "🐔" to "পোল্ট্রি মুরগী",
            "🥚" to "ডিম আইকন",
            "🚜" to "ফার্ম ট্রাক্টর",
            "🌾" to "ধান ও গম",
            "🏡" to "খামার বাড়ি",
            "🐣" to "মুরগীর বাচ্চা",
            "🐓" to "মোরগ",
            "🏢" to "এগ্রোটেক কর্পোরেট"
        )
        AlertDialog(
            onDismissRequest = {
                if (!isUploadingLogo) showLogoSelectionDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "খামার লোগো পরিবর্তন ও আপলোড",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Current Logo Preview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                FarmLogoDisplay(
                                    logoUri = farmProfile.logoUri,
                                    logoEmoji = farmProfile.logoEmoji,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "বর্তমান সক্রিয় লোগো",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = when {
                                        farmProfile.logoUri.isNotBlank() -> "আপলোডকৃত ছবি (PNG/JPG)"
                                        farmProfile.logoEmoji.isNotBlank() && farmProfile.logoEmoji != "🐔" -> "আইকন: ${farmProfile.logoEmoji}"
                                        else -> "ডিফল্ট কাজী এগ্রোটেক লোগো"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    // Device Image Upload Section (Primary Feature)
                    Text(
                        text = "১. নিজস্ব ছবি/লোগো আপলোড করুন (PNG / JPG):",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !isUploadingLogo,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_upload_logo_image")
                    ) {
                        if (isUploadingLogo) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ছবি প্রসেসিং ও সেভ হচ্ছে...", fontSize = 13.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("গ্যালারি থেকে ছবি সিলেক্ট করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Text(
                        text = "💡 যেকোনো PNG বা JPG ফাইল নির্বাচন করতে পারেন। এটি অ্যাপের শীর্ষে এবং সকল PDF রিপোর্টে প্রদর্শিত হবে।",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Built-in presets
                    Text(
                        text = "২. অথবা আইকন লোগো বেছে নিন:",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        logos.take(4).forEach { (emoji, label) ->
                            LogoOption(
                                emoji = emoji,
                                isSelected = farmProfile.logoUri.isBlank() && farmProfile.logoEmoji == emoji,
                                onSelect = {
                                    viewModel.updateFarmLogo(emoji)
                                    showLogoSelectionDialog = false
                                    SnackbarController.showMessage("লোগো আপডেট হয়েছে!")
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        logos.drop(4).forEach { (emoji, label) ->
                            LogoOption(
                                emoji = emoji,
                                isSelected = farmProfile.logoUri.isBlank() && farmProfile.logoEmoji == emoji,
                                onSelect = {
                                    viewModel.updateFarmLogo(emoji)
                                    showLogoSelectionDialog = false
                                    SnackbarController.showMessage("লোগো আপডেট হয়েছে!")
                                }
                            )
                        }
                    }

                    // Reset to default button
                    if (farmProfile.logoUri.isNotBlank() || (farmProfile.logoEmoji.isNotBlank() && farmProfile.logoEmoji != "🐔")) {
                        OutlinedButton(
                            onClick = {
                                viewModel.resetToDefaultLogo {
                                    showLogoSelectionDialog = false
                                    SnackbarController.showMessage("ডিফল্ট লোগো রিসেট করা হয়েছে")
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ডিফল্ট লোগো ফিরিয়ে আনুন", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLogoSelectionDialog = false },
                    enabled = !isUploadingLogo
                ) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(farmProfile.farmName) }
        var ownerInput by remember { mutableStateOf(farmProfile.ownerName) }
        var mobileInput by remember { mutableStateOf(farmProfile.mobileNumber) }
        var addressInput by remember { mutableStateOf(farmProfile.address) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("ফার্ম প্রোফাইল পরিবর্তন (এডমিন)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("ফার্মের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ownerInput,
                        onValueChange = { ownerInput = it },
                        label = { Text("মালিকের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mobileInput,
                        onValueChange = { mobileInput = it },
                        label = { Text("মোবাইল নম্বর") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("ঠিকানা") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.tap()
                        viewModel.updateFarmProfile(
                            farmName = nameInput,
                            ownerName = ownerInput,
                            mobileNumber = mobileInput,
                            address = addressInput,
                            logoEmoji = farmProfile.logoEmoji
                        )
                        showEditProfileDialog = false
                        SnackbarController.showMessage("ফার্ম প্রোফাইল আপডেট সফল হয়েছে!")
                    }
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("পাসওয়ার্ড পরিবর্তন", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (errorMsg.isNotEmpty()) {
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = {
                            newPass = it
                            errorMsg = ""
                        },
                        label = { Text("নতুন শক্তিশালী পাসওয়ার্ড") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    com.example.ui.components.PasswordStrengthIndicator(password = newPass)

                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = {
                            confirmPass = it
                            errorMsg = ""
                        },
                        label = { Text("নতুন পাসওয়ার্ড পুনরায় লিখুন") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.tap()
                        val strength = com.example.ui.components.PasswordStrength.validate(newPass)
                        if (!strength.isStrong) {
                            errorMsg = "অনুগ্রহ করে একটি শক্তিশালী পাসওয়ার্ড প্রদান করুন (কমপক্ষে ৮ অক্ষর, বড় ও ছোট অক্ষর, সংখ্যা ও স্পেশাল চিহ্ন)।"
                        } else if (newPass != confirmPass) {
                            errorMsg = "উভয় পাসওয়ার্ড মিলছে না।"
                        } else {
                            viewModel.changePassword(
                                newPass = newPass,
                                onSuccess = {
                                    showChangePasswordDialog = false
                                    SnackbarController.showMessage("পাসওয়ার্ড পরিবর্তন সফল হয়েছে!")
                                },
                                onError = { errorMsg = it }
                            )
                        }
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("কাজী এগ্রোটেক ম্যানেজমেন্ট") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("সংস্করণ: ১.০.০")
                    Text("কাজী এগ্রোটেক পোল্ট্রি ফার্মের হিসাব ও ব্যবস্থাপনা সিস্টেম।")
                    Text("এডমিন অনুমোদন, রোল কন্ট্রোল ও ক্লাউড সিঙ্ক সিস্টেম সহ।")
                    Text("ডেভেলপার ও প্রযুক্তি সহায়তা: কাজী এগ্রোটেক টিম")
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("ঠিক আছে")
                }
            }
        )
    }

    // Logout Confirm Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("লগআউট নিশ্চিতকরণ") },
            text = { Text("আপনি কি নিশ্চিতভাবে অ্যাকাউন্ট থেকে লগআউট করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.confirm()
                        showLogoutConfirm = false
                        viewModel.logout {
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("লগআউট")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Initial Baseline Stock Setup Dialog (Admin Only)
    if (showInitialStockDialog) {
        var stockInput by remember { mutableStateOf(if (farmProfile.initialOpeningStock > 0) farmProfile.initialOpeningStock.toString() else "") }
        var dateInput by remember { mutableStateOf(farmProfile.initialOpeningDate) }

        AlertDialog(
            onDismissRequest = { showInitialStockDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "প্রারম্ভিক স্টক সেটআপ",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "প্রথম দৈনিক রিপোর্ট রেকর্ড করার পূর্ববর্তী সমাপনী স্টক (Closing Stock) এখানে দিন। পুরো সিস্টেম এই স্টকের উপর ভিত্তি করে ধারাবাহিক স্টক গণনা করবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = { stockInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("প্রারম্ভিক ডিমের স্টক (সংখ্যা)") },
                        placeholder = { Text("উদাহরণ: 729") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("তারিখ (ঐচ্ছিক - YYYY-MM-DD)") },
                        placeholder = { Text("উদাহরণ: 2026-07-31") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "💡 উদাহরণ: আপনার খামারের প্রথম রিপোর্ট যদি ০১/০৮/২০২৬ হয় এবং ৩১/০৭/২০২৬ তারিখে ৭২৯টি ডিম থেকে থাকে, তবে এখানে ৭২৯ লিখুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedStock = stockInput.toIntOrNull() ?: 0
                        viewModel.updateInitialOpeningStock(parsedStock, dateInput.trim())
                        showInitialStockDialog = false
                    }
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInitialStockDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun LogoOption(
    emoji: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 28.sp)
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = "Go",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}
