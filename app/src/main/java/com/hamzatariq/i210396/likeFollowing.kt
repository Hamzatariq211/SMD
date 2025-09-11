package com.hamzatariq.i210396

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class likeFollowing : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_like_following)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Home
        findViewById<View>(R.id.home).setOnClickListener {
            startActivity(Intent(this, HomePage::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Explore
        findViewById<View>(R.id.explore).setOnClickListener {
            startActivity(Intent(this, Explore::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Like like
        findViewById<View>(R.id.like12).setOnClickListener {
            startActivity(Intent(this, likelike::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // ✅ Profile
        findViewById<View>(R.id.profile).setOnClickListener {
            startActivity(Intent(this, profile::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // ✅ Post
        findViewById<View>(R.id.post).setOnClickListener {
            startActivity(Intent(this, AddPost::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
