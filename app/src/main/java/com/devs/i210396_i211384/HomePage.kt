package com.devs.i210396_i211384

import android.content.Intent
import android.os.Bundle
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
import com.devs.i210396_i211384.adapters.PostFeedAdapter
import com.devs.i210396_i211384.adapters.StoryAdapter
import com.devs.i210396_i211384.models.Post
import com.devs.i210396_i211384.models.StoryModel
import com.devs.i210396_i211384.models.UserStoryCollection
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.utils.FCMTokenManager
import com.devs.i210396_i211384.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomePage : AppCompatActivity() {
    private val apiService = ApiService.create()
    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var postFeedAdapter: PostFeedAdapter
    private lateinit var storyImage: ImageView
    private lateinit var profileIcon: ImageView
    private val userStoriesList = mutableListOf<UserStoryCollection>()
    private val postsList = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)

        val mainLayout = findViewById<LinearLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        storyImage = findViewById(R.id.story_image)
        profileIcon = findViewById(R.id.profile)

        // Initialize RecyclerView for stories
        storiesRecyclerView = findViewById(R.id.storiesRecyclerView)
        storiesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        storyAdapter = StoryAdapter(this, userStoriesList) { userStory ->
            val activeStories = userStory.getActiveStories()
            if (activeStories.isNotEmpty()) {
                val intent = Intent(this, ViewStoryActivity::class.java)
                intent.putExtra("userId", userStory.userId)
                intent.putExtra("username", userStory.username)
                // DON'T pass userProfileImage - it causes TransactionTooLargeException
                // ViewStoryActivity will fetch it from the API
                startActivity(intent)
            }
        }
        storiesRecyclerView.adapter = storyAdapter

        // Initialize RecyclerView for posts
        postsRecyclerView = findViewById(R.id.postsRecyclerView)
        postsRecyclerView.layoutManager = LinearLayoutManager(this)

        postFeedAdapter = PostFeedAdapter(this, postsList)
        postsRecyclerView.adapter = postFeedAdapter

        // Load user profile picture from MySQL
        loadUserProfilePicture()

        // Load stories from MySQL (will be empty initially)
        loadStories()

        // Load posts feed from MySQL (will be empty initially)
        loadPostsFeed()

        // Request notification permission and get FCM token
        FCMTokenManager.requestNotificationPermission(this)

        // Add story icon click listener
        findViewById<ImageView>(R.id.add_story_icon).setOnClickListener {
            val intent = Intent(this, Story::class.java)
            startActivity(intent)
        }

        storyImage.setOnClickListener {
            try {
                viewOwnStory()
            } catch (e: Exception) {
                Toast.makeText(this, "Error opening story: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        val cameraIcon = findViewById<ImageView>(R.id.camera)
        cameraIcon.setOnClickListener {
            val intent = Intent(this, Story::class.java)
            startActivity(intent)
        }

        val exploreIcon = findViewById<ImageView>(R.id.exlplore)
        exploreIcon.setOnClickListener {
            val intent = Intent(this, Explore::class.java)
            startActivity(intent)
        }

        val shareIcon = findViewById<ImageView>(R.id.share)
        shareIcon.setOnClickListener {
            val intent = Intent(this, Messages::class.java)
            startActivity(intent)
        }

        val likeIcon = findViewById<ImageView>(R.id.like)
        likeIcon.setOnClickListener {
            val intent = Intent(this, likeFollowing::class.java)
            startActivity(intent)
        }

        profileIcon.setOnClickListener {
            val intent = Intent(this, profileScreen::class.java)
            startActivity(intent)
        }

        val postIcon = findViewById<ImageView>(R.id.post)
        postIcon.setOnClickListener {
            val intent = Intent(this, AddPostScreen::class.java)
            startActivity(intent)
        }

        updateOnlineStatus(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateOnlineStatus(false)
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

    private fun loadPostsFeed() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getPostsFeed()
                }

                if (response.isSuccessful) {
                    val apiPosts = response.body() ?: emptyList()
                    postsList.clear()

                    for (apiPost in apiPosts) {
                        // Create Post from API response
                        postsList.add(
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

                    postFeedAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@HomePage, "No posts available yet", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomePage, "Connect to server: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserProfilePicture() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getCurrentUser()
                }

                if (response.isSuccessful) {
                    val user = response.body()!!
                    val profileImageBase64 = user.profileImageUrl ?: ""

                    if (profileImageBase64.isNotEmpty()) {
                        ImageUtils.loadBase64Image(storyImage, profileImageBase64)
                        ImageUtils.loadBase64Image(profileIcon, profileImageBase64)
                    }
                }
            } catch (e: Exception) {
                // Silently fail or use default image
            }
        }
    }

    private fun loadStories() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getStories()
                }

                if (response.isSuccessful) {
                    val apiStoriesCollections = response.body() ?: emptyList()
                    userStoriesList.clear()

                    for (collection in apiStoriesCollections) {
                        val stories = collection.stories.map { apiStory ->
                            StoryModel(
                                storyId = apiStory.storyId,
                                userId = apiStory.userId,
                                username = "",
                                userProfileImage = "",
                                storyImageBase64 = apiStory.storyImageBase64,
                                timestamp = apiStory.timestamp,
                                expiryTime = apiStory.expiryTime,
                                viewCount = apiStory.viewCount
                            )
                        }.toMutableList()

                        val userStoryCollection = UserStoryCollection(
                            userId = collection.userId,
                            username = collection.username,
                            userProfileImage = collection.userProfileImage,
                            stories = stories,
                            lastUpdated = stories.maxOfOrNull { it.timestamp } ?: 0L
                        )
                        userStoriesList.add(userStoryCollection)
                    }

                    storyAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    private fun viewOwnStory() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getCurrentUser()
                }

                if (response.isSuccessful) {
                    val user = response.body()!!

                    val intent = Intent(this@HomePage, ViewStoryActivity::class.java)
                    intent.putExtra("userId", user.id)
                    intent.putExtra("username", user.username)
                    // DON'T pass userProfileImage - it's too large and causes TransactionTooLargeException
                    // ViewStoryActivity will fetch it from the API
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomePage, "No active stories", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
