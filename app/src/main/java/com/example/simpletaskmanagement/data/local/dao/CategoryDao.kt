package com.example.simpletaskmanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.simpletaskmanagement.data.local.entity.CategoryEntity
import com.example.simpletaskmanagement.data.local.entity.CategoryWithCountTuple
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query(
        """
        SELECT c.*,
               COUNT(CASE WHEN t.isCompleted = 0 AND t.syncState != 'PENDING_DELETE' THEN 1 END) AS uncompletedCount,
               COUNT(CASE WHEN t.syncState != 'PENDING_DELETE' THEN 1 END) AS totalCount
        FROM categories c
        LEFT JOIN tasks t ON c.localId = t.categoryId
        WHERE c.syncState != 'PENDING_DELETE'
        GROUP BY c.localId
        ORDER BY c.isDefault DESC, c.name ASC
        """
    )
    fun getCategoriesWithTaskCount(): Flow<List<CategoryWithCountTuple>>

    @Query("SELECT * FROM categories WHERE syncState != 'PENDING_DELETE' ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE localId = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getCategoryByRemoteId(remoteId: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultCategory(): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("UPDATE categories SET syncState = 'PENDING_DELETE' WHERE localId = :id")
    suspend fun markDeleted(id: String): Int

    @Query("DELETE FROM categories WHERE localId = :id")
    suspend fun deletePermanently(id: String): Int

    @Query("SELECT * FROM categories WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncCategories(): List<CategoryEntity>
}
