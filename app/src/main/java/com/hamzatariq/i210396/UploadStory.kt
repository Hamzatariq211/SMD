package com.hamzatariq.i210396

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.hamzatariq.i210396.models.StoryModel
import java.io.ByteArrayOutputStream
import java.util.*

class UploadStory : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: FirebaseDatabase
    private lateinit var storyImageView: ImageView
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_upload_story)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance()

        // Get views
        storyImageView = findViewById(R.id.storyImage)

        // Get image URI from intent
        val imageUriString = intent.getStringExtra("imageUri")
        if (imageUriString != null) {
            imageUri = Uri.parse(imageUriString)
            storyImageView.setImageURI(imageUri)
        }

        // ✅ "Your Story" button → Upload to Firebase and move to StoryUploaded
        val yourStoryBtn = findViewById<LinearLayout>(R.id.btnYourStory)
        yourStoryBtn.setOnClickListener {
            uploadStoryToFirebase()
        }

        // Close button
        val closeBtn = findViewById<ImageView>(R.id.btnClose)
        closeBtn.setOnClickListener {
            finish()
        }
    }

    private fun uploadStoryToFirebase() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Uploading story...", Toast.LENGTH_SHORT).show()

        // Get user data from Firestore
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: "User"
                    val profileImageBase64 = document.getString("profileImageUrl") ?: ""

                    // Convert story image to Base64
                    val storyImageBase64 = convertImageToBase64(imageUri!!)

                    if (storyImageBase64.isEmpty()) {
                        Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Create story model
                    val storyId = UUID.randomUUID().toString()
                    val currentTime = System.currentTimeMillis()
                    val expiryTime = currentTime + (24 * 60 * 60 * 1000) // 24 hours

                    val story = StoryModel(
                        storyId = storyId,
                        userId = currentUser.uid,
                        username = username,
                        userProfileImage = profileImageBase64,
                        storyImageBase64 = storyImageBase64,
                        timestamp = currentTime,
                        expiryTime = expiryTime
                    )

                    // Save to Firebase Realtime Database under user's ID
                    // Structure: stories/{userId}/storyItems/{storyId}
                    val userStoriesRef = database.reference.child("stories").child(currentUser.uid)

                    // Save user info first
                    val userInfo = mapOf(
                        "userId" to currentUser.uid,
                        "username" to username,
                        "userProfileImage" to profileImageBase64,
                        "lastUpdated" to currentTime
                    )

                    userStoriesRef.updateChildren(userInfo)

                    // Save the story item
                    userStoriesRef.child("storyItems").child(storyId)
                        .setValue(story)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Story uploaded successfully!", Toast.LENGTH_SHORT).show()

                            // Navigate to HomePage
                            val intent = Intent(this, HomePage::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to upload story: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun convertImageToBase64(imageUri: Uri): String {
        return try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Resize bitmap to reduce size (max 1024x1024 for stories)
            val resizedBitmap = resizeBitmap(bitmap, 1024, 1024)

            // Convert to Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
