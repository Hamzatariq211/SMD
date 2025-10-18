package com.hamzatariq.i210396

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hamzatariq.i210396.adapters.PostGridAdapter
import com.hamzatariq.i210396.models.Post
import com.hamzatariq.i210396.utils.ImageUtils
import com.hamzatariq.i210396.utils.NotificationHelper

class SociallyOtherUser : AppCompatActivity() {

    private var isFollowing = false
    private var isPrivateAccount = false
    private var hasRequestedFollow = false
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: FirebaseDatabase
    private var otherUserId: String = ""
    private var currentUserName: String = ""

    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostGridAdapter
    private val userPostsList = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_other_user)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance()

        // Get user data from intent
        otherUserId = intent.getStringExtra("userId") ?: ""

        // Load user profile picture (current user's for navigation)
        loadCurrentUserProfilePicture()

        // Load other user's profile
        loadOtherUserProfile()

        // Setup RecyclerView for posts
        postsRecyclerView = findViewById(R.id.posted_pictures)
        postsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        postAdapter = PostGridAdapter(this, userPostsList) { }
        postsRecyclerView.adapter = postAdapter

        // Load other user's posts
        loadOtherUserPosts()

        // Check if already following
        checkFollowingStatus()

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

        // Get current user name
        loadCurrentUserName()
    }

    private fun loadCurrentUserName() {
        val currentUserId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentUserName = document.getString("username") ?: "User"
                }
            }
    }

    private fun loadOtherUserProfile() {
        if (otherUserId.isEmpty()) return

        // Load from intent first
        val username = intent.getStringExtra("username") ?: ""
        val firstName = intent.getStringExtra("firstName") ?: ""
        val lastName = intent.getStringExtra("lastName") ?: ""
        val bio = intent.getStringExtra("bio") ?: ""
        val profileImageUrl = intent.getStringExtra("profileImageUrl") ?: ""

        findViewById<TextView>(R.id.usernameText)?.text = username
        findViewById<TextView>(R.id.username)?.text = username
        findViewById<TextView>(R.id.name)?.text = "$firstName $lastName"
        findViewById<TextView>(R.id.bio)?.text = bio

        if (profileImageUrl.isNotEmpty()) {
            ImageUtils.loadBase64Image(findViewById(R.id.profilePic), profileImageUrl)
        }

        // Load follower/following counts
        loadFollowerCounts()

        // Check if the account is private
        checkIfPrivateAccount()
    }

    private fun checkIfPrivateAccount() {
        if (otherUserId.isEmpty()) return

        // Check the privacy setting of the user
        firestore.collection("users").document(otherUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    isPrivateAccount = document.getBoolean("isPrivate") == true

                    // Show/hide content based on privacy
                    if (isPrivateAccount) {
                        handlePrivateAccount()
                    } else {
                        // Public account, load posts normally
                        loadOtherUserPosts()
                    }
                }
            }
    }

    private fun handlePrivateAccount() {
        // Hide posts and show follow button
        findViewById<TextView>(R.id.postsCount)?.text = "0"
        postsRecyclerView.visibility = View.GONE
        findViewById<Button>(R.id.btnFollow).visibility = View.VISIBLE

        // Check if the current user has already sent a follow request
        checkFollowRequestStatus()
    }

    private fun checkFollowRequestStatus() {
        val currentUserId = auth.currentUser?.uid ?: return
        if (otherUserId.isEmpty()) return

        // Check if there is a follow request pending
        firestore.collection("users").document(otherUserId)
            .collection("followRequests")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                hasRequestedFollow = document.exists()

                // Update follow button text accordingly
                val btnFollow = findViewById<Button>(R.id.btnFollow)
                if (hasRequestedFollow) {
                    btnFollow.text = getString(R.string.request_sent)
                    btnFollow.isEnabled = false
                } else {
                    btnFollow.text = getString(R.string.follow)
                    btnFollow.isEnabled = true
                }
            }
    }

    private fun loadFollowerCounts() {
        if (otherUserId.isEmpty()) return

        // Get followers count
        firestore.collection("users").document(otherUserId)
            .collection("followers")
            .get()
            .addOnSuccessListener { documents ->
                val followersCount = documents.size()
                findViewById<TextView>(R.id.followersCount)?.text = formatCount(followersCount)
            }

        // Get following count
        firestore.collection("users").document(otherUserId)
            .collection("following")
            .get()
            .addOnSuccessListener { documents ->
                val followingCount = documents.size()
                findViewById<TextView>(R.id.followingCount)?.text = formatCount(followingCount)
            }
    }

    private fun loadOtherUserPosts() {
        if (otherUserId.isEmpty()) return

        database.reference.child("posts")
            .orderByChild("userId")
            .equalTo(otherUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userPostsList.clear()

                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(Post::class.java)
                        if (post != null) {
                            userPostsList.add(post)
                        }
                    }

                    userPostsList.sortByDescending { it.timestamp }
                    findViewById<TextView>(R.id.postsCount)?.text = userPostsList.size.toString()
                    postAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun checkFollowingStatus() {
        val currentUserId = auth.currentUser?.uid ?: return
        if (otherUserId.isEmpty()) return

        firestore.collection("users").document(currentUserId)
            .collection("following")
            .document(otherUserId)
            .get()
            .addOnSuccessListener { document ->
                isFollowing = document.exists()
                applyFollowStyle(findViewById(R.id.btnFollow), isFollowing)
            }
    }

    private fun toggleFollow() {
        val currentUserId = auth.currentUser?.uid ?: return
        if (otherUserId.isEmpty()) return

        val btnFollow = findViewById<Button>(R.id.btnFollow)

        if (isFollowing) {
            // Unfollow
            firestore.collection("users").document(currentUserId)
                .collection("following")
                .document(otherUserId)
                .delete()

            firestore.collection("users").document(otherUserId)
                .collection("followers")
                .document(currentUserId)
                .delete()
                .addOnSuccessListener {
                    isFollowing = false
                    applyFollowStyle(btnFollow, isFollowing)
                    loadFollowerCounts()
                    Toast.makeText(this, "Unfollowed", Toast.LENGTH_SHORT).show()

                    // Show posts if account is private
                    if (isPrivateAccount) {
                        postsRecyclerView.visibility = View.GONE
                    }
                }
        } else {
            // Check if account is private
            if (isPrivateAccount && !hasRequestedFollow) {
                // Send follow request instead of following directly
                sendFollowRequest()
            } else {
                // Public account - follow directly
                val followData = hashMapOf(
                    "userId" to otherUserId,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                firestore.collection("users").document(currentUserId)
                    .collection("following")
                    .document(otherUserId)
                    .set(followData)

                val followerData = hashMapOf(
                    "userId" to currentUserId,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                firestore.collection("users").document(otherUserId)
                    .collection("followers")
                    .document(currentUserId)
                    .set(followerData)
                    .addOnSuccessListener {
                        isFollowing = true
                        applyFollowStyle(btnFollow, isFollowing)
                        loadFollowerCounts()
                        Toast.makeText(this, "Following", Toast.LENGTH_SHORT).show()

                        // Send notification to the followed user
                        NotificationHelper.sendFollowerNotification(
                            followedUserId = otherUserId,
                            followerName = currentUserName,
                            followerId = currentUserId
                        )

                        // Show posts
                        postsRecyclerView.visibility = View.VISIBLE
                        loadOtherUserPosts()
                    }
            }
        }
    }

    private fun sendFollowRequest() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Get current user info
        firestore.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: ""
                    val profileImageUrl = document.getString("profileImageUrl") ?: ""

                    // Create follow request
                    val followRequest = hashMapOf(
                        "fromUserId" to currentUserId,
                        "fromUsername" to username,
                        "fromProfileImageUrl" to profileImageUrl,
                        "toUserId" to otherUserId,
                        "status" to "pending",
                        "timestamp" to FieldValue.serverTimestamp()
                    )

                    // Add follow request to the other user's followRequests collection
                    firestore.collection("users").document(otherUserId)
                        .collection("followRequests")
                        .document(currentUserId)
                        .set(followRequest)
                        .addOnSuccessListener {
                            hasRequestedFollow = true
                            val btnFollow = findViewById<Button>(R.id.btnFollow)
                            btnFollow.text = "Requested"
                            btnFollow.isEnabled = false
                            Toast.makeText(this, "Follow request sent", Toast.LENGTH_SHORT).show()

                            // Send notification
                            NotificationHelper.sendFollowRequestNotification(
                                toUserId = otherUserId,
                                fromUsername = username,
                                fromUserId = currentUserId
                            )
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to send request: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun loadCurrentUserProfilePicture() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val profileImageBase64 = document.getString("profileImageUrl") ?: ""
                    val profileNavIcon = findViewById<ImageView>(R.id.profile)
                    ImageUtils.loadBase64Image(profileNavIcon, profileImageBase64)
                }
            }
    }

    private fun applyFollowStyle(button: Button, following: Boolean) {
        if (following) {
            button.text = getString(R.string.following)
            button.background = ContextCompat.getDrawable(this, R.drawable.bg_follow_white)
            button.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        } else {
            button.text = getString(R.string.follow)
            button.background = ContextCompat.getDrawable(this, R.drawable.bg_follow_brown)
            button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        }
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= 1000000 -> String.format(java.util.Locale.US, "%.1fM", count / 1000000.0)
            count >= 1000 -> String.format(java.util.Locale.US, "%.1fK", count / 1000.0)
            else -> count.toString()
        }
    }
}
