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

package androidx.privacysandbox.ui.integration.sdkproviderutils

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.WeakHashMap

/** Create PlayerView with Player and controlling playback based on player viewability. */
class PlayerViewProvider {

    private val handler = Handler(Looper.getMainLooper())
    private val createdViews = WeakHashMap<PlayerView, PlayerWithState>()

    fun createPlayerView(windowContext: Context, videoUrl: String): View {
        val viewId = View.generateViewId()

        val view = PlayerView(windowContext)
        view.id = viewId

        val playerWithState = PlayerWithState(windowContext, videoUrl)

        handler.post {
            createdViews[view] = playerWithState
            val player = playerWithState.initializePlayer()
            view.setPlayer(player)
        }

        return view
    }

    inner class PlayerWithState(private val context: Context, private val videoUrl: String) {
        private var player: ExoPlayer? = null
        private var autoPlay = true
        private var autoPlayPosition: Long

        init {
            autoPlayPosition = C.TIME_UNSET
        }

        fun initializePlayer(): Player? {
            player?.let {
                return it
            }

            val audioAttributes =
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build()

            player = ExoPlayer.Builder(context).setAudioAttributes(audioAttributes, true).build()
            player?.apply {
                setPlayWhenReady(autoPlay)
                setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
                val hasStartPosition = autoPlayPosition != C.TIME_UNSET
                if (hasStartPosition) {
                    seekTo(0, autoPlayPosition)
                }
                prepare()
            }

            return player
        }

        fun releasePlayer() {
            player?.run {
                autoPlay = playWhenReady
                autoPlayPosition = contentPosition
                release()
            }
            player = null
        }
    }

    private companion object {
        const val TAG = "PlayerViewProvider"
    }
}
