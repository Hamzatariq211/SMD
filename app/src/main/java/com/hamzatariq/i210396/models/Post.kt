package com.hamzatariq.i210396.models

data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val postImageBase64: String = "",
    val caption: String = "",
    val timestamp: Long = 0L,
    val likes: MutableMap<String, Boolean> = mutableMapOf(),
    val comments: MutableMap<String, Comment> = mutableMapOf()
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", "", "", "", 0L, mutableMapOf(), mutableMapOf())

    // Get like count
    fun getLikeCount(): Int = likes.size

    // Get comment count
    fun getCommentCount(): Int = comments.size

    // Check if user has liked the post
    fun isLikedByUser(userId: String): Boolean = likes.containsKey(userId)
}

data class Comment(
    val commentId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val commentText: String = "",
    val timestamp: Long = 0L
) {
    constructor() : this("", "", "", "", "", 0L)
}

