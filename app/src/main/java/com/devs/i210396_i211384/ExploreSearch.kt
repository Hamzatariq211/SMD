package com.devs.i210396_i211384

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devs.i210396_i211384.adapters.UserListAdapter
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.network.UserListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExploreSearch : AppCompatActivity() {
    private val apiService = ApiService.create()
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserListAdapter
    private lateinit var searchEditText: EditText

    private val allUsers = mutableListOf<UserListItem>()
    private val filteredUsers = mutableListOf<UserListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SessionManager
        SessionManager.init(this)

        setContentView(R.layout.activity_explore2)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText)
        usersRecyclerView = findViewById(R.id.usersRecyclerView)

        // Back button
        findViewById<android.widget.ImageView>(R.id.backButton)?.setOnClickListener {
            finish()
        }

        // Setup RecyclerView
        usersRecyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserListAdapter(this, filteredUsers)
        usersRecyclerView.adapter = userAdapter

        // Load all users from MySQL
        loadAllUsers()

        // Setup search functionality
        setupSearch()

        // Update online status
        updateOnlineStatus(true)
    }

    private fun loadAllUsers() {
        if (!SessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getAllUsers()
                }

                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    allUsers.clear()
                    allUsers.addAll(users)

                    // Initially show all users
                    filteredUsers.clear()
                    filteredUsers.addAll(allUsers)
                    userAdapter.notifyDataSetChanged()

                    if (filteredUsers.isEmpty()) {
                        Toast.makeText(this@ExploreSearch, "No users found", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ExploreSearch, "Loaded ${filteredUsers.size} users", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ExploreSearch, "Failed to load users: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExploreSearch, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("ExploreSearch", "Error loading users", e)
            }
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterUsers(query: String) {
        filteredUsers.clear()

        if (query.isEmpty()) {
            filteredUsers.addAll(allUsers)
        } else {
            val searchQuery = query.lowercase()
            filteredUsers.addAll(allUsers.filter { user ->
                user.username.lowercase().contains(searchQuery) ||
                        (user.firstName?.lowercase()?.contains(searchQuery) == true) ||
                        (user.lastName?.lowercase()?.contains(searchQuery) == true) ||
                        "${user.firstName} ${user.lastName}".lowercase().contains(searchQuery)
            })
        }

        userAdapter.notifyDataSetChanged()
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    apiService.updateOnlineStatus(
                        com.devs.i210396_i211384.network.UpdateStatusRequest(isOnline)
                    )
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateOnlineStatus(true)
        loadAllUsers()
    }

    override fun onPause() {
        super.onPause()
        updateOnlineStatus(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateOnlineStatus(false)
    }
}
