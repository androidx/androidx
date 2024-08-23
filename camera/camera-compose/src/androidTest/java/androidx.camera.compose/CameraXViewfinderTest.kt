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
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class CameraXViewfinderTest(private val implName: String, private val cameraConfig: CameraXConfig) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName == CameraPipeConfig::class.simpleName,
        )

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    @get:Rule val composeTest = createComposeRule()

    @Test
    fun viewfinderIsDisplayed_withValidSurfaceRequest() = runViewfinderTest {
        composeTest.setContent {
            val currentSurfaceRequest: SurfaceRequest? by surfaceRequests.collectAsState()
            currentSurfaceRequest?.let { surfaceRequest ->
                CameraXViewfinder(
                    surfaceRequest = surfaceRequest,
                    modifier = Modifier.testTag(CAMERAX_VIEWFINDER_TEST_TAG)
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
                    modifier = Modifier.testTag(CAMERAX_VIEWFINDER_TEST_TAG)
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
                modifier = Modifier.testTag(CAMERAX_VIEWFINDER_TEST_TAG)
            )
        }

        composeTest.awaitIdle()

        // Viewfinder should not be displayed since SurfaceRequest was cancelled
        composeTest.onNodeWithTag(CAMERAX_VIEWFINDER_TEST_TAG).assertIsNotDisplayed()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
            )

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
                val preview = Preview.Builder().build()
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
                            preview
                        )
                    }
                }

                with(
                    PreviewTestScope(
                        surfaceRequests = surfaceRequests.asStateFlow(),
                        resetPreviewSurfaceProvider = resetPreviewSurfaceProvider,
                        startCamera = startCamera,
                        coroutineContext = coroutineContext
                    )
                ) {
                    block()
                }
            } finally {
                fakeLifecycleOwner?.apply {
                    withContext(Dispatchers.Main) {
                        pauseAndStop()
                        destroy()
                    }
                }
                withTimeout(30.seconds) { cameraProvider.shutdownAsync().await() }
            }
        }

    private data class PreviewTestScope(
        val surfaceRequests: StateFlow<SurfaceRequest?>,
        val resetPreviewSurfaceProvider: suspend () -> Unit,
        val startCamera: suspend () -> Camera,
        override val coroutineContext: CoroutineContext
    ) : CoroutineScope
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
