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

package androidx.xr.scenecore.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith

/** Automated integration tests for SceneCore multi-channel glTF animations. */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SceneCoreGltfMultiAnimationTest {

    @Test
    fun animation_singleChannelFsm_transitionsThroughStates() = runTestWithSession { session ->
        val playingDeferred = CompletableDeferred<Unit>()
        val listener =
            Consumer<GltfAnimation.AnimationState> { state ->
                if (state == GltfAnimation.AnimationState.PLAYING) {
                    playingDeferred.complete(Unit)
                }
            }

        val gltfModel = GltfModel.create(session, Paths.get("models", "RobotExpressive.glb"))
        val entity =
            GltfModelEntity.create(
                session = session,
                model = gltfModel,
                pose = Pose(translation = Vector3(0f, 0f, -1.5f)),
                parent = session.scene.activitySpace,
            )

        val animations = entity.getAnimations()
        assertThat(animations).isNotEmpty()

        val walkingAnim = animations.firstOrNull { it.name == "Walking" } ?: animations.first()

        assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)

        walkingAnim.addAnimationStateListener(listener)
        walkingAnim.loop = false
        walkingAnim.speed = 1.0f
        walkingAnim.start()

        if (walkingAnim.animationState == GltfAnimation.AnimationState.PLAYING) {
            playingDeferred.complete(Unit)
        }
        withTimeout(5000) { playingDeferred.await() }
        assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

        delay(500)
        walkingAnim.pause()
        assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PAUSED)

        delay(200)
        walkingAnim.resume()
        assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

        walkingAnim.stop()
        assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)

        walkingAnim.removeAnimationStateListener(listener)
        entity.parent = null
    }

    @Test
    fun animation_multiChannelConcurrency_playsSimultaneously() = runTestWithSession { session ->
        val playingDeferred1 = CompletableDeferred<Unit>()
        val playingDeferred2 = CompletableDeferred<Unit>()
        val listener1 =
            Consumer<GltfAnimation.AnimationState> { state ->
                if (state == GltfAnimation.AnimationState.PLAYING) {
                    playingDeferred1.complete(Unit)
                }
            }
        val listener2 =
            Consumer<GltfAnimation.AnimationState> { state ->
                if (state == GltfAnimation.AnimationState.PLAYING) {
                    playingDeferred2.complete(Unit)
                }
            }

        val gltfModel = GltfModel.create(session, Paths.get("models", "RobotExpressive.glb"))
        val entity =
            GltfModelEntity.create(
                session = session,
                model = gltfModel,
                pose = Pose(translation = Vector3(0f, 0f, -1.5f)),
                parent = session.scene.activitySpace,
            )

        val animations = entity.getAnimations()
        assertThat(animations.size).isAtLeast(2)

        val walkingAnim = animations.firstOrNull { it.name == "Walking" } ?: animations[0]
        val waveAnim =
            animations.firstOrNull { it.name == "Wave" || it.name == "Yes" } ?: animations[1]

        walkingAnim.addAnimationStateListener(listener1)
        waveAnim.addAnimationStateListener(listener2)

        walkingAnim.loop = true
        waveAnim.loop = true
        walkingAnim.start()
        waveAnim.start()

        if (walkingAnim.animationState == GltfAnimation.AnimationState.PLAYING) {
            playingDeferred1.complete(Unit)
        }
        if (waveAnim.animationState == GltfAnimation.AnimationState.PLAYING) {
            playingDeferred2.complete(Unit)
        }
        withTimeout(5000) { playingDeferred1.await() }
        withTimeout(5000) { playingDeferred2.await() }

        assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
        assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

        delay(500)
        walkingAnim.pause()
        assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PAUSED)
        assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

        delay(200)
        walkingAnim.resume()
        assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
        assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

        waveAnim.stop()
        assertThat(waveAnim.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
        assertThat(walkingAnim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

        walkingAnim.stop()
        walkingAnim.removeAnimationStateListener(listener1)
        waveAnim.removeAnimationStateListener(listener2)
        entity.parent = null
    }

    @Test
    fun animation_speedAndLooping_updatesLivePlayback() = runTestWithSession { session ->
        val playingDeferred = CompletableDeferred<Unit>()
        val listener =
            Consumer<GltfAnimation.AnimationState> { state ->
                if (state == GltfAnimation.AnimationState.PLAYING) {
                    playingDeferred.complete(Unit)
                }
            }

        val gltfModel = GltfModel.create(session, Paths.get("models", "RobotExpressive.glb"))
        val entity =
            GltfModelEntity.create(
                session = session,
                model = gltfModel,
                parent = session.scene.activitySpace,
            )

        val anim = entity.getAnimations().first()
        anim.loop = true
        anim.speed = 2.0f
        assertThat(anim.speed).isWithin(1e-4f).of(2.0f)
        anim.addAnimationStateListener(listener)
        anim.start()

        if (anim.animationState == GltfAnimation.AnimationState.PLAYING) {
            playingDeferred.complete(Unit)
        }
        withTimeout(5000) { playingDeferred.await() }
        assertThat(anim.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

        delay(200)
        anim.speed = 0.5f
        assertThat(anim.speed).isWithin(1e-4f).of(0.5f)

        anim.speed = 1.0f
        assertThat(anim.speed).isWithin(1e-4f).of(1.0f)

        anim.stop()
        anim.removeAnimationStateListener(listener)
        assertThat(anim.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
        entity.parent = null
    }

    @Test
    fun animation_stopAllAnimations_abortsAllActiveChannels() = runTestWithSession { session ->
        val playingDeferred1 = CompletableDeferred<Unit>()
        val playingDeferred2 = CompletableDeferred<Unit>()
        val listener1 =
            Consumer<GltfAnimation.AnimationState> { state ->
                if (state == GltfAnimation.AnimationState.PLAYING) {
                    playingDeferred1.complete(Unit)
                }
            }
        val listener2 =
            Consumer<GltfAnimation.AnimationState> { state ->
                if (state == GltfAnimation.AnimationState.PLAYING) {
                    playingDeferred2.complete(Unit)
                }
            }

        val gltfModel = GltfModel.create(session, Paths.get("models", "RobotExpressive.glb"))
        val entity =
            GltfModelEntity.create(
                session = session,
                model = gltfModel,
                parent = session.scene.activitySpace,
            )

        val animations = entity.getAnimations()
        val anim1 = animations[0]
        val anim2 = animations[1]

        anim1.loop = true
        anim1.addAnimationStateListener(listener1)
        anim1.start()

        anim2.loop = true
        anim2.addAnimationStateListener(listener2)
        anim2.start()

        if (anim1.animationState == GltfAnimation.AnimationState.PLAYING) {
            playingDeferred1.complete(Unit)
        }
        if (anim2.animationState == GltfAnimation.AnimationState.PLAYING) {
            playingDeferred2.complete(Unit)
        }
        withTimeout(5000) { playingDeferred1.await() }
        withTimeout(5000) { playingDeferred2.await() }

        assertThat(anim1.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)
        assertThat(anim2.animationState).isEqualTo(GltfAnimation.AnimationState.PLAYING)

        delay(200)
        entity.stopAllAnimations()

        assertThat(anim1.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)
        assertThat(anim2.animationState).isEqualTo(GltfAnimation.AnimationState.STOPPED)

        anim1.removeAnimationStateListener(listener1)
        anim2.removeAnimationStateListener(listener2)
        entity.parent = null
    }
}
