package com.devs.i210396_i211384

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
import com.devs.i210396_i211384.network.SessionManager
import java.io.ByteArrayOutputStream
import java.util.*

class UploadStory : AppCompatActivity() {
    private lateinit var storyImageView: ImageView
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_upload_story)

        // Initialize SessionManager if not already initialized
        SessionManager.init(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get views
        storyImageView = findViewById(R.id.storyImage)

        // Get image URI from intent
        val imageUriString = intent.getStringExtra("imageUri")
        if (imageUriString != null) {
            imageUri = Uri.parse(imageUriString)
            storyImageView.setImageURI(imageUri)
        }

        // ✅ "Your Story" button → Upload story
        val yourStoryBtn = findViewById<LinearLayout>(R.id.btnYourStory)
        yourStoryBtn.setOnClickListener {
            uploadStory()
        }

        // Close button
        val closeBtn = findViewById<ImageView>(R.id.btnClose)
        closeBtn.setOnClickListener {
            finish()
        }
    }

    private fun uploadStory() {
        // Debug: Check login status
        val isLoggedIn = SessionManager.isLoggedIn()
        val userId = SessionManager.getUserId()
        val token = SessionManager.getToken()

        android.util.Log.d("UploadStory", "Is Logged In: $isLoggedIn")
        android.util.Log.d("UploadStory", "User ID: $userId")
        android.util.Log.d("UploadStory", "Token: $token")

        // Check if user is logged in via SessionManager
        if (!isLoggedIn || userId == null || token == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, loginUser::class.java)
            startActivity(intent)
            finish()
            return
        }

        if (imageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Story uploaded successfully! (Feature in development)", Toast.LENGTH_SHORT).show()

        // Navigate back to HomePage
        val intent = Intent(this, HomePage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
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
