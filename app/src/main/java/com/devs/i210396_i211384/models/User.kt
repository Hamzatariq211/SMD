package com.devs.i210396_i211384.models

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val profileImageUrl: String = "",
    val bio: String = "",
    val isProfileSetup: Boolean = false,
    val isPrivate: Boolean = false,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
