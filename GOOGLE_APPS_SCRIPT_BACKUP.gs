/**
 * ==============================================================================
 * KAZI AGROTECH — SECURE GOOGLE APPS SCRIPT BACKUP WEB APP
 * ==============================================================================
 * 
 * Security & Setup Instructions:
 * 1. Open your private Google Sheet (https://sheets.new).
 *    Make sure Sheet Sharing is set to "Restricted" (Only your account has access).
 * 2. Click "Extensions" > "Apps Script".
 * 3. Delete all code in the editor and paste this entire file content.
 * 4. To set your secret API_TOKEN securely without hardcoding:
 *    - Click "Project Settings" (gear icon ⚙️ on the left menu).
 *    - Scroll to "Script Properties" > click "Edit script properties" > "Add script property".
 *    - Property: API_TOKEN
 *    - Value: <Your-Secret-Token-Here> (e.g. a strong random password or UUID)
 *    - Click "Save script properties".
 *    (Alternatively, you can assign API_TOKEN_FALLBACK below if not using Script Properties).
 * 5. Click "Deploy" > "New deployment".
 *    - Select type: "Web app".
 *    - Description: "Kazi Agrotech Secure Backup"
 *    - Execute as: "Me" (your Google account — writes to your private sheet)
 *    - Who has access: "Anyone" (allows the Android app to send authenticated requests)
 * 6. Click "Deploy" and Authorize access.
 * 7. Copy the "Web App URL" (starts with https://script.google.com/macros/s/...)
 * 8. In Kazi Agrotech App: Go to Settings > Cloud Backup, paste the Web App URL and API Token.
 * ==============================================================================
 */

// Fallback token if not configured in Script Properties (can be left empty if using Script Properties)
var API_TOKEN_FALLBACK = "";

// Security Configuration
var MAX_REQUEST_AGE_MS = 5 * 60 * 1000; // 5 minutes validity window
var REQ_CACHE_TTL_SEC = 600; // 10 minutes cache to prevent replay attacks
var RATE_LIMIT_SEC = 3; // Minimum 3 seconds between requests

