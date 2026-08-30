package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.DailyReportEntity
import com.example.data.local.FarmProfileEntity
import com.example.data.local.MonthlyExpenseEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfPreviewModalDialog(
    title: String,
    farmProfile: FarmProfileEntity,
    dailyReports: List<DailyReportEntity> = emptyList(),
    allReports: List<DailyReportEntity> = emptyList(),
    baselineStock: Int = 0,
    expenses: List<MonthlyExpenseEntity> = emptyList(),
    reportCategory: String = if (dailyReports.isEmpty() && expenses.isNotEmpty()) "EXPENSE" else "DAILY",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sortedDailyReports = remember(dailyReports) { dailyReports.sortedBy { it.date } }
    val sortedExpenses = remember(expenses) { expenses.sortedBy { it.date } }
    val fullReports = if (allReports.isNotEmpty()) allReports else dailyReports

    val distinctMonths = remember(sortedDailyReports, sortedExpenses) {
        (sortedDailyReports.map { it.date.take(7) } + sortedExpenses.map { it.date.take(7) })
            .filter { it.length == 7 && it.contains("-") }
            .distinct()
    }
    val monthTagText = remember(distinctMonths) {
        if (distinctMonths.size == 1) {
            BanglaNumberFormatter.formatYearMonth(distinctMonths.first())
        } else if (distinctMonths.size > 1) {
            "একাধিক মাস (${BanglaNumberFormatter.formatNumber(distinctMonths.size)} টি)"
        } else {
            "সকল রেকর্ড"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Modal Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পিডিএফ প্রিন্ট প্রিভিউ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    val printDocName = "Kazi_Agrotech_${System.currentTimeMillis()}"
                                    printHtmlDocument(
                                        context = context,
                                        docName = printDocName,
                                        html = generateHtmlContent(
                                            title = title,
                                            farmProfile = farmProfile,
                                            dailyReports = sortedDailyReports,
                                            allReports = fullReports,
                                            baselineStock = baselineStock,
                                            expenses = sortedExpenses,
                                            reportCategory = reportCategory
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "Print",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "কাজী এগ্রোটেক অফিসিয়াল রিপোর্ট: $title\n" +
                                                    "মাস: $monthTagText\n" +
                                                    "ফার্ম: ${farmProfile.farmName}\n" +
                                                    "মালিক: ${farmProfile.ownerName}\n" +
                                                    "মোবাইল: ${farmProfile.mobileNumber}\n" +
                                                    "ঠিকানা: ${farmProfile.address}\n\n" +
                                                    "মোট দৈনিক রেকর্ড: ${sortedDailyReports.size} টি, ব্যয় রেকর্ড: ${sortedExpenses.size} টি"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "রিপোর্ট শেয়ার করুন"))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Printable Document Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFE2E4E2))
                        .padding(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Official Farm Letterhead
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF0FDF4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FarmLogoDisplay(
                                        logoUri = farmProfile.logoUri,
                                        logoEmoji = farmProfile.logoEmoji,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = farmProfile.farmName,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D631B),
                                            fontSize = 20.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "লেয়ার পোল্ট্রি ফার্ম",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF333333),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = "প্রোঃ ${farmProfile.ownerName} | মোবাইলঃ ${farmProfile.mobileNumber}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF555555),
                                            fontSize = 11.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "ঠিকানাঃ ${farmProfile.address}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF555555),
                                            fontSize = 10.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.size(60.dp))
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFF0D631B), thickness = 2.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Line 1: Report Title
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111111)
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Line 2: Dedicated Month Line & Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFE8F5E9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC8E6C9))
                                ) {
                                    Text(
                                        text = "মাসঃ $monthTagText",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0D631B),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = "তারিখ: ${BanglaNumberFormatter.toBanglaDigits(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()))}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF444444)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Table Content Rendered According to Selected Report Category
                            when (reportCategory) {
                                "MONTHLY" -> MonthlyOverviewPdfTable(sortedDailyReports, sortedExpenses)
                                "SALES" -> SalesReportPdfTable(sortedDailyReports)
                                "PRODUCTION" -> ProductionReportPdfTable(sortedDailyReports)
                                "EXPENSE" -> MonthlyExpensePdfTable(sortedExpenses)
                                "PROFIT_LOSS" -> ProfitLossPdfTable(sortedDailyReports, sortedExpenses)
                                else -> DailyReportPdfTable(sortedDailyReports)
                            }

                            Spacer(modifier = Modifier.height(55.dp))

                            // Signature Lines matching official paper registers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(1.dp)
                                            .background(Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "প্রস্তুতকারক",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.Black,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(1.dp)
                                            .background(Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "অনুমোদনকারী",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.Black,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "কাজী এগ্রোটেক স্বয়ংক্রিয় ফার্ম ম্যানেজমেন্ট সিস্টেম দ্বারা প্রস্তুতকৃত।",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Gray,
                                    fontSize = 9.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Bottom Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("বন্ধ করুন")
                        }

                        Button(
                            onClick = {
                                val printDocName = "Kazi_Agrotech_Report"
                                printHtmlDocument(
                                    context = context,
                                    docName = printDocName,
                                    html = generateHtmlContent(
                                        title = title,
                                        farmProfile = farmProfile,
                                        dailyReports = sortedDailyReports,
                                        allReports = fullReports,
                                        baselineStock = baselineStock,
                                        expenses = sortedExpenses,
                                        reportCategory = reportCategory
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1.3f).height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = "Print")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("প্রিন্ট / সেভ করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// 1. Daily Report Table
@Composable
fun DailyReportPdfTable(reports: List<DailyReportEntity>) {
    val scrollState = rememberScrollState()
    val sortedReports = remember(reports) { reports.sortedBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(1.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 6.dp)
        ) {
            TableCell("তারিখ", width = 75.dp, isHeader = true, align = TextAlign.Center)
            TableCell("মুরগি", width = 65.dp, isHeader = true)
            TableCell("মৃত", width = 50.dp, isHeader = true)
            TableCell("উৎপাদন", width = 75.dp, isHeader = true)
            TableCell("বিক্রয়", width = 75.dp, isHeader = true)
            TableCell("দর", width = 60.dp, isHeader = true)
            TableCell("মোট বিক্রয়", width = 110.dp, isHeader = true)
        }

        sortedReports.forEachIndexed { index, r ->
            val bg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 5.dp)
            ) {
                TableCell(BanglaNumberFormatter.formatShortDate(r.date), width = 75.dp, align = TextAlign.Center)
                TableCell(BanglaNumberFormatter.formatNumber(r.currentBirds), width = 65.dp)
                TableCell(if (r.deadBirds > 0) BanglaNumberFormatter.formatNumber(r.deadBirds) else "০", width = 50.dp)
                TableCell(BanglaNumberFormatter.formatNumber(r.eggProduction), width = 75.dp)
                TableCell(BanglaNumberFormatter.formatNumber(r.eggSold), width = 75.dp)
                TableCell(BanglaNumberFormatter.formatDecimal(r.eggPrice), width = 60.dp)
                TableCell(BanglaNumberFormatter.formatCurrency(r.totalSale), width = 110.dp, isBold = true)
            }
        }

        val totalProd = sortedReports.sumOf { it.eggProduction }
        val totalSold = sortedReports.sumOf { it.eggSold }
        val totalSale = sortedReports.sumOf { it.totalSale }

        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 7.dp)
        ) {
            TableCell("সর্বমোট", width = 75.dp, isHeader = true, align = TextAlign.Center)
            TableCell("-", width = 65.dp, isHeader = true)
            TableCell("-", width = 50.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatNumber(totalProd), width = 75.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatNumber(totalSold), width = 75.dp, isHeader = true)
            TableCell("-", width = 60.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalSale), width = 110.dp, isHeader = true)
        }
    }
}

