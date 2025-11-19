package com.devs.i210396_i211384

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devs.i210396_i211384.adapters.PostGridAdapter
import com.devs.i210396_i211384.models.Post
import com.devs.i210396_i211384.utils.ImageUtils
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.network.FollowRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SociallyOtherUser : AppCompatActivity() {

    private val apiService = ApiService.create()
    private var otherUserId: String = ""
    private var isFollowing: Boolean = false
    private var hasPendingRequest: Boolean = false
    private var isPrivate: Boolean = false

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostGridAdapter
    private val userPostsList = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_other_user)

        // Initialize SessionManager
        SessionManager.init(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        // Get user data from intent
        otherUserId = intent.getStringExtra("userId") ?: ""

        if (otherUserId.isEmpty()) {
            Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load other user's profile from API (this will get the correct profile picture)
        loadOtherUserProfile()

        // Setup RecyclerView for posts
        postsRecyclerView = findViewById(R.id.posted_pictures)
        postsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        postAdapter = PostGridAdapter(this, userPostsList) { }
        postsRecyclerView.adapter = postAdapter

        // Load other user's posts from API
        loadOtherUserPosts()

        // Setup follow button
        val btnFollow = findViewById<Button>(R.id.btnFollow)
        btnFollow.setOnClickListener {
            toggleFollow()
        }

        // Back button
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        // Bottom navigation
        findViewById<ImageView>(R.id.home).setOnClickListener {
            startActivity(Intent(this, HomePage::class.java))
        }

        findViewById<ImageView>(R.id.explore).setOnClickListener {
            startActivity(Intent(this, Explore::class.java))
        }

        findViewById<ImageView>(R.id.post).setOnClickListener {
            startActivity(Intent(this, AddPostScreen::class.java))
        }

        findViewById<ImageView>(R.id.like).setOnClickListener {
            startActivity(Intent(this, likeFollowing::class.java))
        }

        findViewById<ImageView>(R.id.profile).setOnClickListener {
            startActivity(Intent(this, profileScreen::class.java))
        }

        // Setup click listeners for followers/following counts
        findViewById<TextView>(R.id.followersCount)?.setOnClickListener {
            val intent = Intent(this, FollowersFollowingActivity::class.java)
            intent.putExtra("userId", otherUserId)
            intent.putExtra("username", findViewById<TextView>(R.id.username)?.text.toString())
            intent.putExtra("initialTab", 0) // 0 = followers tab
            startActivity(intent)
        }

        findViewById<TextView>(R.id.followingCount)?.setOnClickListener {
            val intent = Intent(this, FollowersFollowingActivity::class.java)
            intent.putExtra("userId", otherUserId)
            intent.putExtra("username", findViewById<TextView>(R.id.username)?.text.toString())
            intent.putExtra("initialTab", 1) // 1 = following tab
            startActivity(intent)
        }
    }

    private fun loadOtherUserProfile() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getUserProfile(otherUserId)
                }

                if (response.isSuccessful) {
                    val userProfile = response.body()
                    if (userProfile != null) {
                        // Update UI with profile data
                        findViewById<TextView>(R.id.username)?.text = userProfile.username
                        findViewById<TextView>(R.id.usernameText)?.text = userProfile.username

                        val fullName = "${userProfile.firstName ?: ""} ${userProfile.lastName ?: ""}".trim()
                        findViewById<TextView>(R.id.name)?.text = if (fullName.isNotEmpty()) fullName else userProfile.username

                        findViewById<TextView>(R.id.bio)?.text = userProfile.bio ?: ""

                        // Load profile picture from API
                        if (!userProfile.profileImageUrl.isNullOrEmpty()) {
                            ImageUtils.loadBase64Image(findViewById(R.id.profilePic), userProfile.profileImageUrl)
                        } else {
                            findViewById<ImageView>(R.id.profilePic).setImageResource(R.drawable.profile)
                        }

                        // Update counts
                        findViewById<TextView>(R.id.postsCount)?.text = userProfile.postsCount.toString()
                        findViewById<TextView>(R.id.followersCount)?.text = userProfile.followersCount.toString()
                        findViewById<TextView>(R.id.followingCount)?.text = userProfile.followingCount.toString()

                        // Update follow status
                        isFollowing = userProfile.isFollowing
                        hasPendingRequest = userProfile.hasPendingRequest
                        isPrivate = userProfile.isPrivate

                        updateFollowButton()
                    } else {
                        Toast.makeText(this@SociallyOtherUser, "User not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@SociallyOtherUser, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SociallyOtherUser, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFollowButton() {
        val btnFollow = findViewById<Button>(R.id.btnFollow)
        when {
            isFollowing -> {
                btnFollow.text = "Following"
                btnFollow.setBackgroundResource(R.drawable.button_background_secondary)
                btnFollow.setTextColor(getColor(android.R.color.black))
            }
            hasPendingRequest -> {
                btnFollow.text = "Requested"
                btnFollow.setBackgroundResource(R.drawable.button_background_secondary)
                btnFollow.setTextColor(getColor(android.R.color.black))
            }
            else -> {
                btnFollow.text = "Follow"
                btnFollow.setBackgroundResource(R.drawable.button_background)
                btnFollow.setTextColor(getColor(android.R.color.white))
            }
        }
    }

    private fun toggleFollow() {
        lifecycleScope.launch {
            try {
                val response = if (isFollowing) {
                    // Unfollow
                    withContext(Dispatchers.IO) {
                        apiService.unfollowUser(FollowRequest(otherUserId))
                    }
                } else {
                    // Follow or send request
                    withContext(Dispatchers.IO) {
                        apiService.followUser(FollowRequest(otherUserId))
                    }
                }

                if (response.isSuccessful) {
                    val message = response.body()?.get("message") ?: "Success"
                    Toast.makeText(this@SociallyOtherUser, message, Toast.LENGTH_SHORT).show()

                    // Update local state
                    if (isFollowing) {
                        // Was following, now unfollowed
                        isFollowing = false
                        hasPendingRequest = false
                        // Update followers count
                        val currentCount = findViewById<TextView>(R.id.followersCount)?.text.toString().toIntOrNull() ?: 0
                        findViewById<TextView>(R.id.followersCount)?.text = (currentCount - 1).coerceAtLeast(0).toString()
                    } else {
                        // Was not following
                        if (isPrivate) {
                            // Private account - request sent
                            hasPendingRequest = true
                            isFollowing = false
                        } else {
                            // Public account - directly followed
                            isFollowing = true
                            hasPendingRequest = false
                            // Update followers count
                            val currentCount = findViewById<TextView>(R.id.followersCount)?.text.toString().toIntOrNull() ?: 0
                            findViewById<TextView>(R.id.followersCount)?.text = (currentCount + 1).toString()
                        }
                    }

                    updateFollowButton()
                } else {
                    Toast.makeText(this@SociallyOtherUser, "Failed to update follow status", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SociallyOtherUser, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadOtherUserPosts() {
        if (otherUserId.isEmpty()) {
            Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getUserPosts(otherUserId)
                }

                if (response.isSuccessful) {
                    val posts = response.body() ?: emptyList<com.devs.i210396_i211384.network.PostResponse>()

                    userPostsList.clear()

                    // Convert API PostResponse to Post model
                    for (postResponse in posts) {
                        val post = Post(
                            postId = postResponse.postId,
                            userId = postResponse.userId,
                            username = postResponse.username,
                            userProfileImage = postResponse.userProfileImage,
                            postImageBase64 = postResponse.postImageBase64,
                            caption = postResponse.caption,
                            likeCount = postResponse.likeCount,
                            commentCount = postResponse.commentCount,
                            timestamp = postResponse.timestamp,
                            isLiked = postResponse.isLiked
                        )
                        userPostsList.add(post)
                    }

                    // Update UI
                    findViewById<TextView>(R.id.postsCount)?.text = userPostsList.size.toString()
                    postAdapter.notifyDataSetChanged()

                    if (userPostsList.isEmpty()) {
                        Toast.makeText(this@SociallyOtherUser, "No posts yet", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@SociallyOtherUser, "Failed to load posts", Toast.LENGTH_SHORT).show()
                    findViewById<TextView>(R.id.postsCount)?.text = "0"
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SociallyOtherUser,
                    "Error loading posts: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                findViewById<TextView>(R.id.postsCount)?.text = "0"
            }
        }
    }
}
