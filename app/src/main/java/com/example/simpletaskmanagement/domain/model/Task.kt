package com.example.simpletaskmanagement.domain.model

import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val remoteId: String? = null,
    val categoryId: String,
    val categoryName: String? = null,
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null, // Epoch millis
    val hasTime: Boolean = false,
    val isCompleted: Boolean = false,
    val repeatRule: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.PENDING_CREATE
)
