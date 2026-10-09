package com.example.simpletaskmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.simpletaskmanagement.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE syncState != 'PENDING_DELETE' ORDER BY isCompleted ASC, dueDate ASC, lastUpdated DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId AND syncState != 'PENDING_DELETE' ORDER BY isCompleted ASC, dueDate ASC, lastUpdated DESC")
    fun getTasksByCategory(categoryId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE localId = :id LIMIT 1")
    fun getTaskById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE localId = :id LIMIT 1")
    suspend fun getTaskByIdDirect(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getTaskByRemoteId(remoteId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, lastUpdated = :lastUpdated, syncState = 'PENDING_UPDATE' WHERE localId = :id")
    suspend fun updateCompletion(id: String, isCompleted: Boolean, lastUpdated: Long): Int

    @Query("UPDATE tasks SET syncState = 'PENDING_DELETE' WHERE localId = :id")
    suspend fun markDeleted(id: String): Int

    @Query("DELETE FROM tasks WHERE localId = :id")
    suspend fun deletePermanently(id: String): Int

    @Query("SELECT * FROM tasks WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncTasks(): List<TaskEntity>
}
