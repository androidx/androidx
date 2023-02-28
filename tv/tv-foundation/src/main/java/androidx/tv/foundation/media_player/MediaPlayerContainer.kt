/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.foundation.media_player

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.exoplayer.ExoPlayer

@Composable
internal fun MediaPlayerContainer(
    modifier: Modifier = Modifier,
    background: @Composable (mediaPlayer: MediaPlayer) -> Unit,
    mediaPlayer: MediaPlayer = MediaPlayerContainerDefaults.exoPlayer,
    mediaPlayerOverlay: @Composable (mediaPlayer: MediaPlayer) -> Unit
) {
    Box(modifier = modifier) {
        background(mediaPlayer)
        mediaPlayer.PlayerView()
        mediaPlayerOverlay(mediaPlayer)
    }
}

internal object MediaPlayerContainerDefaults {
    internal val exoPlayer
        @Composable get() = kotlin.run {
            val context = LocalContext.current
            remember { MediaPlayerImpl(ExoPlayer.Builder(context).build()) }
        }
}
