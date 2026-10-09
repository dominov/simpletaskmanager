package com.example.simpletaskmanagement.domain.model

data class CategoryWithTaskCount(
    val category: Category,
    val uncompletedCount: Int,
    val totalCount: Int
)
