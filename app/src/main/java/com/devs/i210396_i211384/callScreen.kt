package com.devs.i210396_i211384

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.devs.i210396_i211384.services.CallService
import com.devs.i210396_i211384.utils.AgoraConfig
import com.devs.i210396_i211384.utils.AgoraTokenGenerator
import com.devs.i210396_i211384.utils.ImageUtils
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class callScreen : AppCompatActivity() {
    private var mRtcEngine: RtcEngine? = null
    private lateinit var localVideoContainer: FrameLayout
    private lateinit var remoteVideoContainer: FrameLayout
    private lateinit var profileImage: ImageView
    private lateinit var nameText: TextView
    private lateinit var durationText: TextView
    private lateinit var callStatusText: TextView

    private var callId: String = ""
    private var channelName: String = ""
    private var callType: String = "video"
    private var isIncoming: Boolean = false
    private var otherUserId: String = ""
    private var otherUserName: String = ""
    private var otherUserImage: String = ""

    private var isMuted = false
    private var isSpeakerOn = true
    private var isCameraOn = true
    private var isFrontCamera = true

    private var callStartTime: Long = 0
    private var durationHandler: Handler? = null
    private var durationRunnable: Runnable? = null
    private var connectionTimeoutHandler: Handler? = null
    private var isChannelJoined: Boolean = false

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )
    private val CONNECTION_TIMEOUT_MS = 30000L // 30 second timeout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_call)

        // Get call data from intent
        callId = intent.getStringExtra("callId") ?: ""
        channelName = intent.getStringExtra("channelName") ?: ""
        callType = intent.getStringExtra("callType") ?: "video"
        isIncoming = intent.getBooleanExtra("isIncoming", false)
        otherUserId = intent.getStringExtra("otherUserId") ?: ""
        otherUserName = intent.getStringExtra("otherUserName") ?: "User"
        otherUserImage = intent.getStringExtra("otherUserImage") ?: ""

        // Channel name is already properly formatted from AgoraConfig.generateChannelName
        // Do NOT sanitize it again - token is generated based on the exact channel name

        android.util.Log.d("CallScreen", "onCreate - channelName: $channelName, callType: $callType, otherUserId: $otherUserId")

        initViews()

        if (checkSelfPermission()) {
            initializeAndJoinChannel()
            startConnectionTimeout()
        }

        setupCallStatusListener()
    }

    private fun startConnectionTimeout() {
        connectionTimeoutHandler = Handler(Looper.getMainLooper())
        connectionTimeoutHandler?.postDelayed({
            if (!isChannelJoined) {
                android.util.Log.e("CallScreen", "Connection timeout - channel join failed")
                Toast.makeText(this, "Connection timeout. Please try again.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }, CONNECTION_TIMEOUT_MS)
    }

    private fun cancelConnectionTimeout() {
        connectionTimeoutHandler?.removeCallbacksAndMessages(null)
        connectionTimeoutHandler = null
    }

    private fun initViews() {
        localVideoContainer = findViewById(R.id.localVideoContainer)
        remoteVideoContainer = findViewById(R.id.remoteVideoContainer)
        profileImage = findViewById(R.id.ivProfile)
        nameText = findViewById(R.id.tvName)
        durationText = findViewById(R.id.tvDuration)
        callStatusText = findViewById(R.id.tvCallStatus)

        nameText.text = otherUserName
        if (otherUserImage.isNotEmpty()) {
            ImageUtils.loadBase64Image(profileImage, otherUserImage)
        }

        // Initially hide video containers for audio call
        if (callType == "audio") {
            localVideoContainer.visibility = View.GONE
            remoteVideoContainer.visibility = View.GONE
            profileImage.visibility = View.VISIBLE
        } else {
            profileImage.visibility = View.GONE
        }

        callStatusText.text = if (isIncoming) "Connecting..." else "Calling..."
        durationText.text = "00:00"

        // Setup buttons
        findViewById<ImageView>(R.id.ivEndCall).setOnClickListener {
            endCall()
        }

        findViewById<ImageView>(R.id.ivMute)?.setOnClickListener {
            toggleMute()
        }

        findViewById<ImageView>(R.id.ivSpeaker).setOnClickListener {
            toggleSpeaker()
        }

        findViewById<ImageView>(R.id.ivSwitchCamera)?.setOnClickListener {
            switchCamera()
        }

        findViewById<ImageView>(R.id.ivToggleVideo)?.setOnClickListener {
            toggleVideo()
        }
    }

    private fun checkSelfPermission(): Boolean {
        val permissionsNeeded = REQUESTED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQ_ID)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeAndJoinChannel()
            } else {
                Toast.makeText(this, "Permissions required for call", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initializeAndJoinChannel() {
        try {
            // Validate APP_ID
            if (AgoraConfig.APP_ID.isEmpty() || AgoraConfig.APP_ID.contains("YOUR_AGORA")) {
                Toast.makeText(this, "Invalid Agora App ID. Please check AgoraConfig.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            val config = RtcEngineConfig().apply {
                mContext = applicationContext
                mAppId = AgoraConfig.APP_ID
                mEventHandler = rtcEventHandler
            }

            mRtcEngine = RtcEngine.create(config)

            if (mRtcEngine == null) {
                Toast.makeText(this, "Failed to create RTC Engine", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            if (callType == "video") {
                mRtcEngine?.enableVideo()
                setupLocalVideo()
            } else {
                mRtcEngine?.disableVideo()
            }

            mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)

            val options = ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            }

            // Generate unique UID based on user ID
            // Ensure UID is always > 0 to avoid invalid channel join
            var uid = otherUserId.hashCode() and 0x7FFFFFFF
            if (uid == 0) {
                uid = 1 // Default to 1 if hash results in 0
            }

            android.util.Log.d("CallScreen", "=== TOKEN GENERATION ===")
            android.util.Log.d("CallScreen", "APP_ID: ${AgoraConfig.APP_ID}")
            android.util.Log.d("CallScreen", "Channel Name: $channelName")
            android.util.Log.d("CallScreen", "UID: $uid (from otherUserId: $otherUserId)")

            // Generate token locally using AgoraTokenGenerator
            val token = AgoraTokenGenerator.generateToken(
                channelName = channelName,
                uid = uid,
                role = 1, // Broadcaster role
                privilegeExpiredTs = AgoraTokenGenerator.getExpirationTimestamp(3600) // 1 hour
            )

            if (token.isEmpty()) {
                android.util.Log.e("CallScreen", "❌ Failed to generate token locally")
                Toast.makeText(this, "Failed to generate token", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            android.util.Log.d("CallScreen", "✅ Token generated successfully!")
            android.util.Log.d("CallScreen", "Token length: ${token.length}")
            android.util.Log.d("CallScreen", "Token preview: ${token.take(50)}...")

            // Join channel with the generated token
            joinChannelWithToken(token, channelName, uid, options)

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to initialize call: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("CallScreen", "Exception in initializeAndJoinChannel: ${e.message}", e)
            finish()
        }
    }

    private fun fetchTokenFromServer(channelName: String, uid: Int, options: ChannelMediaOptions) {
        // This method is kept as a fallback but should not be called
        // Token generation now happens locally via AgoraTokenGenerator
        android.util.Log.d("CallScreen", "fetchTokenFromServer called - using local generation instead")

        val token = AgoraTokenGenerator.generateToken(
            channelName = channelName,
            uid = uid,
            role = 1,
            privilegeExpiredTs = AgoraTokenGenerator.getExpirationTimestamp(3600)
        )

        if (token.isNotEmpty() && token.length > 50) {
            android.util.Log.d("CallScreen", "✅ Token generated successfully!")
            joinChannelWithToken(token, channelName, uid, options)
        } else {
            android.util.Log.e("CallScreen", "❌ Invalid token generated")
            runOnUiThread {
                Toast.makeText(this@callScreen, "Invalid token generated", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun joinChannelWithToken(token: String, channelName: String, uid: Int, options: ChannelMediaOptions) {
        runOnUiThread {
            try {
                // Final validation before joining
                if (mRtcEngine == null) {
                    android.util.Log.e("CallScreen", "❌ RtcEngine is null! Cannot join channel")
                    Toast.makeText(this@callScreen, "Engine not initialized", Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }

                if (token.isEmpty()) {
                    android.util.Log.e("CallScreen", "❌ Token is empty!")
                    Toast.makeText(this@callScreen, "Invalid token", Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }

                if (channelName.isEmpty()) {
                    android.util.Log.e("CallScreen", "❌ Channel name is empty!")
                    Toast.makeText(this@callScreen, "Invalid channel", Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }

                if (uid <= 0) {
                    android.util.Log.e("CallScreen", "❌ UID is invalid: $uid")
                    Toast.makeText(this@callScreen, "Invalid UID", Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }

                android.util.Log.d("CallScreen", "=== JOINING CHANNEL ===")
                android.util.Log.d("CallScreen", "Channel: $channelName")
                android.util.Log.d("CallScreen", "UID: $uid")
                android.util.Log.d("CallScreen", "Token length: ${token.length}")
                android.util.Log.d("CallScreen", "Token starts with: ${token.take(20)}...")
                android.util.Log.d("CallScreen", "Token format check: ${token.substring(0, 3)} (should be 007 or 006)")

                val result = mRtcEngine?.joinChannel(token, channelName, uid, options)

                if (result != 0) {
                    android.util.Log.e("CallScreen", "❌ joinChannel failed with error code: $result")
                    android.util.Log.e("CallScreen", "Error meanings:")
                    android.util.Log.e("CallScreen", "  -102 = Token invalid or credentials mismatch")
                    android.util.Log.e("CallScreen", "  -3 = Invalid channel name")
                    android.util.Log.e("CallScreen", "  -8 = Not initialized")
                    Toast.makeText(this@callScreen, "Failed to join channel. Error code: $result", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    android.util.Log.d("CallScreen", "✅ joinChannel successful!")
                }
            } catch (e: Exception) {
                android.util.Log.e("CallScreen", "Exception in joinChannelWithToken: ${e.message}", e)
                Toast.makeText(this@callScreen, "Error joining channel: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupLocalVideo() {
        val surfaceView = SurfaceView(this)
        localVideoContainer.addView(surfaceView)
        mRtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        mRtcEngine?.startPreview()
    }

    private fun setupRemoteVideo(uid: Int) {
        runOnUiThread {
            if (callType == "video") {
                profileImage.visibility = View.GONE
                remoteVideoContainer.visibility = View.VISIBLE

                val surfaceView = SurfaceView(this)
                remoteVideoContainer.addView(surfaceView)
                mRtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
            }

            callStatusText.visibility = View.GONE
            startCallDuration()
        }
    }

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                remoteVideoContainer.removeAllViews()
                endCall()
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                callStatusText.text = "Connected"
                isChannelJoined = true
                // Cancel connection timeout
                cancelConnectionTimeout()
            }
        }

        override fun onError(err: Int) {
            runOnUiThread {
                Toast.makeText(this@callScreen, "Call error: $err", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleMute() {
        isMuted = !isMuted
        mRtcEngine?.muteLocalAudioStream(isMuted)
        findViewById<ImageView>(R.id.ivMute)?.setImageResource(
            if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic_on
        )
    }

    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        mRtcEngine?.setEnableSpeakerphone(isSpeakerOn)
        findViewById<ImageView>(R.id.ivSpeaker).setImageResource(
            if (isSpeakerOn) R.drawable.speaker else R.drawable.ic_speaker_off
        )
    }

    private fun switchCamera() {
        if (callType == "video") {
            mRtcEngine?.switchCamera()
            isFrontCamera = !isFrontCamera
        }
    }

    private fun toggleVideo() {
        if (callType == "video") {
            isCameraOn = !isCameraOn
            mRtcEngine?.muteLocalVideoStream(!isCameraOn)
            localVideoContainer.visibility = if (isCameraOn) View.VISIBLE else View.GONE
            findViewById<ImageView>(R.id.ivToggleVideo)?.setImageResource(
                if (isCameraOn) R.drawable.ic_video_on else R.drawable.ic_video_off
            )
        }
    }

    private fun startCallDuration() {
        callStartTime = System.currentTimeMillis()
        durationHandler = Handler(Looper.getMainLooper())
        durationRunnable = object : Runnable {
            override fun run() {
                val elapsed = (System.currentTimeMillis() - callStartTime) / 1000
                val minutes = elapsed / 60
                val seconds = elapsed % 60
                durationText.text = String.format("%02d:%02d", minutes, seconds)
                durationHandler?.postDelayed(this, 1000)
            }
        }
        durationHandler?.post(durationRunnable!!)
    }

    private fun setupCallStatusListener() {
        CallService.listenToCallStatus(callId) { status ->
            runOnUiThread {
                when (status) {
                    "rejected" -> {
                        Toast.makeText(this, "Call rejected", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    "ended" -> {
                        finish()
                    }
                }
            }
        }
    }

    private fun endCall() {
        CallService.updateCallStatus(callId, "ended")
        CallService.endCall(callId)
        leaveChannel()
        finish()
    }

    private fun leaveChannel() {
        mRtcEngine?.leaveChannel()
        mRtcEngine?.stopPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        durationHandler?.removeCallbacks(durationRunnable!!)
        leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
    }
}