// 2. Monthly Comprehensive Overview Table
@Composable
fun MonthlyOverviewPdfTable(reports: List<DailyReportEntity>, expenses: List<MonthlyExpenseEntity>) {
    val scrollState = rememberScrollState()
    val allDates = remember(reports, expenses) {
        (reports.map { it.date } + expenses.map { it.date }).toSortedSet().toList()
    }
    val reportsByDate = remember(reports) { reports.associateBy { it.date } }
    val expensesByDate = remember(expenses) { expenses.associateBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(1.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 6.dp)
        ) {
            TableCell("তারিখ", width = 75.dp, isHeader = true, align = TextAlign.Center)
            TableCell("উৎপাদন", width = 65.dp, isHeader = true)
            TableCell("বিক্রয়", width = 65.dp, isHeader = true)
            TableCell("বিক্রয় আয়", width = 95.dp, isHeader = true)
            TableCell("মাসিক ব্যয়", width = 95.dp, isHeader = true)
            TableCell("নিট উদ্বৃত্ত", width = 105.dp, isHeader = true)
        }

        var sumProd = 0
        var sumSold = 0
        var sumSale = 0.0
        var sumMonthlyExp = 0.0
        var sumNet = 0.0

        allDates.forEachIndexed { index, date ->
            val r = reportsByDate[date]
            val e = expensesByDate[date]

            val prod = r?.eggProduction ?: 0
            val sold = r?.eggSold ?: 0
            val sale = r?.totalSale ?: 0.0
            val monthlyExp = e?.totalExpense ?: 0.0
            val net = sale - monthlyExp

            sumProd += prod
            sumSold += sold
            sumSale += sale
            sumMonthlyExp += monthlyExp
            sumNet += net

            val bg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 5.dp)
            ) {
                TableCell(BanglaNumberFormatter.formatShortDate(date), width = 75.dp, align = TextAlign.Center)
                TableCell(if (prod > 0) BanglaNumberFormatter.formatNumber(prod) else "-", width = 65.dp)
                TableCell(if (sold > 0) BanglaNumberFormatter.formatNumber(sold) else "-", width = 65.dp)
                TableCell(if (sale > 0) BanglaNumberFormatter.formatCurrency(sale) else "০", width = 95.dp)
                TableCell(if (monthlyExp > 0) BanglaNumberFormatter.formatCurrency(monthlyExp) else "০", width = 95.dp)
                TableCell(BanglaNumberFormatter.formatCurrency(net), width = 105.dp, isBold = true)
            }
        }

        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 7.dp)
        ) {
            TableCell("সর্বমোট", width = 75.dp, isHeader = true, align = TextAlign.Center)
            TableCell(BanglaNumberFormatter.formatNumber(sumProd), width = 65.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatNumber(sumSold), width = 65.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(sumSale), width = 95.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(sumMonthlyExp), width = 95.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(sumNet), width = 105.dp, isHeader = true)
        }
    }
}

