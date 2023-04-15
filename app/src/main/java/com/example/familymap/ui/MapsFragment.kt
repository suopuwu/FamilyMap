package com.example.familymap.ui

import Model.Event
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.familymap.R
import com.example.familymap.data.Cache
import com.example.familymap.data.SettingsInfo
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
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
    private lateinit var navController: NavController
    private var map: GoogleMap? = null
    private var polyLines = ArrayList<Polyline>()
    private val MAX_LINE_WIDTH = 35F
    private val MIN_LINE_WIDTH = 5F
    private val LINE_DECAY_SIZE = 10F
    private var currentlyEnabledEvents = HashSet<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        navController = this.findNavController()
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this)
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapView = view.findViewById<MapView>(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(callback)
        mapView.onStart()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_main_map, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.settingsIcon -> {
                navController.navigate(R.id.action_mapsFragment_to_settingsActivity)
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
            val clickedEvent = marker.tag as Model.Event

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
                //if either first event or second event are null, or if either event is filtered out, don't draw the line.
                if (firstEvent == null || secondEvent == null ||
                    !currentlyEnabledEvents.contains(firstEvent.eventID) ||
                    !currentlyEnabledEvents.contains(secondEvent.eventID)
                ) {
                    return
                }

                //draw line
                polyLines.add(
                    googleMap.addPolyline(
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
                val spouseID = Cache.getPerson(clickedEvent.personID)!!.spouseID
                val spouseBirthEvent = Cache.getEarliestEvent(spouseID)
                lineFromTo(clickedEvent, spouseBirthEvent, "Spouse Line")
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
                val eventOwner = Cache.getPerson(clickedEvent.personID)!!

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
                drawFamilyTreeLine(clickedEvent.eventID, 0)
            if (SettingsInfo.getSetting(SettingsInfo.Option.LIFE_STORY_LINES))
                drawLifeStoryLines()

            true
        }

    }

    //todo finish other activities, make bottom panel display info
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
//            googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        }
    }

    override fun onResume() {
        map?.clear()
        addEventsToMap()
        super.onResume()
    }
}