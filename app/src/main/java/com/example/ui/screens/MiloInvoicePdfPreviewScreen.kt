package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.InvoiceEntity
import com.example.data.model.LeadEntity
import com.example.ui.theme.*
import com.example.util.InvoicePdfGenerator
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🦁 Milo Auto-Generated Invoice PDF Preview & Dispatch Screen.
 * Empowers employees to seamlessly review, customize, preview, and share
 * professional GST Tax Invoices and Commercial Quotations generated via Milo AI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiloInvoicePdfPreviewScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    initialInvoice: InvoiceEntity? = null,
    onNavigateToInvoices: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val leads by viewModel.leads.collectAsState()
    val allInvoices by viewModel.invoices.collectAsState(initial = emptyList())
    val employeeName by viewModel.currentEmployeeName.collectAsState()

    // 1. Interactive Form Fields (Auto-filled via Milo initialInvoice or default)
    var invoiceNumber by remember {
        mutableStateOf(
            initialInvoice?.invoiceNumber ?: "INV-${SimpleDateFormat("yyyy", Locale.US).format(Date())}-${(1000..9999).random()}"
        )
    }
    var clientName by remember {
        mutableStateOf(
            initialInvoice?.clientName ?: leads.firstOrNull()?.name ?: "Rahul Verma"
        )
    }
    var clientCompany by remember {
        mutableStateOf(
            initialInvoice?.clientCompany ?: leads.firstOrNull()?.company ?: "Apex Global Enterprises"
        )
    }
    var clientPhone by remember {
        mutableStateOf(
            initialInvoice?.clientPhone ?: leads.firstOrNull()?.phone ?: "+91 98765 43210"
        )
    }
    var clientEmail by remember {
        mutableStateOf(
            initialInvoice?.clientEmail ?: leads.firstOrNull()?.email ?: "billing@apexenterprises.com"
        )
    }
    var itemsSummary by remember {
        mutableStateOf(
            initialInvoice?.itemsSummary ?: "Enterprise Mobile App Development & Cloud CRM Integration"
        )
    }
    var subtotalText by remember {
        mutableStateOf(
            initialInvoice?.subtotal?.let { String.format(Locale.US, "%.0f", it) } ?: "75000"
        )
    }
    var taxPercent by remember { mutableDoubleStateOf(initialInvoice?.taxPercent ?: 18.0) }
    var issueDate by remember {
        mutableStateOf(
            initialInvoice?.issueDate ?: SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        )
    }
    var dueDate by remember {
        mutableStateOf(
            initialInvoice?.dueDate ?: run {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 15)
                SimpleDateFormat("dd MMM yyyy", Locale.US).format(cal.time)
            }
        )
    }
    var notes by remember {
        mutableStateOf(
            initialInvoice?.notes ?: "Payment Terms: Net 15 days via NEFT / UPI. Thank you for partnering with Making Brands!"
        )
    }
    var invoiceStatus by remember { mutableStateOf(initialInvoice?.status ?: "Pending") }

    // Calculated amounts
    val parsedSubtotal = subtotalText.toDoubleOrNull() ?: 0.0
    val calculatedTax = (parsedSubtotal * taxPercent) / 100.0
    val calculatedTotal = parsedSubtotal + calculatedTax

    // Active View Mode: 0 -> A4 Document Visual Preview, 1 -> Edit Details, 2 -> Saved Invoices
    var activeMode by remember { mutableIntStateOf(0) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Function to build current InvoiceEntity
    fun currentInvoiceEntity(): InvoiceEntity {
        return InvoiceEntity(
            id = initialInvoice?.id ?: 0L,
            invoiceNumber = invoiceNumber.trim(),
            clientName = clientName.trim(),
            clientCompany = clientCompany.trim(),
            clientEmail = clientEmail.trim(),
            clientPhone = clientPhone.trim(),
            issueDate = issueDate.trim(),
            dueDate = dueDate.trim(),
            currency = "₹",
            subtotal = parsedSubtotal,
            taxPercent = taxPercent,
            totalAmount = calculatedTotal,
            status = invoiceStatus,
            itemsSummary = itemsSummary.trim(),
            notes = notes.trim()
        )
    }

    // Auto-generate physical PDF on background thread whenever fields update
    LaunchedEffect(invoiceNumber, clientName, clientCompany, clientPhone, clientEmail, itemsSummary, parsedSubtotal, taxPercent) {
        scope.launch(Dispatchers.IO) {
            try {
                val inv = currentInvoiceEntity()
                val file = InvoicePdfGenerator.generateInvoicePdf(context, inv)
                withContext(Dispatchers.Main) {
                    generatedPdfFile = file
                }
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🦁", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Milo PDF Invoice Generator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                        }
                        Text(
                            "Instant GST Invoicing & WhatsApp Dispatch",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GST 18%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Share via System Sheet
                    OutlinedButton(
                        onClick = {
                            val inv = currentInvoiceEntity()
                            scope.launch(Dispatchers.IO) {
                                val file = InvoicePdfGenerator.generateInvoicePdf(context, inv)
                                withContext(Dispatchers.Main) {
                                    val shareIntent = Intent.createChooser(
                                        InvoicePdfGenerator.createShareIntent(context, file, "Tax Invoice ${inv.invoiceNumber}"),
                                        "Share PDF Invoice via..."
                                    )
                                    context.startActivity(shareIntent)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_pdf_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // 2. Direct WhatsApp Dispatch
                    Button(
                        onClick = {
                            val inv = currentInvoiceEntity()
                            // Save to Room DB first
                            viewModel.addInvoice(inv)
                            scope.launch(Dispatchers.IO) {
                                val file = InvoicePdfGenerator.generateInvoicePdf(context, inv)
                                withContext(Dispatchers.Main) {
                                    InvoicePdfGenerator.sharePdfToWhatsApp(
                                        context = context,
                                        pdfFile = file,
                                        recipientPhone = inv.clientPhone,
                                        clientName = inv.clientName,
                                        docNumber = inv.invoiceNumber,
                                        totalAmount = inv.totalAmount
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("whatsapp_dispatch_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send on WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        },
        containerColor = Color(0xFFF1F5F9)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mode Selector Tabs (Preview A4, Edit Data, History)
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeMode == 0,
                        onClick = { activeMode = 0 },
                        label = { Text("📄 A4 Visual Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1.2f)
                    )

                    FilterChip(
                        selected = activeMode == 1,
                        onClick = { activeMode = 1 },
                        label = { Text("✏️ Edit Fields", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = activeMode == 2,
                        onClick = { activeMode = 2 },
                        label = { Text("📋 Recent (${allInvoices.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Client Auto-Populate from Leads
            if (activeMode == 1 && leads.isNotEmpty()) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            "⚡ Milo Quick Fill from CRM Leads:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(leads.take(6)) { lead ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable {
                                        clientName = lead.name
                                        clientCompany = lead.company
                                        clientPhone = lead.phone
                                        clientEmail = lead.email
                                        itemsSummary = lead.requirement.ifBlank { "Website & Custom CRM Application Suite" }
                                        val parsedLeadVal = lead.potentialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 25000.0
                                        subtotalText = String.format(Locale.US, "%.0f", parsedLeadVal.coerceAtLeast(25000.0))
                                        Toast.makeText(context, "Populated data for ${lead.name}", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("👤", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(lead.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(lead.company, fontSize = 9.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Body Content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeMode) {
                    0 -> RenderA4DocumentPreview(
                        invoice = currentInvoiceEntity(),
                        onOpenSystemViewer = {
                            val inv = currentInvoiceEntity()
                            scope.launch(Dispatchers.IO) {
                                val file = InvoicePdfGenerator.generateInvoicePdf(context, inv)
                                withContext(Dispatchers.Main) {
                                    InvoicePdfGenerator.openInSystemViewer(context, file)
                                }
                            }
                        },
                        onSaveToDatabase = {
                            val inv = currentInvoiceEntity()
                            viewModel.addInvoice(inv)
                            Toast.makeText(context, "✅ Saved ${inv.invoiceNumber} to Invoices Database!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    1 -> EditInvoiceFieldsForm(
                        invoiceNumber = invoiceNumber,
                        onInvoiceNumberChange = { invoiceNumber = it },
                        clientName = clientName,
                        onClientNameChange = { clientName = it },
                        clientCompany = clientCompany,
                        onClientCompanyChange = { clientCompany = it },
                        clientPhone = clientPhone,
                        onClientPhoneChange = { clientPhone = it },
                        clientEmail = clientEmail,
                        onClientEmailChange = { clientEmail = it },
                        itemsSummary = itemsSummary,
                        onItemsSummaryChange = { itemsSummary = it },
                        subtotalText = subtotalText,
                        onSubtotalTextChange = { subtotalText = it },
                        taxPercent = taxPercent,
                        onTaxPercentChange = { taxPercent = it },
                        issueDate = issueDate,
                        onIssueDateChange = { issueDate = it },
                        dueDate = dueDate,
                        onDueDateChange = { dueDate = it },
                        notes = notes,
                        onNotesChange = { notes = it },
                        invoiceStatus = invoiceStatus,
                        onStatusChange = { invoiceStatus = it },
                        onDoneEditing = { activeMode = 0 }
                    )
                    2 -> RecentInvoicesList(
                        invoices = allInvoices,
                        onSelectInvoice = { selected ->
                            invoiceNumber = selected.invoiceNumber
                            clientName = selected.clientName
                            clientCompany = selected.clientCompany
                            clientPhone = selected.clientPhone
                            clientEmail = selected.clientEmail
                            itemsSummary = selected.itemsSummary
                            subtotalText = String.format(Locale.US, "%.0f", selected.subtotal)
                            taxPercent = selected.taxPercent
                            issueDate = selected.issueDate
                            dueDate = selected.dueDate
                            notes = selected.notes
                            invoiceStatus = selected.status
                            activeMode = 0
                        }
                    )
                }
            }
        }
    }
}

/**
 * High-Fidelity Rendered A4 Document Canvas Preview.
 */
@Composable
private fun RenderA4DocumentPreview(
    invoice: InvoiceEntity,
    onOpenSystemViewer: () -> Unit,
    onSaveToDatabase: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating Utility Action Pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
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
                            "MakingBrands_${invoice.invoiceNumber}.pdf",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Standard A4 · 595 × 842 pt · Ready to Share",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onOpenSystemViewer,
                        modifier = Modifier.size(32.dp).background(Color(0xFF334155), CircleShape)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "View", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onSaveToDatabase,
                        modifier = Modifier.size(32.dp).background(BrandBlue, CircleShape)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // The A4 Physical Sheet Simulation
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // 1. Corporate Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🦁", fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("MAKING BRANDS", fontWeight = FontWeight.Black, fontSize = 15.sp, color = BrandDarkBlue)
                            Text("Digital Solutions & Technology", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFF93C5FD))
                        ) {
                            Text(
                                "TAX INVOICE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = BrandBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(invoice.invoiceNumber, fontWeight = FontWeight.Black, fontSize = 13.sp, color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Cyber City, Phase II, Gurugram, Haryana · GSTIN: 07AAAAA0000A1Z5", fontSize = 9.sp, color = TextSecondary)
                Text("Email: billing@makingbrands.in · Phone: +91 98765 43210", fontSize = 9.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 1.5.dp, color = BrandDarkBlue)
                Spacer(modifier = Modifier.height(12.dp))

                // 2. Client & Metadata Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("BILLED TO:", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(invoice.clientCompany, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Text("Attn: ${invoice.clientName}", fontSize = 11.sp, color = Color(0xFF334155))
                        Text("Phone: ${invoice.clientPhone}", fontSize = 10.sp, color = TextSecondary)
                        Text("Email: ${invoice.clientEmail}", fontSize = 10.sp, color = TextSecondary)
                    }

                    Column(modifier = Modifier.weight(0.8f), horizontalAlignment = Alignment.End) {
                        Text("INVOICE METRICS:", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Date: ${invoice.issueDate}", fontSize = 10.sp, color = TextPrimary)
                        Text("Due Date: ${invoice.dueDate}", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                        Text("Terms: Net 15 Days", fontSize = 10.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (invoice.status.equals("Paid", ignoreCase = true)) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                "STATUS: ${invoice.status.uppercase()}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (invoice.status.equals("Paid", ignoreCase = true)) Color(0xFF15803D) else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Tabular Deliverables
                Surface(
                    color = BrandDarkBlue,
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("DESCRIPTION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(2.5f))
                        Text("QTY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
                        Text("RATE (₹)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                        Text("AMOUNT (₹)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(2.5f)) {
                        Text(invoice.itemsSummary, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Complete full-stack implementation, UI/UX and deployment.", fontSize = 9.sp, color = TextSecondary)
                    }
                    Text("1 Lot", fontSize = 10.sp, color = Color(0xFF334155), textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
                    Text(String.format(Locale.US, "%,.2f", invoice.subtotal), fontSize = 10.sp, color = Color(0xFF334155), textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                    Text(String.format(Locale.US, "%,.2f", invoice.subtotal), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.End, modifier = Modifier.weight(1.3f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Financial Calculations & Bank Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Bank Details
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("BANK & UPI DETAILS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("A/C: 50200088991122 (HDFC)", fontSize = 8.sp, color = Color(0xFF334155))
                            Text("IFSC: HDFC0001234", fontSize = 8.sp, color = Color(0xFF334155))
                            Text("UPI: makingbrands@hdfcbank", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Calculation Table
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal:", fontSize = 9.sp, color = TextSecondary)
                                Text("₹ ${String.format(Locale.US, "%,.2f", invoice.subtotal)}", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CGST (9%):", fontSize = 8.sp, color = TextSecondary)
                                Text("₹ ${String.format(Locale.US, "%,.2f", invoice.subtotal * 0.09)}", fontSize = 8.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("SGST (9%):", fontSize = 8.sp, color = TextSecondary)
                                Text("₹ ${String.format(Locale.US, "%,.2f", invoice.subtotal * 0.09)}", fontSize = 8.sp)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = Color(0xFFCBD5E1))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Payable:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("₹ ${String.format(Locale.US, "%,.2f", invoice.totalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF15803D))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Terms & Signatory Seal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("TERMS & CONDITIONS:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Text(invoice.notes, fontSize = 7.5.sp, color = TextSecondary, lineHeight = 10.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.8f)) {
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
                        Text("Authorized Signatory", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Making Brands Tech Solutions", fontSize = 7.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

/**
 * Interactive Form for Customizing Invoice Data before Generating PDF.
 */
@Composable
private fun EditInvoiceFieldsForm(
    invoiceNumber: String,
    onInvoiceNumberChange: (String) -> Unit,
    clientName: String,
    onClientNameChange: (String) -> Unit,
    clientCompany: String,
    onClientCompanyChange: (String) -> Unit,
    clientPhone: String,
    onClientPhoneChange: (String) -> Unit,
    clientEmail: String,
    onClientEmailChange: (String) -> Unit,
    itemsSummary: String,
    onItemsSummaryChange: (String) -> Unit,
    subtotalText: String,
    onSubtotalTextChange: (String) -> Unit,
    taxPercent: Double,
    onTaxPercentChange: (Double) -> Unit,
    issueDate: String,
    onIssueDateChange: (String) -> Unit,
    dueDate: String,
    onDueDateChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    invoiceStatus: String,
    onStatusChange: (String) -> Unit,
    onDoneEditing: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("📄 Invoice Identifiers", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)

                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = onInvoiceNumberChange,
                    label = { Text("Invoice Number") },
                    modifier = Modifier.fillMaxWidth().testTag("invoice_number_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = issueDate,
                        onValueChange = onIssueDateChange,
                        label = { Text("Issue Date") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = onDueDateChange,
                        label = { Text("Due Date") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("👤 Client / Recipient Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)

                OutlinedTextField(
                    value = clientCompany,
                    onValueChange = onClientCompanyChange,
                    label = { Text("Client Company Name") },
                    modifier = Modifier.fillMaxWidth().testTag("client_company_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = clientName,
                    onValueChange = onClientNameChange,
                    label = { Text("Contact Person Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = onClientPhoneChange,
                        label = { Text("WhatsApp / Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = clientEmail,
                        onValueChange = onClientEmailChange,
                        label = { Text("Email Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("💰 Line Items & Financials", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)

                OutlinedTextField(
                    value = itemsSummary,
                    onValueChange = onItemsSummaryChange,
                    label = { Text("Item / Deliverables Scope") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = subtotalText,
                        onValueChange = onSubtotalTextChange,
                        label = { Text("Subtotal Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("subtotal_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = "18% (GST)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tax Rate") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Terms & Payment Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Button(
            onClick = onDoneEditing,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("apply_changes_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Update A4 PDF Preview", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

/**
 * List of Recent Invoices saved in Room DB.
 */
@Composable
private fun RecentInvoicesList(
    invoices: List<InvoiceEntity>,
    onSelectInvoice: (InvoiceEntity) -> Unit
) {
    if (invoices.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🦁", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("No Invoices Generated Yet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Text("Ask Milo or create an invoice above to auto-generate PDFs.", fontSize = 12.sp, color = TextSecondary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(invoices) { inv ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().clickable { onSelectInvoice(inv) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandDarkBlue)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (inv.status.equals("Paid", ignoreCase = true)) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        inv.status.uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (inv.status.equals("Paid", ignoreCase = true)) Color(0xFF15803D) else Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(inv.clientCompany, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
                            Text("Attn: ${inv.clientName} · ${inv.issueDate}", fontSize = 10.sp, color = TextSecondary)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹ ${String.format(Locale.US, "%,.0f", inv.totalAmount)}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF15803D))
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { onSelectInvoice(inv) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Open PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
