package com.example.simpletaskmanagement.presentation.taskform

import com.example.simpletaskmanagement.domain.model.Category

data class TaskFormState(
    val taskId: String? = null,
    val title: String = "",
    val description: String = "",
    val selectedCategoryId: String = "",
    val dueDate: Long? = null,
    val dueHour: Int? = null,
    val dueMinute: Int? = null,
    val hasTime: Boolean = false,
    val isCompleted: Boolean = false,
    val repeatRule: String? = null,
    val isBatchMode: Boolean = false,
    val availableCategories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)
