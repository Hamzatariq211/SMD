package com.hamzatariq.i210396.models

data class CallRequest(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerImageUrl: String = "",
    val receiverId: String = "",
    val callType: String = "video", // "video" or "audio"
    val channelName: String = "",
    val status: String = "ringing", // ringing, accepted, rejected, ended, missed
    val timestamp: Long = System.currentTimeMillis()
)

