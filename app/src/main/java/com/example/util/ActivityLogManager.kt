package com.example.util

import android.util.Log
import com.example.data.model.RemoteActivityLog
import com.example.util.rbac.RbacManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object ActivityLogManager {
    private const val TAG = "ActivityLogManager"
    private const val COLLECTION_NAME = "activity_logs"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun logAction(action: String, details: String) {
        val auth = FirebaseSafe.auth ?: return
        val currentUser = auth.currentUser ?: return
        val currentRbac = RbacManager.currentUserRbac.value

        val userId = currentUser.uid
        val userEmail = currentUser.email ?: ""
        val userName = currentRbac?.name ?: currentUser.displayName ?: "مستخدم"

        scope.launch {
            val firestore = FirebaseSafe.firestore ?: return@launch
            try {
                val docRef = firestore.collection(COLLECTION_NAME).document()
                val logEntry = RemoteActivityLog(
                    id = docRef.id,
                    timestamp = System.currentTimeMillis(),
                    userId = userId,
                    userEmail = userEmail,
                    userName = userName,
                    action = action,
                    details = details
                )
                docRef.set(logEntry).await()
                Log.d(TAG, "Successfully logged action: $action - $details")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write activity log", e)
            }
        }
    }

    suspend fun getAllLogs(): List<RemoteActivityLog> {
        val firestore = FirebaseSafe.firestore ?: return emptyList()
        return try {
            val snapshot = firestore.collection(COLLECTION_NAME)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(200) // Limit logs for performance
                .get()
                .await()
            snapshot.toObjects(RemoteActivityLog::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve activity logs", e)
            emptyList()
        }
    }
}
