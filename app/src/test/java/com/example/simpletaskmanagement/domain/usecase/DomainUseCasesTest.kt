package com.example.simpletaskmanagement.domain.usecase

import com.example.simpletaskmanagement.domain.model.Category
import com.example.simpletaskmanagement.domain.model.CategoryWithTaskCount
import com.example.simpletaskmanagement.domain.model.SyncState
import com.example.simpletaskmanagement.domain.model.Task
import com.example.simpletaskmanagement.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class FakeTaskRepository : TaskRepository {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private val categories = MutableStateFlow<List<Category>>(emptyList())

    override fun getAllTasks(): Flow<List<Task>> = tasks

    override fun getTasksByCategory(categoryId: String): Flow<List<Task>> =
        tasks.map { list -> list.filter { it.categoryId == categoryId } }

    override fun getTaskById(taskId: String): Flow<Task?> =
        tasks.map { list -> list.firstOrNull { it.id == taskId } }

    override suspend fun saveTask(task: Task) {
        val current = tasks.value.toMutableList()
        val index = current.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            current[index] = task
        } else {
            current.add(task)
        }
        tasks.value = current
    }

    override suspend fun deleteTask(taskId: String) {
        tasks.value = tasks.value.filter { it.id != taskId }
    }

    override suspend fun toggleTaskCompletion(taskId: String) {
        tasks.value = tasks.value.map {
            if (it.id == taskId) it.copy(isCompleted = !it.isCompleted) else it
        }
    }

    override fun getCategoriesWithCount(): Flow<List<CategoryWithTaskCount>> =
        categories.map { catList ->
            catList.map { cat ->
                val uncompleted = tasks.value.count { it.categoryId == cat.id && !it.isCompleted }
                val total = tasks.value.count { it.categoryId == cat.id }
                CategoryWithTaskCount(cat, uncompleted, total)
            }
        }

    override fun getAllCategories(): Flow<List<Category>> = categories

    override suspend fun getCategoryById(categoryId: String): Category? =
        categories.value.firstOrNull { it.id == categoryId }

    override suspend fun saveCategory(category: Category) {
        categories.value = categories.value + category
    }

    override suspend fun deleteCategory(categoryId: String) {
        categories.value = categories.value.filter { it.id != categoryId }
    }

    override suspend fun syncNow() {}
}

class DomainUseCasesTest {

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var getTasksUseCase: GetTasksUseCase
    private lateinit var saveTaskUseCase: SaveTaskUseCase
    private lateinit var createCategoryUseCase: CreateCategoryUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeTaskRepository()
        getTasksUseCase = GetTasksUseCase(fakeRepository)
        saveTaskUseCase = SaveTaskUseCase(fakeRepository)
        createCategoryUseCase = CreateCategoryUseCase(fakeRepository)
    }

    @Test
    fun saveTaskUseCase_savesValidTask_andTrimsTitle() = runBlocking {
        val task = Task(
            id = "task-1",
            categoryId = "cat-1",
            title = "  Comprar víveres  "
        )
        saveTaskUseCase(task)

        val retrieved = fakeRepository.getTaskById("task-1").first()
        assertNotNull(retrieved)
        assertEquals("Comprar víveres", retrieved?.title)
    }

    @Test(expected = IllegalArgumentException::class)
    fun saveTaskUseCase_emptyTitle_throwsException() = runBlocking {
        val task = Task(
            id = "task-2",
            categoryId = "cat-1",
            title = "    "
        )
        saveTaskUseCase(task)
    }

    @Test
    fun getTasksUseCase_groupsTasksCorrectly() = runBlocking {
        val now = System.currentTimeMillis()
        val yesterday = now - 24 * 60 * 60 * 1000L
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
        }.timeInMillis

        val overdueTask = Task(id = "1", categoryId = "c1", title = "Overdue task", dueDate = yesterday)
        val todayTask = Task(id = "2", categoryId = "c1", title = "Today task", dueDate = today)
        val noDateTask = Task(id = "3", categoryId = "c1", title = "No date task", dueDate = null)
        val completedTask = Task(id = "4", categoryId = "c1", title = "Completed task", isCompleted = true)

        fakeRepository.saveTask(overdueTask)
        fakeRepository.saveTask(todayTask)
        fakeRepository.saveTask(noDateTask)
        fakeRepository.saveTask(completedTask)

        val grouped = getTasksUseCase().first()

        assertTrue(grouped.containsKey("Atrasado"))
        assertTrue(grouped.containsKey("Hoy"))
        assertTrue(grouped.containsKey("Sin fecha"))
        assertTrue(grouped.containsKey("Completadas"))
        assertEquals("Overdue task", grouped["Atrasado"]?.first()?.title)
        assertEquals("Today task", grouped["Hoy"]?.first()?.title)
        assertEquals("No date task", grouped["Sin fecha"]?.first()?.title)
        assertEquals("Completed task", grouped["Completadas"]?.first()?.title)
    }

    @Test
    fun createCategoryUseCase_createsCategorySuccessfully() = runBlocking {
        val category = createCategoryUseCase("Trabajo")
        assertEquals("Trabajo", category.name)
        assertEquals(SyncState.PENDING_CREATE, category.syncState)

        val all = fakeRepository.getAllCategories().first()
        assertEquals(1, all.size)
        assertEquals("Trabajo", all[0].name)
    }
}
