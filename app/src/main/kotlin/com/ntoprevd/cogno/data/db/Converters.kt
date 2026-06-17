package com.ntoprevd.cogno.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(separator = "\n")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return when {
            value == null -> null
            value.isEmpty() -> emptyList()
            else -> value.split("\n")
        }
    }
}
