package com.example.simpletaskmanagement.data.repository

import com.example.simpletaskmanagement.data.local.dao.CategoryDao
import com.example.simpletaskmanagement.data.local.dao.TaskDao
import com.example.simpletaskmanagement.data.mapper.TaskMapper.toDomain
import com.example.simpletaskmanagement.data.mapper.TaskMapper.toEntity
import com.example.simpletaskmanagement.data.sync.SyncManager
import com.example.simpletaskmanagement.domain.model.Category
import com.example.simpletaskmanagement.domain.model.CategoryWithTaskCount
import com.example.simpletaskmanagement.domain.model.SyncState
import com.example.simpletaskmanagement.domain.model.Task
import com.example.simpletaskmanagement.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val syncManager: SyncManager
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return combine(
            taskDao.getAllTasks(),
            categoryDao.getAllCategories()
        ) { taskEntities, categoryEntities ->
            val categoryMap = categoryEntities.associate { it.localId to it.name }
            taskEntities.map { entity ->
                entity.toDomain(categoryName = categoryMap[entity.categoryId])
            }
        }
    }

    override fun getTasksByCategory(categoryId: String): Flow<List<Task>> {
        return combine(
            taskDao.getTasksByCategory(categoryId),
            categoryDao.getAllCategories()
        ) { taskEntities, categoryEntities ->
            val categoryMap = categoryEntities.associate { it.localId to it.name }
            taskEntities.map { entity ->
                entity.toDomain(categoryName = categoryMap[entity.categoryId])
            }
        }
    }

    override fun getTaskById(taskId: String): Flow<Task?> {
        return combine(
            taskDao.getTaskById(taskId),
            categoryDao.getAllCategories()
        ) { taskEntity, categoryEntities ->
            val categoryMap = categoryEntities.associate { it.localId to it.name }
            taskEntity?.let { entity ->
                entity.toDomain(categoryName = categoryMap[entity.categoryId])
            }
        }
    }

    override suspend fun saveTask(task: Task) {
        val existing = taskDao.getTaskByIdDirect(task.id)
        val syncState = if (existing == null) {
            SyncState.PENDING_CREATE
        } else if (existing.remoteId == null) {
            SyncState.PENDING_CREATE
        } else {
            SyncState.PENDING_UPDATE
        }

        val entityToSave = task.toEntity().copy(
            remoteId = task.remoteId ?: existing?.remoteId,
            syncState = syncState,
            lastUpdated = System.currentTimeMillis()
        )
        taskDao.insert(entityToSave)
        syncManager.scheduleOneTimeSync()
    }

    override suspend fun deleteTask(taskId: String) {
        val existing = taskDao.getTaskByIdDirect(taskId)
        if (existing == null) return

        if (existing.remoteId == null) {
            taskDao.deletePermanently(taskId)
        } else {
            taskDao.markDeleted(taskId)
            syncManager.scheduleOneTimeSync()
        }
    }

    override suspend fun toggleTaskCompletion(taskId: String) {
        val existing = taskDao.getTaskByIdDirect(taskId) ?: return
        val newCompleted = !existing.isCompleted
        taskDao.updateCompletion(
            id = taskId,
            isCompleted = newCompleted,
            lastUpdated = System.currentTimeMillis()
        )
        syncManager.scheduleOneTimeSync()
    }

    override fun getCategoriesWithCount(): Flow<List<CategoryWithTaskCount>> {
        return categoryDao.getCategoriesWithTaskCount().map { tuples ->
            tuples.map { it.toDomain() }
        }
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getCategoryById(categoryId: String): Category? {
        return categoryDao.getCategoryById(categoryId)?.toDomain()
    }

    override suspend fun saveCategory(category: Category) {
        categoryDao.insert(category.toEntity())
        syncManager.scheduleOneTimeSync()
    }

    override suspend fun deleteCategory(categoryId: String) {
        val existing = categoryDao.getCategoryById(categoryId) ?: return
        if (existing.remoteId == null) {
            categoryDao.deletePermanently(categoryId)
        } else {
            categoryDao.markDeleted(categoryId)
            syncManager.scheduleOneTimeSync()
        }
    }

    override suspend fun syncNow() {
        syncManager.scheduleOneTimeSync()
    }
}
