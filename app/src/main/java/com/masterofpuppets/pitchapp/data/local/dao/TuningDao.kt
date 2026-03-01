package com.masterofpuppets.pitchapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.masterofpuppets.pitchapp.data.local.entity.Tuning
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the Tuning entity.
 */
@Dao
interface TuningDao {

    @Query("SELECT * FROM tunings WHERE sound_source_id = :soundSourceId")
    fun getTuningsForSoundSource(soundSourceId: Long): Flow<List<Tuning>>

    @Query("SELECT * FROM tunings WHERE id = :id LIMIT 1")
    suspend fun getTuningById(id: Long): Tuning?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTuning(tuning: Tuning): Long

    @Update
    suspend fun updateTuning(tuning: Tuning)

    @Delete
    suspend fun deleteTuning(tuning: Tuning)
}