// 3. Sales Report Table
@Composable
fun SalesReportPdfTable(reports: List<DailyReportEntity>) {
    val scrollState = rememberScrollState()
    val salesReports = remember(reports) { reports.filter { it.eggSold > 0 || it.totalSale > 0.0 }.sortedBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(1.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 6.dp)
        ) {
            TableCell("তারিখ", width = 80.dp, isHeader = true, align = TextAlign.Center)
            TableCell("বিক্রির ডিম (টি)", width = 100.dp, isHeader = true)
            TableCell("দর/টি", width = 80.dp, isHeader = true)
            TableCell("দর/১০০টি", width = 90.dp, isHeader = true)
            TableCell("মোট বিক্রয় মূল্য", width = 130.dp, isHeader = true)
            TableCell("মন্তব্য", width = 110.dp, isHeader = true, align = TextAlign.Start)
        }

        salesReports.forEachIndexed { index, r ->
            val bg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
            val pricePerHundred = r.eggPrice * 100
            val remarkText = if (r.remarks.isNotBlank()) r.remarks else "নগদ বিক্রয়"
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 5.dp)
            ) {
                TableCell(BanglaNumberFormatter.formatShortDate(r.date), width = 80.dp, align = TextAlign.Center)
                TableCell(BanglaNumberFormatter.formatNumber(r.eggSold), width = 100.dp)
                TableCell(BanglaNumberFormatter.formatDecimal(r.eggPrice), width = 80.dp)
                TableCell(BanglaNumberFormatter.formatCurrency(pricePerHundred), width = 90.dp)
                TableCell(BanglaNumberFormatter.formatCurrency(r.totalSale), width = 130.dp, isBold = true)
                TableCell(remarkText, width = 110.dp, align = TextAlign.Start)
            }
        }

        val totalEggs = salesReports.sumOf { it.eggSold }
        val totalRevenue = salesReports.sumOf { it.totalSale }
        val avgPrice = if (totalEggs > 0) totalRevenue / totalEggs else 0.0

        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 7.dp)
        ) {
            TableCell("সর্বমোট / গড়", width = 80.dp, isHeader = true, align = TextAlign.Center)
            TableCell(BanglaNumberFormatter.formatNumber(totalEggs), width = 100.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatDecimal(avgPrice), width = 80.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(avgPrice * 100), width = 90.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalRevenue), width = 130.dp, isHeader = true)
            TableCell("-", width = 110.dp, isHeader = true, align = TextAlign.Start)
        }
    }
}

// 4. Production & Flock Health Report Table
@Composable
fun ProductionReportPdfTable(reports: List<DailyReportEntity>) {
    val scrollState = rememberScrollState()
    val sortedReports = remember(reports) { reports.sortedBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(1.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 6.dp)
        ) {
            TableCell("তারিখ", width = 80.dp, isHeader = true, align = TextAlign.Center)
            TableCell("মোট মুরগি", width = 80.dp, isHeader = true)
            TableCell("মৃত মুরগি", width = 70.dp, isHeader = true)
            TableCell("মৃত্যুর হার %", width = 80.dp, isHeader = true)
            TableCell("ডিম উৎপাদন", width = 90.dp, isHeader = true)
            TableCell("লেইং হার %", width = 85.dp, isHeader = true)
            TableCell("মন্তব্য", width = 105.dp, isHeader = true, align = TextAlign.Start)
        }

        sortedReports.forEachIndexed { index, r ->
            val bg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
            val mortalityRate = if (r.currentBirds > 0) (r.deadBirds.toDouble() / r.currentBirds * 100) else 0.0
            val layingRate = if (r.currentBirds > 0) (r.eggProduction.toDouble() / r.currentBirds * 100) else 0.0
            val remarkText = if (r.remarks.isNotBlank()) r.remarks else "স্বাভাবিক"

            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 5.dp)
            ) {
                TableCell(BanglaNumberFormatter.formatShortDate(r.date), width = 80.dp, align = TextAlign.Center)
                TableCell(BanglaNumberFormatter.formatNumber(r.currentBirds), width = 80.dp)
                TableCell(if (r.deadBirds > 0) BanglaNumberFormatter.formatNumber(r.deadBirds) else "০", width = 70.dp)
                TableCell(if (mortalityRate > 0) String.format(Locale.US, "%.2f%%", mortalityRate) else "০.০%", width = 80.dp)
                TableCell(BanglaNumberFormatter.formatNumber(r.eggProduction), width = 90.dp, isBold = true)
                TableCell(String.format(Locale.US, "%.1f%%", layingRate), width = 85.dp)
                TableCell(remarkText, width = 105.dp, align = TextAlign.Start)
            }
        }

        val totalMortality = sortedReports.sumOf { it.deadBirds }
        val totalProd = sortedReports.sumOf { it.eggProduction }
        val avgBirds = if (sortedReports.isNotEmpty()) sortedReports.map { it.currentBirds }.average().toInt() else 0
        val overallLayingRate = if (avgBirds > 0 && sortedReports.isNotEmpty()) (totalProd.toDouble() / (avgBirds * sortedReports.size) * 100) else 0.0

        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 7.dp)
        ) {
            TableCell("সর্বমোট / গড়", width = 80.dp, isHeader = true, align = TextAlign.Center)
            TableCell(BanglaNumberFormatter.formatNumber(avgBirds), width = 80.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatNumber(totalMortality), width = 70.dp, isHeader = true)
            TableCell("-", width = 80.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatNumber(totalProd), width = 90.dp, isHeader = true)
            TableCell(String.format(Locale.US, "%.1f%%", overallLayingRate), width = 85.dp, isHeader = true)
            TableCell("-", width = 105.dp, isHeader = true, align = TextAlign.Start)
        }
    }
}

