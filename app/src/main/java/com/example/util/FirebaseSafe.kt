package com.example.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseSafe {
    val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }

    val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }

    fun getCenterCollection(collectionPath: String): com.google.firebase.firestore.CollectionReference? {
        val db = firestore ?: return null
        val centerId = com.example.util.rbac.RbacManager.currentUserRbac.value?.centerId
        if (centerId.isNullOrEmpty()) return null
        return db.collection("centers").document(centerId).collection(collectionPath)
    }

    val isAvailable: Boolean
        get() = auth != null && firestore != null
}
