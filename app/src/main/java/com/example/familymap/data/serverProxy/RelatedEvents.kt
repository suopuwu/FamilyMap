package com.example.familymap.data.serverProxy

import com.example.familymap.data.Cache
import com.example.familymap.data.model.RelatedEventsResponse
import com.example.familymap.utils.SuopJsonUtil

object RelatedEvents {
    @Override
    fun deserialize(json: String): RelatedEventsResponse {
        return SuopJsonUtil.gson.fromJson(json, RelatedEventsResponse::class.java)
    }

    public fun getRelatedEvents(): RelatedEventsResponse {
        return deserialize(
            Communicator.get(
                "http://${Cache.host}:${Cache.port}/event",
                Cache.authToken
            )
        )
    }
}