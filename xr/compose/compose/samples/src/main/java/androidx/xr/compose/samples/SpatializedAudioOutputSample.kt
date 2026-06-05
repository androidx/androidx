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

package androidx.xr.compose.samples

import androidx.annotation.Sampled
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.transformingMovable
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.media.SoundFieldExoplayerAudioOutput
import androidx.xr.compose.subspace.media.asSpatializedAudioOutput
import androidx.xr.compose.subspace.media.rememberPointSourceExoplayerAudioOutput
import androidx.xr.compose.subspace.media.spatializedAudioOutput
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.SoundEffectPool
import androidx.xr.scenecore.SoundEffectPoolComponent
import androidx.xr.scenecore.SoundFieldAttributes
import androidx.xr.scenecore.SpatializerConstants

/** A sample demonstrating a point source spatialized audio output with ExoPlayer. */
@Sampled
@Composable
fun SpatializedAudioOutputSample() {
    val context = LocalContext.current

    // Create a PointSourceExoplayerAudioOutput.
    val audioOutput = rememberPointSourceExoplayerAudioOutput(PointSourceParams())

    // Create an ExoPlayer and set its AudioOutputProvider.
    val exoPlayer = remember {
        ExoPlayer.Builder(context).setAudioOutputProvider(audioOutput.audioOutputProvider).build()
    }

    DisposableEffect(Unit) {
        exoPlayer.setMediaItem(MediaItem.fromUri("asset:///audio.mp3"))
        exoPlayer.prepare()
        exoPlayer.play()

        onDispose { exoPlayer.release() }
    }

    // Attach the SpatializedAudioOutput via SubspaceModifier.spatializedAudioOutput. The audio will
    // be spatialized from the position of this SpatialPanel, and will follow the Panel as it moves.
    SpatialPanel(
        modifier =
            SubspaceModifier.width(600.dp)
                .height(400.dp)
                .spatializedAudioOutput(audioOutput)
                .transformingMovable()
    ) {
        // Content of the panel
    }
}

/** A sample demonstrating a sound field spatialized audio output with ExoPlayer. */
@Sampled
@Composable
fun SoundFieldSpatializedAudioOutputSample() {
    val context = LocalContext.current
    val session = LocalSession.current ?: return

    // Create a SoundFieldExoplayerAudioOutput.
    val audioOutput = remember {
        SoundFieldExoplayerAudioOutput(
            session,
            SoundFieldAttributes(SpatializerConstants.AmbisonicsOrder.FIRST_ORDER),
        )
    }

    // Create an ExoPlayer and set its AudioOutputProvider.
    val exoPlayer = remember {
        ExoPlayer.Builder(context).setAudioOutputProvider(audioOutput.audioOutputProvider).build()
    }

    DisposableEffect(Unit) {
        exoPlayer.setMediaItem(MediaItem.fromUri("asset:///ambient.mp3"))
        exoPlayer.prepare()
        exoPlayer.play()

        onDispose { exoPlayer.release() }
    }

    // Attach the SpatializedAudioOutput via SubspaceModifier.spatializedAudioOutput.
    SpatialPanel(
        modifier =
            SubspaceModifier.width(600.dp)
                .height(400.dp)
                .spatializedAudioOutput(audioOutput)
                .transformingMovable()
    ) {
        // Content of the panel
    }
}

/** A sample demonstrating a spatialized sound effect player. */
@Sampled
@Composable
fun SoundEffectPlayerSample() {
    val context = LocalContext.current
    val session = LocalSession.current ?: return

    // Create a SoundEffectPool.
    val soundEffectPool = remember { SoundEffectPool.create(session, maxStreams = 10) }

    // Remember a sound effect loaded via AssetFileDescriptor.
    val soundEffect = remember {
        val assetFileDescriptor = context.assets.openFd("click.wav")
        soundEffectPool.load(assetFileDescriptor)
    }

    // Create a SoundEffectPoolComponent.
    val soundEffectPoolComponent =
        remember(session, soundEffectPool) {
            SoundEffectPoolComponent.create(session, soundEffectPool, PointSourceParams())
        }

    // Attach the SpatializedAudioOutput via SubspaceModifier.spatializedAudioOutput.
    SpatialPanel(
        modifier =
            SubspaceModifier.width(600.dp)
                .height(400.dp)
                .spatializedAudioOutput(soundEffectPoolComponent.asSpatializedAudioOutput())
                .transformingMovable()
    ) {
        Button(
            onClick = {
                soundEffectPoolComponent.play(
                    soundEffect,
                    volume = 1f,
                    priority = 0,
                    isLooping = false,
                )
            }
        ) {
            Text("Play Sound Effect")
        }
    }
}