function doPost(e) {
  var lock = LockService.getScriptLock();
  // Wait up to 30 seconds for lock to avoid concurrent write conflicts
  if (!lock.tryLock(30000)) {
    return createSecureResponse(false, "Backup service temporarily unavailable (Server busy)", 0, 503);
  }

  try {
    // 1. Basic Request Validation
    if (!e || !e.postData || !e.postData.contents) {
      return createSecureResponse(false, "Invalid request: No payload received", 0, 400);
    }

    // 2. Parse JSON Payload safely
    var payload;
    try {
      payload = JSON.parse(e.postData.contents);
    } catch (parseError) {
      return createSecureResponse(false, "Invalid request: Malformed JSON payload", 0, 400);
    }

    // 3. Security: Authentication / Secret Token Check
    var configuredToken = getSecretApiToken();
    if (configuredToken && configuredToken.trim() !== "") {
      var reqToken = extractBearerToken(e, payload);
      if (!reqToken || reqToken !== configuredToken) {
        // Log failed authentication without exposing secrets
        logSecurityEvent("AUTH_FAILED", "Unauthorized backup request attempted");
        return createSecureResponse(false, "Unauthorized: Invalid or missing API Token", 0, 401);
      }
    }

    // 4. Security: Timestamp & Expiry Validation (Replay Prevention)
    var reqTimestamp = Number(payload.timestamp);
    if (!reqTimestamp || isNaN(reqTimestamp)) {
      return createSecureResponse(false, "Invalid request: Missing timestamp", 0, 400);
    }
    var currentServerTime = Date.now();
    var timeDifference = Math.abs(currentServerTime - reqTimestamp);
    if (timeDifference > MAX_REQUEST_AGE_MS) {
      logSecurityEvent("EXPIRED_REQUEST", "Rejected request with timestamp diff: " + Math.round(timeDifference / 1000) + "s");
      return createSecureResponse(false, "Invalid request: Expired timestamp window (> 5 minutes)", 0, 400);
    }

    // 5. Security: Unique Request ID Deduplication (Replay Protection)
    var requestId = payload.request_id ? String(payload.request_id).trim() : "";
    if (!requestId || requestId.length < 8) {
      return createSecureResponse(false, "Invalid request: Missing or invalid request_id", 0, 400);
    }

    var cache = CacheService.getScriptCache();
    var cacheKey = "REQ_" + requestId;
    if (cache.get(cacheKey) !== null) {
      logSecurityEvent("DUPLICATE_REQUEST", "Rejected duplicate request_id: " + requestId.substring(0, 8) + "...");
      return createSecureResponse(false, "Duplicate request detected: This request was already processed", 0, 409);
    }

    // 6. Security: Basic Rate Limiting
    var rateLimitKey = "RATE_LIMIT_GLOBAL";
    if (cache.get(rateLimitKey) !== null) {
      return createSecureResponse(false, "Rate limit exceeded. Please wait a few seconds before retrying.", 0, 429);
    }
    // Set rate limit flag for 3 seconds
    try { cache.put(rateLimitKey, "1", RATE_LIMIT_SEC); } catch (cErr) {}

    // Mark request_id as used in cache for 10 minutes
    try { cache.put(cacheKey, "PROCESSED", REQ_CACHE_TTL_SEC); } catch (cErr) {}

    // 7. Security: Schema & Data Structure Validation
    if (!payload.data || typeof payload.data !== 'object') {
      return createSecureResponse(false, "Invalid request: Missing data payload", 0, 400);
    }

    // 8. Execute Database Sync to Private Google Sheet
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var data = payload.data || {};
    var processedCount = 0;

    // A. Sync Farm Profile
    if (data.farm_profile && typeof data.farm_profile === 'object') {
      syncFarmProfile(ss, data.farm_profile);
      processedCount++;
    }

    // B. Sync Daily Reports (Upsert)
    if (data.daily_reports && Array.isArray(data.daily_reports)) {
      processedCount += syncDailyReports(ss, data.daily_reports);
    }

    // C. Sync Monthly Expenses (Upsert)
    if (data.monthly_expenses && Array.isArray(data.monthly_expenses)) {
      processedCount += syncMonthlyExpenses(ss, data.monthly_expenses);
    }

    // D. Sync Users (Upsert)
    if (data.users && Array.isArray(data.users)) {
      processedCount += syncUsers(ss, data.users);
    }

    // E. Sync Role Permissions (Upsert)
    if (data.role_permissions && typeof data.role_permissions === 'object') {
      processedCount += syncRolePermissions(ss, data.role_permissions);
    }

    // F. Sync Shareholders (Upsert)
    if (data.shareholders && Array.isArray(data.shareholders)) {
      processedCount += syncShareholders(ss, data.shareholders);
    }

    // G. Sync Shareholder Payments (Upsert)
    if (data.shareholder_payments && Array.isArray(data.shareholder_payments)) {
      processedCount += syncShareholderPayments(ss, data.shareholder_payments);
    }

    // H. Sync Staff (Upsert)
    if (data.staff && Array.isArray(data.staff)) {
      processedCount += syncStaff(ss, data.staff);
    }

    // I. Sync Staff Payments (Upsert)
    if (data.staff_payments && Array.isArray(data.staff_payments)) {
      processedCount += syncStaffPayments(ss, data.staff_payments);
    }

    // 9. Log Activity (Securely without logging tokens or personal payload)
    logBackupActivity(ss, payload, processedCount, "SUCCESS", "ক্লাউড ব্যাকআপ সফল হয়েছে");

    return createSecureResponse(true, "ক্লাউড ব্যাকআপ সফল হয়েছে", processedCount, 200);

  } catch (error) {
    try {
      var ssErr = SpreadsheetApp.getActiveSpreadsheet();
      logBackupActivity(ssErr, payload || {}, 0, "ERROR", "Internal execution error");
    } catch (logErr) {}
    // Generic error message to prevent leaking internal stack trace details
    return createSecureResponse(false, "ব্যাকআপ সম্পন্ন হয়নি: সার্ভার অভ্যন্তরীণ ত্রুটি", 0, 500);
  } finally {
    lock.releaseLock();
  }
}

/**
 * Disallow public read access to data.
 */
function doGet(e) {
  return createSecureResponse(true, "Kazi Agrotech Secure Cloud Backup Web App is online.", 0, 200);
}

// -------------------------------------------------------------
// SECURITY & AUTHENTICATION HELPERS
// -------------------------------------------------------------

