package com.hamzatariq.i210396

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

class RegisterScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_screen)

        // Handle system insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 📌 Back button (ImageView)
        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener {
            finish() // closes RegisterScreen so it won’t stay in back stack
        }

        // 📌 DOB input field
        val etDob = findViewById<EditText>(R.id.etDob)

        etDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    etDob.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
                },
                year, month, day
            )

            // Prevent selecting future dates
            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }

        // 📌 Register button → Navigate to HomePage
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
            finish() // optional: close RegisterScreen so user can’t go back
        }
    }
}
