package com.example.familymap.data.serverProxy

import Exchange.Response
import com.example.familymap.utils.SuopJsonUtil

abstract class BaseFetcher {
    protected fun deserialize(json: String): Response {
        return SuopJsonUtil.gson.fromJson(json, Response::class.java)
    }
}