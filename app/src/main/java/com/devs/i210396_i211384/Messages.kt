package com.devs.i210396_i211384

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devs.i210396_i211384.adapters.ChatListAdapter
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.ChatListItem
import com.devs.i210396_i211384.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Messages : AppCompatActivity() {
    private val apiService = ApiService.create()
    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var chatListAdapter: ChatListAdapter
    private lateinit var searchEditText: EditText

    private val chatUsersList = mutableListOf<ChatListItem>()
    private val filteredChatUsers = mutableListOf<ChatListItem>()
    private var isPolling = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dm)

        // Initialize SessionManager
        SessionManager.init(this)

        // Handle system bars for LinearLayout root
        val mainLayout = findViewById<LinearLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        // Start polling for new messages
        startChatPolling()
    }

    private fun loadCurrentUserUsername() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getCurrentUser()
                }

                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        findViewById<android.widget.TextView>(R.id.usernameText)?.text = user.username
                    }
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    private fun loadChats() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getChatList()
                }

                if (response.isSuccessful) {
                    val chats = response.body() ?: emptyList()

                    // Log the response for debugging
                    android.util.Log.d("Messages", "Loaded ${chats.size} chats")

                    chatUsersList.clear()
                    chatUsersList.addAll(chats)

                    // Apply current search filter
                    filterChats(searchEditText.text.toString())
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("Messages", "Failed to load chats: ${response.code()} - $errorBody")
                    Toast.makeText(this@Messages, "Failed to load chats", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("Messages", "Error loading chats", e)
                Toast.makeText(this@Messages, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
            filteredChatUsers.addAll(chatUsersList.filter { chat ->
                chat.username.lowercase().contains(searchQuery)
            })
        }

        chatListAdapter.notifyDataSetChanged()
    }

    private fun startChatPolling() {
        lifecycleScope.launch {
            while (isPolling) {
                delay(5000) // Poll every 5 seconds for new messages
                loadChats()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isPolling = true
        startChatPolling()
        loadChats()
    }

    override fun onPause() {
        super.onPause()
        isPolling = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
    }
}
