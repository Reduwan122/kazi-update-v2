package com.example.data.backup

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.repository.PoultryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SheetsBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "SheetsBackupWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val manager = GoogleSheetsBackupManager(applicationContext)

        if (!manager.isAutoBackupEnabled()) {
            Log.d(TAG, "Auto Google Sheets backup is disabled. Skipping.")
            return@withContext Result.success()
        }

        val webAppUrl = manager.getWebAppUrl()
        if (webAppUrl.isBlank()) {
            Log.w(TAG, "Google Sheets Web App URL not configured. Skipping.")
            return@withContext Result.success()
        }

        if (!manager.isNetworkAvailable()) {
            Log.w(TAG, "No internet connection for Google Sheets backup. Will retry.")
            return@withContext Result.retry()
        }

        try {
            val repository = PoultryRepository(applicationContext)
            val farmProfile = repository.farmProfile.first()
            val dailyReports = repository.allDailyReports.first()
            val monthlyExpenses = repository.allExpenses.first()
            val rolePermissions = repository.rolePermissions.first()
            val allUsers = repository.allUsers.first()
            val shareholders = repository.allShareholders.first()
            val shareholderPayments = repository.allShareholderPayments.first()

            val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
            val currentUser = auth?.currentUser

            val backupResult = manager.executeBackup(
                farmProfile = farmProfile,
                dailyReports = dailyReports,
                monthlyExpenses = monthlyExpenses,
                users = allUsers,
                rolePermissions = rolePermissions,
                shareholders = shareholders,
                shareholderPayments = shareholderPayments,
                userId = currentUser?.uid ?: "",
                userEmail = currentUser?.email ?: ""
            )

            if (backupResult.isSuccess) {
                Log.i(TAG, "Automatic Google Sheets backup succeeded: ${backupResult.getOrNull()?.message}")
                Result.success()
            } else {
                Log.e(TAG, "Automatic Google Sheets backup failed: ${backupResult.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Sheets auto backup: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "KaziAgroSheetsAutoBackupWork"

        fun schedule(context: Context, frequency: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val intervalHours = when (frequency.uppercase()) {
                "DAILY", "24_HOURS", "24" -> 24L
                "12_HOURS", "12" -> 12L
                "6_HOURS", "6" -> 6L
                "MANUAL" -> {
                    cancel(context)
                    return
                }
                else -> 6L // Default 6 hours
            }

            val workRequest = PeriodicWorkRequestBuilder<SheetsBackupWorker>(
                intervalHours, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // Flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.i("SheetsBackupWorker", "Scheduled Sheets Auto Backup every $intervalHours hours")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i("SheetsBackupWorker", "Cancelled Sheets Auto Backup")
        }
    }
}

