package com.example.familymap.ui

import Model.Person
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import com.example.familymap.R
import com.example.familymap.data.Cache
import com.example.familymap.databinding.ActivityPersonBinding
import com.example.familymap.utils.SuopConstants

class PersonActivity : AppCompatActivity() {
    private var _binding: ActivityPersonBinding? = null
    private val binding get() = _binding!!

    private lateinit var personID: String
    private var person: Person? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPersonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setup menu
        val menuHost: MenuHost = this
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //setup data, all instances of PersonActivity must have personID
        personID = intent.getStringExtra("personID")!!
        person = Cache.getPerson(personID)

        //if, somehow a person is opened that doesn't exist, gracefully fail
        if (person == null) {
            finish()
            Toast.makeText(applicationContext, "Error: invalid person ID", Toast.LENGTH_SHORT)
                .show()
        }

        //prepare data holder so on click events function
        val itemIDs = arrayOf(ArrayList<String>(), ArrayList<String>())

        //Add immediate family members
        val familyStrings = ArrayList<String>()
        Cache.getImmediateFamily(personID).forEach {
            val relation = Cache.determineRelationship(person!!, it)


            familyStrings.add(
                "${it.firstName} ${it.lastName}\n${relation}" +
                        "${SuopConstants.STRING_SEPARATOR}${if (it.gender == "f") R.drawable.female_icon else R.drawable.male_icon}"
            )
            itemIDs[1].add(it.personID)
        }

        //add life events
        val eventStrings = ArrayList<String>()
        if (Cache.getFilteredPeopleIDs().contains(personID)) {
            Cache.getEventsForPerson(personID)?.forEach {
                eventStrings.add(
                    "${it.eventType.uppercase()}: ${it.city}, ${it.country} (${it.year})" +
                            "${SuopConstants.STRING_SEPARATOR}${R.drawable.location_icon}"
                )
                itemIDs[0].add(it.eventID)
            }
        }


        //prepare data for the adapter
        val groups = listOf("Life Events", "Family")
        val items = hashMapOf<String, List<String>>(
            Pair(groups[0], eventStrings),
            Pair(groups[1], familyStrings)
        )
        val adapter = CustomExpandableListAdapter(this, groups, items)

        //render
        binding.personDetailsList.setAdapter(adapter)
        binding.personDetails.text = "First name: ${person?.firstName}\n" +
                "Last name: ${person?.lastName}\n" +
                "Gender: ${if (person?.gender == "f") "female" else "male"}"
        supportActionBar?.title = "Details for ${person?.firstName} ${person?.lastName}"

        //make clicks on the lists open new activities as needed
        binding.personDetailsList.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            if (groupPosition == 1) { //family members
                val intent = Intent(this, PersonActivity::class.java)
                intent.putExtra("personID", itemIDs[groupPosition][childPosition])
                startActivity(intent)
            } else if (groupPosition == 0) { //life events
                val intent = Intent(this, EventActivity::class.java)
                intent.putExtra("eventID", itemIDs[groupPosition][childPosition])
                startActivity(intent)
            }
            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //handle up button presses by closing the current activity
        when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent(this, MainActivity::class.java)

                //ensure the original main activity is used
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}