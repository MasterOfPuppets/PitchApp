package com.masterofpuppets.pitchapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masterofpuppets.pitchapp.data.local.SettingsRepository
import com.masterofpuppets.pitchapp.model.Transposition
import com.masterofpuppets.pitchapp.utils.PitchConverter
import com.masterofpuppets.pitchapp.utils.PitchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.log2

/**
 * Represents the current state of the tuning UI.
 */
data class TuningState(
    val noteIndex: Int = -1,
    val octave: Int = 0,
    val targetFrequency: Double = 0.0,
    val deviationInCents: Double = 0.0,
    val currentFrequency: Double = 0.0
)

/**
 * States for the GitHub API update check.
 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpToDate(val version: String) : UpdateState()
    data class UpdateAvailable(val currentVersion: String, val latestVersion: String, val releaseUrl: String) : UpdateState()
    object Error : UpdateState()
}

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

    // --- Settings Flows exposed to UI ---

    val referenceA4: StateFlow<Float> = settingsRepository.referenceA4Flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 440.0f)

    val transposition: StateFlow<Transposition> = settingsRepository.transpositionFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, Transposition.C)

    val noiseGateMidpoint: StateFlow<Float> = settingsRepository.noiseGateMidpointFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.005f)

    val yinTolerance: StateFlow<Float> = settingsRepository.yinToleranceFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.15f)

    val needleBaseWidth: StateFlow<Float> = settingsRepository.needleBaseWidthFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 36f)

    val tuningSoundVolume: StateFlow<Float> = settingsRepository.tuningSoundVolumeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    // --- Update Check State ---
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var lockJob: Job? = null

    private val pitchHistorySize = 5
    private val pitchHistory = mutableListOf<Double>()

    private var lastValidPitchTime: Long = 0L
    private val HOLD_DURATION_MS = 500L

    private var currentSmoothedFrequency: Double = -1.0
    private val EMA_ALPHA = 0.3 // Smoothing factor

    init {
        viewModelScope.launch {
            _noiseGateThreshold.value = settingsRepository.noiseGateMidpointFlow.first()
        }
    }

    fun updatePitchResult(pitchResult: PitchResult, rawFrequency: Double) {
        val currentTime = System.currentTimeMillis()

        if (rawFrequency <= 0.0) {
            if (currentTime - lastValidPitchTime > HOLD_DURATION_MS) {
                pitchHistory.clear()
                currentSmoothedFrequency = -1.0
                publishFilteredState(PitchResult(-1, 0, 0.0, 0.0), 0.0)
            }
            return
        }

        lastValidPitchTime = currentTime

        // Median Filter
        pitchHistory.add(rawFrequency)
        if (pitchHistory.size > pitchHistorySize) {
            pitchHistory.removeAt(0)
        }

        if (pitchHistory.size < pitchHistorySize) {
            applySmoothingAndPublish(rawFrequency)
            return
        }

        val sortedHistory = pitchHistory.sorted()
        val medianFrequency = sortedHistory[pitchHistorySize / 2]

        // HARMONIC REJECTION: Ignore octave jumps (decaying strings)
        if (currentSmoothedFrequency > 0.0) {
            val ratio = medianFrequency / currentSmoothedFrequency
            val isOctaveUp = ratio in 1.85..2.15
            val isOctaveDown = ratio in 0.45..0.55

            if (isOctaveUp || isOctaveDown) {
                applySmoothingAndPublish(currentSmoothedFrequency)
                return
            }
        }

        applySmoothingAndPublish(medianFrequency)
    }

    private fun applySmoothingAndPublish(targetFrequency: Double) {
        if (currentSmoothedFrequency <= 0.0) {
            currentSmoothedFrequency = targetFrequency
        } else {
            // SNAP & SMOOTH: Adaptive Inertia
            val centsDiff = abs(1200.0 * log2(targetFrequency / currentSmoothedFrequency))

            if (centsDiff > 150.0) {
                currentSmoothedFrequency = targetFrequency
            } else {
                currentSmoothedFrequency = (EMA_ALPHA * targetFrequency) + ((1.0 - EMA_ALPHA) * currentSmoothedFrequency)
            }
        }

        val smoothedPitchResult = PitchConverter.convertFrequencyToPitch(
            frequencyHz = currentSmoothedFrequency,
            referenceA4 = referenceA4.value.toDouble(),
            transposition = transposition.value
        )

        publishFilteredState(smoothedPitchResult, currentSmoothedFrequency)
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
                        delay(500)
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

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    fun checkForUpdates(currentVersion: String) {
        if (_updateState.value is UpdateState.Checking) return
        _updateState.value = UpdateState.Checking

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/MasterOfPuppets/PitchApp/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    val latestVersionTag = jsonObject.getString("tag_name")
                    val releaseUrl = jsonObject.getString("html_url")

                    val cleanLatest = latestVersionTag.removePrefix("v")
                    val cleanCurrent = currentVersion.removePrefix("v")

                    if (cleanLatest != cleanCurrent) {
                        _updateState.value = UpdateState.UpdateAvailable(
                            currentVersion = cleanCurrent,
                            latestVersion = cleanLatest,
                            releaseUrl = releaseUrl
                        )
                    } else {
                        _updateState.value = UpdateState.UpToDate(cleanCurrent)
                    }
                } else {
                    _updateState.value = UpdateState.Error
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _updateState.value = UpdateState.Error
            }
        }
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