package com.example.simpletaskmanagement.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.simpletaskmanagement.data.local.dao.CategoryDao
import com.example.simpletaskmanagement.data.local.dao.TaskDao
import com.example.simpletaskmanagement.data.local.entity.CategoryEntity
import com.example.simpletaskmanagement.data.local.entity.Converters
import com.example.simpletaskmanagement.data.local.entity.TaskEntity
import com.example.simpletaskmanagement.domain.model.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [CategoryEntity::class, TaskEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao

    companion object {
        const val DATABASE_NAME = "tasks_db"
        val DEFAULT_CATEGORY_ID: String = UUID.fromString("00000000-0000-0000-0000-000000000001").toString()

        fun createCallback(databaseProvider: () -> TaskDatabase): Callback {
            return object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        val defaultCategory = CategoryEntity(
                            localId = DEFAULT_CATEGORY_ID,
                            remoteId = "@default",
                            name = "Mis tareas",
                            isDefault = true,
                            syncState = SyncState.PENDING_CREATE
                        )
                        databaseProvider().categoryDao().insert(defaultCategory)
                    }
                }
            }
        }
    }
}
