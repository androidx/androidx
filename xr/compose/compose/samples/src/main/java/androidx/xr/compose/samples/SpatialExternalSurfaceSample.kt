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

package androidx.xr.compose.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.xr.compose.subspace.SpatialExternalSurface
import androidx.xr.compose.subspace.StereoMode

@Sampled
@Composable
fun SpatialExternalSurfaceSample() {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    SpatialExternalSurface(stereoMode = StereoMode.SideBySide) {
        onSurfaceCreated { surface ->
            exoPlayer.apply {
                setVideoSurface(surface)
                setMediaItem(MediaItem.fromUri("asset:///video.mp4"))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                play()
            }
        }

        onSurfaceDestroyed { exoPlayer.release() }
    }
}
