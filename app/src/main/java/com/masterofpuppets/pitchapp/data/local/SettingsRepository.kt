package com.masterofpuppets.pitchapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.masterofpuppets.pitchapp.model.Transposition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository to manage user preferences using Jetpack DataStore.
 */
class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val REFERENCE_A4 = floatPreferencesKey("reference_a4")
        val TRANSPOSITION = stringPreferencesKey("transposition")
        val NOISE_GATE_MIDPOINT = floatPreferencesKey("noise_gate_midpoint")
        val YIN_TOLERANCE = floatPreferencesKey("yin_tolerance")
        val NEEDLE_BASE_WIDTH = floatPreferencesKey("needle_base_width")
        val TUNING_SOUND_VOLUME = floatPreferencesKey("tuning_sound_volume")
    }

    val referenceA4Flow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.REFERENCE_A4] ?: 440.0f
    }

    val transpositionFlow: Flow<Transposition> = context.dataStore.data.map { preferences ->
        val name = preferences[PreferencesKeys.TRANSPOSITION] ?: Transposition.C.name
        try {
            Transposition.valueOf(name)
        } catch (e: IllegalArgumentException) {
            Transposition.C
        }
    }

    val noiseGateMidpointFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOISE_GATE_MIDPOINT] ?: 0.005f
    }

    val yinToleranceFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.YIN_TOLERANCE] ?: 0.15f
    }

    val needleBaseWidthFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NEEDLE_BASE_WIDTH] ?: 36f
    }

    val tuningSoundVolumeFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TUNING_SOUND_VOLUME] ?: 0.5f
    }

    suspend fun updateReferenceA4(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REFERENCE_A4] = value
        }
    }

    suspend fun updateTransposition(transposition: Transposition) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRANSPOSITION] = transposition.name
        }
    }

    suspend fun updateNoiseGateMidpoint(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOISE_GATE_MIDPOINT] = value
        }
    }

    suspend fun updateYinTolerance(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.YIN_TOLERANCE] = value
        }
    }

    suspend fun updateNeedleBaseWidth(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NEEDLE_BASE_WIDTH] = value
        }
    }

    suspend fun updateTuningSoundVolume(volume: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TUNING_SOUND_VOLUME] = volume
        }
    }

    suspend fun resetToFactoryDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}