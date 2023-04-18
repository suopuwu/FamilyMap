package com.example.familymap.ui

import Model.Event
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.familymap.R
import com.example.familymap.data.Cache
import com.example.familymap.data.SettingsInfo
import com.example.familymap.databinding.ActivitySettingsBinding
import com.example.familymap.databinding.FragmentMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MapsFragment : Fragment(), MenuProvider {
    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!
    private var map: GoogleMap? = null
    private var polyLines = ArrayList<Polyline>()
    private val MAX_LINE_WIDTH = 35F
    private val MIN_LINE_WIDTH = 5F
    private val LINE_DECAY_SIZE = 10F
    private val ZOOM_FACTOR = 5F
    private var currentlyEnabledEvents = HashSet<String>()
    private var selectedEvent: Event? = null

    //If this fragment is made in an event activity, it should have this. Otherwise, it will be null
    private var preSelectedEventID: String? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        preSelectedEventID = activity?.intent?.getStringExtra("eventID")
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this)
        _binding = FragmentMapsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapView = view.findViewById<MapView>(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(callback)
        mapView.onStart()
        binding.mapDetails.setOnClickListener { onDetailsClicked() }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (preSelectedEventID == null)
            menuInflater.inflate(R.menu.menu_main_map, menu)
        else {
            Cache.getEvent(preSelectedEventID!!)?.let {
                selectEvent(it)
                moveCameraTo(it.latitude, it.longitude)
            }
        }

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.settingsIcon -> {
                val intent = Intent(context, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.searchIcon -> {
                val intent = Intent(context, SearchActivity::class.java)
                startActivity(intent)
                true
            }
            //if, somehow another item is clicked, return false as it is not handled by this function
            else -> false
        }
    }

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
        //show info when a marker is clicked
        googleMap.setOnMarkerClickListener { marker ->
            selectEvent(marker.tag as Event)
            true
        }

    }

    private fun onDetailsClicked() {
        if (selectedEvent == null) {
            Toast.makeText(context, "Please select an event first", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(context, PersonActivity::class.java)
        //ensure that pressing the back button closes the app after this
        intent.putExtra("personID", selectedEvent!!.personID)
        startActivity(intent)
    }

    private fun moveCameraTo(lat: Float, long: Float) {
        map?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(lat.toDouble(), long.toDouble()),
                ZOOM_FACTOR
            )
        )
    }

    private fun selectEvent(event: Event) {
        fillDetails(event)
        drawMapLines(event)
        selectedEvent = event

    }

    private fun fillDetails(event: Event) {
        val eventOwner = Cache.getPerson(event.personID) ?: return


        //change icon to match gender
        val drawable =
            if (eventOwner.gender == "f") R.drawable.female_icon else R.drawable.male_icon
        binding.mapDetails.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0)

        //set text
        binding.mapDetails.text =
            "${eventOwner.firstName} ${eventOwner.lastName} \n${event.eventType.uppercase()} in ${event.city}, ${event.country} (${event.year})"
    }

    private fun drawMapLines(selectedEvent: Event) {
        fun clearPolyLines() {
            for (line in polyLines) {
                line.remove()
            }
            polyLines = ArrayList<Polyline>()
        }

        fun lineFromTo(
            firstEvent: Event?,
            secondEvent: Event?,
            colorKey: String,
            thickness: Float = 10F
        ) {
            //if either first event or second event are null, if either event is filtered out, or if the google map doesn't exist, don't draw the line.
            if (firstEvent == null || secondEvent == null ||
                !currentlyEnabledEvents.contains(firstEvent.eventID) ||
                !currentlyEnabledEvents.contains(secondEvent.eventID) ||
                map == null
            ) {
                return
            }

            //draw line
            polyLines.add(
                map!!.addPolyline(
                    PolylineOptions().add(
                        LatLng(
                            firstEvent.latitude.toDouble(),
                            firstEvent.longitude.toDouble()
                        ),
                        LatLng(
                            secondEvent.latitude.toDouble(),
                            secondEvent.longitude.toDouble()
                        )
                    ).color(Cache.getColorFor(colorKey)).width(thickness)
                )
            )
        }

        fun drawSpouseLine() {
            val spouseID = Cache.getPerson(selectedEvent.personID)!!.spouseID
            val spouseBirthEvent = Cache.getEarliestEvent(spouseID)
            lineFromTo(selectedEvent, spouseBirthEvent, "Spouse Line")
        }

        fun drawFamilyTreeLine(rootEventID: String, depth: Int) {
            val rootEvent = Cache.getEvent(rootEventID)!!
            val rootPerson = Cache.getPerson(rootEvent.personID)!!

            fun drawLineTo(secondPersonID: String) {
                val secondEvent = Cache.getEarliestEvent(secondPersonID)!!
                val potentialWidth = MAX_LINE_WIDTH - (LINE_DECAY_SIZE * depth)
                val width =
                    if (potentialWidth > MIN_LINE_WIDTH) potentialWidth else MIN_LINE_WIDTH

                lineFromTo(rootEvent, secondEvent, "Family Tree Line", width)
                drawFamilyTreeLine(secondEvent.eventID, depth + 1)
            }

            if (rootPerson.fatherID != null) {
                drawLineTo(rootPerson.fatherID)
            }
            if (rootPerson.motherID != null) {
                drawLineTo(rootPerson.motherID)
            }
        }

        fun drawLifeStoryLines() {
            val eventOwner = Cache.getPerson(selectedEvent.personID)!!

            var priorEvent: Event? = null
            for (event in Cache.getEventsForPerson(eventOwner.personID)!!) {
                if (priorEvent != null) {
                    lineFromTo(priorEvent, event, "Life Story Line")
                }
                priorEvent = event
            }
        }
        //clear prior lines, then draw new ones depending on enabled settings
        clearPolyLines()
        if (SettingsInfo.getSetting(SettingsInfo.Option.SPOUSE_LINES))
            drawSpouseLine()
        if (SettingsInfo.getSetting(SettingsInfo.Option.FAMILY_TREE_LINES))
            drawFamilyTreeLine(selectedEvent.eventID, 0)
        if (SettingsInfo.getSetting(SettingsInfo.Option.LIFE_STORY_LINES))
            drawLifeStoryLines()
    }

    private fun addEventsToMap() {
        //start background task to get all events that filters include, then add them to the map
        CoroutineScope(Dispatchers.Default).launch {
            val events = Cache.getFilteredEventList()
            currentlyEnabledEvents.clear()

            for (event in events) {
                currentlyEnabledEvents.add(event.eventID)
                val location = LatLng(event.latitude.toDouble(), event.longitude.toDouble())
                CoroutineScope(Dispatchers.Main).launch {
                    val marker = map?.addMarker(
                        MarkerOptions().position(location)
                            .title(
                                "${Cache.getPerson(event.personID)?.firstName}'s ${event.eventType}"
                            )
                            .icon(BitmapDescriptorFactory.defaultMarker(Cache.getHueFor(event.eventType)))
                    )
                    marker?.tag = event
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map?.clear()
        addEventsToMap()
        if (selectedEvent != null) {
            selectEvent(selectedEvent!!)
        }
    }
}