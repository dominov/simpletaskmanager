package com.example.simpletaskmanagement.presentation.taskform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simpletaskmanagement.domain.model.Category
import com.example.simpletaskmanagement.domain.model.Task
import com.example.simpletaskmanagement.domain.repository.TaskRepository
import com.example.simpletaskmanagement.domain.usecase.CreateCategoryUseCase
import com.example.simpletaskmanagement.domain.usecase.DeleteTaskUseCase
import com.example.simpletaskmanagement.domain.usecase.GetTaskByIdUseCase
import com.example.simpletaskmanagement.domain.usecase.SaveTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    private val saveTaskUseCase: SaveTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskFormState())
    val uiState: StateFlow<TaskFormState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            taskRepository.getAllCategories().collect { categories ->
                _uiState.update { current ->
                    val defaultCat = categories.firstOrNull { it.isDefault } ?: categories.firstOrNull()
                    val selected = if (current.selectedCategoryId.isBlank()) {
                        defaultCat?.id ?: ""
                    } else {
                        current.selectedCategoryId
                    }
                    current.copy(
                        availableCategories = categories,
                        selectedCategoryId = selected
                    )
                }
            }
        }
    }

    fun loadTask(taskId: String?) {
        if (taskId.isNullOrBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, taskId = taskId) }
            val task = getTaskByIdUseCase(taskId).firstOrNull()
            if (task != null) {
                var hour: Int? = null
                var minute: Int? = null
                if (task.hasTime && task.dueDate != null) {
                    val cal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
                    hour = cal.get(Calendar.HOUR_OF_DAY)
                    minute = cal.get(Calendar.MINUTE)
                }

                _uiState.update {
                    it.copy(
                        taskId = task.id,
                        title = task.title,
                        description = task.description ?: "",
                        selectedCategoryId = task.categoryId,
                        dueDate = task.dueDate,
                        dueHour = hour,
                        dueMinute = minute,
                        hasTime = task.hasTime,
                        isCompleted = task.isCompleted,
                        repeatRule = task.repeatRule,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, errorMessage = null) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateCategory(categoryId: String) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun updateDueDate(dateMillis: Long?) {
        _uiState.update { current ->
            if (dateMillis == null) {
                current.copy(dueDate = null, dueHour = null, dueMinute = null, hasTime = false)
            } else {
                val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                if (current.hasTime && current.dueHour != null && current.dueMinute != null) {
                    cal.set(Calendar.HOUR_OF_DAY, current.dueHour)
                    cal.set(Calendar.MINUTE, current.dueMinute)
                } else {
                    cal.set(Calendar.HOUR_OF_DAY, 9)
                    cal.set(Calendar.MINUTE, 0)
                }
                current.copy(dueDate = cal.timeInMillis)
            }
        }
    }

    fun updateDueTime(hour: Int, minute: Int) {
        _uiState.update { current ->
            val cal = Calendar.getInstance().apply {
                if (current.dueDate != null) {
                    timeInMillis = current.dueDate
                }
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            current.copy(
                dueDate = cal.timeInMillis,
                dueHour = hour,
                dueMinute = minute,
                hasTime = true
            )
        }
    }

    fun clearDueTime() {
        _uiState.update { current ->
            current.copy(dueHour = null, dueMinute = null, hasTime = false)
        }
    }

    fun toggleCompleted(completed: Boolean) {
        _uiState.update { it.copy(isCompleted = completed) }
    }

    fun updateRepeatRule(rule: String?) {
        _uiState.update { it.copy(repeatRule = rule) }
    }

    fun toggleBatchMode(enabled: Boolean) {
        _uiState.update { it.copy(isBatchMode = enabled) }
    }

    fun onVoiceInput(recognizedText: String) {
        if (recognizedText.isNotBlank()) {
            _uiState.update { current ->
                val newTitle = if (current.title.isBlank()) {
                    recognizedText
                } else {
                    "${current.title} $recognizedText"
                }
                current.copy(title = newTitle, errorMessage = null)
            }
        }
    }

    fun createCategory(name: String) {
        viewModelScope.launch {
            val category = createCategoryUseCase(name)
            _uiState.update { it.copy(selectedCategoryId = category.id) }
        }
    }

    fun saveTask() {
        val current = _uiState.value
        val trimmedTitle = current.title.trim()
        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El título de la tarea no puede estar vacío") }
            return
        }

        viewModelScope.launch {
            val task = Task(
                id = current.taskId ?: UUID.randomUUID().toString(),
                categoryId = current.selectedCategoryId,
                title = trimmedTitle,
                description = current.description.ifBlank { null },
                dueDate = current.dueDate,
                hasTime = current.hasTime,
                isCompleted = current.isCompleted,
                repeatRule = current.repeatRule
            )
            saveTaskUseCase(task)

            if (current.isBatchMode && current.taskId == null) {
                // Batch mode: reset title/description, keep category & date/time for fast entry!
                _uiState.update {
                    it.copy(
                        title = "",
                        description = "",
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update { it.copy(isSaved = true) }
            }
        }
    }

    fun deleteTask() {
        val id = _uiState.value.taskId ?: return
        viewModelScope.launch {
            deleteTaskUseCase(id)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}
