package com.hamzatariq.i210396

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SwitchAccounts : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_switch_accounts)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the login button by its ID
        val btnLogin = findViewById<Button>(R.id.btnLogin) // Replace with your actual button ID

        // Find the back button ImageView by its ID
        val ivBack = findViewById<ImageView>(R.id.ivBack) // Replace with your actual ImageView ID

        // Find the sign up link TextView by its ID
        val tvSignUpLink = findViewById<TextView>(R.id.tvSignUpLink)

        // Set click listener for the login button
        btnLogin.setOnClickListener {
            // Create intent to navigate to HomePage
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)

            // Optional: finish current activity if you don't want user to come back
            // finish()
        }

        // Set click listener for the back button
        ivBack.setOnClickListener {
            // Create intent to navigate to LoginScreen
            val intent = Intent(this, loginscreen::class.java)
            startActivity(intent)

            // Optional: finish current activity
            finish()
        }

        // Set click listener for the sign up link
        tvSignUpLink.setOnClickListener {
            // Create intent to navigate to RegisterScreen
            val intent = Intent(this, RegisterScreen::class.java)
            startActivity(intent)

            // Optional: finish current activity if you don't want user to come back
            // finish()
        }
    }
}