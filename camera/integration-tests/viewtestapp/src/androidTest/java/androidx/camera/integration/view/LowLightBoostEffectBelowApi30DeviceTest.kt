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

package androidx.camera.integration.view

import android.content.Context
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.effects.playservices.lowlight.LowLightBoostEffect
import androidx.camera.effects.playservices.lowlight.LowLightBoostException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Verifies [LowLightBoostEffect] below Android 11 (API level 30), where Low Light Boost isn't
 * supported: camera frames are passed through, [LowLightBoostEffect.isSupported] returns false, and
 * a [LowLightBoostException] with [LowLightBoostException.ERROR_NOT_SUPPORTED] is reported via
 * [LowLightBoostEffect.error] with [LowLightBoostEffect.STATUS_OFF]. Doesn't use the Low Light
 * Boost test UI, which requires API level 30.
 */
@LargeTest
@RunWith(JUnit4::class)
@SdkSuppress(maxSdkVersion = Build.VERSION_CODES.Q)
class LowLightBoostEffectBelowApi30DeviceTest {

    @get:Rule
    val useCameraRule: TestRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    // Counted down for each preview frame of the latest binding.
    private val frameLatch = AtomicReference(CountDownLatch(0))
    // Identifies the latest binding, so late frames of a previous binding aren't counted.
    private val bindGeneration = AtomicInteger()

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
        // Like the other Low Light Boost device tests, use the app's default CameraX configuration.
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        instrumentation.runOnMainSync {
            lifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
        }
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            instrumentation.runOnMainSync { cameraProvider.unbindAll() }
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun preview_passesFramesThroughAndReportsError() {
        verifyPassthrough(CameraEffect.PREVIEW, withVideoCapture = false)
    }

    @Test
    fun previewAndVideoCapture_passesFramesThroughAndReportsError() {
        verifyPassthrough(
            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
            withVideoCapture = true,
        )
    }

    private fun verifyPassthrough(targets: Int, withVideoCapture: Boolean) {
        // Arrange.
        val cameraInfo = cameraProvider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)
        if (withVideoCapture) {
            assumeTrue(
                "Camera does not support SDR video recording",
                Recorder.getVideoCapabilities(cameraInfo)
                    .getSupportedQualities(DynamicRange.SDR)
                    .isNotEmpty(),
            )
        }
        assertThat(LowLightBoostEffect.isSupportedAsync(context, cameraInfo)[10, TimeUnit.SECONDS])
            .isFalse()
        val effect = LowLightBoostEffect(context, targets)
        assertThat(effect.isLowLightBoostEnabled).isTrue()

        val errorLatch = AtomicReference(CountDownLatch(1))
        val errorClearedLatch = AtomicReference(CountDownLatch(1))
        val disabledLatch = AtomicReference(CountDownLatch(1))
        val enabledLatch = AtomicReference(CountDownLatch(1))
        val reportedError = AtomicReference<LowLightBoostException?>()
        val enabledEvents = Collections.synchronizedList(mutableListOf<Boolean>())
        val errorEvents = Collections.synchronizedList(mutableListOf<LowLightBoostException?>())
        val listener =
            object : LowLightBoostEffect.Listener {
                override fun onLowLightBoostEnabledChanged(enabled: Boolean) {
                    enabledEvents.add(enabled)
                    if (enabled) {
                        enabledLatch.get().countDown()
                    } else {
                        disabledLatch.get().countDown()
                    }
                }

