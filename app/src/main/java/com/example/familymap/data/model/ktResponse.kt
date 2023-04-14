package com.example.familymap.data.model

import Model.Event
import Model.Person


//These are new model classes I didn't need for the server.
data class RelatedEventsResponse(
    val data: Array<Event>?,
    val success: Boolean,
    val message: String?
)

data class FamilyMembersResponse(
    val data: Array<Person>?,
    val success: Boolean,
    val message: String?
)