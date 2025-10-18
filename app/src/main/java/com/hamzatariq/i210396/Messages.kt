package com.hamzatariq.i210396

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.hamzatariq.i210396.adapters.ChatListAdapter
import com.hamzatariq.i210396.models.ChatRoom
import com.hamzatariq.i210396.models.ChatUser

class Messages : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: FirebaseDatabase
    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var chatListAdapter: ChatListAdapter
    private lateinit var searchEditText: EditText

    private val chatUsersList = mutableListOf<ChatUser>()
    private val filteredChatUsers = mutableListOf<ChatUser>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dm)

        // Handle system bars for LinearLayout root
        val mainLayout = findViewById<LinearLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText)
        chatsRecyclerView = findViewById(R.id.chatsRecyclerView)

        // Setup RecyclerView
        chatsRecyclerView.layoutManager = LinearLayoutManager(this)
        chatListAdapter = ChatListAdapter(this, filteredChatUsers)
        chatsRecyclerView.adapter = chatListAdapter

        // Load logged-in user's username and display it
        loadCurrentUserUsername()

        // Back button
        findViewById<ImageView>(R.id.backIcon).setOnClickListener {
            finish()
        }

        // Plus icon - navigate to explore to find users to chat with
        findViewById<ImageView>(R.id.plusIcon).setOnClickListener {
            startActivity(Intent(this, ExploreSearch::class.java))
        }

        // Setup search
        setupSearch()

        // Load chats
        loadChats()
    }

    private fun loadCurrentUserUsername() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: "User"
                    findViewById<android.widget.TextView>(R.id.usernameText)?.text = username
                }
            }
    }

    private fun loadChats() {
        val currentUserId = auth.currentUser?.uid ?: return

        database.reference.child("chats")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatUsersList.clear()

                    for (chatSnapshot in snapshot.children) {
                        val chatRoomId = chatSnapshot.key ?: continue

                        // Check if current user is part of this chat
                        if (!chatRoomId.contains(currentUserId)) continue

                        val infoSnapshot = chatSnapshot.child("info")
                        val userId1 = infoSnapshot.child("userId1").getValue(String::class.java) ?: ""
                        val userId2 = infoSnapshot.child("userId2").getValue(String::class.java) ?: ""

                        if (userId1.isEmpty() || userId2.isEmpty()) continue

                        val otherUserId = if (userId1 == currentUserId) userId2 else userId1
                        val lastMessage = infoSnapshot.child("lastMessage").getValue(String::class.java) ?: ""
                        val lastMessageTime = infoSnapshot.child("lastMessageTime").getValue(Long::class.java) ?: 0L

                        // Load other user's info from Firestore
                        firestore.collection("users").document(otherUserId)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val username = document.getString("username") ?: "User"
                                    val profileImageUrl = document.getString("profileImageUrl") ?: ""

                                    val chatUser = ChatUser(
                                        userId = otherUserId,
                                        username = username,
                                        profileImageUrl = profileImageUrl,
                                        lastMessage = lastMessage,
                                        lastMessageTime = lastMessageTime,
                                        unreadCount = 0
                                    )

                                    // Remove existing entry for this user if any
                                    chatUsersList.removeAll { it.userId == otherUserId }
                                    chatUsersList.add(chatUser)

                                    // Sort by last message time
                                    chatUsersList.sortByDescending { it.lastMessageTime }

                                    // Update filtered list
                                    filterChats(searchEditText.text.toString())
                                }
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterChats(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterChats(query: String) {
        filteredChatUsers.clear()

        if (query.isEmpty()) {
            filteredChatUsers.addAll(chatUsersList)
        } else {
            val searchQuery = query.lowercase()
            filteredChatUsers.addAll(chatUsersList.filter { chatUser ->
                chatUser.username.lowercase().contains(searchQuery)
            })
        }

        chatListAdapter.notifyDataSetChanged()
    }
}
