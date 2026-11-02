package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Grading
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Grading
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AddStudentsDialog
import com.example.ui.components.DatabaseStorageDialog
import com.example.ui.screens.GradeAnalysisScreen
import com.example.ui.screens.HistoricalProgressScreen
import com.example.ui.screens.ScoreFormScreen
import com.example.ui.screens.ScoreListScreen
import com.example.ui.theme.EvaluTrackTheme
import com.example.ui.viewmodel.ScoreUiEvent
import com.example.ui.viewmodel.ScoreViewModel
import kotlinx.coroutines.flow.collectLatest

enum class AppDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    RECORD(
        title = "Record Score",
        selectedIcon = Icons.Filled.EditNote,
        unselectedIcon = Icons.Outlined.EditNote,
        testTag = "nav_record_score"
    ),
    GRADEBOOK(
        title = "Gradebook",
        selectedIcon = Icons.Filled.Grading,
        unselectedIcon = Icons.Outlined.Grading,
        testTag = "nav_gradebook"
    ),
    PROGRESS(
        title = "Progress",
        selectedIcon = Icons.Filled.Timeline,
        unselectedIcon = Icons.Outlined.Timeline,
        testTag = "nav_progress"
    ),
    ANALYSIS(
        title = "Analysis",
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
        testTag = "nav_analysis"
    )
}

class MainActivity : ComponentActivity() {

    private val viewModel: ScoreViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EvaluTrackTheme {
                var currentDestination by remember { mutableStateOf(AppDestination.RECORD) }
                var showAddStudentsDialog by remember { mutableStateOf(false) }
                var showDatabaseStorageDialog by remember { mutableStateOf(false) }
                val snackbarHostState = remember { SnackbarHostState() }

                // Listen for UI events (e.g. snackbar messages)
                LaunchedEffect(viewModel) {
                    viewModel.uiEvents.collectLatest { event ->
                        when (event) {
                            is ScoreUiEvent.ShowSnackbar -> {
                                val result = snackbarHostState.showSnackbar(
                                    message = event.message,
                                    actionLabel = event.actionLabel,
                                    duration = androidx.compose.material3.SnackbarDuration.Short
                                )
                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                    event.onAction?.invoke()
                                }
                            }
                            ScoreUiEvent.ScoreSaved -> {
                                // Keep on current or let caller navigate
                            }
                        }
                    }
                }

                // Handle system back navigation if not on RECORD
                BackHandler(enabled = currentDestination != AppDestination.RECORD) {
                    currentDestination = AppDestination.RECORD
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "EvaluTrack",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { showDatabaseStorageDialog = true },
                                    modifier = Modifier.testTag("top_bar_database_storage_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Database & Cloud Storage (Phone & Google Drive)"
                                    )
                                }
                                IconButton(
                                    onClick = { showAddStudentsDialog = true },
                                    modifier = Modifier.testTag("top_bar_add_students_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Enroll Students (Image/Document/Text)"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            AppDestination.entries.forEach { destination ->
                                val selected = currentDestination == destination
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDestination = destination },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                            contentDescription = destination.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = destination.title,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag(destination.testTag)
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentDestination) {
                            AppDestination.RECORD -> {
                                ScoreFormScreen(
                                    viewModel = viewModel,
                                    onSavedNavigateToList = {
                                        currentDestination = AppDestination.GRADEBOOK
                                    }
                                )
                            }
                            AppDestination.GRADEBOOK -> {
                                ScoreListScreen(
                                    viewModel = viewModel,
                                    onNavigateToRecord = {
                                        currentDestination = AppDestination.RECORD
                                    },
                                    onNavigateToStudentProgress = { _ ->
                                        currentDestination = AppDestination.PROGRESS
                                    }
                                )
                            }
                            AppDestination.PROGRESS -> {
                                HistoricalProgressScreen(
                                    viewModel = viewModel
                                )
                            }
                            AppDestination.ANALYSIS -> {
                                GradeAnalysisScreen(
                                    viewModel = viewModel,
                                    onSelectStudent = { _ ->
                                        currentDestination = AppDestination.PROGRESS
                                    }
                                )
                            }
                        }
                    }
                }

                if (showAddStudentsDialog) {
                    AddStudentsDialog(
                        viewModel = viewModel,
                        onDismiss = { showAddStudentsDialog = false }
                    )
                }

                if (showDatabaseStorageDialog) {
                    DatabaseStorageDialog(
                        viewModel = viewModel,
                        onDismiss = { showDatabaseStorageDialog = false }
                    )
                }
            }
        }
    }
}
