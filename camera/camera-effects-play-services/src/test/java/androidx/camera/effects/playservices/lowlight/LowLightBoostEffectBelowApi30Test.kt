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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
    fun construct_succeedsWithOffStateAndEnabledDefault() {
        val effect =
            LowLightBoostEffect(
                context,
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE,
            )

        assertThat(effect.isLowLightBoostEnabled).isTrue()
        assertThat(effect.status).isEqualTo(LowLightBoostEffect.STATUS_OFF)
        assertThat(effect.error).isNull()
    }

    @Test
    fun isLowLightBoostEnabled_togglesUniformlyAndNotifiesBeforeBinding() {
        val effect = LowLightBoostEffect(context, CameraEffect.PREVIEW)
        val listenerEvents = mutableListOf<Boolean>()
        val observed = mutableListOf<Boolean>()
        effect.addListener(
            object : LowLightBoostEffect.Listener {
                override fun onLowLightBoostEnabledChanged(enabled: Boolean) {
                    listenerEvents.add(enabled)
                }
            }
        )
        val scope =
            CoroutineScope(SupervisorJob() + Handler(getMainLooper()).asCoroutineDispatcher())
        try {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                effect.observe { observed.add(isLowLightBoostEnabled) }
            }

            effect.isLowLightBoostEnabled = false
            assertThat(effect.isLowLightBoostEnabled).isFalse()
            effect.isLowLightBoostEnabled = true
            assertThat(effect.isLowLightBoostEnabled).isTrue()
            shadowOf(getMainLooper()).idle()

            assertThat(listenerEvents).containsExactly(false, true).inOrder()
            assertThat(observed).hasSize(3)
            assertThat(effect.status).isEqualTo(LowLightBoostEffect.STATUS_OFF)
            assertThat(effect.error).isNull()
        } finally {
            scope.cancel()
            shadowOf(getMainLooper()).idle()
        }
    }

    // Robolectric's EGL shadows below API 30 cannot initialize OpenGlRenderer, so this test runs on
    // the target SDK with Build.VERSION.SDK_INT overridden instead.
    @Config(sdk = [Config.TARGET_SDK])
    @Test
    fun inputSurface_providesPassthroughSurfaceAndToggleClearsAndReReportsError() {
        val originalSdkInt = Build.VERSION.SDK_INT
        ReflectionHelpers.setStaticField(
            Build.VERSION::class.java,
            "SDK_INT",
            Build.VERSION_CODES.Q,
        )
        var sessionManager: LowLightBoostSessionManager? = null
        var surfaceRequest: SurfaceRequest? = null
        try {
            // Arrange: a session manager that runs synchronously on the test thread.
            sessionManager =
                LowLightBoostSessionManager(
                    CameraXExecutors.directExecutor(),
                    Handler(getMainLooper()),
                    rendererFactory = { FakeGlRenderer() },
                )
            val processor =
                PlayServicesLowLightBoostSurfaceProcessor(context, { null }, sessionManager)
            val effect = LowLightBoostEffect(CameraEffect.PREVIEW, processor)
            val resolution = Size(1920, 1080)
            surfaceRequest =
                SurfaceRequest(resolution, FakeCamera("0"), DynamicRange.SDR, FRAME_RATE_RANGE) {}
            val events = mutableListOf<String>()
            effect.addListener(
                object : LowLightBoostEffect.Listener {
                    override fun onLowLightBoostEnabledChanged(enabled: Boolean) {
                        events.add("enabled:$enabled")
                    }

                    override fun onStatusChanged(status: Int) {
                        events.add("status:$status")
                    }

                    override fun onErrorChanged(error: Exception?) {
                        events.add("error:${error?.javaClass?.simpleName}")
                    }
                }
            )
            // Act: bind while enabled -> reports UnsupportedOperationException.
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

            assertThat(surfaceRequest.deferrableSurface.surface.isDone).isTrue()
            val initialError = effect.error
            assertThat(effect.status).isEqualTo(LowLightBoostEffect.STATUS_OFF)
            assertThat(initialError).isInstanceOf(UnsupportedOperationException::class.java)
            assertThat(initialError).hasMessageThat().contains("not supported on this device")
            assertThat(sessionManager.getCurrentSession()).isNull()

            // Disabling clears the error to (STATUS_OFF, null).
            effect.isLowLightBoostEnabled = false
            shadowOf(getMainLooper()).idle()
            assertThat(effect.status).isEqualTo(LowLightBoostEffect.STATUS_OFF)
            assertThat(effect.error).isNull()

            // Re-enabling reports the same UnsupportedOperationException again.
            effect.isLowLightBoostEnabled = true
            shadowOf(getMainLooper()).idle()
            assertThat(effect.status).isEqualTo(LowLightBoostEffect.STATUS_OFF)
            assertThat(effect.error).isSameInstanceAs(initialError)

            // Back-to-back false -> true clears error and reports it again. Because glExecutor is
            // directExecutor() here, onBoostEnabledChanged(false) runs synchronously before = true.
            effect.isLowLightBoostEnabled = false
            effect.isLowLightBoostEnabled = true
            shadowOf(getMainLooper()).idle()
            assertThat(effect.error).isSameInstanceAs(initialError)

            assertThat(events)
                .containsExactly(
                    "error:UnsupportedOperationException",
                    "enabled:false",
                    "error:null",
                    "enabled:true",
                    "error:UnsupportedOperationException",
                    "enabled:false",
                    "error:null",
                    "enabled:true",
                    "error:UnsupportedOperationException",
                )
                .inOrder()
        } finally {
            sessionManager?.destroySessionInternal()
            surfaceRequest?.deferrableSurface?.close()
            shadowOf(getMainLooper()).idle()
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", originalSdkInt)
        }
    }

    @Config(sdk = [Config.TARGET_SDK])
    @Test
    fun boundWhileDisabledBelowApi30_keepsNullErrorUntilEnabled() {
        val originalSdkInt = Build.VERSION.SDK_INT
        ReflectionHelpers.setStaticField(
            Build.VERSION::class.java,
            "SDK_INT",
            Build.VERSION_CODES.Q,
        )
        var sessionManager: LowLightBoostSessionManager? = null
        var surfaceRequest: SurfaceRequest? = null
        try {
            sessionManager =
                LowLightBoostSessionManager(
                    CameraXExecutors.directExecutor(),
                    Handler(getMainLooper()),
                    rendererFactory = { FakeGlRenderer() },
                )
            val processor =
                PlayServicesLowLightBoostSurfaceProcessor(context, { null }, sessionManager)
            val effect = LowLightBoostEffect(CameraEffect.PREVIEW, processor)
            surfaceRequest =
                SurfaceRequest(
                    Size(1920, 1080),
                    FakeCamera("0"),
                    DynamicRange.SDR,
                    FRAME_RATE_RANGE,
                ) {}
            effect.isLowLightBoostEnabled = false
            sessionManager.bindInputSurfaceRequest(
                processor,
                surfaceRequest,
                surfaceRequest.camera.cameraInfoInternal,
                "0",
            )
            sessionManager.provideInputSurfaceAndPrepareSession(
                processor = processor,
                surfaceRequest = surfaceRequest,
                resolution = Size(1920, 1080),
                cameraId = "0",
            )
            shadowOf(getMainLooper()).idle()

            assertThat(effect.status).isEqualTo(LowLightBoostEffect.STATUS_OFF)
            assertThat(effect.error).isNull()

            effect.isLowLightBoostEnabled = true
            shadowOf(getMainLooper()).idle()
            assertThat(effect.status).isEqualTo(LowLightBoostEffect.STATUS_OFF)
            assertThat(effect.error).isInstanceOf(UnsupportedOperationException::class.java)
        } finally {
            sessionManager?.destroySessionInternal()
            surfaceRequest?.deferrableSurface?.close()
            shadowOf(getMainLooper()).idle()
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", originalSdkInt)
        }
    }

    @Config(sdk = [Config.TARGET_SDK])
    @Test
    fun postingExecutorBelowApi30_backToBackToggleClearsAndReReportsError() {
        val originalSdkInt = Build.VERSION.SDK_INT
        ReflectionHelpers.setStaticField(
            Build.VERSION::class.java,
            "SDK_INT",
            Build.VERSION_CODES.Q,
        )
        var sessionManager: LowLightBoostSessionManager? = null
        var surfaceRequest: SurfaceRequest? = null
        try {
            val handler = Handler(getMainLooper())
            sessionManager =
                LowLightBoostSessionManager(
                    CameraXExecutors.newHandlerExecutor(handler),
                    handler,
                    rendererFactory = { FakeGlRenderer() },
                )
            val processor =
                PlayServicesLowLightBoostSurfaceProcessor(context, { null }, sessionManager)
            val effect = LowLightBoostEffect(CameraEffect.PREVIEW, processor)
            surfaceRequest =
                SurfaceRequest(
                    Size(1920, 1080),
                    FakeCamera("0"),
                    DynamicRange.SDR,
                    FRAME_RATE_RANGE,
                ) {}
            val events = mutableListOf<String>()
            effect.addListener(
                object : LowLightBoostEffect.Listener {
                    override fun onLowLightBoostEnabledChanged(enabled: Boolean) {
                        events.add("enabled:$enabled")
                    }

                    override fun onStatusChanged(status: Int) {
                        events.add("status:$status")
                    }

                    override fun onErrorChanged(error: Exception?) {
                        events.add("error:${error?.javaClass?.simpleName}")
                    }
                }
            )
            sessionManager.bindInputSurfaceRequest(
                processor,
                surfaceRequest,
                surfaceRequest.camera.cameraInfoInternal,
                "0",
            )
            sessionManager.provideInputSurfaceAndPrepareSession(
                processor = processor,
                surfaceRequest = surfaceRequest,
                resolution = Size(1920, 1080),
                cameraId = "0",
            )
            shadowOf(getMainLooper()).idle()
            events.clear()

            // Synchronous back-to-back false -> true before glExecutor runs either task.
            effect.isLowLightBoostEnabled = false
            effect.isLowLightBoostEnabled = true
            shadowOf(getMainLooper()).idle()

            assertThat(effect.isLowLightBoostEnabled).isTrue()
            assertThat(effect.error).isInstanceOf(UnsupportedOperationException::class.java)
            assertThat(events)
                .containsExactly(
                    "enabled:false",
                    "enabled:true",
                    "error:null",
                    "error:UnsupportedOperationException",
                )
                .inOrder()
        } finally {
            sessionManager?.destroySessionInternal()
            surfaceRequest?.deferrableSurface?.close()
            shadowOf(getMainLooper()).idle()
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", originalSdkInt)
        }
    }

    private companion object {
        private val FRAME_RATE_RANGE = SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
    }
}
