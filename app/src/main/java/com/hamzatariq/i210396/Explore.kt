package com.hamzatariq.i210396

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Explore : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_explore)

        val mainLayout = findViewById<LinearLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Reference to search EditText
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        searchEditText.setOnClickListener {
            val intent = Intent(this, Explore2::class.java)
            startActivity(intent)
        }

        // Reference to home ImageView
        val homeIcon = findViewById<ImageView>(R.id.home)
        homeIcon.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        // Reference to like ImageView
        val likeIcon = findViewById<ImageView>(R.id.like)
        likeIcon.setOnClickListener {
            val intent = Intent(this, likeFollowing::class.java)
            startActivity(intent)
        }

        // ✅ Reference to profile ImageView
        val profileIcon = findViewById<ImageView>(R.id.profile)
        profileIcon.setOnClickListener {
            val intent = Intent(this, profile::class.java)
            startActivity(intent)
        }

        // ✅ Reference to post ImageView
        val postIcon = findViewById<ImageView>(R.id.post)
        postIcon.setOnClickListener {
            val intent = Intent(this, AddPost::class.java) // opens AddPost activity
            startActivity(intent)
        }
    }
}
