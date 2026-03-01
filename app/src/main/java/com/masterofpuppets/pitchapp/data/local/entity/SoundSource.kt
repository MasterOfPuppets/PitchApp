package com.masterofpuppets.pitchapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sound_sources")
data class SoundSource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nameKey: String, // Key to be used with string resources for localization
    val category: SoundSourceCategory,
    val minFrequency: Double,
    val maxFrequency: Double
)