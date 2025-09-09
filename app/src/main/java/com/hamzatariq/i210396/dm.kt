package com.hamzatariq.i210396

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class dm : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dm)

        // Handle system bars for LinearLayout root
        val mainLayout = findViewById<LinearLayout>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back button
        val backIcon = findViewById<ImageView>(R.id.backIcon)
        backIcon.setOnClickListener {
            finish()
        }

        // Open chat when clicking on the row
        val openChatRow = findViewById<RelativeLayout>(R.id.openchat)
        openChatRow.setOnClickListener {
            val intent = Intent(this, chat::class.java) // Replace Chat::class.java with your chat activity
            startActivity(intent)
        }
    }
}
