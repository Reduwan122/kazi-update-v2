package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.backup.SheetsBackupData
import com.example.data.local.DailyReportEntity
import com.example.data.local.FarmProfileEntity
import com.example.data.local.MonthlyExpenseEntity
import com.example.data.local.RolePermissionConfig
import com.example.data.local.ShareholderEntity
import com.example.data.local.ShareholderPaymentEntity
import com.example.data.local.StaffEntity
import com.example.data.local.StaffPaymentEntity
import com.example.data.local.UserEntity
import com.example.domain.StockCalculationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

class PoultryRepository(
    private val context: Context
) {
    private val TAG = "PoultryRepository"
    private val prefs: SharedPreferences = context.getSharedPreferences("kazi_agro_prefs", Context.MODE_PRIVATE)

    private val PREF_KEY_REMEMBER = "key_remember_login"
    private val PREF_KEY_REMEMBERED_EMAIL = "key_remembered_email"
    private val PREF_KEY_DARK_MODE = "key_dark_mode"
    private val PREF_KEY_FARM_NAME = "key_farm_name"
    private val PREF_KEY_OWNER_NAME = "key_owner_name"
    private val PREF_KEY_MOBILE = "key_mobile"
    private val PREF_KEY_ADDRESS = "key_address"
    private val PREF_KEY_LOGO_URI = "key_logo_uri"
    private val PREF_KEY_LOGO_EMOJI = "key_logo_emoji"
    private val PREF_KEY_AUTO_BACKUP = "key_auto_backup"
    private val PREF_KEY_CACHED_USERNAME = "key_cached_username"
    private val PREF_KEY_CACHED_ROLE = "key_cached_role"
    private val PREF_KEY_CACHED_APPROVED = "key_cached_approved"
    private val PREF_KEY_CACHED_PHONE = "key_cached_phone"
    private val PREF_KEY_CACHED_AVATAR = "key_cached_avatar"

    // Node used to permanently lock out a deleted user, since the client app
    // cannot delete another user's Firebase Authentication account directly
    // (that requires the Admin SDK on a server). Any uid listed here is
    // treated as removed everywhere in the app, even if their Auth login
    // technically still exists.
    private val BLOCKED_USERS_NODE = "blockedUsers"

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.e(TAG, "FirebaseAuth initialization error: ${e.message}", e)
            null
        }
    }

    private val database: FirebaseDatabase? by lazy {
        try {
            val db = FirebaseDatabase.getInstance()
            try {
                db.setPersistenceEnabled(true)
            } catch (e: Exception) {
                // Persistence can only be configured once
            }
            db
        } catch (e: Throwable) {
            Log.e(TAG, "FirebaseDatabase initialization error: ${e.message}", e)
            null
        }
    }

    private val dbRef: DatabaseReference? by lazy {
        try {
            database?.reference
        } catch (e: Throwable) {
            null
        }
    }

    // StateFlows exposed to ViewModel (Clean real data only, starting empty)
    private val _allDailyReports = MutableStateFlow<List<DailyReportEntity>>(emptyList())
    val allDailyReports: Flow<List<DailyReportEntity>> = _allDailyReports.asStateFlow()

    private val _allExpenses = MutableStateFlow<List<MonthlyExpenseEntity>>(emptyList())
    val allExpenses: Flow<List<MonthlyExpenseEntity>> = _allExpenses.asStateFlow()

    private val _farmProfile = MutableStateFlow<FarmProfileEntity>(
        FarmProfileEntity(
            id = 1,
            farmName = prefs.getString(PREF_KEY_FARM_NAME, "কাজী এগ্রোটেক") ?: "কাজী এগ্রোটেক",
            ownerName = prefs.getString(PREF_KEY_OWNER_NAME, "খামার মালিক") ?: "খামার মালিক",
            mobileNumber = prefs.getString(PREF_KEY_MOBILE, "০১৭১২-০০০০০০") ?: "০১৭১২-০০০০০০",
            address = prefs.getString(PREF_KEY_ADDRESS, "গাজীপুর, বাংলাদেশ") ?: "গাজীপুর, বাংলাদেশ",
            logoUri = prefs.getString(PREF_KEY_LOGO_URI, "") ?: "",
            logoEmoji = prefs.getString(PREF_KEY_LOGO_EMOJI, "🐔") ?: "🐔",
            autoBackup = prefs.getBoolean(PREF_KEY_AUTO_BACKUP, true),
            isDarkMode = prefs.getBoolean(PREF_KEY_DARK_MODE, false),
            lastSyncTime = System.currentTimeMillis()
        )
    )
    val farmProfile: Flow<FarmProfileEntity> = _farmProfile.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: Flow<UserEntity?> = _currentUser.asStateFlow()

    private val _allUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val allUsers: Flow<List<UserEntity>> = _allUsers.asStateFlow()

    private val _rolePermissions = MutableStateFlow<Map<String, RolePermissionConfig>>(
        mapOf(
            "ADMIN" to loadRolePermissionFromPrefs("ADMIN"),
            "MANAGER" to loadRolePermissionFromPrefs("MANAGER"),
            "SUPERVISOR" to loadRolePermissionFromPrefs("SUPERVISOR"),
            "WORKER" to loadRolePermissionFromPrefs("WORKER")
        )
    )
    val rolePermissions: Flow<Map<String, RolePermissionConfig>> = _rolePermissions.asStateFlow()

    private val _allShareholders = MutableStateFlow<List<ShareholderEntity>>(emptyList())
    val allShareholders: Flow<List<ShareholderEntity>> = _allShareholders.asStateFlow()

    private val _allShareholderPayments = MutableStateFlow<List<ShareholderPaymentEntity>>(emptyList())
    val allShareholderPayments: Flow<List<ShareholderPaymentEntity>> = _allShareholderPayments.asStateFlow()

    private val _allStaff = MutableStateFlow<List<StaffEntity>>(emptyList())
    val allStaff: Flow<List<StaffEntity>> = _allStaff.asStateFlow()

    private val _allStaffPayments = MutableStateFlow<List<StaffPaymentEntity>>(emptyList())
    val allStaffPayments: Flow<List<StaffPaymentEntity>> = _allStaffPayments.asStateFlow()

    init {
        checkCurrentAuthSession()
        setupFirebaseRealtimeListeners()
    }

    private fun loadRolePermissionFromPrefs(roleKey: String): RolePermissionConfig {
        val default = RolePermissionConfig.getDefaultPermissionsForRole(roleKey)
        val prefix = "role_perm_${roleKey.uppercase()}_"
        return RolePermissionConfig(
            roleKey = roleKey.uppercase(),
            roleDisplayName = default.roleDisplayName,
            dailyReportView = prefs.getBoolean("${prefix}daily_view", default.dailyReportView),
            dailyReportAdd = prefs.getBoolean("${prefix}daily_add", default.dailyReportAdd),
            userManagementView = prefs.getBoolean("${prefix}user_view", default.userManagementView),
            expenseView = prefs.getBoolean("${prefix}expense_view", default.expenseView),
            expenseAdd = prefs.getBoolean("${prefix}expense_add", default.expenseAdd),
            expenseDelete = prefs.getBoolean("${prefix}expense_delete", default.expenseDelete),
            reportAnalyticsView = prefs.getBoolean("${prefix}report_view", default.reportAnalyticsView),
            reportAnalyticsDownload = prefs.getBoolean("${prefix}report_download", default.reportAnalyticsDownload)
        )
    }

    private fun saveRolePermissionToPrefs(config: RolePermissionConfig) {
        val prefix = "role_perm_${config.roleKey.uppercase()}_"
        prefs.edit()
            .putBoolean("${prefix}daily_view", config.dailyReportView)
            .putBoolean("${prefix}daily_add", config.dailyReportAdd)
            .putBoolean("${prefix}user_view", config.userManagementView)
            .putBoolean("${prefix}expense_view", config.expenseView)
            .putBoolean("${prefix}expense_add", config.expenseAdd)
            .putBoolean("${prefix}expense_delete", config.expenseDelete)
            .putBoolean("${prefix}report_view", config.reportAnalyticsView)
            .putBoolean("${prefix}report_download", config.reportAnalyticsDownload)
            .apply()
    }

    private fun saveFarmProfileToPrefs(profile: FarmProfileEntity) {
        prefs.edit()
            .putString(PREF_KEY_FARM_NAME, profile.farmName)
            .putString(PREF_KEY_OWNER_NAME, profile.ownerName)
            .putString(PREF_KEY_MOBILE, profile.mobileNumber)
            .putString(PREF_KEY_ADDRESS, profile.address)
            .putString(PREF_KEY_LOGO_URI, profile.logoUri)
            .putString(PREF_KEY_LOGO_EMOJI, profile.logoEmoji)
            .putBoolean(PREF_KEY_AUTO_BACKUP, profile.autoBackup)
            .putBoolean(PREF_KEY_DARK_MODE, profile.isDarkMode)
            .apply()
    }

    private val ROOT_ADMIN_EMAIL = "sahariarredwan5@gmail.com"

    private fun isRootAdminEmail(email: String): Boolean =
        email.equals(ROOT_ADMIN_EMAIL, ignoreCase = true)

    private suspend fun isUserBlocked(uid: String): Boolean {
        return try {
            val snap = dbRef?.child(BLOCKED_USERS_NODE)?.child(uid)?.get()?.await()
            snap != null && snap.exists()
        } catch (e: Exception) {
            Log.w(TAG, "Could not check blocked-user status: ${e.message}")
            false
        }
    }

    private fun forceSignOutBlockedUser() {
        Log.w(TAG, "Blocked user detected; forcing sign-out.")
        signOut()
    }

    private fun checkCurrentAuthSession() {
        try {
            val rememberMe = prefs.getBoolean(PREF_KEY_REMEMBER, true)
            val firebaseUser = auth?.currentUser
            if (firebaseUser != null && rememberMe) {
                // Immediately resume session synchronously from local cache
                resumeCachedSession(firebaseUser)

                // Background check if account was blocked/deleted by admin
                dbRef?.child(BLOCKED_USERS_NODE)?.child(firebaseUser.uid)?.get()
                    ?.addOnSuccessListener { blockedSnap ->
                        if (blockedSnap.exists()) {
                            forceSignOutBlockedUser()
                        }
                    }
            } else if (firebaseUser != null && !rememberMe) {
                signOut()
            } else {
                _currentUser.value = null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error checking current auth session: ${e.message}")
            _currentUser.value = null
        }
    }

    private fun resumeCachedSession(firebaseUser: FirebaseUser) {
        try {
            val email = firebaseUser.email ?: ""
            val isAdminEmail = isRootAdminEmail(email)
            val cachedUsername = prefs.getString(PREF_KEY_CACHED_USERNAME, null)
            val defaultRole = if (isAdminEmail) "ADMIN" else (prefs.getString(PREF_KEY_CACHED_ROLE, "WORKER") ?: "WORKER")
            val defaultApproved = if (isAdminEmail) true else prefs.getBoolean(PREF_KEY_CACHED_APPROVED, false)

            val userEntity = UserEntity(
                id = firebaseUser.uid,
                username = cachedUsername ?: firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                email = email,
                phone = prefs.getString(PREF_KEY_CACHED_PHONE, "") ?: "",
                profileImageUri = prefs.getString(PREF_KEY_CACHED_AVATAR, "") ?: "",
                role = defaultRole,
                isApproved = defaultApproved,
                registeredDate = System.currentTimeMillis(),
                passwordHash = "",
                rememberLogin = true,
                isLoggedIn = true
            )
            _currentUser.value = userEntity
            fetchUserFromDb(firebaseUser.uid, userEntity)
        } catch (e: Throwable) {
            Log.w(TAG, "Error resuming cached session: ${e.message}")
            _currentUser.value = null
        }
    }

    private fun fetchUserFromDb(uid: String, fallback: UserEntity) {
        val reference = dbRef ?: return
        reference.child("users").child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val role = snapshot.child("role").getValue(String::class.java) ?: fallback.role
                val isApproved = snapshot.child("isApproved").getValue(Boolean::class.java)
                    ?: snapshot.child("approved").getValue(Boolean::class.java)
                    ?: fallback.isApproved
                val username = snapshot.child("username").getValue(String::class.java) ?: fallback.username
                val email = snapshot.child("email").getValue(String::class.java) ?: fallback.email
                val phone = snapshot.child("phone").getValue(String::class.java) ?: fallback.phone
                val profileImageUri = snapshot.child("profileImageUri").getValue(String::class.java) ?: fallback.profileImageUri

                val resolvedUser = UserEntity(
                    id = uid,
                    username = username,
                    email = email,
                    phone = phone,
                    profileImageUri = profileImageUri,
                    role = role,
                    isApproved = isApproved,
                    registeredDate = snapshot.child("registeredDate").getValue(Long::class.java) ?: fallback.registeredDate,
                    passwordHash = "",
                    rememberLogin = true,
                    isLoggedIn = true
                )
                _currentUser.value = resolvedUser
                prefs.edit()
                    .putString(PREF_KEY_CACHED_USERNAME, username)
                    .putString(PREF_KEY_CACHED_ROLE, role)
                    .putBoolean(PREF_KEY_CACHED_APPROVED, isApproved)
                    .putString(PREF_KEY_CACHED_PHONE, phone)
                    .putString(PREF_KEY_CACHED_AVATAR, profileImageUri)
                    .apply()
            } else {
                // No profile record found. Before treating this as a fresh
                // user and re-creating their record, make sure they weren't
                // deliberately deleted by an admin — otherwise a deleted
                // user's cached fallback would silently resurrect them.
                reference.child(BLOCKED_USERS_NODE).child(uid).get()
                    .addOnSuccessListener { blockedSnap ->
                        if (blockedSnap.exists()) {
                            forceSignOutBlockedUser()
                        } else {
                            reference.child("users").child(uid).setValue(fallback)
                        }
                    }
                    .addOnFailureListener {
                        reference.child("users").child(uid).setValue(fallback)
                    }
            }
        }
    }

    // Raw snapshots kept so the admin list can be recomputed as either the
    // users node or the blockedUsers node changes, independently.
    private var latestRawUsers: List<UserEntity> = emptyList()
    private var latestBlockedIds: Set<String> = emptySet()

    private fun recomputeVisibleUsers() {
        _allUsers.value = latestRawUsers.filterNot { latestBlockedIds.contains(it.id) }
    }

    private fun setupFirebaseRealtimeListeners() {
        val reference = dbRef ?: return
        try {
            // Enable offline disk sync for instantaneous UI data loading
            try {
                reference.child("daily_reports").keepSynced(true)
                reference.child("monthly_expenses").keepSynced(true)
                reference.child("farm_profile").keepSynced(true)
                reference.child("role_permissions").keepSynced(true)
                reference.child("users").keepSynced(true)
                reference.child(BLOCKED_USERS_NODE).keepSynced(true)
            } catch (e: Exception) {
                Log.w(TAG, "keepSynced error: ${e.message}")
            }

            // Listen to All Registered Users for Admin approval & management
            reference.child("users").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userList = mutableListOf<UserEntity>()
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        for (child in snapshot.children) {
                            try {
                                var user = child.getValue(UserEntity::class.java)
                                if (user == null || user.id.isBlank()) {
                                    val id = child.key ?: child.child("id").getValue(String::class.java) ?: ""
                                    val username = child.child("username").getValue(String::class.java) ?: ""
                                    val email = child.child("email").getValue(String::class.java) ?: ""
                                    val phone = child.child("phone").getValue(String::class.java) ?: ""
                                    val role = child.child("role").getValue(String::class.java) ?: "WORKER"
                                    val isApproved = child.child("isApproved").getValue(Boolean::class.java)
                                        ?: child.child("approved").getValue(Boolean::class.java)
                                        ?: false
                                    val profileImageUri = child.child("profileImageUri").getValue(String::class.java) ?: ""
                                    val regDate = child.child("registeredDate").getValue(Long::class.java) ?: System.currentTimeMillis()
                                    if (id.isNotBlank() || email.isNotBlank()) {
                                        user = UserEntity(
                                            id = id,
                                            username = username,
                                            email = email,
                                            phone = phone,
                                            profileImageUri = profileImageUri,
                                            role = role,
                                            isApproved = isApproved,
                                            registeredDate = regDate,
                                            isLoggedIn = true
                                        )
                                    }
                                }
                                if (user != null && user.id.isNotBlank()) {
                                    userList.add(user)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error parsing user child node: ${e.message}")
                            }
                        }
                    }
                    latestRawUsers = userList
                    recomputeVisibleUsers()

                    // Update current user if in list
                    val currentUid = auth?.currentUser?.uid
                    if (currentUid != null) {
                        val matching = userList.find { it.id == currentUid }
                        if (matching != null && !latestBlockedIds.contains(currentUid)) {
                            _currentUser.value = matching
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "users listener cancelled: ${error.message}")
                }
            })

            // Live watch for the current device's own account being blocked
            // (admin deletion) so an already-open session is kicked out
            // immediately, without waiting for the next app restart. Also
            // drives the admin list filter above so a deleted user
            // disappears from the list immediately too.
            reference.child(BLOCKED_USERS_NODE).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    latestBlockedIds = snapshot.children.mapNotNull { it.key }.toSet()
                    recomputeVisibleUsers()

                    val currentUid = auth?.currentUser?.uid ?: return
                    if (snapshot.hasChild(currentUid)) {
                        forceSignOutBlockedUser()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "blockedUsers listener cancelled: ${error.message}")
                }
            })

            // Listen to Real Daily Reports in Firebase
            reference.child("daily_reports").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val reportsList = mutableListOf<DailyReportEntity>()
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        for (child in snapshot.children) {
                            val report = child.getValue(DailyReportEntity::class.java)
                            if (report != null) {
                                reportsList.add(report)
                            }
                        }
                    }
                    _allDailyReports.value = reportsList.sortedByDescending { it.date }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "daily_reports listener cancelled: ${error.message}")
                }
            })

            // Listen to Real Monthly Expenses in Firebase
            reference.child("monthly_expenses").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val expenseList = mutableListOf<MonthlyExpenseEntity>()
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        for (child in snapshot.children) {
                            val expense = child.getValue(MonthlyExpenseEntity::class.java)
                            if (expense != null) {
                                expenseList.add(expense)
                            }
                        }
                    }
                    _allExpenses.value = expenseList.sortedByDescending { it.date }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "monthly_expenses listener cancelled: ${error.message}")
                }
            })

            // Listen to Real Farm Profile in Firebase
            reference.child("farm_profile").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val profile = snapshot.getValue(FarmProfileEntity::class.java)
                        if (profile != null) {
                            _farmProfile.value = profile
                            saveFarmProfileToPrefs(profile)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "farm_profile listener cancelled: ${error.message}")
                }
            })

            // Listen to Role Permissions in Firebase
            reference.child("role_permissions").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val currentMap = _rolePermissions.value.toMutableMap()
                        for (child in snapshot.children) {
                            val config = child.getValue(RolePermissionConfig::class.java)
                            if (config != null) {
                                currentMap[config.roleKey.uppercase()] = config
                                saveRolePermissionToPrefs(config)
                            }
                        }
                        _rolePermissions.value = currentMap
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "role_permissions listener cancelled: ${error.message}")
                }
            })

            // Listen to Shareholders in Firebase
            reference.child("shareholders").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<ShareholderEntity>()
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        for (child in snapshot.children) {
                            try {
                                val keyId = child.key ?: ""
                                val name = child.child("name").getValue(String::class.java) ?: ""
                                val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                                var s: ShareholderEntity? = null
                                try {
                                    s = child.getValue(ShareholderEntity::class.java)
                                } catch (e: Throwable) {
                                    Log.w(TAG, "Direct parsing failed for shareholder: ${e.message}")
                                }
                                if (s == null || s.id.isBlank() || s.name.isBlank()) {
                                    s = ShareholderEntity(id = keyId, name = name, createdAt = createdAt)
                                } else if (s.id.isBlank()) {
                                    s = s.copy(id = keyId)
                                }
                                if (s.name.isNotBlank()) {
                                    list.add(s)
                                }
                            } catch (e: Throwable) {
                                Log.w(TAG, "Error parsing shareholder: ${e.message}")
                            }
                        }
                    }
                    _allShareholders.value = list.distinctBy { it.id }.sortedBy { it.name }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "shareholders listener cancelled: ${error.message}")
                }
            })

            // Listen to Shareholder Payments in Firebase
            reference.child("shareholder_payments").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<ShareholderPaymentEntity>()
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        for (child in snapshot.children) {
                            try {
                                val keyId = child.key ?: ""
                                var p: ShareholderPaymentEntity? = null
                                try {
                                    p = child.getValue(ShareholderPaymentEntity::class.java)
                                } catch (e: Throwable) {
                                    Log.w(TAG, "Direct parsing failed for shareholder payment: ${e.message}")
                                }
                                if (p == null || p.id.isBlank()) {
                                    val sId = child.child("shareholderId").getValue(String::class.java) ?: ""
                                    val sName = child.child("shareholderName").getValue(String::class.java) ?: ""
                                    val date = child.child("date").getValue(String::class.java) ?: ""
                                    val amount = (child.child("amount").getValue(Double::class.java))
                                        ?: (child.child("amount").getValue(Long::class.java)?.toDouble()) ?: 0.0
                                    val method = child.child("paymentMethod").getValue(String::class.java) ?: "Cash"
                                    val note = child.child("note").getValue(String::class.java) ?: ""
                                    val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                                    p = ShareholderPaymentEntity(
                                        id = keyId,
                                        shareholderId = sId,
                                        shareholderName = sName,
                                        date = date,
                                        amount = amount,
                                        paymentMethod = method,
                                        note = note,
                                        createdAt = createdAt
                                    )
                                } else if (p.id.isBlank()) {
                                    p = p.copy(id = keyId)
                                }
                                if (p.shareholderName.isNotBlank() || p.shareholderId.isNotBlank()) {
                                    list.add(p)
                                }
                            } catch (e: Throwable) {
                                Log.w(TAG, "Error parsing shareholder payment: ${e.message}")
                            }
                        }
                    }
                    _allShareholderPayments.value = list.distinctBy { it.id }.sortedByDescending { it.createdAt }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "shareholder_payments listener cancelled: ${error.message}")
                }
            })

            // Listen to Staff in Firebase
            reference.child("staff").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<StaffEntity>()
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        for (child in snapshot.children) {
                            try {
                                val keyId = child.key ?: ""
                                val name = child.child("name").getValue(String::class.java) ?: ""
                                val phone = child.child("phone").getValue(String::class.java) ?: ""
                                val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                                var s: StaffEntity? = null
                                try {
                                    s = child.getValue(StaffEntity::class.java)
                                } catch (e: Throwable) {
                                    Log.w(TAG, "Direct parsing failed for staff: ${e.message}")
                                }
                                if (s == null || s.id.isBlank() || s.name.isBlank()) {
                                    s = StaffEntity(id = keyId, name = name, phone = phone, createdAt = createdAt)
                                } else if (s.id.isBlank()) {
                                    s = s.copy(id = keyId)
                                }
                                if (s.name.isNotBlank()) {
                                    list.add(s)
                                }
                            } catch (e: Throwable) {
                                Log.w(TAG, "Error parsing staff: ${e.message}")
                            }
                        }
                    }
                    _allStaff.value = list.distinctBy { it.id }.sortedBy { it.name }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "staff listener cancelled: ${error.message}")
                }
            })

            // Listen to Staff Payments in Firebase
            reference.child("staff_payments").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<StaffPaymentEntity>()
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        for (child in snapshot.children) {
                            try {
                                val keyId = child.key ?: ""
                                var p: StaffPaymentEntity? = null
                                try {
                                    p = child.getValue(StaffPaymentEntity::class.java)
                                } catch (e: Throwable) {
                                    Log.w(TAG, "Direct parsing failed for staff payment: ${e.message}")
                                }
                                if (p == null || p.id.isBlank()) {
                                    val sId = child.child("staffId").getValue(String::class.java) ?: ""
                                    val sName = child.child("staffName").getValue(String::class.java) ?: ""
                                    val date = child.child("date").getValue(String::class.java) ?: ""
                                    val amount = (child.child("amount").getValue(Double::class.java))
                                        ?: (child.child("amount").getValue(Long::class.java)?.toDouble()) ?: 0.0
                                    val method = child.child("paymentMethod").getValue(String::class.java) ?: "Cash"
                                    val note = child.child("note").getValue(String::class.java) ?: ""
                                    val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                                    val updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: createdAt
                                    p = StaffPaymentEntity(
                                        id = keyId,
                                        staffId = sId,
                                        staffName = sName,
                                        date = date,
                                        amount = amount,
                                        paymentMethod = method,
                                        note = note,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                } else if (p.id.isBlank()) {
                                    p = p.copy(id = keyId)
                                }
                                if (p.staffName.isNotBlank() || p.staffId.isNotBlank()) {
                                    list.add(p)
                                }
                            } catch (e: Throwable) {
                                Log.w(TAG, "Error parsing staff payment: ${e.message}")
                            }
                        }
                    }
                    _allStaffPayments.value = list.distinctBy { it.id }.sortedByDescending { it.createdAt }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "staff_payments listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring Firebase Realtime Database listeners: ${e.message}", e)
        }
    }

    // -------------------------------------------------------------
    // REAL FIREBASE AUTHENTICATION METHODS
    // -------------------------------------------------------------

    suspend fun signInWithEmail(email: String, pass: String, rememberMe: Boolean = true): Result<UserEntity> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
            ?: return@withContext Result.failure(Exception("Firebase Auth ইনিশিয়ালাইজ করা যায়নি। অনুগ্রহ করে ইন্টারনেট ও ফায়ারবেস কনফিগারেশন চেক করুন।"))

        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user
                ?: return@withContext Result.failure(Exception("ইউজার ডাটা পাওয়া যায়নি।"))

            val userEmail = user.email ?: email.trim()
            val isAdminEmail = isRootAdminEmail(userEmail)

            // Reject login for accounts an admin has removed, even though
            // Firebase Authentication itself still recognizes the credentials.
            if (isUserBlocked(user.uid)) {
                firebaseAuth.signOut()
                return@withContext Result.failure(Exception("এই অ্যাকাউন্টটি অ্যাডমিন কর্তৃক মুছে ফেলা হয়েছে। আর প্রবেশ করা যাবে না।"))
            }

            // Try to load user document from realtime db
            var userEntity: UserEntity? = null
            try {
                val snap = dbRef?.child("users")?.child(user.uid)?.get()?.await()
                if (snap != null && snap.exists()) {
                    userEntity = snap.getValue(UserEntity::class.java)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch user record from DB: ${e.message}")
            }

            if (userEntity == null) {
                userEntity = UserEntity(
                    id = user.uid,
                    username = user.displayName ?: userEmail.substringBefore("@"),
                    email = userEmail,
                    phone = "",
                    role = if (isAdminEmail) "ADMIN" else "WORKER",
                    isApproved = isAdminEmail,
                    registeredDate = System.currentTimeMillis(),
                    passwordHash = "",
                    rememberLogin = rememberMe,
                    isLoggedIn = true
                )
                dbRef?.child("users")?.child(user.uid)?.setValue(userEntity)?.await()
            }

            _currentUser.value = userEntity

            if (rememberMe) {
                prefs.edit()
                    .putBoolean(PREF_KEY_REMEMBER, true)
                    .putString(PREF_KEY_REMEMBERED_EMAIL, userEmail)
                    .putString(PREF_KEY_CACHED_USERNAME, userEntity.username)
                    .putString(PREF_KEY_CACHED_ROLE, userEntity.role)
                    .putBoolean(PREF_KEY_CACHED_APPROVED, userEntity.isApproved)
                    .putString(PREF_KEY_CACHED_PHONE, userEntity.phone)
                    .putString(PREF_KEY_CACHED_AVATAR, userEntity.profileImageUri)
                    .apply()
            } else {
                prefs.edit()
                    .putBoolean(PREF_KEY_REMEMBER, false)
                    .remove(PREF_KEY_REMEMBERED_EMAIL)
                    .remove(PREF_KEY_CACHED_USERNAME)
                    .remove(PREF_KEY_CACHED_ROLE)
                    .remove(PREF_KEY_CACHED_APPROVED)
                    .apply()
            }

            Result.success(userEntity)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase signIn failed: ${e.message}", e)
            val banglaMsg = mapAuthErrorToBangla(e.message ?: "")
            Result.failure(Exception(banglaMsg))
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, fullName: String, phone: String = "", rememberMe: Boolean = true): Result<UserEntity> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
            ?: return@withContext Result.failure(Exception("Firebase Auth ইনিশিয়ালাইজ করা যায়নি।"))

        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user
                ?: return@withContext Result.failure(Exception("অ্যাকাউন্ট তৈরি সফল হয়নি।"))

            // Update display name
            if (fullName.isNotBlank()) {
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName.trim())
                        .build()
                    user.updateProfile(profileUpdates).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not update user display name: ${e.message}")
                }
            }

            val userEmail = user.email ?: email.trim()
            val isAdminEmail = isRootAdminEmail(userEmail)
            val role = if (isAdminEmail) "ADMIN" else "WORKER"
            val isApproved = isAdminEmail // Only default admin is auto approved, all other registrations require admin approval

            val userEntity = UserEntity(
                id = user.uid,
                username = if (fullName.isNotBlank()) fullName.trim() else userEmail.substringBefore("@"),
                email = userEmail,
                phone = phone.trim(),
                role = role,
                isApproved = isApproved,
                registeredDate = System.currentTimeMillis(),
                passwordHash = "",
                rememberLogin = rememberMe,
                isLoggedIn = true
            )

            val userMap = mapOf(
                "id" to user.uid,
                "username" to userEntity.username,
                "email" to userEntity.email,
                "phone" to userEntity.phone,
                "role" to userEntity.role,
                "isApproved" to userEntity.isApproved,
                "approved" to userEntity.isApproved,
                "profileImageUri" to userEntity.profileImageUri,
                "registeredDate" to userEntity.registeredDate,
                "rememberLogin" to rememberMe,
                "isLoggedIn" to true
            )

            // Save to users database
            dbRef?.child("users")?.child(user.uid)?.setValue(userMap)?.await()

            // Update local user state immediately
            latestRawUsers = (latestRawUsers.filterNot { it.id == userEntity.id } + userEntity)
            recomputeVisibleUsers()

            _currentUser.value = userEntity

            if (rememberMe) {
                prefs.edit()
                    .putBoolean(PREF_KEY_REMEMBER, true)
                    .putString(PREF_KEY_REMEMBERED_EMAIL, userEmail)
                    .putString(PREF_KEY_CACHED_USERNAME, userEntity.username)
                    .putString(PREF_KEY_CACHED_ROLE, userEntity.role)
                    .putBoolean(PREF_KEY_CACHED_APPROVED, userEntity.isApproved)
                    .putString(PREF_KEY_CACHED_PHONE, userEntity.phone)
                    .putString(PREF_KEY_CACHED_AVATAR, userEntity.profileImageUri)
                    .apply()
            } else {
                prefs.edit()
                    .putBoolean(PREF_KEY_REMEMBER, false)
                    .remove(PREF_KEY_REMEMBERED_EMAIL)
                    .apply()
            }

            Result.success(userEntity)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase signUp failed: ${e.message}", e)
            val banglaMsg = mapAuthErrorToBangla(e.message ?: "")
            Result.failure(Exception(banglaMsg))
        }
    }

    suspend fun adminAddUser(user: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val id = if (user.id.isBlank()) "user_${System.currentTimeMillis()}" else user.id
            val finalUser = user.copy(id = id)
            dbRef?.child("users")?.child(id)?.setValue(finalUser)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding user: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateUser(user: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dbRef?.child("users")?.child(user.id)?.setValue(user)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Block first so a currently-open session on that account (the
            // live listener below, or their next login) is denied even
            // though we cannot delete their Firebase Authentication login
            // from the client app.
            dbRef?.child(BLOCKED_USERS_NODE)?.child(userId)?.setValue(System.currentTimeMillis())?.await()
            dbRef?.child("users")?.child(userId)?.removeValue()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateCurrentUserProfile(
        name: String,
        phone: String,
        profileImageUri: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("কোনো ইউজার লগইন নেই"))
        try {
            val updated = current.copy(
                username = name.trim(),
                phone = phone.trim(),
                profileImageUri = profileImageUri ?: current.profileImageUri
            )
            _currentUser.value = updated
            if (current.id.isNotBlank()) {
                dbRef?.child("users")?.child(current.id)?.setValue(updated)?.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
            ?: return@withContext Result.failure(Exception("Firebase Auth ইনিশিয়ালাইজ করা যায়নি।"))

        try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase sendPasswordResetEmail failed: ${e.message}", e)
            val banglaMsg = mapAuthErrorToBangla(e.message ?: "")
            Result.failure(Exception(banglaMsg))
        }
    }

    suspend fun changePassword(newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val firebaseUser = auth?.currentUser
            ?: return@withContext Result.failure(Exception("কোনো সক্রিয় লগইন সেশন নেই। পুনরায় লগইন করুন।"))

        try {
            firebaseUser.updatePassword(newPass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase updatePassword failed: ${e.message}", e)
            val banglaMsg = mapAuthErrorToBangla(e.message ?: "")
            Result.failure(Exception(banglaMsg))
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out: ${e.message}")
        }
        _currentUser.value = null
        prefs.edit()
            .remove(PREF_KEY_CACHED_USERNAME)
            .remove(PREF_KEY_CACHED_ROLE)
            .remove(PREF_KEY_CACHED_APPROVED)
            .remove(PREF_KEY_CACHED_PHONE)
            .remove(PREF_KEY_CACHED_AVATAR)
            .apply()
    }

    fun isUserLoggedIn(): Boolean {
        return auth?.currentUser != null
    }

    fun isUserLoggedInAndApproved(): Boolean {
        val firebaseUser = auth?.currentUser ?: return false
        val rememberMe = prefs.getBoolean(PREF_KEY_REMEMBER, true)
        if (!rememberMe) return false

        val user = _currentUser.value
        if (user != null) {
            return user.isApprovedUser()
        }
        val email = firebaseUser.email ?: ""
        if (isRootAdminEmail(email)) return true
        return prefs.getBoolean(PREF_KEY_CACHED_APPROVED, false)
    }

    fun isRememberLoginEnabled(): Boolean = prefs.getBoolean(PREF_KEY_REMEMBER, true)

    fun getRememberedEmail(): String = prefs.getString(PREF_KEY_REMEMBERED_EMAIL, "") ?: ""

    fun setRememberLogin(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_KEY_REMEMBER, enabled).apply()
    }

    private fun mapAuthErrorToBangla(error: String): String {
        return when {
            error.contains("password", ignoreCase = true) && error.contains("least 6", ignoreCase = true) ->
                "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"
            error.contains("email address is already in use", ignoreCase = true) ->
                "এই ইমেইল ঠিকানাটি দিয়ে ইতিমধ্যেই একটি অ্যাকাউন্ট খোলা রয়েছে।"
            error.contains("badly formatted", ignoreCase = true) || error.contains("invalid email", ignoreCase = true) ->
                "দয়া করে একটি সঠিক ও বৈধ ইমেইল ঠিকানা লিখুন।"
            error.contains("user-not-found", ignoreCase = true) || error.contains("no user record", ignoreCase = true) ->
                "এই ইমেইলের কোনো অ্যাকাউন্ট পাওয়া যায়নি।"
            error.contains("wrong-password", ignoreCase = true) || error.contains("invalid-credential", ignoreCase = true) || error.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                "ইমেইল বা পাসওয়ার্ড সঠিক নয়। দয়া করে আবার চেষ্টা করুন।"
            error.contains("network", ignoreCase = true) ->
                "ইন্টারনেট সংযোগ সমস্যা। আপনার ডাটা বা ওয়াইফাই চেক করুন।"
            error.contains("API key not valid", ignoreCase = true) ->
                "ফায়ারবেস API Key সঠিকভাবে কনফিগার করা নেই। অনুগ্রহ করে আপনার আসল google-services.json ফাইলটি অ্যাপে যুক্ত করুন।"
            else ->
                "লগইন ত্রুটি: $error"
        }
    }

    // -------------------------------------------------------------
    // REAL FIREBASE REALTIME DATABASE DATA OPERATIONS
    // -------------------------------------------------------------

    // Daily Report Operations
    suspend fun saveDailyReport(report: DailyReportEntity): Long = withContext(Dispatchers.IO) {
        val targetId = if (report.id <= 0L) System.currentTimeMillis() else report.id
        val finalReport = report.copy(id = targetId, updatedAt = System.currentTimeMillis())

        val currentList = _allDailyReports.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == finalReport.id }
        if (index >= 0) currentList[index] = finalReport else currentList.add(finalReport)

        val baselineStock = _farmProfile.value.initialOpeningStock

        // Recalculate full stock ledger across all records
        val ledger = StockCalculationService.calculateSequentialStockLedger(currentList, baselineStock)
        val recalculatedList = currentList.map { r ->
            val correctStock = ledger[r.date]?.closingStock ?: r.currentStock
            if (r.currentStock != correctStock) r.copy(currentStock = correctStock) else r
        }.sortedByDescending { it.date }

        _allDailyReports.value = recalculatedList

        val reference = dbRef
        if (reference != null) {
            try {
                val updatedPrimary = recalculatedList.find { it.id == targetId } ?: finalReport
                reference.child("daily_reports").child(targetId.toString()).setValue(updatedPrimary).await()

                // Cascade update any subsequent dates whose stock changed
                val subsequentUpdated = recalculatedList.filter { it.date > finalReport.date && it.id != targetId }
                for (sub in subsequentUpdated) {
                    try {
                        reference.child("daily_reports").child(sub.id.toString()).child("currentStock").setValue(sub.currentStock)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not cascade update stock for ${sub.date}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving daily report to Firebase: ${e.message}", e)
            }
        }
        targetId
    }

    private fun updateLocalDailyReportsList(report: DailyReportEntity) {
        val currentList = _allDailyReports.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == report.id }
        if (index >= 0) currentList[index] = report else currentList.add(0, report)
        val baselineStock = _farmProfile.value.initialOpeningStock
        val ledger = StockCalculationService.calculateSequentialStockLedger(currentList, baselineStock)
        _allDailyReports.value = currentList.map { r ->
            val correctStock = ledger[r.date]?.closingStock ?: r.currentStock
            if (r.currentStock != correctStock) r.copy(currentStock = correctStock) else r
        }.sortedByDescending { it.date }
    }

    suspend fun getDailyReportById(id: Long): DailyReportEntity? = withContext(Dispatchers.IO) {
        _allDailyReports.value.find { it.id == id } ?: try {
            val snapshot = dbRef?.child("daily_reports")?.child(id.toString())?.get()?.await()
            snapshot?.getValue(DailyReportEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteDailyReportById(id: Long) = withContext(Dispatchers.IO) {
        val remaining = _allDailyReports.value.filter { it.id != id }
        val baselineStock = _farmProfile.value.initialOpeningStock
        val ledger = StockCalculationService.calculateSequentialStockLedger(remaining, baselineStock)
        val recalculatedList = remaining.map { r ->
            val correctStock = ledger[r.date]?.closingStock ?: r.currentStock
            if (r.currentStock != correctStock) r.copy(currentStock = correctStock) else r
        }.sortedByDescending { it.date }

        _allDailyReports.value = recalculatedList

        val reference = dbRef
        if (reference != null) {
            try {
                reference.child("daily_reports").child(id.toString()).removeValue().await()
                for (r in recalculatedList) {
                    reference.child("daily_reports").child(r.id.toString()).child("currentStock").setValue(r.currentStock)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting report from Firebase: ${e.message}", e)
            }
        }
    }

    suspend fun getPreviousStock(date: String): Int = withContext(Dispatchers.IO) {
        val baselineStock = _farmProfile.value.initialOpeningStock
        StockCalculationService.calculateOpeningStockForDate(_allDailyReports.value, date, baselineStock)
    }

    suspend fun getLatestFlockCount(): Int = withContext(Dispatchers.IO) {
        val latest = _allDailyReports.value.firstOrNull()
        if (latest != null) {
            (latest.currentBirds - latest.deadBirds).coerceAtLeast(0)
        } else {
            0
        }
    }

    // Monthly Expense Operations
    suspend fun saveMonthlyExpense(expense: MonthlyExpenseEntity): Long = withContext(Dispatchers.IO) {
        val targetId = if (expense.id <= 0L) System.currentTimeMillis() else expense.id
        val finalExpense = expense.copy(id = targetId, updatedAt = System.currentTimeMillis())

        val reference = dbRef
        if (reference != null) {
            try {
                reference.child("monthly_expenses").child(targetId.toString()).setValue(finalExpense).await()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving expense to Firebase: ${e.message}", e)
                updateLocalExpenseList(finalExpense)
            }
        } else {
            updateLocalExpenseList(finalExpense)
        }
        targetId
    }

    private fun updateLocalExpenseList(expense: MonthlyExpenseEntity) {
        val currentList = _allExpenses.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == expense.id }
        if (index >= 0) currentList[index] = expense else currentList.add(0, expense)
        _allExpenses.value = currentList.sortedByDescending { it.date }
    }

    suspend fun getExpenseById(id: Long): MonthlyExpenseEntity? = withContext(Dispatchers.IO) {
        _allExpenses.value.find { it.id == id } ?: try {
            val snapshot = dbRef?.child("monthly_expenses")?.child(id.toString())?.get()?.await()
            snapshot?.getValue(MonthlyExpenseEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteExpenseById(id: Long) = withContext(Dispatchers.IO) {
        try {
            dbRef?.child("monthly_expenses")?.child(id.toString())?.removeValue()?.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting expense from Firebase: ${e.message}", e)
        }
        val currentList = _allExpenses.value.filter { it.id != id }
        _allExpenses.value = currentList
    }

    // Farm Profile & Settings
    suspend fun updateFarmProfile(profile: FarmProfileEntity) = withContext(Dispatchers.IO) {
        val updated = profile.copy(lastSyncTime = System.currentTimeMillis())
        _farmProfile.value = updated
        saveFarmProfileToPrefs(updated)
        try {
            dbRef?.child("farm_profile")?.setValue(updated)?.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating farm profile in Firebase: ${e.message}", e)
        }
    }

    suspend fun updateDarkMode(isDark: Boolean) = withContext(Dispatchers.IO) {
        val current = _farmProfile.value
        val updated = current.copy(isDarkMode = isDark, lastSyncTime = System.currentTimeMillis())
        _farmProfile.value = updated
        prefs.edit().putBoolean(PREF_KEY_DARK_MODE, isDark).apply()
        try {
            dbRef?.child("farm_profile")?.child("isDarkMode")?.setValue(isDark)
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    suspend fun updateAutoBackup(enabled: Boolean) = withContext(Dispatchers.IO) {
        val current = _farmProfile.value
        val updated = current.copy(autoBackup = enabled, lastSyncTime = System.currentTimeMillis())
        _farmProfile.value = updated
        try {
            dbRef?.child("farm_profile")?.child("autoBackup")?.setValue(enabled)
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    // CSV Export helper
    suspend fun exportDailyReportsToCsv(reports: List<DailyReportEntity>): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "kazi_agrotech_daily_reports_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.append("তারিখ,বর্তমান মুরগী,মৃত মুরগী,ডিম উৎপাদন,বিক্রয় (ডিম),ডিমের দাম (৳),মোট বিক্রয় (৳),মন্তব্য\n")
            for (r in reports) {
                writer.append("${r.date},${r.currentBirds},${r.deadBirds},${r.eggProduction},${r.eggSold},${r.eggPrice},${r.totalSale},\"${r.remarks.replace("\"", "\"\"")}\"\n")
            }
        }
        file
    }

    suspend fun exportMonthlyExpensesToCsv(expenses: List<MonthlyExpenseEntity>): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "kazi_agrotech_expenses_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.append("তারিখ,খাদ্য/ফিড (৳),ঔষধ ও ভ্যাকসিন (৳),স্টাফ বাজার (৳),স্টাফ বেতন (৳),গাড়ি মেরামত (৳),সম্পদ ক্রয় (৳),বিদ্যুৎ বিল (৳),অন্যান্য (৳),মোট ব্যয় (৳),মন্তব্য\n")
            for (e in expenses) {
                writer.append("${e.date},${e.feedCost},${e.medicineCost},${e.staffMarket},${e.staffSalary},${e.vehicleRepair},${e.assets},${e.electricityBill},${e.otherExpense},${e.totalExpense},\"${e.remarks.replace("\"", "\"\"")}\"\n")
            }
        }
        file
    }

    suspend fun updateRolePermissions(config: RolePermissionConfig) = withContext(Dispatchers.IO) {
        val currentMap = _rolePermissions.value.toMutableMap()
        currentMap[config.roleKey.uppercase()] = config
        _rolePermissions.value = currentMap
        saveRolePermissionToPrefs(config)

        try {
            dbRef?.child("role_permissions")?.child(config.roleKey.uppercase())?.setValue(config)?.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating role permissions in Firebase: ${e.message}", e)
        }
    }

    // -------------------------------------------------------------
    // SHAREHOLDER CRUD OPERATIONS (ADMIN ONLY)
    // -------------------------------------------------------------

    suspend fun addShareholder(
        name: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            withContext(Dispatchers.Main) { onError("শেয়ারহোল্ডারের নাম লিখুন") }
            return@withContext
        }

        val existing = _allShareholders.value.any { it.name.equals(trimmedName, ignoreCase = true) }
        if (existing) {
            withContext(Dispatchers.Main) { onError("এই নামের শেয়ারহোল্ডার ইতোমধ্যে রয়েছে") }
            return@withContext
        }

        val id = dbRef?.child("shareholders")?.push()?.key ?: System.currentTimeMillis().toString()
        val shareholder = ShareholderEntity(
            id = id,
            name = trimmedName,
            createdAt = System.currentTimeMillis()
        )

        try {
            dbRef?.child("shareholders")?.child(id)?.setValue(shareholder)?.await()
            val current = _allShareholders.value.filter { it.id != id }
            _allShareholders.value = (current + shareholder).distinctBy { it.id }.sortedBy { it.name }
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Throwable) {
            Log.e(TAG, "Error adding shareholder: ${e.message}", e)
            withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "শেয়ারহোল্ডার যোগ করা সম্ভব হয়নি") }
        }
    }

    suspend fun updateShareholder(
        id: String,
        name: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            withContext(Dispatchers.Main) { onError("শেয়ারহোল্ডারের নাম লিখুন") }
            return@withContext
        }

        try {
            val current = _allShareholders.value.find { it.id == id } ?: ShareholderEntity(id = id, name = trimmedName)
            val updated = current.copy(name = trimmedName)
            dbRef?.child("shareholders")?.child(id)?.setValue(updated)?.await()

            // Also update shareholderName in all payments associated with this shareholder
            val affectedPayments = _allShareholderPayments.value.filter { it.shareholderId == id }
            if (affectedPayments.isNotEmpty()) {
                val updates = mutableMapOf<String, Any>()
                affectedPayments.forEach { payment ->
                    updates["shareholder_payments/${payment.id}/shareholderName"] = trimmedName
                }
                dbRef?.updateChildren(updates)?.await()
            }

            val updatedList = _allShareholders.value.map { if (it.id == id) updated else it }.distinctBy { it.id }.sortedBy { it.name }
            _allShareholders.value = updatedList
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Throwable) {
            Log.e(TAG, "Error updating shareholder: ${e.message}", e)
            withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "শেয়ারহোল্ডার পরিবর্তন করা সম্ভব হয়নি") }
        }
    }

    suspend fun deleteShareholder(
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            dbRef?.child("shareholders")?.child(id)?.removeValue()?.await()
            val updatedList = _allShareholders.value.filter { it.id != id }
            _allShareholders.value = updatedList
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Throwable) {
            Log.e(TAG, "Error deleting shareholder: ${e.message}", e)
            withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "শেয়ারহোল্ডার মুছে ফেলা সম্ভব হয়নি") }
        }
    }

    // -------------------------------------------------------------
    // SHAREHOLDER PAYMENT CRUD OPERATIONS
    // -------------------------------------------------------------

    suspend fun addShareholderPayment(
        payment: ShareholderPaymentEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (payment.shareholderName.isBlank()) {
            withContext(Dispatchers.Main) { onError("শেয়ারহোল্ডারের নাম নির্বাচন করুন") }
            return@withContext
        }
        if (payment.amount <= 0) {
            withContext(Dispatchers.Main) { onError("সঠিক টাকার পরিমাণ দিন") }
            return@withContext
        }
        if (payment.date.isBlank()) {
            withContext(Dispatchers.Main) { onError("তারিখ নির্বাচন করুন") }
            return@withContext
        }

        val id = if (payment.id.isNotBlank()) payment.id else (dbRef?.child("shareholder_payments")?.push()?.key ?: System.currentTimeMillis().toString())
        val newPayment = payment.copy(id = id)

        try {
            dbRef?.child("shareholder_payments")?.child(id)?.setValue(newPayment)?.await()
            val currentList = _allShareholderPayments.value.filter { it.id != id }
            _allShareholderPayments.value = (listOf(newPayment) + currentList).distinctBy { it.id }.sortedByDescending { it.createdAt }
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Throwable) {
            Log.e(TAG, "Error saving shareholder payment: ${e.message}", e)
            withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "পেমেন্ট সংরক্ষণ করা সম্ভব হয়নি") }
        }
    }

    suspend fun updateShareholderPayment(
        payment: ShareholderPaymentEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (payment.shareholderName.isBlank()) {
            withContext(Dispatchers.Main) { onError("শেয়ারহোল্ডারের নাম নির্বাচন করুন") }
            return@withContext
        }
        if (payment.amount <= 0) {
            withContext(Dispatchers.Main) { onError("সঠিক টাকার পরিমাণ দিন") }
            return@withContext
        }
        if (payment.date.isBlank()) {
            withContext(Dispatchers.Main) { onError("তারিখ নির্বাচন করুন") }
            return@withContext
        }

        try {
            dbRef?.child("shareholder_payments")?.child(payment.id)?.setValue(payment)?.await()
            val updatedList = _allShareholderPayments.value.map { if (it.id == payment.id) payment else it }.distinctBy { it.id }.sortedByDescending { it.createdAt }
            _allShareholderPayments.value = updatedList
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Throwable) {
            Log.e(TAG, "Error updating shareholder payment: ${e.message}", e)
            withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "পেমেন্ট পরিবর্তন করা সম্ভব হয়নি") }
        }
    }

    suspend fun deleteShareholderPayment(
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            dbRef?.child("shareholder_payments")?.child(id)?.removeValue()?.await()
            val updatedList = _allShareholderPayments.value.filter { it.id != id }
            _allShareholderPayments.value = updatedList
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Throwable) {
            Log.e(TAG, "Error deleting shareholder payment: ${e.message}", e)
            withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "পেমেন্ট মুছে ফেলা সম্ভব হয়নি") }
        }
    }

    // -------------------------------------------------------------
    // STAFF CRUD OPERATIONS
    // -------------------------------------------------------------

    suspend fun addStaff(
        name: String,
        phone: String,
        onSuccess: (StaffEntity) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        if (trimmedName.isBlank()) {
            withContext(Dispatchers.Main) { onError("স্টাফের নাম লিখুন") }
            return@withContext
        }
        if (trimmedPhone.isBlank()) {
            withContext(Dispatchers.Main) { onError("মোবাইল নম্বর লিখুন") }
            return@withContext
        }

        val id = dbRef?.child("staff")?.push()?.key ?: System.currentTimeMillis().toString()
        val staff = StaffEntity(id = id, name = trimmedName, phone = trimmedPhone, createdAt = System.currentTimeMillis())

        try {
            dbRef?.child("staff")?.child(id)?.setValue(staff)?.await()
            val currentList = _allStaff.value.filter { it.id != id }
            _allStaff.value = (currentList + staff).distinctBy { it.id }.sortedBy { it.name }
            withContext(Dispatchers.Main) { onSuccess(staff) }
        } catch (e: Throwable) {
            Log.e(TAG, "Error saving staff: ${e.message}", e)
            withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "স্টাফ সংরক্ষণ করা সম্ভব হয়নি") }
        }
    }

    suspend fun updateStaff(
        id: String,
        name: String,
        phone: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        if (trimmedName.isBlank()) {
            withContext(Dispatchers.Main) { onError("স্টাফের নাম লিখুন") }
            return@withContext
        }
        if (trimmedPhone.isBlank()) {
            withContext(Dispatchers.Main) { onError("মোবাইল নম্বর লিখুন") }
            return@withContext
        }

        try {
            val current = _allStaff.value.find { it.id == id } ?: StaffEntity(id = id, name = trimmedName, phone = trimmedPhone)
            val updated = current.copy(name = trimmedName, phone = trimmedPhone)
            dbRef?.child("staff")?.child(id)?.setValue(updated)?.await()

            // Also update staffName in all payments associated with this staff
            val affectedPayments = _allStaffPayments.value.filter { it.staffId == id }
            if (affectedPayments.isNotEmpty()) {
                val updates = mutableMapOf<String, Any>()
                affectedPayments.forEach { payment ->
                    updates["staff_payments/${payment.id}/staffName"] = trimmedName
                }
                dbRef?.updateChildren(updates)?.await()
            }

            val updatedList = _allStaff.value.map { if (it.id == id) updated else it }.distinctBy { it.id }.sortedBy { it.name }
            _allStaff.value = updatedList
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Throwable) {
            Log.e(TAG, "Error updating staff: ${e.message}", e)
            withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "স্টাফের তথ্য পরিবর্তন করা সম্ভব হয়নি") }
        }
    }

    suspend fun deleteStaff(
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            dbRef?.child("staff")?.child(id)?.removeValue()?.await()
            val updatedList = _allStaff.value.filter { it.id != id }
            _allStaff.value = updatedList
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Throwable) {
            Log.e(TAG, "Error deleting staff: ${e.message}", e)
            withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "স্টাফ মুছে ফেলা সম্ভব হয়নি") }
        }
    }

    // -------------------------------------------------------------
    // STAFF PAYMENT CRUD OPERATIONS
    // -------------------------------------------------------------

    suspend fun addStaffPayment(
        payment: StaffPaymentEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (payment.staffName.isBlank()) {
            withContext(Dispatchers.Main) { onError("স্টাফের নাম নির্বাচন করুন") }
            return@withContext
        }
        if (payment.amount <= 0) {
            withContext(Dispatchers.Main) { onError("সঠিক টাকার পরিমাণ দিন") }
            return@withContext
        }
        if (payment.date.isBlank()) {
            withContext(Dispatchers.Main) { onError("তারিখ নির্বাচন করুন") }
            return@withContext
        }

        val id = if (payment.id.isNotBlank()) payment.id else (dbRef?.child("staff_payments")?.push()?.key ?: System.currentTimeMillis().toString())
        val newPayment = payment.copy(id = id, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())

        try {
            dbRef?.child("staff_payments")?.child(id)?.setValue(newPayment)?.await()
            val currentList = _allStaffPayments.value.filter { it.id != id }
            _allStaffPayments.value = (listOf(newPayment) + currentList).distinctBy { it.id }.sortedByDescending { it.createdAt }
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Throwable) {
            Log.e(TAG, "Error saving staff payment: ${e.message}", e)
            withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "স্টাফ পেমেন্ট সংরক্ষণ করা সম্ভব হয়নি") }
        }
    }

    suspend fun updateStaffPayment(
        payment: StaffPaymentEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (payment.staffName.isBlank()) {
            withContext(Dispatchers.Main) { onError("স্টাফের নাম নির্বাচন করুন") }
            return@withContext
        }
        if (payment.amount <= 0) {
            withContext(Dispatchers.Main) { onError("সঠিক টাকার পরিমাণ দিন") }
            return@withContext
        }
        if (payment.date.isBlank()) {
            withContext(Dispatchers.Main) { onError("তারিখ নির্বাচন করুন") }
            return@withContext
        }

        val updatedPayment = payment.copy(updatedAt = System.currentTimeMillis())

        try {
            dbRef?.child("staff_payments")?.child(payment.id)?.setValue(updatedPayment)?.await()
            val updatedList = _allStaffPayments.value.map { if (it.id == payment.id) updatedPayment else it }.distinctBy { it.id }.sortedByDescending { it.createdAt }
            _allStaffPayments.value = updatedList
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Throwable) {
            Log.e(TAG, "Error updating staff payment: ${e.message}", e)
            withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "স্টাফ পেমেন্ট পরিবর্তন করা সম্ভব হয়নি") }
        }
    }

    suspend fun deleteStaffPayment(
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            dbRef?.child("staff_payments")?.child(id)?.removeValue()?.await()
            val updatedList = _allStaffPayments.value.filter { it.id != id }
            _allStaffPayments.value = updatedList
            withContext(Dispatchers.Main) { onSuccess() }
        } catch (e: Throwable) {
            Log.e(TAG, "Error deleting staff payment: ${e.message}", e)
            withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "স্টাফ পেমেন্ট মুছে ফেলা সম্ভব হয়নি") }
        }
    }

    // -------------------------------------------------------------
    // RESTORE & BACKUP
    // -------------------------------------------------------------

    suspend fun restoreCompleteBackup(content: SheetsBackupData) = withContext(Dispatchers.IO) {
        val reference = dbRef ?: throw Exception("Firebase Realtime Database সংযোগ পাওয়া যায়নি।")

        // 1. Recalculate complete sequential stock ledger from source transactions
        val sortedReports = content.dailyReports.sortedBy { it.date }
        val baselineStock = content.farmProfile?.initialOpeningStock ?: _farmProfile.value.initialOpeningStock
        val stockLedger = StockCalculationService.calculateSequentialStockLedger(sortedReports, baselineStock)

        val finalizedReports = sortedReports.map { report ->
            val ledgerRecord = stockLedger[report.date]
            report.copy(currentStock = ledgerRecord?.closingStock ?: report.currentStock)
        }

        // 2. Atomic/Batch write to Firebase
        val updates = mutableMapOf<String, Any?>()

        // Clear and write restored daily_reports
        updates["daily_reports"] = finalizedReports.associateBy { it.id.toString() }

        // Monthly expenses
        updates["monthly_expenses"] = content.monthlyExpenses.associateBy { it.id.toString() }

        // Farm profile
        if (content.farmProfile != null) {
            updates["farm_profile"] = content.farmProfile
        }

        // Role permissions
        if (content.rolePermissions.isNotEmpty()) {
            updates["role_permissions"] = content.rolePermissions
        }

        // Shareholders
        if (content.shareholders.isNotEmpty()) {
            updates["shareholders"] = content.shareholders.associateBy { it.id }
        }

        // Shareholder Payments
        if (content.shareholderPayments.isNotEmpty()) {
            updates["shareholder_payments"] = content.shareholderPayments.associateBy { it.id }
        }

        // Staff
        if (content.staff.isNotEmpty()) {
            updates["staff"] = content.staff.associateBy { it.id }
        }

        // Staff Payments
        if (content.staffPayments.isNotEmpty()) {
            updates["staff_payments"] = content.staffPayments.associateBy { it.id }
        }

        reference.updateChildren(updates).await()

        // 3. Update local state flows
        _allDailyReports.value = finalizedReports.sortedByDescending { it.date }
        _allExpenses.value = content.monthlyExpenses.sortedByDescending { it.date }
        if (content.farmProfile != null) {
            _farmProfile.value = content.farmProfile
            saveFarmProfileToPrefs(content.farmProfile)
        }
        if (content.rolePermissions.isNotEmpty()) {
            _rolePermissions.value = content.rolePermissions
            content.rolePermissions.values.forEach { saveRolePermissionToPrefs(it) }
        }
        if (content.shareholders.isNotEmpty()) {
            _allShareholders.value = content.shareholders.sortedBy { it.name }
        }
        if (content.shareholderPayments.isNotEmpty()) {
            _allShareholderPayments.value = content.shareholderPayments.sortedByDescending { it.createdAt }
        }
        if (content.staff.isNotEmpty()) {
            _allStaff.value = content.staff.sortedBy { it.name }
        }
        if (content.staffPayments.isNotEmpty()) {
            _allStaffPayments.value = content.staffPayments.sortedByDescending { it.createdAt }
        }
    }
}
