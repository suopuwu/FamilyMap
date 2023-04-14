package com.example.familymap.data

import Model.Event
import Model.Person
import Model.User

object Cache {

    var authToken: String? = null
    var host: String? = null
    var port: String? = null
    var eventList: Array<Event>? = null
    var personList: Array<Person>? = null
    var userID: String? = null
    var username: String? = null

//    fun getFilteredEventList( todo finish making the filters work
//        filterMotherSide: Boolean,
//        filterFatherSide: Boolean,
//        filterMale: Boolean,
//        filterFemale: Boolean
//    ): Array<Event> {
//
//    }
}