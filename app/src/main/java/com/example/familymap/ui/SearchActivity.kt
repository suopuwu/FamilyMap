package com.example.familymap.ui

import Model.Person
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.familymap.R
import com.example.familymap.data.Cache
import com.example.familymap.databinding.ActivitySearchBinding
import com.example.familymap.utils.SuopConstants

class SearchActivity : AppCompatActivity() {
    private var _binding: ActivitySearchBinding? = null
    private val binding get() = _binding!!
    private val MAX_CLICK_DURATION = 150
    private var currentDataOrder = ArrayList<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //setup up button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Search"

        //make search text box expanded by default
        binding.searchView.isIconified = false

        //call the search function on submission and text change, unless text is null
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(text: String?): Boolean {
                text?.let { search(it) }
                return true
            }

            override fun onQueryTextSubmit(text: String?): Boolean {
                text?.let { search(it) }
                return true
            }
        })

        //make clicking on search results functional
        binding.searchResults.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                //Handle up events, but only when the press lasted less time than the max duration to prevent activation on swipes
                if (e.action == MotionEvent.ACTION_UP && e.eventTime - e.downTime < MAX_CLICK_DURATION) {
                    //get button pressed
                    val child = binding.searchResults.findChildViewUnder(e.x, e.y)
                    if (child != null) {
                        //navigate to the item in question
                        val position = binding.searchResults.getChildAdapterPosition(child)
                        val value = currentDataOrder[position].split(SuopConstants.STRING_SEPARATOR)
                        if (value.size != 2)
                            return false
                        navigateToDestination(value[0], value[1])
                    }
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
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

    fun search(query: String) {
        //clear existing data and cancel the search if it's empty
        currentDataOrder.clear()
        if (query.isEmpty())
            return


        val searchResults = ArrayList<String>()
        //Get all relevant people, then make a string holding their data, then add them to the results
        Cache.searchPeople(query).forEach { person ->
            val type = SuopConstants.STRING_SEPARATOR +
                    (if (person.gender == "f") R.drawable.female_icon else R.drawable.male_icon) +
                    SuopConstants.STRING_SEPARATOR +
                    "Person"
            currentDataOrder.add(person.personID + SuopConstants.STRING_SEPARATOR + "person")
            searchResults.add("${person.firstName} ${person.lastName}$type")
        }

        //Get all relevant events, then make a string holding their data, then add them to the results
        Cache.searchEvents(query).forEach { event ->
            val person = Cache.getPerson(event.personID) ?: Person()
            val type = SuopConstants.STRING_SEPARATOR +
                    R.drawable.location_icon +
                    SuopConstants.STRING_SEPARATOR +
                    "Event"

            currentDataOrder.add(event.eventID + SuopConstants.STRING_SEPARATOR + "event")
            searchResults.add("${person.firstName} ${person.lastName}'s ${event.eventType}\n${event.city}, ${event.country} (${event.year})$type")
        }

        //update the ui
        binding.searchResults.run {
            adapter = SearchResultAdapter(searchResults)
            layoutManager = LinearLayoutManager(context)
        }
    }

    fun navigateToDestination(id: String, type: String) {
        //Ignore bad requests, if they happen somehow
        if (type != "person" && type != "event")
            return

        //navigate to the new destination
        val intent = Intent(
            this,
            if (type == "person") PersonActivity::class.java else EventActivity::class.java
        )
        intent.putExtra("eventID", id)
        intent.putExtra("personID", id)
        startActivity(intent)
    }
}