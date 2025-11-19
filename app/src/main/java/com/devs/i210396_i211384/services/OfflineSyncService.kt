package com.devs.i210396_i211384.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.devs.i210396_i211384.database.OfflineDatabase
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.SendMessageRequest
import com.devs.i210396_i211384.network.CreatePostRequest
import com.devs.i210396_i211384.network.UploadStoryRequest
import com.devs.i210396_i211384.network.LikePostRequest
import com.devs.i210396_i211384.network.CommentRequest
import com.devs.i210396_i211384.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class OfflineSyncService : Service() {

    companion object {
        private const val TAG = "OfflineSyncService"
        private const val MAX_RETRY_COUNT = 3
        private const val SYNC_INTERVAL = 30000L // 30 seconds
    }

    private val apiService = ApiService.create()
    private lateinit var offlineDb: OfflineDatabase
    private lateinit var networkMonitor: NetworkMonitor
    private var syncJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate() {
        super.onCreate()
        offlineDb = OfflineDatabase.getInstance(this)
        networkMonitor = NetworkMonitor(this)

        Log.d(TAG, "OfflineSyncService created")
        startNetworkMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "OfflineSyncService started")
        startPeriodicSync()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startNetworkMonitoring() {
        networkMonitor.startMonitoring { isConnected ->
            if (isConnected) {
                Log.d(TAG, "Network connected - starting sync")
                syncPendingActions()
            } else {
                Log.d(TAG, "Network disconnected")
            }
        }
    }

    private fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            while (true) {
                if (networkMonitor.isNetworkAvailable()) {
                    syncPendingActions()
                    offlineDb.clearOldCompletedActions()
                }
                delay(SYNC_INTERVAL)
            }
        }
    }

    private fun syncPendingActions() {
        serviceScope.launch {
            try {
                val pendingActions = offlineDb.getPendingActions()

                if (pendingActions.isEmpty()) {
                    Log.d(TAG, "No pending actions to sync")
                    return@launch
                }

                Log.d(TAG, "Syncing ${pendingActions.size} pending actions")

                for (action in pendingActions) {
                    val actionId = action["actionId"] as Long
                    val actionType = action["actionType"] as String
                    val actionData = action["actionData"] as String
                    val retryCount = action["retryCount"] as Int

                    if (retryCount >= MAX_RETRY_COUNT) {
                        Log.w(TAG, "Max retries reached for action $actionId, marking as failed")
                        offlineDb.updateActionStatus(actionId, "failed")
                        continue
                    }

                    val success = processAction(actionType, actionData)

                    if (success) {
                        offlineDb.updateActionStatus(actionId, "completed")
                        Log.d(TAG, "Action $actionId completed successfully")
                    } else {
                        offlineDb.incrementRetryCount(actionId)
                        Log.w(TAG, "Action $actionId failed, retry count: ${retryCount + 1}")
                    }

                    // Small delay between requests to avoid overwhelming server
                    delay(500)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error syncing pending actions", e)
            }
        }
    }

    private suspend fun processAction(actionType: String, actionData: String): Boolean {
        return try {
            val jsonData = JSONObject(actionData)

            when (actionType) {
                "send_message" -> processSendMessage(jsonData)
                "create_post" -> processCreatePost(jsonData)
                "upload_story" -> processUploadStory(jsonData)
                "like_post" -> processLikePost(jsonData)
                "add_comment" -> processAddComment(jsonData)
                "follow_user" -> processFollowUser(jsonData)
                else -> {
                    Log.w(TAG, "Unknown action type: $actionType")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing action: $actionType", e)
            false
        }
    }

    private suspend fun processSendMessage(data: JSONObject): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = SendMessageRequest(
                    receiverId = data.getString("receiverId"),
                    messageText = data.getString("messageText"),
                    messageType = data.optString("messageType", "text"),
                    mediaUrl = data.optString("mediaUrl", ""),
                    postId = data.optString("postId", ""),
                    isVanishMode = data.optBoolean("isVanishMode", false)
                )

                val response = apiService.sendMessage(request)
                response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                false
            }
        }
    }

    private suspend fun processCreatePost(data: JSONObject): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = CreatePostRequest(
                    imageBase64 = data.getString("imageBase64"),
                    caption = data.getString("caption"),
                    location = data.optString("location", "")
                )

                val response = apiService.createPost(request)
                response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Error creating post", e)
                false
            }
        }
    }

    private suspend fun processUploadStory(data: JSONObject): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = UploadStoryRequest(
                    storyImageBase64 = data.getString("storyImageBase64")
                )

                val response = apiService.uploadStory(request)
                response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading story", e)
                false
            }
        }
    }

    private suspend fun processLikePost(data: JSONObject): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = LikePostRequest(
                    postId = data.getString("postId")
                )

                val response = apiService.likePost(request)
                response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Error liking post", e)
                false
            }
        }
    }

    private suspend fun processAddComment(data: JSONObject): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = CommentRequest(
                    postId = data.getString("postId"),
                    commentText = data.getString("commentText")
                )

                val response = apiService.addComment(request)
                response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Error adding comment", e)
                false
            }
        }
    }

    private suspend fun processFollowUser(data: JSONObject): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.devs.i210396_i211384.network.FollowRequest(
                    userId = data.getString("userId")
                )

                val response = apiService.followUser(request)
                response.isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Error following user", e)
                false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        syncJob?.cancel()
        networkMonitor.stopMonitoring()
        Log.d(TAG, "OfflineSyncService destroyed")
    }
}

