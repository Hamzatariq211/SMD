package com.devs.i210396_i211384

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.devs.i210396_i211384.adapters.UserListAdapter
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.network.UserListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FollowersFollowingActivity : AppCompatActivity() {
    private val apiService = ApiService.create()
    private lateinit var recyclerView: RecyclerView
    private lateinit var userListAdapter: UserListAdapter
    private lateinit var tabLayout: TabLayout

    private var userId: String = ""
    private var initialTab: Int = 0 // 0 = followers, 1 = following

    private val followersList = mutableListOf<UserListItem>()
    private val followingList = mutableListOf<UserListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers_following)

        // Initialize SessionManager
        SessionManager.init(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get data from intent
        userId = intent.getStringExtra("userId") ?: SessionManager.getUserId() ?: ""
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
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getFollowers(userId)
                }

                if (response.isSuccessful) {
                    val followers = response.body() ?: emptyList()
                    followersList.clear()
                    followersList.addAll(followers)

                    if (tabLayout.selectedTabPosition == 0) {
                        showFollowers()
                    }
                } else {
                    Toast.makeText(this@FollowersFollowingActivity, "Failed to load followers", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FollowersFollowingActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFollowing() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getFollowing(userId)
                }

                if (response.isSuccessful) {
                    val following = response.body() ?: emptyList()
                    followingList.clear()
                    followingList.addAll(following)

                    if (tabLayout.selectedTabPosition == 1) {
                        showFollowing()
                    }
                } else {
                    Toast.makeText(this@FollowersFollowingActivity, "Failed to load following", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FollowersFollowingActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
