package com.devs.i210396_i211384

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devs.i210396_i211384.adapters.CommentAdapter
import com.devs.i210396_i211384.models.Comment
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.network.LikePostRequest
import com.devs.i210396_i211384.network.CommentRequest
import com.devs.i210396_i211384.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostDetailActivity : AppCompatActivity() {
    private val apiService = ApiService.create()

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
    private var isLiked = false
    private var likeCount = 0
    private var commentCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_post_detail)

        // Initialize SessionManager
        SessionManager.init(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        // Load post data from MySQL API
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
            Toast.makeText(this, "Liked by $likeCount people", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPostData() {
        if (postId == null) return

        lifecycleScope.launch {
            try {
                // Fetch the specific post by ID
                val response = withContext(Dispatchers.IO) {
                    apiService.getPost(postId!!)
                }

                if (response.isSuccessful) {
                    val post = response.body()

                    if (post != null) {
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

                        // Update likes
                        isLiked = post.isLiked
                        likeCount = post.likeCount
                        updateLikesDisplay()
                        updateLikeButton()

                        // Update timestamp
                        timestampText.text = getTimeAgo(post.timestamp)

                        // Update comments count
                        commentCount = post.commentCount
                        viewAllComments.text = "View all $commentCount comments"

                        // Load comments
                        loadComments()
                    } else {
                        Toast.makeText(this@PostDetailActivity, "Post not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    val errorMsg = "Failed to load post: ${response.code()} - ${response.message()}"
                    android.util.Log.e("PostDetailActivity", errorMsg)
                    Toast.makeText(this@PostDetailActivity, errorMsg, Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                val errorMsg = "Error loading post: ${e.message}"
                android.util.Log.e("PostDetailActivity", errorMsg, e)
                Toast.makeText(this@PostDetailActivity, errorMsg, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun updateLikesDisplay() {
        likesCount.text = when (likeCount) {
            0 -> "Be the first to like this"
            1 -> "1 like"
            else -> "$likeCount likes"
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
        if (postId == null) return

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.likePost(LikePostRequest(postId!!))
                }

                if (response.isSuccessful) {
                    // Toggle like state
                    isLiked = !isLiked
                    likeCount = if (isLiked) likeCount + 1 else likeCount - 1

                    updateLikeButton()
                    updateLikesDisplay()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PostDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadComments() {
        if (postId == null) return

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getComments(postId!!)
                }

                if (response.isSuccessful) {
                    val apiComments = response.body() ?: emptyList()
                    commentsList.clear()

                    for (apiComment in apiComments) {
                        val comment = Comment(
                            commentId = apiComment.commentId,
                            userId = apiComment.userId,
                            username = apiComment.username,
                            userProfileImage = apiComment.userProfileImage,
                            commentText = apiComment.commentText,
                            timestamp = apiComment.timestamp
                        )
                        commentsList.add(comment)
                    }

                    commentAdapter.notifyDataSetChanged()

                    // Update comment count
                    viewAllComments.text = "View all ${commentsList.size} comments"
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun postComment() {
        if (!SessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val commentText = commentInput.text.toString().trim()
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (postId == null) return

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.addComment(CommentRequest(postId!!, commentText))
                }

                if (response.isSuccessful) {
                    commentInput.text.clear()
                    Toast.makeText(this@PostDetailActivity, "Comment posted", Toast.LENGTH_SHORT).show()

                    // Reload comments
                    loadComments()

                    // Update comment count
                    commentCount++
                    viewAllComments.text = "View all $commentCount comments"
                } else {
                    Toast.makeText(this@PostDetailActivity, "Failed to post comment", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PostDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCurrentUserProfile() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getCurrentUser()
                }

                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        val profileImageBase64 = user.profileImageUrl ?: ""
                        if (profileImageBase64.isNotEmpty()) {
                            ImageUtils.loadBase64Image(currentUserProfileImage, profileImageBase64)
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> "${diff / 604800000}w ago"
        }
    }
}
