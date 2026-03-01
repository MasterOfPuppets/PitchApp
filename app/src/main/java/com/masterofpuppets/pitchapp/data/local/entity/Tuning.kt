package com.masterofpuppets.pitchapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a tuning for a specific sound source.
 */
@Entity(tableName = "tunings")
data class Tuning(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nameKey: String?, // Null if it's a custom tuning
    val customName: String?, // Free text if created by the user
    @ColumnInfo(name = "sound_source_id")
    val soundSourceId: Long,
    val notes: List<MusicalNote>,
    val isCustom: Boolean
)