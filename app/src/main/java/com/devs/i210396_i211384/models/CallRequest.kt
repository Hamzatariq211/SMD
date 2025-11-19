package com.devs.i210396_i211384.models

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

