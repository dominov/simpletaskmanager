package com.example.simpletaskmanagement.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.simpletaskmanagement.domain.model.SyncState
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val localId: String = UUID.randomUUID().toString(),
    val remoteId: String? = null,
    val name: String,
    val isDefault: Boolean = false,
    val syncState: SyncState = SyncState.PENDING_CREATE
)
