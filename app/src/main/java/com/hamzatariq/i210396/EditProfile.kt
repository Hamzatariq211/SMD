package com.hamzatariq.i210396

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EditProfile : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ✅ Cancel button
        val btnCancel = findViewById<TextView>(R.id.btnCancel)
        btnCancel.setOnClickListener {
            finish()  // closes EditProfile and goes back
        }

        // ✅ Done button
        val btnDone = findViewById<TextView>(R.id.btnDone)
        btnDone.setOnClickListener {
            // You can also save changes here if needed
            finish()  // just closes the activity
        }
    }
}
