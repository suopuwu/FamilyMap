package com.example.familymap

import Exchange.Response
import androidx.core.app.Person
import com.example.familymap.data.Cache
import com.example.familymap.data.serverProxy.FamilyMembers
import com.example.familymap.data.serverProxy.Login
import com.example.familymap.data.serverProxy.Register
import com.example.familymap.data.serverProxy.RelatedEvents
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*
import Model.*
import com.example.familymap.data.SettingsInfo

//Note: these tests assume you are using the pass off database
class ModelTest {
    private val host = "localhost"
    private val port = "8080"

    //Sets the cache up, as if the user had logged in
    @Before
    fun emulateLogin() {
        val response = Login.login("sheila", "parker", host, port)
        //update the cache
        if (response.success) {
            Cache.setLoginInfo(
                response.authtoken,
                response.personID,
                host,
                port,
                response.username
            )
        }

        //Get related events and add to the cache
        val relatedEventsResponse = RelatedEvents.getRelatedEvents()
        Cache.eventList = relatedEventsResponse.data

        //get family members and add to the cache
        val familyMembersResponse = FamilyMembers.getFamilyMembers()
        Cache.personList = familyMembersResponse.data

        Cache.initSecondaryValues()
    }

    @Before
    fun resetSettings() {
        SettingsInfo.resetSettings()
    }

    @Test
    fun `Successful relationship calculation`() {
        //make two people, one with the other set as their spouse
        val spouseOne = Person()
        spouseOne.spouseID = "two"
        val spouseTwo = Person()
        spouseTwo.personID = "two"
        Assert.assertEquals("Spouse", Cache.determineRelationship(spouseOne, spouseTwo))
    }

    @Test
    fun `Not Immediately Related calculation`() {
        //make two people, sharing no attributes
        Assert.assertEquals("Not immediate", Cache.determineRelationship(Person(), Person()))
    }

    @Test
    fun `Filter males`() {
        SettingsInfo.setSetting(SettingsInfo.Option.FILTER_MALE, false)
        val events = Cache.getFilteredEventList()
        for (event in events) {
            Cache.getPerson(event.personID)?.let {
                Assert.assertEquals("f", it.gender)
            }
        }
    }

    @Test
    fun `Filter Everyone`() {
        //I figured this would be "abnormal" enough, because why would you do that.
        SettingsInfo.setSetting(SettingsInfo.Option.FILTER_MALE, false)
        SettingsInfo.setSetting(SettingsInfo.Option.FILTER_FEMALE, false)
        val events = Cache.getFilteredEventList()
        Assert.assertEquals(0, events.size)
    }

    @Test
    fun `Sort Sheila Parker's events`() {
        val events = Cache.getEventsForPerson(Cache.usersPersonID!!)!!
        //ensure there are at least 2 events
        Assert.assertTrue(events.size > 1)

        var priorEventYear = -9999
        //if any event takes place prior to the prior event, generate a false assertion
        for (event in events) {
            if (event.year < priorEventYear) {
                Assert.assertTrue(false)
                return
            }
            priorEventYear = event.year
        }
        //otherwise, everything is good.
        Assert.assertTrue(true)

    }

    @Test
    fun `Sort a nonexistent person's events`() {
        val events = Cache.getEventsForPerson("No such person")
        Assert.assertNull(events)
    }

    @Test
    fun `Search for Sheila Parker + case insensitive`() {
        val people = Cache.searchPeople("SheIlA pARkeR")
        Assert.assertTrue(people[0].personID == Cache.usersPersonID)
    }

    @Test
    fun `Search for a nonexistent person`() {
        val people =
            Cache.searchPeople("I will be very confused if there are any people in the database named this")
        Assert.assertTrue(people.isEmpty())
    }

    @Test
    fun `Search for Sheila Parker's Death`() {
        val events = Cache.searchEvents("2015")

        //make sure it's the user's(Sheila parker) ID
        Assert.assertTrue(events[0].personID == Cache.usersPersonID)
        Assert.assertTrue(events[0].eventType == "death")
    }

    @Test
    fun `Search for an event that hasn't happened`() {
        val events =
            Cache.searchEvents("A string that is definitely not included in any events")
        Assert.assertTrue(events.isEmpty())
    }
}