package com.devs.i210396_i211384.models

data class FollowRequest(
    val requestId: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromProfileImageUrl: String = "",
    val toUserId: String = "",
    val status: String = "pending", // pending, accepted, rejected
    val timestamp: Long = System.currentTimeMillis()
)

