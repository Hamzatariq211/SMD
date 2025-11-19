package com.devs.i210396_i211384

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.network.UpdateFCMTokenRequest
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    private val apiService = ApiService.create()

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check authentication status after 5 seconds (as per requirements)
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationStatus()
        }, 5000) // 5000ms = 5 seconds
    }

    private fun checkAuthenticationStatus() {
        if (SessionManager.isLoggedIn()) {
            // Get and update FCM token
            updateFCMToken()

            // User is logged in, verify session with server and get latest profile status
            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        apiService.getCurrentUser()
                    }

                    if (response.isSuccessful) {
                        val user = response.body()!!

                        // Update session with latest profile setup status
                        SessionManager.setProfileSetup(user.isProfileSetup)

                        // Navigate based on profile setup status
                        if (user.isProfileSetup) {
                            // Profile is setup, go to HomePage
                            startActivity(Intent(this@MainActivity, HomePage::class.java))
                        } else {
                            // Profile not setup, go to EditProfile
                            startActivity(Intent(this@MainActivity, EditProfile::class.java))
                        }
                    } else {
                        // Session invalid, logout and go to login
                        SessionManager.clearSession()
                        startActivity(Intent(this@MainActivity, loginUser::class.java))
                    }
                } catch (e: Exception) {
                    // Network error, use cached session data
                    if (SessionManager.isProfileSetup()) {
                        startActivity(Intent(this@MainActivity, HomePage::class.java))
                    } else {
                        startActivity(Intent(this@MainActivity, EditProfile::class.java))
                    }
                }
                finish()
            }
        } else {
            // User is not logged in, go to LoginScreen
            startActivity(Intent(this@MainActivity, loginUser::class.java))
            finish()
        }
    }

    private fun updateFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                // Send the FCM token to your server
                sendFCMTokenToServer(token)
            } else {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
            }
        }
    }

    private fun sendFCMTokenToServer(token: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.updateFCMToken(UpdateFCMTokenRequest(token))
                }

                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token updated successfully")
                } else {
                    Log.w(TAG, "Failed to update FCM token: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating FCM token", e)
            }
        }
    }
}
