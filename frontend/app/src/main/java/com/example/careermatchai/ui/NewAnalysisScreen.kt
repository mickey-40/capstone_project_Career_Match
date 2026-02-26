@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.careermatchai.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.careermatchai.data.remote.BASE_URL
import kotlinx.coroutines.launch

@Composable
fun NewAnalysisScreen(
    viewModel: MainViewModel,
    onDone: () -> Unit,
    // ⬇️ added props from MainActivity
    isDemo: Boolean,
    demoResume: String,
    demoJob: String
) {
    val s = viewModel.state
    val semanticAvailable = isLocalBackend(BASE_URL)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scroll = rememberScrollState()

    var resume by remember { mutableStateOf(TextFieldValue("")) }
    var job by remember { mutableStateOf(TextFieldValue("")) }
    var submitting by remember { mutableStateOf(false) }
    var lastCreatedId by remember { mutableStateOf<String?>(null) }
    var useSemantic by rememberSaveable { mutableStateOf(false) }

    // Autofill when Demo mode is ON and fields are empty
    LaunchedEffect(isDemo) {
        if (isDemo && resume.text.isBlank() && job.text.isBlank()) {
            if (demoResume.isNotBlank()) resume = TextFieldValue(demoResume)
            if (demoJob.isNotBlank()) job = TextFieldValue(demoJob)
        }
    }

    LaunchedEffect(s.error) { s.error?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(s.analysis?.id) {
        if (submitting && s.analysis?.id != null) {
            lastCreatedId = s.analysis!!.id
            submitting = false
            snackbar.showSnackbar("Created analysis • ${fmtPct(s.analysis!!.readinessScore)}")
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("New Analysis") }) },
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Paste content", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = resume,
                        onValueChange = { resume = it },
                        label = { Text("Resume text") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = job,
                        onValueChange = { job = it },
                        label = { Text("Job description") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Semantic matching", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (semanticAvailable) {
                                    "Uses local embeddings for deeper matching"
                                } else {
                                    "Local backend only (disabled on hosted backend)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useSemantic,
                            onCheckedChange = { useSemantic = it },
                            enabled = semanticAvailable
                        )
                    }
                    if (semanticAvailable) {
                        Text(
                            "Note: first semantic run downloads the model and may take longer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                if (resume.text.isBlank() || job.text.isBlank()) {
                                    scope.launch { snackbar.showSnackbar("Please fill both fields") }
                                    return@Button
                                }
                                lastCreatedId = null
                                submitting = true
                                viewModel.analyze(
                                    resume.text,
                                    job.text,
                                    strategy = if (semanticAvailable && useSemantic) "embedding" else "keyword"
                                )
                            },
                            enabled = !submitting && (s.token != null)
                        ) { Text(if (submitting) "Submitting…" else "Run Analysis") }

                        OutlinedButton(
                            onClick = {
                                resume = TextFieldValue("")
                                job = TextFieldValue("")
                            }
                        ) { Text("Clear") }

                        TextButton(onClick = onDone) { Text("Back to Home") }
                    }

                    if (isDemo) {
                        TextButton(
                            onClick = {
                                resume = TextFieldValue(demoResume)
                                job = TextFieldValue(demoJob)
                            }
                        ) { Text("Use Demo Text") }
                    }
                }
            }

            if (lastCreatedId != null && s.analysis?.id == lastCreatedId) {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Result", style = MaterialTheme.typography.titleMedium)
                        ScoreBar(pct = s.analysis!!.readinessScore)
                        val semanticScore = s.analysis!!.semantic?.score
                        if (semanticScore != null) {
                            Text(
                                "Keyword: ${fmtPct(s.analysis!!.readinessScore)} • Semantic: ${fmtPct(semanticScore)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = {
                                val url = BASE_URL.trimEnd('/') + "/reports/${s.analysis!!.id}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }) { Text("Open Report") }
                            TextButton(onClick = onDone) { Text("Back to Home") }
                        }
                    }
                }
            }
        }
    }
}

private fun isLocalBackend(baseUrl: String): Boolean {
    val host = Uri.parse(baseUrl).host.orEmpty().lowercase()
    return host in setOf("10.0.2.2", "127.0.0.1", "localhost")
}
