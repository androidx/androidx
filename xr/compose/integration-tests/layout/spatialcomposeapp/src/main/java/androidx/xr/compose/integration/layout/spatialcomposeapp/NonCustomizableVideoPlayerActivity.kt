/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.integration.layout.spatialcomposeapp

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.DrmConfiguration
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialExternalSurface
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.StereoMode
import androidx.xr.compose.subspace.SurfaceProtection
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.width
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.scene
import java.io.File

/**
 * For quickly playing a video in SpatialExternalSurface, without configurations. Requires adb
 * pushing video assets with matching file paths.
 */
class NonCustomizableVideoPlayerActivity : ComponentActivity() {
    private val TAG = "NonCustomizableVideoPlayerActivity"
    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }
    private val useDrmState = mutableStateOf(true)

    private val drmLicenseUrl = "https://proxy.uat.widevine.com/proxy?provider=widevine_test"
    private val drmVideoUri =
        Environment.getExternalStorageDirectory().path + "/Download/sdr_singleview_protected.mp4"
    private val regularVideoUri =
        Environment.getExternalStorageDirectory().path + "/Download/vid_bigbuckbunny.mp4"
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f

        checkFile(drmVideoUri)
        checkFile(regularVideoUri)

        setContent { Subspace { VideoInSpatialExternalSurface(stereoMode = StereoMode.Mono) } }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @OptIn(ExperimentalComposeApi::class)
    @Composable
    fun VideoInSpatialExternalSurface(stereoMode: StereoMode) {
        var videoWidth by remember { mutableStateOf(600.dp) }
        var videoHeight by remember { mutableStateOf(600.dp) }

        SpatialExternalSurface(
            modifier =
                SubspaceModifier.width(
                        if (stereoMode == StereoMode.SideBySide) videoWidth / 2 else videoWidth
                    )
                    .height(
                        if (stereoMode == StereoMode.TopBottom) videoHeight / 2 else videoHeight
                    )
                    .movable()
                    .resizable(),
            stereoMode = stereoMode,
            surfaceProtection =
                if (useDrmState.value) SurfaceProtection.Protected else SurfaceProtection.None,
        ) {
            onSurfaceCreated {
                val player = ExoPlayer.Builder(this@NonCustomizableVideoPlayerActivity).build()
                exoPlayer = player
                player.setVideoSurface(it)
                player.setMediaItem(getMediaItem())
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.addListener(
                    object : Player.Listener {
                        override fun onVideoSizeChanged(videoSize: VideoSize) {
                            val width = videoSize.width
                            val height = videoSize.height
                            if (height > 0 && width > 0) {
                                videoHeight = videoWidth * height / width
                            }
                        }
                    }
                )

                player.playWhenReady = true
                player.prepare()
            }

            onSurfaceDestroyed {
                exoPlayer?.release()
                exoPlayer = null
            }

            SpatialBox(
                modifier = SubspaceModifier.fillMaxSize(),
                alignment = SpatialAlignment.TopRight,
            ) {
                SpatialPanel(SubspaceModifier.offset(z = 30.dp)) {
                    Button(onClick = { finish() }) { Text("Close") }
                }
            }

            Orbiter(position = ContentEdge.Bottom, offset = 48.dp) {
                Button(onClick = { useDrmState.value = !useDrmState.value }) {
                    Text(text = if (useDrmState.value) "Use non-drm video" else "Use drm video")
                }
            }
        }
    }

    private fun getMediaItem(): MediaItem {
        return if (useDrmState.value) {
            MediaItem.Builder()
                .setUri(drmVideoUri)
                .setDrmConfiguration(
                    DrmConfiguration.Builder(C.WIDEVINE_UUID).setLicenseUri(drmLicenseUrl).build()
                )
                .build()
        } else {
            MediaItem.fromUri(regularVideoUri)
        }
    }

    private fun checkFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "$filePath does not exist. Did you adb push the asset?")
            Toast.makeText(
                    this@NonCustomizableVideoPlayerActivity,
                    "$filePath does not exist. Did you adb push the asset?",
                    Toast.LENGTH_LONG,
                )
                .show()
            finish()
        }
    }
}
