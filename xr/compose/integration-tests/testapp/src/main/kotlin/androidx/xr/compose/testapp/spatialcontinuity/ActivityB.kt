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

package androidx.xr.compose.testapp.spatialcontinuity

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.ExperimentalSpatialContinuityApi
import androidx.xr.compose.subspace.layout.SpatialContinuityCapturer
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.spatialContinuityTarget
import androidx.xr.compose.subspace.layout.width

@OptIn(ExperimentalSpatialContinuityApi::class)
@SuppressLint("RestrictTo", "RestrictedApiAndroidX")
class ActivityB : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        setContent { MainContent() }
    }

    @Composable
    private fun MainContent() {
        val session = LocalSession.current
        val spatialContinuityCapturer = remember { SpatialContinuityCapturer() }
        Subspace {
            SpatialPanel(
                SubspaceModifier.height(500.dp)
                    .width(400.dp)
                    .offset(z = 100.dp)
                    .spatialContinuityTarget(spatialContinuityCapturer = spatialContinuityCapturer)
                    .movable()
            ) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(fontSize = 28.sp, text = "Activity B")
                        VideoPlayerXR()
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { finish() }) { Text("<< Back") }
                        val intent = Intent(LocalContext.current, ActivityC::class.java)
                        Button(onClick = { startActivity(intent) }) { Text("Launch Activity C") }
                        Button(onClick = { spatialContinuityCapturer.captureContinuity() }) {
                            Text("Set Continuity")
                        }
                        Button(
                            onClick = {
                                session?.let {
                                    SpatialContinuityCapturer.resetContinuity(it)
                                }
                            }
                        ) {
                            Text("Reset Continuity")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun VideoPlayerXR() {
        val context = LocalContext.current
        val videoUri = "file:///android_asset/videos/sample_video.mp4"
        val player = remember {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(videoUri)
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ALL
                prepare()
                play()
            }
        }
        val playerView = remember {
            PlayerView(context).apply {
                this.player = player
                useController = false
            }
        }
        DisposableEffect(player) { onDispose { player.release() } }
        AndroidView(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            factory = { playerView },
        )
    }
}

@OptIn(ExperimentalSpatialContinuityApi::class)
class ActivityC : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        setContent { MainContent() }
    }

    @Composable
    private fun MainContent() {
        val session = LocalSession.current
        val spatialContinuityCapturer = remember { SpatialContinuityCapturer() }
        Subspace {
            SpatialPanel(
                SubspaceModifier.height(400.dp)
                    .width(400.dp)
                    .spatialContinuityTarget(spatialContinuityCapturer = spatialContinuityCapturer)
                    .movable()
            ) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(fontSize = 28.sp, text = "Activity C")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { finish() }) { Text("<< Back") }
                        Button(onClick = { spatialContinuityCapturer.captureContinuity() }) {
                            Text("Set Continuity")
                        }
                        Button(
                            onClick = {
                                session?.let {
                                    SpatialContinuityCapturer.resetContinuity(it)
                                }
                            }
                        ) {
                            Text("Reset Continuity")
                        }
                    }
                }
            }
        }
    }
}
