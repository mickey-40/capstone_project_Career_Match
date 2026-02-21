package com.example.careermatchai.ui.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalClipboardManager

data class AnalysisUi(
    val id: String,
    val title: String,
    val summary: String,
    val shareUrl: String // ensure your mapping fills this
)

@Composable
fun AnalysisCard(
    item: AnalysisUi,
    onShare: (AnalysisUi) -> Unit,
    onCopyLink: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(item.summary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onShare(item) }) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share")
                }
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(item.shareUrl))
                    onCopyLink(item.shareUrl)
                }) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy link")
                }
            }
        }
    }
}
