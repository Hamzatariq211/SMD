package com.hamzatariq.i210396

import android.app.Activity
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.hamzatariq.i210396.adapters.ChatMessageAdapter
import com.hamzatariq.i210396.models.ChatMessage
import com.hamzatariq.i210396.utils.ImageUtils
import com.hamzatariq.i210396.utils.NotificationHelper
import com.hamzatariq.i210396.utils.OnlineStatusManager
import java.io.ByteArrayOutputStream

class chatScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: FirebaseDatabase
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: ChatMessageAdapter
    private lateinit var messageInput: EditText
    private lateinit var statusText: TextView

    private val messagesList = mutableListOf<ChatMessage>()
    private var otherUserId: String = ""
    private var otherUsername: String = ""
    private var otherUserProfileImage: String = ""
    private var chatRoomId: String = ""
    private var currentUserName: String = ""

    private var screenshotObserver: ContentObserver? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                sendImageMessage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance()

        // Get user data from intent
        otherUserId = intent.getStringExtra("userId") ?: ""
        otherUsername = intent.getStringExtra("username") ?: "User"
        otherUserProfileImage = intent.getStringExtra("profileImageUrl") ?: ""

        // Get current user name
        loadCurrentUserName()

        // Set up chat room ID
        val currentUserId = auth.currentUser?.uid ?: ""
        chatRoomId = getChatRoomId(currentUserId, otherUserId)

        // Initialize views
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        statusText = findViewById(R.id.statusText)

        // Setup RecyclerView
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messageAdapter = ChatMessageAdapter(this, messagesList, currentUserId)
        messagesRecyclerView.adapter = messageAdapter

        // Set user info in header
        findViewById<TextView>(R.id.username).text = otherUsername
        val profileIcon = findViewById<ImageView>(R.id.profileIcon)
        if (otherUserProfileImage.isNotEmpty()) {
            ImageUtils.loadBase64Image(profileIcon, otherUserProfileImage)
        }

        // Listen to other user's online status
        listenToUserOnlineStatus()

        // Back button
        findViewById<ImageView>(R.id.backIcon).setOnClickListener {
            finish()
        }

        // Video camera click → open CallActivity
        findViewById<ImageView>(R.id.videoCamera)?.setOnClickListener {
            initiateCall("video")
        }

        // Audio call button → open CallActivity for audio call
        findViewById<ImageView>(R.id.audioCall)?.setOnClickListener {
            initiateCall("audio")
        }

        // Send message button
        messageInput.setOnEditorActionListener { _, _, _ ->
            sendTextMessage()
            true
        }

        // Image button
        findViewById<ImageView>(R.id.btnImage).setOnClickListener {
            openGallery()
        }

        // Camera button
        findViewById<ImageView>(R.id.btnCamera).setOnClickListener {
            openGallery()
        }

        // Load messages
        loadMessages()

        // Start screenshot detection
        startScreenshotDetection()
    }

    private fun loadCurrentUserName() {
        val currentUserId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentUserName = document.getString("username") ?: "User"
                }
            }
    }

    private fun startScreenshotDetection() {
        screenshotObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                // Detect screenshot and notify other user
                NotificationHelper.sendScreenshotAlert(otherUserId, currentUserName)
                Toast.makeText(this@chatScreen, "Screenshot detected", Toast.LENGTH_SHORT).show()
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver!!
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        screenshotObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }

    private fun getChatRoomId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }

    private fun loadMessages() {
        val messagesRef = database.reference
            .child("chats")
            .child(chatRoomId)
            .child("messages")

        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messagesList.clear()

                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(ChatMessage::class.java)
                    if (message != null) {
                        messagesList.add(message)
                    }
                }

                // Sort by timestamp
                messagesList.sortBy { it.timestamp }
                messageAdapter.notifyDataSetChanged()

                // Scroll to bottom
                if (messagesList.isNotEmpty()) {
                    messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@chatScreen, "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendTextMessage() {
        val messageText = messageInput.text.toString().trim()
        if (messageText.isEmpty()) return

        val currentUserId = auth.currentUser?.uid ?: return
        val messageId = database.reference.child("chats").child(chatRoomId).child("messages").push().key ?: return

        val message = ChatMessage(
            messageId = messageId,
            senderId = currentUserId,
            receiverId = otherUserId,
            messageText = messageText,
            messageType = "text",
            timestamp = System.currentTimeMillis()
        )

        database.reference
            .child("chats")
            .child(chatRoomId)
            .child("messages")
            .child(messageId)
            .setValue(message)
            .addOnSuccessListener {
                messageInput.setText("")
                updateChatRoom(messageText)

                // Send notification to other user
                NotificationHelper.sendMessageNotification(
                    receiverId = otherUserId,
                    senderName = currentUserName,
                    messageText = messageText,
                    senderId = currentUserId,
                    senderImage = ""
                )
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendImageMessage(imageUri: Uri) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messageId = database.reference.child("chats").child(chatRoomId).child("messages").push().key ?: return

        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            val resizedBitmap = resizeBitmap(bitmap, 800)
            val imageBase64 = bitmapToBase64(resizedBitmap)

            val message = ChatMessage(
                messageId = messageId,
                senderId = currentUserId,
                receiverId = otherUserId,
                messageText = "",
                messageType = "image",
                imageBase64 = imageBase64,
                timestamp = System.currentTimeMillis()
            )

            database.reference
                .child("chats")
                .child(chatRoomId)
                .child("messages")
                .child(messageId)
                .setValue(message)
                .addOnSuccessListener {
                    updateChatRoom("📷 Photo")

                    // Send notification
                    NotificationHelper.sendMessageNotification(
                        receiverId = otherUserId,
                        senderName = currentUserName,
                        messageText = "📷 Sent a photo",
                        senderId = currentUserId,
                        senderImage = ""
                    )
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to send image", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateChatRoom(lastMessage: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        val chatRoomData = hashMapOf<String, Any>(
            "chatRoomId" to chatRoomId,
            "userId1" to if (currentUserId < otherUserId) currentUserId else otherUserId,
            "userId2" to if (currentUserId < otherUserId) otherUserId else currentUserId,
            "lastMessage" to lastMessage,
            "lastMessageTime" to System.currentTimeMillis(),
            "lastMessageSenderId" to currentUserId
        )

        database.reference
            .child("chats")
            .child(chatRoomId)
            .child("info")
            .updateChildren(chatRoomData)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        if (width > height) {
            width = maxSize
            height = (width / ratio).toInt()
        } else {
            height = maxSize
            width = (height * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun listenToUserOnlineStatus() {
        OnlineStatusManager.listenToUserStatus(otherUserId) { isOnline, lastSeen ->
            runOnUiThread {
                if (isOnline) {
                    statusText.text = "Online"
                    statusText.setTextColor(getColor(R.color.smd_text_color))
                } else {
                    val lastSeenText = OnlineStatusManager.formatLastSeen(lastSeen)
                    statusText.text = "Last seen $lastSeenText"
                    statusText.setTextColor(getColor(android.R.color.darker_gray))
                }
            }
        }
    }

    private fun initiateCall(callType: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Create an intent for the CallActivity
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("userId", otherUserId)
            putExtra("callType", callType) // "video" or "audio"
        }

        // Start the CallActivity
        startActivity(intent)
    }
}