function getSecretApiToken() {
  try {
    var token = PropertiesService.getScriptProperties().getProperty("API_TOKEN");
    if (token && token.trim() !== "") {
      return token.trim();
    }
  } catch (e) {}
  return API_TOKEN_FALLBACK.trim();
}

function extractBearerToken(e, payload) {
  // Check Authorization header (Authorization: Bearer <TOKEN>)
  if (e && e.headers) {
    var authHeader = e.headers["Authorization"] || e.headers["authorization"];
    if (authHeader && typeof authHeader === "string") {
      var parts = authHeader.split(" ");
      if (parts.length === 2 && parts[0].toLowerCase() === "bearer") {
        return parts[1].trim();
      }
      return authHeader.trim();
    }
  }
  // Fallback to payload api_token
  if (payload && payload.api_token) {
    return String(payload.api_token).trim();
  }
  return "";
}

function createSecureResponse(success, message, count, statusCode) {
  var output = {
    success: success,
    message: message,
    records_processed: count,
    timestamp: new Date().toISOString()
  };
  return ContentService
    .createTextOutput(JSON.stringify(output))
    .setMimeType(ContentService.MimeType.JSON);
}

function logSecurityEvent(eventType, message) {
  try {
    console.warn("[" + eventType + "] " + message);
  } catch (e) {}
}

// -------------------------------------------------------------
// PRIVATE SHEET SYNC FUNCTIONS (UPSERT)
// -------------------------------------------------------------

function syncFarmProfile(ss, profile) {
  var sheet = getOrCreateSheet(ss, "Farm Profile (ফার্ম প্রোফাইল)", [
    "Field (বিবরণ)", "Value (তথ্য)", "Last Updated (আপডেট সময়)"
  ]);

  var rows = [
    ["ফার্মের নাম (Farm Name)", sanitizeString(profile.farmName), new Date()],
    ["মালিকের নাম (Owner Name)", sanitizeString(profile.ownerName), new Date()],
    ["মোবাইল নম্বর (Mobile)", sanitizeString(profile.mobileNumber), new Date()],
    ["ঠিকানা (Address)", sanitizeString(profile.address), new Date()],
    ["লোগো ইমোজি (Logo Emoji)", sanitizeString(profile.logoEmoji || "🐔"), new Date()],
    ["প্রারম্ভিক স্টক (Initial Stock)", Number(profile.initialOpeningStock) || 0, new Date()],
    ["প্রারম্ভিক তারিখ (Initial Date)", sanitizeString(profile.initialOpeningDate), new Date()],
    ["ডার্ক মোড (Dark Mode)", profile.isDarkMode ? "ON" : "OFF", new Date()],
    ["স্বয়ংক্রিয় ব্যাকআপ (Auto Backup)", profile.autoBackup ? "ON" : "OFF", new Date()]
  ];

  sheet.getRange(2, 1, sheet.getLastRow() > 1 ? sheet.getLastRow() - 1 : 1, 3).clearContent();
  sheet.getRange(2, 1, rows.length, 3).setValues(rows);
}

function syncDailyReports(ss, reports) {
  var headers = [
    "Record ID", "তারিখ (Date)", "বর্তমান মুরগী", "মৃত মুরগী", "ডিম উৎপাদন",
    "ডিম বিক্রয়", "ডিমের দর (৳)", "মোট বিক্রয় (৳)", "ঔষধ খরচ (৳)",
    "সমাপনী স্টক", "অন্যান্য স্টক বৃদ্ধি", "নষ্ট / ঘাটতি", "স্টক সমন্বয়",
    "সমন্বয়ের কারণ", "মন্তব্য (Remarks)", "Sync Timestamp"
  ];
  var sheet = getOrCreateSheet(ss, "Daily Reports (দৈনিক রিপোর্ট)", headers);
  if (reports.length === 0) return 0;

  var idColIndex = 1;
  var existingMap = getRowIndexMap(sheet, idColIndex);
  var appendRows = [];
  var now = new Date();

  reports.forEach(function(r) {
    var idStr = String(r.id || r.date || "").trim();
    if (!idStr) return;

    var rowValues = [
      idStr,
      sanitizeString(r.date),
      Number(r.currentBirds) || 0,
      Number(r.deadBirds) || 0,
      Number(r.eggProduction) || 0,
      Number(r.eggSold) || 0,
      Number(r.eggPrice) || 0,
      Number(r.totalSale) || 0,
      Number(r.medicineCost) || 0,
      Number(r.currentStock) || 0,
      Number(r.otherStockIn) || 0,
      Number(r.otherStockOut) || 0,
      Number(r.stockAdjustment) || 0,
      sanitizeString(r.adjustmentReason),
      sanitizeString(r.remarks),
      now
    ];

    if (existingMap[idStr]) {
      sheet.getRange(existingMap[idStr], 1, 1, rowValues.length).setValues([rowValues]);
    } else {
      appendRows.push(rowValues);
    }
  });

  if (appendRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, appendRows.length, appendRows[0].length).setValues(appendRows);
  }
  return reports.length;
}

