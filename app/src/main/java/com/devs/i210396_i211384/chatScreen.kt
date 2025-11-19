package com.devs.i210396_i211384

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devs.i210396_i211384.adapters.ChatMessageAdapter
import com.devs.i210396_i211384.database.MessageDatabase
import com.devs.i210396_i211384.models.ChatMessage
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.network.SendMessageRequest
import com.devs.i210396_i211384.network.EditMessageRequest
import com.devs.i210396_i211384.network.DeleteMessageRequest
import com.devs.i210396_i211384.utils.ImageUtils
import com.devs.i210396_i211384.utils.OfflineActionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class chatScreen : AppCompatActivity() {
    private val apiService = ApiService.create()
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: ChatMessageAdapter
    private lateinit var messageInput: EditText
    private lateinit var statusText: TextView
    private lateinit var messageDatabase: MessageDatabase
    private lateinit var offlineDb: com.devs.i210396_i211384.database.OfflineDatabase
    private lateinit var offlineActionManager: OfflineActionManager

    private val messagesList = mutableListOf<ChatMessage>()
    private var otherUserId: String = ""
    private var otherUsername: String = ""
    private var otherUserProfileImage: String = ""
    private var currentUserId: String = ""
    private var chatRoomId: String = ""
    private var isVanishMode: Boolean = false
    private var isPolling: Boolean = true
    private var onlineIndicator: View? = null

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

        // Initialize SessionManager and Database
        SessionManager.init(this)
        currentUserId = SessionManager.getUserId() ?: ""
        messageDatabase = MessageDatabase.getInstance(this)
        offlineDb = com.devs.i210396_i211384.database.OfflineDatabase.getInstance(this)
        offlineActionManager = OfflineActionManager.getInstance(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get user data from intent
        otherUserId = intent.getStringExtra("userId") ?: ""
        otherUsername = intent.getStringExtra("username") ?: "User"
        // Don't get profileImageUrl from intent - will load from API
        otherUserProfileImage = ""
        isVanishMode = intent.getBooleanExtra("isVanishMode", false)

        // Log received data for debugging
        android.util.Log.d("chatScreen", "Opening chat with userId: $otherUserId, username: $otherUsername")

        if (currentUserId.isEmpty() || otherUserId.isEmpty()) {
            android.util.Log.e("chatScreen", "Invalid user data - currentUserId: $currentUserId, otherUserId: $otherUserId")
            Toast.makeText(this, "Error: Invalid user data. Please try again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize views
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        statusText = findViewById(R.id.statusText)

        // Setup RecyclerView
        messagesRecyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messageAdapter = ChatMessageAdapter(
            context = this,
            messages = messagesList,
            currentUserId = currentUserId,
            onEditMessage = { message -> showEditDialog(message) },
            onDeleteMessage = { message -> deleteMessage(message) }
        )
        messagesRecyclerView.adapter = messageAdapter

        // Set user info in header
        findViewById<TextView>(R.id.username).text = otherUsername

        // Load profile image from API (not from intent to avoid TransactionTooLargeException)
        loadUserProfile()

        // Check other user's online status
        checkUserOnlineStatus()

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
        findViewById<ImageView>(R.id.btnSend)?.setOnClickListener {
            sendTextMessage()
        }

        messageInput.setOnEditorActionListener { _, _, _ ->
            sendTextMessage()
            true
        }

        // Image button
        findViewById<ImageView>(R.id.btnImage)?.setOnClickListener {
            openGallery()
        }

        // Camera button
        findViewById<ImageView>(R.id.btnCamera)?.setOnClickListener {
            openGallery()
        }

        // Vanish mode toggle
        findViewById<ImageView>(R.id.vanishModeIcon)?.setOnClickListener {
            toggleVanishMode()
        }

        // Load messages (offline first, then sync)
        loadMessagesOffline()
        loadMessages()

        // Start polling for new messages
        startMessagePolling()
    }

    private fun loadMessagesOffline() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cachedMessages = messageDatabase.getMessages(currentUserId, otherUserId)
                withContext(Dispatchers.Main) {
                    if (cachedMessages.isNotEmpty()) {
                        messagesList.clear()
                        messagesList.addAll(cachedMessages)
                        messageAdapter.notifyDataSetChanged()
                        scrollToBottom()
                    }
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getMessages(otherUserId)
                }

                if (response.isSuccessful) {
                    val apiMessages = response.body() ?: emptyList()
                    messagesList.clear()

                    val messagesToCache = mutableListOf<ChatMessage>()

                    for (apiMsg in apiMessages) {
                        // Skip vanish mode messages that have been seen
                        if (isVanishMode && apiMsg.isSeen && apiMsg.receiverId == currentUserId) {
                            continue
                        }

                        val message = ChatMessage(
                            messageId = apiMsg.messageId,
                            senderId = apiMsg.senderId,
                            receiverId = apiMsg.receiverId,
                            messageText = apiMsg.messageText,
                            messageType = apiMsg.messageType,
                            imageBase64 = apiMsg.imageBase64 ?: "",
                            postId = apiMsg.postId ?: "",
                            isEdited = apiMsg.isEdited,
                            editedAt = apiMsg.editedAt,
                            isDeleted = apiMsg.isDeleted,
                            timestamp = apiMsg.timestamp
                        )
                        messagesList.add(message)

                        // Don't cache vanish mode messages
                        if (!isVanishMode) {
                            messagesToCache.add(message)
                        }
                    }

                    // Cache messages for offline viewing
                    if (messagesToCache.isNotEmpty() && chatRoomId.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            messageDatabase.saveMessages(messagesToCache, chatRoomId)
                        }
                    }

                    messageAdapter.notifyDataSetChanged()
                    scrollToBottom()
                } else {
                    // If network fails, show cached messages
                    loadMessagesOffline()
                    Toast.makeText(this@chatScreen, "Using offline messages", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // If network fails, show cached messages
                loadMessagesOffline()
                Toast.makeText(this@chatScreen, "Offline mode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendTextMessage() {
        val messageText = messageInput.text.toString().trim()
        if (messageText.isEmpty()) return

        lifecycleScope.launch {
            try {
                // Check if online
                if (!offlineActionManager.isOnline()) {
                    // Queue message for later sending
                    offlineActionManager.queueSendMessage(
                        receiverId = otherUserId,
                        messageText = messageText,
                        messageType = "text",
                        isVanishMode = isVanishMode
                    )

                    // Add to local cache immediately for optimistic UI
                    val tempMessage = ChatMessage(
                        messageId = "temp_${System.currentTimeMillis()}",
                        senderId = currentUserId,
                        receiverId = otherUserId,
                        messageText = messageText,
                        messageType = "text",
                        imageBase64 = "",
                        postId = "",
                        isEdited = false,
                        editedAt = 0,
                        isDeleted = false,
                        timestamp = System.currentTimeMillis()
                    )

                    messagesList.add(tempMessage)
                    messageAdapter.notifyItemInserted(messagesList.size - 1)
                    scrollToBottom()
                    messageInput.text.clear()

                    Toast.makeText(this@chatScreen, "Offline - Message will be sent when online", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val request = SendMessageRequest(
                    receiverId = otherUserId,
                    messageText = messageText,
                    messageType = "text",
                    isVanishMode = isVanishMode
                )

                val response = withContext(Dispatchers.IO) {
                    apiService.sendMessage(request)
                }

                if (response.isSuccessful) {
                    val result = response.body()
                    chatRoomId = result?.get("chatRoomId") as? String ?: chatRoomId
                    messageInput.text.clear()
                    loadMessages()
                } else {
                    Toast.makeText(this@chatScreen, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Network error - queue for offline
                offlineActionManager.queueSendMessage(
                    receiverId = otherUserId,
                    messageText = messageText,
                    messageType = "text",
                    isVanishMode = isVanishMode
                )
                Toast.makeText(this@chatScreen, "Queued for sending when online", Toast.LENGTH_SHORT).show()
                messageInput.text.clear()
            }
        }
    }

    private fun sendImageMessage(uri: Uri) {
        lifecycleScope.launch {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val resizedBitmap = resizeBitmap(bitmap, 1024, 1024)
                val base64Image = bitmapToBase64(resizedBitmap)

                val request = SendMessageRequest(
                    receiverId = otherUserId,
                    messageText = "📷 Image",
                    messageType = "image",
                    mediaUrl = base64Image,
                    isVanishMode = isVanishMode
                )

                val response = withContext(Dispatchers.IO) {
                    apiService.sendMessage(request)
                }

                if (response.isSuccessful) {
                    val result = response.body()
                    chatRoomId = result?.get("chatRoomId") as? String ?: chatRoomId
                    loadMessages()
                    Toast.makeText(this@chatScreen, "Image sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@chatScreen, "Failed to send image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@chatScreen, "Error sending image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(message: ChatMessage) {
        // Check if message is within 5 minutes
        val currentTime = System.currentTimeMillis()
        val messageAge = currentTime - message.timestamp
        val fiveMinutes = 5 * 60 * 1000

        if (messageAge > fiveMinutes) {
            Toast.makeText(this, "Can only edit messages within 5 minutes", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Edit Message")

        val input = EditText(this)
        input.setText(message.messageText)
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val newText = input.text.toString().trim()
            if (newText.isNotEmpty() && newText != message.messageText) {
                editMessage(message.messageId, newText)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun editMessage(messageId: String, newText: String) {
        lifecycleScope.launch {
            try {
                val request = EditMessageRequest(messageId, newText)
                val response = withContext(Dispatchers.IO) {
                    apiService.editMessage(request)
                }

                if (response.isSuccessful) {
                    // Update local cache
                    withContext(Dispatchers.IO) {
                        messageDatabase.updateMessage(messageId, newText, isEdited = true)
                    }
                    loadMessages()
                    Toast.makeText(this@chatScreen, "Message edited", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@chatScreen, "Failed to edit message", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@chatScreen, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteMessage(message: ChatMessage) {
        // Check if message is within 5 minutes
        val currentTime = System.currentTimeMillis()
        val messageAge = currentTime - message.timestamp
        val fiveMinutes = 5 * 60 * 1000

        if (messageAge > fiveMinutes) {
            Toast.makeText(this, "Can only delete messages within 5 minutes", Toast.LENGTH_SHORT).show()
            return
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete") { _, _ ->
                performDeleteMessage(message.messageId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeleteMessage(messageId: String) {
        lifecycleScope.launch {
            try {
                val request = DeleteMessageRequest(messageId)
                val response = withContext(Dispatchers.IO) {
                    apiService.deleteMessage(request)
                }

                if (response.isSuccessful) {
                    // Update local cache
                    withContext(Dispatchers.IO) {
                        messageDatabase.updateMessage(messageId, isDeleted = true)
                    }
                    loadMessages()
                    Toast.makeText(this@chatScreen, "Message deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@chatScreen, "Failed to delete message", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@chatScreen, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleVanishMode() {
        isVanishMode = !isVanishMode
        val icon = findViewById<ImageView>(R.id.vanishModeIcon)

        if (isVanishMode) {
            icon?.setImageResource(R.drawable.ic_vanish_on)
            Toast.makeText(this, "Vanish mode ON - Messages will disappear after being seen", Toast.LENGTH_LONG).show()
        } else {
            icon?.setImageResource(R.drawable.ic_vanish_off)
            Toast.makeText(this, "Vanish mode OFF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUserOnlineStatus() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getUserProfile(otherUserId)
                }

                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        updateOnlineStatus(user.isOnline, user.lastSeen)
                    }
                }
            } catch (e: Exception) {
                // Silently fail
            }

            // Check again after 10 seconds for more real-time updates
            delay(10000)
            if (isPolling) {
                checkUserOnlineStatus()
            }
        }
    }

    private fun updateOnlineStatus(isOnline: Boolean, lastSeen: Long) {
        if (isOnline) {
            statusText.text = "Online"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
            // Show green dot if you add one to the layout
            onlineIndicator?.visibility = View.VISIBLE
        } else {
            val timeAgo = getTimeAgo(lastSeen)
            statusText.text = timeAgo
            statusText.setTextColor(getColor(android.R.color.darker_gray))
            onlineIndicator?.visibility = View.GONE
        }
    }

    private fun startMessagePolling() {
        lifecycleScope.launch {
            while (isPolling) {
                delay(3000) // Poll every 3 seconds
                loadMessages()
            }
        }
    }

    private fun initiateCall(callType: String) {
        val intent = Intent(this, CallActivity::class.java)
        intent.putExtra("callType", callType)
        intent.putExtra("receiverId", otherUserId)
        intent.putExtra("receiverName", otherUsername)
        startActivity(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun scrollToBottom() {
        if (messagesList.isNotEmpty()) {
            messagesRecyclerView.smoothScrollToPosition(messagesList.size - 1)
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Active now"
            diff < 3600000 -> "Active ${diff / 60000}m ago"
            diff < 86400000 -> "Active ${diff / 3600000}h ago"
            diff < 604800000 -> "Active ${diff / 86400000}d ago"
            else -> "Active ${diff / 604800000}w ago"
        }
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getUserProfile(otherUserId)
                }

                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        // Update header info with safe calls
                        val username = user.username ?: otherUsername
                        findViewById<TextView>(R.id.username).text = username
                        otherUsername = username

                        // Load profile image with null safety
                        val profileUrl = user.profileImageUrl ?: ""
                        if (profileUrl.isNotEmpty()) {
                            otherUserProfileImage = profileUrl
                            val profileIcon = findViewById<ImageView>(R.id.profileIcon)
                            ImageUtils.loadBase64Image(profileIcon, otherUserProfileImage)
                        }

                        // Update online status
                        if (user.isOnline) {
                            statusText.text = "Online"
                        } else {
                            statusText.text = getTimeAgo(user.lastSeen)
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
    }

    override fun onPause() {
        super.onPause()
        isPolling = false
    }

    override fun onResume() {
        super.onResume()
        isPolling = true
        startMessagePolling()
        loadMessages()
    }
}
