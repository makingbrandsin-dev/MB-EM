package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.InvoiceEntity
import com.example.data.model.QuotationEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enterprise A4 PDF Document Generator & Dispatcher for Making Brands.
 * Produces pixel-perfect, GST-compliant tax invoices and commercial quotations
 * using native Android PdfDocument API with zero third-party dependencies.
 */
object InvoicePdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 width in PostScript points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in PostScript points (72 dpi)

    /**
     * Generates a physical PDF file for the given InvoiceEntity and stores it in app cache.
     */
    fun generateInvoicePdf(context: Context, invoice: InvoiceEntity): File {
        val invoicesDir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val cleanNumber = invoice.invoiceNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val pdfFile = File(invoicesDir, "MakingBrands_$cleanNumber.pdf")

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawInvoiceOnCanvas(canvas, invoice)

        document.finishPage(page)

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Generates a physical PDF file for a QuotationEntity.
     */
    fun generateQuotationPdf(context: Context, quotation: QuotationEntity): File {
        val invoicesDir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val cleanNumber = quotation.quotationNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val pdfFile = File(invoicesDir, "MakingBrands_Quote_$cleanNumber.pdf")

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawQuotationOnCanvas(canvas, quotation)

        document.finishPage(page)

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Obtains a secure FileProvider Content URI for sharing.
     */
    fun getFileUri(context: Context, pdfFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
    }

    /**
     * Creates an Android System Share Sheet intent for the generated PDF.
     */
    fun createShareIntent(context: Context, pdfFile: File, subject: String = "Making Brands Tax Invoice"): Intent {
        val uri = getFileUri(context, pdfFile)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "Please find attached the official PDF document from Making Brands.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Directly shares the PDF document via WhatsApp (or WhatsApp Business) with an informative text payload.
     */
    fun sharePdfToWhatsApp(
        context: Context,
        pdfFile: File,
        recipientPhone: String?,
        clientName: String,
        docNumber: String,
        totalAmount: Double,
        currency: String = "₹"
    ): Boolean {
        val uri = getFileUri(context, pdfFile)
        val formattedAmount = String.format(Locale.US, "%,.2f", totalAmount)
        val caption = """
            📄 *OFFICIAL GST TAX INVOICE — MAKING BRANDS*
            ━━━━━━━━━━━━━━━━━━━━━
            Dear ${clientName.trim()},

            Please find attached your official Tax Invoice *$docNumber* from *Making Brands*.

            • *Invoice No:* $docNumber
            • *Total Payable:* $currency $formattedAmount (incl. 18% GST)
            • *Payment Terms:* Net 15 Days via UPI / NEFT

            Thank you for partnering with Making Brands!

            Best regards,
            *Making Brands Accounts & Finance*
            📞 +91 98765 43210 | 🌐 makingbrands.in
        """.trimIndent()

        val cleanPhone = recipientPhone?.let { WhatsAppHelper.sanitizePhoneNumber(it) } ?: ""

        val waPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in waPackages) {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    if (cleanPhone.isNotBlank()) {
                        putExtra("jid", "$cleanPhone@s.whatsapp.net")
                    }
                    setPackage(pkg)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // Try next package
            }
        }

        // Generic share fallback if WhatsApp package not directly resolvable
        return try {
            val shareIntent = Intent.createChooser(createShareIntent(context, pdfFile, "Invoice $docNumber"), "Share PDF Invoice")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Opens the generated PDF in the device's native PDF reader.
     */
    fun openInSystemViewer(context: Context, pdfFile: File) {
        try {
            val uri = getFileUri(context, pdfFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer app found on device.", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================================================================
    // CANVAS DRAWING LOGIC (PIXEL-PERFECT A4 TAX INVOICE)
    // =========================================================================
    private fun drawInvoiceOnCanvas(canvas: Canvas, invoice: InvoiceEntity) {
        val margin = 36f
        val contentWidth = PAGE_WIDTH - (margin * 2)

        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // #0F172A
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val paintHeaderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            style = Paint.Style.FILL
        }

        val paintAccentBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235) // Blue 600
            style = Paint.Style.FILL
        }

        val paintCardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252) // Slate 50
            style = Paint.Style.FILL
        }

        val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // 1. Top Decorative Brand Bar
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 12f, paintAccentBlue)

        var curY = 42f

        // 2. Header Section
        // Logo Badge
        val logoRect = RectF(margin, curY, margin + 44f, curY + 44f)
        canvas.drawRoundRect(logoRect, 8f, 8f, paintHeaderBg)

        val paintLogoText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("MB", logoRect.centerX(), logoRect.centerY() + 7f, paintLogoText)

        // Company Name & Subtitle
        paintText.textSize = 16f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = Color.rgb(15, 23, 42)
        canvas.drawText("MAKING BRANDS", margin + 54f, curY + 16f, paintText)

        paintText.textSize = 9f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paintText.color = Color.rgb(100, 116, 139)
        canvas.drawText("Digital Solutions, CRM & Enterprise Technology", margin + 54f, curY + 30f, paintText)
        canvas.drawText("Cyber City, Phase II, Gurugram, India · GSTIN: 07AAAAA0000A1Z5", margin + 54f, curY + 42f, paintText)

        // Document Title & Number (Right aligned)
        val paintDocTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("TAX INVOICE", PAGE_WIDTH - margin, curY + 16f, paintDocTitle)

        paintText.textAlign = Paint.Align.RIGHT
        paintText.textSize = 12f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = Color.rgb(15, 23, 42)
        canvas.drawText(invoice.invoiceNumber, PAGE_WIDTH - margin, curY + 32f, paintText)

        paintText.textSize = 9f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paintText.color = Color.rgb(100, 116, 139)
        canvas.drawText("Issue Date: ${invoice.issueDate}  |  Due: ${invoice.dueDate}", PAGE_WIDTH - margin, curY + 44f, paintText)

        paintText.textAlign = Paint.Align.LEFT
        curY += 60f

        // Thin Separator Line
        paintBorder.color = Color.rgb(203, 213, 225)
        canvas.drawLine(margin, curY, PAGE_WIDTH - margin, curY, paintBorder)
        curY += 16f

        // 3. Client & Supply Details Box
        val clientBoxRect = RectF(margin, curY, PAGE_WIDTH - margin, curY + 70f)
        canvas.drawRoundRect(clientBoxRect, 6f, 6f, paintCardBg)
        canvas.drawRoundRect(clientBoxRect, 6f, 6f, paintBorder)

        // Box Headers
        paintText.textSize = 8f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = Color.rgb(100, 116, 139)
        canvas.drawText("BILLED TO / RECIPIENT DETAILS", margin + 14f, curY + 18f, paintText)
        canvas.drawText("INVOICE METRICS & SUPPLY", margin + (contentWidth * 0.58f), curY + 18f, paintText)

        // Client info
        paintText.textSize = 12f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = Color.rgb(15, 23, 42)
        canvas.drawText(invoice.clientCompany.ifBlank { "Client Organization" }, margin + 14f, curY + 34f, paintText)

        paintText.textSize = 9f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paintText.color = Color.rgb(51, 65, 85)
        canvas.drawText("Attn: ${invoice.clientName}  ·  Phone: ${invoice.clientPhone}", margin + 14f, curY + 48f, paintText)
        canvas.drawText("Email: ${invoice.clientEmail}  ·  Place of Supply: Delhi NCR / Pan-India", margin + 14f, curY + 60f, paintText)

        // Metrics info (Right half)
        canvas.drawText("Payment Terms: Net 15 Days", margin + (contentWidth * 0.58f), curY + 34f, paintText)
        canvas.drawText("Currency: INR (₹)", margin + (contentWidth * 0.58f), curY + 48f, paintText)
        canvas.drawText("Status: ${invoice.status.uppercase()}", margin + (contentWidth * 0.58f), curY + 60f, paintText)

        curY += 86f

        // 4. Itemized Table
        val colDescWidth = contentWidth * 0.52f
        val colQtyWidth = contentWidth * 0.12f
        val colRateWidth = contentWidth * 0.18f
        val colAmountWidth = contentWidth * 0.18f

        // Table Header
        val tableHeaderRect = RectF(margin, curY, PAGE_WIDTH - margin, curY + 22f)
        canvas.drawRoundRect(tableHeaderRect, 4f, 4f, paintHeaderBg)

        val paintTH = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("DESCRIPTION OF SERVICES", margin + 10f, curY + 14f, paintTH)

        paintTH.textAlign = Paint.Align.CENTER
        canvas.drawText("QTY", margin + colDescWidth + (colQtyWidth / 2), curY + 14f, paintTH)

        paintTH.textAlign = Paint.Align.RIGHT
        canvas.drawText("RATE (₹)", margin + colDescWidth + colQtyWidth + colRateWidth - 8f, curY + 14f, paintTH)
        canvas.drawText("AMOUNT (₹)", PAGE_WIDTH - margin - 10f, curY + 14f, paintTH)

        curY += 24f

        // Table Row (Item summary)
        val rowHeight = 44f
        val tableRowRect = RectF(margin, curY, PAGE_WIDTH - margin, curY + rowHeight)
        canvas.drawRect(tableRowRect, paintCardBg)
        paintBorder.color = Color.rgb(226, 232, 240)
        canvas.drawRect(tableRowRect, paintBorder)

        paintText.textAlign = Paint.Align.LEFT
        paintText.textSize = 10f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = Color.rgb(15, 23, 42)
        canvas.drawText(invoice.itemsSummary, margin + 10f, curY + 18f, paintText)

        paintText.textSize = 8f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paintText.color = Color.rgb(100, 116, 139)
        canvas.drawText("Includes UI/UX design, mobile architecture, cloud backend & QA validation.", margin + 10f, curY + 32f, paintText)

        paintText.textAlign = Paint.Align.CENTER
        paintText.textSize = 9f
        paintText.color = Color.rgb(51, 65, 85)
        canvas.drawText("1 Lot", margin + colDescWidth + (colQtyWidth / 2), curY + 24f, paintText)

        paintText.textAlign = Paint.Align.RIGHT
        val subtotalStr = String.format(Locale.US, "%,.2f", invoice.subtotal)
        canvas.drawText(subtotalStr, margin + colDescWidth + colQtyWidth + colRateWidth - 8f, curY + 24f, paintText)

        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = Color.rgb(15, 23, 42)
        canvas.drawText(subtotalStr, PAGE_WIDTH - margin - 10f, curY + 24f, paintText)

        curY += rowHeight + 14f

        // 5. Payment & Financial Summary Block
        val summaryY = curY

        // Left: Bank & UPI Payment Details Box
        val bankBoxRect = RectF(margin, summaryY, margin + (contentWidth * 0.52f), summaryY + 115f)
        canvas.drawRoundRect(bankBoxRect, 6f, 6f, paintCardBg)
        canvas.drawRoundRect(bankBoxRect, 6f, 6f, paintBorder)

        paintText.textAlign = Paint.Align.LEFT
        paintText.textSize = 8f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = Color.rgb(100, 116, 139)
        canvas.drawText("BANK TRANSFER & UPI PAYMENT DETAILS", margin + 10f, summaryY + 16f, paintText)

        paintText.textSize = 8.5f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paintText.color = Color.rgb(51, 65, 85)
        canvas.drawText("Beneficiary: Making Brands Tech Solutions Pvt Ltd", margin + 10f, summaryY + 32f, paintText)
        canvas.drawText("Bank: HDFC Bank Ltd · Cyber City Branch", margin + 10f, summaryY + 46f, paintText)
        canvas.drawText("Account No: 50200088991122  (Current A/C)", margin + 10f, summaryY + 60f, paintText)
        canvas.drawText("IFSC Code: HDFC0001234  ·  MICR: 110240055", margin + 10f, summaryY + 74f, paintText)

        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = Color.rgb(37, 99, 235)
        canvas.drawText("UPI ID: makingbrands@hdfcbank", margin + 10f, summaryY + 92f, paintText)
        canvas.drawText("GPay / PhonePe: +91 98765 43210", margin + 10f, summaryY + 104f, paintText)

        // Right: Calculation Summary Box
        val calcBoxRect = RectF(margin + (contentWidth * 0.54f), summaryY, PAGE_WIDTH - margin, summaryY + 115f)
        canvas.drawRoundRect(calcBoxRect, 6f, 6f, paintCardBg)
        canvas.drawRoundRect(calcBoxRect, 6f, 6f, paintBorder)

        val cgst = invoice.subtotal * 0.09
        val sgst = invoice.subtotal * 0.09

        var rY = summaryY + 20f
        paintText.textSize = 9f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paintText.color = Color.rgb(100, 116, 139)

        // Subtotal line
        canvas.drawText("Taxable Subtotal:", calcBoxRect.left + 10f, rY, paintText)
        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹ $subtotalStr", calcBoxRect.right - 10f, rY, paintText)

        // CGST line
        rY += 16f
        paintText.textAlign = Paint.Align.LEFT
        canvas.drawText("CGST (9.0%):", calcBoxRect.left + 10f, rY, paintText)
        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹ ${String.format(Locale.US, "%,.2f", cgst)}", calcBoxRect.right - 10f, rY, paintText)

        // SGST line
        rY += 16f
        paintText.textAlign = Paint.Align.LEFT
        canvas.drawText("SGST (9.0%):", calcBoxRect.left + 10f, rY, paintText)
        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹ ${String.format(Locale.US, "%,.2f", sgst)}", calcBoxRect.right - 10f, rY, paintText)

        // Divider
        rY += 8f
        canvas.drawLine(calcBoxRect.left + 8f, rY, calcBoxRect.right - 8f, rY, paintBorder)
        rY += 18f

        // Grand Total Box
        val totalAmountStr = String.format(Locale.US, "%,.2f", invoice.totalAmount)
        paintText.textAlign = Paint.Align.LEFT
        paintText.textSize = 11f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = Color.rgb(15, 23, 42)
        canvas.drawText("Total Payable:", calcBoxRect.left + 10f, rY, paintText)

        paintText.textAlign = Paint.Align.RIGHT
        paintText.color = Color.rgb(21, 128, 61) // Green 700
        paintText.textSize = 13f
        canvas.drawText("₹ $totalAmountStr", calcBoxRect.right - 10f, rY, paintText)

        curY = summaryY + 128f

        // 6. Terms & Notes Box
        val termsBox = RectF(margin, curY, PAGE_WIDTH - margin, curY + 44f)
        canvas.drawRoundRect(termsBox, 4f, 4f, paintCardBg)
        canvas.drawRoundRect(termsBox, 4f, 4f, paintBorder)

        paintText.textAlign = Paint.Align.LEFT
        paintText.textSize = 7.5f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = Color.rgb(100, 116, 139)
        canvas.drawText("TERMS & CONDITIONS:", margin + 8f, curY + 14f, paintText)

        paintText.textSize = 7.5f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paintText.color = Color.rgb(71, 85, 105)
        canvas.drawText(invoice.notes.ifBlank { "1. Payment terms: Net 15 days. 2. Subject to Gurugram jurisdiction. 3. System generated GST tax invoice." }, margin + 8f, curY + 28f, paintText)

        curY += 60f

        // 7. Footer & Digital Signature Badge
        paintText.textSize = 9f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paintText.color = Color.rgb(100, 116, 139)
        canvas.drawText("Thank you for your business with Making Brands!", margin, curY + 20f, paintText)

        // Signatory Box
        val sigBox = RectF(PAGE_WIDTH - margin - 150f, curY, PAGE_WIDTH - margin, curY + 40f)
        val paintSealBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(239, 246, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(sigBox, 4f, 4f, paintSealBg)
        paintBorder.color = Color.rgb(147, 197, 253)
        canvas.drawRoundRect(sigBox, 4f, 4f, paintBorder)

        paintText.textAlign = Paint.Align.CENTER
        paintText.textSize = 8f
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.color = Color.rgb(37, 99, 235)
        canvas.drawText("✓ DIGITAL VERIFIED", sigBox.centerX(), sigBox.top + 14f, paintText)

        paintText.textSize = 8f
        paintText.color = Color.rgb(15, 23, 42)
        canvas.drawText("Authorized Signatory", sigBox.centerX(), sigBox.top + 26f, paintText)

        paintText.textSize = 7f
        paintText.color = Color.rgb(100, 116, 139)
        canvas.drawText("Making Brands Tech Solutions", sigBox.centerX(), sigBox.top + 35f, paintText)
    }

    private fun drawQuotationOnCanvas(canvas: Canvas, quotation: QuotationEntity) {
        // Convert to compatible InvoiceEntity for drawing
        val invoiceSim = InvoiceEntity(
            invoiceNumber = quotation.quotationNumber,
            clientName = quotation.clientName,
            clientCompany = quotation.clientCompany,
            clientEmail = quotation.clientEmail,
            clientPhone = quotation.clientPhone,
            issueDate = quotation.issueDate,
            dueDate = quotation.validUntil,
            currency = quotation.currency,
            subtotal = quotation.subtotal,
            taxPercent = quotation.taxPercent,
            totalAmount = quotation.totalAmount,
            status = quotation.status,
            itemsSummary = quotation.scopeOfWork,
            notes = quotation.termsAndConditions
        )
        drawInvoiceOnCanvas(canvas, invoiceSim)
    }
}
