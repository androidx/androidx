/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration

import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Build
import android.os.HandlerThread
import android.view.Surface
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.compat.workaround.InactiveSurfaceCloserImpl
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.integration.impl.UseCaseSurfaceManager
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.testing.TestUseCaseCamera
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.DeferrableSurfaces
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.CameraDeviceHolder
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.activity.Camera2TestActivity
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.core.os.HandlerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class UseCaseSurfaceManagerDeviceTest {

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(CameraPipeConfig.defaultConfig())
        )

    private val executor = MoreExecutors.directExecutor()
    private val useCaseThreads by lazy {
        val dispatcher = executor.asCoroutineDispatcher()
        val cameraScope =
            CoroutineScope(
                SupervisorJob() + dispatcher + CoroutineName("UseCaseSurfaceManagerTest")
            )

        UseCaseThreads(cameraScope, executor, dispatcher)
    }

    private lateinit var cameraId: String
    private lateinit var cameraHolder: CameraDeviceHolder
    private lateinit var testSessionParameters: TestSessionParameters
    private lateinit var testUseCaseCamera: TestUseCaseCamera

    @Before
    fun setUp() {
        val cameraIsList = CameraUtil.getBackwardCompatibleCameraIdListOrThrow()
        assumeTrue("Not having a valid Camera for testing", cameraIsList.isNotEmpty())
        cameraId = cameraIsList[0]
    }

    @After
    fun tearDown() = runBlocking {
        if (::testUseCaseCamera.isInitialized) {
            testUseCaseCamera.close().join()
        }
        if (::testSessionParameters.isInitialized) {
            testSessionParameters.cleanup()
        }
        if (::cameraHolder.isInitialized) {
            CameraUtil.releaseCameraDevice(cameraHolder)
            cameraHolder.closedFuture.get(3, TimeUnit.SECONDS)
        }
    }

    @Test
    fun openCloseCameraGraph_deferrableSurfaceUsageCountTest() = runBlocking {
        // Arrange.
        testSessionParameters = TestSessionParameters()
        val useCases =
            listOf(
                createFakeUseCase().apply {
                    setupSessionConfig(testSessionParameters.sessionConfig)
                }
            )

        // Act. Open CameraGraph
        testUseCaseCamera =
            TestUseCaseCamera(
                    context = ApplicationProvider.getApplicationContext(),
                    cameraId = cameraId,
                    useCases = useCases,
                    threads = useCaseThreads,
                )
                .also { it.start() }
        assertThat(testSessionParameters.repeatingOutputDataLatch.await(3, TimeUnit.SECONDS))
            .isTrue()
        val cameraOpenedUsageCount = testSessionParameters.deferrableSurface.useCount
        // Act. close CameraGraph
        testUseCaseCamera.useCaseCameraGraphConfig.graph.close()
        testUseCaseCamera.useCaseSurfaceManager.stopAsync().awaitWithTimeout()
        val cameraClosedUsageCount = testSessionParameters.deferrableSurface.useCount

        // Assert, verify the usage count of the DeferrableSurface
        assertThat(cameraOpenedUsageCount).isEqualTo(2)
        assertThat(cameraClosedUsageCount).isEqualTo(1)
    }

    /**
     * This test launches another (test) Activity with the intention of taking away camera from the
     * test itself. On Android T and above, we listen to onCameraAccessPrioritiesChanged() and
     * retries opening the camera when the camera is disconnected. That means the test activity will
     * no longer deterministically get the final camera access on Android T and above. As such, we
     * set the maximum SDK version to S_V2.
     */
    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.S_V2)
    fun disconnectOpenedCameraGraph_deferrableSurfaceUsageCountTest() = runBlocking {
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())

        // Arrange.
        testSessionParameters = TestSessionParameters()
        val useCases =
            listOf(
                createFakeUseCase().apply {
                    setupSessionConfig(testSessionParameters.sessionConfig)
                }
            )
        testUseCaseCamera =
            TestUseCaseCamera(
                    context = ApplicationProvider.getApplicationContext(),
                    cameraId = cameraId,
                    useCases = useCases,
                    threads = useCaseThreads,
                )
                .also { it.start() }
        val surfaceActiveCountDown = CountDownLatch(1)
        val surfaceInactiveCountDown = CountDownLatch(1)
        testUseCaseCamera.cameraPipe
            .cameraSurfaceManager()
            .addListener(
                object : CameraSurfaceManager.SurfaceListener {
                    override fun onSurfaceActive(surface: Surface) {
                        if (surface == testSessionParameters.surface) {
                            surfaceActiveCountDown.countDown()
                        }
                    }

                    override fun onSurfaceInactive(surface: Surface) {
                        if (surface == testSessionParameters.surface) {
                            surfaceInactiveCountDown.countDown()
                        }
                    }
                }
            )
        assertThat(surfaceActiveCountDown.await(3, TimeUnit.SECONDS)).isTrue()
        val cameraOpenedUsageCount = testSessionParameters.deferrableSurface.useCount

        // Act. Launch Camera2TestActivity to open the camera and wait until it is ready.
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), Camera2TestActivity::class.java)
                .apply { putExtra(Camera2TestActivity.EXTRA_CAMERA_ID, cameraId) }

        var completionIdlingResource: CountingIdlingResource? = null
        var wasPreviewReady = false
        try {
            ActivityScenario.launch<Camera2TestActivity>(intent).use { scenario ->
                var previewStartedIdlingResource: CountingIdlingResource? = null
                scenario.onActivity { activity ->
                    completionIdlingResource = activity.completionIdlingResource
                    previewStartedIdlingResource = activity.previewStartedIdlingResource
                    IdlingRegistry.getInstance().register(completionIdlingResource)
                }
                Espresso.onIdle()
                wasPreviewReady = previewStartedIdlingResource!!.isIdleNow
            }
        } finally {
            completionIdlingResource?.let { IdlingRegistry.getInstance().unregister(it) }
        }

        // Assume/skip the test if the activity failed to start the preview.
        assumeTrue("Camera2TestActivity failed to start preview.", wasPreviewReady)

        // Now that Camera2TestActivity has run and closed, the camera graph should be disconnected.
        // Close the CameraGraph to ensure the usage count goes back down.
        testUseCaseCamera.useCaseCameraGraphConfig.graph.close()
        testUseCaseCamera.useCaseSurfaceManager.stopAsync().awaitWithTimeout()
        assertThat(surfaceInactiveCountDown.await(3, TimeUnit.SECONDS)).isTrue()
        val cameraClosedUsageCount = testSessionParameters.deferrableSurface.useCount

        // Assert, verify the usage count of the DeferrableSurface
        assertThat(cameraOpenedUsageCount).isEqualTo(2)
        assertThat(cameraClosedUsageCount).isEqualTo(1)
    }

    @Test
    fun closingUseCaseSurfaceManagerClosesDeferrableSurface() = runBlocking {
        // Arrange.
        testSessionParameters = TestSessionParameters()
        val useCases =
            listOf(
                createFakeUseCase().apply {
                    setupSessionConfig(testSessionParameters.sessionConfig)
                }
            )

        val context: Context = ApplicationProvider.getApplicationContext()
        val cameraPipe = CameraPipe(CameraPipe.Config(context))
        testUseCaseCamera =
            TestUseCaseCamera(
                    context = context,
                    cameraId = cameraId,
                    useCases = useCases,
                    threads = useCaseThreads,
                    cameraPipe = cameraPipe,
                    useCaseSurfaceManager =
                        UseCaseSurfaceManager(
                            useCaseThreads,
                            cameraPipe,
                            InactiveSurfaceCloserImpl(),
                            SessionConfigAdapter(useCases = useCases),
                        ),
                )
                .also { it.start() }

        // Act.
        testUseCaseCamera.useCaseCameraGraphConfig.graph.close()
        testUseCaseCamera.useCaseSurfaceManager.stopAsync().awaitWithTimeout()

        // Assert, verify the DeferrableSurface is closed.
        assertThat(testSessionParameters.deferrableSurface.isClosed).isTrue()
    }

    private fun createFakeUseCase() =
        object : FakeUseCase(FakeUseCaseConfig.Builder().setTargetName("UseCase").useCaseConfig) {
            fun setupSessionConfig(sessionConfig: SessionConfig) {
                updateSessionConfig(listOf(sessionConfig))
                notifyActive()
            }
        }

    private suspend fun <T> Deferred<T>.awaitWithTimeout(
        timeMillis: Long = TimeUnit.SECONDS.toMillis(5)
    ) = withTimeout(timeMillis) { await() }

    private class TestSessionParameters(name: String = "TestSessionParameters") {
        /** Thread for all asynchronous calls. */
        private val handlerThread: HandlerThread = HandlerThread(name).apply { start() }

        /** Image reader that unlocks the latch waiting for the first image data to appear. */
        private val onImageAvailableListener =
            ImageReader.OnImageAvailableListener { reader ->
                reader.acquireNextImage()?.let { image ->
                    image.close()
                    repeatingOutputDataLatch.countDown()
                }
            }
        private val imageReader: ImageReader =
            ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2).apply {
                setOnImageAvailableListener(
                    onImageAvailableListener,
                    HandlerCompat.createAsync(handlerThread.looper),
                )
            }

        val surface: Surface = imageReader.surface

        val deferrableSurface: DeferrableSurface =
            ImmediateSurface(surface).also { DeferrableSurfaces.incrementAll(listOf(it)) }

        /** Latch to wait for first image data to appear. */
        val repeatingOutputDataLatch = CountDownLatch(1)

        val sessionConfig: SessionConfig =
            SessionConfig.Builder()
                .apply {
                    setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                    addSurface(deferrableSurface)
                    val camera2ConfigBuilder: Camera2ImplConfig.Builder =
                        Camera2ImplConfig.Builder()
                    // Add capture request options for SessionConfig
                    camera2ConfigBuilder
                        .setCaptureRequestOption<Int>(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_AUTO,
                        )
                        .setCaptureRequestOption<Int>(
                            CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON,
                        )
                    addImplementationOptions(camera2ConfigBuilder.build())
                }
                .build()

        /** Clean up resources. */
        fun cleanup() {
            DeferrableSurfaces.decrementAll(listOf(deferrableSurface))
            deferrableSurface.close()
            imageReader.close()
            handlerThread.quitSafely()
        }
    }
}
