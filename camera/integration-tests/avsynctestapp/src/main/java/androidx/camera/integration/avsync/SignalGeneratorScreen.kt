/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.avsync

import androidx.camera.integration.avsync.ui.theme.LightOff
import androidx.camera.integration.avsync.ui.theme.LightOn
import androidx.camera.integration.avsync.ui.widget.AdvancedFloatingActionButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SignalGeneratorScreen(
    beepFrequency: Int,
    beepEnabled: Boolean,
    viewModel: SignalGeneratorViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(true) {
        viewModel.initialRecorder(context, lifecycleOwner)
        viewModel.initialSignalGenerator(context, beepFrequency, beepEnabled)
    }

    MainContent(
        isGeneratorReady = viewModel.isGeneratorReady,
        isRecorderReady = viewModel.isRecorderReady,
        isSignalActive = viewModel.isActivePeriod,
        isSignalStarted = viewModel.isSignalGenerating,
        isRecording = viewModel.isRecording,
        isPaused = viewModel.isPaused,
        onSignalStartClick = { viewModel.startSignalGeneration() },
        onSignalStopClick = { viewModel.stopSignalGeneration() },
        onRecordingStartClick = { viewModel.startRecording(context) },
        onRecordingStopClick = { viewModel.stopRecording() },
        onRecordingPauseClick = { viewModel.pauseRecording() },
        onRecordingResumeClick = { viewModel.resumeRecording() },
    )
}

@Composable
private fun MainContent(
    isGeneratorReady: Boolean = false,
    isRecorderReady: Boolean = false,
    isSignalActive: Boolean = false,
    isSignalStarted: Boolean = false,
    isRecording: Boolean = false,
    isPaused: Boolean = false,
    onSignalStartClick: () -> Unit = {},
    onSignalStopClick: () -> Unit = {},
    onRecordingStartClick: () -> Unit = {},
    onRecordingStopClick: () -> Unit = {},
    onRecordingPauseClick: () -> Unit = {},
    onRecordingResumeClick: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LightingScreen(isOn = isSignalActive)
        ControlPanel {
            SignalControl(
                enabled = isGeneratorReady,
                isStarted = isSignalStarted,
                onStartClick = onSignalStartClick,
                onStopClick = onSignalStopClick,
            )
            RecordingControl(
                enabled = isRecorderReady,
                isStarted = isRecording,
                isPaused = isPaused,
                onStartClick = onRecordingStartClick,
                onStopClick = onRecordingStopClick,
                onPauseClick = onRecordingPauseClick,
                onResumeClick = onRecordingResumeClick,
            )
        }
    }
}

@Composable
private fun LightingScreen(modifier: Modifier = Modifier, isOn: Boolean = false) {
    val backgroundColor = if (isOn) LightOn else LightOff
    Box(
        modifier = modifier.fillMaxSize().background(color = backgroundColor)
    )
}

@Composable
private fun ControlPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.weight(2f))
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun SignalControl(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    isStarted: Boolean,
    onStartClick: () -> Unit = {},
    onStopClick: () -> Unit = {},
) {
    val icon = if (isStarted) Icons.Filled.Close else Icons.Filled.PlayArrow

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AdvancedFloatingActionButton(
            enabled = enabled,
            onClick = if (isStarted) onStopClick else onStartClick,
            backgroundColor = Color.Cyan
        ) {
            Icon(icon, stringResource(R.string.desc_signal_control))
        }
    }
}

@Composable
private fun RecordingControl(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    isStarted: Boolean,
    isPaused: Boolean = false,
    onStartClick: () -> Unit = {},
    onStopClick: () -> Unit = {},
    onPauseClick: () -> Unit = {},
    onResumeClick: () -> Unit = {},
) {
    val startStopIconRes = if (isStarted) R.drawable.ic_stop else R.drawable.ic_record
    val pauseResumeIconRes = if (isPaused) R.drawable.ic_record else R.drawable.ic_pause

    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            AdvancedFloatingActionButton(
                enabled = enabled,
                onClick = if (isStarted) onStopClick else onStartClick,
                backgroundColor = Color.Cyan
            ) {
                Icon(
                    painterResource(startStopIconRes),
                    stringResource(R.string.desc_recording_control),
                    modifier.size(16.dp)
                )
            }
        }
        Box(
            modifier = modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            if (enabled && isStarted) {
                AdvancedFloatingActionButton(
                    enabled = true,
                    onClick = if (isPaused) onResumeClick else onPauseClick,
                    backgroundColor = Color.Cyan
                ) {
                    Icon(
                        painterResource(pauseResumeIconRes),
                        stringResource(R.string.desc_pause_control),
                        modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_SignalGeneratorPage() {
    MainContent()
}
