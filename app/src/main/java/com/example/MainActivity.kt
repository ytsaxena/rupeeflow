package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CurrencyFormatUtils
import com.example.ui.dialogs.AddBorrowLendDialog
import com.example.ui.dialogs.AddManualEntryDialog
import com.example.ui.dialogs.ExportDateRangeDialog
import com.example.ui.dialogs.PdfPasswordDialog
import com.example.ui.dialogs.UploadProgressDialog
import com.example.ui.screens.BorrowLendScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.StatementDetailScreen
import com.example.ui.screens.StatementsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BorrowLendViewModel
import com.example.viewmodel.ExportState
import com.example.viewmodel.ImportState
import com.example.viewmodel.StatementViewModel
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.launch

enum class MainNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Dashboard", Icons.Filled.BarChart, Icons.Outlined.BarChart, "tab_dashboard"),
    STATEMENTS("Statements", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "tab_statements"),
    BORROW_LEND("Borrow & Lend", Icons.Filled.Handshake, Icons.Outlined.Handshake, "tab_borrow_lend")
}

class MainActivity : ComponentActivity() {

    private val viewModel: StatementViewModel by viewModels()
    private val borrowLendViewModel: BorrowLendViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel, borrowLendViewModel = borrowLendViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: StatementViewModel, borrowLendViewModel: BorrowLendViewModel) {
    var currentTab by remember { mutableStateOf(MainNavTab.DASHBOARD) }
    var selectedStatementId by remember { mutableStateOf<Long?>(null) }
    var showAddManualDialog by remember { mutableStateOf(false) }
    var showExportDateDialog by remember { mutableStateOf(false) }
    var showAddBorrowLendDialog by remember { mutableStateOf(false) }
    var pendingExportRange by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importPdf(uri)
    }

    val pdfSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val range = pendingExportRange
        if (uri != null && range != null) {
            viewModel.exportRange(uri, range.first, range.second)
        }
        pendingExportRange = null
    }

    LaunchedEffect(importState) {
        when (val state = importState) {
            is ImportState.Success -> {
                snackbarHostState.showSnackbar("Imported ${state.count} transactions")
                viewModel.dismissImport()
            }
            is ImportState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.dismissImport()
            }
            else -> Unit
        }
    }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Success -> {
                snackbarHostState.showSnackbar("Statement exported")
                viewModel.dismissExport()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.dismissExport()
            }
            else -> Unit
        }
    }

    if (importState is ImportState.NeedsPassword) {
        PdfPasswordDialog(
            wrongAttempt = (importState as ImportState.NeedsPassword).wrongAttempt,
            onDismiss = { viewModel.dismissImport() },
            onSubmit = { password -> viewModel.submitPassword(password) }
        )
    }

    if (importState is ImportState.Loading) {
        UploadProgressDialog()
    }

    if (exportState is ExportState.Loading) {
        UploadProgressDialog(message = "Generating PDF…")
    }

    if (showAddManualDialog) {
        AddManualEntryDialog(
            categories = categories,
            onDismiss = { showAddManualDialog = false },
            onSave = { type, date, amount, description, statementName, category ->
                viewModel.addManualEntry(type, date, amount, description, statementName, category)
                showAddManualDialog = false
            }
        )
    }

    if (showExportDateDialog) {
        ExportDateRangeDialog(
            onDismiss = { showExportDateDialog = false },
            onConfirm = { startUtcMillis, endUtcMillis ->
                val (rangeStart, _) = CurrencyFormatUtils.localDayRangeFromUtcMillis(startUtcMillis)
                val (_, rangeEnd) = CurrencyFormatUtils.localDayRangeFromUtcMillis(endUtcMillis)
                pendingExportRange = rangeStart to rangeEnd
                showExportDateDialog = false
                pdfSaveLauncher.launch("RupeeFlow_Statement.pdf")
            }
        )
    }

    if (showAddBorrowLendDialog) {
        AddBorrowLendDialog(
            onDismiss = { showAddBorrowLendDialog = false },
            onSave = { personInfo, amount, type, date ->
                borrowLendViewModel.addEntry(personInfo, amount, type, date)
                showAddBorrowLendDialog = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedStatementId == null) {
                if (currentTab == MainNavTab.BORROW_LEND) {
                    FloatingActionButton(
                        onClick = { showAddBorrowLendDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier.padding(bottom = 8.dp).testTag("add_borrow_lend_fab")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add borrow/lend entry", modifier = Modifier.size(24.dp))
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (currentTab == MainNavTab.DASHBOARD) {
                            SmallFloatingActionButton(
                                onClick = { showAddManualDialog = true },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.testTag("add_manual_entry_fab")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add transaction")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        FloatingActionButton(
                            onClick = {
                                if (importState !is ImportState.Loading) {
                                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                            modifier = Modifier.padding(bottom = 8.dp).testTag("upload_statement_fab")
                        ) {
                            if (importState is ImportState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(imageVector = Icons.Default.UploadFile, contentDescription = "Upload Statement", modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (selectedStatementId == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).testTag("main_navigation_bar")
                ) {
                    MainNavTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(text = tab.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val statementId = selectedStatementId
        if (statementId != null) {
            StatementDetailScreen(
                viewModel = viewModel,
                statementId = statementId,
                onBack = { selectedStatementId = null },
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
        } else {
            Crossfade(
                targetState = currentTab,
                label = "tab_crossfade",
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) { tab ->
                when (tab) {
                    MainNavTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onUploadClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        onViewAllTransactions = { currentTab = MainNavTab.STATEMENTS },
                        onAutoCategorizeClick = {
                            coroutineScope.launch {
                                val count = viewModel.autoCategorizeExisting()
                                snackbarHostState.showSnackbar(
                                    if (count > 0) "Tagged $count transaction${if (count == 1) "" else "s"}"
                                    else "No new transactions matched a known category"
                                )
                            }
                        }
                    )
                    MainNavTab.STATEMENTS -> StatementsScreen(
                        viewModel = viewModel,
                        onOpenStatement = { selectedStatementId = it },
                        onExportClick = { showExportDateDialog = true }
                    )
                    MainNavTab.BORROW_LEND -> BorrowLendScreen(
                        viewModel = borrowLendViewModel
                    )
                }
            }
        }
    }
}