function syncMonthlyExpenses(ss, expenses) {
  var headers = [
    "Record ID", "তারিখ (Date)", "খাদ্য / ফিড (৳)", "ঔষধ ও ভ্যাকসিন (৳)",
    "স্টাফ বাজার (৳)", "স্টাফ বেতন (৳)", "গাড়ি মেরামত (৳)", "সম্পদ ক্রয় (৳)",
    "বিদ্যুৎ বিল (৳)", "অন্যান্য খরচ (৳)", "মোট ব্যয় (৳)", "মন্তব্য (Remarks)",
    "Sync Timestamp"
  ];
  var sheet = getOrCreateSheet(ss, "Monthly Expenses (মাসিক খরচ)", headers);
  if (expenses.length === 0) return 0;

  var idColIndex = 1;
  var existingMap = getRowIndexMap(sheet, idColIndex);
  var appendRows = [];
  var now = new Date();

  expenses.forEach(function(e) {
    var idStr = String(e.id || e.date || "").trim();
    if (!idStr) return;

    var rowValues = [
      idStr,
      sanitizeString(e.date),
      Number(e.feedCost) || 0,
      Number(e.medicineCost) || 0,
      Number(e.staffMarket) || 0,
      Number(e.staffSalary) || 0,
      Number(e.vehicleRepair) || 0,
      Number(e.assets) || 0,
      Number(e.electricityBill) || 0,
      Number(e.otherExpense) || 0,
      Number(e.totalExpense) || 0,
      sanitizeString(e.remarks),
      now
    ];

    if (existingMap[idStr]) {
      sheet.getRange(existingMap[idStr], 1, 1, rowValues.length).setValues([rowValues]);
    } else {
      appendRows.push(rowValues);
    }
  });

  if (appendRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, appendRows.length, appendRows[0].length).setValues(appendRows);
  }
  return expenses.length;
}

function syncUsers(ss, users) {
  var headers = [
    "User ID (UID)", "ইউজারনেম (Name)", "ইমেইল (Email)", "মোবাইল (Phone)",
    "রোল (Role)", "অনুমোদিত (Approved)", "রেজিস্ট্রেশন তারিখ", "Sync Timestamp"
  ];
  var sheet = getOrCreateSheet(ss, "Users (ইউজার তালিকা)", headers);
  if (users.length === 0) return 0;

  var idColIndex = 1;
  var existingMap = getRowIndexMap(sheet, idColIndex);
  var appendRows = [];
  var now = new Date();

  users.forEach(function(u) {
    var idStr = String(u.id || u.email || "").trim();
    if (!idStr) return;

    var regDateStr = u.registeredDate ? new Date(u.registeredDate).toLocaleString() : "";
    var rowValues = [
      idStr,
      sanitizeString(u.username),
      sanitizeString(u.email),
      sanitizeString(u.phone),
      sanitizeString(u.role || "WORKER"),
      u.isApproved ? "YES (অনুমোদিত)" : "NO (পেন্ডিং)",
      regDateStr,
      now
    ];

    if (existingMap[idStr]) {
      sheet.getRange(existingMap[idStr], 1, 1, rowValues.length).setValues([rowValues]);
    } else {
      appendRows.push(rowValues);
    }
  });

  if (appendRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, appendRows.length, appendRows[0].length).setValues(appendRows);
  }
  return users.length;
}

