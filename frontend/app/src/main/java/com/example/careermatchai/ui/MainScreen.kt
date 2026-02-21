@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.careermatchai.ui

import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.careermatchai.data.remote.AnalysisRow
import com.example.careermatchai.data.remote.BASE_URL
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onCreateNew: () -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    isDemo: Boolean,
    onToggleDemo: () -> Unit,
    demoResume: String,
    demoJob: String
) {
    val s = viewModel.state
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isAuthed = s.token != null
    var query by rememberSaveable { mutableStateOf("") }

    // feedback
    LaunchedEffect(s.error) { s.error?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(s.analysis?.id) {
        s.analysis?.let {
            val pct = fmtPct(it.readinessScore)
            snackbar.showSnackbar("Analysis complete • $pct")
        }
    }
    LaunchedEffect(query) {
        delay(300)
        viewModel.loadAnalyses(page = 1, q = query.takeIf { it.isNotBlank() })
    }

    // ✅ Material 3 Pull-to-refresh state
    val isRefreshing = s.listLoading && s.page == 1
    val refreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("CareerMatch AI") },
                actions = {
                    if (isAuthed) {
                        IconButton(onClick = { viewModel.loadAnalyses(1) }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { pad ->

        // ✅ Use PullToRefreshBox (no .pullRefresh modifier / no external indicator)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.reloadFirstPage() },
            state = refreshState,
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            val scroll = rememberScrollState()
            var email by remember { mutableStateOf("demo@example.com") }
            var password by remember { mutableStateOf("Password123!") }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Auth / Prefs ---
                ElevatedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Welcome", style = MaterialTheme.typography.titleMedium)

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AssistChip(onClick = { viewModel.ping() }, label = { Text("Ping Backend") })
                            val statusText = s.pingStatus?.ifBlank { "Unknown" } ?: "Unknown"
                            AssistChip(onClick = {}, label = { Text("Health: $statusText") }, enabled = false)
                        }

                        if (!isAuthed) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    if (!isAuthed && (email.isBlank() || password.isBlank())) {
                                        scope.launch { snackbar.showSnackbar("Email and password required") }
                                    } else {
                                        viewModel.login(email, password)
                                    }
                                }
                            ) { Text(if (isAuthed) "Re-login" else "Login") }

                            if (isAuthed) {
                                OutlinedButton(onClick = { viewModel.logout() }) { Text("Logout") }
                            }
                        }

                        // Theme toggle
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Dark theme", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Switch between light and dark",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = isDark, onCheckedChange = { onToggleTheme() })
                        }

                        // Demo toggle
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Demo mode", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Prefill New Analysis with sample text",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = isDemo, onCheckedChange = { onToggleDemo() })
                        }

                        val status = if (isAuthed) "Authenticated" else "Logged out"
                        Text(
                            "Status: $status",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isAuthed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (s.token != null) {
                            Text("Token: ${s.token.take(12)}…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // --- Quick analyze demo ---
                ElevatedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Quick Demo", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = {
                                viewModel.analyze(
                                    resume = "Python • FastAPI • Docker • Kubernetes • REST",
                                    job = "Seeking Python / FastAPI / PostgreSQL backend engineer"
                                )
                            },
                            enabled = isAuthed
                        ) { Text("Analyze Sample") }
                        if (!isAuthed) {
                            Text("Login to enable analysis and history.", style = MaterialTheme.typography.bodySmall)
                        }

                        s.analysis?.let { out ->
                            HorizontalDivider(Modifier.padding(top = 4.dp))
                            Text("Latest Result", style = MaterialTheme.typography.titleSmall)
                            ScoreBar(pct = normalizePct(s.analysis!!.readinessScore))
                            Text(
                                "Suggestions",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(out.suggestions.joinToString { it.message })
                        }
                    }
                }

                if (s.loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                // --- History ---
                if (isAuthed) {
                    AnalysesSection(
                        analyses = s.analyses,
                        loading = s.listLoading,
                        error   = s.listError,
                        page    = s.page,
                        total   = s.total,
                        pageSize = s.pageSize,
                        onPrev = { viewModel.prevPage() },
                        onNext = { viewModel.nextPage() },
                        onOpen = { id -> viewModel.openAnalysis(id) },
                        onOpenReport = { id ->
                            val url = BASE_URL.trimEnd('/') + "/reports/$id"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        onCreateNew = onCreateNew,
                        onDelete = { id -> viewModel.deleteAnalysis(id) },
                        query = query,
                        onQueryChange = { query = it },
                        onShare = { id ->
                            val url = BASE_URL.trimEnd('/') + "/reports/$id"
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "CareerMatch AI Report")
                                putExtra(Intent.EXTRA_TEXT, url)
                            }
                            context.startActivity(Intent.createChooser(send, "Share report via"))
                        },
                        sort = s.sort,
                        onChangeSort = { viewModel.setSort(it) },
                        onChangePageSize = { viewModel.setPageSize(it) },
                        onCopied = { scope.launch { snackbar.showSnackbar("Link copied") } }
                    )
                }
            }
        }
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun AnalysesSection(
    analyses: List<AnalysisRow>,   // ✅ fix generic syntax
    loading: Boolean,
    error: String?,
    page: Int,
    total: Int,
    pageSize: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onOpen: (String) -> Unit,
    onOpenReport: (String) -> Unit,
    onCreateNew: () -> Unit,
    onDelete: (String) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onShare: (String) -> Unit,
    sort: Sort,
    onChangeSort: (Sort) -> Unit,
    onChangePageSize: (Int) -> Unit,
    onCopied: () -> Unit
) {
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    ElevatedCard {
        Column(Modifier.padding(16.dp)) {
            // Header row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Analyses", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onCreateNew) { Text("New Analysis") }
            }

            // Sort + page size row
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = (sort == Sort.DATE_DESC),
                        onClick = { onChangeSort(Sort.DATE_DESC) },
                        label = { Text("Newest") }
                    )
                    FilterChip(
                        selected = (sort == Sort.SCORE_DESC),
                        onClick = { onChangeSort(Sort.SCORE_DESC) },
                        label = { Text("Top Score") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 20).forEach { sz ->
                        AssistChip(
                            onClick = { onChangePageSize(sz) },
                            label = { Text("$sz") },
                            enabled = pageSize != sz
                        )
                    }
                }
            }

            // Search
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search analyses") },
                placeholder = { Text("Filter by keyword…") },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        TextButton(onClick = { onQueryChange("") }) { Text("Clear") }
                    }
                }
            )

            when {
                loading -> {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                error != null -> {
                    Spacer(Modifier.height(12.dp))
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                }
                analyses.isEmpty() -> {
                    Spacer(Modifier.height(12.dp))
                    EmptyState(
                        title = "No analyses yet",
                        subtitle = if (query.isBlank())
                            "Run your first analysis to see it here."
                        else
                            "No results for “$query”."
                    )
                }
                else -> {
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(analyses) { row ->
                            val shareUrl = BASE_URL.trimEnd('/') + "/reports/${row.id}"
                            AnalysisRowCard(
                                row = row,
                                onOpen = { onOpen(row.id) },
                                onOpenReport = { onOpenReport(row.id) },
                                onShare = { onShare(row.id) },
                                onCopyLink = {
                                    clipboard.setText(AnnotatedString(shareUrl))
                                    onCopied()
                                },
                                onDelete = { confirmDeleteId = row.id }
                            )
                        }
                    }
                }
            }

            val maxPage = ((total + pageSize - 1) / pageSize).coerceAtLeast(1)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Page $page / $maxPage", style = MaterialTheme.typography.bodySmall)
                Row {
                    OutlinedButton(onClick = onPrev, enabled = page > 1) { Text("Prev") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onNext, enabled = page < maxPage) { Text("Next") }
                }
            }
        }
    }

    if (confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Delete analysis?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(confirmDeleteId!!)
                    confirmDeleteId = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteId = null }) { Text("Cancel") } }
        )
    }
}

