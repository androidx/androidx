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

package androidx.camera.effects.playservices.lowlight

import android.content.Context
import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Size
import androidx.camera.core.CameraEffect
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class LowLightBoostEffectTest {

    private lateinit var context: Context

    private val client = FakeLowLightBoostClient()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        PlayServicesLowLightBoostAvailability.reset()
        PlayServicesLowLightBoostAvailability.clientFactoryForTesting = { client }
    }

    @After
    fun tearDown() {
        PlayServicesLowLightBoostAvailability.reset()
    }

    @Test
    fun testLowLightBoostEffectInitialization() {
        val effect = LowLightBoostEffect(context, CameraEffect.PREVIEW)
        assertThat(effect.targets).isEqualTo(CameraEffect.PREVIEW)
        assertThat(effect.transformation).isEqualTo(CameraEffect.TRANSFORMATION_ARBITRARY)
        assertThat(effect.state.value?.type).isEqualTo(LowLightBoostEffectState.OFF)

        val effectWithAllTargets =
            LowLightBoostEffect(
                context,
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE,
            )
        assertThat(effectWithAllTargets.targets)
            .isEqualTo(
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE
            )

        assertThrows(IllegalArgumentException::class.java) {
            LowLightBoostEffect(context, CameraEffect.IMAGE_CAPTURE)
        }
    }

    @Test
    fun testSetLowLightBoostEnabledToggling() {
        // Run the session work synchronously on the test thread.
        val sessionManager =
            LowLightBoostSessionManager(
                CameraXExecutors.directExecutor(),
                Handler(getMainLooper()),
                rendererFactory = { FakeGlRenderer() },
            )
        val effect =
            LowLightBoostEffect(
                CameraEffect.PREVIEW,
                PlayServicesLowLightBoostSurfaceProcessor(context, { client }, sessionManager),
            )
        // The processor callbacks run on the GL thread of the session manager.
        assertThat(effect.executor).isSameInstanceAs(sessionManager.glExecutor)

        effect.setLowLightBoostEnabled(false)
        shadowOf(getMainLooper()).idle()
        assertThat(effect.state.value?.type).isEqualTo(LowLightBoostEffectState.OFF)

        // The effect isn't bound, so it doesn't process a camera stream.
        effect.setLowLightBoostEnabled(true)
        shadowOf(getMainLooper()).idle()
        assertThat(effect.state.value?.type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    @Test
    fun testIsSupported() = runBlocking {
        val camera0 = FakeCameraInfoInternal("0")
        client.cameraSupportedTask = { Tasks.forResult(false) }
        assertThat(LowLightBoostEffect.isSupported(context, camera0)).isFalse()

        // Once support is cached, isSupportedAsync completes immediately.
        PlayServicesLowLightBoostAvailability.setCameraSupported("0", true)
        val cachedSupportedFuture = LowLightBoostEffect.isSupportedAsync(context, camera0)
        assertThat(cachedSupportedFuture.isDone).isTrue()
        assertThat(cachedSupportedFuture.get()).isTrue()
        assertThat(LowLightBoostEffect.isSupported(context, camera0)).isTrue()
    }

    @Test
    fun testResolutionValidator() {
        val effect = LowLightBoostEffect(context, CameraEffect.PREVIEW)
        val validator = effect.resolutionValidator

        // Supported resolutions (width <= 2560, height <= 2560, area <= 4,194,304)
        assertThat(validator.test(Size(1920, 1080))).isTrue()
        assertThat(validator.test(Size(1920, 1440))).isTrue()
        assertThat(validator.test(Size(2560, 1440))).isTrue()

        // Unsupported resolutions (exceeds 2560 dimension or 4MP area)
        assertThat(validator.test(Size(3840, 2160))).isFalse()
        assertThat(validator.test(Size(2560, 1920))).isFalse()
    }
}
