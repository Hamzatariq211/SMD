package com.hamzatariq.i210396.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object OnlineStatusManager {
    private const val TAG = "OnlineStatusManager"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Set user status to online when app becomes active
     */
    fun setUserOnline() {
        val userId = auth.currentUser?.uid ?: return

        val statusData = hashMapOf<String, Any>(
            "isOnline" to true,
            "lastSeen" to FieldValue.serverTimestamp()
        )

        firestore.collection("users").document(userId)
            .update(statusData)
            .addOnSuccessListener {
                Log.d(TAG, "User status set to online")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error setting user online: ${e.message}")
            }
    }

    /**
     * Set user status to offline when app goes to background or user logs out
     */
    fun setUserOffline() {
        val userId = auth.currentUser?.uid ?: return

        val statusData = hashMapOf<String, Any>(
            "isOnline" to false,
            "lastSeen" to FieldValue.serverTimestamp()
        )

        firestore.collection("users").document(userId)
            .update(statusData)
            .addOnSuccessListener {
                Log.d(TAG, "User status set to offline")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error setting user offline: ${e.message}")
            }
    }

    /**
     * Listen to user's online status in real-time
     */
    fun listenToUserStatus(userId: String, onStatusChange: (isOnline: Boolean, lastSeen: Long) -> Unit) {
        firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to user status: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val isOnline = snapshot.getBoolean("isOnline") ?: false

                    // Handle lastSeen - it could be a Timestamp or a Long
                    val lastSeen = try {
                        val timestamp = snapshot.getTimestamp("lastSeen")
                        timestamp?.toDate()?.time ?: 0L
                    } catch (e: Exception) {
                        // Try to get it as a Long if it's not a Timestamp
                        try {
                            snapshot.getLong("lastSeen") ?: 0L
                        } catch (e2: Exception) {
                            Log.w(TAG, "Could not parse lastSeen: ${e2.message}")
                            0L
                        }
                    }

                    onStatusChange(isOnline, lastSeen)
                }
            }
    }

    /**
     * Format last seen time to human-readable format
     */
    fun formatLastSeen(lastSeenTimestamp: Long): String {
        if (lastSeenTimestamp == 0L) return "Unknown"

        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastSeenTimestamp

        val seconds = timeDiff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            days < 7 -> "$days day${if (days > 1) "s" else ""} ago"
            else -> "Long time ago"
        }
    }
}
