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

package androidx.xr.compose.testapp.mediaplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.testapp.ui.components.CommonTestPanel
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.unit.DpVolumeSize

class MediaPlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { IntegrationTestsAppTheme { TestVideoPlayer() } }
    }

    @Composable
    private fun TestVideoPlayer() {
        Subspace {
            SpatialColumn {
                CommonTestPanel(
                    size = DpVolumeSize(640.dp, 480.dp, 0.dp),
                    showBottomBar = true,
                    title = "Media Player Test",
                    onClickBackArrow = { this@MediaPlayerActivity.finish() },
                    onClickRecreate = { this@MediaPlayerActivity.recreate() },
                ) { padding ->
                    Column(modifier = Modifier.padding(padding)) { VideoPlayerXR() }
                }
            }
        }
    }

    @Composable
    private fun VideoPlayerXR() {
        val context = LocalContext.current
        val videoUri = "file:///android_asset/videos/sample_video.mp4"
        val mediaItem = MediaItem.fromUri(videoUri)
        val player = ExoPlayer.Builder(context).build()
        player.setMediaItem(mediaItem)
        val playerView = PlayerView(context)
        playerView.player = player

        LaunchedEffect(player) {
            player.prepare()
            player.play()
        }

        DisposableEffect(player) { onDispose { player.release() } }

        AndroidView(modifier = Modifier.fillMaxSize(), factory = { playerView })
    }
}
