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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
import com.example.data.local.ShareholderPaymentEntity

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
                .systemBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
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
                            // Farm Header with Logo
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
                                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)),
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
                                        text = "প্রো: ${farmProfile.ownerName} • মোবাইল: ${farmProfile.mobileNumber} • ঠিকানা: ${farmProfile.address}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF555555),
                                            fontSize = 11.sp
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
                                    color = Color(0xFF0D631B)
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
                                    text = "তারিখঃ ${BanglaNumberFormatter.formatBanglaDate(BanglaNumberFormatter.getCurrentDateFormatted())}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555))
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

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

                            Spacer(modifier = Modifier.height(10.dp))

                            // Table Preview
                            val hScroll = rememberScrollState()
                            Box(modifier = Modifier.fillMaxWidth().horizontalScroll(hScroll)) {
                                Column(modifier = Modifier.width(540.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0D631B))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text("ক্রঃ", modifier = Modifier.weight(0.5f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        Text("শেয়ারহোল্ডার", modifier = Modifier.weight(1.8f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("তারিখ", modifier = Modifier.weight(1.2f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("পরিমাণ", modifier = Modifier.weight(1.4f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                        Text("মাধ্যম", modifier = Modifier.weight(1.0f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        Text("নোট", modifier = Modifier.weight(1.5f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    sortedPayments.forEachIndexed { i, p ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (i % 2 == 1) Color(0xFFF9FBF9) else Color.White)
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Text(BanglaNumberFormatter.formatNumber(i + 1), modifier = Modifier.weight(0.5f), fontSize = 10.5.sp, textAlign = TextAlign.Center)
                                            Text(p.shareholderName, modifier = Modifier.weight(1.8f), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                            Text(p.date, modifier = Modifier.weight(1.2f), fontSize = 10.5.sp)
                                            Text(BanglaNumberFormatter.formatCurrency(p.amount), modifier = Modifier.weight(1.4f), fontSize = 10.5.sp, textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold)
                                            Text(p.paymentMethod, modifier = Modifier.weight(1.0f), fontSize = 10.5.sp, textAlign = TextAlign.Center)
                                            Text(p.note.ifBlank { "—" }, modifier = Modifier.weight(1.5f), fontSize = 9.5.sp, color = Color(0xFF666666))
                                        }
                                        HorizontalDivider(color = Color(0xFFEEEEEE))
                                    }

                                    // Total Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFE8F5E9))
                                            .padding(horizontal = 8.dp, vertical = 7.dp)
                                    ) {
                                        Text("সর্বমোট", modifier = Modifier.weight(3.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D631B))
                                        Text(BanglaNumberFormatter.formatCurrency(totalAmount), modifier = Modifier.weight(1.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D631B), textAlign = TextAlign.End)
                                        Spacer(modifier = Modifier.weight(2.5f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(45.dp))

                            // Signatures
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(110.dp).height(1.dp).background(Color(0xFF333333)))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("প্রস্তুতকারক", fontSize = 11.sp, color = Color(0xFF333333), fontWeight = FontWeight.SemiBold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(110.dp).height(1.dp).background(Color(0xFF333333)))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("অনুমোদনকারী", fontSize = 11.sp, color = Color(0xFF333333), fontWeight = FontWeight.SemiBold)
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
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("বাতিল")
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
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF প্রিন্ট করুন")
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

    val logoHtml = if (farmProfile.logoUri.isNotBlank()) {
        """<img src="${farmProfile.logoUri}" style="max-height: 65px; max-width: 85px; object-fit: contain; border-radius: 6px;" alt="Logo" />"""
    } else if (farmProfile.logoEmoji.isNotBlank() && farmProfile.logoEmoji != "🐔") {
        """<div style="font-size: 38px; line-height: 1;">${farmProfile.logoEmoji}</div>"""
    } else {
        """<div style="font-size: 38px; line-height: 1;">🐔</div>"""
    }

    val distinctMonths = payments.map { p ->
        if (p.date.contains("-")) {
            val parts = p.date.split("-")
            if (parts.size >= 2) "${parts[0]}-${parts[1].padStart(2, '0')}" else p.date
        } else if (p.date.contains("/")) {
            val parts = p.date.split("/")
            if (parts.size == 3) "${parts[2]}-${parts[1].padStart(2, '0')}" else p.date
        } else p.date.take(7)
    }.filter { it.length == 7 && it.contains("-") }.distinct()

    val monthTagText = if (distinctMonths.size == 1) {
        BanglaNumberFormatter.formatYearMonth(distinctMonths.first())
    } else if (distinctMonths.size > 1) {
        "একাধিক মাস (${BanglaNumberFormatter.formatNumber(distinctMonths.size)} টি)"
    } else {
        "সকল রেকর্ড"
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
                <td style="text-align: left; font-size: 10.5px; color: #555;">${p.note.ifBlank { "—" }}</td>
            </tr>
        """.trimIndent())
    }

    return """
        <!DOCTYPE html>
        <html lang="bn">
        <head>
            <meta charset="utf-8">
            <title>$title - $monthTagText</title>
            <style>
                @page {
                    size: A4 portrait;
                    margin: 8mm 10mm 8mm 10mm;
                }
                *, *:before, *:after { box-sizing: border-box; }
                body {
                    font-family: 'SolaimanLipi', 'Noto Sans Bengali', Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                    color: #111111;
                    background-color: #ffffff;
                    font-size: 12.8px;
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
                    font-size: 11.5px;
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
                    align-items: flex-end;
                    margin-bottom: 6px;
                }
                .meta-left {
                    display: flex;
                    flex-direction: column;
                    align-items: flex-start;
                    gap: 3px;
                }
                .meta-left .report-title {
                    font-size: 13.5px;
                    font-weight: bold;
                    color: #111111;
                }
                .report-month-tag {
                    font-weight: bold;
                    color: #0D631B;
                    background-color: #E8F5E9;
                    padding: 2px 8px;
                    border-radius: 4px;
                    border: 1px solid #C8E6C9;
                    font-size: 11.5px;
                    display: inline-block;
                }
                .meta-right {
                    color: #444444;
                    font-size: 12px;
                    font-weight: 600;
                }
                .summary-card {
                    background: #F4F9F5;
                    border: 1px solid #CCE8D2;
                    border-radius: 6px;
                    padding: 6px 14px;
                    margin-bottom: 6px;
                    display: flex;
                    justify-content: space-around;
                    font-size: 12px;
                }
                table {
                    width: 100%;
                    border-collapse: separate;
                    border-spacing: 0;
                    margin-top: 4px;
                    font-size: 12.8px;
                    border-radius: 5px;
                    overflow: hidden;
                    border: 1px solid #D0D0D0;
                    box-shadow: 0 1px 4px rgba(0,0,0,0.03);
                    margin-bottom: 6px;
                }
                th, td {
                    border-bottom: 1px solid #E0E0E0;
                    border-right: 1px solid #E0E0E0;
                    padding: 4px 6px;
                    line-height: 1.2;
                }
                th:last-child, td:last-child {
                    border-right: none;
                }
                th {
                    background-color: #0D631B !important;
                    color: #FFFFFF !important;
                    font-weight: 700;
                    text-align: center;
                    font-size: 13.8px;
                    letter-spacing: 0.2px;
                    padding: 4px 6px;
                }
                tr:nth-child(even) {
                    background-color: #F4F9F5;
                }
                tr.total-row td {
                    color: #0B4D16;
                    border-top: 2px solid #0D631B;
                    font-weight: 700;
                    font-size: 13px;
                    background-color: #E8F5E9 !important;
                }
                .footer-signatures {
                    display: flex;
                    justify-content: space-between;
                    padding: 0 40px;
                    margin-top: 28px;
                    margin-bottom: 2mm;
                    page-break-inside: avoid;
                }
                .sig-line {
                    border-top: 1.5px solid #333333;
                    width: 140px;
                    text-align: center;
                    padding-top: 5px;
                    font-size: 12px;
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
                            <div class="subtitle">প্রো: ${farmProfile.ownerName} &bull; মোবাইল: ${farmProfile.mobileNumber} &bull; ঠিকানা: ${farmProfile.address}</div>
                        </div>
                        <div class="header-spacer"></div>
                    </div>

                    <div class="meta">
                        <div class="meta-left">
                            <div class="report-title">$title</div>
                            <div class="report-month-tag">মাস: $monthTagText</div>
                        </div>
                        <div class="meta-right">
                            <span>তারিখ: $currentDateBangla</span>
                        </div>
                    </div>

                    <div class="summary-card">
                        <div><strong>মোট লেনদেন:</strong> ${BanglaNumberFormatter.formatNumber(payments.size)} বার</div>
                        <div><strong>মোট পরিশোধিত অর্থ:</strong> ${BanglaNumberFormatter.formatCurrency(totalAmount)}</div>
                    </div>

                    <table>
                        <thead>
                            <tr>
                                <th style="width: 6%;">ক্রঃ</th>
                                <th style="width: 25%;">শেয়ারহোল্ডার</th>
                                <th style="width: 15%;">তারিখ</th>
                                <th style="width: 20%;">পরিমাণ</th>
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
                </div>

                <div class="footer-signatures">
                    <div class="sig-line">প্রস্তুতকারক</div>
                    <div class="sig-line">অনুমোদনকারী</div>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
}
