package com.example.visual

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnCalculator = findViewById<Button>(R.id.calcButton)
        val btnMediaplayer = findViewById<Button>(R.id.mediaButton)
        val btnLocation = findViewById<Button>(R.id.locationButton)
        val btnClient = findViewById<Button>(R.id.clientButton)


        btnCalculator.setOnClickListener {
            val intent = Intent(this, CalcActivity::class.java)
            startActivity(intent)
        }

        btnMediaplayer.setOnClickListener {
            val intent = Intent(this, MediaActivity::class.java)
            startActivity(intent)
        }

        btnLocation.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            startActivity(intent)
        }

        btnClient.setOnClickListener {
            val intent = Intent(this, ClientActivity::class.java)
            startActivity(intent)
        }

    }
}