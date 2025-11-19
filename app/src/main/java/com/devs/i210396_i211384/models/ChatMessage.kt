package com.devs.i210396_i211384.models

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    val messageType: String = "text", // text, image, post
    val imageBase64: String = "",
    val postId: String = "",
    val timestamp: Long = 0L,
    val isEdited: Boolean = false,
    val editedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val isSeen: Boolean = false
) {
    constructor() : this("", "", "", "", "text", "", "", 0L, false, 0L, false, false)

    // Check if message can be edited/deleted (within 5 minutes)
    fun canEdit(): Boolean {
        val currentTime = System.currentTimeMillis()
        val fiveMinutes = 5 * 60 * 1000 // 5 minutes in milliseconds
        return (currentTime - timestamp) <= fiveMinutes && !isDeleted
    }
}

data class ChatRoom(
    val chatRoomId: String = "",
    val userId1: String = "",
    val userId2: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSenderId: String = "",
    val user1Unread: Int = 0,
    val user2Unread: Int = 0
) {
    constructor() : this("", "", "", "", 0L, "", 0, 0)

    // Get the other user's ID
    fun getOtherUserId(currentUserId: String): String {
        return if (userId1 == currentUserId) userId2 else userId1
    }

    // Get unread count for current user
    fun getUnreadCount(currentUserId: String): Int {
        return if (userId1 == currentUserId) user1Unread else user2Unread
    }
}

data class ChatUser(
    val userId: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0
) {
    constructor() : this("", "", "", "", 0L, 0)
}
