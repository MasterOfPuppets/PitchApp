package com.masterofpuppets.pitchapp.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterofpuppets.pitchapp.R
import com.masterofpuppets.pitchapp.ui.components.PitchTextPanel
import com.masterofpuppets.pitchapp.ui.components.dial.TunerDial
import com.masterofpuppets.pitchapp.viewmodel.MainViewModel

@Composable
fun TunerScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val tuningState by viewModel.tuningState.collectAsState()
    val noiseGateThreshold by viewModel.noiseGateThreshold.collectAsState()
    val waveformData by viewModel.waveformData.collectAsState()
    val isPitchLocked by viewModel.isPitchLocked.collectAsState()

    val needleWidth by viewModel.needleBaseWidth.collectAsState()
    val noiseGateMidpoint by viewModel.noiseGateMidpoint.collectAsState()

    val noteNamesArray = stringArrayResource(id = R.array.note_names_sharps)
    val displayNoteName = if (tuningState.noteIndex == -1) {
        "--"
    } else {
        noteNamesArray[tuningState.noteIndex]
    }

    val isPitchDetected = tuningState.noteIndex != -1

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val containerModifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .systemBarsPadding()
        .padding(16.dp)

    Box(modifier = containerModifier) {
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TunerDial(
                    cents = tuningState.deviationInCents,
                    waveformData = waveformData,
                    isPitchDetected = isPitchDetected,
                    isPitchLocked = isPitchLocked,
                    needleBaseWidth = needleWidth,
                    modifier = Modifier.weight(1f)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PitchTextPanel(
                        noteName = displayNoteName,
                        octave = tuningState.octave,
                        frequency = tuningState.currentFrequency
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    NoiseGateControl(
                        threshold = noiseGateThreshold,
                        midpoint = noiseGateMidpoint,
                        onThresholdChange = { viewModel.setNoiseGateThreshold(it) }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TunerDial(
                    cents = tuningState.deviationInCents,
                    waveformData = waveformData,
                    isPitchDetected = isPitchDetected,
                    isPitchLocked = isPitchLocked,
                    needleBaseWidth = needleWidth,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                PitchTextPanel(
                    noteName = displayNoteName,
                    octave = tuningState.octave,
                    frequency = tuningState.currentFrequency
                )

                Spacer(modifier = Modifier.height(48.dp))

                NoiseGateControl(
                    threshold = noiseGateThreshold,
                    midpoint = noiseGateMidpoint,
                    onThresholdChange = { viewModel.setNoiseGateThreshold(it) }
                )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToAbout) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(id = R.string.about_title),
                    tint = Color.LightGray
                )
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(id = R.string.settings_title),
                    tint = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun NoiseGateControl(
    threshold: Float,
    midpoint: Float,
    onThresholdChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxThreshold = midpoint * 2f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.noise_gate_label),
            color = Color.LightGray,
            fontSize = 14.sp
        )

        Slider(
            value = threshold,
            onValueChange = onThresholdChange,
            valueRange = 0.0f..maxThreshold,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.noise_gate_min),
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                text = stringResource(id = R.string.noise_gate_max),
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}