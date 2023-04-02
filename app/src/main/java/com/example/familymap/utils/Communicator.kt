package com.example.familymap.utils

import Exchange.ExchangeTypes
import Exchange.Response
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class Communicator {
    companion object {
        private fun communicate(
            url: String,
            requestMethod: String,
            jsonBody: String? = null,
            authToken: String? = null
        ): Response {
            //open the connection
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.run {
                    //set headers

                    setRequestMethod(requestMethod)
                    if (authToken != null) {
                        setRequestProperty("Authorization", authToken)
                    }
                    setRequestProperty("Content-Type", "application/json; utf-8")
                    setRequestProperty("Accept", "application/json")

                    if (requestMethod == "POST") {
                        doOutput = true
                    }
                    //set body
                    if (jsonBody != null) {
                        outputStream.write(jsonBody.toByteArray())
                    }

                    return Response.deserialize(
                        Json.streamToString(errorStream ?: inputStream),
                        ExchangeTypes.RESPONSE
                    ) as Response
                }
            } catch (throwable: Throwable) {
                println(throwable)
                return Response()
            }
        }

        public fun get(url: String, token: String? = null): Response {
            return communicate(url, "GET", null, token)
        }

        public fun post(url: String, jsonBody: String? = null): Response {
            return communicate(url, "POST", jsonBody)
        }
    }
}
