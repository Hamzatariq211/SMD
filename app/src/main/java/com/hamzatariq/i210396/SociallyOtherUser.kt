package com.hamzatariq.i210396

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SociallyOtherUser : AppCompatActivity() {

    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_other_user)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        val btnFollow = findViewById<Button>(R.id.btnFollow)

        // Initial style (Follow/brown)
        applyFollowStyle(btnFollow, isFollowing)

        btnFollow.setOnClickListener {
            isFollowing = !isFollowing
            applyFollowStyle(btnFollow, isFollowing)
            // TODO: Add follow/unfollow API logic here if needed
        }

        // ✅ Bottom navigation
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
    }

    private fun applyFollowStyle(button: Button, following: Boolean) {
        if (following) {
            button.text = "Following"
            button.background = ContextCompat.getDrawable(this, R.drawable.bg_follow_white)
            button.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        } else {
            button.text = "Follow"
            button.background = ContextCompat.getDrawable(this, R.drawable.bg_follow_brown)
            button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        }
    }
}
