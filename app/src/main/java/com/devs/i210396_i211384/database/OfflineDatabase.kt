package com.devs.i210396_i211384.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.devs.i210396_i211384.models.ChatMessage
import org.json.JSONObject

class OfflineDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "instagram_offline.db"
        private const val DATABASE_VERSION = 3

        // Messages table
        private const val TABLE_MESSAGES = "messages"
        private const val COL_MSG_ID = "message_id"
        private const val COL_MSG_CHAT_ROOM_ID = "chat_room_id"
        private const val COL_MSG_SENDER_ID = "sender_id"
        private const val COL_MSG_RECEIVER_ID = "receiver_id"
        private const val COL_MSG_TEXT = "message_text"
        private const val COL_MSG_TYPE = "message_type"
        private const val COL_MSG_IMAGE = "image_base64"
        private const val COL_MSG_POST_ID = "post_id"
        private const val COL_MSG_IS_EDITED = "is_edited"
        private const val COL_MSG_EDITED_AT = "edited_at"
        private const val COL_MSG_IS_DELETED = "is_deleted"
        private const val COL_MSG_TIMESTAMP = "timestamp"

        // Posts table
        private const val TABLE_POSTS = "posts"
        private const val COL_POST_ID = "post_id"
        private const val COL_POST_USER_ID = "user_id"
        private const val COL_POST_USERNAME = "username"
        private const val COL_POST_USER_IMAGE = "user_profile_image"
        private const val COL_POST_IMAGE = "post_image_base64"
        private const val COL_POST_CAPTION = "caption"
        private const val COL_POST_LIKE_COUNT = "like_count"
        private const val COL_POST_COMMENT_COUNT = "comment_count"
        private const val COL_POST_TIMESTAMP = "timestamp"
        private const val COL_POST_IS_LIKED = "is_liked"

        // Stories table
        private const val TABLE_STORIES = "stories"
        private const val COL_STORY_ID = "story_id"
        private const val COL_STORY_USER_ID = "user_id"
        private const val COL_STORY_USERNAME = "username"
        private const val COL_STORY_USER_IMAGE = "user_profile_image"
        private const val COL_STORY_IMAGE = "story_image_base64"
        private const val COL_STORY_TIMESTAMP = "timestamp"
        private const val COL_STORY_EXPIRY = "expiry_time"
        private const val COL_STORY_VIEW_COUNT = "view_count"

        // Pending actions table (for offline queue)
        private const val TABLE_PENDING_ACTIONS = "pending_actions"
        private const val COL_ACTION_ID = "action_id"
        private const val COL_ACTION_TYPE = "action_type"
        private const val COL_ACTION_DATA = "action_data"
        private const val COL_ACTION_TIMESTAMP = "action_timestamp"
        private const val COL_ACTION_RETRY_COUNT = "retry_count"
        private const val COL_ACTION_STATUS = "status"

        @Volatile
        private var INSTANCE: OfflineDatabase? = null

        fun getInstance(context: Context): OfflineDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OfflineDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create messages table
        db.execSQL("""
            CREATE TABLE $TABLE_MESSAGES (
                $COL_MSG_ID TEXT PRIMARY KEY,
                $COL_MSG_CHAT_ROOM_ID TEXT NOT NULL,
                $COL_MSG_SENDER_ID TEXT NOT NULL,
                $COL_MSG_RECEIVER_ID TEXT NOT NULL,
                $COL_MSG_TEXT TEXT,
                $COL_MSG_TYPE TEXT DEFAULT 'text',
                $COL_MSG_IMAGE TEXT,
                $COL_MSG_POST_ID TEXT,
                $COL_MSG_IS_EDITED INTEGER DEFAULT 0,
                $COL_MSG_EDITED_AT INTEGER DEFAULT 0,
                $COL_MSG_IS_DELETED INTEGER DEFAULT 0,
                $COL_MSG_TIMESTAMP INTEGER NOT NULL
            )
        """)

        // Create posts table
        db.execSQL("""
            CREATE TABLE $TABLE_POSTS (
                $COL_POST_ID TEXT PRIMARY KEY,
                $COL_POST_USER_ID TEXT NOT NULL,
                $COL_POST_USERNAME TEXT,
                $COL_POST_USER_IMAGE TEXT,
                $COL_POST_IMAGE TEXT,
                $COL_POST_CAPTION TEXT,
                $COL_POST_LIKE_COUNT INTEGER DEFAULT 0,
                $COL_POST_COMMENT_COUNT INTEGER DEFAULT 0,
                $COL_POST_TIMESTAMP INTEGER NOT NULL,
                $COL_POST_IS_LIKED INTEGER DEFAULT 0
            )
        """)

        // Create stories table
        db.execSQL("""
            CREATE TABLE $TABLE_STORIES (
                $COL_STORY_ID TEXT PRIMARY KEY,
                $COL_STORY_USER_ID TEXT NOT NULL,
                $COL_STORY_USERNAME TEXT,
                $COL_STORY_USER_IMAGE TEXT,
                $COL_STORY_IMAGE TEXT,
                $COL_STORY_TIMESTAMP INTEGER NOT NULL,
                $COL_STORY_EXPIRY INTEGER NOT NULL,
                $COL_STORY_VIEW_COUNT INTEGER DEFAULT 0
            )
        """)

        // Create pending actions table
        db.execSQL("""
            CREATE TABLE $TABLE_PENDING_ACTIONS (
                $COL_ACTION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ACTION_TYPE TEXT NOT NULL,
                $COL_ACTION_DATA TEXT NOT NULL,
                $COL_ACTION_TIMESTAMP INTEGER NOT NULL,
                $COL_ACTION_RETRY_COUNT INTEGER DEFAULT 0,
                $COL_ACTION_STATUS TEXT DEFAULT 'pending'
            )
        """)

        // Create indexes
        db.execSQL("CREATE INDEX idx_msg_chat_room ON $TABLE_MESSAGES($COL_MSG_CHAT_ROOM_ID)")
        db.execSQL("CREATE INDEX idx_msg_timestamp ON $TABLE_MESSAGES($COL_MSG_TIMESTAMP)")
        db.execSQL("CREATE INDEX idx_post_timestamp ON $TABLE_POSTS($COL_POST_TIMESTAMP)")
        db.execSQL("CREATE INDEX idx_story_expiry ON $TABLE_STORIES($COL_STORY_EXPIRY)")
        db.execSQL("CREATE INDEX idx_action_status ON $TABLE_PENDING_ACTIONS($COL_ACTION_STATUS)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POSTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_ACTIONS")
        onCreate(db)
    }

    // ==================== MESSAGES ====================

    fun saveMessage(message: ChatMessage, chatRoomId: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_MSG_ID, message.messageId)
            put(COL_MSG_CHAT_ROOM_ID, chatRoomId)
            put(COL_MSG_SENDER_ID, message.senderId)
            put(COL_MSG_RECEIVER_ID, message.receiverId)
            put(COL_MSG_TEXT, message.messageText)
            put(COL_MSG_TYPE, message.messageType)
            put(COL_MSG_IMAGE, message.imageBase64)
            put(COL_MSG_POST_ID, message.postId)
            put(COL_MSG_IS_EDITED, if (message.isEdited) 1 else 0)
            put(COL_MSG_EDITED_AT, message.editedAt)
            put(COL_MSG_IS_DELETED, if (message.isDeleted) 1 else 0)
            put(COL_MSG_TIMESTAMP, message.timestamp)
        }
        db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getMessages(userId1: String, userId2: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MESSAGES,
            null,
            "($COL_MSG_SENDER_ID = ? AND $COL_MSG_RECEIVER_ID = ?) OR ($COL_MSG_SENDER_ID = ? AND $COL_MSG_RECEIVER_ID = ?)",
            arrayOf(userId1, userId2, userId2, userId1),
            null, null,
            "$COL_MSG_TIMESTAMP ASC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    messages.add(ChatMessage(
                        messageId = it.getString(it.getColumnIndexOrThrow(COL_MSG_ID)),
                        senderId = it.getString(it.getColumnIndexOrThrow(COL_MSG_SENDER_ID)),
                        receiverId = it.getString(it.getColumnIndexOrThrow(COL_MSG_RECEIVER_ID)),
                        messageText = it.getString(it.getColumnIndexOrThrow(COL_MSG_TEXT)) ?: "",
                        messageType = it.getString(it.getColumnIndexOrThrow(COL_MSG_TYPE)) ?: "text",
                        imageBase64 = it.getString(it.getColumnIndexOrThrow(COL_MSG_IMAGE)) ?: "",
                        postId = it.getString(it.getColumnIndexOrThrow(COL_MSG_POST_ID)) ?: "",
                        isEdited = it.getInt(it.getColumnIndexOrThrow(COL_MSG_IS_EDITED)) == 1,
                        editedAt = it.getLong(it.getColumnIndexOrThrow(COL_MSG_EDITED_AT)),
                        isDeleted = it.getInt(it.getColumnIndexOrThrow(COL_MSG_IS_DELETED)) == 1,
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_MSG_TIMESTAMP))
                    ))
                } while (it.moveToNext())
            }
        }
        return messages
    }

    // ==================== POSTS ====================

    fun savePost(postId: String, userId: String, username: String, userImage: String,
                 postImage: String, caption: String, likeCount: Int, commentCount: Int,
                 timestamp: Long, isLiked: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_POST_ID, postId)
            put(COL_POST_USER_ID, userId)
            put(COL_POST_USERNAME, username)
            put(COL_POST_USER_IMAGE, userImage)
            put(COL_POST_IMAGE, postImage)
            put(COL_POST_CAPTION, caption)
            put(COL_POST_LIKE_COUNT, likeCount)
            put(COL_POST_COMMENT_COUNT, commentCount)
            put(COL_POST_TIMESTAMP, timestamp)
            put(COL_POST_IS_LIKED, if (isLiked) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_POSTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getPosts(limit: Int = 50): List<Map<String, Any>> {
        val posts = mutableListOf<Map<String, Any>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_POSTS, null, null, null, null, null,
            "$COL_POST_TIMESTAMP DESC", limit.toString()
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    posts.add(mapOf(
                        "postId" to it.getString(it.getColumnIndexOrThrow(COL_POST_ID)),
                        "userId" to it.getString(it.getColumnIndexOrThrow(COL_POST_USER_ID)),
                        "username" to it.getString(it.getColumnIndexOrThrow(COL_POST_USERNAME)),
                        "userProfileImage" to it.getString(it.getColumnIndexOrThrow(COL_POST_USER_IMAGE)),
                        "postImageBase64" to it.getString(it.getColumnIndexOrThrow(COL_POST_IMAGE)),
                        "caption" to it.getString(it.getColumnIndexOrThrow(COL_POST_CAPTION)),
                        "likeCount" to it.getInt(it.getColumnIndexOrThrow(COL_POST_LIKE_COUNT)),
                        "commentCount" to it.getInt(it.getColumnIndexOrThrow(COL_POST_COMMENT_COUNT)),
                        "timestamp" to it.getLong(it.getColumnIndexOrThrow(COL_POST_TIMESTAMP)),
                        "isLiked" to (it.getInt(it.getColumnIndexOrThrow(COL_POST_IS_LIKED)) == 1)
                    ))
                } while (it.moveToNext())
            }
        }
        return posts
    }

    // ==================== STORIES ====================

    fun saveStory(storyId: String, userId: String, username: String, userImage: String,
                  storyImage: String, timestamp: Long, expiryTime: Long, viewCount: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_STORY_ID, storyId)
            put(COL_STORY_USER_ID, userId)
            put(COL_STORY_USERNAME, username)
            put(COL_STORY_USER_IMAGE, userImage)
            put(COL_STORY_IMAGE, storyImage)
            put(COL_STORY_TIMESTAMP, timestamp)
            put(COL_STORY_EXPIRY, expiryTime)
            put(COL_STORY_VIEW_COUNT, viewCount)
        }
        db.insertWithOnConflict(TABLE_STORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getActiveStories(): List<Map<String, Any>> {
        val stories = mutableListOf<Map<String, Any>>()
        val db = readableDatabase
        val currentTime = System.currentTimeMillis()

        val cursor = db.query(
            TABLE_STORIES, null,
            "$COL_STORY_EXPIRY > ?", arrayOf(currentTime.toString()),
            null, null, "$COL_STORY_TIMESTAMP DESC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    stories.add(mapOf(
                        "storyId" to it.getString(it.getColumnIndexOrThrow(COL_STORY_ID)),
                        "userId" to it.getString(it.getColumnIndexOrThrow(COL_STORY_USER_ID)),
                        "username" to it.getString(it.getColumnIndexOrThrow(COL_STORY_USERNAME)),
                        "userProfileImage" to it.getString(it.getColumnIndexOrThrow(COL_STORY_USER_IMAGE)),
                        "storyImageBase64" to it.getString(it.getColumnIndexOrThrow(COL_STORY_IMAGE)),
                        "timestamp" to it.getLong(it.getColumnIndexOrThrow(COL_STORY_TIMESTAMP)),
                        "expiryTime" to it.getLong(it.getColumnIndexOrThrow(COL_STORY_EXPIRY)),
                        "viewCount" to it.getInt(it.getColumnIndexOrThrow(COL_STORY_VIEW_COUNT))
                    ))
                } while (it.moveToNext())
            }
        }
        return stories
    }

    fun deleteExpiredStories() {
        val db = writableDatabase
        val currentTime = System.currentTimeMillis()
        db.delete(TABLE_STORIES, "$COL_STORY_EXPIRY < ?", arrayOf(currentTime.toString()))
    }

    // ==================== PENDING ACTIONS QUEUE ====================

    fun addPendingAction(actionType: String, actionData: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ACTION_TYPE, actionType)
            put(COL_ACTION_DATA, actionData)
            put(COL_ACTION_TIMESTAMP, System.currentTimeMillis())
            put(COL_ACTION_RETRY_COUNT, 0)
            put(COL_ACTION_STATUS, "pending")
        }
        return db.insert(TABLE_PENDING_ACTIONS, null, values)
    }

    fun getPendingActions(): List<Map<String, Any>> {
        val actions = mutableListOf<Map<String, Any>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PENDING_ACTIONS, null,
            "$COL_ACTION_STATUS = ?", arrayOf("pending"),
            null, null, "$COL_ACTION_TIMESTAMP ASC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    actions.add(mapOf(
                        "actionId" to it.getLong(it.getColumnIndexOrThrow(COL_ACTION_ID)),
                        "actionType" to it.getString(it.getColumnIndexOrThrow(COL_ACTION_TYPE)),
                        "actionData" to it.getString(it.getColumnIndexOrThrow(COL_ACTION_DATA)),
                        "timestamp" to it.getLong(it.getColumnIndexOrThrow(COL_ACTION_TIMESTAMP)),
                        "retryCount" to it.getInt(it.getColumnIndexOrThrow(COL_ACTION_RETRY_COUNT))
                    ))
                } while (it.moveToNext())
            }
        }
        return actions
    }

    fun updateActionStatus(actionId: Long, status: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ACTION_STATUS, status)
        }
        db.update(TABLE_PENDING_ACTIONS, values, "$COL_ACTION_ID = ?", arrayOf(actionId.toString()))
    }

    fun incrementRetryCount(actionId: Long) {
        val db = writableDatabase
        db.execSQL("UPDATE $TABLE_PENDING_ACTIONS SET $COL_ACTION_RETRY_COUNT = $COL_ACTION_RETRY_COUNT + 1 WHERE $COL_ACTION_ID = ?", arrayOf(actionId))
    }

    fun deletePendingAction(actionId: Long) {
        val db = writableDatabase
        db.delete(TABLE_PENDING_ACTIONS, "$COL_ACTION_ID = ?", arrayOf(actionId.toString()))
    }

    fun clearOldCompletedActions() {
        val db = writableDatabase
        val twoDaysAgo = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000)
        db.delete(
            TABLE_PENDING_ACTIONS,
            "$COL_ACTION_STATUS = ? AND $COL_ACTION_TIMESTAMP < ?",
            arrayOf("completed", twoDaysAgo.toString())
        )
    }
}

