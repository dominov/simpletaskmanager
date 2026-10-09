package com.example.simpletaskmanagement.presentation.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.simpletaskmanagement.presentation.main.MainViewModel
import com.example.simpletaskmanagement.presentation.main.TaskListScreen
import com.example.simpletaskmanagement.presentation.main.components.AppDrawer
import com.example.simpletaskmanagement.presentation.settings.SettingsScreen
import com.example.simpletaskmanagement.presentation.taskform.TaskCreateScreen
import com.example.simpletaskmanagement.presentation.taskform.TaskEditScreen
import com.example.simpletaskmanagement.presentation.taskform.TaskFormViewModel
import com.example.simpletaskmanagement.ui.theme.SimpleTaskManagementTheme
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var authError by remember { mutableStateOf<String?>(null) }
    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        mainViewModel.onAuthResult(result.data) { authError = it }
    }

    SimpleTaskManagementTheme(darkTheme = mainUiState.isDarkMode) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    categories = mainUiState.categories,
                    selectedCategory = mainUiState.selectedCategory,
                    isDarkMode = mainUiState.isDarkMode,
                    onCategorySelected = { category ->
                        mainViewModel.onCategorySelected(category)
                        scope.launch { drawerState.close() }
                    },
                    onAddCategoryClicked = {
                        mainViewModel.setShowAddCategoryDialog(true)
                        scope.launch { drawerState.close() }
                    },
                    onSettingsClicked = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(NavRoutes.SETTINGS)
                        }
                    },
                    onToggleTheme = { mainViewModel.toggleTheme() }
                )
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = NavRoutes.TASK_LIST,
                modifier = modifier
            ) {
                // Task List Screen
                composable(NavRoutes.TASK_LIST) {
                    TaskListScreen(
                        uiState = mainUiState,
                        onMenuClicked = { scope.launch { drawerState.open() } },
                        onSearchClicked = { mainViewModel.toggleSearch() },
                        onSearchQueryChanged = { mainViewModel.onSearchQueryChanged(it) },
                        onSyncClicked = { mainViewModel.syncNow() },
                        onToggleTaskCompletion = { mainViewModel.toggleTaskCompletion(it) },
                        onTaskClicked = { taskId ->
                            navController.navigate(NavRoutes.taskEdit(taskId))
                        },
                        onCreateTaskClicked = {
                            navController.navigate(NavRoutes.TASK_CREATE)
                        },
                        onAddCategoryConfirm = { name ->
                            mainViewModel.createCategory(name)
                        },
                        onAddCategoryDismiss = {
                            mainViewModel.setShowAddCategoryDialog(false)
                        }
                    )
                }

                // Create Task Screen
                composable(NavRoutes.TASK_CREATE) {
                    val formViewModel: TaskFormViewModel = hiltViewModel()
                    val formState by formViewModel.uiState.collectAsStateWithLifecycle()

                    TaskCreateScreen(
                        uiState = formState,
                        onTitleChanged = { formViewModel.updateTitle(it) },
                        onDescriptionChanged = { formViewModel.updateDescription(it) },
                        onCategoryChanged = { formViewModel.updateCategory(it) },
                        onDateChanged = { formViewModel.updateDueDate(it) },
                        onTimeChanged = { h, m -> formViewModel.updateDueTime(h, m) },
                        onClearTime = { formViewModel.clearDueTime() },
                        onRepeatRuleChanged = { formViewModel.updateRepeatRule(it) },
                        onBatchModeToggled = { formViewModel.toggleBatchMode(it) },
                        onVoiceInput = { formViewModel.onVoiceInput(it) },
                        onCreateCategory = { formViewModel.createCategory(it) },
                        onSaveTask = { formViewModel.saveTask() },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Edit Task Screen
                composable(
                    route = NavRoutes.TASK_EDIT,
                    arguments = listOf(navArgument("taskId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                    val formViewModel: TaskFormViewModel = hiltViewModel()
                    val formState by formViewModel.uiState.collectAsStateWithLifecycle()

                    TaskEditScreen(
                        taskId = taskId,
                        uiState = formState,
                        onLoadTask = { formViewModel.loadTask(it) },
                        onTitleChanged = { formViewModel.updateTitle(it) },
                        onDescriptionChanged = { formViewModel.updateDescription(it) },
                        onCategoryChanged = { formViewModel.updateCategory(it) },
                        onDateChanged = { formViewModel.updateDueDate(it) },
                        onTimeChanged = { h, m -> formViewModel.updateDueTime(h, m) },
                        onClearTime = { formViewModel.clearDueTime() },
                        onToggleCompleted = { formViewModel.toggleCompleted(it) },
                        onRepeatRuleChanged = { formViewModel.updateRepeatRule(it) },
                        onVoiceInput = { formViewModel.onVoiceInput(it) },
                        onSaveTask = { formViewModel.saveTask() },
                        onDeleteTask = { formViewModel.deleteTask() },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Settings Screen
                composable(NavRoutes.SETTINGS) {
                    SettingsScreen(
                        isAuthenticated = mainUiState.isAuthenticated,
                        userEmail = mainUiState.userEmail,
                        isDarkMode = mainUiState.isDarkMode,
                        onToggleTheme = { mainViewModel.toggleTheme() },
                        onSyncNow = { mainViewModel.syncNow() },
                        onSignIn = {
                            mainViewModel.signIn(
                                onResolution = {
                                    authLauncher.launch(IntentSenderRequest.Builder(it).build())
                                },
                                onError = { authError = it }
                            )
                        },
                        onSignOut = { mainViewModel.signOut() },
                        authError = authError,
                        onAuthErrorShown = { authError = null },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

