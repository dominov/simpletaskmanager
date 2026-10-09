package com.example.simpletaskmanagement.domain.usecase

import com.example.simpletaskmanagement.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleTaskCompletionUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String) {
        taskRepository.toggleTaskCompletion(taskId)
    }
}
