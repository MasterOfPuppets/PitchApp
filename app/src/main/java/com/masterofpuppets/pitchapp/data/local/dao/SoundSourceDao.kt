package com.masterofpuppets.pitchapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.masterofpuppets.pitchapp.data.local.entity.SoundSource
/**
 * Data Access Object for the SoundSource entity.
 */
@Dao
interface SoundSourceDao {

    @Query("SELECT * FROM sound_sources")
    fun getAllSoundSources(): Flow<List<SoundSource>>

    @Query("SELECT * FROM sound_sources WHERE id = :id LIMIT 1")
    suspend fun getSoundSourceById(id: Long): SoundSource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoundSource(soundSource: SoundSource): Long

    @Update
    suspend fun updateSoundSource(soundSource: SoundSource)

    @Delete
    suspend fun deleteSoundSource(soundSource: SoundSource)
}