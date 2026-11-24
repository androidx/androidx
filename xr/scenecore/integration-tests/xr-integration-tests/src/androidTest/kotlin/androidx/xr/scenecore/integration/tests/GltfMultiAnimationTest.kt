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
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfAnimation
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/** Automated integration tests for SceneCore multi-channel glTF animations. */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class GltfMultiAnimationTest {

    @get:Rule(order = 0) val sessionRule = createXrSessionRule()

    @get:Rule(order = 1) val timeout: Timeout = Timeout.seconds(TEST_TIMEOUT_SECONDS)

    @Before
    fun setUp() {
        sessionRule.requestFullSpace()
    }

    @Test
    fun animation_singleChannelFsm_transitionsThroughStates() {
        sessionRule.onMainThread {
            val gltfModel = GltfModel.create(session, Paths.get("models", "RobotExpressive.glb"))
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    pose = Pose(translation = Vector3(0f, 0f, -1.5f)),
                    parent = session.scene.activitySpace,
                )

            assertThat(entity.getAnimations()).isNotEmpty()

            val walkingAnim = entity.requireAnimation(WALKING)

            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)

            // Loop so that the renderer cannot naturally end playback (and asynchronously
            // report STOPPED) before pause() is called on a slow device.
            walkingAnim.loop = true
            walkingAnim.speed = 1.0f
            walkingAnim.start()
            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

            // Not a synchronization point: only lets the animation play for a while.
            delay(PLAYBACK_INTERVAL)
            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
            walkingAnim.pause()
            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PAUSED)

            // TODO(b/581865187): Add golden screenshot verification that the skeleton pose changes
            //  while PLAYING and freezes while PAUSED.

            // Not a synchronization point: only keeps the animation paused for a settle window.
            delay(SETTLE_WINDOW)
            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PAUSED)
            walkingAnim.resume()
            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

            walkingAnim.stop()
            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
        }
    }

    @Test
    fun animation_multiChannelConcurrency_playsSimultaneously() {
        sessionRule.onMainThread {
            val gltfModel = GltfModel.create(session, Paths.get("models", "RobotExpressive.glb"))
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    pose = Pose(translation = Vector3(0f, 0f, -1.5f)),
                    parent = session.scene.activitySpace,
                )

            assertThat(entity.getAnimations().size).isAtLeast(2)

            // Both channels drive the same skeleton, so visually the higher-index channel wins.
            // Concurrency here refers to independent per-channel playback state, not blending.
            val walkingAnim = entity.requireAnimation(WALKING)
            val waveAnim = entity.requireAnimation(WAVE)

            walkingAnim.loop = true
            waveAnim.loop = true
            walkingAnim.start()
            waveAnim.start()

            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
            assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

            // Not a synchronization point: only lets both channels play for a while.
            delay(PLAYBACK_INTERVAL)
            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
            assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
            walkingAnim.pause()
            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PAUSED)
            assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

            // Not a synchronization point: only keeps the first channel paused for a settle window.
            delay(SETTLE_WINDOW)
            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PAUSED)
            assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
            walkingAnim.resume()
            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
            assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

            waveAnim.stop()
            assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

            walkingAnim.stop()
            assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
        }
    }

    @Test
    fun animation_speedAndLooping_updatesLivePlayback() {
        // A non-looping animation reaching its end is the renderer-driven transition that is
        // reported asynchronously as STOPPED through the listener.
        val loopAnimStopped = CountDownLatch(1)
        val loopListener =
            Consumer<GltfAnimation.AnimationState> { state ->
                if (state == GltfAnimation.AnimationState.STOPPED) {
                    loopAnimStopped.countDown()
                }
            }
        val speedAnimStopped = CountDownLatch(1)
        val speedListener =
            Consumer<GltfAnimation.AnimationState> { state ->
                if (state == GltfAnimation.AnimationState.STOPPED) {
                    speedAnimStopped.countDown()
                }
            }

        // 1. Looping: load the model, attach listeners, and start a 2x looping animation.
        val (loopAnim, speedAnim, loopDurationMs) =
            sessionRule.onMainThread {
                val gltfModel =
                    GltfModel.create(session, Paths.get("models", "RobotExpressive.glb"))
                val entity =
                    GltfModelEntity.create(
                        session = session,
                        model = gltfModel,
                        parent = session.scene.activitySpace,
                    )
                val loopAnim = entity.requireAnimation(DEATH)
                val speedAnim = entity.requireAnimation(DANCE)
                loopAnim.addAnimationStateListener(loopListener)
                speedAnim.addAnimationStateListener(speedListener)

                loopAnim.loop = true
                loopAnim.speed = 2.0f
                loopAnim.start()
                assertThat(loopAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
                Triple(loopAnim, speedAnim, loopAnim.duration.toMillis())
            }

        // Lower bound only (~3 cycles at 2x): renderer lag can only delay the natural end, so this
        // wait cannot cause a false failure.
        assertThat(loopAnimStopped.await(loopDurationMs * 3 / 2, TimeUnit.MILLISECONDS)).isFalse()
        assertThat(loopAnimStopped.count).isEqualTo(1L)

        // Verify loop kept playing, then disable loop so the renderer ends it naturally.
        sessionRule.onMainThread {
            assertThat(loopAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
            loopAnim.loop = false
        }
        assertThat(
                loopAnimStopped.await(CALLBACK_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            )
            .isTrue()

        // 2. Speed: verify STOPPED, then start a slowed-down, non-looping animation.
        val speedDurationMs = sessionRule.onMainThread {
            assertThat(loopAnim.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)

            speedAnim.loop = false
            speedAnim.speed = SLOW_SPEED
            speedAnim.start()
            assertThat(speedAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
            speedAnim.duration.toMillis()
        }

        // Lower bound only: at SLOW_SPEED, at most ~30% of the animation has played by now.
        assertThat(speedAnimStopped.await(speedDurationMs * 6 / 5, TimeUnit.MILLISECONDS)).isFalse()
        assertThat(speedAnimStopped.count).isEqualTo(1L)

        // Speeding up the live playback must make it end well within the timeout; at SLOW_SPEED the
        // remaining playback would take longer than CALLBACK_TIMEOUT.
        sessionRule.onMainThread {
            assertThat(speedAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
            speedAnim.speed = FAST_SPEED
        }
        assertThat(
                speedAnimStopped.await(CALLBACK_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            )
            .isTrue()
        sessionRule.onMainThread {
            assertThat(speedAnim.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
        }
    }

    @Test
    fun animation_negativeSpeed_loopsContinuouslyAndResumesForward() {
        val stopped = CountDownLatch(1)
        val listener =
            Consumer<GltfAnimation.AnimationState> { state ->
                if (state == GltfAnimation.AnimationState.STOPPED) {
                    stopped.countDown()
                }
            }

        // Start WALKING (~1 s) looping in reverse at -2x (R33 Step 10).
        val (walkingAnim, startedState) =
            sessionRule.onMainThread {
                val gltfModel =
                    GltfModel.create(session, Paths.get("models", "RobotExpressive.glb"))
                val entity =
                    GltfModelEntity.create(
                        session = session,
                        model = gltfModel,
                        parent = session.scene.activitySpace,
                    )
                val anim = entity.requireAnimation(WALKING)
                anim.addAnimationStateListener(listener)
                anim.loop = true
                anim.speed = REVERSE_SPEED
                anim.start()
                anim to anim.animationState
            }
        assertThat(startedState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

        // Let ~3 reverse cycles run (1.5 s at -2x) without stopping; verified via the listener
        // latch so waiting stays off the main thread and provides a happens-before boundary.
        assertThat(stopped.await(REVERSE_LOOP_DURATION.inWholeMilliseconds, TimeUnit.MILLISECONDS))
            .isFalse()
        assertThat(stopped.count).isEqualTo(1L)

        // Switch live speed back to 1x forward while looping (R33 Step 11).
        val stateBeforeResume = sessionRule.onMainThread {
            val state = walkingAnim.animationState
            walkingAnim.speed = 1.0f
            state
        }
        assertThat(stateBeforeResume).isEqualTo(GltfAnimation.AnimationState.PLAYING)
        assertThat(stopped.await(PLAYBACK_INTERVAL.inWholeMilliseconds, TimeUnit.MILLISECONDS))
            .isFalse()
        assertThat(stopped.count).isEqualTo(1L)

        // Stopping must notify the listener and transition state to STOPPED.
        val (stateBeforeStop, stateAfterStop) =
            sessionRule.onMainThread {
                val before = walkingAnim.animationState
                walkingAnim.stop()
                before to walkingAnim.animationState
            }
        assertThat(stateBeforeStop).isEqualTo(GltfAnimation.AnimationState.PLAYING)
        assertThat(stateAfterStop).isEqualTo(GltfAnimation.AnimationState.STOPPED)
        assertThat(stopped.await(CALLBACK_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS))
            .isTrue()
    }

    @Test
    fun animation_negativeSpeed_reversesNonLoopingPlaybackToStart() {
        val stopped = CountDownLatch(1)
        val listener =
            Consumer<GltfAnimation.AnimationState> { state ->
                if (state == GltfAnimation.AnimationState.STOPPED) {
                    stopped.countDown()
                }
            }

        // Play DANCE (3.33 s) forward at 0.5x for 1.0 s (~0.5 s elapsed, ~2.83 s remaining), then
        // flip speed to -1x. Rewinding to t=0 takes ~0.5 s (< 2 s timeout); if the speed update
        // were ignored (+0.5x -> ~5.66 s) or the minus sign dropped (+1x -> ~2.83 s), it would time
        // out. GltfAnimation does not expose current playback time, so a 1.0 s forward window
        // balances lag headroom against total duration; pose-level checks are tracked in
        // b/581865187.
        val (danceAnim, startedState) =
            sessionRule.onMainThread {
                val gltfModel =
                    GltfModel.create(session, Paths.get("models", "RobotExpressive.glb"))
                val entity =
                    GltfModelEntity.create(
                        session = session,
                        model = gltfModel,
                        parent = session.scene.activitySpace,
                    )
                val anim = entity.requireAnimation(DANCE)
                anim.addAnimationStateListener(listener)
                anim.loop = false
                anim.speed = 0.5f
                anim.start()
                anim to anim.animationState
            }
        assertThat(startedState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

        assertThat(
                stopped.await(
                    REVERSE_FORWARD_PLAY_DURATION.inWholeMilliseconds,
                    TimeUnit.MILLISECONDS,
                )
            )
            .isFalse()
        assertThat(stopped.count).isEqualTo(1L)

        val stateBeforeReverse = sessionRule.onMainThread {
            val state = danceAnim.animationState
            danceAnim.speed = -1.0f
            state
        }
        assertThat(stateBeforeReverse).isEqualTo(GltfAnimation.AnimationState.PLAYING)
        assertThat(
                stopped.await(
                    REVERSE_COMPLETION_TIMEOUT.inWholeMilliseconds,
                    TimeUnit.MILLISECONDS,
                )
            )
            .isTrue()
        val finalState = sessionRule.onMainThread { danceAnim.animationState }
        assertThat(finalState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
    }

    @Test
    fun animation_negativeSpeed_nonLoopingFromStart_stopsImmediately() {
        val stopped = CountDownLatch(1)
        val listener =
            Consumer<GltfAnimation.AnimationState> { state ->
                if (state == GltfAnimation.AnimationState.STOPPED) {
                    stopped.countDown()
                }
            }

        // DANCE (3.33 s) started non-looping at -1x from t=0 hits the beginning immediately and
        // stops well under 2 s; if the minus sign were ignored (+1x forward), it would time out.
        val danceAnim = sessionRule.onMainThread {
            val gltfModel = GltfModel.create(session, Paths.get("models", "RobotExpressive.glb"))
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    parent = session.scene.activitySpace,
                )
            val anim = entity.requireAnimation(DANCE)
            anim.addAnimationStateListener(listener)
            anim.loop = false
            anim.speed = -1.0f
            anim.start()
            anim
        }

        assertThat(
                stopped.await(
                    REVERSE_COMPLETION_TIMEOUT.inWholeMilliseconds,
                    TimeUnit.MILLISECONDS,
                )
            )
            .isTrue()
        val finalState = sessionRule.onMainThread { danceAnim.animationState }
        assertThat(finalState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
    }

    @Test
    fun animation_stopAllAnimations_abortsAllActiveChannels() {
        sessionRule.onMainThread {
            val gltfModel = GltfModel.create(session, Paths.get("models", "RobotExpressive.glb"))
            val entity =
                GltfModelEntity.create(
                    session = session,
                    model = gltfModel,
                    parent = session.scene.activitySpace,
                )

            assertThat(entity.getAnimations().size).isAtLeast(2)
            val anim1 = entity.requireAnimation(WALKING)
            val anim2 = entity.requireAnimation(WAVE)

            anim1.loop = true
            anim1.start()

            anim2.loop = true
            anim2.start()

            assertThat(anim1.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
            assertThat(anim2.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

            // Not a synchronization point: only lets both channels play for a settle window.
            delay(SETTLE_WINDOW)
            assertThat(anim1.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
            assertThat(anim2.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
            entity.stopAllAnimations()

            assertThat(anim1.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
            assertThat(anim2.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
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

        /** Timeout for reverse completion; shorter than DANCE (3.33 s) remaining forward time. */
        val REVERSE_COMPLETION_TIMEOUT = 2.seconds

        /** Time to let a looping reverse animation run past multiple cycles. */
        val REVERSE_LOOP_DURATION = 1.5.seconds

        /** Forward play window before flipping non-looping DANCE to reverse speed. */
        val REVERSE_FORWARD_PLAY_DURATION = 1.seconds

        /** Time to let an animation play; not used for synchronization. */
        val PLAYBACK_INTERVAL = 500.milliseconds

        /** Settle window to keep an animation playing or paused; not used for synchronization. */
        val SETTLE_WINDOW = 300.milliseconds

        const val TEST_TIMEOUT_SECONDS = 30L
        const val SLOW_SPEED = 0.25f
        const val FAST_SPEED = 4.0f
        const val REVERSE_SPEED = -2.0f

        // Animation channels in RobotExpressive.glb.
        const val DANCE = "Dance" // 3.33 s
        const val DEATH = "Death" // 0.96 s
        const val WALKING = "Walking" // 0.96 s
        const val WAVE = "Wave" // 1.83 s
    }
}
