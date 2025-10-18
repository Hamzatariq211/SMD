package com.hamzatariq.i210396

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hamzatariq.i210396.adapters.UserAdapter
import com.hamzatariq.i210396.models.User
import com.hamzatariq.i210396.utils.OnlineStatusManager

class ExploreSearch : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var searchEditText: EditText

    private val allUsers = mutableListOf<User>()
    private val filteredUsers = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            setContentView(R.layout.activity_explore2)

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            // Initialize Firebase
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()

            // Set user online
            OnlineStatusManager.setUserOnline()

            // Initialize views
            searchEditText = findViewById(R.id.searchEditText)
            usersRecyclerView = findViewById(R.id.usersRecyclerView)

            // Back button
            findViewById<android.widget.ImageView>(R.id.backButton)?.setOnClickListener {
                finish()
            }

            // Setup RecyclerView
            usersRecyclerView.layoutManager = LinearLayoutManager(this)
            userAdapter = UserAdapter(this, filteredUsers)
            usersRecyclerView.adapter = userAdapter

            // Load all users
            loadAllUsers()

            // Setup search functionality
            setupSearch()

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading page: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    private fun loadAllUsers() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        android.util.Log.d("ExploreSearch", "Loading users from Firestore...")

        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                allUsers.clear()

                android.util.Log.d("ExploreSearch", "Found ${documents.size()} total documents")

                var loadedCount = 0
                for (document in documents) {
                    try {
                        // Get the user object and set uid from document ID if needed
                        val userData = document.data
                        val user = User(
                            uid = document.id, // Use document ID as uid
                            email = userData["email"] as? String ?: "",
                            username = userData["username"] as? String ?: "",
                            firstName = userData["firstName"] as? String ?: "",
                            lastName = userData["lastName"] as? String ?: "",
                            dateOfBirth = userData["dateOfBirth"] as? String ?: "",
                            profileImageUrl = userData["profileImageUrl"] as? String ?: "",
                            bio = userData["bio"] as? String ?: "",
                            isProfileSetup = userData["isProfileSetup"] as? Boolean ?: false,
                            isPrivate = userData["isPrivate"] as? Boolean ?: false,
                            isOnline = userData["isOnline"] as? Boolean ?: false,
                            lastSeen = userData["lastSeen"] as? Long ?: 0L,
                            createdAt = userData["createdAt"] as? Long ?: 0L
                        )

                        android.util.Log.d("ExploreSearch", "Processing user: ${user.username}, uid: ${user.uid}")

                        // Exclude current user from the list and ensure user has required fields
                        if (user.uid != currentUserId && user.username.isNotEmpty()) {
                            allUsers.add(user)
                            loadedCount++
                            android.util.Log.d("ExploreSearch", "Added user: ${user.username}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ExploreSearch", "Error parsing user document ${document.id}: ${e.message}", e)
                        // Skip malformed user documents
                        continue
                    }
                }

                android.util.Log.d("ExploreSearch", "Loaded $loadedCount users successfully")

                // Initially show all users
                filteredUsers.clear()
                filteredUsers.addAll(allUsers)
                userAdapter.notifyDataSetChanged()

                if (filteredUsers.isEmpty()) {
                    Toast.makeText(this, "No users found. Try adding some users to the database.", Toast.LENGTH_LONG).show()
                    android.util.Log.w("ExploreSearch", "No users to display!")
                } else {
                    Toast.makeText(this, "Loaded ${filteredUsers.size} users", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("ExploreSearch", "Failed to load users: ${exception.message}", exception)
                Toast.makeText(this, "Failed to load users: ${exception.message}", Toast.LENGTH_SHORT).show()
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
                        user.firstName.lowercase().contains(searchQuery) ||
                        user.lastName.lowercase().contains(searchQuery) ||
                        "${user.firstName} ${user.lastName}".lowercase().contains(searchQuery)
            })
        }

        userAdapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        OnlineStatusManager.setUserOnline()
        loadAllUsers()
    }

    override fun onPause() {
        super.onPause()
        OnlineStatusManager.setUserOffline()
    }

    override fun onDestroy() {
        super.onDestroy()
        OnlineStatusManager.setUserOffline()
    }
}
