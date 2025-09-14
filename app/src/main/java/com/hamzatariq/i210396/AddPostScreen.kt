package com.hamzatariq.i210396

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AddPostScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_post)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ✅ Cancel button
        val btnCancel = findViewById<TextView>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            finish() // close activity
        }

        // ✅ Next button
        val btnNext = findViewById<TextView>(R.id.btnNext)
        btnNext.setOnClickListener {
            finish() // also close activity (can add navigation later)
        }
    }
}