// 5. Monthly Expense Report Table
@Composable
fun MonthlyExpensePdfTable(expenses: List<MonthlyExpenseEntity>) {
    val scrollState = rememberScrollState()
    val sortedExpenses = remember(expenses) { expenses.sortedBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(1.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 6.dp)
        ) {
            TableCell("তারিখ", width = 75.dp, isHeader = true, align = TextAlign.Center)
            TableCell("খাদ্য", width = 75.dp, isHeader = true)
            TableCell("মেডিসিন", width = 75.dp, isHeader = true)
            TableCell("বাজার", width = 65.dp, isHeader = true)
            TableCell("বেতন", width = 75.dp, isHeader = true)
            TableCell("মেরামত", width = 70.dp, isHeader = true)
            TableCell("সম্পদ", width = 65.dp, isHeader = true)
            TableCell("বিদ্যুৎ", width = 65.dp, isHeader = true)
            TableCell("অন্যান্য", width = 65.dp, isHeader = true)
            TableCell("মোট ব্যয়", width = 95.dp, isHeader = true)
        }

        sortedExpenses.forEachIndexed { index, e ->
            val bg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 5.dp)
            ) {
                TableCell(BanglaNumberFormatter.formatShortDate(e.date), width = 75.dp, align = TextAlign.Center)
                TableCell(if (e.feedCost > 0) BanglaNumberFormatter.formatCurrency(e.feedCost) else "০", width = 75.dp)
                TableCell(if (e.medicineCost > 0) BanglaNumberFormatter.formatCurrency(e.medicineCost) else "০", width = 75.dp)
                TableCell(if (e.staffMarket > 0) BanglaNumberFormatter.formatCurrency(e.staffMarket) else "০", width = 65.dp)
                TableCell(if (e.staffSalary > 0) BanglaNumberFormatter.formatCurrency(e.staffSalary) else "০", width = 75.dp)
                TableCell(if (e.vehicleRepair > 0) BanglaNumberFormatter.formatCurrency(e.vehicleRepair) else "০", width = 70.dp)
                TableCell(if (e.assets > 0) BanglaNumberFormatter.formatCurrency(e.assets) else "০", width = 65.dp)
                TableCell(if (e.electricityBill > 0) BanglaNumberFormatter.formatCurrency(e.electricityBill) else "০", width = 65.dp)
                TableCell(if (e.otherExpense > 0) BanglaNumberFormatter.formatCurrency(e.otherExpense) else "০", width = 65.dp)
                TableCell(BanglaNumberFormatter.formatCurrency(e.totalExpense), width = 95.dp, isBold = true)
            }
        }

        val totalFeed = sortedExpenses.sumOf { it.feedCost }
        val totalMed = sortedExpenses.sumOf { it.medicineCost }
        val totalMarket = sortedExpenses.sumOf { it.staffMarket }
        val totalSalary = sortedExpenses.sumOf { it.staffSalary }
        val totalVehicle = sortedExpenses.sumOf { it.vehicleRepair }
        val totalAssets = sortedExpenses.sumOf { it.assets }
        val totalElec = sortedExpenses.sumOf { it.electricityBill }
        val totalOthers = sortedExpenses.sumOf { it.otherExpense }
        val grandTotal = sortedExpenses.sumOf { it.totalExpense }

        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 7.dp)
        ) {
            TableCell("সর্বমোট", width = 75.dp, isHeader = true, align = TextAlign.Center)
            TableCell(BanglaNumberFormatter.formatCurrency(totalFeed), width = 75.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalMed), width = 75.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalMarket), width = 65.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalSalary), width = 75.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalVehicle), width = 70.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalAssets), width = 65.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalElec), width = 65.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalOthers), width = 65.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(grandTotal), width = 95.dp, isHeader = true)
        }
    }
}

// 6. Profit & Loss Statement Table
@Composable
fun ProfitLossPdfTable(reports: List<DailyReportEntity>, expenses: List<MonthlyExpenseEntity>) {
    val scrollState = rememberScrollState()
    val allDates = remember(reports, expenses) {
        (reports.map { it.date } + expenses.map { it.date }).toSortedSet().toList()
    }
    val reportsByDate = remember(reports) { reports.associateBy { it.date } }
    val expensesByDate = remember(expenses) { expenses.associateBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(1.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 6.dp)
        ) {
            TableCell("তারিখ", width = 85.dp, isHeader = true, align = TextAlign.Center)
            TableCell("বিক্রয় আয়", width = 110.dp, isHeader = true)
            TableCell("খামার ব্যয়", width = 110.dp, isHeader = true)
            TableCell("মুনাফা / (ঘাটতি)", width = 130.dp, isHeader = true)
            TableCell("স্ট্যাটাস", width = 80.dp, isHeader = true, align = TextAlign.Center)
        }

        var totalSale = 0.0
        var totalExpense = 0.0
        var totalNet = 0.0

        allDates.forEachIndexed { index, date ->
            val r = reportsByDate[date]
            val e = expensesByDate[date]

            val sale = r?.totalSale ?: 0.0
            val exp = (r?.medicineCost ?: 0.0) + (e?.totalExpense ?: 0.0)
            val net = sale - exp
            val isProfit = net >= 0

            totalSale += sale
            totalExpense += exp
            totalNet += net

            val bg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 5.dp)
            ) {
                TableCell(BanglaNumberFormatter.formatShortDate(date), width = 85.dp, align = TextAlign.Center)
                TableCell(BanglaNumberFormatter.formatCurrency(sale), width = 110.dp)
                TableCell(BanglaNumberFormatter.formatCurrency(exp), width = 110.dp)
                TableCell(
                    text = BanglaNumberFormatter.formatCurrency(net),
                    width = 130.dp,
                    isBold = true
                )
                TableCell(
                    text = if (isProfit) "মুনাফা" else "ঘাটতি",
                    width = 80.dp,
                    isBold = true,
                    align = TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 7.dp)
        ) {
            TableCell("সর্বমোট", width = 85.dp, isHeader = true, align = TextAlign.Center)
            TableCell(BanglaNumberFormatter.formatCurrency(totalSale), width = 110.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalExpense), width = 110.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalNet), width = 130.dp, isHeader = true)
            TableCell(if (totalNet >= 0) "মুনাফা" else "ঘাটতি", width = 80.dp, isHeader = true, align = TextAlign.Center)
        }
    }
}

@Composable
fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isHeader: Boolean = false,
    isBold: Boolean = false,
    align: TextAlign = TextAlign.End
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            color = Color.Black,
            fontSize = if (isHeader) 11.sp else 10.sp,
            fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal
        ),
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        textAlign = align
    )
}

