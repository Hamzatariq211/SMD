package com.hamzatariq.i210396

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomePage : AppCompatActivity() {
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

        // Explore
        val exploreIcon = findViewById<ImageView>(R.id.exlplore)
        exploreIcon.setOnClickListener {
            val intent = Intent(this, Explore::class.java)
            startActivity(intent)
        }

        // Share (DM)
        val shareIcon = findViewById<ImageView>(R.id.share)
        shareIcon.setOnClickListener {
            val intent = Intent(this, dm::class.java)
            startActivity(intent)
        }

        // Like
        val likeIcon = findViewById<ImageView>(R.id.like)
        likeIcon.setOnClickListener {
            val intent = Intent(this, likeFollowing::class.java)
            startActivity(intent)
        }

        // Profile
        val profileIcon = findViewById<ImageView>(R.id.profile)
        profileIcon.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            startActivity(intent)
        }

        // Post
        val postIcon = findViewById<ImageView>(R.id.post)
        postIcon.setOnClickListener {
            val intent = Intent(this, AddPost::class.java)
            startActivity(intent)
        }

        // ✅ Camera → Story
        val cameraIcon = findViewById<ImageView>(R.id.camera)
        cameraIcon.setOnClickListener {
            val intent = Intent(this, Story::class.java)
            startActivity(intent)
        }

        // ✅ Story Image → Story
        val storyImageIcon = findViewById<ImageView>(R.id.story_image)
        storyImageIcon.setOnClickListener {
            val intent = Intent(this, Story::class.java)
            startActivity(intent)
        }
    }
}