function syncRolePermissions(ss, rolePermsMap) {
  var headers = [
    "Role Key", "রোল নাম", "Daily View", "Daily Add", "User View",
    "Expense View", "Expense Add", "Expense Delete", "Report View", "Report Download", "Sync Timestamp"
  ];
  var sheet = getOrCreateSheet(ss, "Role Permissions (রোল পারমিশন)", headers);
  var keys = Object.keys(rolePermsMap);
  if (keys.length === 0) return 0;

  var idColIndex = 1;
  var existingMap = getRowIndexMap(sheet, idColIndex);
  var appendRows = [];
  var now = new Date();

  keys.forEach(function(k) {
    var p = rolePermsMap[k];
    var idStr = String(p.roleKey || k).toUpperCase().trim();
    if (!idStr) return;

    var rowValues = [
      idStr,
      sanitizeString(p.roleDisplayName || idStr),
      p.dailyReportView ? "TRUE" : "FALSE",
      p.dailyReportAdd ? "TRUE" : "FALSE",
      p.userManagementView ? "TRUE" : "FALSE",
      p.expenseView ? "TRUE" : "FALSE",
      p.expenseAdd ? "TRUE" : "FALSE",
      p.expenseDelete ? "TRUE" : "FALSE",
      p.reportAnalyticsView ? "TRUE" : "FALSE",
      p.reportAnalyticsDownload ? "TRUE" : "FALSE",
      now
    ];

    if (existingMap[idStr]) {
      sheet.getRange(existingMap[idStr], 1, 1, rowValues.length).setValues([rowValues]);
    } else {
      appendRows.push(rowValues);
    }
  });

  if (appendRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, appendRows.length, appendRows[0].length).setValues(appendRows);
  }
  return keys.length;
}

function syncShareholders(ss, list) {
  if (!list || list.length === 0) return 0;
  var headers = ["Shareholder ID (আইডি)", "Shareholder Name (নাম)", "Created Date", "Last Synced"];
  var sheet = getOrCreateSheet(ss, "Shareholders (শেয়ারহোল্ডার)", headers);
  var existingMap = getRowIndexMap(sheet, 1);
  var now = new Date();
  var appendRows = [];

  list.forEach(function(s) {
    var idStr = String(s.id || "").trim();
    if (!idStr) return;

    var createdDate = s.createdAt ? new Date(Number(s.createdAt)) : now;
    var rowValues = [
      idStr,
      sanitizeString(s.name || ""),
      createdDate,
      now
    ];

    if (existingMap[idStr]) {
      sheet.getRange(existingMap[idStr], 1, 1, rowValues.length).setValues([rowValues]);
    } else {
      appendRows.push(rowValues);
    }
  });

  if (appendRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, appendRows.length, appendRows[0].length).setValues(appendRows);
  }
  return list.length;
}

function syncShareholderPayments(ss, list) {
  if (!list || list.length === 0) return 0;
  var headers = [
    "Payment ID (আইডি)", "Shareholder ID", "Shareholder Name (নাম)",
    "Date (তারিখ)", "Amount (টাকার পরিমাণ ৳)", "Payment Method (মাধ্যম)",
    "Note (মন্তব্য)", "Created Date", "Last Synced"
  ];
  var sheet = getOrCreateSheet(ss, "Shareholder Payments (পেমেন্ট)", headers);
  var existingMap = getRowIndexMap(sheet, 1);
  var now = new Date();
  var appendRows = [];

  list.forEach(function(p) {
    var idStr = String(p.id || "").trim();
    if (!idStr) return;

    var createdDate = p.createdAt ? new Date(Number(p.createdAt)) : now;
    var rowValues = [
      idStr,
      sanitizeString(p.shareholderId || ""),
      sanitizeString(p.shareholderName || ""),
      sanitizeString(p.date || ""),
      Number(p.amount) || 0,
      sanitizeString(p.paymentMethod || "Cash"),
      sanitizeString(p.note || ""),
      createdDate,
      now
    ];

    if (existingMap[idStr]) {
      sheet.getRange(existingMap[idStr], 1, 1, rowValues.length).setValues([rowValues]);
    } else {
      appendRows.push(rowValues);
    }
  });

  if (appendRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, appendRows.length, appendRows[0].length).setValues(appendRows);
  }
  return list.length;
}

