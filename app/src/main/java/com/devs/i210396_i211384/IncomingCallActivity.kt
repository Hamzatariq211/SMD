package com.devs.i210396_i211384

import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.devs.i210396_i211384.models.CallRequest
import com.devs.i210396_i211384.services.CallService
import com.devs.i210396_i211384.utils.ImageUtils

class IncomingCallActivity : AppCompatActivity() {
    private lateinit var callRequest: CallRequest
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        // Get call data from intent
        val callId = intent.getStringExtra("callId") ?: ""
        val callerId = intent.getStringExtra("callerId") ?: ""
        val callerName = intent.getStringExtra("callerName") ?: "Unknown"
        val callerImage = intent.getStringExtra("callerImageUrl") ?: ""
        val callType = intent.getStringExtra("callType") ?: "video"
        val channelName = intent.getStringExtra("channelName") ?: ""

        // If data is missing, try to get from Firebase
        if (callId.isNotEmpty() && callerId.isEmpty()) {
            loadCallDataFromFirebase(callId)
            return
        }

        callRequest = CallRequest(
            callId = callId,
            callerId = callerId,
            callerName = callerName,
            callerImageUrl = callerImage,
            receiverId = "",
            callType = callType,
            channelName = channelName,
            status = "ringing"
        )

        setupUI()
        playRingtone()

        // Listen for call cancellation
        CallService.listenToCallStatus(callId) { status ->
            if (status == "ended" || status == "cancelled") {
                stopRingtone()
                finish()
            }
        }
    }

    private fun loadCallDataFromFirebase(callId: String) {
        CallService.getCallData(callId) { call ->
            if (call != null) {
                callRequest = call
                setupUI()
                playRingtone()

                // Listen for call cancellation
                CallService.listenToCallStatus(callId) { status ->
                    if (status == "ended" || status == "cancelled") {
                        stopRingtone()
                        finish()
                    }
                }
            } else {
                finish()
            }
        }
    }

    private fun setupUI() {
        findViewById<TextView>(R.id.tvCallerName).text = callRequest.callerName
        findViewById<TextView>(R.id.tvCallType).text =
            if (callRequest.callType == "video") "Incoming Video Call" else "Incoming Voice Call"

        val profileImage = findViewById<ImageView>(R.id.ivCallerProfile)
        if (callRequest.callerImageUrl.isNotEmpty()) {
            ImageUtils.loadBase64Image(profileImage, callRequest.callerImageUrl)
        }

        // Accept button
        findViewById<ImageView>(R.id.ivAcceptCall).setOnClickListener {
            acceptCall()
        }

        // Reject button
        findViewById<ImageView>(R.id.ivRejectCall).setOnClickListener {
            rejectCall()
        }
    }

    private fun acceptCall() {
        stopRingtone()
        CallService.updateCallStatus(callRequest.callId, "accepted")

        // Start call screen
        val intent = Intent(this, callScreen::class.java).apply {
            putExtra("callId", callRequest.callId)
            putExtra("channelName", callRequest.channelName)
            putExtra("callType", callRequest.callType)
            putExtra("isIncoming", true)
            putExtra("otherUserId", callRequest.callerId)
            putExtra("otherUserName", callRequest.callerName)
            putExtra("otherUserImage", callRequest.callerImageUrl)
        }
        startActivity(intent)
        finish()
    }

    private fun rejectCall() {
        stopRingtone()
        CallService.updateCallStatus(callRequest.callId, "rejected")
        finish()
    }

    private fun playRingtone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@IncomingCallActivity, ringtoneUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRingtone() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
    }
}
