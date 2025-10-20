package com.hamzatariq.i210396

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hamzatariq.i210396.services.CallService
import com.hamzatariq.i210396.utils.AgoraConfig
import com.hamzatariq.i210396.utils.AgoraTokenGenerator
import com.hamzatariq.i210396.utils.ImageUtils
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

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_call)

        // Get call data
        callId = intent.getStringExtra("callId") ?: ""
        channelName = intent.getStringExtra("channelName") ?: ""
        callType = intent.getStringExtra("callType") ?: "video"
        isIncoming = intent.getBooleanExtra("isIncoming", false)
        otherUserId = intent.getStringExtra("otherUserId") ?: ""
        otherUserName = intent.getStringExtra("otherUserName") ?: "User"
        otherUserImage = intent.getStringExtra("otherUserImage") ?: ""

        initViews()

        if (checkSelfPermission()) {
            initializeAndJoinChannel()
        }

        setupCallStatusListener()
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

            // Generate Agora token for authentication
            val token = if (AgoraConfig.APP_CERTIFICATE.isNotEmpty()) {
                // Generate token if certificate is available
                AgoraTokenGenerator.generateToken(
                    channelName = channelName,
                    uid = 0, // Use 0 for auto-assigned UID
                    role = 1, // 1 = publisher
                    privilegeExpiredTs = 0 // Will use default 1 hour expiration
                )
            } else {
                // Use null token for testing mode (must be enabled in Agora console)
                null
            }

            android.util.Log.d("CallScreen", "Channel: $channelName")
            android.util.Log.d("CallScreen", "Token mode: ${if (token != null) "Authenticated" else "Testing (null)"}")
            if (token != null) {
                android.util.Log.d("CallScreen", "Token generated: ${if (token.isNotEmpty()) "Yes (${token.length} chars)" else "Failed"}")
                android.util.Log.d("CallScreen", "Token preview: ${token.take(50)}...")
            }

            if (token != null && token.isEmpty()) {
                Toast.makeText(this, "Failed to generate token. Check AgoraConfig.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Join channel with generated token (or null for testing mode)
            val result = mRtcEngine?.joinChannel(token, channelName, 0, options)

            android.util.Log.d("CallScreen", "joinChannel result: $result")

            if (result != 0) {
                Toast.makeText(this, "Failed to join channel. Error code: $result", Toast.LENGTH_LONG).show()
                finish()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to initialize call: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
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
