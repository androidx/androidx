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

package androidx.xr.compose.integration.tests

import android.content.Context
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.AndroidExternalSurfaceScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.screenshot.assertAgainstGolden
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.media.PointSourceExoplayerAudioOutput
import androidx.xr.compose.subspace.media.rememberPointSourceExoplayerAudioOutput
import androidx.xr.compose.subspace.media.spatializedAudioOutput
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.PositionalAudioComponent
import androidx.xr.testutils.DEFAULT_XR_MSSIM_THRESHOLD
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrScreenshotTestRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrumentation test for the **[Compose] Video in Spatial Panel** CUJ.
 *
 * Uses [createAndroidComposeRule] and [setXrScreenshotContent], starts a 2D video
 * (`videos/sample_video.mp4`) inside a [SpatialPanel] via a button, pauses playback once the first
 * video frame is rendered, seeks deterministically to the first frame (`0ms`), captures the settled
 * 3D compositor viewport via [captureToBitmap], asserts against golden images with
 * [assertAgainstGolden] and [XrScreenshotTestRule], and stops the video via a button.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class VideoInSpatialPanelScreenshotTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val screenshotRule = XrScreenshotTestRule(GOLDEN_PATH)

    @Test
    @XrDeviceTest
    fun videoInSpatialPanel_startPauseOnFirstFrameVerifyScreenshotAndStop() {
        val exoSession = ExoPlayerPanelSession(composeTestRule.activity, composeTestRule)

        composeTestRule.setXrScreenshotContent {
            var isVideoStarted by remember { mutableStateOf(false) }
            val audioOutput = rememberPointSourceExoplayerAudioOutput(PointSourceParams())

            SpatialPanel(
                modifier =
                    SubspaceModifier.width(640.dp)
                        .height(360.dp)
                        .movable()
                        .spatializedAudioOutput(audioOutput)
                        .testTag("video_spatial_panel")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isVideoStarted) {
                        AndroidExternalSurface(
                            modifier = Modifier.fillMaxSize().testTag("video_external_surface")
                        ) {
                            exoSession.bind(this, audioOutput)
                        }
                    }

                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!isVideoStarted) {
                            Button(
                                onClick = { isVideoStarted = true },
                                modifier = Modifier.testTag("start_video_button"),
                            ) {
                                Text("Start Video")
                            }
                        } else {
                            Button(
                                onClick = { exoSession.pause() },
                                modifier = Modifier.testTag("pause_video_button"),
                            ) {
                                Text("Pause Video")
                            }
                            Button(
                                onClick = { isVideoStarted = false },
                                modifier = Modifier.testTag("stop_video_button"),
                            ) {
                                Text("Stop Video")
                            }
                        }
                    }
                }
            }
        }

        // 1. Verify initial state before starting video.
        composeTestRule
            .onSubspaceNodeWithTag("video_spatial_panel")
            .assertExists()
            .assertWidthIsEqualTo(640.dp)
            .assertHeightIsEqualTo(360.dp)
        composeTestRule.onNodeWithText("Start Video").assertIsDisplayed()
        composeTestRule.onNodeWithTag("video_external_surface").assertDoesNotExist()

        // 2. Click "Start Video" and immediately wait for the first frame to render.
        composeTestRule.onNodeWithText("Start Video").performClick()
        exoSession.awaitFirstFrameRendered()

        // 3. Pause playback, seek deterministically to the first frame (0ms), and verify UI and
        // screenshot.
        composeTestRule.onNodeWithText("Pause Video").performClick()
        composeTestRule.waitUntil(timeoutMillis = PLAYBACK_TIMEOUT_MS) {
            !exoSession.isPlaying.get()
        }
        exoSession.seekToAndAwaitFrame(FIRST_FRAME_TIMESTAMP_MS)

        composeTestRule.onNodeWithTag("video_external_surface").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pause Video").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stop Video").assertIsDisplayed()

        val semanticsNode =
            composeTestRule.onSubspaceNodeWithTag("video_spatial_panel").fetchSemanticsNode()
        val panelEntity = checkNotNull(semanticsNode.semanticsEntity as? PanelEntity)
        assertThat(panelEntity.getComponentsOfType(PositionalAudioComponent::class.java)).hasSize(1)

        composeTestRule
            .captureToBitmap()
            .assertAgainstGolden(
                screenshotRule,
                "compose_video_in_spatial_panel_first_frame",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )

        // 4. Click "Stop Video" and verify the video stops and surface is destroyed.
        composeTestRule.onNodeWithText("Stop Video").performClick()
        composeTestRule.waitUntil(timeoutMillis = PLAYBACK_TIMEOUT_MS) {
            exoSession.destroyedSurface.get() != null
        }
        composeTestRule.onNodeWithTag("video_external_surface").assertDoesNotExist()
        composeTestRule.onNodeWithText("Start Video").assertIsDisplayed()
    }

    private class ExoPlayerPanelSession(
        private val context: Context,
        private val composeTestRule: ComposeTestRule,
    ) {
        val surface = AtomicReference<Surface?>(null)
        val destroyedSurface = AtomicReference<Surface?>(null)
        val isPlaying = AtomicBoolean(false)
        val firstFrameRendered = AtomicBoolean(false)
        val playbackState = AtomicInteger(Player.STATE_IDLE)
        val videoWidth = AtomicInteger(0)
        val videoHeight = AtomicInteger(0)
        private val playerRef = AtomicReference<ExoPlayer?>(null)

        fun bind(
            scope: AndroidExternalSurfaceScope,
            audioOutput: PointSourceExoplayerAudioOutput,
        ) {
            with(scope) {
                onSurface { createdSurface, _, _ ->
                    surface.set(createdSurface)
                    val player =
                        ExoPlayer.Builder(context)
                            .setAudioOutputProvider(audioOutput.audioOutputProvider)
                            .build()
                            .apply {
                                setVideoSurface(createdSurface)
                                addListener(
                                    object : Player.Listener {
                                        override fun onIsPlayingChanged(playing: Boolean) {
                                            this@ExoPlayerPanelSession.isPlaying.set(playing)
                                        }

                                        override fun onPlaybackStateChanged(state: Int) {
                                            this@ExoPlayerPanelSession.playbackState.set(state)
                                        }

                                        override fun onRenderedFirstFrame() {
                                            firstFrameRendered.set(true)
                                        }

                                        override fun onVideoSizeChanged(videoSize: VideoSize) {
                                            if (videoSize.width > 0 && videoSize.height > 0) {
                                                videoWidth.set(videoSize.width)
                                                videoHeight.set(videoSize.height)
                                            }
                                        }
                                    }
                                )
                                setMediaItem(MediaItem.fromUri(SAMPLE_VIDEO_ASSET_URI))
                                repeatMode = Player.REPEAT_MODE_ALL
                                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                                prepare()
                                play()
                            }
                    playerRef.set(player)

                    createdSurface.onDestroyed {
                        destroyedSurface.set(createdSurface)
                        this@ExoPlayerPanelSession.isPlaying.set(false)
                        this@ExoPlayerPanelSession.playbackState.set(Player.STATE_IDLE)
                        playerRef.getAndSet(null)?.let { activePlayer ->
                            runCatching {
                                activePlayer.stop()
                                activePlayer.clearVideoSurface()
                                activePlayer.release()
                            }
                        }
                    }
                }
            }
        }

        fun pause() {
            playerRef.get()?.pause()
        }

        fun awaitFirstFrameRendered() {
            composeTestRule.waitUntil(
                conditionDescription = "ExoPlayer rendered first video frame in SpatialPanel",
                timeoutMillis = PLAYBACK_TIMEOUT_MS,
            ) {
                surface.get()?.isValid == true &&
                    firstFrameRendered.get() &&
                    videoWidth.get() > 0 &&
                    videoHeight.get() > 0
            }
        }

        fun seekToAndAwaitFrame(positionMs: Long) {
            composeTestRule.runOnUiThread {
                val player = checkNotNull(playerRef.get()) { "ExoPlayer is not initialized" }
                playbackState.set(Player.STATE_BUFFERING)
                player.seekTo(positionMs)
            }
            composeTestRule.waitUntil(
                conditionDescription =
                    "ExoPlayer rendered seeked frame at ${positionMs}ms in SpatialPanel",
                timeoutMillis = PLAYBACK_TIMEOUT_MS,
            ) {
                surface.get()?.isValid == true && playbackState.get() == Player.STATE_READY
            }
        }
    }

    private companion object {
        const val SAMPLE_VIDEO_ASSET_URI = "asset:///videos/sample_video.mp4"
        const val FIRST_FRAME_TIMESTAMP_MS = 0L
        const val PLAYBACK_TIMEOUT_MS = 10_000L
    }
}
