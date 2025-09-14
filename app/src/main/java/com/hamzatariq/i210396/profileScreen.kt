package com.hamzatariq.i210396

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class profileScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find views by ID
        val exploreBtn = findViewById<ImageView>(R.id.explore)
        val homeBtn = findViewById<ImageView>(R.id.home)
        val likeBtn = findViewById<ImageView>(R.id.like1)
        val editProfileBtn = findViewById<Button>(R.id.editProfileBtn)
        val postBtn = findViewById<ImageView>(R.id.post)

        // Navigate to Explore
        exploreBtn.setOnClickListener {
            val intent = Intent(this, Explore::class.java)
            startActivity(intent)
        }

        // Navigate to HomePage
        homeBtn.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        // Navigate to likeFollowing
        likeBtn.setOnClickListener {
            val intent = Intent(this, likeFollowing::class.java)
            startActivity(intent)
        }

        // ✅ Navigate to EditProfile
        editProfileBtn.setOnClickListener {
            val intent = Intent(this, EditProfile::class.java)
            startActivity(intent)
        }

        // ✅ Navigate to AddPost
        postBtn.setOnClickListener {
            val intent = Intent(this, AddPostScreen::class.java)
            startActivity(intent)
        }
    }
}
