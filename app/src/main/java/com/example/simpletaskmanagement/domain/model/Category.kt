package com.example.simpletaskmanagement.domain.model

import java.util.UUID

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val remoteId: String? = null,
    val name: String,
    val isDefault: Boolean = false,
    val syncState: SyncState = SyncState.PENDING_CREATE
)
