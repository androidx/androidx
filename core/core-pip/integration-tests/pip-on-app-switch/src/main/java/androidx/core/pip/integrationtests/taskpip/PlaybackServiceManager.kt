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

package androidx.core.pip.integrationtests.taskpip

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * A singleton manager that wraps the ExoPlayer instance.
 *
 * This allows the playback to continue even if the Activity is recreated or goes to background.
 */
class PlaybackServiceManager private constructor(context: Context) {
    val player: ExoPlayer =
        ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_ALL }

    /** Attaches the given [PlayerView] to the managed ExoPlayer instance. */
    fun attachPlayerView(playerView: PlayerView) {
        playerView.player = player
    }

    /** Detaches any attached PlayerView from the managed ExoPlayer instance. */
    fun detachPlayerView(playerView: PlayerView) {
        if (playerView.player == player) {
            playerView.player = null
        }
    }

    /** Sets the media item from a local resource and starts playback. */
    fun setResourceMedia(context: Context, resId: Int) {
        val uri = Uri.parse("android.resource://${context.packageName}/$resId")
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    /** Sets the media item from a remote URL and starts playback. */
    fun setRemoteMedia(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun release() {
        player.release()
    }

    companion object {
        @Volatile private var instance: PlaybackServiceManager? = null

        fun getInstance(context: Context): PlaybackServiceManager {
            return instance
                ?: synchronized(this) {
                    instance
                        ?: PlaybackServiceManager(context.applicationContext).also { instance = it }
                }
        }
    }
}
