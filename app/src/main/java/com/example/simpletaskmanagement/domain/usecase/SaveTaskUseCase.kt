package com.example.simpletaskmanagement.domain.usecase

import com.example.simpletaskmanagement.domain.model.Task
import com.example.simpletaskmanagement.domain.repository.TaskRepository
import javax.inject.Inject

class SaveTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(task: Task) {
        val trimmedTitle = task.title.trim()
        require(trimmedTitle.isNotBlank()) { "Task title cannot be empty" }
        taskRepository.saveTask(task.copy(title = trimmedTitle))
    }
}
