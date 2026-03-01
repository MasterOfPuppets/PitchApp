package com.masterofpuppets.pitchapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.masterofpuppets.pitchapp.audio.AudioEngine
import com.masterofpuppets.pitchapp.data.local.SettingsRepository
import com.masterofpuppets.pitchapp.ui.screens.SettingsScreen
import com.masterofpuppets.pitchapp.ui.screens.TunerScreen
import com.masterofpuppets.pitchapp.utils.PitchConverter
import com.masterofpuppets.pitchapp.utils.PitchResult
import com.masterofpuppets.pitchapp.viewmodel.MainViewModel
import com.masterofpuppets.pitchapp.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), AudioEngine.PitchListener {

    private val audioEngine = AudioEngine()

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(SettingsRepository(applicationContext))
    }

    private lateinit var soundPool: SoundPool
    private var tunedSoundId: Int = 0
    private var isSoundLoaded = false
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                startAudio()
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasAudioFocus = false
                audioEngine.stopAudioEngine()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            requestAudioFocusAndStart()
        } else {
            Log.e("MainActivity", "Microphone permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initSoundPool()

        audioEngine.listener = this

        lifecycleScope.launch {
            viewModel.noiseGateThreshold.collect { newThreshold ->
                audioEngine.setNoiseGateThreshold(newThreshold)
            }
        }

        lifecycleScope.launch {
            viewModel.yinTolerance.collect { newTolerance ->
                audioEngine.setYinTolerance(newTolerance)
            }
        }

        lifecycleScope.launch {
            viewModel.isPitchLocked.collect { isLocked ->
                val volume = viewModel.tuningSoundVolume.value
                if (isLocked && isSoundLoaded && volume > 0f) {
                    soundPool.play(tunedSoundId, volume, volume, 1, 0, 1f)
                }
            }
        }

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "tuner") {
                composable("tuner") {
                    TunerScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                isSoundLoaded = true
            }
        }

        try {
            tunedSoundId = soundPool.load(this, R.raw.tuned_beep, 1)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to load tuning sound: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndStartAudio()
    }

    override fun onPause() {
        super.onPause()
        audioEngine.stopAudioEngine()
        abandonAudioFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }

    private fun checkPermissionAndStartAudio() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                requestAudioFocusAndStart()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun requestAudioFocusAndStart() {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()

        val result = audioManager.requestAudioFocus(audioFocusRequest!!)
        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)

        if (hasAudioFocus) {
            startAudio()
        }
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }
        hasAudioFocus = false
    }

    private fun startAudio() {
        val success = audioEngine.startAudioEngine()
        if (!success) {
            Log.e("MainActivity", "Failed to start audio engine")
        }
    }

    override fun onPitchDetected(pitchInHz: Float) {
        if (pitchInHz <= 0f) {
            val emptyResult = PitchResult(-1, 0, 0.0, 0.0)
            viewModel.updatePitchResult(emptyResult, 0.0)
            return
        }

        val currentReferenceA4 = viewModel.referenceA4.value.toDouble()
        val currentTransposition = viewModel.transposition.value

        val pitchResult = PitchConverter.convertFrequencyToPitch(
            frequencyHz = pitchInHz.toDouble(),
            referenceA4 = currentReferenceA4,
            transposition = currentTransposition
        )

        viewModel.updatePitchResult(pitchResult, pitchInHz.toDouble())
    }

    override fun onWaveformData(waveformData: FloatArray) {
        viewModel.updateWaveformData(waveformData)
    }
}