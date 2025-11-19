package com.devs.i210396_i211384

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.lifecycle.lifecycleScope
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.network.UpdateProfileRequest
import com.devs.i210396_i211384.utils.ImageUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class EditProfile : AppCompatActivity() {
    private val apiService = ApiService.create()

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

        // Cancel button
        val btnCancel = findViewById<TextView>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            finish()
        }

        // Done button - Save profile
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
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getCurrentUser()
                }

                if (response.isSuccessful) {
                    val user = response.body()!!

                    val firstName = user.firstName ?: ""
                    val lastName = user.lastName ?: ""
                    val fullName = "$firstName $lastName".trim()

                    etName.setText(fullName)
                    etUsername.setText(user.username)
                    etBio.setText(user.bio ?: "")
                    etWebsite.setText(user.website ?: "")
                    etEmail.setText(user.email)
                    etPhone.setText(user.phone ?: "")
                    etGender.setText(user.gender ?: "")
                    switchPrivateAccount.isChecked = user.isPrivate

                    currentProfileImageBase64 = user.profileImageUrl ?: ""

                    // Load profile image if available
                    if (currentProfileImageBase64.isNotEmpty()) {
                        try {
                            val bitmap = ImageUtils.base64ToBitmap(currentProfileImageBase64)
                            profilePhoto.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            // Keep default image if conversion fails
                        }
                    }
                } else {
                    Toast.makeText(this@EditProfile, "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditProfile,
                    "Error loading profile: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun saveProfile() {
        val fullName = etName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val bio = etBio.text.toString().trim()
        val website = etWebsite.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val gender = etGender.text.toString().trim()
        val isPrivate = switchPrivateAccount.isChecked

        // Validation
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

        // Show progress
        Toast.makeText(this, "Updating profile...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // Get profile image Base64
                val profileImageBase64 = if (selectedImageUri != null) {
                    withContext(Dispatchers.IO) {
                        ImageUtils.convertImageToBase64(this@EditProfile, selectedImageUri!!)
                    }
                } else {
                    currentProfileImageBase64
                }

                // Update profile via MySQL API
                val updateRequest = UpdateProfileRequest(
                    firstName = firstName,
                    lastName = lastName,
                    username = username,
                    bio = bio,
                    website = website,
                    email = email,
                    phone = phone,
                    gender = gender,
                    profileImageUrl = profileImageBase64,
                    isPrivate = isPrivate
                )

                val response = withContext(Dispatchers.IO) {
                    apiService.updateProfile(updateRequest)
                }

                if (response.isSuccessful) {
                    // Update session to mark profile as setup
                    SessionManager.setProfileSetup(true)

                    Toast.makeText(this@EditProfile, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

                    // Navigate to HomePage after successful profile setup
                    val intent = Intent(this@EditProfile, HomePage::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val error = Gson().fromJson(errorBody, com.devs.i210396_i211384.network.ErrorResponse::class.java)
                        error.error
                    } catch (e: Exception) {
                        "Failed to update profile"
                    }
                    Toast.makeText(this@EditProfile, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditProfile,
                    "Network error: ${e.message}. Make sure XAMPP is running.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

