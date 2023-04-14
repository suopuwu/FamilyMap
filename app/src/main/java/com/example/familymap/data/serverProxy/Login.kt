package com.example.familymap.data.serverProxy

import Exchange.Request
import Exchange.Response

object Login : BaseFetcher() {
    fun login(
        usernameP: String,
        passwordP: String,
        host: String,
        port: String
    ): Response {
        val request = Request()
        request.run {
            username = usernameP
            password = passwordP
        }
        return deserialize(
            Communicator.post(
                "http://$host:$port/user/login",
                request.serialize()
            )
        )
    }
}