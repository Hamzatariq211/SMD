package com.devs.i210396_i211384

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Explore : AppCompatActivity() {
    private val apiService = ApiService.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_explore)

        // Initialize SessionManager
        SessionManager.init(this)

        val mainLayout = findViewById<LinearLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load user profile picture
        loadUserProfilePicture()

        // Reference to search EditText
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        searchEditText.setOnClickListener {
            val intent = Intent(this, ExploreSearch::class.java)
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

        // Reference to profile ImageView
        val profileIcon = findViewById<ImageView>(R.id.profile)
        profileIcon.setOnClickListener {
            val intent = Intent(this, profileScreen::class.java)
            startActivity(intent)
        }

        // Reference to post ImageView
        val postIcon = findViewById<ImageView>(R.id.post)
        postIcon.setOnClickListener {
            val intent = Intent(this, AddPostScreen::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserProfilePicture() {
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
                            val profileNavIcon = findViewById<ImageView>(R.id.profile)
                            ImageUtils.loadBase64Image(profileNavIcon, profileImageBase64)
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
}
