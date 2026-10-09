package com.example.simpletaskmanagement.data.mapper

import com.example.simpletaskmanagement.data.local.entity.CategoryEntity
import com.example.simpletaskmanagement.data.local.entity.CategoryWithCountTuple
import com.example.simpletaskmanagement.data.local.entity.TaskEntity
import com.example.simpletaskmanagement.data.remote.dto.GoogleTaskDto
import com.example.simpletaskmanagement.data.remote.dto.GoogleTaskListDto
import com.example.simpletaskmanagement.domain.model.Category
import com.example.simpletaskmanagement.domain.model.CategoryWithTaskCount
import com.example.simpletaskmanagement.domain.model.SyncState
import com.example.simpletaskmanagement.domain.model.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object TaskMapper {

    private val rfc3339Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val rfc3339FormatShort = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parseRfc3339(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            rfc3339Format.parse(dateStr)?.time
        } catch (e: Exception) {
            try {
                rfc3339FormatShort.parse(dateStr)?.time
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun formatRfc3339(epochMillis: Long?): String? {
        if (epochMillis == null) return null
        return rfc3339Format.format(Date(epochMillis))
    }

    // Google Tasks "due" is a date-only value stored as UTC midnight; map it to local 9:00 of the same calendar day.
    fun parseGoogleDue(dateStr: String?): Long? {
        val utcMillis = parseRfc3339(dateStr) ?: return null
        val utc = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        return java.util.Calendar.getInstance().apply {
            clear()
            set(
                utc.get(java.util.Calendar.YEAR),
                utc.get(java.util.Calendar.MONTH),
                utc.get(java.util.Calendar.DAY_OF_MONTH),
                9, 0, 0
            )
        }.timeInMillis
    }

    fun formatGoogleDue(localMillis: Long?): String? {
        if (localMillis == null) return null
        val local = java.util.Calendar.getInstance().apply { timeInMillis = localMillis }
        val utcMidnight = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(java.util.Calendar.YEAR),
                local.get(java.util.Calendar.MONTH),
                local.get(java.util.Calendar.DAY_OF_MONTH),
                0, 0, 0
            )
        }.timeInMillis
        return formatRfc3339(utcMidnight)
    }

    // Domain <-> Entity
    fun TaskEntity.toDomain(categoryName: String? = null): Task = Task(
        id = localId,
        remoteId = remoteId,
        categoryId = categoryId,
        categoryName = categoryName,
        title = title,
        description = description,
        dueDate = dueDate,
        hasTime = hasTime,
        isCompleted = isCompleted,
        repeatRule = repeatRule,
        lastUpdated = lastUpdated,
        syncState = syncState
    )

    fun Task.toEntity(): TaskEntity = TaskEntity(
        localId = id,
        remoteId = remoteId,
        categoryId = categoryId,
        title = title,
        description = description,
        dueDate = dueDate,
        hasTime = hasTime,
        isCompleted = isCompleted,
        repeatRule = repeatRule,
        lastUpdated = lastUpdated,
        syncState = syncState
    )

    fun CategoryEntity.toDomain(): Category = Category(
        id = localId,
        remoteId = remoteId,
        name = name,
        isDefault = isDefault,
        syncState = syncState
    )

    fun Category.toEntity(): CategoryEntity = CategoryEntity(
        localId = id,
        remoteId = remoteId,
        name = name,
        isDefault = isDefault,
        syncState = syncState
    )

    fun CategoryWithCountTuple.toDomain(): CategoryWithTaskCount = CategoryWithTaskCount(
        category = category.toDomain(),
        uncompletedCount = uncompletedCount,
        totalCount = totalCount
    )

    // Remote <-> Entity
    fun GoogleTaskDto.toEntity(categoryId: String, existingLocalId: String? = null): TaskEntity {
        val updatedTime = parseRfc3339(updated) ?: System.currentTimeMillis()
        val parsedDue = parseGoogleDue(due)
        val completedState = status.equals("completed", ignoreCase = true) || !completed.isNullOrBlank()

        return TaskEntity(
            localId = existingLocalId ?: UUID.randomUUID().toString(),
            remoteId = id,
            categoryId = categoryId,
            title = title ?: "",
            description = notes,
            dueDate = parsedDue,
            hasTime = false,
            isCompleted = completedState,
            repeatRule = null,
            lastUpdated = updatedTime,
            syncState = SyncState.SYNCED
        )
    }

    fun TaskEntity.toDto(): GoogleTaskDto = GoogleTaskDto(
        id = remoteId,
        title = title,
        notes = description,
        due = formatGoogleDue(dueDate),
        status = if (isCompleted) "completed" else "needsAction",
        completed = if (isCompleted) formatRfc3339(lastUpdated) else null,
        deleted = (syncState == SyncState.PENDING_DELETE),
        updated = formatRfc3339(lastUpdated)
    )

    fun GoogleTaskListDto.toEntity(existingLocalId: String? = null, isDefault: Boolean = false): CategoryEntity {
        return CategoryEntity(
            localId = existingLocalId ?: UUID.randomUUID().toString(),
            remoteId = id,
            name = title ?: "Lista sin título",
            isDefault = isDefault,
            syncState = SyncState.SYNCED
        )
    }

    fun CategoryEntity.toDto(): GoogleTaskListDto = GoogleTaskListDto(
        id = remoteId,
        title = name,
        updated = null
    )
}
