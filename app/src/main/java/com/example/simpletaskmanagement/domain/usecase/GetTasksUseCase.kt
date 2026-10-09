package com.example.simpletaskmanagement.domain.usecase

import com.example.simpletaskmanagement.domain.model.Task
import com.example.simpletaskmanagement.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(
        categoryId: String? = null,
        searchQuery: String = ""
    ): Flow<Map<String, List<Task>>> {
        val flow = if (categoryId == null) {
            taskRepository.getAllTasks()
        } else {
            taskRepository.getTasksByCategory(categoryId)
        }

        return flow.map { tasks ->
            val filtered = if (searchQuery.isNotBlank()) {
                tasks.filter { task ->
                    task.title.contains(searchQuery, ignoreCase = true) ||
                            (task.description?.contains(searchQuery, ignoreCase = true) == true)
                }
            } else {
                tasks
            }

            groupTasksBySection(filtered)
        }
    }

    private fun groupTasksBySection(tasks: List<Task>): Map<String, List<Task>> {
        val result = linkedMapOf<String, MutableList<Task>>()
        result["Atrasado"] = mutableListOf()
        result["Hoy"] = mutableListOf()
        result["Mañana"] = mutableListOf()
        result["Próximos"] = mutableListOf()
        result["Sin fecha"] = mutableListOf()
        result["Completadas"] = mutableListOf()

        val now = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfTomorrow = Calendar.getInstance().apply {
            timeInMillis = startOfToday
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        val startOfDayAfterTomorrow = Calendar.getInstance().apply {
            timeInMillis = startOfTomorrow
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        for (task in tasks) {
            if (task.isCompleted) {
                result["Completadas"]?.add(task)
            } else {
                val due = task.dueDate
                when {
                    due == null -> result["Sin fecha"]?.add(task)
                    due < startOfToday -> result["Atrasado"]?.add(task)
                    due < startOfTomorrow -> result["Hoy"]?.add(task)
                    due < startOfDayAfterTomorrow -> result["Mañana"]?.add(task)
                    else -> result["Próximos"]?.add(task)
                }
            }
        }

        // Return only sections that have items
        return result.filterValues { it.isNotEmpty() }
    }
}
