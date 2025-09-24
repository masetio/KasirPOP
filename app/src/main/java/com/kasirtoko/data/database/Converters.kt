package com.kasirtoko.data.database

import androidx.room.TypeConverter

class Converters {
    
    @TypeConverterAssistant
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }
    }
}
