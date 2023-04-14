package com.example.familymap.data.serverProxy

import Exchange.Request
import Exchange.Response

object Register : BaseFetcher() {
    fun register(
        usernameP: String,
        passwordP: String,
        firstNameP: String,
        lastNameP: String,
        emailP: String,
        genderP: String,
        host: String,
        port: String
    ): Response {
        val request = Request()
        request.run {
            username = usernameP
            password = passwordP
            firstName = firstNameP
            lastName = lastNameP
            email = emailP
            gender = genderP
        }

        return deserialize(
            Communicator.post(
                "http://$host:$port/user/register",
                request.serialize()
            )
        )
    }
}