package com.example.familymap

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.familymap.data.serverProxy.Communicator

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun personTest() {
        //for non instrumented tests, localhost is used. For instrumented ones, 10.0.2.2 is used
        val response =
            Communicator.get("http://10.0.2.2:8080/person/237b94d5-bf71-4f88-a4fa-0bfa5647a836");
    }
}