@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.careermatchai.ui

import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    val semanticAvailable = isLocalBackend(BASE_URL)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isAuthed = s.token != null
    var query by rememberSaveable { mutableStateOf("") }
    var useSemantic by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(s.error) { s.error?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(s.analysis?.id) {
        s.analysis?.let {
            val pct = fmtPct(it.readinessScore)
            snackbar.showSnackbar("Analysis complete - $pct")
        }
    }
    LaunchedEffect(query) {
        delay(300)
        viewModel.loadAnalyses(page = 1, q = query.takeIf { it.isNotBlank() })
    }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionCard(title = "Sign In") {
                    if (!isAuthed) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }

                if (isAuthed) {
                    SectionCard(title = "Preferences") {
                        LabeledSwitchRow(
                            title = "Dark theme",
                            subtitle = "Switch between light and dark",
                            checked = isDark,
                            onCheckedChange = { onToggleTheme() }
                        )
                        Spacer(Modifier.height(8.dp))
                        LabeledSwitchRow(
                            title = "Demo mode",
                            subtitle = "Prefill new analysis with sample text",
                            checked = isDemo,
                            onCheckedChange = { onToggleDemo() }
                        )
                        Spacer(Modifier.height(8.dp))
                        LabeledSwitchRow(
                            title = "Semantic matching",
                            subtitle = if (semanticAvailable) {
                                "Use embeddings for deeper matching"
                            } else {
                                "Local backend only (disabled on hosted backend)"
                            },
                            checked = useSemantic,
                            onCheckedChange = { useSemantic = it },
                            enabled = semanticAvailable
                        )
                        if (semanticAvailable) {
                            Text(
                                "First semantic run may take longer while model files load.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SectionCard(title = "Quick Demo") {
                        Button(
                            onClick = {
                                viewModel.analyze(
                                    resume = "Python FastAPI Docker Kubernetes REST",
                                    job = "Seeking Python FastAPI PostgreSQL backend engineer",
                                    strategy = if (semanticAvailable && useSemantic) "embedding" else "keyword"
                                )
                            }
                        ) { Text("Analyze sample") }
                    }
                }

                s.analysis?.let { out ->
                    SectionCard(title = "Latest Result") {
                        ScoreBar(pct = normalizePct(out.readinessScore))
                        val semanticScore = out.semantic?.score
                        if (semanticScore != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Keyword: ${fmtPct(out.readinessScore)}  Semantic: ${fmtPct(semanticScore)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Suggestions",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(out.suggestions.joinToString { it.message })
                    }
                }

                if (s.loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (isAuthed) {
                    AnalysesSection(
                        analyses = s.analyses,
                        loading = s.listLoading,
                        error = s.listError,
                        page = s.page,
                        total = s.total,
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
    analyses: List<AnalysisRow>,
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

    SectionCard(title = "My Analyses", headerAction = {
        TextButton(onClick = onCreateNew) { Text("New analysis") }
    }) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search analyses") },
            placeholder = { Text("Filter by keyword") },
            trailingIcon = {
                if (query.isNotBlank()) {
                    TextButton(onClick = { onQueryChange("") }) { Text("Clear") }
                }
            }
        )

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = sort == Sort.DATE_DESC,
                    onClick = { onChangeSort(Sort.DATE_DESC) },
                    label = { Text("Newest") }
                )
                FilterChip(
                    selected = sort == Sort.SCORE_DESC,
                    onClick = { onChangeSort(Sort.SCORE_DESC) },
                    label = { Text("Top score") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10, 20).forEach { sz ->
                    AssistChip(
                        onClick = { onChangePageSize(sz) },
                        enabled = pageSize != sz,
                        label = { Text("$sz") }
                    )
                }
            }
        }

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
                    subtitle = if (query.isBlank()) {
                        "Run your first analysis to see it here."
                    } else {
                        "No results for '$query'."
                    }
                )
            }
            else -> {
                Spacer(Modifier.height(10.dp))
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Page $page / $maxPage", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPrev, enabled = page > 1) { Text("Prev") }
                Button(onClick = onNext, enabled = page < maxPage) { Text("Next") }
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

@Composable
private fun SectionCard(
    title: String,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                headerAction?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun StatusChip(label: String, value: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text("$label: $value") }
    )
}

@Composable
private fun LabeledSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

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
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Analysis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        formatIso(row.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ScoreChip(pct = pct)
            }

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (pct / 100.0).toFloat() },
                modifier = Modifier.fillMaxWidth()
            )

            if (row.semanticScore != null || row.keywordScore != null) {
                Spacer(Modifier.height(8.dp))
                val keyword = row.keywordScore ?: row.readinessScore
                val semantic = row.semanticScore
                val scoreLine = if (semantic != null) {
                    "Keyword: ${fmtPct(keyword)}  Semantic: ${fmtPct(semantic)}"
                } else {
                    "Keyword: ${fmtPct(keyword)}"
                }
                Text(
                    scoreLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onOpenReport) { Text("Report") }
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
    else -> Color(0xFFC62828)
}

private fun formatIso(iso: String): String = try {
    val odt = OffsetDateTime.parse(iso)
    odt.format(DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a"))
} catch (_: Throwable) {
    iso
}

private fun isLocalBackend(baseUrl: String): Boolean {
    val host = Uri.parse(baseUrl).host.orEmpty().lowercase()
    return host in setOf("10.0.2.2", "127.0.0.1", "localhost")
}
