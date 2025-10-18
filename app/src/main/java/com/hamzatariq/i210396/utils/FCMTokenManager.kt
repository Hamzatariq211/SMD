package com.hamzatariq.i210396.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object FCMTokenManager {

    private const val TAG = "FCMTokenManager"
    private const val NOTIFICATION_PERMISSION_CODE = 1001

    /**
     * Request notification permission for Android 13+
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            } else {
                // Permission already granted, get token
                getFCMToken()
            }
        } else {
            // No permission needed for older versions
            getFCMToken()
        }
    }

    /**
     * Get FCM token and save to Firestore
     */
    fun getFCMToken() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Token: $token")

            // Save token to Firestore
            saveFCMTokenToFirestore(userId, token)
        }
    }

    /**
     * Save FCM token to Firestore user document
     */
    private fun saveFCMTokenToFirestore(userId: String, token: String) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users").document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error saving FCM token", e)
                // If update fails, try to set it
                firestore.collection("users").document(userId)
                    .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
            }
    }

    /**
     * Delete FCM token (for logout)
     */
    fun deleteFCMToken() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "FCM token deleted")

                // Remove from Firestore
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("users").document(userId)
                    .update("fcmToken", null)
            }
        }
    }
}

