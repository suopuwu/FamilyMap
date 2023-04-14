package com.example.familymap.utils

import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.InputStream

object SuopJsonUtil {
    fun streamToString(stream: InputStream): String {
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var length: Int
        while (stream.read(buffer).also { length = it } != -1) {
            result.write(buffer, 0, length)
        }
        return result.toString()
    }

    public val gson = Gson()
}