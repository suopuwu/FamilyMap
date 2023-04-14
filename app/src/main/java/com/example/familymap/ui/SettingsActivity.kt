package com.example.familymap.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Switch
import androidx.core.view.MenuHost
import com.example.familymap.data.SettingsInfo
import com.example.familymap.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private var _binding: ActivitySettingsBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        //create layout and bindings
        super.onCreate(savedInstanceState)
        _binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //setup up button
        val menuHost: MenuHost = this
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        //make logout button functional
        binding.logoutButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            //ensure that pressing the back button closes the app after this
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP;
            startActivity(intent)
        }

        //make settings toggles functional
        fun bindToggle(toggleElement: Switch, setting: SettingsInfo.Option) {
            toggleElement.isChecked = SettingsInfo.getSetting(setting)
            toggleElement.setOnClickListener {
                SettingsInfo.setSetting(setting, toggleElement.isChecked)
                val temp = SettingsInfo
            }
        }

        //bind all of them
        bindToggle(binding.lifeStoryLinesToggle, SettingsInfo.Option.LIFE_STORY_LINES)
        bindToggle(binding.familyTreeLinesToggle, SettingsInfo.Option.FAMILY_TREE_LINES)
        bindToggle(binding.spouseLinesToggle, SettingsInfo.Option.SPOUSE_LINES)
        bindToggle(binding.fathersSideToggle, SettingsInfo.Option.FILTER_FATHER)
        bindToggle(binding.mothersSideToggle, SettingsInfo.Option.FILTER_MOTHER)
        bindToggle(binding.maleToggle, SettingsInfo.Option.FILTER_MALE)
        bindToggle(binding.femaleToggle, SettingsInfo.Option.FILTER_FEMALE)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //handle up button presses by closing the current activity
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}