package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.InvoiceEntity
import com.example.data.model.QuotationEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.StandardScreenHeader
import com.example.ui.theme.*
import com.example.util.InvoicePdfGenerator
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceQuotationScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToDocumentScanner: () -> Unit = {}
) {
    val invoices by viewModel.invoices.collectAsState(initial = emptyList())
    val quotations by viewModel.quotations.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf("invoices") } // "invoices" or "quotations"
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }

    // Dialog & Preview States
    var showCreateInvoiceDialog by remember { mutableStateOf(false) }
    var showCreateQuotationDialog by remember { mutableStateOf(false) }
    var previewInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    var previewQuotation by remember { mutableStateOf<QuotationEntity?>(null) }
    var actionSnackbarMsg by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionSnackbarMsg) {
        actionSnackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            actionSnackbarMsg = null
        }
    }

    // Filter Invoices
    val filteredInvoices = invoices.filter { inv ->
        val matchesSearch = inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                inv.clientCompany.contains(searchQuery, ignoreCase = true) ||
                inv.clientName.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (statusFilter) {
            "All" -> true
            "Pending" -> inv.status.equals("Pending", ignoreCase = true)
            "Paid" -> inv.status.equals("Paid", ignoreCase = true)
            "Overdue" -> inv.status.equals("Overdue", ignoreCase = true)
            else -> true
        }
        matchesSearch && matchesFilter
    }

    // Filter Quotations
    val filteredQuotations = quotations.filter { qt ->
        val matchesSearch = qt.quotationNumber.contains(searchQuery, ignoreCase = true) ||
                qt.clientCompany.contains(searchQuery, ignoreCase = true) ||
                qt.clientName.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (statusFilter) {
            "All" -> true
            "Sent" -> qt.status.equals("Sent", ignoreCase = true)
            "Accepted" -> qt.status.equals("Accepted", ignoreCase = true)
            "Converted" -> qt.status.equals("Converted to Invoice", ignoreCase = true)
            else -> true
        }
        matchesSearch && matchesFilter
    }

    // Financial calculations
    val totalInvoiceBilled = invoices.sumOf { it.totalAmount }
    val totalInvoicePaid = invoices.filter { it.status.equals("Paid", ignoreCase = true) }.sumOf { it.totalAmount }
    val totalInvoicePending = invoices.filter { it.status.equals("Pending", ignoreCase = true) }.sumOf { it.totalAmount }
    val totalQuotationPipeline = quotations.filter { !it.status.equals("Declined", ignoreCase = true) }.sumOf { it.totalAmount }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Invoices & Quotations",
                subMenuSubtitle = "GST Invoicing & Estimates",
                onBack = onBack,
                onNavigateToChat = onNavigateToChat
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == "invoices") {
                        showCreateInvoiceDialog = true
                    } else {
                        showCreateQuotationDialog = true
                    }
                },
                containerColor = BrandDarkBlue,
                contentColor = Color.White
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Create New", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (selectedTab == "invoices") "New Invoice" else "New Quotation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Tab Switcher: Invoices vs Quotations
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TabSelectionButton(
                            title = "🧾 Invoices (${invoices.size})",
                            isSelected = selectedTab == "invoices",
                            onClick = {
                                selectedTab = "invoices"
                                statusFilter = "All"
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TabSelectionButton(
                            title = "📋 Quotations (${quotations.size})",
                            isSelected = selectedTab == "quotations",
                            onClick = {
                                selectedTab = "quotations"
                                statusFilter = "All"
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 📷 Milo AI Document & Receipt Scanner Action Banner
            item {
                Card(
                    onClick = onNavigateToDocumentScanner,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E293B)
                        ) {
                            Box(
                                modifier = Modifier.padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = "Document Scanner",
                                    tint = Color(0xFFEAB308),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Scan Paper Invoice / Receipt",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF15803D),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "VISION OCR",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Camera capture • Auto-extract totals & GST • Direct Firestore sync",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }

            // Summary Financial Metrics Banner
            item {
                if (selectedTab == "invoices") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinancialMetricPill(
                            title = "Total Billed",
                            amount = "₹${String.format(Locale.US, "%,.0f", totalInvoiceBilled)}",
                            subtext = "${invoices.size} Invoices",
                            color = Color(0xFF2563EB),
                            bgColor = Color(0xFFEFF6FF),
                            modifier = Modifier.weight(1f)
                        )
                        FinancialMetricPill(
                            title = "Paid Received",
                            amount = "₹${String.format(Locale.US, "%,.0f", totalInvoicePaid)}",
                            subtext = "Cleared",
                            color = Color(0xFF16A34A),
                            bgColor = Color(0xFFF0FDF4),
                            modifier = Modifier.weight(1f)
                        )
                        FinancialMetricPill(
                            title = "Pending Due",
                            amount = "₹${String.format(Locale.US, "%,.0f", totalInvoicePending)}",
                            subtext = "Awaiting payment",
                            color = Color(0xFFD97706),
                            bgColor = Color(0xFFFFFBEB),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinancialMetricPill(
                            title = "Active Pipeline",
                            amount = "₹${String.format(Locale.US, "%,.0f", totalQuotationPipeline)}",
                            subtext = "${quotations.size} Estimates",
                            color = Color(0xFF7C3AED),
                            bgColor = Color(0xFFFAF5FF),
                            modifier = Modifier.weight(1f)
                        )
                        FinancialMetricPill(
                            title = "Converted",
                            amount = "${quotations.count { it.status.contains("Converted", ignoreCase = true) }} Quotes",
                            subtext = "To Invoices",
                            color = Color(0xFF16A34A),
                            bgColor = Color(0xFFF0FDF4),
                            modifier = Modifier.weight(1f)
                        )
                        FinancialMetricPill(
                            title = "Success Rate",
                            amount = if (quotations.isNotEmpty()) "${(quotations.count { it.status != "Declined" } * 100) / quotations.size}%" else "100%",
                            subtext = "Client Acceptance",
                            color = Color(0xFF0284C7),
                            bgColor = Color(0xFFF0F9FF),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Search Bar & Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by ${if (selectedTab == "invoices") "Invoice #" else "Quotation #"}, Client or Company...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = appTextFieldColors()
                    )

                    // Status Filters
                    val currentFilters = if (selectedTab == "invoices") {
                        listOf("All", "Pending", "Paid", "Overdue")
                    } else {
                        listOf("All", "Sent", "Accepted", "Converted")
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(currentFilters) { f ->
                            val isSel = statusFilter == f
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) BrandDarkBlue else Color.White,
                                border = BorderStroke(1.dp, if (isSel) BrandDarkBlue else BorderLight),
                                modifier = Modifier.clickable { statusFilter = f }
                            ) {
                                Text(
                                    text = f,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section Header with Total Count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == "invoices") "Invoices (${filteredInvoices.size})" else "Quotations & Estimates (${filteredQuotations.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )

                    Text(
                        text = "Tap 'Preview' to view PDF before sending",
                        fontSize = 11.sp,
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // INVOICES LIST
            if (selectedTab == "invoices") {
                if (filteredInvoices.isEmpty()) {
                    item {
                        EmptyStatePlaceholder(
                            title = "No Invoices Found",
                            subtitle = "Create your first professional invoice with 1-click PDF preview.",
                            buttonText = "+ Create New Invoice",
                            onClick = { showCreateInvoiceDialog = true }
                        )
                    }
                } else {
                    items(filteredInvoices) { inv ->
                        InvoiceItemCard(
                            invoice = inv,
                            onPreviewPdf = { previewInvoice = inv },
                            onSendWhatsApp = {
                                viewModel.sendChatMessage("🧾 Invoice ${inv.invoiceNumber} (₹${String.format(Locale.US, "%,.0f", inv.totalAmount)}) attached for ${inv.clientCompany}", "${inv.invoiceNumber}.pdf", "840 KB")
                                actionSnackbarMsg = "Invoice ${inv.invoiceNumber} shared via WhatsApp!"
                            },
                            onUpdateStatus = { newStatus ->
                                viewModel.updateInvoiceStatus(inv, newStatus)
                                actionSnackbarMsg = "${inv.invoiceNumber} marked as $newStatus"
                            },
                            onDelete = {
                                viewModel.deleteInvoice(inv.id)
                                actionSnackbarMsg = "Invoice ${inv.invoiceNumber} deleted."
                            }
                        )
                    }
                }
            }

            // QUOTATIONS LIST
            else {
                if (filteredQuotations.isEmpty()) {
                    item {
                        EmptyStatePlaceholder(
                            title = "No Quotations Found",
                            subtitle = "Generate customized client proposals and quotations with live PDF preview.",
                            buttonText = "+ Create New Quotation",
                            onClick = { showCreateQuotationDialog = true }
                        )
                    }
                } else {
                    items(filteredQuotations) { qt ->
                        QuotationItemCard(
                            quotation = qt,
                            onPreviewPdf = { previewQuotation = qt },
                            onSendWhatsApp = {
                                viewModel.sendChatMessage("📋 Quotation ${qt.quotationNumber} (₹${String.format(Locale.US, "%,.0f", qt.totalAmount)}) sent to ${qt.clientCompany}", "${qt.quotationNumber}.pdf", "620 KB")
                                actionSnackbarMsg = "Quotation ${qt.quotationNumber} sent via WhatsApp!"
                            },
                            onConvertToInvoice = {
                                viewModel.convertQuotationToInvoice(qt)
                                actionSnackbarMsg = "${qt.quotationNumber} converted to new Tax Invoice!"
                            },
                            onUpdateStatus = { newStatus ->
                                viewModel.updateQuotationStatus(qt, newStatus)
                                actionSnackbarMsg = "${qt.quotationNumber} status updated to $newStatus"
                            },
                            onDelete = {
                                viewModel.deleteQuotation(qt.id)
                                actionSnackbarMsg = "Quotation ${qt.quotationNumber} deleted."
                            }
                        )
                    }
                }
            }

            // Bottom space for FAB
            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // 📄 RENDERED PDF PREVIEW DIALOG FOR INVOICE
    previewInvoice?.let { inv ->
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        RenderedPdfPreviewDialog(
            docTitle = "TAX INVOICE",
            docNumber = inv.invoiceNumber,
            date = inv.issueDate,
            dueDate = inv.dueDate,
            clientName = inv.clientName,
            clientCompany = inv.clientCompany,
            clientEmail = inv.clientEmail,
            clientPhone = inv.clientPhone,
            itemsSummary = inv.itemsSummary,
            subtotal = inv.subtotal,
            discountPercent = 0.0,
            taxPercent = inv.taxPercent,
            totalAmount = inv.totalAmount,
            currency = inv.currency,
            notes = inv.notes,
            status = inv.status,
            onDismiss = { previewInvoice = null },
            onSendClient = {
                coroutineScope.launch(Dispatchers.IO) {
                    val pdfFile = InvoicePdfGenerator.generateInvoicePdf(context, inv)
                    withContext(Dispatchers.Main) {
                        InvoicePdfGenerator.sharePdfToWhatsApp(
                            context = context,
                            pdfFile = pdfFile,
                            recipientPhone = inv.clientPhone,
                            clientName = inv.clientName,
                            docNumber = inv.invoiceNumber,
                            totalAmount = inv.totalAmount
                        )
                    }
                }
                viewModel.sendChatMessage("🧾 Official Invoice ${inv.invoiceNumber} attached (₹${String.format(Locale.US, "%,.0f", inv.totalAmount)})", "${inv.invoiceNumber}.pdf", "840 KB")
                actionSnackbarMsg = "Invoice PDF successfully dispatched to ${inv.clientCompany}!"
                previewInvoice = null
            }
        )
    }

    // 📄 RENDERED PDF PREVIEW DIALOG FOR QUOTATION
    previewQuotation?.let { qt ->
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        RenderedPdfPreviewDialog(
            docTitle = "COMMERCIAL QUOTATION",
            docNumber = qt.quotationNumber,
            date = qt.issueDate,
            dueDate = qt.validUntil,
            clientName = qt.clientName,
            clientCompany = qt.clientCompany,
            clientEmail = qt.clientEmail,
            clientPhone = qt.clientPhone,
            itemsSummary = qt.scopeOfWork,
            subtotal = qt.subtotal,
            discountPercent = qt.discountPercent,
            taxPercent = qt.taxPercent,
            totalAmount = qt.totalAmount,
            currency = qt.currency,
            notes = qt.termsAndConditions,
            status = qt.status,
            isQuotation = true,
            onDismiss = { previewQuotation = null },
            onSendClient = {
                coroutineScope.launch(Dispatchers.IO) {
                    val pdfFile = InvoicePdfGenerator.generateQuotationPdf(context, qt)
                    withContext(Dispatchers.Main) {
                        InvoicePdfGenerator.sharePdfToWhatsApp(
                            context = context,
                            pdfFile = pdfFile,
                            recipientPhone = qt.clientPhone,
                            clientName = qt.clientName,
                            docNumber = qt.quotationNumber,
                            totalAmount = qt.totalAmount
                        )
                    }
                }
                viewModel.sendChatMessage("📋 Commercial Quotation ${qt.quotationNumber} attached (₹${String.format(Locale.US, "%,.0f", qt.totalAmount)})", "${qt.quotationNumber}.pdf", "620 KB")
                actionSnackbarMsg = "Quotation PDF successfully dispatched to ${qt.clientCompany}!"
                previewQuotation = null
            }
        )
    }

    // ➕ CREATE INVOICE DIALOG WITH LIVE PREVIEW ACTION
    if (showCreateInvoiceDialog) {
        CreateInvoiceDialog(
            onDismiss = { showCreateInvoiceDialog = false },
            onCreate = { cName, cComp, cEmail, cPhone, dueDate, subtotal, taxPct, summary, notes ->
                viewModel.addInvoice(
                    clientName = cName,
                    clientCompany = cComp,
                    clientEmail = cEmail,
                    clientPhone = cPhone,
                    dueDate = dueDate,
                    subtotal = subtotal,
                    taxPercent = taxPct,
                    itemsSummary = summary,
                    notes = notes
                )
                actionSnackbarMsg = "New Invoice generated for $cComp"
                showCreateInvoiceDialog = false
            },
            onPreviewBeforeCreate = { cName, cComp, cEmail, cPhone, dueDate, subtotal, taxPct, summary, notes ->
                val total = subtotal + (subtotal * taxPct / 100.0)
                previewInvoice = InvoiceEntity(
                    invoiceNumber = "INV-2026-PREVIEW",
                    clientName = cName.ifBlank { "Client Name" },
                    clientCompany = cComp.ifBlank { "Client Company Pvt Ltd" },
                    clientEmail = cEmail.ifBlank { "client@example.com" },
                    clientPhone = cPhone.ifBlank { "+91 98765 43210" },
                    issueDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
                    dueDate = dueDate.ifBlank { "30 Oct 2026" },
                    currency = "₹",
                    subtotal = subtotal,
                    taxPercent = taxPct,
                    totalAmount = total,
                    status = "Draft Preview",
                    itemsSummary = summary.ifBlank { "Website & Android App Development Suite" },
                    notes = notes
                )
            }
        )
    }

    // ➕ CREATE QUOTATION DIALOG WITH LIVE PREVIEW ACTION
    if (showCreateQuotationDialog) {
        CreateQuotationDialog(
            onDismiss = { showCreateQuotationDialog = false },
            onCreate = { cName, cComp, cEmail, cPhone, validUntil, subtotal, discountPct, taxPct, scope, terms ->
                viewModel.addQuotation(
                    clientName = cName,
                    clientCompany = cComp,
                    clientEmail = cEmail,
                    clientPhone = cPhone,
                    validUntil = validUntil,
                    subtotal = subtotal,
                    discountPercent = discountPct,
                    taxPercent = taxPct,
                    scopeOfWork = scope,
                    termsAndConditions = terms
                )
                actionSnackbarMsg = "New Quotation created for $cComp"
                showCreateQuotationDialog = false
            },
            onPreviewBeforeCreate = { cName, cComp, cEmail, cPhone, validUntil, subtotal, discountPct, taxPct, scope, terms ->
                val afterDisc = subtotal - (subtotal * discountPct / 100.0)
                val total = afterDisc + (afterDisc * taxPct / 100.0)
                previewQuotation = QuotationEntity(
                    quotationNumber = "QT-2026-PREVIEW",
                    clientName = cName.ifBlank { "Client Name" },
                    clientCompany = cComp.ifBlank { "Client Company Pvt Ltd" },
                    clientEmail = cEmail.ifBlank { "client@example.com" },
                    clientPhone = cPhone.ifBlank { "+91 98765 43210" },
                    issueDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
                    validUntil = validUntil.ifBlank { "30 Days from Issue" },
                    currency = "₹",
                    subtotal = subtotal,
                    discountPercent = discountPct,
                    taxPercent = taxPct,
                    totalAmount = total,
                    status = "Draft Preview",
                    scopeOfWork = scope.ifBlank { "Custom Enterprise Application Development & Cloud Infrastructure" },
                    termsAndConditions = terms
                )
            }
        )
    }
}

// -------------------------------------------------------------
// INVOICE ITEM CARD
// -------------------------------------------------------------
@Composable
fun InvoiceItemCard(
    invoice: InvoiceEntity,
    onPreviewPdf: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val statusBg = when (invoice.status.lowercase()) {
        "paid" -> Color(0xFFDCFCE7)
        "pending" -> Color(0xFFFEF3C7)
        "overdue" -> Color(0xFFFEE2E2)
        else -> Color(0xFFF1F5F9)
    }

    val statusColor = when (invoice.status.lowercase()) {
        "paid" -> Color(0xFF15803D)
        "pending" -> Color(0xFFB45309)
        "overdue" -> Color(0xFFB91C1C)
        else -> Color(0xFF475569)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Invoice #, Date & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = invoice.invoiceNumber,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = BrandDarkBlue
                        )
                        Text(
                            text = "Issued: ${invoice.issueDate} · Due: ${invoice.dueDate}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = invoice.status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mark as Paid") },
                                onClick = {
                                    onUpdateStatus("Paid")
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Mark as Pending") },
                                onClick = {
                                    onUpdateStatus("Pending")
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFD97706)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Mark as Overdue") },
                                onClick = {
                                    onUpdateStatus("Overdue")
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626)) }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Delete Invoice", color = Color(0xFFDC2626)) },
                                onClick = {
                                    onDelete()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626)) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Client & Company Info
            Text(
                text = invoice.clientCompany,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Text(
                text = "${invoice.clientName} · ${invoice.clientPhone} · ${invoice.clientEmail}",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Items: ${invoice.itemsSummary}",
                fontSize = 12.sp,
                color = Color(0xFF475569),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = BorderLight)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Amount and Action Buttons (Preview PDF & WhatsApp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Amount (incl. 18% GST)", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = "${invoice.currency} ${String.format(Locale.US, "%,.0f", invoice.totalAmount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = BrandDarkBlue
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 🌟 PREVIEW BUTTON (User requested)
                    OutlinedButton(
                        onClick = onPreviewPdf,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BrandBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "Preview PDF", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // SEND VIA WHATSAPP BUTTON
                    Button(
                        onClick = onSendWhatsApp,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// QUOTATION ITEM CARD
// -------------------------------------------------------------
@Composable
fun QuotationItemCard(
    quotation: QuotationEntity,
    onPreviewPdf: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onConvertToInvoice: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val statusBg = when (quotation.status.lowercase()) {
        "accepted" -> Color(0xFFDCFCE7)
        "sent" -> Color(0xFFEFF6FF)
        "converted to invoice" -> Color(0xFFFAF5FF)
        "declined" -> Color(0xFFFEE2E2)
        else -> Color(0xFFF1F5F9)
    }

    val statusColor = when (quotation.status.lowercase()) {
        "accepted" -> Color(0xFF15803D)
        "sent" -> Color(0xFF1D4ED8)
        "converted to invoice" -> Color(0xFF7E22CE)
        "declined" -> Color(0xFFB91C1C)
        else -> Color(0xFF475569)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFAF5FF),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF9333EA),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = quotation.quotationNumber,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = BrandDarkBlue
                        )
                        Text(
                            text = "Issued: ${quotation.issueDate} · Valid: ${quotation.validUntil}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = quotation.status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mark as Accepted") },
                                onClick = {
                                    onUpdateStatus("Accepted")
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color(0xFF16A34A)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Convert to Tax Invoice") },
                                onClick = {
                                    onConvertToInvoice()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF9333EA)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Mark as Declined") },
                                onClick = {
                                    onUpdateStatus("Declined")
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFDC2626)) }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Delete Quotation", color = Color(0xFFDC2626)) },
                                onClick = {
                                    onDelete()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626)) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = quotation.clientCompany,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Text(
                text = "${quotation.clientName} · ${quotation.clientPhone} · ${quotation.clientEmail}",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Scope: ${quotation.scopeOfWork}",
                fontSize = 12.sp,
                color = Color(0xFF475569),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = BorderLight)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Quote Amount", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = "${quotation.currency} ${String.format(Locale.US, "%,.0f", quotation.totalAmount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = Color(0xFF7E22CE)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 🌟 PREVIEW BUTTON (User requested)
                    OutlinedButton(
                        onClick = onPreviewPdf,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF9333EA)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9333EA)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "Preview PDF", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // SEND VIA WHATSAPP BUTTON
                    Button(
                        onClick = onSendWhatsApp,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 🌟 RENDERED PDF PREVIEW DIALOG (HIGH FIDELITY A4 DOCUMENT SIMULATION)
// -------------------------------------------------------------
@Composable
fun RenderedPdfPreviewDialog(
    docTitle: String,
    docNumber: String,
    date: String,
    dueDate: String,
    clientName: String,
    clientCompany: String,
    clientEmail: String,
    clientPhone: String,
    itemsSummary: String,
    subtotal: Double,
    discountPercent: Double = 0.0,
    taxPercent: Double = 18.0,
    totalAmount: Double,
    currency: String = "₹",
    notes: String = "",
    status: String = "Draft",
    isQuotation: Boolean = false,
    onDismiss: () -> Unit,
    onSendClient: () -> Unit
) {
    var isSending by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val configuration = LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 600
        val screenHeight = configuration.screenHeightDp.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(if (isTablet) 0.88f else 0.96f)
                    .heightIn(max = screenHeight * 0.94f)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                tonalElevation = 12.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                // PDF Viewer Top Bar
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEF4444)
                            ) {
                                Text(
                                    "PDF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "$docNumber.pdf",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Rendered A4 Document Preview · 1 Page",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }
                }

                // Scrollable Rendered A4 PDF Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF334155))
                        .padding(12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // A4 Sheet Container
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // 1. Corporate Header & Logo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF0F172A), // bg-slate-900
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            shadowElevation = 3.dp,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize().padding(3.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.milo_final),
                                                    contentDescription = "Milo Logo",
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("MAKING BRANDS", fontWeight = FontWeight.Black, fontSize = 16.sp, color = BrandDarkBlue)
                                            Text("Digital Solutions & Technology", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Making Brands Tech Solutions Pvt Ltd", fontSize = 10.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
                                    Text("Cyber City, Phase II, Gurugram, India - 122002", fontSize = 10.sp, color = TextSecondary)
                                    Text("Email: billing@makingbrands.in · +91 98765 43210", fontSize = 10.sp, color = TextSecondary)
                                    Text("GSTIN: 07AAAAA0000A1Z5 · PAN: AAAAA0000A", fontSize = 10.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isQuotation) Color(0xFFFAF5FF) else Color(0xFFEFF6FF),
                                        border = BorderStroke(1.dp, if (isQuotation) Color(0xFFC084FC) else Color(0xFF93C5FD))
                                    ) {
                                        Text(
                                            text = docTitle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isQuotation) Color(0xFF7E22CE) else BrandBlue,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(docNumber, fontWeight = FontWeight.Black, fontSize = 15.sp, color = BrandDarkBlue)
                                    Text("Date: $date", fontSize = 10.sp, color = TextSecondary)
                                    Text(if (isQuotation) "Valid Until: $dueDate" else "Due Date: $dueDate", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (status.equals("Paid", ignoreCase = true) || status.equals("Accepted", ignoreCase = true)) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            "STATUS: ${status.uppercase()}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (status.equals("Paid", ignoreCase = true) || status.equals("Accepted", ignoreCase = true)) Color(0xFF15803D) else Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(thickness = 2.dp, color = BrandDarkBlue)
                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. Bill To / Client Details Box
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("BILLED TO / CLIENT DETAILS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(clientCompany, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Text("Attn: $clientName", fontSize = 11.sp, color = Color(0xFF334155))
                                    Text("Contact: $clientPhone · $clientEmail", fontSize = 11.sp, color = TextSecondary)
                                    Text("Place of Supply: Delhi NCR / Pan-India", fontSize = 10.sp, color = TextSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 3. Tabular Line Items
                            Text("ITEMIZED DELIVERABLES & SERVICES", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(6.dp))

                            // Table Header
                            Surface(
                                color = BrandDarkBlue,
                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("DESCRIPTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(2.5f))
                                    Text("QTY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f))
                                    Text("RATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                                    Text("AMOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
                                }
                            }

                            // Line Item 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0xFFE2E8F0))
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2.5f)) {
                                    Text(itemsSummary, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Includes UI/UX, API integration, QA and deployment warranty.", fontSize = 9.sp, color = TextSecondary)
                                }
                                Text("1 Lot", fontSize = 10.sp, color = Color(0xFF334155), textAlign = TextAlign.Center, modifier = Modifier.weight(0.7f))
                                Text("$currency ${String.format(Locale.US, "%,.0f", subtotal)}", fontSize = 10.sp, color = Color(0xFF334155), textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                                Text("$currency ${String.format(Locale.US, "%,.0f", subtotal)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4. Financial Calculations Box
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left Column: Payment & Bank Transfer Details
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("BANK PAYMENT / UPI DETAILS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF475569))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Bank: HDFC Bank Ltd", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            Text("A/C No: 50200084729104", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("IFSC: HDFC0001234", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                                            Text("UPI ID: makingbrands@hdfcbank", fontSize = 9.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Right Column: Subtotal, Taxes and Grand Total
                                Column(
                                    modifier = Modifier.weight(1.3f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FinancialSummaryRow("Subtotal:", "$currency ${String.format(Locale.US, "%,.0f", subtotal)}")
                                    if (discountPercent > 0.0) {
                                        val discAmt = subtotal * discountPercent / 100.0
                                        FinancialSummaryRow("Discount (${discountPercent.toInt()}%):", "-$currency ${String.format(Locale.US, "%,.0f", discAmt)}", highlightRed = true)
                                    }
                                    val taxAmt = (subtotal - (subtotal * discountPercent / 100.0)) * taxPercent / 100.0
                                    FinancialSummaryRow("GST (${taxPercent.toInt()}%):", "$currency ${String.format(Locale.US, "%,.0f", taxAmt)}")
                                    Divider(color = Color(0xFFCBD5E1))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("TOTAL AMOUNT:", fontSize = 11.sp, fontWeight = FontWeight.Black, color = BrandDarkBlue)
                                        Text("$currency ${String.format(Locale.US, "%,.0f", totalAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF15803D))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 5. Terms & Signature
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("TERMS & CONDITIONS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
                                    Text(notes.ifBlank { "1. Payment due within 15 days. 2. Subject to Gurugram jurisdiction. 3. System generated document." }, fontSize = 8.sp, color = TextSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Authorized Signatory
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    "Thank you for choosing Making Brands!",
                                    fontSize = 10.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = Color(0xFF64748B)
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFEFF6FF),
                                        border = BorderStroke(1.dp, Color(0xFF93C5FD))
                                    ) {
                                        Text(
                                            "✓ DIGITAL VERIFIED",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BrandBlue,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Authorized Signatory", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Making Brands Tech Solutions", fontSize = 8.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                // PDF Action Bar (WhatsApp Send, Email, Download & Print)
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Text("Close Preview", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                onSendClient()
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send PDF via WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
}

// -------------------------------------------------------------
// CREATE INVOICE DIALOG WITH LIVE PREVIEW SUPPORT
// -------------------------------------------------------------
@Composable
fun CreateInvoiceDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, Double, Double, String, String) -> Unit,
    onPreviewBeforeCreate: (String, String, String, String, String, Double, Double, String, String) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var clientCompany by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("15 Oct 2026") }
    var subtotalStr by remember { mutableStateOf("45000") }
    var taxPercentStr by remember { mutableStateOf("18.0") }
    var itemsSummary by remember { mutableStateOf("Website UI/UX Redesign & Android Application Development Suite") }
    var notes by remember { mutableStateOf("Payment terms: Net 15 days. Subject to Gurugram jurisdiction.") }

    Dialog(onDismissRequest = onDismiss) {
        val configuration = LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 600
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxWidth(if (isTablet) 0.8f else 1f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Tax Invoice", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Divider(color = BorderLight)

                OutlinedTextField(
                    value = clientCompany,
                    onValueChange = { clientCompany = it },
                    label = { Text("Client Company Name *") },
                    placeholder = { Text("e.g. Acme Innovations Pvt Ltd") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Contact Person") },
                        placeholder = { Text("e.g. Rahul Sharma") },
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+91 98765...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = clientEmail,
                    onValueChange = { clientEmail = it },
                    label = { Text("Client Email") },
                    placeholder = { Text("accounts@acme.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    colors = appTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = itemsSummary,
                    onValueChange = { itemsSummary = it },
                    label = { Text("Items / Scope of Work") },
                    maxLines = 3,
                    colors = appTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subtotalStr,
                        onValueChange = { subtotalStr = it },
                        label = { Text("Subtotal (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = taxPercentStr,
                        onValueChange = { taxPercentStr = it },
                        label = { Text("GST (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.8f)
                    )
                }

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Payment Due Date") },
                    placeholder = { Text("e.g. 30 Oct 2026") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Calculated Grand Total Preview Pill
                val subtotalVal = subtotalStr.toDoubleOrNull() ?: 0.0
                val taxPctVal = taxPercentStr.toDoubleOrNull() ?: 18.0
                val grandTotal = subtotalVal + (subtotalVal * taxPctVal / 100.0)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calculated Total (incl. GST):", fontSize = 12.sp, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                        Text("₹ ${String.format(Locale.US, "%,.0f", grandTotal)}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = BrandDarkBlue)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons: PREVIEW and CREATE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 🌟 Preview Button Before Saving (User requested)
                    OutlinedButton(
                        onClick = {
                            onPreviewBeforeCreate(
                                clientName, clientCompany, clientEmail, clientPhone,
                                dueDate, subtotalVal, taxPctVal, itemsSummary, notes
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BrandBlue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview PDF", fontWeight = FontWeight.Bold)
                    }

                    // Create & Generate Invoice
                    Button(
                        onClick = {
                            if (clientCompany.isNotBlank() && subtotalVal > 0) {
                                onCreate(
                                    clientName.ifBlank { "Client" },
                                    clientCompany.trim(),
                                    clientEmail.ifBlank { "client@example.com" },
                                    clientPhone.ifBlank { "+91 98765 43210" },
                                    dueDate.ifBlank { "30 Days Net" },
                                    subtotalVal,
                                    taxPctVal,
                                    itemsSummary,
                                    notes
                                )
                            }
                        },
                        enabled = clientCompany.isNotBlank() && subtotalVal > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandDarkBlue),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Save & Send", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// CREATE QUOTATION DIALOG WITH LIVE PREVIEW SUPPORT
// -------------------------------------------------------------
@Composable
fun CreateQuotationDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, Double, Double, Double, String, String) -> Unit,
    onPreviewBeforeCreate: (String, String, String, String, String, Double, Double, Double, String, String) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var clientCompany by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var validUntil by remember { mutableStateOf("30 Days from Issue") }
    var subtotalStr by remember { mutableStateOf("60000") }
    var discountPercentStr by remember { mutableStateOf("5.0") }
    var taxPercentStr by remember { mutableStateOf("18.0") }
    var scopeOfWork by remember { mutableStateOf("Custom Mobile & Web Application Suite with Cloud Database & Lead Automation APIs") }
    var terms by remember { mutableStateOf("50% Advance on project kickoff, 50% on final milestone UAT sign-off.") }

    Dialog(onDismissRequest = onDismiss) {
        val configuration = LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 600
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxWidth(if (isTablet) 0.8f else 1f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFAF5FF),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Quotation", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Divider(color = BorderLight)

                OutlinedTextField(
                    value = clientCompany,
                    onValueChange = { clientCompany = it },
                    label = { Text("Client Company Name *") },
                    placeholder = { Text("e.g. NextGen Retail Ltd") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Contact Person") },
                        placeholder = { Text("e.g. Ananya Roy") },
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+91 98765...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = clientEmail,
                    onValueChange = { clientEmail = it },
                    label = { Text("Client Email") },
                    placeholder = { Text("info@client.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    colors = appTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = scopeOfWork,
                    onValueChange = { scopeOfWork = it },
                    label = { Text("Scope of Work / Deliverables") },
                    maxLines = 3,
                    colors = appTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subtotalStr,
                        onValueChange = { subtotalStr = it },
                        label = { Text("Subtotal (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = discountPercentStr,
                        onValueChange = { discountPercentStr = it },
                        label = { Text("Discount %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.7f)
                    )
                    OutlinedTextField(
                        value = taxPercentStr,
                        onValueChange = { taxPercentStr = it },
                        label = { Text("GST %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.7f)
                    )
                }

                OutlinedTextField(
                    value = validUntil,
                    onValueChange = { validUntil = it },
                    label = { Text("Quotation Validity") },
                    placeholder = { Text("e.g. Valid for 30 Days") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Calculated Grand Total Preview Pill
                val subtotalVal = subtotalStr.toDoubleOrNull() ?: 0.0
                val discPctVal = discountPercentStr.toDoubleOrNull() ?: 0.0
                val taxPctVal = taxPercentStr.toDoubleOrNull() ?: 18.0
                val afterDisc = subtotalVal - (subtotalVal * discPctVal / 100.0)
                val grandTotal = afterDisc + (afterDisc * taxPctVal / 100.0)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFAF5FF),
                    border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Estimated Total Quote:", fontSize = 12.sp, color = Color(0xFF7E22CE), fontWeight = FontWeight.SemiBold)
                        Text("₹ ${String.format(Locale.US, "%,.0f", grandTotal)}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF6B21A8))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons: PREVIEW and CREATE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 🌟 Preview Button Before Saving (User requested)
                    OutlinedButton(
                        onClick = {
                            onPreviewBeforeCreate(
                                clientName, clientCompany, clientEmail, clientPhone,
                                validUntil, subtotalVal, discPctVal, taxPctVal, scopeOfWork, terms
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF9333EA)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9333EA)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview PDF", fontWeight = FontWeight.Bold)
                    }

                    // Create & Generate Quotation
                    Button(
                        onClick = {
                            if (clientCompany.isNotBlank() && subtotalVal > 0) {
                                onCreate(
                                    clientName.ifBlank { "Client" },
                                    clientCompany.trim(),
                                    clientEmail.ifBlank { "client@example.com" },
                                    clientPhone.ifBlank { "+91 98765 43210" },
                                    validUntil.ifBlank { "30 Days from Issue" },
                                    subtotalVal,
                                    discPctVal,
                                    taxPctVal,
                                    scopeOfWork,
                                    terms
                                )
                            }
                        },
                        enabled = clientCompany.isNotBlank() && subtotalVal > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE)),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Save & Send", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// HELPER COMPONENTS
// -------------------------------------------------------------
@Composable
fun TabSelectionButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) BrandDarkBlue else Color.Transparent,
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

@Composable
fun FinancialMetricPill(
    title: String,
    amount: String,
    subtext: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(amount, fontSize = 14.sp, fontWeight = FontWeight.Black, color = BrandDarkBlue)
            Text(subtext, fontSize = 9.sp, color = TextSecondary)
        }
    }
}

@Composable
fun FinancialSummaryRow(label: String, value: String, highlightRed: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 10.sp, color = TextSecondary)
        Text(
            value,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (highlightRed) Color(0xFFDC2626) else TextPrimary
        )
    }
}

@Composable
fun EmptyStatePlaceholder(
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFEFF6FF),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandDarkBlue)
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
