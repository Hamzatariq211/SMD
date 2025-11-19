package com.devs.i210396_i211384

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.devs.i210396_i211384.models.CallRequest
import com.devs.i210396_i211384.services.CallService
import com.devs.i210396_i211384.utils.AgoraConfig
import com.devs.i210396_i211384.utils.NotificationHelper

class CallActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val receiverId = intent.getStringExtra("userId") ?: ""
        val callType = intent.getStringExtra("callType") ?: "video"

        if (receiverId.isEmpty()) {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initiateCall(receiverId, callType)
    }

    private fun initiateCall(receiverId: String, callType: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Get current user info
        firestore.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                val callerName = document.getString("username") ?: "User"
                val callerImage = document.getString("profileImageUrl") ?: ""

                // Generate unique call ID and channel name
                val callId = "${currentUserId}_${receiverId}_${System.currentTimeMillis()}"
                val channelName = AgoraConfig.generateChannelName(currentUserId, receiverId)

                val callRequest = CallRequest(
                    callId = callId,
                    callerId = currentUserId,
                    callerName = callerName,
                    callerImageUrl = callerImage,
                    receiverId = receiverId,
                    callType = callType,
                    channelName = channelName,
                    status = "ringing"
                )

                // Save call to Firebase
                CallService.initiateCall(
                    callRequest,
                    onSuccess = {
                        // Get receiver info and send notification
                        getReceiverInfo(receiverId) { receiverName, receiverImage ->
                            // Send notification to receiver
                            NotificationHelper.sendCallNotification(
                                receiverId = receiverId,
                                callerName = callerName,
                                callerImage = callerImage,
                                callId = callId,
                                callType = callType,
                                channelName = channelName
                            )

                            // Start call screen
                            val intent = Intent(this, callScreen::class.java).apply {
                                putExtra("callId", callId)
                                putExtra("channelName", channelName)
                                putExtra("callType", callType)
                                putExtra("isIncoming", false)
                                putExtra("otherUserId", receiverId)
                                putExtra("otherUserName", receiverName)
                                putExtra("otherUserImage", receiverImage)
                            }
                            startActivity(intent)
                            finish()
                        }
                    },
                    onFailure = { error ->
                        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
    }

    private fun getReceiverInfo(receiverId: String, callback: (String, String) -> Unit) {
        firestore.collection("users").document(receiverId)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("username") ?: "User"
                val image = document.getString("profileImageUrl") ?: ""
                callback(name, image)
            }
            .addOnFailureListener {
                callback("User", "")
            }
    }
}
