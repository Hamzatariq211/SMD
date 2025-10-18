package com.hamzatariq.i210396

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.hamzatariq.i210396.models.Post
import com.hamzatariq.i210396.utils.ImageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddPostScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: FirebaseDatabase

    private lateinit var postImagePreview: ImageView
    private lateinit var tvSelectImage: TextView
    private lateinit var etCaption: EditText
    private lateinit var btnPost: TextView

    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null

    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        // Apply window insets to handle system bars properly
        val mainLayout = findViewById<RelativeLayout>(R.id.main)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize views
        postImagePreview = findViewById(R.id.postImagePreview)
        tvSelectImage = findViewById(R.id.tvSelectImage)
        etCaption = findViewById(R.id.etCaption)
        btnPost = findViewById(R.id.btnPost)

        // Initialize launchers
        initializeLaunchers()

        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Camera button
        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            checkPermissionsAndOpenCamera()
        }

        // Gallery button
        findViewById<Button>(R.id.btnGallery).setOnClickListener {
            checkPermissionsAndOpenGallery()
        }

        // Image preview click
        postImagePreview.setOnClickListener {
            checkPermissionsAndOpenGallery()
        }

        // Post button
        btnPost.setOnClickListener {
            uploadPost()
        }
    }

    private fun initializeLaunchers() {
        // Gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    postImagePreview.setImageURI(uri)
                    tvSelectImage.visibility = View.GONE
                }
            }
        }

        // Camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoPath?.let { path ->
                    val file = File(path)
                    selectedImageUri = Uri.fromFile(file)
                    postImagePreview.setImageURI(selectedImageUri)
                    tvSelectImage.visibility = View.GONE
                }
            }
        }

        // Permission launcher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
            val storageGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissions[android.Manifest.permission.READ_MEDIA_IMAGES] ?: false
            } else {
                permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            }

            if (cameraGranted || storageGranted) {
                if (cameraGranted) {
                    openCamera()
                } else if (storageGranted) {
                    openGallery()
                }
            } else {
                Toast.makeText(this, "Permission required to select image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionsAndOpenCamera() {
        val cameraPermission = android.Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, cameraPermission) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            permissionLauncher.launch(arrayOf(cameraPermission))
        }
    }

    private fun checkPermissionsAndOpenGallery() {
        val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, storagePermission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            permissionLauncher.launch(arrayOf(storagePermission))
        }
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
            File.createTempFile("POST_${timeStamp}_", ".jpg", storageDir).apply {
                currentPhotoPath = absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun uploadPost() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val caption = etCaption.text.toString().trim()

        Toast.makeText(this, "Uploading post...", Toast.LENGTH_SHORT).show()

        // Get user data from Firestore
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: "User"
                    val profileImageBase64 = document.getString("profileImageUrl") ?: ""

                    // Convert post image to Base64
                    val postImageBase64 = ImageUtils.convertImageToBase64(this, selectedImageUri!!)

                    if (postImageBase64.isEmpty()) {
                        Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Create post model
                    val postId = UUID.randomUUID().toString()
                    val post = Post(
                        postId = postId,
                        userId = currentUser.uid,
                        username = username,
                        userProfileImage = profileImageBase64,
                        postImageBase64 = postImageBase64,
                        caption = caption,
                        timestamp = System.currentTimeMillis()
                    )

                    // Save to Firebase Realtime Database
                    database.reference.child("posts").child(postId)
                        .setValue(post)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Post uploaded successfully!", Toast.LENGTH_SHORT).show()

                            // Navigate to profile screen
                            val intent = Intent(this, profileScreen::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to upload post: ${e.message}", Toast.LENGTH_SHORT).show()
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
