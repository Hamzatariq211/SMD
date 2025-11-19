package com.devs.i210396_i211384

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devs.i210396_i211384.adapters.PostGridAdapter
import com.devs.i210396_i211384.models.Post
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class profileScreen : AppCompatActivity() {
    private val apiService = ApiService.create()
    private lateinit var profilePic: ImageView
    private lateinit var tvNatasha: TextView
    private lateinit var username: TextView
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostGridAdapter
    private val userPostsList = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        val mainLayout = findViewById<LinearLayout>(R.id.main)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        profilePic = findViewById(R.id.profilePic)
        tvNatasha = findViewById(R.id.tvNatasha)
        username = findViewById(R.id.username)

        // Initialize RecyclerView for posts grid
        postsRecyclerView = findViewById(R.id.posted_pictures)
        postsRecyclerView.layoutManager = GridLayoutManager(this, 3)

        // Initialize adapter
        postAdapter = PostGridAdapter(this, userPostsList) { post ->
            val intent = Intent(this, PostDetailActivity::class.java)
            intent.putExtra("postId", post.postId)
            startActivity(intent)
        }
        postsRecyclerView.adapter = postAdapter

        // Load user profile data from MySQL
        loadUserProfile()

        // Load user posts from MySQL
        loadUserPosts()

        // Navigation buttons
        findViewById<ImageView>(R.id.home).setOnClickListener {
            startActivity(Intent(this, HomePage::class.java))
        }

        findViewById<ImageView>(R.id.explore).setOnClickListener {
            startActivity(Intent(this, Explore::class.java))
        }

        findViewById<ImageView>(R.id.post).setOnClickListener {
            startActivity(Intent(this, AddPostScreen::class.java))
        }

        findViewById<ImageView>(R.id.like1).setOnClickListener {
            startActivity(Intent(this, likeFollowing::class.java))
        }

        findViewById<ImageView>(R.id.profile).setOnClickListener {
            // Already on profile screen
        }

        findViewById<Button>(R.id.editProfileBtn).setOnClickListener {
            startActivity(Intent(this, EditProfile::class.java))
        }

        findViewById<ImageView>(R.id.menuIcon).setOnClickListener {
            showMenuOptions()
        }
    }

    private fun showMenuOptions() {
        val options = arrayOf("Follow Requests", "Settings", "Logout")

        AlertDialog.Builder(this)
            .setTitle("Menu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, FollowRequestsActivity::class.java)
                        startActivity(intent)
                    }
                    1 -> {
                        val intent = Intent(this, EditProfile::class.java)
                        startActivity(intent)
                    }
                    2 -> {
                        showLogoutDialog()
                    }
                }
            }
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            apiService.logout()
                        }
                    } catch (e: Exception) {
                        // Ignore error
                    } finally {
                        SessionManager.clearSession()

                        val intent = Intent(this@profileScreen, loginUser::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val userId = SessionManager.getUserId()
                if (userId == null) {
                    Toast.makeText(this@profileScreen, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                android.util.Log.d("ProfileScreen", "Loading profile for user ID: $userId")

                // Load complete profile with counts from MySQL
                val profileResponse = withContext(Dispatchers.IO) {
                    apiService.getUserProfile(userId)
                }

                if (profileResponse.isSuccessful) {
                    val profile = profileResponse.body()
                    if (profile == null) {
                        Toast.makeText(this@profileScreen, "Profile data is null", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    android.util.Log.d("ProfileScreen", "Profile loaded: ${profile.username}, postsCount: ${profile.postsCount}, followersCount: ${profile.followersCount}, followingCount: ${profile.followingCount}")

                    // Set username and name
                    username.text = profile.username
                    val fullName = "${profile.firstName ?: ""} ${profile.lastName ?: ""}".trim()
                    tvNatasha.text = if (fullName.isNotEmpty()) fullName else profile.username

                    // Load profile picture
                    val profileImageBase64 = profile.profileImageUrl
                    android.util.Log.d("ProfileScreen", "Profile image length: ${profileImageBase64?.length ?: 0}")

                    if (!profileImageBase64.isNullOrEmpty()) {
                        try {
                            ImageUtils.loadBase64Image(profilePic, profileImageBase64)
                            android.util.Log.d("ProfileScreen", "Profile image loaded successfully")
                        } catch (e: Exception) {
                            android.util.Log.e("ProfileScreen", "Error loading profile image: ${e.message}", e)
                            profilePic.setImageResource(R.drawable.profile)
                        }
                    } else {
                        android.util.Log.d("ProfileScreen", "No profile image, using default")
                        profilePic.setImageResource(R.drawable.profile)
                    }

                    // Display counts
                    findViewById<TextView>(R.id.postsCount)?.text = profile.postsCount.toString()
                    findViewById<TextView>(R.id.followersCount)?.text = profile.followersCount.toString()
                    findViewById<TextView>(R.id.followingCount)?.text = profile.followingCount.toString()

                    android.util.Log.d("ProfileScreen", "Counts displayed - Posts: ${profile.postsCount}, Followers: ${profile.followersCount}, Following: ${profile.followingCount}")

                    // Setup click listeners for followers/following
                    setupFollowersFollowingClickListeners(profile.username)
                } else {
                    val errorMsg = "Failed to load profile: ${profileResponse.code()} - ${profileResponse.message()}"
                    android.util.Log.e("ProfileScreen", errorMsg)
                    Toast.makeText(this@profileScreen, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val errorMsg = "Error loading profile: ${e.message}"
                android.util.Log.e("ProfileScreen", errorMsg, e)
                Toast.makeText(this@profileScreen, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupFollowersFollowingClickListeners(username: String) {
        val userId = SessionManager.getUserId() ?: return

        // Click on followers count - show who follows you
        findViewById<TextView>(R.id.followersCount)?.setOnClickListener {
            val intent = Intent(this, FollowersFollowingActivity::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("username", username)
            intent.putExtra("initialTab", 0) // 0 = followers tab
            startActivity(intent)
        }

        // Click on following count - show whom you follow
        findViewById<TextView>(R.id.followingCount)?.setOnClickListener {
            val intent = Intent(this, FollowersFollowingActivity::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("username", username)
            intent.putExtra("initialTab", 1) // 1 = following tab
            startActivity(intent)
        }

        // Optional: Make the "Followers" and "Following" labels clickable too
        findViewById<TextView>(R.id.followersCount)?.parent?.let { parent ->
            (parent as? android.view.ViewGroup)?.setOnClickListener {
                findViewById<TextView>(R.id.followersCount)?.performClick()
            }
        }
    }

    private fun loadUserPosts() {
        lifecycleScope.launch {
            try {
                val userId = SessionManager.getUserId() ?: return@launch

                val response = withContext(Dispatchers.IO) {
                    apiService.getUserPosts(userId)
                }

                if (response.isSuccessful) {
                    val apiPosts = response.body() ?: emptyList<com.devs.i210396_i211384.network.PostResponse>()
                    userPostsList.clear()

                    for (apiPost in apiPosts) {
                        userPostsList.add(
                            Post(
                                postId = apiPost.postId,
                                userId = apiPost.userId,
                                username = apiPost.username,
                                userProfileImage = apiPost.userProfileImage,
                                postImageBase64 = apiPost.postImageBase64,
                                caption = apiPost.caption,
                                timestamp = apiPost.timestamp,
                                likeCount = apiPost.likeCount,
                                commentCount = apiPost.commentCount,
                                isLiked = apiPost.isLiked
                            )
                        )
                    }

                    postAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@profileScreen, "No posts yet", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Silently fail or show empty state
            }
        }
    }
}
