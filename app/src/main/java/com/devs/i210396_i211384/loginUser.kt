package com.devs.i210396_i211384

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.LoginRequest
import com.devs.i210396_i211384.network.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class loginUser : AppCompatActivity() {
    private val apiService = ApiService.create()

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var tvSignUp: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_loginscreen_new)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        etEmail = findViewById(R.id.etLoginEmail)
        etPassword = findViewById(R.id.etLoginPassword)
        loginButton = findViewById(R.id.btnLogin)
        tvSignUp = findViewById(R.id.tvSignUp)
        progressBar = findViewById(R.id.progressBar)

        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // Sign Up text navigation
        tvSignUp.setOnClickListener {
            val intent = Intent(this, RegisterUser::class.java)
            startActivity(intent)
        }

        // Login button
        loginButton.setOnClickListener {
            loginUser()
        }

        // Forgot Password
        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Password reset feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validation
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            etPassword.requestFocus()
            return
        }

        // Show progress
        progressBar.visibility = View.VISIBLE
        loginButton.isEnabled = false

        // Call API
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.login(LoginRequest(email, password))
                }

                progressBar.visibility = View.GONE
                loginButton.isEnabled = true

                if (response.isSuccessful) {
                    val authResponse = response.body()!!

                    // Save session
                    SessionManager.saveSession(
                        authResponse.token,
                        authResponse.userId,
                        authResponse.isProfileSetup
                    )

                    // Debug: Verify session was saved
                    android.util.Log.d("LoginUser", "Session saved - Token: ${authResponse.token}")
                    android.util.Log.d("LoginUser", "Session saved - UserId: ${authResponse.userId}")
                    android.util.Log.d("LoginUser", "Session saved - IsProfileSetup: ${authResponse.isProfileSetup}")

                    // Verify we can retrieve it
                    android.util.Log.d("LoginUser", "Retrieved Token: ${SessionManager.getToken()}")
                    android.util.Log.d("LoginUser", "Retrieved UserId: ${SessionManager.getUserId()}")
                    android.util.Log.d("LoginUser", "Is Logged In: ${SessionManager.isLoggedIn()}")

                    Toast.makeText(this@loginUser, "Login successful!", Toast.LENGTH_SHORT).show()

                    // Navigate based on profile setup status
                    if (authResponse.isProfileSetup) {
                        val intent = Intent(this@loginUser, HomePage::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        val intent = Intent(this@loginUser, EditProfile::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val error = Gson().fromJson(errorBody, com.devs.i210396_i211384.network.ErrorResponse::class.java)
                        error.error
                    } catch (e: Exception) {
                        "Login failed. Please try again."
                    }
                    Toast.makeText(this@loginUser, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                loginButton.isEnabled = true
                Toast.makeText(
                    this@loginUser,
                    "Network error: ${e.message}. Make sure XAMPP is running.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
