package com.example.simpletaskmanagement.presentation.main

import com.example.simpletaskmanagement.domain.model.Category
import com.example.simpletaskmanagement.domain.model.CategoryWithTaskCount
import com.example.simpletaskmanagement.domain.model.Task

data class MainUiState(
    val categories: List<CategoryWithTaskCount> = emptyList(),
    val selectedCategory: Category? = null,
    val groupedTasks: Map<String, List<Task>> = emptyMap(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isDarkMode: Boolean = false,
    val isLoading: Boolean = false,
    val userEmail: String? = null,
    val isAuthenticated: Boolean = false,
    val showAddCategoryDialog: Boolean = false
) {
    val currentTitle: String
        get() = selectedCategory?.name ?: "Todas las tareas"
}
