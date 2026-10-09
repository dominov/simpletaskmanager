package com.example.simpletaskmanagement.domain.repository

import com.example.simpletaskmanagement.domain.model.Category
import com.example.simpletaskmanagement.domain.model.CategoryWithTaskCount
import com.example.simpletaskmanagement.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTasksByCategory(categoryId: String): Flow<List<Task>>
    fun getTaskById(taskId: String): Flow<Task?>
    suspend fun saveTask(task: Task)
    suspend fun deleteTask(taskId: String)
    suspend fun toggleTaskCompletion(taskId: String)

    fun getCategoriesWithCount(): Flow<List<CategoryWithTaskCount>>
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(categoryId: String): Category?
    suspend fun saveCategory(category: Category)
    suspend fun deleteCategory(categoryId: String)

    suspend fun syncNow()
}
