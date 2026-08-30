package com.example.data.backup

import com.example.data.local.DailyReportEntity
import com.example.data.local.FarmProfileEntity
import com.example.data.local.MonthlyExpenseEntity
import com.example.data.local.RolePermissionConfig
import com.example.data.local.ShareholderEntity
import com.example.data.local.ShareholderPaymentEntity
import com.example.data.local.StaffEntity
import com.example.data.local.StaffPaymentEntity
import com.example.data.local.UserEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Root JSON backup payload format for Google Apps Script
 */
@JsonClass(generateAdapter = true)
data class SheetsBackupPayload(
    @Json(name = "backup_schema_version")
    val backupSchemaVersion: Int = 1,
    @Json(name = "app_version")
    val appVersion: String = "1.0.0",
    @Json(name = "app_name")
    val appName: String = "Kazi Agrotech",
    @Json(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @Json(name = "request_id")
    val requestId: String = "",
    @Json(name = "formatted_time")
    val formattedTime: String = "",
    @Json(name = "user_id")
    val userId: String = "",
    @Json(name = "user_email")
    val userEmail: String = "",
    @Json(name = "api_token")
    val apiToken: String = "",
    @Json(name = "data")
    val data: SheetsBackupData = SheetsBackupData()
)

@JsonClass(generateAdapter = true)
data class SheetsBackupData(
    @Json(name = "farm_profile")
    val farmProfile: FarmProfileEntity? = null,
    @Json(name = "daily_reports")
    val dailyReports: List<DailyReportEntity> = emptyList(),
    @Json(name = "monthly_expenses")
    val monthlyExpenses: List<MonthlyExpenseEntity> = emptyList(),
    @Json(name = "users")
    val users: List<UserEntity> = emptyList(),
    @Json(name = "role_permissions")
    val rolePermissions: Map<String, RolePermissionConfig> = emptyMap(),
    @Json(name = "shareholders")
    val shareholders: List<ShareholderEntity> = emptyList(),
    @Json(name = "shareholder_payments")
    val shareholderPayments: List<ShareholderPaymentEntity> = emptyList(),
    @Json(name = "staff")
    val staff: List<StaffEntity> = emptyList(),
    @Json(name = "staff_payments")
    val staffPayments: List<StaffPaymentEntity> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SheetsBackupResponse(
    @Json(name = "success")
    val success: Boolean = false,
    @Json(name = "message")
    val message: String = "",
    @Json(name = "records_processed")
    val recordsProcessed: Int = 0,
    @Json(name = "timestamp")
    val timestamp: String = ""
)

sealed class SheetsBackupStatus {
    object Idle : SheetsBackupStatus()
    object InProgress : SheetsBackupStatus()
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : SheetsBackupStatus()
    data class Error(val errorMessage: String) : SheetsBackupStatus()
}
