package com.hamzatariq.i210396

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hamzatariq.i210396.models.StoryModel
import com.hamzatariq.i210396.utils.ImageUtils

class ViewStoryActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
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

    private var storyItems = mutableListOf<StoryModel>()
    private var currentStoryIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_story)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

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
        isOwnStory = intent.getBooleanExtra("isOwnStory", false)

        // Display user info
        if (userProfileImageBase64.isNotEmpty()) {
            ImageUtils.loadBase64Image(userProfileImage, userProfileImageBase64)
        }
        usernameText.text = username

        // Show delete button if it's user's own story
        if (isOwnStory) {
            deleteStoryLayout.visibility = View.VISIBLE
            deleteStoryLayout.setOnClickListener {
                showDeleteConfirmationDialog()
            }
        }

        // Close button
        findViewById<ImageView>(R.id.btnCloseStory).setOnClickListener {
            finish()
        }

        // Load user's stories from Firebase
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

        // Show loading toast
        Toast.makeText(this, "Loading stories...", Toast.LENGTH_SHORT).show()

        // Load all story items for this user
        database.reference.child("stories").child(userId!!).child("storyItems")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    storyItems.clear()

                    if (!snapshot.exists()) {
                        Toast.makeText(this@ViewStoryActivity, "No stories found", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }

                    for (storySnapshot in snapshot.children) {
                        val story = storySnapshot.getValue(StoryModel::class.java)
                        if (story != null && !story.isExpired()) {
                            storyItems.add(story)
                        }
                    }

                    if (storyItems.isEmpty()) {
                        Toast.makeText(this@ViewStoryActivity, "No active stories", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }

                    // Sort by timestamp (oldest first)
                    storyItems.sortBy { it.timestamp }

                    // Display first story
                    displayCurrentStory()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ViewStoryActivity, "Error loading stories: ${error.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun displayCurrentStory() {
        if (currentStoryIndex >= storyItems.size) {
            finish()
            return
        }

        try {
            val currentStory = storyItems[currentStoryIndex]

            // Display story image
            if (currentStory.storyImageBase64.isNotEmpty()) {
                ImageUtils.loadBase64Image(storyImageView, currentStory.storyImageBase64)
            } else {
                Toast.makeText(this, "Story image not available", Toast.LENGTH_SHORT).show()
                moveToNextStory()
                return
            }

            // Start story timer (5 seconds per story)
            startStoryTimer()
        } catch (e: Exception) {
            Toast.makeText(this, "Error displaying story: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startStoryTimer() {
        storyTimer?.cancel()
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

    private fun showDeleteConfirmationDialog() {
        if (!isOwnStory || currentStoryIndex >= storyItems.size) return

        val currentStory = storyItems[currentStoryIndex]

        AlertDialog.Builder(this)
            .setTitle("Delete Story")
            .setMessage("Are you sure you want to delete this story?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCurrentStory(currentStory.storyId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCurrentStory(storyId: String) {
        if (userId == null) return

        // Delete from Firebase Realtime Database
        database.reference.child("stories").child(userId!!).child("storyItems").child(storyId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Story deleted successfully", Toast.LENGTH_SHORT).show()

                // Remove from local list
                storyItems.removeAt(currentStoryIndex)

                // If no more stories, finish
                if (storyItems.isEmpty()) {
                    // Delete entire user story node
                    database.reference.child("stories").child(userId!!).removeValue()
                    finish()
                } else {
                    // Adjust index if needed
                    if (currentStoryIndex >= storyItems.size) {
                        currentStoryIndex = storyItems.size - 1
                    }
                    // Display next/current story
                    displayCurrentStory()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete story: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        storyTimer?.cancel()
    }

    override fun onPause() {
        super.onPause()
        storyTimer?.cancel()
    }
}