                override fun onErrorChanged(error: LowLightBoostException?) {
                    errorEvents.add(error)
                    if (error != null) {
                        reportedError.set(error)
                        errorLatch.get().countDown()
                    } else {
                        errorClearedLatch.get().countDown()
                    }
                }
            }
        effect.addListener(listener)
        // The (isLowLightBoostEnabled, status, error) snapshots that observe reports.
        val observedStates =
            Collections.synchronizedList(
                mutableListOf<Triple<Boolean, Int, LowLightBoostException?>>()
            )
        val observeScope = CoroutineScope(Dispatchers.Main.immediate + Job())
        try {
            instrumentation.runOnMainSync {
                observeScope.launch {
                    effect.observe {
                        observedStates.add(Triple(isLowLightBoostEnabled, status, error))
                    }
                }
            }
            assertThat(observedStates)
                .containsExactly(Triple(true, LowLightBoostEffect.STATUS_OFF, null))
            // Act & Assert: frames are passed through, and the error is reported.
            bind(effect, withVideoCapture)
            assertFramesArrive("after binding")
            assertWithMessage("The error wasn't reported")
                .that(errorLatch.get().await(10, TimeUnit.SECONDS))
                .isTrue()
            assertUnsupportedError(effect, reportedError, observedStates)

            // Act & Assert: disabling clears the error and notifies listeners and observe.
            errorClearedLatch.set(CountDownLatch(1))
            disabledLatch.set(CountDownLatch(1))
            enabledEvents.clear()
            errorEvents.clear()
            effect.isLowLightBoostEnabled = false
            assertThat(effect.isLowLightBoostEnabled).isFalse()
            assertWithMessage("Disabling didn't notify onLowLightBoostEnabledChanged(false)")
                .that(disabledLatch.get().await(10, TimeUnit.SECONDS))
                .isTrue()
            assertWithMessage("Disabling didn't clear the error")
                .that(errorClearedLatch.get().await(10, TimeUnit.SECONDS))
                .isTrue()
            assertClearedState(effect, observedStates)
            assertFramesArrive("after disabling Low Light Boost")

            // Act & Assert: re-enabling reports the error again while frames continue to arrive.
            errorLatch.set(CountDownLatch(1))
            enabledLatch.set(CountDownLatch(1))
            effect.isLowLightBoostEnabled = true
            assertThat(effect.isLowLightBoostEnabled).isTrue()
            assertWithMessage("Re-enabling didn't notify onLowLightBoostEnabledChanged(true)")
                .that(enabledLatch.get().await(10, TimeUnit.SECONDS))
                .isTrue()
            assertWithMessage("Re-enabling didn't report the error")
                .that(errorLatch.get().await(10, TimeUnit.SECONDS))
                .isTrue()
            assertUnsupportedError(effect, reportedError, observedStates)
            assertFramesArrive("after re-enabling Low Light Boost")

            // Act & Assert: back-to-back false -> true clears error and reports it again.
            enabledEvents.clear()
            errorEvents.clear()
            disabledLatch.set(CountDownLatch(1))
            enabledLatch.set(CountDownLatch(1))
            errorClearedLatch.set(CountDownLatch(1))
            errorLatch.set(CountDownLatch(1))
            effect.isLowLightBoostEnabled = false
            effect.isLowLightBoostEnabled = true
            assertWithMessage("Back-to-back toggle didn't fire false callback")
                .that(disabledLatch.get().await(10, TimeUnit.SECONDS))
                .isTrue()
            assertWithMessage("Back-to-back toggle didn't fire true callback")
                .that(enabledLatch.get().await(10, TimeUnit.SECONDS))
                .isTrue()
            assertWithMessage("Back-to-back toggle didn't clear error")
                .that(errorClearedLatch.get().await(10, TimeUnit.SECONDS))
                .isTrue()
            assertWithMessage("Back-to-back toggle didn't re-report error")
                .that(errorLatch.get().await(10, TimeUnit.SECONDS))
                .isTrue()
            assertThat(enabledEvents).containsExactly(false, true).inOrder()
            assertThat(errorEvents).hasSize(2)
            assertThat(errorEvents[0]).isNull()
            assertThat(errorEvents[1]?.errorCode)
                .isEqualTo(LowLightBoostException.ERROR_NOT_SUPPORTED)
            assertUnsupportedError(effect, reportedError, observedStates)
            assertFramesArrive("after back-to-back toggling Low Light Boost")

            // Act & Assert: the effect can be bound again.
            instrumentation.runOnMainSync { cameraProvider.unbindAll() }
            errorLatch.set(CountDownLatch(1))
            bind(effect, withVideoCapture)
            assertFramesArrive("after binding again")
            assertWithMessage("Rebinding didn't report the error")
                .that(errorLatch.get().await(10, TimeUnit.SECONDS))
                .isTrue()
            assertUnsupportedError(effect, reportedError, observedStates)
        } finally {
            observeScope.cancel()
            effect.removeListener(listener)
        }
    }

    /**
     * Asserts on the main thread that [effect] and the latest observed state reflect a cleared
     * error with STATUS_OFF and `isLowLightBoostEnabled == false`.
     */
    private fun assertClearedState(
        effect: LowLightBoostEffect,
        observedStates: List<Triple<Boolean, Int, LowLightBoostException?>>,
    ) {
        var enabled: Boolean? = null
        var status: Int? = null
        var currentError: LowLightBoostException? =
            LowLightBoostException(LowLightBoostException.ERROR_UNKNOWN, "sentinel")
        var latestObservedState: Triple<Boolean, Int, LowLightBoostException?>? = null
        instrumentation.runOnMainSync {
            enabled = effect.isLowLightBoostEnabled
            status = effect.status
            currentError = effect.error
            latestObservedState = observedStates.lastOrNull()
        }
        assertThat(enabled).isFalse()
        assertThat(status).isEqualTo(LowLightBoostEffect.STATUS_OFF)
        assertThat(currentError).isNull()
        assertWithMessage("observe didn't report cleared state")
            .that(latestObservedState)
            .isEqualTo(Triple(false, LowLightBoostEffect.STATUS_OFF, null))
    }

    /**
     * Asserts on the main thread that [effect] and the latest observed state match [reportedError]
     * with STATUS_OFF and `isLowLightBoostEnabled == true`.
     */
    private fun assertUnsupportedError(
        effect: LowLightBoostEffect,
        reportedError: AtomicReference<LowLightBoostException?>,
        observedStates: List<Triple<Boolean, Int, LowLightBoostException?>>,
    ) {
        var enabled: Boolean? = null
        var status: Int? = null
        var currentError: LowLightBoostException? = null
        var latestListenerError: LowLightBoostException? = null
        var latestObservedState: Triple<Boolean, Int, LowLightBoostException?>? = null
        // Runs on the main thread so listener, effect, and observe snapshots come from the same
        // committed state after already-posted transitions are delivered.
        instrumentation.runOnMainSync {
            enabled = effect.isLowLightBoostEnabled
            status = effect.status
            currentError = effect.error
            latestListenerError = reportedError.get()
            latestObservedState = observedStates.lastOrNull()
        }
        assertThat(enabled).isTrue()
        assertThat(status).isEqualTo(LowLightBoostEffect.STATUS_OFF)
        assertThat(currentError?.errorCode).isEqualTo(LowLightBoostException.ERROR_NOT_SUPPORTED)
        assertThat(latestListenerError).isSameInstanceAs(currentError)
        assertWithMessage("observe didn't report the error")
            .that(latestObservedState)
            .isEqualTo(Triple(true, LowLightBoostEffect.STATUS_OFF, currentError))
    }

    /** Binds a Preview, and a VideoCapture if [withVideoCapture], with [effect]. */
    private fun bind(effect: LowLightBoostEffect, withVideoCapture: Boolean) {
        val generation = bindGeneration.incrementAndGet()
        instrumentation.runOnMainSync {
            val preview =
                Preview.Builder().build().apply {
                    setSurfaceProvider(
                        SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                            if (bindGeneration.get() == generation) {
                                frameLatch.get().countDown()
                            }
                        }
                    )
                }
            val useCaseGroup = UseCaseGroup.Builder().addUseCase(preview).addEffect(effect)
            if (withVideoCapture) {
                useCaseGroup.addUseCase(VideoCapture.withOutput(Recorder.Builder().build()))
            }
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCaseGroup.build(),
            )
        }
    }

    private fun assertFramesArrive(phase: String) {
        val latch = CountDownLatch(FRAME_COUNT)
        frameLatch.set(latch)
        assertWithMessage("Preview frames didn't arrive $phase")
            .that(latch.await(10, TimeUnit.SECONDS))
            .isTrue()
    }

    private companion object {
        private const val FRAME_COUNT = 10
    }
}
