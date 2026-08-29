package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.FarmProfileEntity
import com.example.data.local.ShareholderPaymentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShareholderPdfPreviewModalDialog(
    title: String,
    farmProfile: FarmProfileEntity,
    payments: List<ShareholderPaymentEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sortedPayments = remember(payments) { payments.sortedBy { it.date } }
    val totalAmount = remember(payments) { payments.sumOf { it.amount } }

    val distinctMonths = remember(sortedPayments) {
        sortedPayments.mapNotNull { p ->
            val raw = p.date.trim()
            if (raw.contains("-") && raw.length >= 7) raw.take(7)
            else if (raw.contains("/") && raw.length >= 7) {
                val parts = raw.split("/")
                if (parts.size == 3) "${parts[2]}-${parts[1].padStart(2, '0')}" else null
            } else null
        }.distinct()
    }
    val monthLabel = remember(distinctMonths) {
        if (distinctMonths.size == 1) BanglaNumberFormatter.formatYearMonth(distinctMonths.first()) else ""
    }
    val displayTitle = remember(title, monthLabel) {
        if (monthLabel.isNotBlank() && !title.contains(monthLabel)) "$title ($monthLabel)" else title
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
                                    val printDocName = "Kazi_Agrotech_Shareholder_${System.currentTimeMillis()}"
                                    printShareholderHtml(
                                        context = context,
                                        docName = printDocName,
                                        html = generateShareholderHtml(
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
                                            "কাজী এগ্রোটেক অফিসিয়াল রিপোর্ট: $displayTitle\n" +
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
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF0FDF4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FarmLogoDisplay(
                                        logoUri = farmProfile.logoUri,
                                        logoEmoji = farmProfile.logoEmoji,
                                        modifier = Modifier
                                            .size(52.dp)
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
                                        text = "লেয়ার পোল্ট্রি ফার্ম",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF2E7D32),
                                            fontSize = 12.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "প্রোঃ ${farmProfile.ownerName} | মোবাইলঃ ${farmProfile.mobileNumber}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF444444),
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

                                Spacer(modifier = Modifier.size(56.dp))
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFF0D631B), thickness = 2.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Report Title & Date (Separate lines for Title and Month)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D631B)
                                        )
                                    )
                                    if (monthLabel.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "মাসঃ $monthLabel",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = "তারিখঃ ${BanglaNumberFormatter.formatBanglaDate(BanglaNumberFormatter.getCurrentDateFormatted())}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555))
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Summary Banner
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF4F9F5),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCCE8D2))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("মোট লেনদেন", fontSize = 11.sp, color = Color(0xFF555555))
                                        Text("${BanglaNumberFormatter.formatNumber(sortedPayments.size)} বার", fontWeight = FontWeight.Bold, color = Color(0xFF0D631B))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("মোট পরিশোধিত অর্থ", fontSize = 11.sp, color = Color(0xFF555555))
                                        Text(BanglaNumberFormatter.formatCurrency(totalAmount), fontWeight = FontWeight.Bold, color = Color(0xFF0D631B))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Data Table
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Column(modifier = Modifier.width(620.dp)) {
                                    // Table Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0D631B))
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ক্রঃ", modifier = Modifier.width(40.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                                        Text("শেয়ারহোল্ডার", modifier = Modifier.width(160.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                                        Text("তারিখ", modifier = Modifier.width(100.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                                        Text("পরিমাণ (৳)", modifier = Modifier.width(110.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                                        Text("মাধ্যম", modifier = Modifier.width(80.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                                        Text("নোট", modifier = Modifier.width(130.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                                    }

                                    sortedPayments.forEachIndexed { index, item ->
                                        val rowBg = if (index % 2 == 1) Color(0xFFF9FBF9) else Color.White
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(rowBg)
                                                .padding(vertical = 7.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(BanglaNumberFormatter.formatNumber(index + 1), modifier = Modifier.width(40.dp), fontSize = 11.sp, textAlign = TextAlign.Center)
                                            Text(item.shareholderName, modifier = Modifier.width(160.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111111))
                                            Text(BanglaNumberFormatter.formatBanglaDate(item.date), modifier = Modifier.width(100.dp), fontSize = 10.sp, textAlign = TextAlign.Center)
                                            Text(BanglaNumberFormatter.formatCurrency(item.amount), modifier = Modifier.width(110.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D631B), textAlign = TextAlign.End)
                                            Text(item.paymentMethod, modifier = Modifier.width(80.dp), fontSize = 11.sp, textAlign = TextAlign.Center)
                                            Text(item.note.ifBlank { "—" }, modifier = Modifier.width(130.dp), fontSize = 10.sp, color = Color(0xFF555555))
                                        }
                                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                                    }

                                    // Total Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFE8F5E9))
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("সর্বমোট", modifier = Modifier.width(300.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0D631B), textAlign = TextAlign.Center)
                                        Text(BanglaNumberFormatter.formatCurrency(totalAmount), modifier = Modifier.width(110.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0D631B), textAlign = TextAlign.End)
                                        Spacer(modifier = Modifier.width(210.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(42.dp))

                            // Signatures
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(120.dp).height(1.dp).background(Color(0xFF333333)))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("হিসাব রক্ষক", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(120.dp).height(1.dp).background(Color(0xFF333333)))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("মালিকের স্বাক্ষর", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
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
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("বন্ধ করুন")
                        }

                        Button(
                            onClick = {
                                val printDocName = "Kazi_Agrotech_Shareholder_${System.currentTimeMillis()}"
                                printShareholderHtml(
                                    context = context,
                                    docName = printDocName,
                                    html = generateShareholderHtml(
                                        title = title,
                                        farmProfile = farmProfile,
                                        payments = sortedPayments
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("প্রিন্ট / PDF")
                        }
                    }
                }
            }
        }
    }
}

