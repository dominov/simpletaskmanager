package com.example.simpletaskmanagement.presentation.main

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simpletaskmanagement.data.auth.AuthManager
import com.example.simpletaskmanagement.data.auth.AuthorizationOutcome
import com.example.simpletaskmanagement.data.auth.GoogleAuthorizer
import com.example.simpletaskmanagement.domain.model.Category
import com.example.simpletaskmanagement.domain.usecase.CreateCategoryUseCase
import com.example.simpletaskmanagement.domain.usecase.GetCategoriesUseCase
import com.example.simpletaskmanagement.domain.usecase.GetTasksUseCase
import com.example.simpletaskmanagement.domain.usecase.SyncTasksUseCase
import com.example.simpletaskmanagement.domain.usecase.ToggleTaskCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val syncTasksUseCase: SyncTasksUseCase,
    private val authManager: AuthManager,
    private val googleAuthorizer: GoogleAuthorizer
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _isDarkMode = MutableStateFlow(false)
    private val _showAddCategoryDialog = MutableStateFlow(false)

    // FlatMap latest tasks based on selected category and search query
    private val _tasksFlow = combine(_selectedCategory, _searchQuery) { category, query ->
        category to query
    }.flatMapLatest { (category, query) ->
        getTasksUseCase(categoryId = category?.id, searchQuery = query)
    }

    private data class FilterState(
        val selectedCategory: Category?,
        val searchQuery: String,
        val isSearchActive: Boolean
    )

    private val _filterState = combine(
        _selectedCategory,
        _searchQuery,
        _isSearchActive
    ) { category, query, searchActive ->
        FilterState(category, query, searchActive)
    }

    private data class PreferenceState(
        val isDarkMode: Boolean,
        val showAddCategoryDialog: Boolean,
        val isAuthenticated: Boolean
    )

    private val _preferenceState = combine(
        _isDarkMode,
        _showAddCategoryDialog,
        authManager.authState
    ) { darkMode, showDialog, isAuth ->
        PreferenceState(darkMode, showDialog, isAuth)
    }

    val uiState: StateFlow<MainUiState> = combine(
        getCategoriesUseCase(),
        _tasksFlow,
        _filterState,
        _preferenceState
    ) { categoriesWithCount, groupedTasks, filters, prefs ->
        MainUiState(
            categories = categoriesWithCount,
            selectedCategory = filters.selectedCategory,
            groupedTasks = groupedTasks,
            searchQuery = filters.searchQuery,
            isSearchActive = filters.isSearchActive,
            isDarkMode = prefs.isDarkMode,
            showAddCategoryDialog = prefs.showAddCategoryDialog,
            userEmail = authManager.getUserEmail(),
            isAuthenticated = prefs.isAuthenticated,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun onCategorySelected(category: Category?) {
        _selectedCategory.value = category
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        val nextState = !_isSearchActive.value
        _isSearchActive.value = nextState
        if (!nextState) {
            _searchQuery.value = ""
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            toggleTaskCompletionUseCase(taskId)
        }
    }

    fun setShowAddCategoryDialog(show: Boolean) {
        _showAddCategoryDialog.value = show
    }

    fun createCategory(name: String) {
        viewModelScope.launch {
            val newCategory = createCategoryUseCase(name)
            _selectedCategory.value = newCategory
            _showAddCategoryDialog.value = false
        }
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun syncNow() {
        viewModelScope.launch {
            syncTasksUseCase()
        }
    }

    fun signIn(onResolution: (PendingIntent) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            handleOutcome(googleAuthorizer.authorize(), onResolution, onError)
        }
    }

    fun onAuthResult(data: Intent?, onError: (String) -> Unit) {
        handleOutcome(googleAuthorizer.handleResultIntent(data), { }, onError)
    }

    fun signOut() {
        authManager.clearAuth()
    }

    private fun handleOutcome(
        outcome: AuthorizationOutcome,
        onResolution: (PendingIntent) -> Unit,
        onError: (String) -> Unit
    ) {
        when (outcome) {
            is AuthorizationOutcome.Success -> syncNow()
            is AuthorizationOutcome.NeedsResolution -> onResolution(outcome.pendingIntent)
            is AuthorizationOutcome.Failure ->
                onError(outcome.error.localizedMessage ?: "No se pudo iniciar sesión con Google")
        }
    }
}
