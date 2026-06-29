package com.example.data.model

data class RemoteActivityLog(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val action: String = "", // e.g., "LOGIN", "ADD_STUDENT", "DELETE_DATA", "EDIT_FEES"
    val details: String = "" // In Arabic
)
