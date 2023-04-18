package com.example.familymap

import Exchange.Response
import com.example.familymap.data.Cache
import com.example.familymap.data.serverProxy.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ProxyTest {
    private val host = "localhost"
    private val port = "8080"

    //the cache is a singleton, so to prevent tests from affecting each other, I've added this
    @Before
    fun clearCache() {
        Cache.run {
            authToken = null
            host = null
            port = null
            eventList = null
            personList = null
            usersPersonID = null
            username = null
        }
    }

    //the cache is only updated on a successful login or register. It is done in the ui, so I mimic it here

    private fun updateCache(response: Response) {
        if (response.success) {
            Cache.setLoginInfo(
                response.authtoken,
                response.personID,
                host,
                port,
                response.username
            )
        }
    }

    @Test
    fun `Successful login test`() {
        val response = Login.login("sheila", "parker", host, port)
        updateCache(response)
        Assert.assertTrue(response.success)
    }

    @Test
    fun `Failed login, incorrect password test`() {
        val response = Login.login("sheila", "wrong password", host, port)
        updateCache(response)
        Assert.assertFalse(response.success)
    }

    @Test
    fun `Successful register`() {
        //I'm going to be very confused if UUID generates a duplicate username
        val response = Register.register(
            UUID.randomUUID().toString(),
            "password",
            "first",
            "last",
            "email",
            "f",
            host,
            port
        )
        updateCache(response)
        Assert.assertTrue(response.success)
    }

    @Test
    fun `Failed register, duplicate username`() {
        //I'm going to be very confused if UUID generates a duplicate username
        val response = Register.register(
            "sheila",
            "password",
            "first",
            "last",
            "email",
            "f",
            host,
            port
        )
        updateCache(response)
        Assert.assertFalse(response.success)
    }

    @Test
    fun `Successful retrieve people test`() {
        //login to start
        `Successful login test`()

        val getPeopleResponse = FamilyMembers.getFamilyMembers()
        Assert.assertTrue(getPeopleResponse.success)
    }

    @Test
    fun `Failed retrieve people test, wrong password`() {
        `Failed register, duplicate username`()

        val getPeopleResponse = FamilyMembers.getFamilyMembers()
        Assert.assertFalse(getPeopleResponse.success)
    }

    @Test
    fun `Successful retrieve events test`() {
        `Successful login test`()

        val getEventsResponse = RelatedEvents.getRelatedEvents()
        Assert.assertTrue(getEventsResponse.success)
    }

    @Test
    fun `Failed retrieve events test, wrong password`() {
        `Failed login, incorrect password test`()

        val getEventsResponse = RelatedEvents.getRelatedEvents()
        Assert.assertFalse(getEventsResponse.success)

    }
}