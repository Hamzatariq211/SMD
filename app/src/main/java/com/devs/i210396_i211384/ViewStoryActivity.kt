package com.devs.i210396_i211384

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.network.StoryResponse
import com.devs.i210396_i211384.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewStoryActivity : AppCompatActivity() {
    private val apiService = ApiService.create()
    private lateinit var storyImageView: ImageView
    private lateinit var userProfileImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var storyProgress: ProgressBar
    private lateinit var deleteStoryLayout: LinearLayout
    private var storyTimer: CountDownTimer? = null

    private var userId: String? = null
    private var username: String = "User"
    private var userProfileImageBase64: String = ""
    private var isOwnStory: Boolean = false

    private var storyItems = mutableListOf<StoryResponse>()
    private var currentStoryIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_story)

        // Initialize SessionManager
        SessionManager.init(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        storyImageView = findViewById(R.id.storyImageView)
        userProfileImage = findViewById(R.id.userProfileImage)
        usernameText = findViewById(R.id.usernameText)
        storyProgress = findViewById(R.id.storyProgress)
        deleteStoryLayout = findViewById(R.id.deleteStoryLayout)

        // Get data from intent
        userId = intent.getStringExtra("userId")
        username = intent.getStringExtra("username") ?: "User"
        userProfileImageBase64 = intent.getStringExtra("userProfileImage") ?: ""

        // Check if viewing own story
        val currentUserId = SessionManager.getUserId()
        isOwnStory = userId == currentUserId

        // Display user info
        if (userProfileImageBase64.isNotEmpty()) {
            ImageUtils.loadBase64Image(userProfileImage, userProfileImageBase64)
        }
        usernameText.text = username

        // Show delete button if it's user's own story
        if (isOwnStory) {
            deleteStoryLayout.visibility = View.VISIBLE
            deleteStoryLayout.setOnClickListener {
                Toast.makeText(this, "Delete story feature coming soon", Toast.LENGTH_SHORT).show()
            }
        }

        // Close button
        findViewById<ImageView>(R.id.btnCloseStory).setOnClickListener {
            finish()
        }

        // Load user's stories from API
        loadUserStories()

        // Click to move to next story or finish
        storyImageView.setOnClickListener {
            moveToNextStory()
        }
    }

    private fun loadUserStories() {
        if (userId == null || userId!!.isEmpty()) {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Toast.makeText(this, "Loading stories...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getUserStories(userId!!)
                }

                if (response.isSuccessful) {
                    val stories = response.body() ?: emptyList<StoryResponse>()

                    storyItems.clear()

                    // Filter out expired stories (client-side check)
                    val currentTime = System.currentTimeMillis()
                    for (story in stories) {
                        if (story.expiryTime > currentTime) {
                            storyItems.add(story)
                        }
                    }

                    if (storyItems.isEmpty()) {
                        Toast.makeText(this@ViewStoryActivity, "No active stories", Toast.LENGTH_SHORT).show()
                        finish()
                        return@launch
                    }

                    // Get profile image from the first story's data
                    if (storyItems.isNotEmpty()) {
                        userProfileImageBase64 = storyItems[0].userProfileImage
                        if (userProfileImageBase64.isNotEmpty()) {
                            ImageUtils.loadBase64Image(userProfileImage, userProfileImageBase64)
                        }
                    }

                    // Display first story
                    displayCurrentStory()
                } else {
                    Toast.makeText(this@ViewStoryActivity, "Failed to load stories", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ViewStoryActivity,
                    "Error loading stories: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                android.util.Log.e("ViewStoryActivity", "Error loading stories", e)
                finish()
            }
        }
    }

    private fun displayCurrentStory() {
        if (currentStoryIndex >= storyItems.size) {
            finish()
            return
        }

        val currentStory = storyItems[currentStoryIndex]

        // Display story image
        ImageUtils.loadBase64Image(storyImageView, currentStory.storyImageBase64)

        // Start progress timer (5 seconds per story)
        startStoryTimer()
    }

    private fun startStoryTimer() {
        storyTimer?.cancel()

        storyProgress.max = 100
        storyProgress.progress = 0

        storyTimer = object : CountDownTimer(5000, 50) {
            override fun onTick(millisUntilFinished: Long) {
                val progress = ((5000 - millisUntilFinished) * 100 / 5000).toInt()
                storyProgress.progress = progress
            }

            override fun onFinish() {
                storyProgress.progress = 100
                moveToNextStory()
            }
        }.start()
    }

    private fun moveToNextStory() {
        currentStoryIndex++
        if (currentStoryIndex < storyItems.size) {
            displayCurrentStory()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        storyTimer?.cancel()
    }
}
