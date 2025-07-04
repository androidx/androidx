/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.integration.mediaplayerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.UiComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.width

class MediaPlayerApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Subspace {
                SpatialPanel(
                    modifier = SubspaceModifier.width(600.dp).height(600.dp).movable(enabled = true)
                ) {
                    MediaPlayer()
                }
            }
        }
    }

    @UiComposable
    @Composable
    fun MediaPlayer() {
        val context = LocalContext.current

        val exoPlayer by remember { mutableStateOf(ExoPlayer.Builder(context).build()) }
        // file:///android_asset/ resolves to the assets/ directory.
        // https://developer.android.com/reference/android/webkit/WebViewClient
        val media by remember {
            mutableStateOf(MediaItem.fromUri("file:///android_asset/videos/sample_video.mp4"))
        }

        LaunchedEffect(media) {
            exoPlayer.setMediaItem(media)
            exoPlayer.prepare()
        }

        DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

        AndroidView(factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer } })
    }
}
