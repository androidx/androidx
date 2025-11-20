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

import android.media.MediaPlayer
import android.net.Uri
import androidx.annotation.Sampled
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.onPointSourceParamsAvailable
import androidx.xr.compose.subspace.layout.size
import androidx.xr.scenecore.SpatialMediaPlayer

@Sampled
public fun OnPointSourceParamsAvailableSample() {

    @Composable
    @SubspaceComposable
    fun MediaPlayerInSpatialPanel(mediaUri: Uri) {
        val session = LocalSession.current
        val context = LocalContext.current
        val mediaPlayer = remember { MediaPlayer() }
        val paramsSet = remember { mutableStateOf(false) }

        SpatialPanel(
            SubspaceModifier.size(400.dp).onPointSourceParamsAvailable {
                if (!paramsSet.value) {
                    paramsSet.value = true
                    mediaPlayer.setDataSource(context, mediaUri)
                    SpatialMediaPlayer.setPointSourceParams(session!!, mediaPlayer, it)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                }
            },
            dragPolicy = MovePolicy(),
        ) {
            DisposableEffect(Unit) { onDispose { mediaPlayer.release() } }

            // Use this for playing video, or omit it for audio only use cases.
            AndroidExternalSurface {
                onSurface { surface, _, _ -> mediaPlayer.setSurface(surface) }
            }
        }
    }
}
