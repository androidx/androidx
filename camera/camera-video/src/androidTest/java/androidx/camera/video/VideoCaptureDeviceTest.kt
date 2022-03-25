/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.MutableStateObservable
import androidx.camera.core.impl.Observable
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.GLUtil
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.fail
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class VideoCaptureDeviceTest {

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var cameraInfo: CameraInfo

    @Before
    fun setUp() {
        CameraXUtil.initialize(
            context,
            Camera2Config.defaultConfig()
        ).get()

        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
        cameraInfo = cameraUseCaseAdapter.cameraInfo
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraUseCaseAdapter.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraUseCaseAdapter.apply {
                    removeUseCases(useCases)
                }
            }
        }

        val timeout = 10.seconds
        withTimeoutOrNull(timeout) {
            CameraXUtil.shutdown().await() ?: "Shutdown succeeded."
        } ?: fail("Timed out waiting for CameraX to shutdown. Waited $timeout.")
    }

    @Test
    fun addUseCases_canReceiveFrame(): Unit = runBlocking {
        // Arrange.
        val videoOutput = TestVideoOutput()
        val videoCapture = VideoCapture.withOutput(videoOutput)

        // Act.
        withContext(Dispatchers.Main) {
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
        }

        // Assert.
        val surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
        val frameCountFlow = surfaceRequest.provideUpdatingSurface()
        val timeout = 10.seconds
        withTimeoutOrNull(timeout) {
            frameCountFlow.takeWhile { frameCount -> frameCount <= 5 }.last()
        } ?: fail("Timed out waiting for `frameCount >= 5`. Waited $timeout.")
    }

    @Test
    fun changeStreamState_canReceiveFrame(): Unit = runBlocking {
        // Arrange.
        val videoOutput =
            TestVideoOutput(
                streamInfo = StreamInfo.of(
                    StreamInfo.STREAM_ID_ANY,
                    StreamInfo.StreamState.INACTIVE
                )
            )
        val videoCapture = VideoCapture.withOutput(videoOutput)

        // Act.
        withContext(Dispatchers.Main) {
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
        }

        // Assert.
        val surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
        val frameCountFlow = surfaceRequest.provideUpdatingSurface()
        // No frame should be updated by INACTIVE state
        val expectedTimeout = 2.seconds
        withTimeoutOrNull(expectedTimeout) {
            // assertThat should never run since timeout should occur, but if it does,
            // we'll get a nicer error message.
            assertThat(frameCountFlow.dropWhile { frameCount -> frameCount < 1 }
                .first()).isAtMost(0)
        }

        // Act.
        videoOutput.setStreamInfo(
            StreamInfo.of(
                StreamInfo.STREAM_ID_ANY,
                StreamInfo.StreamState.ACTIVE
            )
        )

        // Assert.
        val timeout = 10.seconds
        withTimeoutOrNull(timeout) {
            frameCountFlow.take(5).last()
        } ?: fail("Timed out waiting for 5 frame updates. Waited $timeout.")
    }

    @Test
    fun addUseCases_setSupportedQuality_getCorrectResolution() = runBlocking {
        assumeTrue(QualitySelector.getSupportedQualities(cameraInfo).isNotEmpty())
        // Cuttlefish API 29 has inconsistent resolution issue. See b/184015059.
        assumeFalse(Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29)

        // Arrange.
        val qualityList = QualitySelector.getSupportedQualities(cameraInfo)
        qualityList.forEach loop@{ quality ->
            val targetResolution = QualitySelector.getResolution(cameraInfo, quality)
            val videoOutput = TestVideoOutput(
                mediaSpec = MediaSpec.builder().configureVideo {
                    it.setQualitySelector(QualitySelector.from(quality))
                }.build()
            )
            val videoCapture = VideoCapture.withOutput(videoOutput)

            // Act.
            if (!cameraUseCaseAdapter.isUseCasesCombinationSupported(videoCapture)) {
                return@loop
            }
            withContext(Dispatchers.Main) {
                cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
            }

            // Assert.
            val surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
            assertWithMessage("Set quality value by $quality")
                .that(surfaceRequest.resolution).isEqualTo(targetResolution)

            // Cleanup.
            withContext(Dispatchers.Main) {
                cameraUseCaseAdapter.apply {
                    removeUseCases(listOf(videoCapture))
                }
            }
        }
    }

    @Test
    fun addUseCases_setQualityWithRotation_getCorrectResolution() = runBlocking {
        assumeTrue(QualitySelector.getSupportedQualities(cameraInfo).isNotEmpty())
        // Cuttlefish API 29 has inconsistent resolution issue. See b/184015059.
        assumeFalse(Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29)

        val targetResolution = QualitySelector.getResolution(cameraInfo, Quality.LOWEST)

        arrayOf(
            Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270
        ).forEach { rotation ->
            // Arrange.
            val videoOutput = TestVideoOutput(
                mediaSpec = MediaSpec.builder().configureVideo {
                    it.setQualitySelector(QualitySelector.from(Quality.LOWEST))
                }.build()
            )
            val videoCapture = VideoCapture.withOutput(videoOutput)

            // Act.
            withContext(Dispatchers.Main) {
                cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
            }

            // Assert.
            val surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
            assertWithMessage("Set rotation value by $rotation")
                .that(surfaceRequest.resolution).isEqualTo(targetResolution)

            // Cleanup.
            withContext(Dispatchers.Main) {
                cameraUseCaseAdapter.apply {
                    removeUseCases(listOf(videoCapture))
                }
            }
        }
    }

    @Test
    fun useCaseCanBeReused(): Unit = runBlocking {
        // Arrange.
        val videoOutput = TestVideoOutput()
        val videoCapture = VideoCapture.withOutput(videoOutput)

        // Act.
        withContext(Dispatchers.Main) {
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
        }

        // Assert.
        var surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
        var frameCountFlow = surfaceRequest.provideUpdatingSurface()

        val timeout = 10.seconds
        withTimeoutOrNull(timeout) {
            frameCountFlow.takeWhile { frameCount -> frameCount <= 5 }.last()
        } ?: fail("Timed out waiting for `frameCount >= 5`. Waited $timeout.")

        // Act.
        // Reuse use case
        withContext(Dispatchers.Main) {
            cameraUseCaseAdapter.apply {
                removeUseCases(listOf(videoCapture))
            }
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
        }

        // Assert.
        surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
        frameCountFlow = surfaceRequest.provideUpdatingSurface()
        withTimeoutOrNull(timeout) {
            frameCountFlow.takeWhile { frameCount -> frameCount <= 5 }.last()
        } ?: fail("Timed out waiting for `frameCount >= 5`. Waited $timeout.")
    }

    private class TestVideoOutput(
        streamInfo: StreamInfo = StreamInfo.of(
            StreamInfo.STREAM_ID_ANY,
            StreamInfo.StreamState.ACTIVE
        ),
        mediaSpec: MediaSpec = MediaSpec.builder().build()
    ) : VideoOutput {
        private val surfaceRequests = ArrayBlockingQueue<SurfaceRequest>(10)

        private val streamInfoObservable: MutableStateObservable<StreamInfo> =
            MutableStateObservable.withInitialState(streamInfo)

        private val mediaSpecObservable: MutableStateObservable<MediaSpec> =
            MutableStateObservable.withInitialState(mediaSpec)

        override fun onSurfaceRequested(surfaceRequest: SurfaceRequest) {
            surfaceRequests.put(surfaceRequest)
        }

        override fun getStreamInfo(): Observable<StreamInfo> = streamInfoObservable

        override fun getMediaSpec(): Observable<MediaSpec> = mediaSpecObservable

        fun nextSurfaceRequest(timeout: Long, timeUnit: TimeUnit): SurfaceRequest {
            return surfaceRequests.poll(timeout, timeUnit)
        }

        fun setStreamInfo(streamInfo: StreamInfo) = streamInfoObservable.setState(streamInfo)

        fun setMediaSpec(mediaSpec: MediaSpec) = mediaSpecObservable.setState(mediaSpec)
    }

    private suspend fun SurfaceRequest.provideUpdatingSurface(): StateFlow<Int> {
        var isReleased = false
        val frameCountFlow = MutableStateFlow(0)
        val executor = Executors.newFixedThreadPool(1)

        val surfaceTexture = withContext(executor.asCoroutineDispatcher()) {
            SurfaceTexture(0).apply {
                setDefaultBufferSize(640, 480)
                detachFromGLContext()
                attachToGLContext(GLUtil.getTexIdFromGLContext())
                setOnFrameAvailableListener {
                    frameCountFlow.getAndUpdate { frameCount -> frameCount + 1 }
                    executor.execute {
                        if (!isReleased) {
                            updateTexImage()
                        }
                    }
                }
            }
        }
        val surface = Surface(surfaceTexture)

        provideSurface(surface, executor) {
            surfaceTexture.release()
            surface.release()
            executor.shutdown()
            isReleased = true
        }

        return frameCountFlow.asStateFlow()
    }
}
