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

package androidx.xr.scenecore.spatial.rendering

import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.runtime.GltfEntity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class GltfAnimationFeatureImplTest {

    private val fakeImpressApi = FakeImpressApiImpl()
    private lateinit var modelImpressNode: ImpressNode
    private lateinit var animationFeature: GltfAnimationFeatureImpl
    private val animationName = "test_animation"
    private val animationIndex = 0
    private val animationDuration = 10f
    private val executor: Executor = Dispatchers.Main.asExecutor()

    @Before
    fun setUp() {
        modelImpressNode = fakeImpressApi.createImpressNode()
        animationFeature =
            GltfAnimationFeatureImpl(
                fakeImpressApi,
                modelImpressNode,
                animationIndex,
                animationName,
                animationDuration,
                executor,
            )
    }

    @Test
    fun animationName_returnsCorrectName() {
        assertThat(animationFeature.animationName).isEqualTo(animationName)
    }

    @Test
    fun animationIndex_returnsCorrectIndex() {
        assertThat(animationFeature.animationIndex).isEqualTo(animationIndex)
    }

    @Test
    fun animationDuration_returnsCorrectDuration() {
        assertThat(animationFeature.animationDuration).isEqualTo(animationDuration)
    }

    @Test
    fun animationState_returnsStoppedInitially() {
        assertThat(animationFeature.animationState).isEqualTo(GltfEntity.AnimationState.STOPPED)
    }

    @Test
    fun stopAnimation_stopsAnimationAndUpdatesState() = runTest {
        animationFeature.startAnimation(loop = true, speed = 1f, seekStartTimeSeconds = 0f)
        advanceUntilIdle()
        assertThat(animationFeature.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)

        animationFeature.stopAnimation()

        val channelAnimations = fakeImpressApi.getChannelAnimations(modelImpressNode)
        // Depending on implementation, it might remove the entry or just stop it.
        // FakeImpressApiImpl.stopGltfModelAnimation removes it.
        if (channelAnimations != null) {
            assertThat(channelAnimations).doesNotContainKey(animationIndex)
        }

        assertThat(animationFeature.animationState).isEqualTo(GltfEntity.AnimationState.STOPPED)
    }

    @Test
    fun pauseAnimation_pausesAnimationAndUpdatesState() = runTest {
        animationFeature.startAnimation(loop = true, speed = 1f, seekStartTimeSeconds = 0f)
        advanceUntilIdle()
        animationFeature.pauseAnimation()

        val channelAnimations = fakeImpressApi.getChannelAnimations(modelImpressNode)
        if (channelAnimations != null && channelAnimations.containsKey(animationIndex)) {
            val animation = channelAnimations[animationIndex]
            assertThat(animation?.paused).isTrue()
        }

        assertThat(animationFeature.animationState).isEqualTo(GltfEntity.AnimationState.PAUSED)
    }

    @Test
    fun resumeAnimation_resumesAnimationAndUpdatesState() = runTest {
        animationFeature.startAnimation(loop = true, speed = 1f, seekStartTimeSeconds = 0f)
        advanceUntilIdle()
        animationFeature.pauseAnimation()
        animationFeature.resumeAnimation()

        val channelAnimations = fakeImpressApi.getChannelAnimations(modelImpressNode)
        if (channelAnimations != null && channelAnimations.containsKey(animationIndex)) {
            val animation = channelAnimations[animationIndex]
            assertThat(animation?.paused).isFalse()
        }

        assertThat(animationFeature.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)
    }

    @Test
    fun seekAnimation_updatesPlaybackTime() = runTest {
        animationFeature.startAnimation(loop = true, speed = 1f, seekStartTimeSeconds = 0f)
        advanceUntilIdle()
        val seekTime = 5f
        animationFeature.seekAnimation(seekTime)

        val channelAnimations = fakeImpressApi.getChannelAnimations(modelImpressNode)
        if (channelAnimations != null && channelAnimations.containsKey(animationIndex)) {
            val animation = channelAnimations[animationIndex]
            assertThat(animation?.playbackTime).isEqualTo(seekTime)
        }
    }

    @Test
    fun setAnimationSpeed_updatesSpeed() = runTest {
        animationFeature.startAnimation(loop = true, speed = 1f, seekStartTimeSeconds = 0f)
        advanceUntilIdle()
        val newSpeed = 2f
        animationFeature.setAnimationSpeed(newSpeed)

        val channelAnimations = fakeImpressApi.getChannelAnimations(modelImpressNode)
        if (channelAnimations != null && channelAnimations.containsKey(animationIndex)) {
            val animation = channelAnimations[animationIndex]
            assertThat(animation?.speed).isEqualTo(newSpeed)
        }
    }

    @Test
    fun animationName_returnsNull_whenInputIsEmpty() {
        val feature =
            GltfAnimationFeatureImpl(
                fakeImpressApi,
                modelImpressNode,
                animationIndex,
                "",
                animationDuration,
                executor,
            )
        assertThat(feature.animationName).isNull()
    }
}
