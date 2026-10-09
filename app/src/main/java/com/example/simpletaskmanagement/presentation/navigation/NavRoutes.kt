package com.example.simpletaskmanagement.presentation.navigation

object NavRoutes {
    const val TASK_LIST = "task_list"
    const val TASK_CREATE = "task_create"
    const val TASK_EDIT = "task_edit/{taskId}"
    const val SETTINGS = "settings"

    fun taskEdit(taskId: String): String = "task_edit/$taskId"
}
