package studylock.app.ui.screens.academics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.ImeAction
import studylock.app.R
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import studylock.app.AppUiState
import studylock.app.ModuleWithTopics
import studylock.app.parseOutline
import studylock.app.ui.components.DetailRow
import studylock.app.ui.components.EmptyState
import studylock.app.ui.components.PrimaryActionButton
import studylock.app.ui.components.SecondaryActionButton
import studylock.app.ui.components.SectionHeading
import studylock.app.ui.components.StatusChip
import studylock.app.ui.components.StudyLockCard
import studylock.app.ui.theme.Spacing
import studylock.domain.Module
import studylock.domain.Topic

@Composable
fun AcademicsScreen(
    state: AppUiState,
    onAddModule: (name: String, code: String?) -> Unit,
    onOpenModule: (UUID) -> Unit,
    onOpenSetup: () -> Unit,
    onConfirmMatch: (UUID) -> Unit,
    onRenameModule: (UUID, String, String?) -> Unit,
    onRemoveModule: (UUID) -> Unit,
    onEditSemester: (String, String) -> Unit
) {
    var showAddModule by remember { mutableStateOf(false) }
    var showEditSemester by remember { mutableStateOf(false) }
    var editingModule by remember { mutableStateOf<ModuleWithTopics?>(null) }
    var removingModule by remember { mutableStateOf<ModuleWithTopics?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
    ) {
        val semester = state.semester
        if (semester == null) {
            EmptyState(
                title = "No semester yet",
                body = "Add the academic year and semester label to start recording modules and topics.",
                actionLabel = "Set up my semester",
                onAction = onOpenSetup
            )
        } else {

            StudyLockCard {
                Text(
                    text = "Current semester",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = semester.label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = semester.academicYear,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.md))
                DetailRow(
                    label = "Start date",
                    value = semester.startDate?.format(
                        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
                    ) ?: "Not provided"
                )
                DetailRow(label = "Modules", value = state.modules.size.toString())
                DetailRow(
                    label = "Topics",
                    value = state.modules.sumOf { it.topics.size }.toString()
                )
                Spacer(Modifier.height(Spacing.sm))
                TextButton(onClick = { showEditSemester = true }) { Text("Edit semester") }
            }

            if (state.pendingTopicMatches.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xxl))
                SectionHeading(
                    title = "Confirm prior learning",
                    supporting = "These topics look similar. Only confirm a match if it is the same topic you already studied."
                )
                Spacer(Modifier.height(Spacing.md))
                state.pendingTopicMatches.forEach { match ->
                    StudyLockCard {
                        Text(
                            text = "Possible match",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        DetailRow(
                            label = "Earlier topic",
                            value = state.topicTitle(match.existingTopicId) ?: "Earlier learning"
                        )
                        DetailRow(
                            label = "Outline topic",
                            value = state.topicTitle(match.newTopicId) ?: "New topic"
                        )
                        DetailRow(
                            label = "Match confidence",
                            value = "${(match.matchConfidence * 100).toInt()}%"
                        )
                        val reason = match.matchReason
                        if (!reason.isNullOrBlank()) {
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(Spacing.md))
                        PrimaryActionButton(
                            text = "Confirm this match",
                            onClick = { onConfirmMatch(match.id) }
                        )
                    }
                    Spacer(Modifier.height(Spacing.md))
                }
            }
    
            Spacer(Modifier.height(Spacing.xxl))
            SectionHeading(
                title = "Modules",
                supporting = "Add only registered modules. Topics can be added as outlines arrive."
            )
            Spacer(Modifier.height(Spacing.md))
    
            if (state.modules.isEmpty()) {
                EmptyState(
                    title = "No modules yet",
                    body = "Add the modules you are registered for. Each module keeps its own topic list and lecturer order.",
                    actionLabel = "Add a module",
                    onAction = { showAddModule = true }
                )
            } else {
                state.modules.forEach { moduleWithTopics ->
                    ModuleCard(
                        moduleWithTopics = moduleWithTopics,
                        onClick = { onOpenModule(moduleWithTopics.module.id) },
                        onEdit = { editingModule = moduleWithTopics },
                        onRemove = { removingModule = moduleWithTopics }
                    )
                    Spacer(Modifier.height(Spacing.md))
                }
                PrimaryActionButton(
                    text = "Add another module",
                    onClick = { showAddModule = true }
                )
            }
    
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = "Open a module to add topics manually or paste a lecturer outline. Lecturer order is preserved and you can rename or remove entries later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xl))
        }
    }

    if (showAddModule) {
        AddModuleDialog(
            onDismiss = { showAddModule = false },
            onConfirm = { name, code ->
                onAddModule(name, code)
                showAddModule = false
            }
        )
    }
    if (showEditSemester && state.semester != null) {
        EditSemesterDialog(
            initialYear = state.semester.academicYear,
            initialLabel = state.semester.label,
            onDismiss = { showEditSemester = false },
            onConfirm = { year, label ->
                onEditSemester(year, label)
                showEditSemester = false
            }
        )
    }
    if (editingModule != null) {
        EditModuleDialog(
            module = editingModule!!.module,
            onDismiss = { editingModule = null },
            onConfirm = { name, code ->
                onRenameModule(editingModule!!.module.id, name, code)
                editingModule = null
            }
        )
    }
    if (removingModule != null) {
        ConfirmRemoveDialog(
            title = "Remove ${removingModule!!.module.name}?",
            body = "The module is hidden from your plan. Its topics, topic history and session records are kept on this phone.",
            confirmLabel = "Remove module",
            onDismiss = { removingModule = null },
            onConfirm = {
                onRemoveModule(removingModule!!.module.id)
                removingModule = null
            }
        )
    }
}

