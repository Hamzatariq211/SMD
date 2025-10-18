package com.hamzatariq.i210396

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.hamzatariq.i210396.adapters.CommentAdapter
import com.hamzatariq.i210396.models.Comment
import com.hamzatariq.i210396.models.Post
import com.hamzatariq.i210396.utils.ImageUtils
import java.util.*

class PostDetailActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: FirebaseDatabase

    private lateinit var userProfileImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var postImage: ImageView
    private lateinit var btnLike: ImageView
    private lateinit var btnComment: ImageView
    private lateinit var likesCount: TextView
    private lateinit var captionUsername: TextView
    private lateinit var captionText: TextView
    private lateinit var viewAllComments: TextView
    private lateinit var timestampText: TextView
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var currentUserProfileImage: ImageView
    private lateinit var commentInput: EditText
    private lateinit var btnPostComment: TextView

    private lateinit var commentAdapter: CommentAdapter
    private val commentsList = mutableListOf<Comment>()

    private var postId: String? = null
    private var currentPost: Post? = null
    private var isLiked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_post_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance()

        // Get post ID from intent
        postId = intent.getStringExtra("postId")
        if (postId == null) {
            Toast.makeText(this, "Error loading post", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        initializeViews()

        // Setup RecyclerView
        setupCommentsRecyclerView()

        // Load post data
        loadPostData()

        // Load current user profile
        loadCurrentUserProfile()

        // Setup click listeners
        setupClickListeners()
    }

    private fun initializeViews() {
        userProfileImage = findViewById(R.id.userProfileImage)
        usernameText = findViewById(R.id.usernameText)
        postImage = findViewById(R.id.postImage)
        btnLike = findViewById(R.id.btnLike)
        btnComment = findViewById(R.id.btnComment)
        likesCount = findViewById(R.id.likesCount)
        captionUsername = findViewById(R.id.captionUsername)
        captionText = findViewById(R.id.captionText)
        viewAllComments = findViewById(R.id.viewAllComments)
        timestampText = findViewById(R.id.timestampText)
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView)
        currentUserProfileImage = findViewById(R.id.currentUserProfileImage)
        commentInput = findViewById(R.id.commentInput)
        btnPostComment = findViewById(R.id.btnPostComment)
    }

    private fun setupCommentsRecyclerView() {
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter(this, commentsList)
        commentsRecyclerView.adapter = commentAdapter
    }

    private fun setupClickListeners() {
        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Like button
        btnLike.setOnClickListener {
            toggleLike()
        }

        // Comment button - focus on input
        btnComment.setOnClickListener {
            commentInput.requestFocus()
        }

        // Post comment button
        btnPostComment.setOnClickListener {
            postComment()
        }

        // Likes count - show users who liked
        likesCount.setOnClickListener {
            // TODO: Show list of users who liked
            Toast.makeText(this, "Show users who liked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPostData() {
        database.reference.child("posts").child(postId!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentPost = snapshot.getValue(Post::class.java)
                    if (currentPost != null) {
                        displayPost(currentPost!!)
                        loadComments()
                    } else {
                        Toast.makeText(this@PostDetailActivity, "Post not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PostDetailActivity, "Error loading post", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun displayPost(post: Post) {
        // Load user profile image
        if (post.userProfileImage.isNotEmpty()) {
            ImageUtils.loadBase64Image(userProfileImage, post.userProfileImage)
        }

        // Set username
        usernameText.text = post.username
        captionUsername.text = post.username

        // Load post image
        if (post.postImageBase64.isNotEmpty()) {
            ImageUtils.loadBase64Image(postImage, post.postImageBase64)
        }

        // Set caption
        if (post.caption.isNotEmpty()) {
            captionText.text = post.caption
            captionText.visibility = View.VISIBLE
        } else {
            captionText.visibility = View.GONE
        }

        // Update likes count
        updateLikesDisplay(post)

        // Update timestamp
        timestampText.text = getTimeAgo(post.timestamp)

        // Check if current user liked the post
        val currentUserId = auth.currentUser?.uid
        isLiked = currentUserId != null && post.likes.containsKey(currentUserId)
        updateLikeButton()

        // Update comments count
        viewAllComments.text = "View all ${post.comments.size} comments"
    }

    private fun updateLikesDisplay(post: Post) {
        val count = post.likes.size
        likesCount.text = when (count) {
            0 -> "Be the first to like this"
            1 -> "1 like"
            else -> "$count likes"
        }
    }

    private fun updateLikeButton() {
        if (isLiked) {
            btnLike.setImageResource(R.drawable.likebold)
        } else {
            btnLike.setImageResource(R.drawable.like)
        }
    }

    private fun toggleLike() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null || postId == null) return

        val postRef = database.reference.child("posts").child(postId!!)

        if (isLiked) {
            // Unlike the post
            postRef.child("likes").child(currentUserId).removeValue()
                .addOnSuccessListener {
                    isLiked = false
                    updateLikeButton()
                }
        } else {
            // Like the post
            postRef.child("likes").child(currentUserId).setValue(true)
                .addOnSuccessListener {
                    isLiked = true
                    updateLikeButton()
                }
        }
    }

    private fun loadComments() {
        if (postId == null) return

        database.reference.child("posts").child(postId!!).child("comments")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    commentsList.clear()

                    for (commentSnapshot in snapshot.children) {
                        val comment = commentSnapshot.getValue(Comment::class.java)
                        if (comment != null) {
                            commentsList.add(comment)
                        }
                    }

                    // Sort by timestamp (oldest first)
                    commentsList.sortBy { it.timestamp }

                    commentAdapter.notifyDataSetChanged()

                    // Update comment count
                    viewAllComments.text = "View all ${commentsList.size} comments"
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun postComment() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val commentText = commentInput.text.toString().trim()
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (postId == null) return

        // Get user data from Firestore
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: "User"
                    val profileImageBase64 = document.getString("profileImageUrl") ?: ""

                    // Create comment
                    val commentId = UUID.randomUUID().toString()
                    val comment = Comment(
                        commentId = commentId,
                        userId = currentUser.uid,
                        username = username,
                        userProfileImage = profileImageBase64,
                        commentText = commentText,
                        timestamp = System.currentTimeMillis()
                    )

                    // Save to Firebase
                    database.reference.child("posts").child(postId!!).child("comments").child(commentId)
                        .setValue(comment)
                        .addOnSuccessListener {
                            commentInput.text.clear()
                            Toast.makeText(this, "Comment posted", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to post comment: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCurrentUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val profileImageBase64 = document.getString("profileImageUrl") ?: ""
                        if (profileImageBase64.isNotEmpty()) {
                            ImageUtils.loadBase64Image(currentUserProfileImage, profileImageBase64)
                        }
                    }
                }
        }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} minutes ago"
            diff < 86400000 -> "${diff / 3600000} hours ago"
            diff < 604800000 -> "${diff / 86400000} days ago"
            else -> "${diff / 604800000} weeks ago"
        }
    }
}
