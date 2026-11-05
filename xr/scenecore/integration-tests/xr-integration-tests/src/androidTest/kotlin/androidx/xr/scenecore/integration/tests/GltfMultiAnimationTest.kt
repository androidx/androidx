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

@file:kotlin.OptIn(androidx.xr.scenecore.ExperimentalGltfAnimationApi::class)

package androidx.xr.scenecore.integration.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.runtime.RequiresSpatialApi
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfAnimation
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import java.util.function.Consumer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith

/** Automated integration tests for SceneCore multi-channel glTF animations. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class GltfMultiAnimationTest {

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun animation_singleChannelFsm_transitionsThroughStates() = runTestWithSession { session ->
        session.scene.requestFullSpaceAndAwait()
        GltfModel.create(session, Paths.get("models", "RobotExpressive.glb")).use { gltfModel ->
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    pose = Pose(translation = Vector3(0f, 0f, -1.5f)),
                    parent = session.scene.activitySpace,
                )

            try {
                assertThat(entity.getAnimations()).isNotEmpty()

                val walkingAnim = entity.requireAnimation(WALKING)

                assertThat(walkingAnim.animationState)
                    .isEqualTo(GltfAnimation.AnimationState.STOPPED)

                // Loop so that the renderer cannot naturally end playback (and asynchronously
                // report STOPPED) before pause() is called on a slow device.
                walkingAnim.loop = true
                walkingAnim.speed = 1.0f
                walkingAnim.start()
                assertThat(walkingAnim.animationState)
                    .isEqualTo(GltfAnimation.AnimationState.PLAYING)

                // Not a synchronization point: only lets the animation play for a while.
                delay(PLAYBACK_INTERVAL)
                walkingAnim.pause()
                assertThat(walkingAnim.animationState)
                    .isEqualTo(GltfAnimation.AnimationState.PAUSED)

                // TODO(b/581865187): Verify skeleton pose changes while PLAYING and freezes
                //  while PAUSED (once 3D visual similarity testing is supported).

                // Not a synchronization point: only keeps the animation paused for a while.
                delay(SHORT_PLAYBACK_INTERVAL)
                walkingAnim.resume()
                assertThat(walkingAnim.animationState)
                    .isEqualTo(GltfAnimation.AnimationState.PLAYING)

                walkingAnim.stop()
                assertThat(walkingAnim.animationState)
                    .isEqualTo(GltfAnimation.AnimationState.STOPPED)
            } finally {
                entity.stopAllAnimations()
                entity.parent = null
            }
        }
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun animation_multiChannelConcurrency_playsSimultaneously() = runTestWithSession { session ->
        session.scene.requestFullSpaceAndAwait()
        GltfModel.create(session, Paths.get("models", "RobotExpressive.glb")).use { gltfModel ->
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    pose = Pose(translation = Vector3(0f, 0f, -1.5f)),
                    parent = session.scene.activitySpace,
                )

            try {
                assertThat(entity.getAnimations().size).isAtLeast(2)

                // Both channels drive the same skeleton, so visually the higher-index channel wins.
                // Concurrency here refers to independent per-channel playback state, not blending.
                val walkingAnim = entity.requireAnimation(WALKING)
                val waveAnim = entity.requireAnimation(WAVE)

                walkingAnim.loop = true
                waveAnim.loop = true
                walkingAnim.start()
                waveAnim.start()

                assertThat(walkingAnim.animationState)
                    .isEqualTo(GltfAnimation.AnimationState.PLAYING)
                assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

                // Not a synchronization point: only lets both channels play for a while.
                delay(PLAYBACK_INTERVAL)
                walkingAnim.pause()
                assertThat(walkingAnim.animationState)
                    .isEqualTo(GltfAnimation.AnimationState.PAUSED)
                assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

                // Not a synchronization point: only keeps the first channel paused for a while.
                delay(SHORT_PLAYBACK_INTERVAL)
                walkingAnim.resume()
                assertThat(walkingAnim.animationState)
                    .isEqualTo(GltfAnimation.AnimationState.PLAYING)
                assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

                waveAnim.stop()
                assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
                assertThat(walkingAnim.animationState)
                    .isEqualTo(GltfAnimation.AnimationState.PLAYING)

                walkingAnim.stop()
                assertThat(walkingAnim.animationState)
                    .isEqualTo(GltfAnimation.AnimationState.STOPPED)
            } finally {
                entity.stopAllAnimations()
                entity.parent = null
            }
        }
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun animation_speedAndLooping_updatesLivePlayback() = runTestWithSession { session ->
        session.scene.requestFullSpaceAndAwait()
        GltfModel.create(session, Paths.get("models", "RobotExpressive.glb")).use { gltfModel ->
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    parent = session.scene.activitySpace,
                )

            try {
                val loopAnim = entity.requireAnimation(DEATH)
                val speedAnim = entity.requireAnimation(DANCE)
                // A non-looping animation reaching its end is the renderer-driven transition that
                // is reported asynchronously as STOPPED through the listener.
                val loopAnimStopped = CompletableDeferred<Unit>()
                val loopListener =
                    Consumer<GltfAnimation.AnimationState> { state ->
                        if (state == GltfAnimation.AnimationState.STOPPED) {
                            loopAnimStopped.complete(Unit)
                        }
                    }
                val speedAnimStopped = CompletableDeferred<Unit>()
                val speedListener =
                    Consumer<GltfAnimation.AnimationState> { state ->
                        if (state == GltfAnimation.AnimationState.STOPPED) {
                            speedAnimStopped.complete(Unit)
                        }
                    }

                try {
                    loopAnim.addAnimationStateListener(loopListener)
                    speedAnim.addAnimationStateListener(speedListener)

                    // 1. Looping: a looping animation must keep playing past its natural end.
                    val loopDurationMs = loopAnim.duration.toMillis()
                    loopAnim.loop = true
                    loopAnim.speed = 2.0f
                    loopAnim.start()
                    assertThat(loopAnim.animationState)
                        .isEqualTo(GltfAnimation.AnimationState.PLAYING)

                    // Lower bound only (~3 cycles at 2x): renderer lag can only delay the natural
                    // end, so this wait cannot cause a false failure.
                    delay(loopDurationMs * 3 / 2)
                    assertThat(loopAnimStopped.isCompleted).isFalse()
                    assertThat(loopAnim.animationState)
                        .isEqualTo(GltfAnimation.AnimationState.PLAYING)

                    // Disabling loop on the live playback must let the renderer end it naturally.
                    loopAnim.loop = false
                    withTimeout(CALLBACK_TIMEOUT) { loopAnimStopped.await() }
                    assertThat(loopAnim.animationState)
                        .isEqualTo(GltfAnimation.AnimationState.STOPPED)

                    // 2. Speed: a slowed-down, non-looping animation must still be playing after
                    // its nominal (1x) duration has elapsed.
                    val speedDurationMs = speedAnim.duration.toMillis()
                    speedAnim.loop = false
                    speedAnim.speed = SLOW_SPEED
                    speedAnim.start()
                    assertThat(speedAnim.animationState)
                        .isEqualTo(GltfAnimation.AnimationState.PLAYING)

                    // Lower bound only: at SLOW_SPEED, at most ~30% of the animation has played by
                    // now.
                    delay(speedDurationMs * 6 / 5)
                    assertThat(speedAnimStopped.isCompleted).isFalse()
                    assertThat(speedAnim.animationState)
                        .isEqualTo(GltfAnimation.AnimationState.PLAYING)

                    // Speeding up the live playback must make it end well within the timeout; at
                    // SLOW_SPEED the remaining playback would take longer than CALLBACK_TIMEOUT.
                    speedAnim.speed = FAST_SPEED
                    withTimeout(CALLBACK_TIMEOUT) { speedAnimStopped.await() }
                    assertThat(speedAnim.animationState)
                        .isEqualTo(GltfAnimation.AnimationState.STOPPED)
                } finally {
                    loopAnim.removeAnimationStateListener(loopListener)
                    speedAnim.removeAnimationStateListener(speedListener)
                }
            } finally {
                entity.stopAllAnimations()
                entity.parent = null
            }
        }
    }

    @Test
    @XrDeviceTest
    @RequiresSpatialApi(SpatialApiVersions.SPATIAL_API_V2)
    fun animation_stopAllAnimations_abortsAllActiveChannels() = runTestWithSession { session ->
        session.scene.requestFullSpaceAndAwait()
        GltfModel.create(session, Paths.get("models", "RobotExpressive.glb")).use { gltfModel ->
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    parent = session.scene.activitySpace,
                )

            try {
                assertThat(entity.getAnimations().size).isAtLeast(2)
                val anim1 = entity.requireAnimation(WALKING)
                val anim2 = entity.requireAnimation(WAVE)

                anim1.loop = true
                anim1.start()

                anim2.loop = true
                anim2.start()

                assertThat(anim1.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
                assertThat(anim2.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

                // Not a synchronization point: only lets both channels play for a while.
                delay(SHORT_PLAYBACK_INTERVAL)
                entity.stopAllAnimations()

                assertThat(anim1.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
                assertThat(anim2.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
            } finally {
                entity.stopAllAnimations()
                entity.parent = null
            }
        }
    }

    /** Returns the animation channel named [name], failing with a clear message if missing. */
    private fun GltfModelEntity.requireAnimation(name: String): GltfAnimation =
        checkNotNull(getAnimations().firstOrNull { it.name == name }) {
            "Animation \"$name\" not found in RobotExpressive.glb"
        }

    private companion object {
        /** Timeout for renderer-driven animation state callbacks. */
        val CALLBACK_TIMEOUT = 5.seconds

        /** Time to let an animation play; not used for synchronization. */
        val PLAYBACK_INTERVAL = 500.milliseconds

        /** Short time to let an animation play or stay paused; not used for synchronization. */
        val SHORT_PLAYBACK_INTERVAL = 200.milliseconds

        const val SLOW_SPEED = 0.25f
        const val FAST_SPEED = 4.0f

        // Animation channels in RobotExpressive.glb.
        const val DANCE = "Dance" // 3.33 s
        const val DEATH = "Death" // 0.96 s
        const val WALKING = "Walking" // 0.96 s
        const val WAVE = "Wave" // 1.83 s
    }
}
