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
                                            "কাজী এগ্রোটেক অফিসিয়াল রিপোর্ট: $title\n" +
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
                            // Farm Header
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
                                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)),
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

                                Spacer(modifier = Modifier.size(56.dp))
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFF0D631B), thickness = 2.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Report Title & Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0D631B)
                                    )
                                )
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
                                        Text("পরিমাণ (৳)", modifier = Modifier.weight(1.4f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                        Text("মাধ্যম", modifier = Modifier.weight(1.0f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        Text("নোট", modifier = Modifier.weight(1.5f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    sortedPayments.forEachIndexed { i, p ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (i % 2 == 1) Color(0xFFF9FBF9) else Color.White)
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(BanglaNumberFormatter.formatNumber(i + 1), modifier = Modifier.weight(0.5f), fontSize = 11.sp, textAlign = TextAlign.Center)
                                            Text(p.shareholderName, modifier = Modifier.weight(1.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text(p.date, modifier = Modifier.weight(1.2f), fontSize = 11.sp)
                                            Text(BanglaNumberFormatter.formatCurrency(p.amount), modifier = Modifier.weight(1.4f), fontSize = 11.sp, textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold)
                                            Text(p.paymentMethod, modifier = Modifier.weight(1.0f), fontSize = 11.sp, textAlign = TextAlign.Center)
                                            Text(p.note.ifBlank { "—" }, modifier = Modifier.weight(1.5f), fontSize = 10.sp, color = Color(0xFF666666))
                                        }
                                        HorizontalDivider(color = Color(0xFFEEEEEE))
                                    }

                                    // Total Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFE8F5E9))
                                            .padding(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Text("সর্বমোট", modifier = Modifier.weight(3.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D631B))
                                        Text(BanglaNumberFormatter.formatCurrency(totalAmount), modifier = Modifier.weight(1.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D631B), textAlign = TextAlign.End)
                                        Spacer(modifier = Modifier.weight(2.5f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(55.dp))

                            // Signatures
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(100.dp).height(1.dp).background(Color(0xFF333333)))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("হিসাব রক্ষক", fontSize = 11.sp, color = Color(0xFF333333))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(100.dp).height(1.dp).background(Color(0xFF333333)))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("মালিকের স্বাক্ষর", fontSize = 11.sp, color = Color(0xFF333333))
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
            <title>$title</title>
            <style>
                @page { size: A4; margin: 12mm 15mm; }
                body { font-family: 'SolaimanLipi', 'Noto Sans Bengali', sans-serif; color: #222; margin: 0; padding: 0; }
                .header { text-align: center; border-bottom: 2px solid #0D631B; padding-bottom: 8px; margin-bottom: 12px; }
                .farm-name { font-size: 22px; font-weight: bold; color: #0D631B; margin: 0; }
                .farm-sub { font-size: 12px; color: #555; margin: 2px 0; }
                .title-bar { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 13px; font-weight: bold; color: #0D631B; }
                .summary-card { background: #F4F9F5; border: 1px solid #CCE8D2; border-radius: 6px; padding: 8px 16px; margin-bottom: 12px; display: flex; justify-content: space-around; font-size: 12px; }
                table { width: 100%; border-collapse: collapse; margin-bottom: 24px; font-size: 11px; }
                th { background-color: #0D631B; color: #FFF; padding: 6px 8px; text-align: center; border: 1px solid #0D631B; }
                td { padding: 6px 8px; border: 1px solid #DDD; }
                .even-row { background-color: #F9FBF9; }
                .total-row { background-color: #E8F5E9; font-weight: bold; color: #0D631B; }
                .signatures { display: flex; justify-content: space-between; margin-top: 55px; margin-bottom: 6mm; }
                .sig-box { width: 140px; text-align: center; border-top: 1px solid #333; padding-top: 4px; font-size: 11px; }
            </style>
        </head>
        <body>
            <div class="header">
                <div class="farm-name">${farmProfile.farmName}</div>
                <div class="farm-sub">লেয়ার পোল্ট্রি ফার্ম</div>
                <div class="farm-sub">প্রোঃ ${farmProfile.ownerName} | মোবাইলঃ ${farmProfile.mobileNumber}</div>
                <div class="farm-sub">ঠিকানাঃ ${farmProfile.address}</div>
            </div>

            <div class="title-bar">
                <span>$title</span>
                <span style="color: #555;">তারিখঃ $currentDateBangla</span>
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

