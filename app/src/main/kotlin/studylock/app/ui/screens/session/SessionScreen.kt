package studylock.app.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import studylock.app.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import studylock.app.AppUiState
import studylock.app.SessionTimerService
import studylock.app.asClockString
import studylock.app.remainingSeconds
import studylock.app.ui.components.EmptyState
import studylock.app.ui.components.PrimaryActionButton
import studylock.app.ui.components.SecondaryActionButton
import studylock.app.ui.components.StatusChip
import studylock.app.ui.theme.Spacing
import studylock.focus.SessionState

@Composable
fun SessionScreen(
    state: AppUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onLostFocus: () -> Unit,
    onEndEarly: () -> Unit,
    onOpenSummary: () -> Unit,
    clockTick: StateFlow<Pair<Long, Long>>
) {
    // Localised one-second tick: only the countdown recomposes, not the app.
    val tick by clockTick.collectAsStateWithLifecycle()
    var showAttentionReset by remember { mutableStateOf(false) }
    var exitStep by remember { mutableIntStateOf(0) }
    var emergencyStep by remember { mutableIntStateOf(0) }
    var puzzleAnswer by remember { mutableStateOf("") }
    var puzzleError by remember { mutableStateOf(false) }
    val session = state.activeSession
    // Deterministic per session: no random source, no stored puzzle state.
    val puzzle = remember(session?.id) {
        val seed = abs(session?.id?.hashCode() ?: 0)
        (seed % 7 + 3) to (seed % 5 + 3)
    }
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(session?.id, session?.state) {
        when (session?.state) {
            SessionState.ACTIVE -> {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                SessionTimerService.start(context)
            }
            SessionState.PAUSED -> SessionTimerService.stop(context)
            else -> Unit
        }
    }

    LaunchedEffect(state.lastSummary) {
        if (state.lastSummary != null) onOpenSummary()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (session == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.Center
            ) {
                EmptyState(
                    title = "No active session",
                    body = "Start a session from Today when you are ready."
                )
            }
        } else {

            val module = state.module(session.moduleId)
            val topic = state.topic(session.moduleId, session.topicId)
            val nowMillis = if (tick.first == 0L) state.timestampMillis else tick.first
            val monotonicMillis = if (tick.second == 0L) state.monotonicMillis else tick.second
            val remaining = session.remainingSeconds(nowMillis, monotonicMillis)
            val elapsed = session.targetMinutes * 60L - remaining
            val progress = if (session.targetMinutes > 0) {
                (elapsed.toFloat() / (session.targetMinutes * 60L)).coerceIn(0f, 1f)
            } else {
                0f
            }
    
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusChip(
                    text = if (session.state == SessionState.PAUSED) "Paused" else "In progress",
                    emphasized = true
                )
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    text = module?.code ?: module?.name ?: "Study session",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = topic?.title ?: "Continue your session",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (module != null && module.code != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = module.name,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
    
                Spacer(Modifier.height(Spacing.xl))
                Card(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.sl_color_media)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .offset(x = 24.dp, y = 28.dp)
                                .rotate(-16f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(colorResource(R.color.sl_color_accent_primary))
                        )
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .offset(x = 276.dp, y = 34.dp)
                                .clip(CircleShape)
                                .background(colorResource(R.color.sl_color_hero_amber))
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(Spacing.lg))
                            Surface(shape = CircleShape, color = Color.White) {
                                Box(
                                    modifier = Modifier.size(68.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "▶",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = colorResource(R.color.sl_color_media)
                                    )
                                }
                            }
                            Spacer(Modifier.height(Spacing.md))
                            Text(
                                text = remaining.asClockString(),
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White
                            )
                            Text(
                                text = "remaining of ${session.targetMinutes} minutes",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Spacer(Modifier.height(Spacing.md))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
    
                if (session.state == SessionState.PAUSED) {
                    PrimaryActionButton(text = "Resume session", onClick = onResume)
                } else {
                    PrimaryActionButton(text = "Pause", onClick = onPause)
                }
                Spacer(Modifier.height(Spacing.sm))
                SecondaryActionButton(
                    text = "I lost focus",
                    enabled = session.state == SessionState.ACTIVE,
                    onClick = {
                        onLostFocus()
                        showAttentionReset = true
                    }
                )
                Spacer(Modifier.height(Spacing.md))
                TextButton(
                    onClick = { exitStep = 1 },
                    modifier = Modifier.height(Spacing.touchTarget)
                ) {
                    Text(
                        text = "End session early",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { emergencyStep = 1 },
                    modifier = Modifier.height(Spacing.touchTarget)
                ) {
                    Text(
                        text = "Emergency exit",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.leftAppCount > 0 && session.state == SessionState.ACTIVE) {
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = "You stepped away from StudyLock ${state.leftAppCount} time(s). " +
                            "Your progress is saved. Return when you are ready.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
    
                Spacer(Modifier.height(Spacing.xl))
                Text(
                    text = "The timer records time spent, not proof of learning. A notification shows the countdown while you are away. Emergency calls and essential phone functions stay available.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }

    if (showAttentionReset) {
        AlertDialog(
            onDismissRequest = { showAttentionReset = false },
            title = { Text("Let's return to the next step") },
            text = {
                Text(
                    "Your progress is saved. Nothing was restarted. Take a breath, then resume when you are ready."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAttentionReset = false
                        onResume()
                    }
                ) { Text("Resume now") }
            },
            dismissButton = {
                TextButton(onClick = { showAttentionReset = false }) { Text("Stay paused") }
            }
        )
    }

    if (exitStep > 0) {
        AlertDialog(
            onDismissRequest = {
                exitStep = 0
                puzzleAnswer = ""
                puzzleError = false
            },
            title = {
                Text(
                    when (exitStep) {
                        1 -> "End session early?"
                        2 -> "End now?"
                        else -> "One quick check"
                    }
                )
            },
            text = {
                when (exitStep) {
                    1 -> Text("Your recorded time is kept and nothing is lost. You can continue on another study day.")
                    2 -> Text("This ends the session and saves your elapsed time. One short check comes next.")
                    else -> Column {
                        Text("What is ${puzzle.first} + ${puzzle.second}?")
                        Spacer(Modifier.height(Spacing.sm))
                        OutlinedTextField(
                            value = puzzleAnswer,
                            onValueChange = {
                                puzzleAnswer = it
                                puzzleError = false
                            },
                            label = { Text("Answer") },
                            singleLine = true,
                            isError = puzzleError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        if (puzzleError) {
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = "Not quite. Try again.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = "This check is only for ending normally. Use Emergency exit if you need to leave right away.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (exitStep) {
                            1 -> exitStep = 2
                            2 -> exitStep = 3
                            else -> {
                                if (puzzleAnswer.trim().toIntOrNull() == puzzle.first + puzzle.second) {
                                    exitStep = 0
                                    puzzleAnswer = ""
                                    puzzleError = false
                                    onEndEarly()
                                } else {
                                    puzzleError = true
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        when (exitStep) {
                            1 -> "End early"
                            2 -> "Continue"
                            else -> "Confirm and end"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    exitStep = 0
                    puzzleAnswer = ""
                    puzzleError = false
                }) {
                    Text(if (exitStep == 1) "Keep going" else "Cancel")
                }
            }
        )
    }

    if (emergencyStep > 0) {
        AlertDialog(
            onDismissRequest = { emergencyStep = 0 },
            title = {
                Text(if (emergencyStep == 1) "Emergency exit?" else "Leave session now?")
            },
            text = {
                Text(
                    "No puzzle and no explanation needed. Your recorded time is kept. " +
                        "Emergency calls and essential phone functions remain available."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (emergencyStep == 1) {
                            emergencyStep = 2
                        } else {
                            emergencyStep = 0
                            onEndEarly()
                        }
                    }
                ) {
                    Text(if (emergencyStep == 1) "Continue" else "Exit now")
                }
            },
            dismissButton = {
                TextButton(onClick = { emergencyStep = 0 }) { Text("Cancel") }
            }
        )
    }
}
