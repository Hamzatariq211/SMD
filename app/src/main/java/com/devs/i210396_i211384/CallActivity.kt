package com.devs.i210396_i211384

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devs.i210396_i211384.utils.AgoraConfig
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.network.ApiService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallActivity : AppCompatActivity() {
    private val apiService = ApiService.create()

    companion object {
        private const val TAG = "CallActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SessionManager.init(this)

        // Get call parameters from intent
        val receiverId = intent.getStringExtra("receiverId") ?: ""
        val receiverName = intent.getStringExtra("receiverName") ?: "User"
        val callType = intent.getStringExtra("callType") ?: "video"

        Log.d(TAG, "CallActivity onCreate - receiverId: $receiverId, callType: $callType")

        if (receiverId.isEmpty()) {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Launch call screen immediately with basic info
        launchCallScreen(receiverId, receiverName, callType)
    }

    private fun launchCallScreen(receiverId: String, receiverName: String, callType: String) {
        val currentUserId = SessionManager.getUserId()

        Log.d(TAG, "Launching call screen - currentUserId: $currentUserId, receiverId: $receiverId")

        if (currentUserId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Generate channel name and call ID
        val callId = "${currentUserId}_${receiverId}_${System.currentTimeMillis()}"
        val channelName = AgoraConfig.generateChannelName(currentUserId, receiverId)

        Log.d(TAG, "Generated callId: $callId, channelName: $channelName")

        // Launch call screen immediately
        val intent = Intent(this, callScreen::class.java).apply {
            putExtra("callId", callId)
            putExtra("channelName", channelName)
            putExtra("callType", callType)
            putExtra("isIncoming", false)
            putExtra("otherUserId", receiverId)
            putExtra("otherUserName", receiverName)
            putExtra("otherUserImage", "") // Will load in callScreen if needed
            putExtra("currentUserId", currentUserId)
        }

        Log.d(TAG, "Starting callScreen activity with channelName: $channelName")
        startActivity(intent)
        finish()

        // Load receiver image in background (non-blocking)
        loadReceiverImageInBackground(receiverId)
    }

    private fun loadReceiverImageInBackground(receiverId: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getUserProfile(receiverId)
                }
                if (response.isSuccessful) {
                    Log.d(TAG, "Loaded receiver profile successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading receiver image: ${e.message}")
            }
        }
    }
}
