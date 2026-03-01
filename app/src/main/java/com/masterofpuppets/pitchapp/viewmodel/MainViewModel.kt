package com.masterofpuppets.pitchapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masterofpuppets.pitchapp.data.local.SettingsRepository
import com.masterofpuppets.pitchapp.model.Transposition
import com.masterofpuppets.pitchapp.utils.PitchConverter
import com.masterofpuppets.pitchapp.utils.PitchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Represents the current state of the tuning UI.
 */
data class TuningState(
    val noteIndex: Int = -1, // There's no note to show
    val octave: Int = 0,
    val targetFrequency: Double = 0.0,
    val deviationInCents: Double = 0.0,
    val currentFrequency: Double = 0.0
)

/**
 * ViewModel to manage the tuning data and expose it to Jetpack Compose.
 */
class MainViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _tuningState = MutableStateFlow(TuningState())
    val tuningState: StateFlow<TuningState> = _tuningState.asStateFlow()

    private val _noiseGateThreshold = MutableStateFlow(0.005f)
    val noiseGateThreshold: StateFlow<Float> = _noiseGateThreshold.asStateFlow()

    private val _waveformData = MutableStateFlow(FloatArray(0))
    val waveformData: StateFlow<FloatArray> = _waveformData.asStateFlow()

    private val _isPitchLocked = MutableStateFlow(false)
    val isPitchLocked: StateFlow<Boolean> = _isPitchLocked.asStateFlow()

    val referenceA4: StateFlow<Float> = settingsRepository.referenceA4Flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 440.0f)

    val transposition: StateFlow<Transposition> = settingsRepository.transpositionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Transposition.C)

    val noiseGateMidpoint: StateFlow<Float> = settingsRepository.noiseGateMidpointFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.005f)

    val yinTolerance: StateFlow<Float> = settingsRepository.yinToleranceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.15f)

    val needleBaseWidth: StateFlow<Float> = settingsRepository.needleBaseWidthFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 36f)

    val tuningSoundVolume: StateFlow<Float> = settingsRepository.tuningSoundVolumeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    private var lockJob: Job? = null

    private val pitchHistorySize = 5
    private val pitchHistory = mutableListOf<Double>()

    /**
     * Updates the UI state by applying a median filter to raw pitch data.
     */
    fun updatePitchResult(pitchResult: PitchResult, rawFrequency: Double) {

        if (rawFrequency <= 0.0) {
            pitchHistory.clear()
            publishFilteredState(pitchResult, rawFrequency)
            return
        }

        pitchHistory.add(rawFrequency)

        if (pitchHistory.size > pitchHistorySize) {
            pitchHistory.removeAt(0)
        }

        if (pitchHistory.size < pitchHistorySize) {
            publishFilteredState(pitchResult, rawFrequency)
            return
        }

        val sortedHistory = pitchHistory.sorted()
        val medianFrequency = sortedHistory[pitchHistorySize / 2]
        val smoothedPitchResult = PitchConverter.convertFrequencyToPitch(
            frequencyHz = medianFrequency,
            referenceA4 = referenceA4.value.toDouble(),
            transposition = transposition.value
        )

        publishFilteredState(smoothedPitchResult, medianFrequency)
    }

    private fun publishFilteredState(pitchResult: PitchResult, frequency: Double) {
        _tuningState.value = TuningState(
            noteIndex = pitchResult.noteIndex,
            octave = pitchResult.octave,
            targetFrequency = pitchResult.targetFrequency,
            deviationInCents = pitchResult.deviationInCents,
            currentFrequency = frequency
        )

        val isPerfectlyInTune = abs(pitchResult.deviationInCents) <= 3.0 && pitchResult.noteIndex != -1

        if (isPerfectlyInTune) {
            if (lockJob == null || lockJob?.isActive == false) {
                if (!_isPitchLocked.value) {
                    lockJob = viewModelScope.launch {
                        delay(500) // detection of pitch lock duration. TODO: make it configurable
                        _isPitchLocked.value = true
                    }
                }
            }
        } else {
            lockJob?.cancel()
            lockJob = null
            if (_isPitchLocked.value) {
                _isPitchLocked.value = false
            }
        }
    }

    fun setNoiseGateThreshold(threshold: Float) {
        _noiseGateThreshold.value = threshold
    }

    fun updateWaveformData(data: FloatArray) {
        _waveformData.value = data
    }

    fun updateReferenceA4Setting(value: Float) = viewModelScope.launch {
        settingsRepository.updateReferenceA4(value)
    }

    fun updateTranspositionSetting(transposition: Transposition) = viewModelScope.launch {
        settingsRepository.updateTransposition(transposition)
    }

    fun updateNoiseGateMidpointSetting(value: Float) = viewModelScope.launch {
        settingsRepository.updateNoiseGateMidpoint(value)
        _noiseGateThreshold.value = value
    }

    fun updateYinToleranceSetting(value: Float) = viewModelScope.launch {
        settingsRepository.updateYinTolerance(value)
    }

    fun updateNeedleBaseWidthSetting(value: Float) = viewModelScope.launch {
        settingsRepository.updateNeedleBaseWidth(value)
    }

    fun updateTuningSoundVolumeSetting(volume: Float) = viewModelScope.launch {
        settingsRepository.updateTuningSoundVolume(volume)
    }

    fun resetToFactoryDefaults() = viewModelScope.launch {
        settingsRepository.resetToFactoryDefaults()
        _noiseGateThreshold.value = 0.005f
    }

    fun resetNoiseGateSliderToMidpoint() {
        _noiseGateThreshold.value = noiseGateMidpoint.value
    }
}

class MainViewModelFactory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}