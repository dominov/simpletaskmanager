package com.example.simpletaskmanagement.domain.usecase

import com.example.simpletaskmanagement.domain.model.CategoryWithTaskCount
import com.example.simpletaskmanagement.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(): Flow<List<CategoryWithTaskCount>> {
        return taskRepository.getCategoriesWithCount()
    }
}