function syncStaff(ss, list) {
  if (!list || list.length === 0) return 0;
  var headers = ["Staff ID (আইডি)", "Staff Name (নাম)", "Mobile (মোবাইল)", "Created Date", "Last Synced"];
  var sheet = getOrCreateSheet(ss, "Staff (স্টাফ তালিকা)", headers);
  var existingMap = getRowIndexMap(sheet, 1);
  var now = new Date();
  var appendRows = [];

  list.forEach(function(s) {
    var idStr = String(s.id || "").trim();
    if (!idStr) return;

    var createdDate = s.createdAt ? new Date(Number(s.createdAt)) : now;
    var rowValues = [
      idStr,
      sanitizeString(s.name || ""),
      sanitizeString(s.phone || ""),
      createdDate,
      now
    ];

    if (existingMap[idStr]) {
      sheet.getRange(existingMap[idStr], 1, 1, rowValues.length).setValues([rowValues]);
    } else {
      appendRows.push(rowValues);
    }
  });

  if (appendRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, appendRows.length, appendRows[0].length).setValues(appendRows);
  }
  return list.length;
}

function syncStaffPayments(ss, list) {
  if (!list || list.length === 0) return 0;
  var headers = [
    "Payment ID (আইডি)", "Staff ID", "Staff Name (নাম)",
    "Date (তারিখ)", "Amount (টাকার পরিমাণ ৳)", "Payment Method (মাধ্যম)",
    "Note (মন্তব্য)", "Created Date", "Last Synced"
  ];
  var sheet = getOrCreateSheet(ss, "Staff Payments (স্টাফ পেমেন্ট)", headers);
  var existingMap = getRowIndexMap(sheet, 1);
  var now = new Date();
  var appendRows = [];

  list.forEach(function(p) {
    var idStr = String(p.id || "").trim();
    if (!idStr) return;

    var createdDate = p.createdAt ? new Date(Number(p.createdAt)) : now;
    var rowValues = [
      idStr,
      sanitizeString(p.staffId || ""),
      sanitizeString(p.staffName || ""),
      sanitizeString(p.date || ""),
      Number(p.amount) || 0,
      sanitizeString(p.paymentMethod || "Cash"),
      sanitizeString(p.note || ""),
      createdDate,
      now
    ];

    if (existingMap[idStr]) {
      sheet.getRange(existingMap[idStr], 1, 1, rowValues.length).setValues([rowValues]);
    } else {
      appendRows.push(rowValues);
    }
  });

  if (appendRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, appendRows.length, appendRows[0].length).setValues(appendRows);
  }
  return list.length;
}

function logBackupActivity(ss, payload, recordCount, status, message) {
  var headers = [
    "Timestamp (সময়)", "Status (স্ট্যাটাস)", "Records Synced", "App Version",
    "Schema Version", "Request ID", "Triggered By", "Message / Note"
  ];
  var sheet = getOrCreateSheet(ss, "Backup Log (লগ)", headers);
  var now = new Date();
  var reqId = payload.request_id ? String(payload.request_id).substring(0, 13) + "..." : "-";
  var row = [
    now,
    status,
    recordCount,
    sanitizeString(payload.app_version || "1.0.0"),
    Number(payload.backup_schema_version) || 1,
    reqId,
    sanitizeString(payload.user_email || "Auto/Worker"),
    sanitizeString(message)
  ];
  sheet.appendRow(row);
}

// -------------------------------------------------------------
// HELPER UTILITIES
// -------------------------------------------------------------

function getOrCreateSheet(ss, sheetName, headers) {
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
    sheet.appendRow(headers);
    var headerRange = sheet.getRange(1, 1, 1, headers.length);
    headerRange.setBackground("#1B5E20");
    headerRange.setFontColor("#FFFFFF");
    headerRange.setFontWeight("bold");
    sheet.setFrozenRows(1);
  }
  return sheet;
}

function getRowIndexMap(sheet, idColIndex) {
  var map = {};
  var lastRow = sheet.getLastRow();
  if (lastRow <= 1) return map;

  var ids = sheet.getRange(2, idColIndex, lastRow - 1, 1).getValues();
  for (var i = 0; i < ids.length; i++) {
    var val = String(ids[i][0]).trim();
    if (val !== "") {
      map[val] = i + 2;
    }
  }
  return map;
}

function sanitizeString(val) {
  if (val === null || val === undefined) return "";
  return String(val).trim();
}