/* ---------- helpers ---------- */

@Composable
private fun AnalysisRowCard(
    row: AnalysisRow,
    onOpen: () -> Unit,
    onOpenReport: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onDelete: () -> Unit
) {
    val pct = normalizePct(row.readinessScore)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpen() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Analysis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ScoreChip(pct = pct)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (pct / 100.0).toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                formatIso(row.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenReport) { Text("Open Report") }
                IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, contentDescription = "Share") }
                IconButton(onClick = onCopyLink) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy link") }
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete analysis")
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ScoreBar(pct: Double) {
    val clamped = normalizePct(pct)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ScoreChip(pct = clamped)
        Spacer(Modifier.width(12.dp))
        LinearProgressIndicator(
            progress = { (clamped / 100.0).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
    Spacer(Modifier.height(4.dp))
    Text("Score: ${"%.1f".format(clamped)}%", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ScoreChip(pct: Double) {
    val color = scoreColor(pct)
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text("${"%.0f".format(pct)}%") },
        leadingIcon = {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    )
}

private fun scoreColor(pct: Double): Color = when {
    pct >= 80 -> Color(0xFF2E7D32)
    pct >= 60 -> Color(0xFFF9A825)
    else      -> Color(0xFFC62828)
}

private fun formatIso(iso: String): String = try {
    val odt = OffsetDateTime.parse(iso)
    odt.format(DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a"))
} catch (_: Throwable) {
    iso
}