private fun printShareholderHtml(context: Context, docName: String, html: String) {
    try {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(docName)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                printManager?.print(docName, printAdapter, printAttributes)
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html; charset=utf-8", "UTF-8", null)
    } catch (e: Exception) {
        // Fallback or ignore
    }
}

private fun generateShareholderHtml(
    title: String,
    farmProfile: FarmProfileEntity,
    payments: List<ShareholderPaymentEntity>
): String {
    val totalAmount = payments.sumOf { it.amount }
    val currentDateBangla = BanglaNumberFormatter.formatBanglaDate(BanglaNumberFormatter.getCurrentDateFormatted())

    val distinctMonths = payments.mapNotNull { p ->
        val raw = p.date.trim()
        if (raw.contains("-") && raw.length >= 7) raw.take(7)
        else if (raw.contains("/") && raw.length >= 7) {
            val parts = raw.split("/")
            if (parts.size == 3) "${parts[2]}-${parts[1].padStart(2, '0')}" else null
        } else null
    }.distinct()

    val monthLabel = if (distinctMonths.size == 1) {
        BanglaNumberFormatter.formatYearMonth(distinctMonths.first())
    } else ""

    val resolvedTitle = if (monthLabel.isNotBlank() && !title.contains(monthLabel)) {
        "$title ($monthLabel)"
    } else {
        title
    }

    val logoHtml = if (farmProfile.logoUri.isNotBlank()) {
        """<img src="${farmProfile.logoUri}" style="max-height: 48px; max-width: 70px; object-fit: contain; border-radius: 4px;" alt="Logo" />"""
    } else if (farmProfile.logoEmoji.isNotBlank() && farmProfile.logoEmoji != "🐔") {
        """<div class="emoji-logo" style="font-size: 32px; line-height: 1;">${farmProfile.logoEmoji}</div>"""
    } else {
        """<div class="emoji-logo" style="font-size: 32px; line-height: 1;">🐔</div>"""
    }

    val rowsHtml = StringBuilder()
    payments.forEachIndexed { index, p ->
        val bgClass = if (index % 2 == 1) "even-row" else ""
        rowsHtml.append("""
            <tr class="$bgClass">
                <td style="text-align: center;">${BanglaNumberFormatter.formatNumber(index + 1)}</td>
                <td style="text-align: left; font-weight: 600;">${p.shareholderName}</td>
                <td style="text-align: center;">${p.date}</td>
                <td style="text-align: right; font-weight: 600;">${BanglaNumberFormatter.formatCurrency(p.amount)}</td>
                <td style="text-align: center;">${p.paymentMethod}</td>
                <td style="text-align: left; font-size: 10px; color: #555;">${p.note.ifBlank { "—" }}</td>
            </tr>
        """.trimIndent())
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>$resolvedTitle</title>
            <style>
                @page { size: A4 portrait; margin: 8mm 10mm 8mm 10mm; }
                body { font-family: 'SolaimanLipi', 'Noto Sans Bengali', sans-serif; color: #222; margin: 0; padding: 0; }
                .header { display: flex; align-items: center; justify-content: space-between; border-bottom: 2px solid #0D631B; padding-bottom: 4px; margin-bottom: 8px; }
                .header-logo { flex: 0 0 70px; text-align: left; }
                .header-text { flex: 1 1 auto; text-align: center; }
                .header-spacer { flex: 0 0 70px; }
                .farm-name { font-size: 18px; font-weight: bold; color: #0D631B; margin: 0; line-height: 1.2; }
                .farm-sub { font-size: 10px; color: #444; margin: 1px 0; }
                .title-bar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px; }
                .summary-card { background: #F4F9F5; border: 1px solid #CCE8D2; border-radius: 4px; padding: 5px 12px; margin-bottom: 8px; display: flex; justify-content: space-around; font-size: 11px; }
                table { width: 100%; border-collapse: collapse; margin-bottom: 0; font-size: 10.5px; }
                th { background-color: #0D631B; color: #FFF; padding: 4px 6px; text-align: center; border: 1px solid #0D631B; font-size: 10.5px; }
                td { padding: 4px 6px; border: 1px solid #DDD; font-size: 10px; }
                .even-row { background-color: #F9FBF9; }
                .total-row { background-color: #E8F5E9; font-weight: bold; color: #0D631B; }
                .signatures { display: flex; justify-content: space-between; padding: 0 20px; margin-top: 42px; margin-bottom: 2mm; page-break-inside: avoid; break-inside: avoid; }
                .sig-box { width: 130px; text-align: center; border-top: 1.2px solid #333; padding-top: 4px; font-size: 10.5px; font-weight: 600; }
            </style>
        </head>
        <body>
            <div class="header">
                <div class="header-logo">$logoHtml</div>
                <div class="header-text">
                    <div class="farm-name">${farmProfile.farmName}</div>
                    <div class="farm-sub">লেয়ার পোল্ট্রি ফার্ম</div>
                    <div class="farm-sub">প্রোঃ ${farmProfile.ownerName} | মোবাইলঃ ${farmProfile.mobileNumber}</div>
                    <div class="farm-sub">ঠিকানাঃ ${farmProfile.address}</div>
                </div>
                <div class="header-spacer"></div>
            </div>

            <div class="title-bar">
                <div>
                    <div style="font-size: 13px; font-weight: bold; color: #0D631B;">$title</div>
                    ${if (monthLabel.isNotBlank()) """<div style="font-size: 11px; font-weight: 600; color: #2E7D32; margin-top: 2px;">মাসঃ $monthLabel</div>""" else ""}
                </div>
                <div style="color: #555; font-size: 10.5px; padding-top: 2px;">তারিখঃ $currentDateBangla</div>
            </div>

            <div class="summary-card">
                <div><strong>মোট লেনদেনঃ</strong> ${BanglaNumberFormatter.formatNumber(payments.size)} বার</div>
                <div><strong>মোট পরিশোধিত অর্থঃ</strong> ${BanglaNumberFormatter.formatCurrency(totalAmount)}</div>
            </div>

            <table>
                <thead>
                    <tr>
                        <th style="width: 6%;">ক্রঃ</th>
                        <th style="width: 25%;">শেয়ারহোল্ডার</th>
                        <th style="width: 15%;">তারিখ</th>
                        <th style="width: 20%;">পরিমাণ (৳)</th>
                        <th style="width: 14%;">মাধ্যম</th>
                        <th style="width: 20%;">নোট</th>
                    </tr>
                </thead>
                <tbody>
                    $rowsHtml
                    <tr class="total-row">
                        <td colspan="3" style="text-align: center;">সর্বমোট</td>
                        <td style="text-align: right;">${BanglaNumberFormatter.formatCurrency(totalAmount)}</td>
                        <td colspan="2"></td>
                    </tr>
                </tbody>
            </table>

            <div class="signatures">
                <div class="sig-box">হিসাব রক্ষক</div>
                <div class="sig-box">মালিকের স্বাক্ষর</div>
            </div>
        </body>
        </html>
    """.trimIndent()
}
