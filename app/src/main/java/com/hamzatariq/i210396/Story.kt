package com.hamzatariq.i210396

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.hamzatariq.i210396.models.StoryModel
import com.hamzatariq.i210396.utils.ImageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Story : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: FirebaseDatabase
    private lateinit var storyImageView: ImageView
    private var imageUri: Uri? = null
    private var currentPhotoPath: String? = null

    // Activity result launchers
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize views
        storyImageView = findViewById(R.id.storyImage)

        // Initialize launchers
        initializeLaunchers()

        // Camera button
        findViewById<LinearLayout>(R.id.btnCamera).setOnClickListener {
            checkPermissionsAndOpenCamera()
        }

        // Gallery button
        findViewById<LinearLayout>(R.id.btnGallery).setOnClickListener {
            checkPermissionsAndOpenGallery()
        }

        // Upload to story button
        findViewById<LinearLayout>(R.id.btnYourStory).setOnClickListener {
            if (imageUri != null) {
                uploadStoryToFirebase()
            } else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        // Close button
        findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            finish()
        }
    }

    private fun initializeLaunchers() {
        // Gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    imageUri = uri
                    storyImageView.setImageURI(uri)
                }
            }
        }

        // Camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoPath?.let { path ->
                    val file = File(path)
                    imageUri = Uri.fromFile(file)
                    storyImageView.setImageURI(imageUri)
                }
            }
        }

        // Permission launcher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false

            // Check for storage permission based on Android version
            val storageGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissions[android.Manifest.permission.READ_MEDIA_IMAGES] ?: false
            } else {
                permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            }

            if (cameraGranted || storageGranted) {
                // At least one permission granted, proceed
                if (cameraGranted) {
                    openCamera()
                } else if (storageGranted) {
                    openGallery()
                }
            } else {
                Toast.makeText(this, "Camera or gallery permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionsAndOpenCamera() {
        val cameraPermission = android.Manifest.permission.CAMERA

        if (ContextCompat.checkSelfPermission(this, cameraPermission) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            // Request only camera permission
            permissionLauncher.launch(arrayOf(cameraPermission))
        }
    }

    private fun checkPermissionsAndOpenGallery() {
        // Check permission based on Android version
        val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, storagePermission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            // Request storage permission
            permissionLauncher.launch(arrayOf(storagePermission))
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Add Story")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoFile?.let { file ->
            val photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            cameraLauncher.launch(photoUri)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(null)
            File.createTempFile("STORY_${timeStamp}_", ".jpg", storageDir).apply {
                currentPhotoPath = absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
                    val storyImageBase64 = ImageUtils.convertImageToBase64(this, imageUri!!)

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
                            Toast.makeText(this, "Failed to upload story: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
