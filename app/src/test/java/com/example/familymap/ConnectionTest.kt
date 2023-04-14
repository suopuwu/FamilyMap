package com.example.familymap

import com.example.familymap.data.serverProxy.Communicator
import org.junit.Assert
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ConnectionTest {
    @Test
    fun `Cat fact communicator test`() {
        Communicator.get("https://catfact.ninja/fact")
    }

    @Test
    fun `Clear test`() {
        //for non instrumented tests, localhost is used. For instrumented ones, 10.0.2.2 is used
        Assert.assertEquals(
            "{\"message\":\"Clear succeeded.\",\"success\":true}",
            Communicator.post("http://localhost:8080/clear")
        )
    }

    @Test
    fun `Person test`() {
        //for non instrumented tests, localhost is used. For instrumented ones, 10.0.2.2 is used
        Assert.assertEquals(
            "{\"message\":\"Clear succeeded.\",\"success\":true}",
            Communicator.get("http://localhost:8080/person/237b94d5-bf71-4f88-a4fa-0bfa5647a836")
        )
    }
}