package com.example.simpletaskmanagement.data.remote

import com.example.simpletaskmanagement.data.remote.dto.GoogleTaskDto
import com.example.simpletaskmanagement.data.remote.dto.GoogleTaskListDto
import com.example.simpletaskmanagement.data.remote.dto.GoogleTaskListsResponse
import com.example.simpletaskmanagement.data.remote.dto.GoogleTasksResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleTasksApi {

    @GET("tasks/v1/users/@me/lists")
    suspend fun getTaskLists(): GoogleTaskListsResponse

    @POST("tasks/v1/users/@me/lists")
    suspend fun createTaskList(@Body list: GoogleTaskListDto): GoogleTaskListDto

    @DELETE("tasks/v1/users/@me/lists/{tasklist}")
    suspend fun deleteTaskList(@Path("tasklist") taskListId: String): Response<Unit>

    @GET("tasks/v1/lists/{tasklist}/tasks")
    suspend fun getTasks(
        @Path("tasklist") taskListId: String,
        @Query("showCompleted") showCompleted: Boolean = true,
        @Query("showHidden") showHidden: Boolean = true
    ): GoogleTasksResponse

    @POST("tasks/v1/lists/{tasklist}/tasks")
    suspend fun insertTask(
        @Path("tasklist") taskListId: String,
        @Body task: GoogleTaskDto
    ): GoogleTaskDto

    @PATCH("tasks/v1/lists/{tasklist}/tasks/{task}")
    suspend fun updateTask(
        @Path("tasklist") taskListId: String,
        @Path("task") taskId: String,
        @Body task: GoogleTaskDto
    ): GoogleTaskDto

    @DELETE("tasks/v1/lists/{tasklist}/tasks/{task}")
    suspend fun deleteTask(
        @Path("tasklist") taskListId: String,
        @Path("task") taskId: String
    ): Response<Unit>
}
