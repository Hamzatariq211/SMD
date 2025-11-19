package com.devs.i210396_i211384.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.network.UpdateFCMTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                getFCMToken(activity)
            }
        } else {
            // No permission needed for older versions
            getFCMToken(activity)
        }
    }

    /**
     * Get FCM token and save to MySQL server
     */
    fun getFCMToken(activity: Activity) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Token: $token")

            // Save token to MySQL server
            saveFCMTokenToServer(activity, token)
        }
    }

    /**
     * Save FCM token to MySQL server via API
     */
    private fun saveFCMTokenToServer(activity: Activity, token: String) {
        SessionManager.init(activity)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiService.create()
                val response = apiService.updateFCMToken(UpdateFCMTokenRequest(token))

                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token saved to server successfully")
                } else {
                    Log.w(TAG, "Failed to save FCM token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving FCM token to server", e)
            }
        }
    }

    /**
     * Delete FCM token (for logout)
     */
    fun deleteFCMToken(activity: Activity) {
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "FCM token deleted")

                // Remove from server
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val apiService = ApiService.create()
                        apiService.updateFCMToken(UpdateFCMTokenRequest(""))
                        Log.d(TAG, "FCM token removed from server")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing FCM token from server", e)
                    }
                }
            }
        }
    }
}
