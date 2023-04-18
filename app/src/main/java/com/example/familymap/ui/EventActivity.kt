package com.example.familymap.ui

import Model.Event
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.MenuHost
import com.example.familymap.R
import com.example.familymap.data.Cache
import com.example.familymap.databinding.ActivityEventBinding
import com.example.familymap.databinding.ActivitySearchBinding
import com.example.familymap.databinding.ActivitySettingsBinding

class EventActivity : AppCompatActivity() {
    private var _binding: ActivityEventBinding? = null
    private val binding get() = _binding!!

    private lateinit var eventID: String
    private var event: Event? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setup menu
        val menuHost: MenuHost = this
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Event Details"

        //setup data, all instances of EventActivity must have personID
        eventID = intent.getStringExtra("eventID")!!
        event = Cache.getEvent(eventID)

        //if, somehow a event is opened that doesn't exist, gracefully fail
        if (event == null) {
            finish()
            Toast.makeText(applicationContext, "Error: invalid event ID", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //handle up button presses by closing the current activity
        when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}