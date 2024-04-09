/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.integration.core

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.util.Range
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.adapter.awaitUntil
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.integration.core.util.CameraPipeUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests for checking if a capture option is submitted from end-to-end.
 *
 * Usually, these tests only check if the corresponding capture request is submitted properly by
 * checking the [CaptureRequest] with `Camera2Interop` capture callback. This does not mean the
 * framework will always honor these capture request options, so we don't usually care about the
 * result. If the [TotalCaptureResult] or response by camera also need to be verified for some
 * specific cases, we may need to have additional considerations and will probably vary in a
 * case-to-case basis.
 *
 * TODO: Will probably be better to use CameraController whenever possible to increase the scope.
 */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class CaptureOptionSubmissionTest(
    private val selectorName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraConfig: CameraXConfig
) {
    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraConfig)
    )

    @get:Rule
    val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    // Capture callback added to session, so only a repeating capture callback, not non-repeating
    private lateinit var sessionCaptureCallback: CaptureCallback

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        sessionCaptureCallback = CaptureCallback()

        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
        }
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
            }
        }
    }

    /*
     * Only testing if a supported FPS range is submitted to camera in [CaptureRequest] without
     * caring about the result. This test basically checks if [Preview.Builder.setTargetFrameRate]
     * works properly.
     */

    @Test
    fun canSubmitSupportedAeTargetFpsRanges_whenTargetFrameRateSetToPreviewOnly() = runBlocking {
        assumeTrue(
            "TODO(b/331900702): Enable when the bug is fixed at camera-pipe",
            implName != CameraPipeConfig::class.simpleName
        )

        assumeTrue(
            "TODO(b/332235883): Enable for legacy when the bug is resolved",
            !isHwLevelLegacy()
        )

        // At least 2 FPS ranges should be checked as the submitted range may just be from template
        getSupportedFpsRanges().forEach { targetFpsRange ->
            if (targetFpsRange.upper > 30) {
                // TODO: b/332464740 - High FPS may not be supported as per stream config map
                return@forEach
            }

            var lastSubmittedFpsRange: Range<Int>? = null
            val result = sessionCaptureCallback.verify { captureRequest, _ ->
                captureRequest[CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE]?.let {
                    lastSubmittedFpsRange = it
                }
                captureRequest[CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE] == targetFpsRange
            }

            bindUseCases(listOf(Preview.Builder().setTargetFrameRate(targetFpsRange)))

            assertWithMessage(
                "Test failed for targetFpsRange = $targetFpsRange" +
                    ", lastSubmittedFpsRange = $lastSubmittedFpsRange"
            ).that(result.awaitUntil(timeoutMillis = 10000)).isTrue()

            unbindAllUseCases()
        }
    }

    @Test
    fun canSubmitSupportedAeTargetFpsRanges_whenTargetFrameRateSetToVideoCaptureOnly() =
        runBlocking {
            assumeTrue(
                "TODO(b/331900702): Enable when the bug is fixed at camera-pipe",
                implName != CameraPipeConfig::class.simpleName
            )

            assumeTrue(
                "TODO(b/332235883): Enable for legacy when the bug is resolved",
                !isHwLevelLegacy()
            )

            // At least 2 FPS ranges should be checked as the submitted range may be from template
            getSupportedFpsRanges().forEach { targetFpsRange ->
                if (targetFpsRange.upper > 30) {
                    // TODO: b/332464740 - High FPS may not be supported as per stream config map
                    return@forEach
                }

                var lastSubmittedFpsRange: Range<Int>? = null
                val result = sessionCaptureCallback.verify { captureRequest, _ ->
                    captureRequest[CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE]?.let {
                        lastSubmittedFpsRange = it
                    }
                    captureRequest[CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE] == targetFpsRange
                }

                bindUseCases(
                    listOf(
                        VideoCapture.Builder(Recorder.Builder().build())
                            .setTargetFrameRate(targetFpsRange),
                        // TODO: b/333365764 - Remove extra Preview use case added to avoid capture
                        //  session failure due to MediaCodec error
                        Preview.Builder()
                    )
                )

                assertWithMessage(
                    "Test failed for targetFpsRange = $targetFpsRange" +
                        ", lastSubmittedFpsRange = $lastSubmittedFpsRange"
                ).that(result.awaitUntil(timeoutMillis = 10000)).isTrue()

                unbindAllUseCases()
            }
        }

    // TODO: b/332464991 - Add a FPS test adding different FPS ranges to Preview & VideoCapture

    // TODO - Adds tests to check capture option is consistent for both non-repeating and repeating
    //  captures. E.g., FPS range is not submitted for non-repeating capture right now. But this
    //  will probably require us to add Camera2Interop callback for non-repeating captures as well,
    //  something that comes up every now and then, although low priority.

    private fun getSupportedFpsRanges(): Array<Range<Int>> {
        val cameraCharacteristics = CameraUtil.getCameraCharacteristics(cameraSelector.lensFacing!!)
        Assume.assumeNotNull(cameraCharacteristics)

        val fpsRanges = cameraCharacteristics!!.get(CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
        Assume.assumeNotNull(fpsRanges)

        return fpsRanges!!
    }

    private fun isHwLevelLegacy(): Boolean {
        val cameraCharacteristics = CameraUtil.getCameraCharacteristics(cameraSelector.lensFacing!!)
        Assume.assumeNotNull(cameraCharacteristics)

        val hwLevel = cameraCharacteristics!!.get(INFO_SUPPORTED_HARDWARE_LEVEL)
        Assume.assumeNotNull(hwLevel)

        return hwLevel == INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    }

    private suspend fun bindUseCases(
        useCaseBuilders: List<UseCaseConfig.Builder<*, *, *>> = listOf(Preview.Builder())
    ) {
        if (useCaseBuilders.isEmpty()) {
            return
        }

        withContext(Dispatchers.Main) {
            val useCases = mutableListOf<UseCase>()

            useCaseBuilders.forEachIndexed { index, builder ->
                useCases.add(builder.also {
                    if (index == 0) { // adding to just one use case is enough
                        CameraPipeUtil.setCameraCaptureSessionCallback(
                            implName,
                            it,
                            sessionCaptureCallback
                        )
                    }
                }.build().apply {
                    if (this is Preview) {
                        setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
                    }
                })
            }

            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                cameraSelector,
                *useCases.toTypedArray()
            )
        }
    }

    private suspend fun unbindAllUseCases() {
        withContext(Dispatchers.Main) {
            cameraProvider.unbindAll()
        }
    }

    class CaptureCallback : CameraCaptureSession.CaptureCallback() {
        data class Verification(
            val condition: (
                captureRequest: CaptureRequest,
                captureResult: TotalCaptureResult
            ) -> Boolean,
            val isVerified: CompletableDeferred<Unit>
        )

        private var pendingVerifications = mutableListOf<Verification>()

        /**
         * Returns a [Deferred] representing if verification has been completed
         */
        fun verify(
            condition: (
                captureRequest: CaptureRequest,
                captureResult: TotalCaptureResult
            ) -> Boolean = { _, _ -> false },
        ): Deferred<Unit> = CompletableDeferred<Unit>().apply {
            val verification = Verification(condition, this)
            pendingVerifications.add(verification)

            invokeOnCompletion {
                pendingVerifications.remove(verification)
            }
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            pendingVerifications.forEach {
                if (it.condition(request, result)) {
                    it.isVerified.complete(Unit)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "selector={0},config={2}")
        fun data() = listOf(
            arrayOf(
                "back",
                CameraSelector.DEFAULT_BACK_CAMERA,
                Camera2Config::class.simpleName,
                Camera2Config.defaultConfig()
            ),
            arrayOf(
                "back",
                CameraSelector.DEFAULT_BACK_CAMERA,
                CameraPipeConfig::class.simpleName,
                CameraPipeConfig.defaultConfig()
            ),
            // front camera is not important with the current test, but may be required in future
        )
    }
}
