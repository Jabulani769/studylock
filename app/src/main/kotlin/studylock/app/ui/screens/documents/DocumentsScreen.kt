package studylock.app.ui.screens.documents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import studylock.app.R
import studylock.app.AppUiState
import studylock.app.ui.components.PrimaryActionButton
import studylock.app.ui.components.SectionHeading
import studylock.app.ui.components.StudyLockCard
import studylock.app.ui.theme.Spacing

@Composable
fun DocumentsScreen(
    state: AppUiState,
    onImport: (Uri) -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onImport(uri) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
    ) {
        SectionHeading(
            title = "Study materials",
            supporting = "Read your own documents without leaving StudyLock."
        )
        Spacer(Modifier.height(Spacing.md))
        PrimaryActionButton(
            text = "Import PDF, image or text",
            onClick = { picker.launch("*/*") }
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Files are copied into this app's private storage. Nothing is uploaded, " +
                "and documents are not part of backups.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.lg))
        if (state.documents.isEmpty()) {
            StudyLockCard {
                Text(
                    text = "No documents yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "Import a study document to read it here. Your place is remembered.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            state.documents.forEach { document ->
                StudyLockCard {
                    Text(
                        text = document.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "${document.type.name} · page/position ${document.lastPosition + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { onOpen(document.id) },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Read", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onDelete(document.id) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_remove),
                                contentDescription = "Remove document",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }
        }
    }
}
