package com.example.simpletaskmanagement.data.local.entity

import androidx.room.Embedded

data class CategoryWithCountTuple(
    @Embedded val category: CategoryEntity,
    val uncompletedCount: Int,
    val totalCount: Int
)
