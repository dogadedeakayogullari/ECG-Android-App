package com.example.bluetoothhc06app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val nameInput: EditText = findViewById(R.id.nameInput)
        val loginButton: Button = findViewById(R.id.loginButton)

        loginButton.setOnClickListener {
            val name = nameInput.text.toString()

            if (name.isNotEmpty()) {
                val prefs = getSharedPreferences("UserInfo", MODE_PRIVATE)
                prefs.edit().putString("name", name).apply()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}
