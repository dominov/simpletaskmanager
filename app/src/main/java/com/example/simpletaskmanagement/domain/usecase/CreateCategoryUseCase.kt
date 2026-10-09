package com.example.simpletaskmanagement.domain.usecase

import com.example.simpletaskmanagement.domain.model.Category
import com.example.simpletaskmanagement.domain.model.SyncState
import com.example.simpletaskmanagement.domain.repository.TaskRepository
import java.util.UUID
import javax.inject.Inject

class CreateCategoryUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(name: String): Category {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Category name cannot be empty" }
        val category = Category(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            isDefault = false,
            syncState = SyncState.PENDING_CREATE
        )
        taskRepository.saveCategory(category)
        return category
    }
}
