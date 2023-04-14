package com.example.familymap.data.serverProxy

import com.example.familymap.data.Cache
import com.example.familymap.data.model.FamilyMembersResponse
import com.example.familymap.utils.SuopJsonUtil

object FamilyMembers {
    @Override
    fun deserialize(json: String): FamilyMembersResponse {
        return SuopJsonUtil.gson.fromJson(json, FamilyMembersResponse::class.java)
    }

    public fun getFamilyMembers(): FamilyMembersResponse {
        return deserialize(
            Communicator.get(
                "http://${Cache.host}:${Cache.port}/person",
                Cache.authToken
            )
        )
    }
}