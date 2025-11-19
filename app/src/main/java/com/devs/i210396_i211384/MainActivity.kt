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

            // User is logged in, navigate to HomePage immediately.
            // We'll still verify the session with the server in background, but do not
            // block navigation to HomePage (avoids sending users to EditProfile from splash).
            startActivity(Intent(this@MainActivity, HomePage::class.java))
            finish()

            // Verify session with server and update latest profile status in background
            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        apiService.getCurrentUser()
                    }

                    if (response.isSuccessful) {
                        val user = response.body()!!

                        // Update session with latest profile setup status
                        SessionManager.setProfileSetup(user.isProfileSetup)

                        // If server invalidates the session (unlikely here), handle by clearing session and
                        // notifying the user from the HomePage flow instead of forcing a navigation from the splash.
                    } else {
                        // Session invalid on server, clear local session so next start goes to login
                        SessionManager.clearSession()
                        Log.w(TAG, "Session invalidated by server; user will need to login again.")
                    }
                } catch (e: Exception) {
                    // Network error: keep using cached session (already navigated to HomePage)
                    Log.w(TAG, "Network error while verifying session: ${e.message}")
                }
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
