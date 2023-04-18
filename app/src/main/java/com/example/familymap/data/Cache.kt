package com.example.familymap.data

import Model.Event
import Model.Model
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

    fun setLoginInfo(
        authTokenParam: String,
        personIDParam: String,
        hostParam: String,
        portParam: String,
        usernameParam: String
    ) {
        authToken = authTokenParam
        usersPersonID = personIDParam
        host = hostParam
        port = portParam
        username = usernameParam
    }

    //initializes secondary values, like maps
    fun initSecondaryValues() {
        resetFilterSets()
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
    fun getHueFor(key: String): Float {
        val hueOwner = key.lowercase()
        //return hue if owner already has one
        if (markerHues.containsKey(hueOwner)) {
            return markerHues[hueOwner]!!
        }
        //when initialized, or when all colors somehow run out, regenerate them
        if (remainingColors.size == 0) {
            //Step value determines how many colors are in the set.
            for (i in 0..359 step 30) {//360 crashes the program
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

    fun getPerson(personID: String?): Person? {
        return internalPersonMap[personID]
    }

    fun getEvent(eventID: String?): Event? {
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

    fun getEarliestEvent(personID: String?): Event? {
        return getEventsForPerson(personID ?: "")?.first()
    }

    private fun resetFilterSets() {
        femaleMothersSideEvents.clear()
        maleMothersSideEvents.clear()
        femaleFathersSideEvents.clear()
        maleFathersSideEvents.clear()
    }

    //sets for performance, prevents filters from being regenerated every map refresh.
    private val femaleMothersSideEvents = HashSet<String>()
    private val maleMothersSideEvents = HashSet<String>()
    private val femaleFathersSideEvents = HashSet<String>()
    private val maleFathersSideEvents = HashSet<String>()

    enum class Side {
        MOTHERS,
        FATHERS
    }

    fun getFilteredPeopleIDs(): HashSet<String> {
        val filterMother = SettingsInfo.getSetting(SettingsInfo.Option.FILTER_MOTHER)
        val filterFather = SettingsInfo.getSetting(SettingsInfo.Option.FILTER_FATHER)
        val filterMale = SettingsInfo.getSetting(SettingsInfo.Option.FILTER_MALE)
        val filterFemale = SettingsInfo.getSetting(SettingsInfo.Option.FILTER_FEMALE)

        //returns true if the gender of the person passed is included in the filters
        fun genderIncluded(person: Person?): Boolean {
            if (person == null)
                return false
            return (filterMale && person.gender == "m") || (filterFemale && person.gender == "f")
        }

        //prep for adding people
        val userPerson = getPerson(usersPersonID!!)!!

        //recursively traverses up a family tree from a person
        fun addPeople(personID: String?, side: Side) {
            //end condition
            if (personID == null) {
                return
            }
            //add them to the list if their gender is included in the filter
            val person = getPerson(personID) ?: return
            if (person.gender.lowercase() == "f") {
                if (side == Side.MOTHERS)
                    femaleMothersSideEvents.add(personID)
                if (side == Side.FATHERS)
                    femaleFathersSideEvents.add(personID)
            } else {
                if (side == Side.MOTHERS)
                    maleMothersSideEvents.add(personID)
                if (side == Side.FATHERS)
                    maleFathersSideEvents.add(personID)
            }
            //call addPeople on their parents
            addPeople(person.fatherID, side)
            addPeople(person.motherID, side)
        }

        //Fill the sets, if they are not already filled.
        if (femaleMothersSideEvents.size == 0 && femaleFathersSideEvents.size == 0 &&
            maleMothersSideEvents.size == 0 && maleFathersSideEvents.size == 0
        ) {
            addPeople(userPerson.motherID, Side.MOTHERS)
            addPeople(userPerson.fatherID, Side.FATHERS)
        }

        val returnSet = HashSet<String>()
        if (genderIncluded(userPerson))
            returnSet.add(userPerson.personID)
        if (userPerson.spouseID != null && genderIncluded(getPerson(userPerson.spouseID))) {
            returnSet.add(userPerson.spouseID)
        }
        if (filterFemale) {
            if (filterMother)
                returnSet.addAll(femaleMothersSideEvents)
            if (filterFather)
                returnSet.addAll(femaleFathersSideEvents)
        }
        if (filterMale) {
            if (filterMother)
                returnSet.addAll(maleMothersSideEvents)
            if (filterFather)
                returnSet.addAll(maleFathersSideEvents)
        }

        return returnSet
    }

    fun getFilteredEventList(): Array<Event> {
        val returnEvents = ArrayList<Event>()

        fun addPersonsEvents(personID: String) {
            internalPersonToSortedEventListMap[personID]?.let {
                returnEvents.addAll(it)
            }
        }

        //add all events that are included in the filter
        for (personID in getFilteredPeopleIDs()) {
            addPersonsEvents(personID)
        }
        return returnEvents.toTypedArray()
    }

    //does not get siblings, as per the spec
    fun getImmediateFamily(personID: String): ArrayList<Person> {
        val person = getPerson(personID) ?: return arrayListOf()
        val returnList = ArrayList<Person>()

        //add mother, father, spouse
        fun addPeople(vararg personToAddID: String?) {
            personToAddID.forEach {
                if (it != null)
                    getPerson(it)?.run { returnList.add(this) }
            }
        }
        addPeople(person.motherID, person.fatherID, person.spouseID)

        //add all children
        personList?.forEach {
            //if more performance is needed, make a map here
            if (it.fatherID == person.personID || it.motherID == person.personID) {
                returnList.add(it)
            }
        }
        return returnList
    }

    fun determineRelationship(personOne: Person, personTwo: Person): String {
        return if (personOne.motherID != null && personOne.motherID == personTwo.personID) "Mother"
        else if (personOne.fatherID != null && personOne.fatherID == personTwo.personID) "Father"
        else if (personOne.spouseID != null && personOne.spouseID == personTwo.personID) "Spouse"
        else if ((personTwo.fatherID != null && personTwo.fatherID == personOne.personID) ||
            (personTwo.motherID != null && personTwo.motherID == personOne.personID)
        ) "Child"
        else "Not immediate"
    }

    private fun stringIsFound(query: String, vararg comparisons: String): Boolean {
        var returnBoolean = false
        comparisons.forEach { comparison ->
            if (comparison.lowercase().contains(query.lowercase())) {
                returnBoolean = true
                return@forEach
            }
        }
        return returnBoolean
    }

    //these two are definitely probably the most intensive functions, but optimizing would require magic
    fun searchPeople(query: String): Array<Person> {
        val returnList = ArrayList<Person>()
        personList?.forEach { item ->
            if (stringIsFound(query, "${item.firstName} ${item.lastName}")) {

                returnList.add(item)
            }
        }
        return returnList.toTypedArray()
    }

    //these share a lot of code, but they're different enough that it wouldn't really be feasible to share the code
    //after doing a lot of research, kotlin arrayLists are invariant, which apparently means that ArrayList<Person>
    //is not a subtype of ArrayList<Any>
    fun searchEvents(query: String): Array<Event> {
        val returnList = ArrayList<Event>()
        getFilteredEventList().forEach { item ->
            if (stringIsFound(
                    query,
                    "${item.country}, ${item.city}",
                    item.year.toString(),
                    item.eventType
                )
            ) {
                returnList.add(item)
            }
        }
        return returnList.toTypedArray()
    }
}