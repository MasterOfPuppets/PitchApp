package com.masterofpuppets.pitchapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.masterofpuppets.pitchapp.data.local.converters.Converters
import com.masterofpuppets.pitchapp.data.local.dao.SoundSourceDao
import com.masterofpuppets.pitchapp.data.local.dao.TuningDao
import com.masterofpuppets.pitchapp.data.local.entity.SoundSource
import com.masterofpuppets.pitchapp.data.local.entity.Tuning

/**
 * Main database class for the application.
 */
@Database(
    entities =[SoundSource::class, Tuning::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PitchAppDatabase : RoomDatabase() {

    abstract fun soundSourceDao(): SoundSourceDao
    abstract fun tuningDao(): TuningDao

    companion object {
        @Volatile
        private var INSTANCE: PitchAppDatabase? = null

        fun getDatabase(context: Context): PitchAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PitchAppDatabase::class.java,
                    "pitchapp_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}