fun printHtmlDocument(context: Context, docName: String, html: String) {
    try {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(docName)
                printManager?.print(
                    docName,
                    printAdapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html; charset=utf-8", "UTF-8", null)
    } catch (e: Exception) {
        SnackbarController.showError("প্রিন্ট সেবা চালু করা যায়নি: ${e.message}")
    }
}

fun generateHtmlContent(
    title: String,
    farmProfile: FarmProfileEntity,
    dailyReports: List<DailyReportEntity>,
    allReports: List<DailyReportEntity> = dailyReports,
    baselineStock: Int = 0,
    expenses: List<MonthlyExpenseEntity>,
    reportCategory: String = "DAILY"
): String {
    val sortedReports = dailyReports.sortedBy { it.date }
    val sortedExpenses = expenses.sortedBy { it.date }
    val currentDateStr = BanglaNumberFormatter.toBanglaDigits(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()))

    val logoHtml = if (farmProfile.logoUri.isNotBlank()) {
        """<img src="${farmProfile.logoUri}" style="max-height: 65px; max-width: 85px; object-fit: contain; border-radius: 6px;" alt="Farm Logo" />"""
    } else if (farmProfile.logoEmoji.isNotBlank() && farmProfile.logoEmoji != "🐔") {
        """<div class="emoji-logo">${farmProfile.logoEmoji}</div>"""
    } else {
        """<div class="emoji-logo">🐔</div>"""
    }

    // Find all distinct months (YYYY-MM) present in the dataset
    val distinctMonths = (sortedReports.map { it.date.take(7) } + sortedExpenses.map { it.date.take(7) })
        .filter { it.length == 7 && it.contains("-") }
        .toSortedSet()
        .toList()

    val monthsToRender = if (distinctMonths.isNotEmpty()) distinctMonths else listOf("")
    val pagesHtml = StringBuilder()

    for (month in monthsToRender) {
        val monthReports = if (month.isNotBlank()) sortedReports.filter { it.date.startsWith(month) } else sortedReports
        val monthExpenses = if (month.isNotBlank()) sortedExpenses.filter { it.date.startsWith(month) } else sortedExpenses

        if (monthReports.isEmpty() && monthExpenses.isEmpty() && monthsToRender.size > 1) {
            continue
        }

        val monthLabel = if (month.isNotBlank()) BanglaNumberFormatter.formatYearMonth(month) else "সকল রেকর্ড"

        val tableHeaders: String
        val tableRows = StringBuilder()
        var rowCount = 0

        when (reportCategory) {
            "MONTHLY" -> {
                tableHeaders = "<th>তারিখ</th><th>উৎপাদন</th><th>বিক্রয়</th><th>বিক্রয় আয়</th><th>মাসিক ব্যয়</th><th>নিট উদ্বৃত্ত</th>"
                val allDates = (monthReports.map { it.date } + monthExpenses.map { it.date }).toSortedSet().toList()
                val reportsByDate = monthReports.associateBy { it.date }
                val expensesByDate = monthExpenses.associateBy { it.date }
                rowCount = allDates.size

                var sumProd = 0
                var sumSold = 0
                var sumSale = 0.0
                var sumMonthlyExp = 0.0
                var sumNet = 0.0

                for (date in allDates) {
                    val r = reportsByDate[date]
                    val e = expensesByDate[date]
                    val prod = r?.eggProduction ?: 0
                    val sold = r?.eggSold ?: 0
                    val sale = r?.totalSale ?: 0.0
                    val monthlyExp = e?.totalExpense ?: 0.0
                    val net = sale - monthlyExp

                    sumProd += prod
                    sumSold += sold
                    sumSale += sale
                    sumMonthlyExp += monthlyExp
                    sumNet += net

                    tableRows.append("<tr>")
                    tableRows.append("<td class='text-center'>${BanglaNumberFormatter.formatShortDate(date)}</td>")
                    tableRows.append("<td>${if (prod > 0) BanglaNumberFormatter.formatNumber(prod) else "-"}</td>")
                    tableRows.append("<td>${if (sold > 0) BanglaNumberFormatter.formatNumber(sold) else "-"}</td>")
                    tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(sale)}</td>")
                    tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(monthlyExp)}</td>")
                    tableRows.append("<td style='font-weight:bold; color:${if (net >= 0) "#0D631B" else "#BA1A1A"};'>${BanglaNumberFormatter.formatCurrency(net)}</td>")
                    tableRows.append("</tr>")
                }

                tableRows.append("<tr class='total-row'>")
                tableRows.append("<td class='text-center'>সর্বমোট</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatNumber(sumProd)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatNumber(sumSold)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(sumSale)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(sumMonthlyExp)}</td>")
                tableRows.append("<td style='color:${if (sumNet >= 0) "#0D631B" else "#BA1A1A"};'>${BanglaNumberFormatter.formatCurrency(sumNet)}</td></tr>")
            }

            "SALES" -> {
                tableHeaders = "<th>তারিখ</th><th>বিক্রির ডিম (টি)</th><th>দর/টি</th><th>দর/১০০টি</th><th>মোট বিক্রয় মূল্য</th><th>মন্তব্য</th>"
                val salesReports = monthReports.filter { it.eggSold > 0 || it.totalSale > 0.0 }
                rowCount = salesReports.size
                for (r in salesReports) {
                    val pricePerHundred = r.eggPrice * 100
                    val remarkText = if (r.remarks.isNotBlank()) r.remarks else "নগদ বিক্রয়"
                    tableRows.append("<tr>")
                    tableRows.append("<td class='text-center'>${BanglaNumberFormatter.formatShortDate(r.date)}</td>")
                    tableRows.append("<td>${BanglaNumberFormatter.formatNumber(r.eggSold)}</td>")
                    tableRows.append("<td>${BanglaNumberFormatter.formatDecimal(r.eggPrice)}</td>")
                    tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(pricePerHundred)}</td>")
                    tableRows.append("<td style='font-weight:bold; color:#0D631B;'>${BanglaNumberFormatter.formatCurrency(r.totalSale)}</td>")
                    tableRows.append("<td style='text-align:left;'>$remarkText</td>")
                    tableRows.append("</tr>")
                }
                val totalEggs = salesReports.sumOf { it.eggSold }
                val totalRevenue = salesReports.sumOf { it.totalSale }
                val avgPrice = if (totalEggs > 0) totalRevenue / totalEggs else 0.0
                tableRows.append("<tr class='total-row'>")
                tableRows.append("<td class='text-center'>সর্বমোট</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatNumber(totalEggs)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatDecimal(avgPrice)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(avgPrice * 100)}</td>")
                tableRows.append("<td style='color:#0D631B;'>${BanglaNumberFormatter.formatCurrency(totalRevenue)}</td><td>-</td></tr>")
            }

            "PRODUCTION" -> {
                tableHeaders = "<th>তারিখ</th><th>মোট মুরগি</th><th>মৃত মুরগি</th><th>মৃত্যুর হার %</th><th>ডিম উৎপাদন</th><th>লেইং হার %</th><th>মন্তব্য</th>"
                rowCount = monthReports.size
                for (r in monthReports) {
                    val mortalityRate = if (r.currentBirds > 0) (r.deadBirds.toDouble() / r.currentBirds * 100) else 0.0
                    val layingRate = if (r.currentBirds > 0) (r.eggProduction.toDouble() / r.currentBirds * 100) else 0.0
                    val remarkText = if (r.remarks.isNotBlank()) r.remarks else "স্বাভাবিক"
                    tableRows.append("<tr>")
                    tableRows.append("<td class='text-center'>${BanglaNumberFormatter.formatShortDate(r.date)}</td>")
                    tableRows.append("<td>${BanglaNumberFormatter.formatNumber(r.currentBirds)}</td>")
                    tableRows.append("<td>${if (r.deadBirds > 0) BanglaNumberFormatter.formatNumber(r.deadBirds) else "০"}</td>")
                    tableRows.append("<td>${String.format(Locale.US, "%.2f%%", mortalityRate)}</td>")
                    tableRows.append("<td style='font-weight:bold; color:#0D631B;'>${BanglaNumberFormatter.formatNumber(r.eggProduction)}</td>")
                    tableRows.append("<td>${String.format(Locale.US, "%.1f%%", layingRate)}</td>")
                    tableRows.append("<td style='text-align:left;'>$remarkText</td>")
                    tableRows.append("</tr>")
                }
                val totalMortality = monthReports.sumOf { it.deadBirds }
                val totalProd = monthReports.sumOf { it.eggProduction }
                val avgBirds = if (monthReports.isNotEmpty()) monthReports.map { it.currentBirds }.average().toInt() else 0
                val overallLayingRate = if (avgBirds > 0 && monthReports.isNotEmpty()) (totalProd.toDouble() / (avgBirds * monthReports.size) * 100) else 0.0
                tableRows.append("<tr class='total-row'>")
                tableRows.append("<td class='text-center'>সর্বমোট / গড়</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatNumber(avgBirds)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatNumber(totalMortality)}</td>")
                tableRows.append("<td>-</td>")
                tableRows.append("<td style='color:#0D631B;'>${BanglaNumberFormatter.formatNumber(totalProd)}</td>")
                tableRows.append("<td>${String.format(Locale.US, "%.1f%%", overallLayingRate)}</td><td>-</td></tr>")
            }

            "EXPENSE" -> {
                tableHeaders = "<th>তারিখ</th><th>খাদ্য</th><th>মেডিসিন</th><th>বাজার</th><th>বেতন</th><th>মেরামত</th><th>সম্পদ</th><th>বিদ্যুৎ</th><th>অন্যান্য</th><th>মোট ব্যয়</th>"
                rowCount = monthExpenses.size
                for (e in monthExpenses) {
                    tableRows.append("<tr>")
                    tableRows.append("<td class='text-center'>${BanglaNumberFormatter.formatShortDate(e.date)}</td>")
                    tableRows.append("<td>${if (e.feedCost > 0) BanglaNumberFormatter.formatCurrency(e.feedCost) else "০"}</td>")
                    tableRows.append("<td>${if (e.medicineCost > 0) BanglaNumberFormatter.formatCurrency(e.medicineCost) else "০"}</td>")
                    tableRows.append("<td>${if (e.staffMarket > 0) BanglaNumberFormatter.formatCurrency(e.staffMarket) else "০"}</td>")
                    tableRows.append("<td>${if (e.staffSalary > 0) BanglaNumberFormatter.formatCurrency(e.staffSalary) else "০"}</td>")
                    tableRows.append("<td>${if (e.vehicleRepair > 0) BanglaNumberFormatter.formatCurrency(e.vehicleRepair) else "০"}</td>")
                    tableRows.append("<td>${if (e.assets > 0) BanglaNumberFormatter.formatCurrency(e.assets) else "০"}</td>")
                    tableRows.append("<td>${if (e.electricityBill > 0) BanglaNumberFormatter.formatCurrency(e.electricityBill) else "০"}</td>")
                    tableRows.append("<td>${if (e.otherExpense > 0) BanglaNumberFormatter.formatCurrency(e.otherExpense) else "০"}</td>")
                    tableRows.append("<td style='font-weight:bold; color:#BA1A1A;'>${BanglaNumberFormatter.formatCurrency(e.totalExpense)}</td>")
                    tableRows.append("</tr>")
                }
                val totalFeed = monthExpenses.sumOf { it.feedCost }
                val totalMed = monthExpenses.sumOf { it.medicineCost }
                val totalMarket = monthExpenses.sumOf { it.staffMarket }
                val totalSalary = monthExpenses.sumOf { it.staffSalary }
                val totalVehicle = monthExpenses.sumOf { it.vehicleRepair }
                val totalAssets = monthExpenses.sumOf { it.assets }
                val totalElec = monthExpenses.sumOf { it.electricityBill }
                val totalOthers = monthExpenses.sumOf { it.otherExpense }
                val grandTotal = monthExpenses.sumOf { it.totalExpense }

                tableRows.append("<tr class='total-row'>")
                tableRows.append("<td class='text-center'>সর্বমোট</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalFeed)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalMed)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalMarket)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalSalary)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalVehicle)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalAssets)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalElec)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalOthers)}</td>")
                tableRows.append("<td style='color:#BA1A1A;'>${BanglaNumberFormatter.formatCurrency(grandTotal)}</td></tr>")
            }

            "PROFIT_LOSS" -> {
                tableHeaders = "<th>তারিখ</th><th>বিক্রয় আয়</th><th>খামার ব্যয়</th><th>মুনাফা / (ঘাটতি)</th><th>স্ট্যাটাস</th>"
                val allDates = (monthReports.map { it.date } + monthExpenses.map { it.date }).toSortedSet().toList()
                val reportsByDate = monthReports.associateBy { it.date }
                val expensesByDate = monthExpenses.associateBy { it.date }
                rowCount = allDates.size

                var totalSale = 0.0
                var totalExpense = 0.0
                var totalNet = 0.0

                for (date in allDates) {
                    val r = reportsByDate[date]
                    val e = expensesByDate[date]

                    val sale = r?.totalSale ?: 0.0
                    val exp = (r?.medicineCost ?: 0.0) + (e?.totalExpense ?: 0.0)
                    val net = sale - exp
                    val isProfit = net >= 0

                    totalSale += sale
                    totalExpense += exp
                    totalNet += net

                    tableRows.append("<tr>")
                    tableRows.append("<td class='text-center'>${BanglaNumberFormatter.formatShortDate(date)}</td>")
                    tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(sale)}</td>")
                    tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(exp)}</td>")
                    tableRows.append("<td style='font-weight:bold; color:${if (isProfit) "#0D631B" else "#BA1A1A"};'>${BanglaNumberFormatter.formatCurrency(net)}</td>")
                    tableRows.append("<td style='text-align:center; font-weight:bold; color:${if (isProfit) "#0D631B" else "#BA1A1A"};'>${if (isProfit) "মুনাফা" else "ঘাটতি"}</td>")
                    tableRows.append("</tr>")
                }

                tableRows.append("<tr class='total-row'>")
                tableRows.append("<td class='text-center'>সর্বমোট</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalSale)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalExpense)}</td>")
                tableRows.append("<td style='color:${if (totalNet >= 0) "#0D631B" else "#BA1A1A"};'>${BanglaNumberFormatter.formatCurrency(totalNet)}</td>")
                tableRows.append("<td style='text-align:center;'>${if (totalNet >= 0) "মুনাফা" else "ঘাটতি"}</td></tr>")
            }

            else -> {
                tableHeaders = "<th>তারিখ</th><th>মুরগি</th><th>মৃত</th><th>উৎপাদন</th><th>বিক্রয়</th><th>দর</th><th>মোট বিক্রয়</th>"
                rowCount = monthReports.size
                for (r in monthReports) {
                    tableRows.append("<tr>")
                    tableRows.append("<td class='text-center'>${BanglaNumberFormatter.formatShortDate(r.date)}</td>")
                    tableRows.append("<td>${BanglaNumberFormatter.formatNumber(r.currentBirds)}</td>")
                    tableRows.append("<td>${if (r.deadBirds > 0) BanglaNumberFormatter.formatNumber(r.deadBirds) else "০"}</td>")
                    tableRows.append("<td>${BanglaNumberFormatter.formatNumber(r.eggProduction)}</td>")
                    tableRows.append("<td>${BanglaNumberFormatter.formatNumber(r.eggSold)}</td>")
                    tableRows.append("<td>${BanglaNumberFormatter.formatDecimal(r.eggPrice)}</td>")
                    tableRows.append("<td style='font-weight:bold; color:#0D631B;'>${BanglaNumberFormatter.formatCurrency(r.totalSale)}</td>")
                    tableRows.append("</tr>")
                }
                val totalProd = monthReports.sumOf { it.eggProduction }
                val totalSold = monthReports.sumOf { it.eggSold }
                val totalSale = monthReports.sumOf { it.totalSale }
                tableRows.append("<tr class='total-row'>")
                tableRows.append("<td class='text-center'>সর্বমোট</td><td>-</td><td>-</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatNumber(totalProd)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatNumber(totalSold)}</td>")
                tableRows.append("<td>-</td>")
                tableRows.append("<td style='color:#0D631B;'>${BanglaNumberFormatter.formatCurrency(totalSale)}</td></tr>")
            }
        }

        val tableClass = if (rowCount <= 12) "table-spacious" else "table-standard"

        pagesHtml.append("""
            <div class="page-container">
                <div class="page-body">
                    <div class="header">
                        <div class="header-logo">$logoHtml</div>
                        <div class="header-text">
                            <h1 class="title">${farmProfile.farmName}</h1>
                            <div class="subtitle">লেয়ার পোল্ট্রি ফার্ম</div>
                            <div class="subtitle">প্রোঃ ${farmProfile.ownerName} | মোবাইলঃ ${farmProfile.mobileNumber}</div>
                            <div class="subtitle">ঠিকানাঃ ${farmProfile.address}</div>
                        </div>
                        <div class="header-spacer"></div>
                    </div>
                    <div class="meta">
                        <div style="display: flex; flex-direction: column; align-items: flex-start; gap: 4px;">
                            <strong>$title</strong>
                            <span class="report-month-tag">মাসঃ $monthLabel</span>
                        </div>
                        <span>তারিখ: $currentDateStr</span>
                    </div>
                    <table class="$tableClass">
                        <thead>
                            <tr>$tableHeaders</tr>
                        </thead>
                        <tbody>
                            $tableRows
                        </tbody>
                    </table>
                </div>
                <div class="footer-signatures">
                    <div class="sig-line">প্রস্তুতকারক</div>
                    <div class="sig-line">অনুমোদনকারী</div>
                </div>
            </div>
        """.trimIndent())
    }

    return """
        <!DOCTYPE html>
        <html lang="bn">
        <head>
            <meta charset="utf-8">
            <title>${farmProfile.farmName} - $title</title>
            <style>
                @page {
                    size: A4 portrait;
                    margin: 8mm 10mm 8mm 10mm;
                }

                *, *:before, *:after {
                    box-sizing: border-box;
                }

                body {
                    font-family: 'SolaimanLipi', 'Noto Sans Bengali', Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                    color: #111111;
                    background-color: #ffffff;
                    font-size: 11.5px;
                    -webkit-print-color-adjust: exact;
                    print-color-adjust: exact;
                }

                .page-container {
                    width: 100%;
                    min-height: 275mm;
                    display: flex;
                    flex-direction: column;
                    justify-content: space-between;
                    page-break-after: always;
                    break-after: page;
                    box-sizing: border-box;
                    padding-bottom: 1mm;
                }

                .page-container:last-child {
                    page-break-after: avoid;
                    break-after: auto;
                }

                .page-body {
                    flex: 1 0 auto;
                    width: 100%;
                }

                /* ─── অফিসিয়াল প্যাড / লেটারহেড ─── */
                .header {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    border-bottom: 2px solid #0D631B;
                    padding-top: 0;
                    padding-bottom: 6px;
                    margin-bottom: 8px;
                }

                .header-logo {
                    flex: 0 0 80px;
                    text-align: left;
                }

                .header-logo img {
                    max-height: 55px;
                    max-width: 80px;
                    object-fit: contain;
                    border-radius: 6px;
                }

                .header-logo .emoji-logo {
                    font-size: 34px;
                    line-height: 1;
                }

                .header-text {
                    flex: 1 1 auto;
                    text-align: center;
                }

                .header-text .title {
                    font-size: 19px;
                    font-weight: bold;
                    color: #0D631B;
                    margin: 0;
                    line-height: 1.2;
                    letter-spacing: 0.3px;
                }

                .header-text .subtitle {
                    font-size: 11px;
                    color: #222222;
                    margin: 1px 0;
                    font-weight: 500;
                    line-height: 1.25;
                }

                .header-spacer {
                    flex: 0 0 80px;
                }

                /* ─── রিপোর্ট মেটাডাটা ও তারিখ ─── */
                .meta {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 6px;
                    font-size: 12px;
                    font-weight: 600;
                }

                .meta strong {
                    font-size: 12.5px;
                    color: #111111;
                }

                .report-month-tag {
                    font-weight: bold;
                    color: #0D631B;
                    background-color: #E8F5E9;
                    padding: 1.5px 7px;
                    border-radius: 4px;
                    border: 1px solid #C8E6C9;
                    font-size: 11px;
                    display: inline-block;
                }

                .meta span {
                    color: #444444;
                    font-size: 11.5px;
                }

                /* ─── ডেটা টেবিল ─── */
                table {
                    width: 100%;
                    border-collapse: separate;
                    border-spacing: 0;
                    margin-top: 4px;
                    font-size: 11px;
                    border-radius: 5px;
                    overflow: hidden;
                    border: 1px solid #D0D0D0;
                    box-shadow: 0 1px 4px rgba(0,0,0,0.03);
                    page-break-inside: auto;
                }

                th, td {
                    text-align: right;
                    border-bottom: 1px solid #E0E0E0;
                    border-right: 1px solid #E0E0E0;
                    font-weight: 500;
                }

                table.table-standard th, table.table-standard td {
                    padding: 3.6px 6px;
                    line-height: 1.2;
                }

                table.table-spacious th, table.table-spacious td {
                    padding: 5.5px 7.5px;
                    line-height: 1.25;
                    font-size: 11.5px;
                }

                th:last-child, td:last-child {
                    border-right: none;
                }

                tbody tr:last-child td {
                    border-bottom: none;
                }

                th {
                    background-color: #0D631B !important;
                    color: #FFFFFF !important;
                    font-weight: 600;
                    text-align: center;
                    font-size: 11px;
                    letter-spacing: 0.2px;
                    padding: 4.5px 6px;
                }

                td.text-center {
                    text-align: center;
                }

                tr:nth-child(even) {
                    background-color: #F4F9F5;
                }

                tr.total-row {
                    background-color: #E8F5E9 !important;
                    page-break-inside: avoid;
                }

                tr.total-row td {
                    color: #0B4D16;
                    border-top: 2px solid #0D631B;
                    font-weight: 700;
                    font-size: 11.5px;
                    padding: 4.2px 6px;
                }

                /* ─── সিগনেচার সেকশন ─── */
                .footer-signatures {
                    display: flex;
                    justify-content: space-between;
                    padding: 0 30px;
                    margin-top: 32px;
                    margin-bottom: 3mm;
                    page-break-inside: avoid;
                }

                .sig-line {
                    border-top: 1.5px solid #333333;
                    width: 130px;
                    text-align: center;
                    padding-top: 5px;
                    font-size: 11.5px;
                    font-weight: 600;
                    color: #222222;
                }
            </style>
        </head>
        <body>
            $pagesHtml
        </body>
        </html>
    """.trimIndent()
}
