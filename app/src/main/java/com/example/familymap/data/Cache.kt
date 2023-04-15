package com.example.familymap.data

import Model.Event
import Model.Person
import android.graphics.Color
import androidx.compose.ui.text.toLowerCase

object Cache {
    var authToken: String? = null
    var host: String? = null
    var port: String? = null
    var eventList: Array<Event>? = null
    var personList: Array<Person>? = null
    var usersPersonID: String? = null
    var username: String? = null

    //maps for performance and ease of use
    private var internalPersonMap = HashMap<String, Person>()
    private var internalEventMap = HashMap<String, Event>()
    private var internalPersonToSortedEventListMap = HashMap<String, ArrayList<Event>>()

    private val markerHues: HashMap<String, Float> = HashMap()
    private var remainingColors = mutableSetOf<Int>()

    //initializes secondary values, like maps
    fun initSecondaryValues() {
        fillDataMaps()
    }

    private fun fillDataMaps() {
        //add people to map
        for (person in personList ?: arrayOf<Person>()) {
            internalPersonMap[person.personID] = person
        }
        //add events to event map and sorted map
        for (event in eventList ?: arrayOf<Event>()) {
            internalEventMap[event.eventID] = event
            if (internalPersonToSortedEventListMap.containsKey(event.personID)) {
                internalPersonToSortedEventListMap[event.personID]!!.add(event)
            } else {
                internalPersonToSortedEventListMap[event.personID] = arrayListOf(event)
            }
        }
        //sort sorted map
        sortEventListMap()
    }

    private fun sortEventListMap() {
        for (entry in internalPersonToSortedEventListMap) {
            val list = entry.value
            val sortedList = ArrayList<Event>()
            var death: Event? = null
            //ensure birth is added first
            for (event in list) {
                if (event.eventType.equals("birth", ignoreCase = true)) {
                    sortedList.add(event)
                    list.remove(event)
                    break
                }
            }

            //set death aside to be added later.
            for (event in list) {
                if (event.eventType.equals("death", ignoreCase = true)) {
                    death = event
                    list.remove(event)
                    break
                }
            }

            //if more performance is needed, write a more efficient sorting algo.
            //iterates over the pool of events, adding the earliest remaining event to the sorted until none remain.
            while (list.size > 0) {
                var currentEarliest: Event = list.first()
                for (event in list) {
                    //if the current earliest occurs later than the one being considered
                    if (currentEarliest.year > event.year) {
                        currentEarliest = event

                        //if they share years, compare them alphabetically.
                    } else if (currentEarliest.year == event.year) {
                        if (currentEarliest.eventType.lowercase() > event.eventType.lowercase()) {
                            currentEarliest = event
                        }
                    }
                }
                //add the one found to be the lowest, and remove it from the pool.
                sortedList.add(currentEarliest)
                list.remove(currentEarliest)
            }

            //add the death event last, if it exists
            if (death != null) {
                sortedList.add(death)
            }

            internalPersonToSortedEventListMap[entry.key] = sortedList
        }
    }

    //returns a hue for a given key, without duplicates for the first 17 keys
    fun getHueFor(hueOwner: String): Float {
        //return hue if owner already has one
        if (markerHues.containsKey(hueOwner)) {
            return markerHues[hueOwner]!!
        }
        //when initialized, or when all colors somehow run out, regenerate them
        if (remainingColors.size == 0) {
            //Step value determines how many colors are in the set.
            for (i in 0..359 step 20) {//360 crashes the program
                remainingColors.add(i)
            }
        }
        //get new, unique color, remove it from the pool of colors, and add it to the colors map
        val color = remainingColors.random()
        remainingColors.remove(color)
        markerHues[hueOwner] = color.toFloat()

        return markerHues[hueOwner]!!
    }

    //returns a hex color for a given key
    fun getColorFor(colorOwner: String): Int {
        val hue = getHueFor(colorOwner)
        return Color.HSVToColor(floatArrayOf(hue, 0.8F, 0.65F))
    }

    fun getPerson(personID: String): Person? {
        return internalPersonMap[personID]
    }

    fun getEvent(eventID: String): Event? {
        return internalEventMap[eventID]
    }

    fun getEventsForPerson(personID: String): ArrayList<Event>? {
        return internalPersonToSortedEventListMap[personID]
    }

    //returns the event of eventType belonging to personID if it exists. eventType is not case sensitive
    fun getPersonsEvent(personID: String, eventType: String): Event? {
        val events = getEventsForPerson(personID) ?: return null
        for (event in events) {
            if (event.eventType.equals(eventType, ignoreCase = true)) {
                return event
            }
        }
        return null
    }

    fun getEarliestEvent(personID: String): Event? {
        return getEventsForPerson(personID)?.first()
    }

    fun getFilteredEventList(): Array<Event> {
        val filterMother = SettingsInfo.getSetting(SettingsInfo.Option.FILTER_MOTHER)
        val filterFather = SettingsInfo.getSetting(SettingsInfo.Option.FILTER_FATHER)
        val filterMale = SettingsInfo.getSetting(SettingsInfo.Option.FILTER_MALE)
        val filterFemale = SettingsInfo.getSetting(SettingsInfo.Option.FILTER_FEMALE)

        //returns true if the gender of the person passed is included in the filters
        fun genderIncluded(person: Person): Boolean {
            return (filterMale && person.gender == "m") || (filterFemale && person.gender == "f")
        }

        //prep for adding people
        val filteredPeople = ArrayList<Person>()
        val userPerson = getPerson(usersPersonID!!)!!
        if (genderIncluded(userPerson)) {
            filteredPeople.add(userPerson)
        }
        //recursively traverses up a family tree from a person
        fun addPeople(personID: String?) {
            //end condition
            if (personID == null) {
                return
            }
            //add them to the list if their gender is included in the filter
            val person = getPerson(personID) ?: return
            if (genderIncluded(person)) {
                filteredPeople.add(person)
            }
            //call addPeople on their parents
            addPeople(person.fatherID)
            addPeople(person.motherID)
        }

        //add all people included by the filters
        if (filterMother) {
            addPeople(userPerson.motherID)
        }
        if (filterFather) {
            addPeople(userPerson.fatherID)
        }

        //get all events held by people in the filtered people list
        val events = ArrayList<Event>()
        for (event in eventList!!) {
            for (person in filteredPeople) {
                if (event.personID == person.personID) {//you could improve performance here if necessary with another map
                    events.add(event)
                    break;
                }
            }
        }

        return events.toTypedArray()
    }
}