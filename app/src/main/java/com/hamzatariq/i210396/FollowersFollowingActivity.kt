package com.hamzatariq.i210396

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hamzatariq.i210396.adapters.UserListAdapter
import com.hamzatariq.i210396.models.User

class FollowersFollowingActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var userListAdapter: UserListAdapter
    private lateinit var tabLayout: TabLayout

    private var userId: String = ""
    private var initialTab: Int = 0 // 0 = followers, 1 = following

    private val followersList = mutableListOf<User>()
    private val followingList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers_following)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Get data from intent
        userId = intent.getStringExtra("userId") ?: auth.currentUser?.uid ?: ""
        initialTab = intent.getIntExtra("initialTab", 0)
        val username = intent.getStringExtra("username") ?: "User"

        // Initialize views
        findViewById<TextView>(R.id.usernameText).text = username
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        userListAdapter = UserListAdapter(this, mutableListOf())
        recyclerView.adapter = userListAdapter

        // Setup TabLayout
        tabLayout.addTab(tabLayout.newTab().setText("Followers"))
        tabLayout.addTab(tabLayout.newTab().setText("Following"))

        // Select initial tab
        tabLayout.getTabAt(initialTab)?.select()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showFollowers()
                    1 -> showFollowing()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Back button
        findViewById<ImageView>(R.id.backIcon).setOnClickListener {
            finish()
        }

        // Load initial data
        loadFollowers()
        loadFollowing()

        if (initialTab == 0) {
            showFollowers()
        } else {
            showFollowing()
        }
    }

    private fun loadFollowers() {
        firestore.collection("users").document(userId)
            .collection("followers")
            .get()
            .addOnSuccessListener { documents ->
                followersList.clear()
                var loadedCount = 0
                val totalDocs = documents.size()

                if (totalDocs == 0) {
                    if (tabLayout.selectedTabPosition == 0) {
                        showFollowers()
                    }
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val followerId = doc.getString("userId") ?: continue

                    firestore.collection("users").document(followerId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                val user = userDoc.toObject(User::class.java)
                                user?.let { followersList.add(it) }
                            }

                            loadedCount++
                            if (loadedCount == totalDocs && tabLayout.selectedTabPosition == 0) {
                                showFollowers()
                            }
                        }
                }
            }
    }

    private fun loadFollowing() {
        firestore.collection("users").document(userId)
            .collection("following")
            .get()
            .addOnSuccessListener { documents ->
                followingList.clear()
                var loadedCount = 0
                val totalDocs = documents.size()

                if (totalDocs == 0) {
                    if (tabLayout.selectedTabPosition == 1) {
                        showFollowing()
                    }
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val followingId = doc.getString("userId") ?: continue

                    firestore.collection("users").document(followingId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                val user = userDoc.toObject(User::class.java)
                                user?.let { followingList.add(it) }
                            }

                            loadedCount++
                            if (loadedCount == totalDocs && tabLayout.selectedTabPosition == 1) {
                                showFollowing()
                            }
                        }
                }
            }
    }

    private fun showFollowers() {
        userListAdapter.updateList(followersList)
    }

    private fun showFollowing() {
        userListAdapter.updateList(followingList)
    }
}

