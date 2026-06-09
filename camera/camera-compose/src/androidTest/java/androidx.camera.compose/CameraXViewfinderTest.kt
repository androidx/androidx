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

package androidx.camera.compose

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.concurrent.futures.await
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class CameraXViewfinderTest(private val implName: String, private val cameraConfig: CameraXConfig) {
    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    @get:Rule val composeTest = createComposeRule(StandardTestDispatcher())

    @Test
    fun viewfinderIsDisplayed_withValidSurfaceRequest() = runViewfinderTest {
        composeTest.setContent {
            val currentSurfaceRequest: SurfaceRequest? by surfaceRequests.collectAsState()
            currentSurfaceRequest?.let { surfaceRequest ->
                CameraXViewfinder(
                    surfaceRequest = surfaceRequest,
                    modifier = Modifier.testTag(CAMERAX_VIEWFINDER_TEST_TAG),
                )
            }
        }

        // Start the camera
        startCamera()

        // Wait for first SurfaceRequest
        surfaceRequests.filterNotNull().first()

        composeTest.awaitIdle()

        // CameraXViewfinder should now have a child Viewfinder
        composeTest
            .onNodeWithTag(CAMERAX_VIEWFINDER_TEST_TAG)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.hasChild())

        ensureCameraIsStreaming()
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun changingImplementation_sendsNewSurfaceRequest() = runViewfinderTest {
        var implementationMode: ImplementationMode by mutableStateOf(ImplementationMode.EXTERNAL)
        composeTest.setContent {
            val currentSurfaceRequest: SurfaceRequest? by surfaceRequests.collectAsState()
            currentSurfaceRequest?.let { surfaceRequest ->
                CameraXViewfinder(
                    surfaceRequest = surfaceRequest,
                    implementationMode = implementationMode,
                    modifier = Modifier.testTag(CAMERAX_VIEWFINDER_TEST_TAG),
                )
            }
        }

        // Collect expected number of SurfaceRequests for 2 mode changes
        val surfaceRequestSequence = surfaceRequests.filterNotNull().take(3).produceIn(this)

        // Start the camera
        startCamera()

        // Swap implementation modes twice to produce 3 SurfaceRequests
        val allSurfaceRequests = buildList {
            for (surfaceRequest in surfaceRequestSequence) {
                add(surfaceRequest)
                composeTest.awaitIdle()

                if (!surfaceRequestSequence.isClosedForReceive) {
                    // Changing the implementation mode will invalidate the previous SurfaceRequest
                    // and cause Preview to send a new SurfaceRequest
                    implementationMode = implementationMode.swapMode()
                    composeTest.awaitIdle()
                }
            }
        }

        assertThat(allSurfaceRequests.size).isEqualTo(3)
        assertThat(allSurfaceRequests).containsNoDuplicates()
    }

    @Test
    fun cancelledSurfaceRequest_doesNotInstantiateViewfinder() = runViewfinderTest {
        // Start the camera
        startCamera()

        // Wait for first SurfaceRequest
        val surfaceRequest = surfaceRequests.filterNotNull().first()

        // Reset surface provider to cause cancellation of the last SurfaceRequest
        resetPreviewSurfaceProvider()

        // Ensure the SurfaceRequest is cancelled
        surfaceRequest.awaitCancellation()

        // Pass on cancelled SurfaceRequest to CameraXViewfinder
        composeTest.setContent {
            CameraXViewfinder(
                surfaceRequest = surfaceRequest,
                modifier = Modifier.testTag(CAMERAX_VIEWFINDER_TEST_TAG),
            )
        }

        composeTest.awaitIdle()

        // Viewfinder should not be displayed since SurfaceRequest was cancelled
        composeTest.onNodeWithTag(CAMERAX_VIEWFINDER_TEST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun removingViewfinder_andAddingWithSameSurfaceRequest_recovers() = runViewfinderTest {
        val showViewfinderContent = mutableStateOf(true)
        composeTest.setContent {
            var showContent by remember { showViewfinderContent }
            val currentSurfaceRequest: SurfaceRequest? by surfaceRequests.collectAsState()
            if (showContent) {
                currentSurfaceRequest?.let { surfaceRequest ->
                    CameraXViewfinder(
                        surfaceRequest = surfaceRequest,
                        modifier = Modifier.testTag(CAMERAX_VIEWFINDER_TEST_TAG),
                    )
                }
            }
        }

        // Start the camera
        startCamera()

        // Wait for first SurfaceRequest
        val firstSurfaceRequest = surfaceRequests.filterNotNull().first()

        composeTest.awaitIdle()

        // CameraXViewfinder should now have a child Viewfinder
        composeTest
            .onNodeWithTag(CAMERAX_VIEWFINDER_TEST_TAG)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.hasChild())

        ensureCameraIsStreaming()

        // Remove the Viewfinder from the composition
        showViewfinderContent.value = false

        composeTest.waitUntil(timeoutMillis = 5000) {
            composeTest.onNodeWithTag(CAMERAX_VIEWFINDER_TEST_TAG).isNotDisplayed()
        }

        // Add the Viewfinder back to the composition
        showViewfinderContent.value = true

        composeTest.awaitIdle()

        // CameraXViewfinder should now be displayed with a child viewfinder
        composeTest
            .onNodeWithTag(CAMERAX_VIEWFINDER_TEST_TAG)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.hasChild())

        val newSurfaceRequest =
            withTimeoutOrNull(timeout = 5.seconds) {
                surfaceRequests.filterNotNull().first { it != firstSurfaceRequest }
            }

        ensureCameraIsStreaming()

        // A new surface request should have been created since the old one was invalidated
        assertThat(newSurfaceRequest).isNotNull()
    }

    @SdkSuppress(minSdkVersion = 24) // b/441562610
    @Test
    fun movableContentOf_recoversAfterMove() = runViewfinderTest {
        val moveViewfinderContent = mutableStateOf(false)
        composeTest.setContent {
            val currentSurfaceRequest: SurfaceRequest? by surfaceRequests.collectAsState()

            Column {
                val content = remember {
                    movableContentOf {
                        currentSurfaceRequest?.let { surfaceRequest ->
                            CameraXViewfinder(
                                surfaceRequest = surfaceRequest,
                                implementationMode = ImplementationMode.EXTERNAL,
                                modifier = Modifier.testTag(CAMERAX_VIEWFINDER_TEST_TAG),
                            )
                        }
                    }
                }

                var moveContent by remember { moveViewfinderContent }

                if (moveContent) {
                    content()
                } else {
                    content()
                }
            }
        }

        // Start the camera
        startCamera()

        // Wait for first SurfaceRequest
        surfaceRequests.filterNotNull().first()

        composeTest.awaitIdle()

        // CameraXViewfinder should now have a child Viewfinder
        composeTest
            .onNodeWithTag(CAMERAX_VIEWFINDER_TEST_TAG)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.hasChild())

        ensureCameraIsStreaming()

        // Move the content
        moveViewfinderContent.value = true

        composeTest.awaitIdle()

        // CameraXViewfinder should still have a child Viewfinder
        composeTest
            .onNodeWithTag(CAMERAX_VIEWFINDER_TEST_TAG)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.hasChild())

        ensureCameraIsStreaming()
    }

    @Test
    fun streamState_transitionsToStreamingAndBackToIdle_whenRequestRemoved() = runViewfinderTest {
        assertStreamStateTest {
            // Scenario: Pipeline Reconfiguration (Request removed)
            setSurfaceRequest(null)
        }
    }

    @Test
    fun streamState_transitionsToIdle_whenRequestInvalidated() = runViewfinderTest {
        assertStreamStateTest { currentRequest ->
            // Scenario: Pipeline Reconfiguration (Invalidation)
            withContext(Dispatchers.Main) { currentRequest?.invalidate() }
        }
    }

    @Test
    fun streamState_transitionsToIdle_whenCameraStopped() = runViewfinderTest {
        assertStreamStateTest {
            // Scenario: Hardware Shutdown (Camera stopped/unbound)
            stopCamera()
        }
    }

    @Test
    fun streamState_transitionsToIdle_whenDisposed() = runViewfinderTest {
        var showViewfinder by mutableStateOf(true)
        assertStreamStateTest(shouldShowViewfinder = { showViewfinder }) {
            // Scenario: UI Detachment (Disposal)
            showViewfinder = false
        }
    }

    @Test
    fun streamState_transitionsToIdle_whenLifecycleStopped() = runViewfinderTest {
        assertStreamStateTest {
            // Scenario: Hardware Shutdown (Lifecycle STOPPED)
            val startTime = TimeSource.Monotonic.markNow()
            pauseLifecycle()

            // Verifying transition to IDLE
            composeTest.waitUntil(timeoutMillis = 5000) {
                currentStreamState.get() == Preview.STREAM_STATE_IDLE
            }
            val elapsed = startTime.elapsedNow()

            // Should be immediate (less than 1s) even if device close is delayed
            assertThat(elapsed).isLessThan(1.seconds)
        }
    }

    @Test
    fun streamState_recoversToStreaming_whenLifecycleResumed() = runViewfinderTest {
        assertStreamStateTest {
            // Pause lifecycle and wait for IDLE
            pauseLifecycle()
            composeTest.waitUntil(timeoutMillis = 5000) {
                currentStreamState.get() == Preview.STREAM_STATE_IDLE
            }

            // Resume lifecycle
            resumeLifecycle()

            // Verifying transition back to STREAMING
            composeTest.waitUntil(timeoutMillis = 10000) {
                currentStreamState.get() == Preview.STREAM_STATE_STREAMING
            }

            // Finally trigger IDLE for assertStreamStateTest to complete
            stopCamera()
        }
    }

    private suspend fun PreviewTestScope.assertStreamStateTest(
        shouldShowViewfinder: @Composable () -> Boolean = { true },
        block: suspend PreviewTestScope.(currentRequest: SurfaceRequest?) -> Unit,
    ) {
        var currentRequest: SurfaceRequest? = null
        composeTest.setContent {
            if (shouldShowViewfinder()) {
                val currentSurfaceRequest: SurfaceRequest? by surfaceRequests.collectAsState()
                currentSurfaceRequest?.let { surfaceRequest ->
                    currentRequest = surfaceRequest
                    CameraXViewfinder(
                        surfaceRequest = surfaceRequest,
                        onStreamStateChanged = { currentStreamState.set(it) },
                        modifier = Modifier.testTag(CAMERAX_VIEWFINDER_TEST_TAG),
                    )
                }
            }
        }

        // Wait for composition
        composeTest.awaitIdle()
        assertThat(currentStreamState.get()).isEqualTo(Preview.STREAM_STATE_IDLE)

        // Start the camera
        startCamera()

        // Wait for state to become STREAMING
        composeTest.waitUntil(timeoutMillis = 10000) {
            currentStreamState.get() == Preview.STREAM_STATE_STREAMING
        }

        block(currentRequest)

        // Wait for state to become IDLE (if not already handled in block)
        composeTest.waitUntil(timeoutMillis = 10000) {
            currentStreamState.get() == Preview.STREAM_STATE_IDLE
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()))

        private const val CAMERAX_VIEWFINDER_TEST_TAG = "CameraXViewfinderTestTag"
    }

    private inline fun runViewfinderTest(crossinline block: suspend PreviewTestScope.() -> Unit) =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val cameraProvider =
                withTimeout(10.seconds) {
                    ProcessCameraProvider.configureInstance(cameraConfig)
                    ProcessCameraProvider.getInstance(context).await()
                }

            var fakeLifecycleOwner: FakeLifecycleOwner? = null
            try {
                val latestDeliveredFrameNumber = MutableStateFlow(-1L)
                val preview =
                    Preview.Builder()
                        .also {
                            Camera2Interop.Extender(it)
                                .setSessionCaptureCallback(
                                    object : CameraCaptureSession.CaptureCallback() {
                                        override fun onCaptureCompleted(
                                            session: CameraCaptureSession,
                                            request: CaptureRequest,
                                            result: TotalCaptureResult,
                                        ) {
                                            super.onCaptureCompleted(session, request, result)
                                            latestDeliveredFrameNumber.value = result.frameNumber
                                        }
                                    }
                                )
                        }
                        .build()

                val surfaceRequests = MutableStateFlow<SurfaceRequest?>(null)
                val resetPreviewSurfaceProvider =
                    suspend {
                            withContext(Dispatchers.Main) {
                                // Reset the surface provider to a new lambda that will continue to
                                // publish to surfaceRequests
                                preview.setSurfaceProvider { surfaceRequest ->
                                    surfaceRequests.value = surfaceRequest
                                }
                            }
                        }
                        .also { it.invoke() }

                val startCamera = suspend {
                    withContext(Dispatchers.Main) {
                        val lifecycleOwner =
                            FakeLifecycleOwner().apply {
                                startAndResume()
                                fakeLifecycleOwner = this
                            }

                        val firstAvailableCameraSelector =
                            cameraProvider.availableCameraInfos
                                .asSequence()
                                .map { it.cameraSelector }
                                .first()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            firstAvailableCameraSelector,
                            preview,
                        )
                    }
                }

                val stopCamera = suspend {
                    withContext(Dispatchers.Main) {
                        fakeLifecycleOwner?.apply {
                            if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                                pauseAndStop()
                            }
                            if (lifecycle.currentState == Lifecycle.State.STARTED) {
                                stop()
                            }
                            if (lifecycle.currentState == Lifecycle.State.CREATED) {
                                destroy()
                            }
                        }
                        fakeLifecycleOwner = null
                        cameraProvider.unbindAll()
                    }
                }

                val pauseLifecycle = suspend {
                    withContext(Dispatchers.Main) {
                        fakeLifecycleOwner?.apply {
                            if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                                pauseAndStop()
                            } else if (lifecycle.currentState == Lifecycle.State.STARTED) {
                                stop()
                            }
                        }
                    }
                    Unit
                }

                val resumeLifecycle = suspend {
                    withContext(Dispatchers.Main) {
                        if (
                            fakeLifecycleOwner?.lifecycle?.currentState == Lifecycle.State.CREATED
                        ) {
                            fakeLifecycleOwner?.startAndResume()
                        } else if (
                            fakeLifecycleOwner?.lifecycle?.currentState == Lifecycle.State.STARTED
                        ) {
                            fakeLifecycleOwner?.start()
                        }
                    }
                    Unit
                }

                with(
                    PreviewTestScope(
                        surfaceRequests = surfaceRequests.asStateFlow(),
                        setSurfaceRequest = { surfaceRequests.value = it },
                        resetPreviewSurfaceProvider = resetPreviewSurfaceProvider,
                        startCamera = startCamera,
                        stopCamera = stopCamera,
                        pauseLifecycle = pauseLifecycle,
                        resumeLifecycle = resumeLifecycle,
                        coroutineContext = coroutineContext,
                        lastFrames = latestDeliveredFrameNumber.asStateFlow(),
                    )
                ) {
                    block()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    fakeLifecycleOwner?.apply {
                        if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                            pauseAndStop()
                        }
                        if (lifecycle.currentState == Lifecycle.State.STARTED) {
                            stop()
                        }
                        if (lifecycle.currentState == Lifecycle.State.CREATED) {
                            destroy()
                        }
                    }
                }
                withTimeout(30.seconds) { cameraProvider.shutdownAsync().await() }
            }
        }

    private data class PreviewTestScope(
        val surfaceRequests: StateFlow<SurfaceRequest?>,
        val setSurfaceRequest: (SurfaceRequest?) -> Unit,
        val resetPreviewSurfaceProvider: suspend () -> Unit,
        val startCamera: suspend () -> Camera,
        val stopCamera: suspend () -> Unit,
        val pauseLifecycle: suspend () -> Unit,
        val resumeLifecycle: suspend () -> Unit,
        override val coroutineContext: CoroutineContext,
        private val lastFrames: StateFlow<Long>,
        val currentStreamState: AtomicInteger = AtomicInteger(Preview.STREAM_STATE_IDLE),
    ) : CoroutineScope {
        suspend fun ensureCameraIsStreaming(timeout: Duration = 5.seconds) {
            withTimeout(timeout) { lastFrames.take(NUM_FRAMES_TO_WAIT_FOR).collect {} }
        }

        companion object {
            private const val NUM_FRAMES_TO_WAIT_FOR = 10
        }
    }
}

private fun ImplementationMode.swapMode(): ImplementationMode {
    return when (this) {
        ImplementationMode.EXTERNAL -> ImplementationMode.EMBEDDED
        ImplementationMode.EMBEDDED -> ImplementationMode.EXTERNAL
    }
}

private fun SemanticsMatcher.Companion.hasChild() =
    SemanticsMatcher("Has child") { node -> node.children.isNotEmpty() }

private suspend fun SurfaceRequest.awaitCancellation(): Unit = suspendCancellableCoroutine { cont ->
    addRequestCancellationListener(Runnable::run) { cont.resume(Unit) }
}
