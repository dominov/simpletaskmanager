package com.example.simpletaskmanagement.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.simpletaskmanagement.domain.model.SyncState
import java.util.UUID

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["localId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class TaskEntity(
    @PrimaryKey val localId: String = UUID.randomUUID().toString(),
    val remoteId: String? = null,
    val categoryId: String,
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val hasTime: Boolean = false,
    val isCompleted: Boolean = false,
    val repeatRule: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.PENDING_CREATE
)
