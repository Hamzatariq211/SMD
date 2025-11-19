package com.devs.i210396_i211384.models

data class StoryModel(
    val storyId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val storyImageBase64: String = "",
    val timestamp: Long = 0L,
    val expiryTime: Long = 0L,
    val viewCount: Int = 0
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", "", "", 0L, 0L, 0)

    // Check if story is expired (24 hours)
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiryTime
    }
}

// Model for user's story collection
data class UserStoryCollection(
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val stories: MutableList<StoryModel> = mutableListOf(),
    val lastUpdated: Long = 0L
) {
    constructor() : this("", "", "", mutableListOf(), 0L)

    // Check if all stories are expired
    fun hasActiveStories(): Boolean {
        return stories.any { !it.isExpired() }
    }

    // Get non-expired stories
    fun getActiveStories(): List<StoryModel> {
        return stories.filter { !it.isExpired() }
    }
}
