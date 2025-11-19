package com.devs.i210396_i211384

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.devs.i210396_i211384.utils.ImageUtils

class likelikePage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_likelike)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Load user profile picture
        loadUserProfilePicture()

        // Home
        findViewById<View>(R.id.home)?.setOnClickListener {
            startActivity(Intent(this, HomePage::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Explore
        findViewById<View>(R.id.explore)?.setOnClickListener {
            startActivity(Intent(this, Explore::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Following
        findViewById<LinearLayout>(R.id.following)?.setOnClickListener {
            startActivity(Intent(this, likeFollowing::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Profile
        findViewById<View>(R.id.profile)?.setOnClickListener {
            startActivity(Intent(this, profileScreen::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Post
        findViewById<View>(R.id.post)?.setOnClickListener {
            startActivity(Intent(this, AddPostScreen::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun loadUserProfilePicture() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val profileImageBase64 = document.getString("profileImageUrl") ?: ""
                    val profileNavIcon = findViewById<ImageView>(R.id.profile)
                    ImageUtils.loadBase64Image(profileNavIcon, profileImageBase64)
                }
            }
    }
}

