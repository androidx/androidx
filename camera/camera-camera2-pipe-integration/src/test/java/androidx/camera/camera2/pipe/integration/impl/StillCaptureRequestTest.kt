/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.os.Build
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.CaptureConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.adapter.ZslControlNoOpImpl
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpTemplateParamsOverride
import androidx.camera.camera2.pipe.integration.compat.workaround.NotUseFlashModeTorchFor3aUpdate
import androidx.camera.camera2.pipe.integration.compat.workaround.NotUseTorchAsFlash
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraph
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraphSession
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeState3AControlCreator
import androidx.camera.camera2.pipe.integration.testing.FakeSurface
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraRequestControl
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeRequestFailure
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.CaptureConfig
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class StillCaptureRequestTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    private val fakeUseCaseThreads by lazy {
        UseCaseThreads(testScope, testDispatcher.asExecutor(), testDispatcher)
    }
    private val fakeCameraProperties = FakeCameraProperties()
    private val fakeSurface = FakeSurface()

    private lateinit var fakeCameraGraphSession: FakeCameraGraphSession
    private lateinit var fakeCameraGraph: FakeCameraGraph
    private lateinit var fakeUseCaseGraphConfig: UseCaseGraphConfig

    private lateinit var fakeConfigAdapter: CaptureConfigAdapter
    private lateinit var fakeUseCaseCameraState: UseCaseCameraState

    private val fakeState3AControl: State3AControl =
        FakeState3AControlCreator.createState3AControl(
            requestControl = FakeUseCaseCameraRequestControl()
        )

    private lateinit var useCaseCameraRequestControl: UseCaseCameraRequestControl

    private val stillCaptureRequestControl =
        StillCaptureRequestControl(
            FlashControl(
                fakeCameraProperties,
                fakeState3AControl,
                fakeUseCaseThreads,
                TorchControl(fakeCameraProperties, fakeState3AControl, fakeUseCaseThreads),
                NotUseFlashModeTorchFor3aUpdate,
            ),
            fakeUseCaseThreads
        )

    private val captureConfigList =
        listOf(
            CaptureConfig.Builder().apply { addSurface(fakeSurface) }.build(),
            CaptureConfig.Builder().apply { addSurface(fakeSurface) }.build()
        )

    @Before
    fun setUp() {
        stillCaptureRequestControl.setNewRequestControl()
    }

    @After
    fun tearDown() {
        fakeSurface.close()
    }

    @Test
    fun captureRequestsSubmitted_whenCameraIsSet() =
        runTest(testDispatcher) {
            stillCaptureRequestControl.issueCaptureRequests()

            advanceUntilIdle()
            assertThat(fakeCameraGraphSession.submittedRequests.size)
                .isEqualTo(captureConfigList.size)
        }

    @Test
    fun captureRequestsNotSubmitted_whenCameraIsNull() =
        runTest(testDispatcher) {
            stillCaptureRequestControl.requestControl = null

            stillCaptureRequestControl.issueCaptureRequests()

            advanceUntilIdle()
            assertThat(fakeCameraGraphSession.submittedRequests.size).isEqualTo(0)
        }

    @Test
    fun captureRequestsSubmittedAfterCameraIsAvailable_whenCameraIsNull() =
        runTest(testDispatcher) {
            stillCaptureRequestControl.requestControl = null

            stillCaptureRequestControl.issueCaptureRequests()
            advanceUntilIdle()

            // new camera is attached
            stillCaptureRequestControl.setNewRequestControl()

            // the previous request should be submitted in the new camera
            advanceUntilIdle()
            assertThat(fakeCameraGraphSession.submittedRequests.size)
                .isEqualTo(captureConfigList.size)
        }

    @Test
    fun captureRequestsComplete_onTotalCaptureOfAllRequests(): Unit =
        runTest(testDispatcher) {
            val requestFuture = stillCaptureRequestControl.issueCaptureRequests()

            advanceUntilIdle()
            assumeTrue(fakeCameraGraphSession.submittedRequests.size == captureConfigList.size)

            fakeCameraGraphSession.submittedRequests.forEach { request ->
                request.listeners.forEach { listener ->
                    listener.onTotalCaptureResult(
                        FakeRequestMetadata(),
                        FrameNumber(0),
                        FakeFrameInfo()
                    )
                }
            }

            advanceUntilIdle()
            requestFuture.completes()
        }

    @Test
    fun captureRequestsFailWithTimeout_onTotalCaptureOfSomeRequests(): Unit =
        runTest(testDispatcher) {
            val requestFuture = stillCaptureRequestControl.issueCaptureRequests()

            advanceUntilIdle()
            assumeTrue(fakeCameraGraphSession.submittedRequests.size == captureConfigList.size)

            fakeCameraGraphSession.submittedRequests.first().let { request ->
                request.listeners.forEach { listener ->
                    listener.onTotalCaptureResult(
                        FakeRequestMetadata(),
                        FrameNumber(0),
                        FakeFrameInfo()
                    )
                }
            }

            advanceUntilIdle()
            requestFuture.failsWithTimeout()
        }

    @Test
    fun captureRequestsFailWithCaptureFailedError_onFailed(): Unit =
        runTest(testDispatcher) {
            val requestFuture = stillCaptureRequestControl.issueCaptureRequests()
            val fakeRequestMetadata = FakeRequestMetadata()
            val frameNumber = FrameNumber(0)

            advanceUntilIdle()
            assumeTrue(fakeCameraGraphSession.submittedRequests.size == captureConfigList.size)

            fakeCameraGraphSession.submittedRequests.first().let { request ->
                request.listeners.forEach { listener ->
                    listener.onFailed(
                        fakeRequestMetadata,
                        frameNumber,
                        FakeRequestFailure(fakeRequestMetadata, frameNumber)
                    )
                }
            }

            advanceUntilIdle()
            requestFuture.failsWithCaptureFailedError()
        }

    @Test
    fun captureRequestsSubmittedToNextCamera_onAborted(): Unit =
        runTest(testDispatcher) {
            stillCaptureRequestControl.issueCaptureRequests()

            // waits for requests to be submitted before camera is closing
            advanceUntilIdle()
            assumeTrue(fakeCameraGraphSession.submittedRequests.size == captureConfigList.size)

            // simulates previous camera closing and thus reporting onAborted
            fakeCameraGraphSession.submittedRequests.first().let { request ->
                request.listeners.forEach { listener -> listener.onAborted(request) }
            }

            // new camera is attached
            stillCaptureRequestControl.setNewRequestControl()

            // the previous request should be submitted again in the new camera
            advanceUntilIdle()
            assertThat(fakeCameraGraphSession.submittedRequests.size)
                .isEqualTo(captureConfigList.size)
        }

    @Test
    fun captureRequestsNotSubmittedToSameCamera_onAborted(): Unit =
        runTest(testDispatcher) {
            stillCaptureRequestControl.issueCaptureRequests()

            // waits for requests to be submitted before camera is simulated to be closed
            advanceUntilIdle()
            assumeTrue(fakeCameraGraphSession.submittedRequests.size == captureConfigList.size)
            val submittedRequests = fakeCameraGraphSession.submittedRequests.toList()

            // clear previous request submission info
            fakeCameraGraphSession.submittedRequests.clear()

            // simulates onAborted being invoked due to previous camera closing
            submittedRequests.first().let { request ->
                request.listeners.forEach { listener -> listener.onAborted(request) }
            }

            // since new camera has not been set, no other request should have been submitted
            advanceUntilIdle()
            assertThat(fakeCameraGraphSession.submittedRequests.size).isEqualTo(0)
        }

    @Test
    fun captureRequestsSubmittedToNextCamera_whenCameraIsClosed(): Unit =
        runTest(testDispatcher) {
            fakeCameraGraph.close()

            stillCaptureRequestControl.issueCaptureRequests()

            // making sure issuing is attempted before new camera is not attached
            advanceUntilIdle()

            stillCaptureRequestControl.setNewRequestControl()

            // the previous request should be submitted in the new camera
            advanceUntilIdle()
            assertThat(fakeCameraGraphSession.submittedRequests.size)
                .isEqualTo(captureConfigList.size)
        }

    @Test
    fun captureRequestsNotResubmitted_whenNewCameraIsSet() =
        runTest(testDispatcher) {
            stillCaptureRequestControl.issueCaptureRequests()
            advanceUntilIdle()

            // simulates previous camera closing and new camera being set
            stillCaptureRequestControl.setNewRequestControl()

            advanceUntilIdle()
            assertThat(fakeCameraGraphSession.submittedRequests.size).isEqualTo(0)
        }

    @Test
    fun notSubmittedAgain_whenNewCameraIsSetAfterSuccessfullySubmittingPendingRequests() =
        runTest(testDispatcher) {
            stillCaptureRequestControl.issueCaptureRequests()

            // waits for requests to be submitted before camera is closing
            advanceUntilIdle()

            // simulates previous camera closing
            fakeCameraGraphSession.submittedRequests.first().let { request ->
                request.listeners.forEach { listener -> listener.onAborted(request) }
            }

            // new camera is attached
            stillCaptureRequestControl.setNewRequestControl()

            // the previous request should be submitted again in the new camera
            advanceUntilIdle()

            // new camera is attached again
            stillCaptureRequestControl.setNewRequestControl()

            // since the previous request was successful, it should not be submitted again
            assertThat(fakeCameraGraphSession.submittedRequests.size).isEqualTo(0)
        }

    @Test
    fun noPendingRequestRemaining_whenReset() =
        runTest(testDispatcher) {
            // simulate adding to pending list
            stillCaptureRequestControl.requestControl = null
            stillCaptureRequestControl.issueCaptureRequests()
            stillCaptureRequestControl.issueCaptureRequests()

            // reset after all operations are done
            advanceUntilIdle()
            stillCaptureRequestControl.reset()

            // new camera is attached
            stillCaptureRequestControl.setNewRequestControl()

            // if no new request submitted, it should imply all pending requests were cleared
            advanceUntilIdle()
            assertThat(fakeCameraGraphSession.submittedRequests.size).isEqualTo(0)
        }

    @Test
    fun allPendingRequestsAreCancelled_whenReset() =
        runTest(testDispatcher) {
            // simulate adding to pending list
            stillCaptureRequestControl.requestControl = null
            val requestFutures =
                listOf(
                    stillCaptureRequestControl.issueCaptureRequests(),
                    stillCaptureRequestControl.issueCaptureRequests()
                )

            // reset after all operations are done
            advanceUntilIdle()
            stillCaptureRequestControl.reset()

            advanceUntilIdle()
            requestFutures.forEach { it.failsWithCameraClosedError() }
        }

    private fun StillCaptureRequestControl.issueCaptureRequests() =
        issueCaptureRequests(captureConfigList, CAPTURE_MODE_MINIMIZE_LATENCY, FLASH_MODE_OFF)

    private fun <T> ListenableFuture<T>.completes(timeoutMs: Long = 1000) {
        get(timeoutMs, TimeUnit.MILLISECONDS)
    }

    private fun <T> ListenableFuture<T>.failsWithTimeout(timeoutMs: Long = 1000) {
        assertThrows(TimeoutException::class.java) { get(timeoutMs, TimeUnit.MILLISECONDS) }
    }

    private fun <T> ListenableFuture<T>.failsWithCaptureFailedError(timeoutMs: Long = 1000) {
        assertThrows(ExecutionException::class.java) { get(timeoutMs, TimeUnit.MILLISECONDS) }
            .apply {
                assertThat(cause).isInstanceOf(ImageCaptureException::class.java)
                assertThat((cause as ImageCaptureException).imageCaptureError)
                    .isEqualTo(ImageCapture.ERROR_CAPTURE_FAILED)
            }
    }

    private fun <T> ListenableFuture<T>.failsWithCameraClosedError(timeoutMs: Long = 1000) {
        assertThrows(ExecutionException::class.java) { get(timeoutMs, TimeUnit.MILLISECONDS) }
            .apply {
                assertThat(cause).isInstanceOf(ImageCaptureException::class.java)
                assertThat((cause as ImageCaptureException).imageCaptureError)
                    .isEqualTo(ImageCapture.ERROR_CAMERA_CLOSED)
            }
    }

    private fun initUseCaseCameraScopeObjects() {
        fakeCameraGraphSession = FakeCameraGraphSession()
        fakeCameraGraph =
            FakeCameraGraph(
                fakeCameraGraphSession = fakeCameraGraphSession,
            )
        fakeUseCaseGraphConfig =
            UseCaseGraphConfig(
                graph = fakeCameraGraph,
                surfaceToStreamMap = mapOf(fakeSurface to StreamId(0)),
                cameraStateAdapter = CameraStateAdapter(),
            )
        fakeConfigAdapter =
            CaptureConfigAdapter(
                useCaseGraphConfig = fakeUseCaseGraphConfig,
                cameraProperties = fakeCameraProperties,
                zslControl = ZslControlNoOpImpl(),
                threads = fakeUseCaseThreads,
                templateParamsOverride = NoOpTemplateParamsOverride,
            )
        fakeUseCaseCameraState =
            UseCaseCameraState(
                useCaseGraphConfig = fakeUseCaseGraphConfig,
                threads = fakeUseCaseThreads,
                sessionProcessorManager = null,
                templateParamsOverride = NoOpTemplateParamsOverride,
            )
        val torchControl =
            TorchControl(fakeCameraProperties, fakeState3AControl, fakeUseCaseThreads)
        useCaseCameraRequestControl =
            UseCaseCameraRequestControlImpl(
                capturePipeline =
                    CapturePipelineImpl(
                        configAdapter = fakeConfigAdapter,
                        cameraProperties = fakeCameraProperties,
                        requestListener = ComboRequestListener(),
                        threads = fakeUseCaseThreads,
                        torchControl = torchControl,
                        useCaseGraphConfig = fakeUseCaseGraphConfig,
                        useCaseCameraState = fakeUseCaseCameraState,
                        useTorchAsFlash = NotUseTorchAsFlash,
                        sessionProcessorManager = null,
                        flashControl =
                            FlashControl(
                                cameraProperties = fakeCameraProperties,
                                state3AControl = fakeState3AControl,
                                threads = fakeUseCaseThreads,
                                torchControl = torchControl,
                                useFlashModeTorchFor3aUpdate = NotUseFlashModeTorchFor3aUpdate,
                            ),
                    ),
                state = fakeUseCaseCameraState,
                useCaseGraphConfig = fakeUseCaseGraphConfig,
            )
    }

    private fun StillCaptureRequestControl.setNewRequestControl() {
        initUseCaseCameraScopeObjects()
        requestControl = useCaseCameraRequestControl
    }
}
