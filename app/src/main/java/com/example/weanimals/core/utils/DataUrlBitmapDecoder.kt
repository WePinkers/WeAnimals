package com.example.weanimals.core.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

object DataUrlBitmapDecoder {
    fun decode(value: String): Bitmap? {
        if (!value.startsWith("data:image/", ignoreCase = true)) return null
        val separator = value.indexOf(',')
        if (separator < 0) return null

        return runCatching {
            val encoded = value.substring(separator + 1)
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}
