package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ShareholderEntity
import com.example.ui.components.AccessDeniedView
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.SnackbarController
import com.example.ui.components.rememberHaptics
import com.example.ui.viewmodel.PoultryViewModel

@Composable
fun ShareholderSettingsScreen(
    viewModel: PoultryViewModel,
    onBack: () -> Unit
) {
    val haptics = rememberHaptics()
    val shareholders by viewModel.shareholders.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.isAdmin() == true

    var showAddDialog by remember { mutableStateOf(false) }
    var editingShareholder by remember { mutableStateOf<ShareholderEntity?>(null) }
    var deletingShareholder by remember { mutableStateOf<ShareholderEntity?>(null) }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "শেয়ারহোল্ডার সেটিংস",
                isRootScreen = false,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        if (!isAdmin) {
            AccessDeniedView(
                title = "শেয়ারহোল্ডার সেটিংস সংরক্ষিত",
                message = "শুধুমাত্র এডমিন শেয়ারহোল্ডার তৈরি, পরিবর্তন বা মুছে ফেলতে পারেন।",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .testTag("shareholder_settings_screen"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // + Add Shareholder Button
                Button(
                    onClick = {
                        haptics.tap()
                        showAddDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_add_shareholder"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "শেয়ারহোল্ডার যোগ করুন",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                // Shareholder List or Empty State
                if (shareholders.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GroupOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "কোনো শেয়ারহোল্ডার পাওয়া যায়নি",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "নতুন শেয়ারহোল্ডার যোগ করতে উপরের বাটনে ক্লিক করুন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(shareholders, key = { it.id.ifBlank { it.name + "_" + it.createdAt } }) { shareholder ->
                            ShareholderRowItem(
                                shareholder = shareholder,
                                onEdit = {
                                    haptics.tap()
                                    editingShareholder = shareholder
                                },
                                onDelete = {
                                    haptics.tap()
                                    deletingShareholder = shareholder
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog: Add Shareholder
    // ══════════════════════════════════════════════════════════════
    if (showAddDialog) {
        var inputName by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddDialog = false },
            title = {
                Text(
                    text = "শেয়ারহোল্ডার যোগ করুন",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "শেয়ারহোল্ডারের নাম",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        placeholder = { Text("শেয়ারহোল্ডারের নাম লিখুন") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_shareholder_name")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.tap()
                        isSaving = true
                        viewModel.addShareholder(
                            name = inputName,
                            onSuccess = {
                                isSaving = false
                                showAddDialog = false
                                SnackbarController.showMessage("শেয়ারহোল্ডার সফলভাবে যোগ করা হয়েছে!")
                            },
                            onError = { err ->
                                isSaving = false
                                SnackbarController.showError(err)
                            }
                        )
                    },
                    enabled = inputName.isNotBlank() && !isSaving,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isSaving) "সংরক্ষণ হচ্ছে..." else "সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    enabled = !isSaving
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog: Edit Shareholder
    // ══════════════════════════════════════════════════════════════
    editingShareholder?.let { shareholder ->
        var inputName by remember { mutableStateOf(shareholder.name) }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) editingShareholder = null },
            title = {
                Text(
                    text = "শেয়ারহোল্ডারের নাম পরিবর্তন করুন",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "শেয়ারহোল্ডারের নাম",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        placeholder = { Text("শেয়ারহোল্ডারের নাম লিখুন") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.tap()
                        isSaving = true
                        viewModel.updateShareholder(
                            id = shareholder.id,
                            name = inputName,
                            onSuccess = {
                                isSaving = false
                                editingShareholder = null
                                SnackbarController.showMessage("শেয়ারহোল্ডারের তথ্য আপডেট হয়েছে!")
                            },
                            onError = { err ->
                                isSaving = false
                                SnackbarController.showError(err)
                            }
                        )
                    },
                    enabled = inputName.isNotBlank() && !isSaving,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isSaving) "সংরক্ষণ হচ্ছে..." else "সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { editingShareholder = null },
                    enabled = !isSaving
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog: Delete Confirmation
    // ══════════════════════════════════════════════════════════════
    deletingShareholder?.let { shareholder ->
        var isDeleting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isDeleting) deletingShareholder = null },
            title = {
                Text(
                    text = "শেয়ারহোল্ডার মুছে ফেলবেন?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে '${shareholder.name}' কে তালিকা থেকে মুছে ফেলতে চান?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.tap()
                        isDeleting = true
                        viewModel.deleteShareholder(
                            id = shareholder.id,
                            onSuccess = {
                                isDeleting = false
                                deletingShareholder = null
                                SnackbarController.showMessage("শেয়ারহোল্ডার সফলভাবে মুছে ফেলা হয়েছে!")
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
                    onClick = { deletingShareholder = null },
                    enabled = !isDeleting
                ) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun ShareholderRowItem(
    shareholder: ShareholderEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val initialLetter = remember(shareholder.name) {
        val trimmed = shareholder.name.trim()
        if (trimmed.isNotEmpty()) trimmed.take(1) else "শ"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Circle Avatar with initial letter
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialLetter,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = shareholder.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

