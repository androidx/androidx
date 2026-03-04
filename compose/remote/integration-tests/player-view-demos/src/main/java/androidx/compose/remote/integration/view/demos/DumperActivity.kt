/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class DumperActivity : ComponentActivity() {
    @Suppress("RestrictedApiAndroidX")
    override fun onCreate(savedInstanceState: Bundle?) {
        @OptIn(androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi::class)
        RemoteComposeCreationComposeFlags.isRemoteApplierEnabled = false
        super.onCreate(savedInstanceState)

        val initialMode = RenderMode.fromString(intent.getStringExtra("mode"))

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                    color = Color.DarkGray,
                ) {
                    DumperScreen(this, initialMode)
                }
            }
        }
    }
}

@Composable
fun DumperScreen(context: Context, initialMode: RenderMode) {
    var renderMode by remember { mutableStateOf(initialMode) }
    var resolution by remember { mutableStateOf(Resolution.RES_480X480) }
    var duration by remember { mutableStateOf(Duration.SEC_30) }
    var fps by remember { mutableStateOf(Fps.FPS_30) }
    var bitrate by remember { mutableStateOf(Bitrate.BITRATE_200K) }
    var selectedSample by remember { mutableStateOf(AllSamples.first()) }
    var isRunning by remember { mutableStateOf(false) }
    var outputInfo by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text("Remote Compose Dumper Tool", color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        if (outputInfo.isNotEmpty()) {
            Text(
                text = "Output: $outputInfo",
                modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(8.dp),
                color = Color.Black,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isRunning) {
            DumperControlPanel(
                renderMode = renderMode,
                onModeChange = { renderMode = it },
                resolution = resolution,
                onResolutionChange = { resolution = it },
                duration = duration,
                onDurationChange = { duration = it },
                fps = fps,
                onFpsChange = { fps = it },
                bitrate = bitrate,
                onBitrateChange = { bitrate = it },
                samples = AllSamples,
                selectedSample = selectedSample,
                onSampleChange = { selectedSample = it },
                onStartClick = {
                    isRunning = true
                    outputInfo = ""
                },
            )
        } else {
            DumperPreviewSection(
                context = context,
                renderMode = renderMode,
                selectedSample = selectedSample,
                resolution = resolution,
                duration = duration,
                fps = fps,
                bitrate = bitrate,
                onOutputReady = {
                    outputInfo = it
                    isRunning = false
                },
            )
        }
    }
}

@Composable
fun DumperControlPanel(
    renderMode: RenderMode,
    onModeChange: (RenderMode) -> Unit,
    resolution: Resolution,
    onResolutionChange: (Resolution) -> Unit,
    duration: Duration,
    onDurationChange: (Duration) -> Unit,
    fps: Fps,
    onFpsChange: (Fps) -> Unit,
    bitrate: Bitrate,
    onBitrateChange: (Bitrate) -> Unit,
    samples: List<DumperSample>,
    selectedSample: DumperSample,
    onSampleChange: (DumperSample) -> Unit,
    onStartClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RenderModeSelector(renderMode, onModeChange)
        ResolutionSelector(resolution, onResolutionChange)
        DurationSelector(duration, onDurationChange)
        FpsSelector(fps, onFpsChange)
        BitrateSelector(bitrate, onBitrateChange)
        SampleSelector(samples, selectedSample.name, onSampleChange)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onStartClick, modifier = Modifier.fillMaxWidth()) { Text("Start") }
    }
}

@Composable
fun DumperPreviewSection(
    context: Context,
    renderMode: RenderMode,
    selectedSample: DumperSample,
    resolution: Resolution,
    duration: Duration,
    fps: Fps,
    bitrate: Bitrate,
    onOutputReady: (String) -> Unit,
) {
    Box(modifier = Modifier.background(Color.Black).padding(4.dp)) {
        when (renderMode) {
            RenderMode.REMOTE -> {
                RemoteComposeDumper(
                    sample = selectedSample,
                    width = resolution.width,
                    height = resolution.height,
                    onFinished = onOutputReady,
                )
            }
            RenderMode.REMOTE_VIDEO_ENCODE -> {
                val result =
                    mediaH264Preview(
                        context = context,
                        sample = selectedSample,
                        width = resolution.width,
                        height = resolution.height,
                        durationMillis = duration.millis,
                        fps = fps.value,
                        bitrate = bitrate.bps,
                    )
                LaunchedEffect(result) { result?.let { onOutputReady(it.filePath) } }
            }
        }
    }
}
