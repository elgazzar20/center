package com.example.util.rbac

import android.content.Context
import android.util.Log
import com.example.data.model.AccountType
import com.example.data.model.Permission
import com.example.data.model.UserRbac
import com.example.util.FirebaseSafe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

object RbacManager {
    private const val TAG = "RbacManager"
    private const val COLLECTION_NAME = "user_rbac"

    private val _currentUserRbac = MutableStateFlow<UserRbac?>(null)
    val currentUserRbac: StateFlow<UserRbac?> = _currentUserRbac.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(context: Context) {
        val auth = FirebaseSafe.auth ?: return
        
        // Listen for authentication changes to automatically sync user permissions
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                loadUserRbac(firebaseUser.uid, firebaseUser.email ?: "", firebaseUser.displayName ?: "مستخدم")
            } else {
                _currentUserRbac.value = null
            }
        }
    }

    /**
     * Checks if the active user possesses a specific permission.
     * If no user is logged in (e.g. in offline or demo mode), defaults to true to allow complete access.
     */
    fun hasPermission(permission: Permission): Boolean {
        val current = _currentUserRbac.value
        // If we know there is an auth user but RBAC is not loaded yet, we should probably wait or return false to be safe.
        // For offline mode, current is null, so it might return true. But let's return true only if auth is null.
        if (current == null) {
            return FirebaseSafe.auth?.currentUser == null // True only if offline
        }
        if (current.role == AccountType.OWNER) return true // Owner always has full access
        return current.permissions.contains(permission.name)
    }

    /**
     * Checks permission reactively in Compose.
     */
    @androidx.compose.runtime.Composable
    fun hasPermissionAsState(permission: Permission): androidx.compose.runtime.State<Boolean> {
        val current by currentUserRbac.collectAsState()
        return androidx.compose.runtime.derivedStateOf {
            val user = current
            if (user == null) {
                FirebaseSafe.auth?.currentUser == null // True only if offline
            } else {
                if (user.role == AccountType.OWNER) true
                else user.permissions.contains(permission.name)
            }
        }
    }

    /**
     * Loads the UserRbac document from Firestore. If it does not exist, provisions a default record.
     */
    fun loadUserRbac(uid: String, email: String, name: String) {
        scope.launch {
            val firestore = FirebaseSafe.firestore
            if (firestore == null) {
                // Offline fallback - provision standard Owner in memory
                _currentUserRbac.value = UserRbac(
                    userId = uid,
                    email = email,
                    name = name,
                    role = AccountType.OWNER,
                    permissions = RbacDefaults.getDefaultPermissionsForRole(AccountType.OWNER).map { it.name },
                    centerId = uid
                )
                return@launch
            }

            try {
                val docRef = firestore.collection(COLLECTION_NAME).document(uid)
                val snapshot = docRef.get().await()
                if (snapshot.exists()) {
                    val rbac = snapshot.toObject(UserRbac::class.java)
                    _currentUserRbac.value = rbac
                    Log.d(TAG, "Successfully loaded user RBAC: $rbac")
                    
                    // Log session start
                    com.example.util.ActivityLogManager.logAction("تسجيل الدخول", "سجل المستخدم ${rbac?.name ?: email} الدخول للنظام بنجاح")
                } else {
                    // Self-bootstrapping: Check if this is the first user in the collection.
                    // If the collection is empty, make this user OWNER. Otherwise, default to SECRETARY.
                    val query = firestore.collection(COLLECTION_NAME).limit(1).get().await()
                    val role = if (query.isEmpty) AccountType.OWNER else AccountType.SECRETARY
                    val defaultPerms = RbacDefaults.getDefaultPermissionsForRole(role).map { it.name }
                    
                    val newRbac = UserRbac(
                        userId = uid,
                        email = email,
                        name = name,
                        role = role,
                        permissions = defaultPerms,
                        centerId = uid // Owner's centerId is their own userId
                    )
                    
                    docRef.set(newRbac).await()
                    _currentUserRbac.value = newRbac
                    Log.d(TAG, "Created default user RBAC record: $newRbac")
                    
                    // Log session start
                    com.example.util.ActivityLogManager.logAction("تسجيل الدخول", "تم إنشاء الحساب لأول مرة وتسجيل الدخول كمالك نظام ($name)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user RBAC, applying local fallback", e)
                _currentUserRbac.value = UserRbac(
                    userId = uid,
                    email = email,
                    name = name,
                    role = AccountType.OWNER,
                    permissions = RbacDefaults.getDefaultPermissionsForRole(AccountType.OWNER).map { it.name },
                    centerId = uid
                )
            }
        }
    }

    /**
     * Fetches all registered user RBAC records from Firestore.
     */
    suspend fun getAllUsers(): List<UserRbac> {
        val firestore = FirebaseSafe.firestore ?: return emptyList()
        return try {
            val querySnapshot = firestore.collection(COLLECTION_NAME).get().await()
            querySnapshot.toObjects(UserRbac::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all user RBACs", e)
            emptyList()
        }
    }

    /**
     * Updates an existing user's role and custom permissions on Firestore.
     */
    suspend fun updateUserRbac(userRbac: UserRbac): Boolean {
        val firestore = FirebaseSafe.firestore ?: return false
        return try {
            firestore.collection(COLLECTION_NAME).document(userRbac.userId).set(userRbac).await()
            // If the updated user is the current active user, update local state
            if (userRbac.userId == FirebaseAuth.getInstance().currentUser?.uid) {
                _currentUserRbac.value = userRbac
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user RBAC for ${userRbac.userId}", e)
            false
        }
    }

    /**
     * Delete user RBAC record from Firestore.
     */
    suspend fun deleteUserRbac(userId: String): Boolean {
        val firestore = FirebaseSafe.firestore ?: return false
        return try {
            firestore.collection(COLLECTION_NAME).document(userId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete user RBAC", e)
            false
        }
    }
}
