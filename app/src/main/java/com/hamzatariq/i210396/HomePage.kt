package com.hamzatariq.i210396

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.hamzatariq.i210396.adapters.PostFeedAdapter
import com.hamzatariq.i210396.adapters.StoryAdapter
import com.hamzatariq.i210396.models.Post
import com.hamzatariq.i210396.models.StoryModel
import com.hamzatariq.i210396.models.UserStoryCollection
import com.hamzatariq.i210396.utils.ImageUtils
import com.hamzatariq.i210396.utils.FCMTokenManager
import com.hamzatariq.i210396.utils.OnlineStatusManager

class HomePage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: FirebaseDatabase
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

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize views
        storyImage = findViewById(R.id.story_image)
        profileIcon = findViewById(R.id.profile)

        // Initialize RecyclerView for stories
        storiesRecyclerView = findViewById(R.id.storiesRecyclerView)
        storiesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Initialize stories adapter
        storyAdapter = StoryAdapter(this, userStoriesList) { userStory ->
            // Open story viewer when clicked
            val activeStories = userStory.getActiveStories()
            if (activeStories.isNotEmpty()) {
                val intent = Intent(this, ViewStoryActivity::class.java)
                intent.putExtra("userId", userStory.userId)
                intent.putExtra("username", userStory.username)
                intent.putExtra("userProfileImage", userStory.userProfileImage)
                startActivity(intent)
            }
        }
        storiesRecyclerView.adapter = storyAdapter

        // Initialize RecyclerView for posts
        postsRecyclerView = findViewById(R.id.postsRecyclerView)
        postsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize posts adapter
        postFeedAdapter = PostFeedAdapter(this, postsList)
        postsRecyclerView.adapter = postFeedAdapter

        // Load user profile picture
        loadUserProfilePicture()

        // Load stories from Firebase
        loadStories()

        // Load posts from followed users
        loadFollowedUsersPosts()

        // Request notification permission and get FCM token
        FCMTokenManager.requestNotificationPermission(this)

        // Add story icon click listener
        findViewById<ImageView>(R.id.add_story_icon).setOnClickListener {
            val intent = Intent(this, Story::class.java)
            startActivity(intent)
        }

        // Story image click - view own story
        storyImage.setOnClickListener {
            try {
                viewOwnStory()
            } catch (e: Exception) {
                Toast.makeText(this, "Error opening story: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
                android.util.Log.e("HomePage", "Error viewing own story", e)
            }
        }

        // Camera icon - open story creation
        val cameraIcon = findViewById<ImageView>(R.id.camera)
        cameraIcon.setOnClickListener {
            val intent = Intent(this, Story::class.java)
            startActivity(intent)
        }

        // Explore
        val exploreIcon = findViewById<ImageView>(R.id.exlplore)
        exploreIcon.setOnClickListener {
            val intent = Intent(this, Explore::class.java)
            startActivity(intent)
        }

        // Share (DM)
        val shareIcon = findViewById<ImageView>(R.id.share)
        shareIcon.setOnClickListener {
            val intent = Intent(this, Messages::class.java)
            startActivity(intent)
        }

        // Like
        val likeIcon = findViewById<ImageView>(R.id.like)
        likeIcon.setOnClickListener {
            val intent = Intent(this, likeFollowing::class.java)
            startActivity(intent)
        }

        // Profile
        profileIcon.setOnClickListener {
            val intent = Intent(this, profileScreen::class.java)
            startActivity(intent)
        }

        // Post
        val postIcon = findViewById<ImageView>(R.id.post)
        postIcon.setOnClickListener {
            val intent = Intent(this, AddPostScreen::class.java)
            startActivity(intent)
        }

        // Set user online when app opens
        OnlineStatusManager.setUserOnline()
    }

    private fun loadFollowedUsersPosts() {
        val currentUserId = auth.currentUser?.uid ?: return

        // First, get the list of users the current user is following
        firestore.collection("users").document(currentUserId)
            .collection("following")
            .get()
            .addOnSuccessListener { followingDocuments ->
                val followedUserIds = mutableListOf<String>()

                // Add current user to show their own posts too
                followedUserIds.add(currentUserId)

                for (doc in followingDocuments) {
                    followedUserIds.add(doc.id)
                }

                // Now load posts from these users
                if (followedUserIds.isNotEmpty()) {
                    loadPostsFromUsers(followedUserIds)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load posts: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPostsFromUsers(userIds: List<String>) {
        val postsRef = database.reference.child("posts")

        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postsList.clear()

                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    if (post != null && userIds.contains(post.userId)) {
                        postsList.add(post)
                    }
                }

                // Sort by timestamp (newest first)
                postsList.sortByDescending { it.timestamp }
                postFeedAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomePage, "Failed to load posts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserProfilePicture() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val profileImageBase64 = document.getString("profileImageUrl") ?: ""
                        if (profileImageBase64.isNotEmpty()) {
                            ImageUtils.loadBase64Image(storyImage, profileImageBase64)
                            ImageUtils.loadBase64Image(profileIcon, profileImageBase64)
                        }
                    }
                }
        }
    }

    private fun loadStories() {
        val storiesRef = database.reference.child("stories")

        storiesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userStoriesList.clear()
                val currentUserId = auth.currentUser?.uid

                // Iterate through each user's stories
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue

                    // Skip own stories (they're shown separately)
                    if (userId == currentUserId) continue

                    val username = userSnapshot.child("username").getValue(String::class.java) ?: "User"
                    val userProfileImage = userSnapshot.child("userProfileImage").getValue(String::class.java) ?: ""
                    val lastUpdated = userSnapshot.child("lastUpdated").getValue(Long::class.java) ?: 0L

                    val storyItemsSnapshot = userSnapshot.child("storyItems")
                    val stories = mutableListOf<StoryModel>()

                    for (storySnapshot in storyItemsSnapshot.children) {
                        val story = storySnapshot.getValue(StoryModel::class.java)
                        if (story != null && !story.isExpired()) {
                            stories.add(story)
                        } else if (story != null && story.isExpired()) {
                            // Delete expired story
                            storySnapshot.ref.removeValue()
                        }
                    }

                    // Only add user if they have active stories
                    if (stories.isNotEmpty()) {
                        val userStoryCollection = UserStoryCollection(
                            userId = userId,
                            username = username,
                            userProfileImage = userProfileImage,
                            stories = stories,
                            lastUpdated = lastUpdated
                        )
                        userStoriesList.add(userStoryCollection)
                    } else {
                        // Delete user story node if no active stories
                        userSnapshot.ref.removeValue()
                    }
                }

                // Sort by last updated (most recent first)
                userStoriesList.sortByDescending { it.lastUpdated }

                storyAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun viewOwnStory() {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        // Check if user has stories
        database.reference.child("stories").child(currentUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.child("storyItems").exists()) {
                        val username = snapshot.child("username").getValue(String::class.java) ?: "User"
                        val userProfileImage = snapshot.child("userProfileImage").getValue(String::class.java) ?: ""

                        // Open story viewer for own stories
                        val intent = Intent(this@HomePage, ViewStoryActivity::class.java)
                        intent.putExtra("userId", currentUser.uid)
                        intent.putExtra("username", username)
                        intent.putExtra("userProfileImage", userProfileImage)
                        intent.putExtra("isOwnStory", true)
                        startActivity(intent)
                    } else {
                        // No story found, open story creation
                        val intent = Intent(this@HomePage, Story::class.java)
                        startActivity(intent)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    override fun onResume() {
        super.onResume()
        // Reload stories when returning to this activity
        loadStories()
        loadUserProfilePicture()

        // Set user online when returning to app
        OnlineStatusManager.setUserOnline()
    }

    override fun onPause() {
        super.onPause()
        // Set user offline when leaving app
        OnlineStatusManager.setUserOffline()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Set user offline when app closes
        OnlineStatusManager.setUserOffline()
    }
}
