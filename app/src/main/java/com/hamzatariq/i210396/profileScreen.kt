package com.hamzatariq.i210396

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.hamzatariq.i210396.adapters.PostGridAdapter
import com.hamzatariq.i210396.models.Post
import com.hamzatariq.i210396.utils.ImageUtils

class profileScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: FirebaseDatabase

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

        // Apply window insets to handle system bars properly
        val mainLayout = findViewById<LinearLayout>(R.id.main)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize views
        profilePic = findViewById(R.id.profilePic)
        tvNatasha = findViewById(R.id.tvNatasha)
        username = findViewById(R.id.username)

        // Initialize RecyclerView for posts grid
        postsRecyclerView = findViewById(R.id.posted_pictures)
        postsRecyclerView.layoutManager = GridLayoutManager(this, 3) // 3 columns

        // Initialize adapter
        postAdapter = PostGridAdapter(this, userPostsList) { post ->
            // TODO: Open post detail view
        }
        postsRecyclerView.adapter = postAdapter

        // Load user profile data
        loadUserProfile()

        // Load user posts
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

        // Menu icon - Logout dialog
        findViewById<ImageView>(R.id.menuIcon).setOnClickListener {
            showMenuOptions()
        }
    }

    private fun showMenuOptions() {
        val options = mutableListOf("Follow Requests", "Settings", "Logout")

        AlertDialog.Builder(this)
            .setTitle("Menu")
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        // Follow Requests
                        val intent = Intent(this, FollowRequestsActivity::class.java)
                        startActivity(intent)
                    }
                    1 -> {
                        // Settings - currently opens EditProfile
                        val intent = Intent(this, EditProfile::class.java)
                        startActivity(intent)
                    }
                    2 -> {
                        // Logout
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
                // Sign out from Firebase
                auth.signOut()

                // Navigate to login screen
                val intent = Intent(this, loginUser::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val usernameText = document.getString("username") ?: "User"
                        val fullName = document.getString("firstName") + " " + document.getString("lastName")
                        val profileImageBase64 = document.getString("profileImageUrl") ?: ""
                        val bio = document.getString("bio") ?: ""
                        val website = document.getString("website") ?: ""

                        tvNatasha.text = usernameText
                        username.text = fullName

                        // Populate bio section
                        findViewById<TextView>(R.id.nameText)?.text = fullName
                        findViewById<TextView>(R.id.descriptionText)?.text = bio
                        findViewById<TextView>(R.id.extraLine)?.text = website

                        if (profileImageBase64.isNotEmpty()) {
                            ImageUtils.loadBase64Image(profilePic, profileImageBase64)
                            ImageUtils.loadBase64Image(findViewById(R.id.profile), profileImageBase64)
                        }
                    }
                }

            // Load genuine follower/following counts
            loadFollowerCounts(currentUser.uid)
        }
    }

    private fun loadFollowerCounts(userId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Get followers count
        firestore.collection("users").document(userId)
            .collection("followers")
            .get()
            .addOnSuccessListener { documents ->
                val followersCount = documents.size()
                val followersCountView = findViewById<TextView>(R.id.followersCount)
                followersCountView?.text = followersCount.toString()

                // Add click listener to show followers
                findViewById<LinearLayout>(R.id.statsContainer)?.let { statsContainer ->
                    // Find the Followers section (second LinearLayout in statsContainer)
                    val followersSection = (statsContainer.getChildAt(1) as? LinearLayout)
                    followersSection?.setOnClickListener {
                        val intent = Intent(this, FollowersFollowingActivity::class.java)
                        intent.putExtra("userId", currentUserId)
                        intent.putExtra("username", tvNatasha.text.toString())
                        intent.putExtra("initialTab", 0) // 0 = followers tab
                        startActivity(intent)
                    }
                }
            }

        // Get following count
        firestore.collection("users").document(userId)
            .collection("following")
            .get()
            .addOnSuccessListener { documents ->
                val followingCount = documents.size()
                val followingCountView = findViewById<TextView>(R.id.followingCount)
                followingCountView?.text = followingCount.toString()

                // Add click listener to show following
                findViewById<LinearLayout>(R.id.statsContainer)?.let { statsContainer ->
                    // Find the Following section (third LinearLayout in statsContainer)
                    val followingSection = (statsContainer.getChildAt(2) as? LinearLayout)
                    followingSection?.setOnClickListener {
                        val intent = Intent(this, FollowersFollowingActivity::class.java)
                        intent.putExtra("userId", currentUserId)
                        intent.putExtra("username", tvNatasha.text.toString())
                        intent.putExtra("initialTab", 1) // 1 = following tab
                        startActivity(intent)
                    }
                }
            }
    }

    private fun loadUserPosts() {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        database.reference.child("posts")
            .orderByChild("userId")
            .equalTo(currentUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userPostsList.clear()

                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(Post::class.java)
                        if (post != null) {
                            userPostsList.add(post)
                        }
                    }

                    // Sort by timestamp (newest first)
                    userPostsList.sortByDescending { it.timestamp }

                    // Update posts count display
                    findViewById<TextView>(R.id.postsCount)?.text = userPostsList.size.toString()

                    postAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
        loadUserPosts()
    }
}
