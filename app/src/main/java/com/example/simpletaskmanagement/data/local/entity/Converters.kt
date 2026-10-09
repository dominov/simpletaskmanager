package com.example.simpletaskmanagement.data.local.entity

import androidx.room.TypeConverter
import com.example.simpletaskmanagement.domain.model.SyncState

class Converters {
    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = try {
        SyncState.valueOf(value)
    } catch (e: Exception) {
        SyncState.SYNCED
    }
}
