package com.example.familymap.data.serverProxy

import Exchange.Response
import com.example.familymap.utils.SuopJsonUtil
import java.net.HttpURLConnection
import java.net.URL

class Communicator {
    companion object {
        private fun getResponseJson(
            url: String,
            requestMethod: String,
            jsonBody: String? = null,
            authToken: String? = null,
        ): String {
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
                    return SuopJsonUtil.streamToString(errorStream ?: inputStream)

                }
            } catch (throwable: Throwable) {
                println(throwable)
                val response = Response()
                response.message = throwable.message
                return response.serialize()
            }
        }

        public fun get(url: String, token: String? = null): String {
            return getResponseJson(url, "GET", null, token)
        }

        public fun post(url: String, jsonBody: String? = null): String {
            return getResponseJson(url, "POST", jsonBody)
        }
    }
}
