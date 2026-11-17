package studylock.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.util.UUID
import studylock.app.AppContainer
import studylock.app.AppUiState
import studylock.app.AppViewModel
import studylock.app.AppViewModelFactory
import studylock.app.ui.screens.academics.AcademicsScreen
import studylock.app.ui.screens.academics.ModuleDetailScreen
import studylock.app.ui.screens.exams.ExamsScreen
import studylock.app.ui.screens.onboarding.OnboardingScreen
import studylock.app.ui.screens.progress.ProgressScreen
import studylock.app.ui.screens.session.SessionScreen
import studylock.app.ui.screens.session.SessionSummaryScreen
import studylock.app.ui.screens.settings.SettingsScreen
import studylock.app.ui.screens.today.TodayScreen
import studylock.app.ui.theme.Spacing

private data class TopLevelTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val topLevelTabs = listOf(
    TopLevelTab(Routes.TODAY, "Today", Icons.Filled.Home),
    TopLevelTab(Routes.ACADEMICS, "Academics", Icons.AutoMirrored.Filled.List),
    TopLevelTab(Routes.EXAMS, "Exams", Icons.Filled.DateRange),
    TopLevelTab(Routes.PROGRESS, "Progress", Icons.Filled.CheckCircle),
    TopLevelTab(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

private val topLevelRoutes = topLevelTabs.map { it.route }.toSet()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyLockApp(container: AppContainer) {
    val factory = remember(container) { AppViewModelFactory(container) }
    val viewModel: AppViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    when {
        state.isLoading -> LoadingScreen()
        !state.onboardingCompleted && state.semester == null -> OnboardingScreen(
            existingSemester = state.semester,
            initialAvailableDays = state.initialAvailableDays(),
            busy = state.busy,
            onFinish = viewModel::finishOnboarding,
            onSkip = viewModel::skipOnboarding
        )
        else -> MainScaffold(
            state = state,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    state: AppUiState,
    viewModel: AppViewModel,
    snackbarHostState: SnackbarHostState
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val moduleId = backStackEntry?.arguments?.getString("moduleId")
        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToToday: () -> Unit = {
        viewModel.dismissSummary()
        navController.navigate(Routes.TODAY) {
            popUpTo(Routes.TODAY) { inclusive = true }
            launchSingleTop = true
        }
    }

    val title = when (currentRoute) {
        Routes.TODAY -> "Today"
        Routes.ACADEMICS -> "Academics"
        Routes.MODULE_DETAIL -> state.module(moduleId)?.name ?: "Module"
        Routes.EXAMS -> "Exams"
        Routes.PROGRESS -> "Progress"
        Routes.SETTINGS -> "Settings"
        Routes.SUMMARY -> "Session summary"
        Routes.ONBOARDING -> "Setup"
        else -> "StudyLock"
    }

    val onBack: (() -> Unit)? = when (currentRoute) {
        Routes.MODULE_DETAIL, Routes.ONBOARDING -> {
            { navController.popBackStack() }
        }
        Routes.SUMMARY -> {
            { navigateToToday() }
        }
        else -> null
    }

    val showBottomBar = currentRoute in topLevelRoutes
    val showTopBar = currentRoute != Routes.SESSION

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .padding(horizontal = Spacing.sm)
                                    .size(44.dp)
                                    .clickable(onClick = onBack)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 14.dp,
                        modifier = Modifier.fillMaxWidth().height(72.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            topLevelTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { navigateToTab(tab.route) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else Color.Transparent
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.label,
                                            tint = if (selected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.TODAY) {
                TodayScreen(
                    state = state,
                    onStartSession = {
                        viewModel.startSession()
                        navController.navigate(Routes.SESSION)
                    },
                    onResumeSession = { navController.navigate(Routes.SESSION) },
                    onOpenSetup = { navController.navigate(Routes.ONBOARDING) },
                    onOpenAcademics = { navigateToTab(Routes.ACADEMICS) },
                    onOpenSummary = { navController.navigate(Routes.SUMMARY) },
                    clockTick = viewModel.clockTick
                )
            }

            composable(Routes.ACADEMICS) {
                AcademicsScreen(
                    state = state,
                    onAddModule = viewModel::addModule,
                    onOpenModule = { id ->
                        navController.navigate(Routes.moduleDetail(id.toString()))
                    },
                    onOpenSetup = { navController.navigate(Routes.ONBOARDING) },
                    onConfirmMatch = viewModel::confirmTopicMatch,
                    onRenameModule = viewModel::renameModule,
                    onRemoveModule = viewModel::removeModule,
                    onEditSemester = viewModel::editSemester
                )
            }

            composable(
                route = Routes.MODULE_DETAIL,
                arguments = listOf(navArgument("moduleId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("moduleId")
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                if (id == null) {
                    MissingRouteScreen()
                } else {
                    ModuleDetailScreen(
                        state = state,
                        moduleId = id,
                        onAddTopic = { title -> viewModel.addTopic(id, title) },
                        onImportOutline = { text -> viewModel.importOutline(id, text) },
                        onRenameTopic = viewModel::renameTopic,
                        onRemoveTopic = viewModel::removeTopic
                    )
                }
            }

            composable(Routes.EXAMS) {
                ExamsScreen(
                    state = state,
                    onAddExam = viewModel::addExam,
                    onOpenAcademics = { navigateToTab(Routes.ACADEMICS) },
                    onDeleteExam = viewModel::deleteExam
                )
            }

            composable(Routes.PROGRESS) {
                ProgressScreen(
                    state = state,
                    onOpenSummary = { session ->
                        viewModel.openSummary(session.id)
                        navController.navigate(Routes.SUMMARY) { launchSingleTop = true }
                    }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    state = state,
                    onSetAvailabilityDay = viewModel::setAvailabilityDay,
                    onCreateBackup = viewModel::createBackup,
                    onRestoreBackup = viewModel::restoreBackup,
                    onOpenSetup = { navController.navigate(Routes.ONBOARDING) },
                    onOpenUsageAccess = viewModel::openUsageAccessSettings,
                    onOpenNotificationPolicy = viewModel::openNotificationPolicySettings,
                    onAcknowledgeInterruption = viewModel::acknowledgeEnforcementInterruption
                )
            }

            composable(Routes.SESSION) {
                SessionScreen(
                    state = state,
                    onPause = viewModel::pauseSession,
                    onResume = viewModel::resumeSession,
                    onLostFocus = { viewModel.pauseSession("lost_focus") },
                    onEndEarly = viewModel::endSessionEarly,
                    onOpenSummary = {
                        navController.navigate(Routes.SUMMARY) { launchSingleTop = true }
                    },
                    clockTick = viewModel.clockTick
                )
            }

            composable(Routes.SUMMARY) {
                BackHandler { navigateToToday() }
                SessionSummaryScreen(
                    state = state,
                    onSetLearningEvidence = viewModel::setLearningEvidence,
                    onRecordAssessment = { correct, total ->
                        state.lastSummary?.topicId?.let { topicId ->
                            viewModel.recordAssessment(topicId, correct, total)
                        }
                    },
                    onDone = navigateToToday
                )
            }

            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    existingSemester = state.semester,
                    initialAvailableDays = state.initialAvailableDays(),
                    busy = state.busy,
                    onFinish = { year, label, modules, days ->
                        viewModel.finishOnboarding(year, label, modules, days)
                        navController.navigate(Routes.TODAY) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onSkip = {
                        viewModel.skipOnboarding()
                        navController.navigate(Routes.TODAY) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MissingRouteScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "This item is no longer available.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun AppUiState.initialAvailableDays(): Set<Int> {
    val days = availability.filter { it.available }.map { it.dayOfWeek }.toSet()
    return days.ifEmpty { (1..5).toSet() }
}
