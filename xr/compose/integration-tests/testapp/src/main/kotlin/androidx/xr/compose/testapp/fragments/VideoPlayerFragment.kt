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

package androidx.xr.compose.testapp.fragments

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialExternalSurface
import androidx.xr.compose.subspace.SpatialExternalSurfaceProtection
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.StereoMode
import androidx.xr.compose.subspace.layout.InteractionPolicy
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.common.isDrmSupported

/** A Fragment using spatial UI. */
class VideoPlayerFragment : Fragment() {

    private val useDrmState = mutableStateOf(false)

    private val drmLicenseUrl = "https://proxy.uat.widevine.com/proxy?provider=widevine_test"
    private val drmVideoUri =
        Environment.getExternalStorageDirectory().path + "/Download/sdr_singleview_protected.mp4"
    private val regularVideoUri =
        Environment.getExternalStorageDirectory().path + "/Download/vid_bigbuckbunny.mp4"
    private var exoPlayer: ExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Create a ComposeView, which is the bridge between the View system and Compose.
        return ComposeView(requireContext()).apply {

            // This strategy handles disposing the Composition when the Fragment's
            // View lifecycle is destroyed, preventing memory leaks.
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

            // Set the Compose content for this Fragment.
            setContent {
                MaterialTheme { Subspace { VideoInSpatialExternalSurface(StereoMode.Mono) } }
            }
        }
    }

    @Composable
    private fun VideoInSpatialExternalSurface(stereoMode: StereoMode) {
        var videoWidth by remember { mutableStateOf(600.dp) }
        var videoHeight by remember { mutableStateOf(600.dp) }
        val isDrmSupported = remember { isDrmSupported() }
        SpatialExternalSurface(
            modifier =
                SubspaceModifier.width(
                        if (stereoMode == StereoMode.SideBySide) videoWidth / 2 else videoWidth
                    )
                    .height(
                        if (stereoMode == StereoMode.TopBottom) videoHeight / 2 else videoHeight
                    )
                    .movable(),
            resizePolicy = ResizePolicy(),
            interactionPolicy =
                InteractionPolicy.clickable {
                    exoPlayer?.let {
                        if (it.isPlaying) {
                            it.pause()
                        } else {
                            it.play()
                        }
                    }
                },
            stereoMode = stereoMode,
            surfaceProtection =
                if (useDrmState.value) SpatialExternalSurfaceProtection.Protected
                else SpatialExternalSurfaceProtection.None,
        ) {
            onSurfaceCreated {
                val player = ExoPlayer.Builder(requireActivity()).build()
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
                alignment = SpatialAlignment.TopEnd,
            ) {
                SpatialPanel(SubspaceModifier.offset(z = 30.dp)) {
                    Column {
                        Button(onClick = { parentFragmentManager.popBackStack() }) { Text("Back") }
                        Button(onClick = { requireActivity().finish() }) { Text("Close") }
                    }
                }
            }

            Orbiter(position = ContentEdge.Bottom, offset = 48.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { useDrmState.value = !useDrmState.value }) {
                        Text(text = if (useDrmState.value) "Use non-drm video" else "Use drm video")
                    }
                    if (!isDrmSupported) {
                        Text(
                            text = "DRM is not supported on this device",
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        }
    }

    private fun getMediaItem(): MediaItem {
        return if (useDrmState.value) {
            MediaItem.Builder()
                .setUri(drmVideoUri)
                .setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                        .setLicenseUri(drmLicenseUrl)
                        .build()
                )
                .build()
        } else {
            MediaItem.fromUri(regularVideoUri)
        }
    }
}
