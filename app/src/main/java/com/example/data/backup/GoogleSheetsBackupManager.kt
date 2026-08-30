package com.example.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.DailyReportEntity
import com.example.data.local.FarmProfileEntity
import com.example.data.local.MonthlyExpenseEntity
import com.example.data.local.RolePermissionConfig
import com.example.data.local.ShareholderEntity
import com.example.data.local.ShareholderPaymentEntity
import com.example.data.local.StaffEntity
import com.example.data.local.StaffPaymentEntity
import com.example.data.local.UserEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GoogleSheetsBackupManager(private val context: Context) {

    private val TAG = "GoogleSheetsBackup"
    private val PREFS_NAME = "kazi_sheets_backup_prefs"

    private val KEY_WEB_APP_URL = "key_sheets_web_app_url"
    private val KEY_API_TOKEN = "key_sheets_api_token"
    private val KEY_AUTO_BACKUP = "key_sheets_auto_backup"
    private val KEY_FREQUENCY = "key_sheets_frequency"
    private val KEY_LAST_BACKUP_TIME = "key_sheets_last_backup_time"
    private val KEY_LAST_BACKUP_STATUS = "key_sheets_last_backup_status"
    private val KEY_LAST_BACKUP_COUNT = "key_sheets_last_backup_count"
    private val KEY_LAST_BACKUP_MSG = "key_sheets_last_backup_msg"

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // -------------------------------------------------------------
    // PREFERENCE ACCESSORS
    // -------------------------------------------------------------

    fun getWebAppUrl(): String = prefs.getString(KEY_WEB_APP_URL, "") ?: ""

    fun setWebAppUrl(url: String) {
        val trimmed = url.trim()
        val secureUrl = if (trimmed.startsWith("http://", ignoreCase = true)) {
            "https://" + trimmed.removePrefix("http://")
        } else trimmed
        prefs.edit().putString(KEY_WEB_APP_URL, secureUrl).apply()
    }

    fun getApiToken(): String {
        val encrypted = prefs.getString(KEY_API_TOKEN, "") ?: ""
        return BackupSecurityHelper.decrypt(encrypted)
    }

    fun setApiToken(token: String) {
        val encrypted = BackupSecurityHelper.encrypt(token.trim())
        prefs.edit().putString(KEY_API_TOKEN, encrypted).apply()
    }

    fun isAutoBackupEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_BACKUP, true)

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply()
    }

    fun getAutoBackupFrequency(): String = prefs.getString(KEY_FREQUENCY, "6_HOURS") ?: "6_HOURS"

    fun setAutoBackupFrequency(freq: String) {
        prefs.edit().putString(KEY_FREQUENCY, freq).apply()
    }

    fun getLastBackupTimestamp(): Long = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)

    fun getLastBackupStatus(): String = prefs.getString(KEY_LAST_BACKUP_STATUS, "IDLE") ?: "IDLE"

    fun getLastBackupCount(): Int = prefs.getInt(KEY_LAST_BACKUP_COUNT, 0)

    fun getLastBackupMessage(): String = prefs.getString(KEY_LAST_BACKUP_MSG, "") ?: ""

    fun saveBackupResult(timestamp: Long, status: String, count: Int, message: String) {
        prefs.edit()
            .putLong(KEY_LAST_BACKUP_TIME, timestamp)
            .putString(KEY_LAST_BACKUP_STATUS, status)
            .putInt(KEY_LAST_BACKUP_COUNT, count)
            .putString(KEY_LAST_BACKUP_MSG, message)
            .apply()
    }

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // -------------------------------------------------------------
    // BACKUP EXECUTION
    // -------------------------------------------------------------

    suspend fun executeBackup(
        farmProfile: FarmProfileEntity?,
        dailyReports: List<DailyReportEntity>,
        monthlyExpenses: List<MonthlyExpenseEntity>,
        users: List<UserEntity>,
        rolePermissions: Map<String, RolePermissionConfig>,
        shareholders: List<ShareholderEntity> = emptyList(),
        shareholderPayments: List<ShareholderPaymentEntity> = emptyList(),
        staff: List<StaffEntity> = emptyList(),
        staffPayments: List<StaffPaymentEntity> = emptyList(),
        userId: String = "",
        userEmail: String = ""
    ): Result<SheetsBackupResponse> = withContext(Dispatchers.IO) {
        val webAppUrl = getWebAppUrl()
        if (webAppUrl.isBlank()) {
            val err = "গুগল শিট ওয়েব অ্যাপ ইউআরএল (Web App URL) সেট করা নেই। সেটিংসে গিয়ে URL দিন।"
            saveBackupResult(System.currentTimeMillis(), "ERROR", 0, err)
            return@withContext Result.failure(Exception(err))
        }

        if (!webAppUrl.startsWith("https://", ignoreCase = true)) {
            val err = "নিরাপত্তার স্বার্থে শুধুমাত্র সুরক্ষিত HTTPS URL ব্যবহারযোগ্য।"
            saveBackupResult(System.currentTimeMillis(), "ERROR", 0, err)
            return@withContext Result.failure(Exception(err))
        }

        if (!isNetworkAvailable()) {
            val err = "ইন্টারনেট সংযোগ নেই। অনুগ্রহ করে ডাটা বা ওয়াইফাই চালু করুন।"
            saveBackupResult(System.currentTimeMillis(), "ERROR", 0, err)
            return@withContext Result.failure(Exception(err))
        }

        try {
            val nowTime = System.currentTimeMillis()
            val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US)
            val formattedTime = sdf.format(Date(nowTime))
            val requestId = java.util.UUID.randomUUID().toString()
            val token = getApiToken()

            // Ensure daily reports and monthly expenses are sorted chronologically ascending (top-to-bottom: Day 1 at Row 2, Day 2 at Row 3... Day 31)
            val sortedDailyReports = dailyReports.sortedBy { it.date }
            val sortedMonthlyExpenses = monthlyExpenses.sortedBy { it.date }
            val sortedStaffPayments = staffPayments.sortedBy { it.date }
            val sortedShareholderPayments = shareholderPayments.sortedBy { it.date }

            val payload = SheetsBackupPayload(
                backupSchemaVersion = 1,
                appVersion = "2.5.0",
                appName = "Kazi Agrotech",
                timestamp = nowTime,
                requestId = requestId,
                formattedTime = formattedTime,
                userId = userId,
                userEmail = userEmail,
                apiToken = token,
                data = SheetsBackupData(
                    farmProfile = farmProfile,
                    dailyReports = sortedDailyReports,
                    monthlyExpenses = sortedMonthlyExpenses,
                    users = users,
                    rolePermissions = rolePermissions,
                    shareholders = shareholders.sortedBy { it.name },
                    shareholderPayments = sortedShareholderPayments,
                    staff = staff.sortedBy { it.name },
                    staffPayments = sortedStaffPayments
                )
            )

            val adapter = moshi.adapter(SheetsBackupPayload::class.java)
            val jsonPayloadString = adapter.toJson(payload)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonPayloadString.toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(webAppUrl)
                .post(body)
                .addHeader("Content-Type", "application/json; charset=utf-8")

            if (token.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val request = requestBuilder.build()
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful && response.code != 302) {
                val err = when (response.code) {
                    401 -> "অননুমোদিত এক্সেস: সিক্রেট API Token সঠিক নয়।"
                    429 -> "অতিরিক্ত রিকোয়েস্ট সীমা অতিক্রম হয়েছে। কিছুক্ষণ পর চেষ্টা করুন।"
                    else -> "সার্ভার রেসপন্স ব্যর্থ: HTTP ${response.code}"
                }
                saveBackupResult(nowTime, "ERROR", 0, err)
                return@withContext Result.failure(Exception(err))
            }

            // Google Apps Script may redirect with 302 or return 200 with JSON
            val parsedResponse = try {
                val jsonObject = JSONObject(responseBody)
                val isSuccess = jsonObject.optBoolean("success", false)
                val message = jsonObject.optString("message", if (isSuccess) "ক্লাউড ব্যাকআপ সফল হয়েছে" else "ব্যর্থ")
                val processedCount = jsonObject.optInt("records_processed", 0)
                val ts = jsonObject.optString("timestamp", "")
                SheetsBackupResponse(isSuccess, message, processedCount, ts)
            } catch (e: Exception) {
                // If response is HTML or plain text error
                if (responseBody.contains("success\":true") || response.isSuccessful) {
                    val count = dailyReports.size + monthlyExpenses.size + (if (farmProfile != null) 1 else 0)
                    SheetsBackupResponse(true, "ক্লাউড ব্যাকআপ সফল হয়েছে", count, formattedTime)
                } else {
                    val err = "গুগল শিট থেকে অপ্রত্যাশিত রেসপন্স: " + responseBody.take(100)
                    saveBackupResult(nowTime, "ERROR", 0, err)
                    return@withContext Result.failure(Exception(err))
                }
            }

            if (parsedResponse.success) {
                val count = if (parsedResponse.recordsProcessed > 0) parsedResponse.recordsProcessed
                            else dailyReports.size + monthlyExpenses.size + (if (farmProfile != null) 1 else 0)
                saveBackupResult(nowTime, "SUCCESS", count, "ক্লাউড ব্যাকআপ সফল হয়েছে")
                Result.success(parsedResponse.copy(recordsProcessed = count))
            } else {
                val err = parsedResponse.message.ifBlank { "ব্যাকআপ সম্পন্ন হয়নি। পরে আবার চেষ্টা করা হবে।" }
                saveBackupResult(nowTime, "ERROR", 0, err)
                Result.failure(Exception(err))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error executing Google Sheets backup: ${e.message}", e)
            val err = "ব্যাকআপ সম্পন্ন হয়নি: ${e.localizedMessage ?: "নেটওয়ার্ক ত্রুটি"}"
            saveBackupResult(System.currentTimeMillis(), "ERROR", 0, err)
            Result.failure(Exception(err))
        }
    }

    // -------------------------------------------------------------
    // FORMATTING HELPERS
    // -------------------------------------------------------------

    fun formatTimestampBangla(millis: Long): String {
        if (millis <= 0L) return "কোনো ব্যাকআপ নেই"
        val sdfDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.US)
        val formatted = sdfDate.format(Date(millis))
        return toBanglaDigitsAndMonths(formatted)
    }

    private fun toBanglaDigitsAndMonths(text: String): String {
        val months = mapOf(
            "January" to "জানুয়ারি", "February" to "ফেব্রুয়ারি", "March" to "মার্চ",
            "April" to "এপ্রিল", "May" to "মে", "June" to "জুন",
            "July" to "জুলাই", "August" to "আগস্ট", "September" to "সেপ্টেম্বর",
            "October" to "অক্টোবর", "November" to "নভেম্বর", "December" to "ডিসেম্বর",
            "AM" to "AM", "PM" to "PM"
        )
        var result = text
        for ((eng, bng) in months) {
            result = result.replace(eng, bng)
        }
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder()
        for (ch in result) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}

