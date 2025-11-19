package com.devs.i210396_i211384.utils

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.ReportScreenshotRequest

class ScreenshotDetector(
    private val context: Context,
    private val chatRoomId: String
) {
    private var contentObserver: ContentObserver? = null
    private val apiService = ApiService.create()

    companion object {
        private const val TAG = "ScreenshotDetector"
        private val SCREENSHOT_PATHS = arrayOf(
            "screenshot",
            "screen_capture",
            "screen-capture",
            "screencap",
            "screenshots"
        )
    }

    fun startListening() {
        val handler = Handler(Looper.getMainLooper())

        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                uri?.let {
                    if (isScreenshotUri(it)) {
                        onScreenshotDetected()
                    }
                }
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver!!
            )
            Log.d(TAG, "Screenshot detection started for chat room: $chatRoomId")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting screenshot detection", e)
        }
    }

    fun stopListening() {
        contentObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
                Log.d(TAG, "Screenshot detection stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping screenshot detection", e)
            }
        }
        contentObserver = null
    }

    private fun isScreenshotUri(uri: Uri): Boolean {
        val path = uri.path?.lowercase() ?: return false
        return SCREENSHOT_PATHS.any { path.contains(it) }
    }

    private fun onScreenshotDetected() {
        Log.d(TAG, "Screenshot detected! Reporting to server...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.reportScreenshot(
                    ReportScreenshotRequest(chatRoomId)
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "Screenshot reported successfully")
                } else {
                    Log.w(TAG, "Failed to report screenshot: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reporting screenshot", e)
            }
        }
    }
}