@Composable
fun ModuleDetailScreen(
    state: AppUiState,
    moduleId: UUID,
    onAddTopic: (title: String) -> Unit,
    onImportOutline: (String) -> Unit,
    onRenameTopic: (UUID, String) -> Unit,
    onRemoveTopic: (UUID) -> Unit
) {
    var showAddTopic by remember { mutableStateOf(false) }
    var showImportOutline by remember { mutableStateOf(false) }
    var editingTopic by remember { mutableStateOf<Topic?>(null) }
    var removingTopic by remember { mutableStateOf<Topic?>(null) }
    val moduleWithTopics = state.modules.firstOrNull { it.module.id == moduleId }

    if (moduleWithTopics == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg)
        ) {
            EmptyState(
                title = "Module not found",
                body = "This module is no longer in your local data."
            )
        }
    } else {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.sl_color_accent_primary)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Row(modifier = Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colorResource(R.color.sl_color_media)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = Color.White, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = moduleWithTopics.module.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = moduleWithTopics.module.code ?: "Your module",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "${moduleWithTopics.topics.size} topics · ${outlineStatusLabel(moduleWithTopics.module.outlineStatus)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
        SecondaryActionButton(
            text = "Import outline",
            onClick = { showImportOutline = true }
        )

        Spacer(Modifier.height(Spacing.xxl))
        SectionHeading(
            title = "Topics",
            supporting = "Lecturer order is preserved. This is the sequence used for coverage."
        )
        Spacer(Modifier.height(Spacing.md))

        if (moduleWithTopics.topics.isEmpty()) {
            EmptyState(
                title = "No topics yet",
                body = "Add the topics you know, in lecturer order. Leave out anything you have not received.",
                actionLabel = "Add a topic",
                onAction = { showAddTopic = true }
            )
        } else {
            moduleWithTopics.topics.forEachIndexed { index, topic ->
                TopicRow(
                    index = index,
                    topic = topic,
                    onEdit = { editingTopic = topic },
                    onRemove = { removingTopic = topic }
                )
                if (index != moduleWithTopics.topics.lastIndex) {
                    Spacer(Modifier.height(Spacing.sm))
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            PrimaryActionButton(
                text = "Add another topic",
                onClick = { showAddTopic = true }
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = "Topics added here are marked as information you provided. Nothing is imported or generated.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.xl))
    }
    }

    if (showAddTopic) {
        AddTopicDialog(
            onDismiss = { showAddTopic = false },
            onConfirm = { title ->
                onAddTopic(title)
                showAddTopic = false
            }
        )
    }
    if (showImportOutline) {
        ImportOutlineDialog(
            onDismiss = { showImportOutline = false },
            onConfirm = { text ->
                onImportOutline(text)
                showImportOutline = false
            }
        )
    }
    if (editingTopic != null) {
        EditTopicDialog(
            initialTitle = editingTopic!!.title,
            onDismiss = { editingTopic = null },
            onConfirm = { title ->
                onRenameTopic(editingTopic!!.id, title)
                editingTopic = null
            }
        )
    }
    if (removingTopic != null) {
        ConfirmRemoveDialog(
            title = "Remove ${removingTopic!!.title}?",
            body = "The topic is marked as removed and hidden from your plan. Past learning records and answers are kept on this phone.",
            confirmLabel = "Remove topic",
            onDismiss = { removingTopic = null },
            onConfirm = {
                onRemoveTopic(removingTopic!!.id)
                removingTopic = null
            }
        )
    }
}

@Composable
private fun ModuleCard(
    moduleWithTopics: ModuleWithTopics,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    StudyLockCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = moduleWithTopics.module.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = moduleWithTopics.module.code ?: "No module code",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(text = "${moduleWithTopics.topics.size} topics")
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = outlineStatusLabel(moduleWithTopics.module.outlineStatus),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.sm))
        Row {
            TextButton(onClick = onEdit) { Text("Edit") }
            TextButton(onClick = onRemove) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun TopicRow(
    index: Int,
    topic: Topic,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colorResource(R.color.sl_color_accent_primary)),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${index + 1}. ${topic.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (topic.sourceStatus == "RECEIVED") {
                        "Added by you"
                    } else {
                        "Source not provided"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onRemove) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AddModuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add module") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Module name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Module code (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        error = "Enter a module name."
                    } else {
                        onConfirm(name.trim(), code.trim().ifBlank { null })
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddTopicDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add topic") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Topic title") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "It will be added at the end of the lecturer sequence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isBlank()) {
                        error = "Enter a topic title."
                    } else {
                        onConfirm(title.trim())
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ImportOutlineDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val titles = parseOutline(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import outline") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Topic titles, one per line") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = if (titles.isEmpty()) {
                        "Paste the lecturer outline. Numbering and bullets are removed."
                    } else {
                        "${titles.size} topic(s) will be added in this order. Existing titles are skipped."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = titles.isNotEmpty(),
                onClick = { onConfirm(text) }
            ) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditTopicDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename topic") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Topic title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onConfirm(title.trim()) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditSemesterDialog(
    initialYear: String,
    initialLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var year by remember { mutableStateOf(initialYear) }
    var label by remember { mutableStateOf(initialLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit semester") },
        text = {
            Column {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Academic year") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Semester label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = year.isNotBlank() && label.isNotBlank(),
                onClick = { onConfirm(year.trim(), label.trim()) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditModuleDialog(
    module: Module,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf(module.name) }
    var code by remember { mutableStateOf(module.code.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit module") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Module name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Module code (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim(), code.trim().ifBlank { null }) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ConfirmRemoveDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun outlineStatusLabel(status: String): String = when (status) {
    "NOT_PROVIDED" -> "No outline yet"
    "ESTIMATED" -> "Outline estimated"
    "RECEIVED" -> "Outline added"
    "VERIFIED" -> "Outline verified"
    "SUPERSEDED" -> "Outline superseded"
    "CONFLICTING" -> "Outline has conflicts"
    else -> "Outline status unknown"
}
