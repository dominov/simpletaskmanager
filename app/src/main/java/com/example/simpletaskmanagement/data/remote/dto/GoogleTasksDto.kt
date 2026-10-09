package com.example.simpletaskmanagement.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GoogleTaskListDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("updated") val updated: String? = null
)

data class GoogleTaskListsResponse(
    @SerializedName("items") val items: List<GoogleTaskListDto>? = null
)

data class GoogleTaskDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("due") val due: String? = null, // RFC 3339 formatted
    @SerializedName("status") val status: String? = null, // "needsAction" or "completed"
    @SerializedName("completed") val completed: String? = null,
    @SerializedName("deleted") val deleted: Boolean? = null,
    @SerializedName("updated") val updated: String? = null
)

data class GoogleTasksResponse(
    @SerializedName("items") val items: List<GoogleTaskDto>? = null
)
