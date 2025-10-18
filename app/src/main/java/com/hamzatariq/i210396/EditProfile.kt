package com.hamzatariq.i210396

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hamzatariq.i210396.utils.ImageUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class EditProfile : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var etName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etBio: EditText
    private lateinit var etWebsite: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etGender: EditText
    private lateinit var profilePhoto: ImageView
    private lateinit var changePhoto: TextView
    private lateinit var switchPrivateAccount: androidx.appcompat.widget.SwitchCompat

    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var currentProfileImageBase64: String = ""

    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        etName = findViewById(R.id.etName)
        etUsername = findViewById(R.id.etUsername)
        etBio = findViewById(R.id.etBio)
        etWebsite = findViewById(R.id.etWebsite)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etGender = findViewById(R.id.etGender)
        profilePhoto = findViewById(R.id.profilePhoto)
        changePhoto = findViewById(R.id.changePhoto)
        switchPrivateAccount = findViewById(R.id.switchPrivateAccount)

        // Initialize launchers
        initializeLaunchers()

        // Load current user data
        loadUserProfile()

        // Change photo button
        changePhoto.setOnClickListener {
            showImagePickerDialog()
        }

        // Profile photo click
        profilePhoto.setOnClickListener {
            showImagePickerDialog()
        }

        // ✅ Cancel button
        val btnCancel = findViewById<TextView>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            finish()  // closes EditProfile and goes back
        }

        // ✅ Done button - Save profile
        val btnDone = findViewById<TextView>(R.id.btnDone)
        btnDone.setOnClickListener {
            saveProfile()
        }
    }

    private fun initializeLaunchers() {
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    profilePhoto.setImageURI(uri)
                }
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoPath?.let { path ->
                    val file = File(path)
                    selectedImageUri = Uri.fromFile(file)
                    profilePhoto.setImageURI(selectedImageUri)
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                showImagePickerDialog()
            } else {
                Toast.makeText(this, "Permissions required to select image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(this)
            .setTitle("Change Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpenCamera()
                    1 -> checkGalleryPermissionAndOpenGallery()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpenCamera() {
        val cameraPermission = android.Manifest.permission.CAMERA
        val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, cameraPermission) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(cameraPermission)
        }
        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(storagePermission)
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            openCamera()
        }
    }

    private fun checkGalleryPermissionAndOpenGallery() {
        val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(storagePermission))
        } else {
            openGallery()
        }
    }

    private fun openCamera() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            null
        }

        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                it
            )
            cameraLauncher.launch(photoURI)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir("Pictures") ?: filesDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val username = document.getString("username") ?: ""
                    val bio = document.getString("bio") ?: ""
                    val website = document.getString("website") ?: ""
                    val email = document.getString("email") ?: ""
                    val phone = document.getString("phone") ?: ""
                    val gender = document.getString("gender") ?: ""
                    currentProfileImageBase64 = document.getString("profileImageUrl") ?: ""

                    etName.setText("$firstName $lastName")
                    etUsername.setText(username)
                    etBio.setText(bio)
                    etWebsite.setText(website)
                    etEmail.setText(email)
                    etPhone.setText(phone)
                    etGender.setText(gender)

                    // Load profile picture
                    ImageUtils.loadBase64Image(profilePhoto, currentProfileImageBase64)

                    // Set private account switch
                    val isPrivate = document.getBoolean("isPrivate") ?: false
                    switchPrivateAccount.isChecked = isPrivate
                }
            }
    }

    private fun convertImageToBase64(imageUri: Uri): String {
        return try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Resize bitmap to reduce size (max 800x800)
            val resizedBitmap = resizeBitmap(bitmap, 800, 800)

            // Convert to Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to process image: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun saveProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val fullName = etName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val bio = etBio.text.toString().trim()
        val website = etWebsite.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val gender = etGender.text.toString().trim()
        val isPrivate = switchPrivateAccount.isChecked

        if (fullName.isEmpty()) {
            etName.error = "Name is required"
            etName.requestFocus()
            return
        }

        if (username.isEmpty()) {
            etUsername.error = "Username is required"
            etUsername.requestFocus()
            return
        }

        // Split full name into first and last name
        val nameParts = fullName.split(" ", limit = 2)
        val firstName = nameParts.getOrNull(0) ?: ""
        val lastName = nameParts.getOrNull(1) ?: ""

        // Get profile image Base64
        val profileImageBase64 = if (selectedImageUri != null) {
            convertImageToBase64(selectedImageUri!!)
        } else {
            currentProfileImageBase64
        }

        // Update profile in Firestore
        val updates = hashMapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName,
            "username" to username,
            "bio" to bio,
            "website" to website,
            "email" to email,
            "phone" to phone,
            "gender" to gender,
            "profileImageUrl" to profileImageBase64,
            "isProfileSetup" to true,
            "isPrivate" to isPrivate
        )

        firestore.collection("users").document(currentUser.uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e: Exception ->
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
