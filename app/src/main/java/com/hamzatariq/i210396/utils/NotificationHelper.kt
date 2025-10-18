package com.hamzatariq.i210396.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val FCM_API = "https://fcm.googleapis.com/fcm/send"
    // Note: In production, use Firebase Functions or your backend server
    // This is a simplified example

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Send notification when a new message is received
     */
    fun sendMessageNotification(
        receiverId: String,
        senderName: String,
        messageText: String,
        senderId: String,
        senderImage: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val receiverDoc = firestore.collection("users").document(receiverId).get().await()
                val fcmToken = receiverDoc.getString("fcmToken") ?: return@launch

                val notification = JSONObject().apply {
                    put("to", fcmToken)
                    put("priority", "high")

                    val data = JSONObject().apply {
                        put("type", "new_message")
                        put("title", senderName)
                        put("body", messageText)
                        put("senderId", senderId)
                        put("senderName", senderName)
                        put("senderImage", senderImage)
                    }
                    put("data", data)
                }

                Log.d(TAG, "Sending message notification to: $receiverId")
                // In production, call your backend API here
                // sendToFCM(notification.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message notification", e)
            }
        }
    }

    /**
     * Send notification when someone follows the user
     */
    fun sendFollowerNotification(
        followedUserId: String,
        followerName: String,
        followerId: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userDoc = firestore.collection("users").document(followedUserId).get().await()
                val fcmToken = userDoc.getString("fcmToken") ?: return@launch

                val notification = JSONObject().apply {
                    put("to", fcmToken)
                    put("priority", "high")

                    val data = JSONObject().apply {
                        put("type", "new_follower")
                        put("title", "New Follower")
                        put("body", "$followerName started following you")
                        put("senderId", followerId)
                    }
                    put("data", data)
                }

                Log.d(TAG, "Sending follower notification to: $followedUserId")
                // In production, call your backend API here
                // sendToFCM(notification.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error sending follower notification", e)
            }
        }
    }

    /**
     * Send notification when someone sends a follow request
     */
    fun sendFollowRequestNotification(
        toUserId: String,
        fromUsername: String,
        fromUserId: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userDoc = firestore.collection("users").document(toUserId).get().await()
                val fcmToken = userDoc.getString("fcmToken") ?: return@launch

                val notification = JSONObject().apply {
                    put("to", fcmToken)
                    put("priority", "high")

                    val data = JSONObject().apply {
                        put("type", "follow_request")
                        put("title", "Follow Request")
                        put("body", "$fromUsername wants to follow you")
                        put("senderId", fromUserId)
                    }
                    put("data", data)
                }

                Log.d(TAG, "Sending follow request notification to: $toUserId")
                // In production, call your backend API here
                // sendToFCM(notification.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error sending follow request notification", e)
            }
        }
    }

    /**
     * Send notification when someone takes a screenshot of the chat
     */
    fun sendScreenshotAlert(
        userId: String,
        screenshotTakerName: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                val fcmToken = userDoc.getString("fcmToken") ?: return@launch

                val notification = JSONObject().apply {
                    put("to", fcmToken)
                    put("priority", "high")

                    val data = JSONObject().apply {
                        put("type", "screenshot_alert")
                        put("title", "Screenshot Alert")
                        put("body", "$screenshotTakerName took a screenshot of your chat")
                    }
                    put("data", data)
                }

                Log.d(TAG, "Sending screenshot alert to: $userId")
                // In production, call your backend API here
                // sendToFCM(notification.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error sending screenshot alert", e)
            }
        }
    }

    /**
     * Send notification for post likes
     */
    fun sendLikeNotification(
        postOwnerId: String,
        likerName: String,
        likerId: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userDoc = firestore.collection("users").document(postOwnerId).get().await()
                val fcmToken = userDoc.getString("fcmToken") ?: return@launch

                val notification = JSONObject().apply {
                    put("to", fcmToken)
                    put("priority", "default")

                    val data = JSONObject().apply {
                        put("type", "post_like")
                        put("title", "New Like")
                        put("body", "$likerName liked your post")
                        put("senderId", likerId)
                    }
                    put("data", data)
                }

                Log.d(TAG, "Sending like notification to: $postOwnerId")
                // In production, call your backend API here
            } catch (e: Exception) {
                Log.e(TAG, "Error sending like notification", e)
            }
        }
    }

    /**
     * Send notification for post comments
     */
    fun sendCommentNotification(
        postOwnerId: String,
        commenterName: String,
        commentText: String,
        commenterId: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userDoc = firestore.collection("users").document(postOwnerId).get().await()
                val fcmToken = userDoc.getString("fcmToken") ?: return@launch

                val notification = JSONObject().apply {
                    put("to", fcmToken)
                    put("priority", "high")

                    val data = JSONObject().apply {
                        put("type", "post_comment")
                        put("title", "$commenterName commented")
                        put("body", commentText)
                        put("senderId", commenterId)
                    }
                    put("data", data)
                }

                Log.d(TAG, "Sending comment notification to: $postOwnerId")
                // In production, call your backend API here
            } catch (e: Exception) {
                Log.e(TAG, "Error sending comment notification", e)
            }
        }
    }

    // Helper method to actually send to FCM (requires server key)
    // This should be done from a backend server in production
    @Suppress("unused")
    private fun sendToFCM(jsonBody: String) {
        try {
            val url = URL(FCM_API)
            val conn = url.openConnection() as HttpURLConnection
            conn.useCaches = false
            conn.doInput = true
            conn.doOutput = true
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "key=YOUR_SERVER_KEY_HERE")
            conn.setRequestProperty("Content-Type", "application/json")

            val wr = OutputStreamWriter(conn.outputStream)
            wr.write(jsonBody)
            wr.flush()

            val responseCode = conn.responseCode
            Log.d(TAG, "FCM Response Code: $responseCode")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending to FCM", e)
        }
    }
}
