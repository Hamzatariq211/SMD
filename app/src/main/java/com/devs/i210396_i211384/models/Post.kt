package com.devs.i210396_i211384.models

data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val postImageBase64: String = "",
    val caption: String = "",
    val timestamp: Long = 0L,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    // Keep for backward compatibility with Firebase if needed
    val likes: MutableMap<String, Boolean> = mutableMapOf(),
    val comments: MutableMap<String, Comment> = mutableMapOf()
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", "", "", "", 0L, 0, 0, false, mutableMapOf(), mutableMapOf())

    // Get like count (use likeCount if available, otherwise count from map)
    // Renamed to avoid clash with auto-generated getLikeCount()
    fun getTotalLikes(): Int = if (likeCount > 0) likeCount else likes.size

    // Get comment count (use commentCount if available, otherwise count from map)
    // Renamed to avoid clash with auto-generated getCommentCount()
    fun getTotalComments(): Int = if (commentCount > 0) commentCount else comments.size

    // Check if user has liked the post
    fun isLikedByUser(userId: String): Boolean = isLiked || likes.containsKey(userId)
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
