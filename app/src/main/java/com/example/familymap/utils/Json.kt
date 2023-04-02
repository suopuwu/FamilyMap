package com.example.familymap.utils

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class Json {
    companion object {
        fun streamToString(stream: InputStream): String {
            val result = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var length: Int
            while (stream.read(buffer).also { length = it } != -1) {
                result.write(buffer, 0, length)
            }
            return result.toString()
        }
    }
}