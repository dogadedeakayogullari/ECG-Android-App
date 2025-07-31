package com.example.bluetoothhc06app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val prefs = getSharedPreferences("UserInfo", MODE_PRIVATE)
        val name = prefs.getString("name", "Bilinmiyor")

        val nameTextView: TextView = findViewById(R.id.nameTextView)
        val logoutButton: Button = findViewById(R.id.logoutButton)

        nameTextView.text = "Hoş geldin, $name"

        logoutButton.setOnClickListener {
            prefs.edit().clear().apply()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
