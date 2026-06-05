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

package androidx.xr.compose.testapp.spatialaudio

import android.content.res.AssetFileDescriptor
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.transformingMovable
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.media.PointSourceExoplayerAudioOutput
import androidx.xr.compose.subspace.media.SoundFieldExoplayerAudioOutput
import androidx.xr.compose.subspace.media.asSpatializedAudioOutput
import androidx.xr.compose.subspace.media.spatializedAudioOutput
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.SoundEffect
import androidx.xr.scenecore.SoundEffectPool
import androidx.xr.scenecore.SoundEffectPoolComponent
import androidx.xr.scenecore.SoundFieldAttributes
import androidx.xr.scenecore.SpatializerConstants.AmbisonicsOrder
import androidx.xr.scenecore.Stream
import java.io.File

class SpatialAudioActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Subspace { SpatialAudioTestApp() } }
    }

    @Composable
    private fun SpatialAudioTestApp() {
        val session = checkNotNull(LocalSession.current) { "LocalSession must be available." }
        val context = LocalContext.current
        var showSharedAllPanel by remember { mutableStateOf(false) }

        // File paths
        val tigerPath = Environment.getExternalStorageDirectory().path + "/Download/tiger_16db.mp3"
        val ambisonicPath =
            Environment.getExternalStorageDirectory().path + "/Download/foa_basketball_16bit.wav"
        val tigerExists = remember { File(tigerPath).exists() }
        val ambisonicExists = remember { File(ambisonicPath).exists() }

        // 1. Point Source Audio Output
        val pointSourceOutput = remember {
            PointSourceExoplayerAudioOutput(session, PointSourceParams())
        }
        val pointSourcePlayer = remember {
            ExoPlayer.Builder(context)
                .setAudioOutputProvider(pointSourceOutput.audioOutputProvider)
                .build()
        }

        // 2. Sound Field Audio Output
        val soundFieldOutput = remember {
            SoundFieldExoplayerAudioOutput(
                session,
                SoundFieldAttributes(AmbisonicsOrder.FIRST_ORDER),
            )
        }
        val soundFieldPlayer = remember {
            ExoPlayer.Builder(context)
                .setAudioOutputProvider(soundFieldOutput.audioOutputProvider)
                .build()
        }

        // 3. Sound Effect Player
        val soundEffectPool = remember { SoundEffectPool.create(session, 10) }
        val soundEffectPlayer =
            remember(session, soundEffectPool) {
                SoundEffectPoolComponent.create(session, soundEffectPool, PointSourceParams())
            }
        val soundEffectAudioOutput =
            remember(soundEffectPlayer) { soundEffectPlayer.asSpatializedAudioOutput() }
        var loadedSoundEffect by remember { mutableStateOf<SoundEffect?>(null) }
        var currentStream by remember { mutableStateOf<Stream?>(null) }

        LaunchedEffect(soundEffectPool) {
            val tigerFile = File(tigerPath)
            if (tigerFile.exists()) {
                val afd =
                    AssetFileDescriptor(
                        ParcelFileDescriptor.open(tigerFile, ParcelFileDescriptor.MODE_READ_ONLY),
                        0L,
                        tigerFile.length(),
                    )
                soundEffectPool.addLoadCompleteListener { effect, success ->
                    if (success) {
                        loadedSoundEffect = effect
                    }
                    Log.i("SpatialAudioActivity", "Loaded $effect, success: $success")
                }
                soundEffectPool.load(afd)
            }
        }

        SpatialPanel(
            modifier = SubspaceModifier.width(600.dp).height(700.dp).transformingMovable()
        ) {
            CommonTestScaffold(
                title = "Spatial Audio Tests",
                showBottomBar = false,
                onClickBackArrow = { finish() },
            ) { padding ->
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Spatial Audio Controls", style = MaterialTheme.typography.headlineSmall)

                    // Point Source Section
                    AudioSection(
                        title = "Point Source (ExoPlayer)",
                        enabled = tigerExists,
                        onPlay = {
                            pointSourcePlayer.setMediaItem(MediaItem.fromUri(tigerPath))
                            pointSourcePlayer.prepare()
                            pointSourcePlayer.play()
                        },
                        onStop = { pointSourcePlayer.stop() },
                    )

                    // Sound Field Section
                    AudioSection(
                        title = "Sound Field (Ambisonics)",
                        enabled = ambisonicExists,
                        onPlay = {
                            soundFieldPlayer.setMediaItem(MediaItem.fromUri(ambisonicPath))
                            soundFieldPlayer.prepare()
                            soundFieldPlayer.play()
                        },
                        onStop = { soundFieldPlayer.stop() },
                    )

                    // Sound Effect Section
                    AudioSection(
                        title = "Sound Effect (SoundPool)",
                        enabled = loadedSoundEffect != null,
                        onPlay = {
                            loadedSoundEffect?.let {
                                currentStream = soundEffectPlayer.play(it, 1.0f, 1, false)
                            }
                        },
                        onStop = {
                            currentStream?.let {
                                soundEffectPlayer.stop(it)
                                currentStream = null
                            }
                        },
                    )

                    // Shared Output Section
                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                                .background(Color.LightGray.copy(alpha = 0.2f))
                                .padding(8.dp)
                    ) {
                        Text("Shared Output Test", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "When the 'Shared All' panel is visible, all 3 audio sources above will become head-locked as they are each shared across two panels.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Show 'Shared All' Panel")
                            Switch(
                                checked = showSharedAllPanel,
                                onCheckedChange = { showSharedAllPanel = it },
                            )
                        }
                    }
                }
            }
        }

        // Point Source Panel
        SpatialPanel(
            modifier =
                SubspaceModifier.offset(x = 500.dp)
                    .width(200.dp)
                    .height(200.dp)
                    .spatializedAudioOutput(pointSourceOutput)
                    .transformingMovable()
        ) {
            Box(Modifier.fillMaxSize().background(Color.Red), contentAlignment = Alignment.Center) {
                Text("Point Source", color = Color.White)
            }
        }

        // Sound Field Panel
        SpatialPanel(
            modifier =
                SubspaceModifier.offset(x = (-500).dp)
                    .width(200.dp)
                    .height(200.dp)
                    .spatializedAudioOutput(soundFieldOutput)
                    .transformingMovable()
        ) {
            Box(
                Modifier.fillMaxSize().background(Color.Blue),
                contentAlignment = Alignment.Center,
            ) {
                Text("Sound Field", color = Color.White)
            }
        }

        // Sound Effect Panel
        SpatialPanel(
            modifier =
                SubspaceModifier.offset(y = 500.dp)
                    .width(200.dp)
                    .height(200.dp)
                    .spatializedAudioOutput(soundEffectAudioOutput)
                    .transformingMovable()
        ) {
            Box(
                Modifier.fillMaxSize().background(Color.Yellow),
                contentAlignment = Alignment.Center,
            ) {
                Text("Sound Effect", color = Color.Black)
            }
        }

        if (showSharedAllPanel) {
            // This panel 'steals' spatialization from the other 3 panels by sharing their outputs.
            SpatialPanel(
                modifier =
                    SubspaceModifier.offset(y = (-500).dp)
                        .width(200.dp)
                        .height(200.dp)
                        .spatializedAudioOutput(pointSourceOutput)
                        .spatializedAudioOutput(soundFieldOutput)
                        .spatializedAudioOutput(soundEffectAudioOutput)
                        .transformingMovable()
            ) {
                Box(
                    Modifier.fillMaxSize().background(Color.Magenta),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Shared All", color = Color.White)
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                pointSourcePlayer.release()
                soundFieldPlayer.release()
                soundEffectPool.release()
            }
        }
    }

    @Composable
    private fun AudioSection(
        title: String,
        enabled: Boolean = true,
        onPlay: () -> Unit,
        onStop: () -> Unit,
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha = 0.2f)).padding(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPlay, enabled = enabled) {
                    Text(if (enabled) "Play" else "Missing")
                }
                Button(onClick = onStop, enabled = enabled) { Text("Stop") }
            }
        }
    }
}
