package com.example.simpletaskmanagement.domain.usecase

import com.example.simpletaskmanagement.domain.model.Task
import com.example.simpletaskmanagement.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTaskByIdUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(taskId: String): Flow<Task?> {
        return taskRepository.getTaskById(taskId)
    }
}
