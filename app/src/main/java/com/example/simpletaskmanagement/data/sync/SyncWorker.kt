package com.example.simpletaskmanagement.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.simpletaskmanagement.data.auth.AuthManager
import com.example.simpletaskmanagement.data.local.dao.CategoryDao
import com.example.simpletaskmanagement.data.local.dao.TaskDao
import com.example.simpletaskmanagement.data.mapper.TaskMapper
import com.example.simpletaskmanagement.data.mapper.TaskMapper.toDto
import com.example.simpletaskmanagement.data.mapper.TaskMapper.toEntity
import com.example.simpletaskmanagement.data.remote.GoogleTasksApi
import com.example.simpletaskmanagement.domain.model.SyncState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val googleTasksApi: GoogleTasksApi,
    private val authManager: AuthManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // If not authenticated, we operate offline only.
        Log.d(TAG, "doWork iniciado, intento=$runAttemptCount")
        if (!authManager.hasToken()) {
            Log.w(TAG, "Sin token de Google: no se sincroniza")
            return Result.success()
        }

        return try {
            pushLocalChanges()
            pullRemoteChanges()
            Log.d(TAG, "Sincronizacion terminada OK")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun pushLocalChanges() {
        // 1. Push pending categories
        val pendingCategories = categoryDao.getPendingSyncCategories()
        Log.d(TAG, "PUSH categorias pendientes: ${pendingCategories.size}")
        for (category in pendingCategories) {
            when (category.syncState) {
                SyncState.PENDING_CREATE -> {
                    try {
                        val created = googleTasksApi.createTaskList(category.toDto())
                        categoryDao.update(
                            category.copy(
                                remoteId = created.id,
                                syncState = SyncState.SYNCED
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}", e)
                    }
                }
                SyncState.PENDING_DELETE -> {
                    try {
                        if (category.remoteId != null && category.remoteId != "@default") {
                            googleTasksApi.deleteTaskList(category.remoteId)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}", e)
                    }
                    categoryDao.deletePermanently(category.localId)
                }
                SyncState.PENDING_UPDATE -> {
                    categoryDao.update(category.copy(syncState = SyncState.SYNCED))
                }
                SyncState.SYNCED -> Unit
            }
        }

        // 2. Push pending tasks
        val pendingTasks = taskDao.getPendingSyncTasks()
        Log.d(TAG, "PUSH tareas pendientes: ${pendingTasks.size}")
        for (task in pendingTasks) {
            Log.d(TAG, "PUSH tarea '${task.title}' estado=${task.syncState}")
            val category = categoryDao.getCategoryById(task.categoryId)
            val taskListId = category?.remoteId ?: "@default"

            when (task.syncState) {
                SyncState.PENDING_CREATE -> {
                    try {
                        val created = googleTasksApi.insertTask(taskListId, task.toDto())
                        taskDao.update(
                            task.copy(
                                remoteId = created.id,
                                syncState = SyncState.SYNCED
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}", e)
                    }
                }
                SyncState.PENDING_UPDATE -> {
                    try {
                        if (task.remoteId != null) {
                            googleTasksApi.updateTask(taskListId, task.remoteId, task.toDto())
                            taskDao.update(task.copy(syncState = SyncState.SYNCED))
                        } else {
                            val created = googleTasksApi.insertTask(taskListId, task.toDto())
                            taskDao.update(
                                task.copy(
                                    remoteId = created.id,
                                    syncState = SyncState.SYNCED
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}", e)
                    }
                }
                SyncState.PENDING_DELETE -> {
                    try {
                        if (task.remoteId != null) {
                            googleTasksApi.deleteTask(taskListId, task.remoteId)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}", e)
                    }
                    taskDao.deletePermanently(task.localId)
                }
                SyncState.SYNCED -> Unit
            }
        }
    }

    private suspend fun pullRemoteChanges() {
        val listsResponse = googleTasksApi.getTaskLists()
        val remoteLists = listsResponse.items ?: emptyList()
        Log.d(TAG, "PULL listas remotas: ${remoteLists.size}")

        for (remoteList in remoteLists) {
            val remoteId = remoteList.id ?: continue
            val existing = categoryDao.getCategoryByRemoteId(remoteId)
            val localId = if (existing != null) {
                if (existing.syncState == SyncState.SYNCED) {
                    categoryDao.update(existing.copy(name = remoteList.title ?: existing.name))
                }
                existing.localId
            } else {
                val newCat = remoteList.toEntity()
                categoryDao.insert(newCat)
                newCat.localId
            }

            // Pull tasks for this list
            try {
                val tasksResponse = googleTasksApi.getTasks(remoteId)
                val remoteTasks = tasksResponse.items ?: emptyList()
                Log.d(TAG, "PULL lista '${remoteList.title}': ${remoteTasks.size} tareas")

                for (remoteTask in remoteTasks) {
                    val taskRemoteId = remoteTask.id ?: continue
                    val existingTask = taskDao.getTaskByRemoteId(taskRemoteId)

                    if (remoteTask.deleted == true) {
                        if (existingTask != null && existingTask.syncState == SyncState.SYNCED) {
                            taskDao.deletePermanently(existingTask.localId)
                        }
                        continue
                    }

                    if (existingTask == null) {
                        val newTask = remoteTask.toEntity(categoryId = localId)
                        taskDao.insert(newTask)
                    } else if (existingTask.syncState == SyncState.SYNCED) {
                        val parsedRemoteUpdated = TaskMapper.parseRfc3339(remoteTask.updated) ?: 0L
                        if (parsedRemoteUpdated >= existingTask.lastUpdated) {
                            val updatedTask = remoteTask.toEntity(
                                categoryId = localId,
                                existingLocalId = existingTask.localId
                            )
                            taskDao.update(updatedTask)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
    }
}
