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
import androidx.compose.material.icons.filled.PictureAsPdf
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
import com.example.data.local.FarmProfileEntity
import com.example.data.local.StaffPaymentEntity

@Composable
fun StaffPdfPreviewModalDialog(
    title: String,
    farmProfile: FarmProfileEntity,
    payments: List<StaffPaymentEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sortedPayments = remember(payments) { payments.sortedBy { it.date } }
    val totalAmount = remember(payments) { payments.sumOf { it.amount } }

    val distinctMonths = remember(sortedPayments) {
        sortedPayments.map { p ->
            if (p.date.contains("-")) {
                val parts = p.date.split("-")
                if (parts.size >= 2) "${parts[0]}-${parts[1].padStart(2, '0')}" else p.date
            } else if (p.date.contains("/")) {
                val parts = p.date.split("/")
                if (parts.size == 3) "${parts[2]}-${parts[1].padStart(2, '0')}" else p.date
            } else p.date.take(7)
        }.filter { it.length == 7 && it.contains("-") }.distinct()
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
                                    val printDocName = "Kazi_Agrotech_Staff_${System.currentTimeMillis()}"
                                    printStaffHtml(
                                        context = context,
                                        docName = printDocName,
                                        html = generateStaffHtml(
                                            title = title,
                                            farmProfile = farmProfile,
                                            payments = sortedPayments
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
                                                    "মোবাইল: ${farmProfile.mobileNumber}\n\n" +
                                                    "মোট পেমেন্ট রেকর্ড: ${sortedPayments.size} টি\n" +
                                                    "মোট টাকার পরিমাণ: ${BanglaNumberFormatter.formatCurrency(totalAmount)}"
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
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header
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
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D631B)
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "প্রো: ${farmProfile.ownerName}  •  মোবাইল: ${BanglaNumberFormatter.toBanglaDigits(farmProfile.mobileNumber)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF333333)),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = farmProfile.address,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF444444)),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.size(60.dp))
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                thickness = 2.dp,
                                color = Color(0xFF0D631B)
                            )

                            // Title & Meta
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                )
                                Text(
                                    text = "তারিখ: ${BanglaNumberFormatter.getCurrentDateBangla()}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555))
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE8F5E9))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "মাস: $monthTagText",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D631B)
                                        )
                                    )
                                }
                                Text(
                                    text = "মোট পেমেন্ট: ${BanglaNumberFormatter.formatCurrency(totalAmount)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0D631B)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Table
                            StaffPaymentTablePreview(payments = sortedPayments)

                            Spacer(modifier = Modifier.height(35.dp))

                            // Signatures
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(110.dp).height(1.dp).background(Color.Black))
                                    Text(
                                        text = "ম্যানেজার",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(110.dp).height(1.dp).background(Color.Black))
                                    Text(
                                        text = "অনুমোদনকারী",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }

                // Modal Footer
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                val printDocName = "Kazi_Agrotech_Staff_${System.currentTimeMillis()}"
                                printStaffHtml(
                                    context = context,
                                    docName = printDocName,
                                    html = generateStaffHtml(
                                        title = title,
                                        farmProfile = farmProfile,
                                        payments = sortedPayments
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

@Composable
private fun StaffPaymentTablePreview(payments: List<StaffPaymentEntity>) {
    val scrollState = rememberScrollState()

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
            TableCell("স্টাফের নাম", width = 120.dp, isHeader = true, align = TextAlign.Start)
            TableCell("তারিখ", width = 85.dp, isHeader = true, align = TextAlign.Center)
            TableCell("পরিমাণ", width = 100.dp, isHeader = true)
            TableCell("পেমেন্ট মাধ্যম", width = 95.dp, isHeader = true, align = TextAlign.Center)
            TableCell("নোট", width = 120.dp, isHeader = true, align = TextAlign.Start)
        }

        payments.forEachIndexed { index, p ->
            val bg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 5.dp)
            ) {
                TableCell(p.staffName, width = 120.dp, align = TextAlign.Start)
                TableCell(BanglaNumberFormatter.formatShortDate(p.date), width = 85.dp, align = TextAlign.Center)
                TableCell(BanglaNumberFormatter.formatCurrency(p.amount), width = 100.dp, isBold = true)
                TableCell(p.paymentMethod, width = 95.dp, align = TextAlign.Center)
                TableCell(p.note.ifBlank { "—" }, width = 120.dp, align = TextAlign.Start)
            }
        }

        val totalAmount = payments.sumOf { it.amount }
        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 7.dp)
        ) {
            TableCell("সর্বমোট", width = 120.dp, isHeader = true, align = TextAlign.Start)
            TableCell("-", width = 85.dp, isHeader = true, align = TextAlign.Center)
            TableCell(BanglaNumberFormatter.formatCurrency(totalAmount), width = 100.dp, isHeader = true)
            TableCell("-", width = 95.dp, isHeader = true, align = TextAlign.Center)
            TableCell("-", width = 120.dp, isHeader = true, align = TextAlign.Start)
        }
    }
}

