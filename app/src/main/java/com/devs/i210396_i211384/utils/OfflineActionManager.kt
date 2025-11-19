package com.devs.i210396_i211384.utils

import android.content.Context
import android.util.Log
import com.devs.i210396_i211384.database.OfflineDatabase
import org.json.JSONObject

class OfflineActionManager(private val context: Context) {

    private val offlineDb = OfflineDatabase.getInstance(context)
    private val networkMonitor = NetworkMonitor(context)

    companion object {
        private const val TAG = "OfflineActionManager"

        @Volatile
        private var INSTANCE: OfflineActionManager? = null

        fun getInstance(context: Context): OfflineActionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OfflineActionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun isOnline(): Boolean {
        return networkMonitor.isNetworkAvailable()
    }

    // Queue a message to be sent when online
    fun queueSendMessage(receiverId: String, messageText: String, messageType: String = "text",
                         mediaUrl: String = "", postId: String = "", isVanishMode: Boolean = false): Long {
        val actionData = JSONObject().apply {
            put("receiverId", receiverId)
            put("messageText", messageText)
            put("messageType", messageType)
            put("mediaUrl", mediaUrl)
            put("postId", postId)
            put("isVanishMode", isVanishMode)
        }.toString()

        val actionId = offlineDb.addPendingAction("send_message", actionData)
        Log.d(TAG, "Queued send_message action with ID: $actionId")
        return actionId
    }

    // Queue a post to be created when online
    fun queueCreatePost(imageBase64: String, caption: String, location: String = ""): Long {
        val actionData = JSONObject().apply {
            put("imageBase64", imageBase64)
            put("caption", caption)
            put("location", location)
        }.toString()

        val actionId = offlineDb.addPendingAction("create_post", actionData)
        Log.d(TAG, "Queued create_post action with ID: $actionId")
        return actionId
    }

    // Queue a story to be uploaded when online
    fun queueUploadStory(storyImageBase64: String): Long {
        val actionData = JSONObject().apply {
            put("storyImageBase64", storyImageBase64)
        }.toString()

        val actionId = offlineDb.addPendingAction("upload_story", actionData)
        Log.d(TAG, "Queued upload_story action with ID: $actionId")
        return actionId
    }

    // Queue a like action when offline
    fun queueLikePost(postId: String): Long {
        val actionData = JSONObject().apply {
            put("postId", postId)
        }.toString()

        val actionId = offlineDb.addPendingAction("like_post", actionData)
        Log.d(TAG, "Queued like_post action with ID: $actionId")
        return actionId
    }

    // Queue a comment when offline
    fun queueAddComment(postId: String, commentText: String): Long {
        val actionData = JSONObject().apply {
            put("postId", postId)
            put("commentText", commentText)
        }.toString()

        val actionId = offlineDb.addPendingAction("add_comment", actionData)
        Log.d(TAG, "Queued add_comment action with ID: $actionId")
        return actionId
    }

    // Queue a follow action when offline
    fun queueFollowUser(userId: String): Long {
        val actionData = JSONObject().apply {
            put("userId", userId)
        }.toString()

        val actionId = offlineDb.addPendingAction("follow_user", actionData)
        Log.d(TAG, "Queued follow_user action with ID: $actionId")
        return actionId
    }

    // Get count of pending actions
    fun getPendingActionsCount(): Int {
        return offlineDb.getPendingActions().size
    }

    // Get all pending actions for display
    fun getPendingActions(): List<Map<String, Any>> {
        return offlineDb.getPendingActions()
    }

    // Manually trigger sync (useful for pull-to-refresh)
    fun triggerSync(context: Context) {
        if (isOnline()) {
            val intent = android.content.Intent(context, com.devs.i210396_i211384.services.OfflineSyncService::class.java)
            context.startService(intent)
            Log.d(TAG, "Manually triggered sync")
        } else {
            Log.d(TAG, "Cannot sync - device is offline")
        }
    }
}

