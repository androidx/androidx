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
import android.os.Build
import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Size
import androidx.camera.core.CameraEffect
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** Verifies that [LowLightBoostEffect] can be used on devices below Android 11 (API level 30). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class LowLightBoostEffectBelowApi30Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        PlayServicesLowLightBoostAvailability.reset()
    }

    @After
    fun tearDown() {
        PlayServicesLowLightBoostAvailability.reset()
    }

    @Test
    fun isSupportedAsync_resolvesToFalseImmediately() {
        val cameraInfo = FakeCameraInfoInternal("0")
        // Even if support was cached as true, devices below API 30 are never supported.
        PlayServicesLowLightBoostAvailability.setDeviceSupported(true)
        PlayServicesLowLightBoostAvailability.setCameraSupported("0", true)

        val future = LowLightBoostEffect.isSupportedAsync(context, cameraInfo)

        assertThat(future.isDone).isTrue()
        assertThat(future.get()).isFalse()
    }

    @Test
    fun isSupported_returnsFalse(): Unit = runBlocking {
        val cameraInfo = FakeCameraInfoInternal("0")
        PlayServicesLowLightBoostAvailability.setDeviceSupported(true)
        PlayServicesLowLightBoostAvailability.setCameraSupported("0", true)

        assertThat(LowLightBoostEffect.isSupported(context, cameraInfo)).isFalse()
    }

    @Test
    fun construct_succeedsWithOffState() {
        val effect =
            LowLightBoostEffect(
                context,
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE,
            )

        assertThat(effect.state.value?.type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    @Test
    fun setLowLightBoostEnabled_doesNothing() {
        val effect = LowLightBoostEffect(context, CameraEffect.PREVIEW)

        effect.setLowLightBoostEnabled(true)
        shadowOf(getMainLooper()).idle()

        assertThat(effect.state.value?.type).isEqualTo(LowLightBoostEffectState.OFF)
    }

    // Robolectric's EGL shadows below API 30 cannot initialize OpenGlRenderer, so this test runs on
    // the target SDK with Build.VERSION.SDK_INT overridden instead.
    @Config(sdk = [Config.TARGET_SDK])
    @Test
    fun inputSurface_providesPassthroughSurfaceAndReportsUnsupportedDevice() {
        val originalSdkInt = Build.VERSION.SDK_INT
        ReflectionHelpers.setStaticField(
            Build.VERSION::class.java,
            "SDK_INT",
            Build.VERSION_CODES.Q,
        )
        // Arrange: a session manager that runs synchronously on the test thread.
        val sessionManager =
            LowLightBoostSessionManager(
                CameraXExecutors.directExecutor(),
                Handler(getMainLooper()),
                rendererFactory = { FakeGlRenderer() },
            )
        val processor = PlayServicesLowLightBoostSurfaceProcessor(context, { null }, sessionManager)
        val resolution = Size(1920, 1080)
        val surfaceRequest =
            SurfaceRequest(resolution, FakeCamera("0"), DynamicRange.SDR, FRAME_RATE_RANGE) {}
        try {
            // Act.
            sessionManager.bindInputSurfaceRequest(
                processor,
                surfaceRequest,
                surfaceRequest.camera.cameraInfoInternal,
                "0",
            )
            sessionManager.provideInputSurfaceAndPrepareSession(
                processor = processor,
                surfaceRequest = surfaceRequest,
                resolution = resolution,
                cameraId = "0",
            )
            shadowOf(getMainLooper()).idle()

            // Assert: the camera stream is still connected for passthrough rendering, and the
            // device is reported as unsupported like any other unsupported device.
            assertThat(surfaceRequest.deferrableSurface.surface.isDone).isTrue()
            val state = requireNotNull(processor.stateLiveData.value)
            assertThat(state.type).isEqualTo(LowLightBoostEffectState.ERROR)
            assertThat(state.error).isInstanceOf(UnsupportedOperationException::class.java)
            assertThat(state.error).hasMessageThat().contains("not supported on this device")
            assertThat(sessionManager.getCurrentSession()).isNull()
        } finally {
            sessionManager.destroySessionInternal()
            surfaceRequest.deferrableSurface.close()
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", originalSdkInt)
        }
    }

    private companion object {
        private val FRAME_RATE_RANGE = SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
    }
}
