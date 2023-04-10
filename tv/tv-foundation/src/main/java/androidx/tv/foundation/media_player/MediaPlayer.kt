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

import androidx.compose.runtime.Composable
import androidx.media3.exoplayer.ExoPlayer

internal interface MediaPlayer {
    @Composable
    fun PlayerView(): Unit
}

internal class MediaPlayerImpl(val exoPlayer: ExoPlayer) : MediaPlayer {
    @Composable
    override fun PlayerView() { TODO("Not yet implemented") }
}
