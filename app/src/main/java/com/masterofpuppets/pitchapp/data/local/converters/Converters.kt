package com.masterofpuppets.pitchapp.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.masterofpuppets.pitchapp.data.local.entity.MusicalNote
import com.masterofpuppets.pitchapp.data.local.entity.SoundSourceCategory

/**
 * Type converters for Room database to handle complex data types.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromSoundSourceCategory(category: SoundSourceCategory): String {
        return category.name
    }

    @TypeConverter
    fun toSoundSourceCategory(categoryName: String): SoundSourceCategory {
        return SoundSourceCategory.valueOf(categoryName)
    }

    @TypeConverter
    fun fromMusicalNoteList(notes: List<MusicalNote>): String {
        return gson.toJson(notes)
    }

    @TypeConverter
    fun toMusicalNoteList(notesString: String): List<MusicalNote> {
        val listType = object : TypeToken<List<MusicalNote>>() {}.type
        return gson.fromJson(notesString, listType)
    }
}