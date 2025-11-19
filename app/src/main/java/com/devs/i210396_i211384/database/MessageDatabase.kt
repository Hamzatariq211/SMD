package com.devs.i210396_i211384.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.devs.i210396_i211384.models.ChatMessage

class MessageDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "instagram_messages.db"
        private const val DATABASE_VERSION = 1

        // Messages table
        private const val TABLE_MESSAGES = "messages"
        private const val COLUMN_MESSAGE_ID = "message_id"
        private const val COLUMN_CHAT_ROOM_ID = "chat_room_id"
        private const val COLUMN_SENDER_ID = "sender_id"
        private const val COLUMN_RECEIVER_ID = "receiver_id"
        private const val COLUMN_MESSAGE_TEXT = "message_text"
        private const val COLUMN_MESSAGE_TYPE = "message_type"
        private const val COLUMN_IMAGE_BASE64 = "image_base64"
        private const val COLUMN_POST_ID = "post_id"
        private const val COLUMN_IS_EDITED = "is_edited"
        private const val COLUMN_EDITED_AT = "edited_at"
        private const val COLUMN_IS_DELETED = "is_deleted"
        private const val COLUMN_IS_SEEN = "is_seen"
        private const val COLUMN_TIMESTAMP = "timestamp"

        // Chat rooms table
        private const val TABLE_CHAT_ROOMS = "chat_rooms"
        private const val COLUMN_ROOM_ID = "room_id"
        private const val COLUMN_USER1_ID = "user1_id"
        private const val COLUMN_USER2_ID = "user2_id"
        private const val COLUMN_LAST_MESSAGE = "last_message"
        private const val COLUMN_LAST_MESSAGE_TIME = "last_message_time"
        private const val COLUMN_UNREAD_COUNT = "unread_count"

        @Volatile
        private var INSTANCE: MessageDatabase? = null

        fun getInstance(context: Context): MessageDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MessageDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create messages table
        val createMessagesTable = """
            CREATE TABLE $TABLE_MESSAGES (
                $COLUMN_MESSAGE_ID TEXT PRIMARY KEY,
                $COLUMN_CHAT_ROOM_ID TEXT NOT NULL,
                $COLUMN_SENDER_ID TEXT NOT NULL,
                $COLUMN_RECEIVER_ID TEXT NOT NULL,
                $COLUMN_MESSAGE_TEXT TEXT,
                $COLUMN_MESSAGE_TYPE TEXT DEFAULT 'text',
                $COLUMN_IMAGE_BASE64 TEXT,
                $COLUMN_POST_ID TEXT,
                $COLUMN_IS_EDITED INTEGER DEFAULT 0,
                $COLUMN_EDITED_AT INTEGER DEFAULT 0,
                $COLUMN_IS_DELETED INTEGER DEFAULT 0,
                $COLUMN_IS_SEEN INTEGER DEFAULT 0,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createMessagesTable)

        // Create chat rooms table
        val createChatRoomsTable = """
            CREATE TABLE $TABLE_CHAT_ROOMS (
                $COLUMN_ROOM_ID TEXT PRIMARY KEY,
                $COLUMN_USER1_ID TEXT NOT NULL,
                $COLUMN_USER2_ID TEXT NOT NULL,
                $COLUMN_LAST_MESSAGE TEXT,
                $COLUMN_LAST_MESSAGE_TIME INTEGER DEFAULT 0,
                $COLUMN_UNREAD_COUNT INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createChatRoomsTable)

        // Create indexes for performance
        db.execSQL("CREATE INDEX idx_chat_room ON $TABLE_MESSAGES($COLUMN_CHAT_ROOM_ID)")
        db.execSQL("CREATE INDEX idx_timestamp ON $TABLE_MESSAGES($COLUMN_TIMESTAMP)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT_ROOMS")
        onCreate(db)
    }

    // Insert or update message
    fun saveMessage(message: ChatMessage, chatRoomId: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MESSAGE_ID, message.messageId)
            put(COLUMN_CHAT_ROOM_ID, chatRoomId)
            put(COLUMN_SENDER_ID, message.senderId)
            put(COLUMN_RECEIVER_ID, message.receiverId)
            put(COLUMN_MESSAGE_TEXT, message.messageText)
            put(COLUMN_MESSAGE_TYPE, message.messageType)
            put(COLUMN_IMAGE_BASE64, message.imageBase64)
            put(COLUMN_POST_ID, message.postId)
            put(COLUMN_IS_EDITED, if (message.isEdited) 1 else 0)
            put(COLUMN_EDITED_AT, message.editedAt)
            put(COLUMN_IS_DELETED, if (message.isDeleted) 1 else 0)
            put(COLUMN_IS_SEEN, 0)
            put(COLUMN_TIMESTAMP, message.timestamp)
        }

        db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    // Save multiple messages
    fun saveMessages(messages: List<ChatMessage>, chatRoomId: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (message in messages) {
                val values = ContentValues().apply {
                    put(COLUMN_MESSAGE_ID, message.messageId)
                    put(COLUMN_CHAT_ROOM_ID, chatRoomId)
                    put(COLUMN_SENDER_ID, message.senderId)
                    put(COLUMN_RECEIVER_ID, message.receiverId)
                    put(COLUMN_MESSAGE_TEXT, message.messageText)
                    put(COLUMN_MESSAGE_TYPE, message.messageType)
                    put(COLUMN_IMAGE_BASE64, message.imageBase64)
                    put(COLUMN_POST_ID, message.postId)
                    put(COLUMN_IS_EDITED, if (message.isEdited) 1 else 0)
                    put(COLUMN_EDITED_AT, message.editedAt)
                    put(COLUMN_IS_DELETED, if (message.isDeleted) 1 else 0)
                    put(COLUMN_IS_SEEN, 0)
                    put(COLUMN_TIMESTAMP, message.timestamp)
                }
                db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    // Get messages for a chat room
    fun getMessages(userId1: String, userId2: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_MESSAGES,
            null,
            "($COLUMN_SENDER_ID = ? AND $COLUMN_RECEIVER_ID = ?) OR ($COLUMN_SENDER_ID = ? AND $COLUMN_RECEIVER_ID = ?)",
            arrayOf(userId1, userId2, userId2, userId1),
            null,
            null,
            "$COLUMN_TIMESTAMP ASC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val message = ChatMessage(
                        messageId = it.getString(it.getColumnIndexOrThrow(COLUMN_MESSAGE_ID)),
                        senderId = it.getString(it.getColumnIndexOrThrow(COLUMN_SENDER_ID)),
                        receiverId = it.getString(it.getColumnIndexOrThrow(COLUMN_RECEIVER_ID)),
                        messageText = it.getString(it.getColumnIndexOrThrow(COLUMN_MESSAGE_TEXT)) ?: "",
                        messageType = it.getString(it.getColumnIndexOrThrow(COLUMN_MESSAGE_TYPE)) ?: "text",
                        imageBase64 = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE_BASE64)) ?: "",
                        postId = it.getString(it.getColumnIndexOrThrow(COLUMN_POST_ID)) ?: "",
                        isEdited = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_EDITED)) == 1,
                        editedAt = it.getLong(it.getColumnIndexOrThrow(COLUMN_EDITED_AT)),
                        isDeleted = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_DELETED)) == 1,
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    )
                    messages.add(message)
                } while (it.moveToNext())
            }
        }
        db.close()
        return messages
    }

    // Update message (for edit/delete)
    fun updateMessage(messageId: String, messageText: String? = null, isEdited: Boolean? = null, isDeleted: Boolean? = null) {
        val db = writableDatabase
        val values = ContentValues()

        messageText?.let { values.put(COLUMN_MESSAGE_TEXT, it) }
        isEdited?.let { values.put(COLUMN_IS_EDITED, if (it) 1 else 0) }
        isDeleted?.let { values.put(COLUMN_IS_DELETED, if (it) 1 else 0) }

        if (isEdited == true) {
            values.put(COLUMN_EDITED_AT, System.currentTimeMillis())
        }

        db.update(TABLE_MESSAGES, values, "$COLUMN_MESSAGE_ID = ?", arrayOf(messageId))
        db.close()
    }

    // Delete messages for a chat
    fun deleteMessagesForChat(userId1: String, userId2: String) {
        val db = writableDatabase
        db.delete(
            TABLE_MESSAGES,
            "($COLUMN_SENDER_ID = ? AND $COLUMN_RECEIVER_ID = ?) OR ($COLUMN_SENDER_ID = ? AND $COLUMN_RECEIVER_ID = ?)",
            arrayOf(userId1, userId2, userId2, userId1)
        )
        db.close()
    }

    // Clear all messages
    fun clearAllMessages() {
        val db = writableDatabase
        db.delete(TABLE_MESSAGES, null, null)
        db.delete(TABLE_CHAT_ROOMS, null, null)
        db.close()
    }
}

