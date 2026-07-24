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

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.pip.PictureInPictureDelegate
import androidx.core.pip.VideoPlaybackPictureInPicture
import androidx.core.pip.contentpip.ContentPipCallback
import androidx.core.pip.contentpip.enablePipOnAppSwitch
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import java.util.concurrent.Executors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main activity for the Task PIP demo.
 *
 * It hosts a [PlayerView] and uses [PlaybackServiceManager] to manage media playback.
 */
class MainActivity : AppCompatActivity(), ContentPipCallback {

    private lateinit var playerView: PlayerView
    private lateinit var playbackManager: PlaybackServiceManager
    private lateinit var pipImplementation: VideoPlaybackPictureInPicture
    private val pipExecutor = Executors.newSingleThreadExecutor()

    private val REMOTE_VIDEO_URL = "https://media.w3.org/2010/05/sintel/trailer.mp4"

    private val ACTION_PLAY = "androidx.core.pip.taskpip.ACTION_PLAY"
    private val ACTION_PAUSE = "androidx.core.pip.taskpip.ACTION_PAUSE"

    private val pipReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_PLAY -> playbackManager.play()
                    ACTION_PAUSE -> playbackManager.pause()
                }
            }
        }

    private val playerListener =
        object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                pipImplementation.setEnabled(isPlaying)
                updatePipActions(isPlaying)
                pipImplementation.commit()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.player_view)
        playbackManager = PlaybackServiceManager.getInstance(this)

        // Setup remote media item
        playbackManager.setRemoteMedia(REMOTE_VIDEO_URL)

        playbackManager.player.addListener(playerListener)

        // Initialize PiP implementation
        pipImplementation = VideoPlaybackPictureInPicture(this, pipExecutor)
        pipImplementation
            .setPlayerView(playerView)
            .setAspectRatio(Rational(16, 9))
            .setEnabled(playbackManager.player.isPlaying)

        updatePipActions(playbackManager.player.isPlaying)
        pipImplementation.commit()

        // Enable Task PiP fallback (Synchronous trigger + Lifecycle Pullback)
        enablePipOnAppSwitch(this)

        val descriptionText = findViewById<View>(R.id.description_text)

        pipImplementation.addOnPictureInPictureEventListener(
            mainExecutor,
            object : PictureInPictureDelegate.OnPictureInPictureEventListener {
                override fun onPictureInPictureEvent(
                    event: PictureInPictureDelegate.Event,
                    config: android.content.res.Configuration?,
                ) {
                    when (event) {
                        PictureInPictureDelegate.Event.ENTER_ANIMATION_START,
                        PictureInPictureDelegate.Event.ENTERED -> {
                            descriptionText.visibility = View.GONE
                            playerView.useController = false
                        }
                        PictureInPictureDelegate.Event.EXITED -> {
                            descriptionText.visibility = View.VISIBLE
                            playerView.useController = true
                        }
                    }
                }
            },
        )

        ContextCompat.registerReceiver(
            this,
            pipReceiver,
            IntentFilter().apply {
                addAction(ACTION_PLAY)
                addAction(ACTION_PAUSE)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    private fun updatePipActions(isPlaying: Boolean) {
        val actionIntent =
            if (isPlaying) {
                Intent(ACTION_PAUSE).setPackage(packageName)
            } else {
                Intent(ACTION_PLAY).setPackage(packageName)
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                this,
                isPlaying.hashCode(), // Unique requestCode
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val iconRes =
            if (isPlaying) {
                android.R.drawable.ic_media_pause
            } else {
                android.R.drawable.ic_media_play
            }

        val title =
            if (isPlaying) {
                getString(R.string.pip_action_pause)
            } else {
                getString(R.string.pip_action_play)
            }

        val action =
            RemoteAction(Icon.createWithResource(this, iconRes), title, title, pendingIntent)

        pipImplementation.setActions(listOf(action))
    }

    override fun onStart() {
        super.onStart()
        playbackManager.attachPlayerView(playerView)
    }

    override fun onResume() {
        super.onResume()
        // Ensure player view is attached
        playbackManager.attachPlayerView(playerView)
    }

    override fun onStop() {
        super.onStop()
        // Detach to avoid leaks, but playback continues in background if desired
        playbackManager.detachPlayerView(playerView)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(pipReceiver)
        playbackManager.player.removeListener(playerListener)
        pipImplementation.close()
        pipExecutor.shutdown()
        // If we want to release the player when the activity is finished
        if (isFinishing) {
            playbackManager.release()
        }
    }

    override fun onInitContentPip(): Boolean {
        return playbackManager.player.isPlaying
    }

    override fun onPrepareContentPip(): Boolean {
        playbackManager.detachPlayerView(playerView)
        return true
    }

    override fun onAttachContentPip(pipActivity: ComponentActivity) {
        val pipPlayerView = PlayerView(pipActivity)
        pipPlayerView.useController = false

        // --- Essential Polish: Visibility Hack ---
        // Initially invisible and transparent to prevent full-screen flash
        pipPlayerView.visibility = View.INVISIBLE
        pipPlayerView.setBackgroundColor(Color.TRANSPARENT)
        pipActivity.setContentView(pipPlayerView)
        playbackManager.attachPlayerView(pipPlayerView)

        // Show after a short delay once PiP mode is likely active
        pipActivity.lifecycleScope.launch {
            delay(200L)
            pipPlayerView.visibility = View.VISIBLE
        }

        // --- Essential Polish: Set the params ---
        val paramsBuilder = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            paramsBuilder.setSeamlessResizeEnabled(true)
        }
        pipActivity.setPictureInPictureParams(paramsBuilder.build())
    }

    override fun onFinishContentPip(isDismissed: Boolean) {
        // isStopping=true suggests the PiP is dismissed, stop the playback.
        if (isDismissed) {
            playbackManager.player.stop()
        } else {
            playbackManager.player.play()
            // Bring MainActivity back to the front smoothly
            val intent =
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            startActivity(intent)
        }
    }
}
