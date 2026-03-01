package com.masterofpuppets.pitchapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterofpuppets.pitchapp.R
import com.masterofpuppets.pitchapp.model.Transposition
import com.masterofpuppets.pitchapp.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val referenceA4 by viewModel.referenceA4.collectAsState()
    val transposition by viewModel.transposition.collectAsState()
    val noiseGateMidpoint by viewModel.noiseGateMidpoint.collectAsState()
    val yinTolerance by viewModel.yinTolerance.collectAsState()
    val needleWidth by viewModel.needleBaseWidth.collectAsState()
    val tuningSoundVolume by viewModel.tuningSoundVolume.collectAsState()

    val scrollState = rememberScrollState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetNoiseGateSliderToMidpoint()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.action_back),
                    tint = Color.White
                )
            }
            Text(
                text = stringResource(id = R.string.settings_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Settings Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Reference A4 Calibration
            SettingSliderItem(
                label = stringResource(id = R.string.settings_calibration_label),
                valueText = String.format(stringResource(id = R.string.settings_calibration_value), referenceA4),
                value = referenceA4,
                valueRange = 430f..450f,
                onValueChange = { viewModel.updateReferenceA4Setting(it) }
            )

            // 2. Transposition Dropdown
            TranspositionSettingItem(
                currentTransposition = transposition,
                onTranspositionSelected = { viewModel.updateTranspositionSetting(it) }
            )

            // 3. Noise Gate Midpoint
            SettingSliderItem(
                label = stringResource(id = R.string.settings_noise_gate_midpoint_label),
                description = stringResource(id = R.string.settings_noise_gate_midpoint_desc),
                valueText = String.format("%.0f", noiseGateMidpoint * 1000f),
                value = noiseGateMidpoint * 1000f,
                valueRange = 1f..10f,
                steps = 8,
                onValueChange = { viewModel.updateNoiseGateMidpointSetting(it / 1000f) }
            )

            // 4. YIN Tolerance
            SettingSliderItem(
                label = stringResource(id = R.string.settings_yin_tolerance_label),
                description = stringResource(id = R.string.settings_yin_tolerance_desc),
                valueText = String.format("%.2f", yinTolerance),
                value = yinTolerance,
                valueRange = 0.05f..0.30f,
                onValueChange = { viewModel.updateYinToleranceSetting(it) }
            )

            // 5. Needle Width
            SettingSliderItem(
                label = stringResource(id = R.string.settings_needle_width_label),
                valueText = String.format("%.0f", needleWidth),
                value = needleWidth,
                valueRange = 10f..80f,
                onValueChange = { viewModel.updateNeedleBaseWidthSetting(it) }
            )

            // 6. Tuning Sound Volume (0.0 a 1.0)
            SettingSliderItem(
                label = stringResource(id = R.string.settings_tuning_sound_label),
                description = stringResource(id = R.string.settings_tuning_sound_desc),
                valueText = String.format(stringResource(id = R.string.settings_tuning_sound_value), tuningSoundVolume * 100f),
                value = tuningSoundVolume,
                valueRange = 0f..1f,
                onValueChange = { viewModel.updateTuningSoundVolumeSetting(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 7. Factory Reset Button
            Button(
                onClick = { viewModel.resetToFactoryDefaults() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.settings_factory_reset),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SettingSliderItem(
    label: String,
    description: String? = null,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.White, fontSize = 16.sp)
            Text(text = valueText, color = Color.Cyan, fontSize = 16.sp)
        }

        if (description != null) {
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TranspositionSettingItem(
    currentTransposition: Transposition,
    onTranspositionSelected: (Transposition) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val getTranspositionName = { trans: Transposition ->
        val resId = context.resources.getIdentifier(trans.nameKey, "string", context.packageName)
        if (resId != 0) context.getString(resId) else trans.name
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.settings_transposition_label),
            color = Color.White,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = getTranspositionName(currentTransposition),
                color = Color.Cyan,
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .background(Color.DarkGray.copy(alpha = 0.5f))
                    .padding(16.dp)
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.DarkGray)
            ) {
                Transposition.values().forEach { transposition ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = getTranspositionName(transposition),
                                color = Color.White
                            )
                        },
                        onClick = {
                            onTranspositionSelected(transposition)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}