fun printStaffHtml(context: Context, docName: String, html: String) {
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

fun generateStaffHtml(
    title: String,
    farmProfile: FarmProfileEntity,
    payments: List<StaffPaymentEntity>
): String {
    val sortedPayments = payments.sortedBy { it.date }
    val currentDateStr = BanglaNumberFormatter.getCurrentDateBangla()

    val logoHtml = if (farmProfile.logoUri.isNotBlank()) {
        """<img src="${farmProfile.logoUri}" style="max-height: 55px; max-width: 80px; object-fit: contain; border-radius: 6px;" alt="Farm Logo" />"""
    } else if (farmProfile.logoEmoji.isNotBlank() && farmProfile.logoEmoji != "🐔") {
        """<div class="emoji-logo">${farmProfile.logoEmoji}</div>"""
    } else {
        """<div class="emoji-logo">🐔</div>"""
    }

    val tableRows = StringBuilder()
    var totalAmount = 0.0

    for (p in sortedPayments) {
        totalAmount += p.amount
        tableRows.append("<tr>")
        tableRows.append("<td style='text-align:left; font-weight:600;'>${p.staffName}</td>")
        tableRows.append("<td class='text-center'>${BanglaNumberFormatter.formatShortDate(p.date)}</td>")
        tableRows.append("<td style='font-weight:bold; color:#0D631B;'>${BanglaNumberFormatter.formatCurrency(p.amount)}</td>")
        tableRows.append("<td class='text-center'>${p.paymentMethod}</td>")
        tableRows.append("<td style='text-align:left;'>${if (p.note.isNotBlank()) p.note else "—"}</td>")
        tableRows.append("</tr>")
    }

    tableRows.append("<tr class='total-row'>")
    tableRows.append("<td style='text-align:left;'>সর্বমোট</td>")
    tableRows.append("<td class='text-center'>-</td>")
    tableRows.append("<td style='color:#0D631B;'>${BanglaNumberFormatter.formatCurrency(totalAmount)}</td>")
    tableRows.append("<td class='text-center'>-</td>")
    tableRows.append("<td style='text-align:left;'>-</td>")
    tableRows.append("</tr>")

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
                    padding: 3.6px 6px;
                    line-height: 1.2;
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
            <div class="page-container">
                <div class="page-body">
                    <div class="header">
                        <div class="header-logo">$logoHtml</div>
                        <div class="header-text">
                            <h1 class="title">${farmProfile.farmName}</h1>
                            <div class="subtitle">প্রো: ${farmProfile.ownerName} &bull; মোবাইল: ${farmProfile.mobileNumber}</div>
                            <div class="subtitle">${farmProfile.address}</div>
                        </div>
                        <div class="header-spacer"></div>
                    </div>

                    <div class="meta">
                        <strong>$title</strong>
                        <span>তারিখ: $currentDateStr</span>
                    </div>

                    <table>
                        <thead>
                            <tr>
                                <th>স্টাফের নাম</th>
                                <th>তারিখ</th>
                                <th>পরিমাণ</th>
                                <th>পেমেন্ট মাধ্যম</th>
                                <th>নোট</th>
                            </tr>
                        </thead>
                        <tbody>
                            $tableRows
                        </tbody>
                    </table>
                </div>

                <div class="footer-signatures">
                    <div class="sig-line">ম্যানেজার</div>
                    <div class="sig-line">অনুমোদনকারী</div>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
}
