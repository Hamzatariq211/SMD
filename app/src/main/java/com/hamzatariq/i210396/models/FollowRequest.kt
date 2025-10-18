package com.hamzatariq.i210396.models

data class FollowRequest(
    val requestId: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromProfileImageUrl: String = "",
    val toUserId: String = "",
    val status: String = "pending", // pending, accepted, rejected
    val timestamp: Long = System.currentTimeMillis()
)

