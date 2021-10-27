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
import androidx.camera.core.UseCase
import androidx.camera.core.impl.MutableStateObservable
import androidx.camera.core.impl.Observable
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.GLUtil
import androidx.camera.video.QualitySelector.QUALITY_LOWEST
import androidx.camera.video.VideoOutput.StreamState
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class VideoCaptureDeviceTest {

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
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
    fun tearDown() {
        if (this::cameraUseCaseAdapter.isInitialized) {
            instrumentation.runOnMainSync {
                cameraUseCaseAdapter.apply {
                    removeUseCases(useCases)
                }
            }
        }
        CameraXUtil.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun addUseCases_canReceiveFrame() = runBlocking {
        // Arrange.
        val videoOutput = TestVideoOutput()
        val videoCapture = VideoCapture.withOutput(videoOutput)

        // Act.
        instrumentation.runOnMainSync {
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
        }

        // Assert.
        val surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
        val frameUpdateSemaphore = surfaceRequest.provideUpdatingSurface()
        assertThat(frameUpdateSemaphore.tryAcquire(5, 10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun changeStreamState_canReceiveFrame() = runBlocking {
        // Arrange.
        val videoOutput = TestVideoOutput(streamState = StreamState.INACTIVE)
        val videoCapture = VideoCapture.withOutput(videoOutput)

        // Act.
        instrumentation.runOnMainSync {
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
        }

        // Assert.
        val surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
        val frameUpdateSemaphore = surfaceRequest.provideUpdatingSurface()
        // No frame should be updated by INACTIVE state
        assertThat(frameUpdateSemaphore.tryAcquire(1, 2, TimeUnit.SECONDS)).isFalse()

        // Act.
        videoOutput.setStreamState(StreamState.ACTIVE)

        // Assert.
        assertThat(frameUpdateSemaphore.tryAcquire(5, 10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun addUseCases_setSupportedQuality_getCorrectResolution() {
        assumeTrue(QualitySelector.getSupportedQualities(cameraInfo).isNotEmpty())
        // Cuttlefish API 29 has inconsistent resolution issue. See b/184015059.
        assumeFalse(Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29)

        // Arrange.
        val qualityList = QualitySelector.getSupportedQualities(cameraInfo)
        qualityList.forEach loop@{ quality ->
            val targetResolution = QualitySelector.getResolution(cameraInfo, quality)
            val videoOutput = TestVideoOutput(
                mediaSpec = MediaSpec.builder().configureVideo {
                    it.setQualitySelector(QualitySelector.of(quality))
                }.build()
            )
            val videoCapture = VideoCapture.withOutput(videoOutput)

            // Act.
            if (!checkUseCasesCombinationSupported(videoCapture)) {
                return@loop
            }
            instrumentation.runOnMainSync {
                cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
            }

            // Assert.
            val surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
            assertWithMessage("Set quality value by $quality")
                .that(surfaceRequest.resolution).isEqualTo(targetResolution)

            // Cleanup.
            instrumentation.runOnMainSync {
                cameraUseCaseAdapter.apply {
                    removeUseCases(listOf(videoCapture))
                }
            }
        }
    }

    @Test
    fun addUseCases_setQualityWithRotation_getCorrectResolution() {
        assumeTrue(QualitySelector.getSupportedQualities(cameraInfo).isNotEmpty())
        // Cuttlefish API 29 has inconsistent resolution issue. See b/184015059.
        assumeFalse(Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29)

        val targetResolution = QualitySelector.getResolution(cameraInfo, QUALITY_LOWEST)

        arrayOf(
            Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180, Surface.ROTATION_270
        ).forEach { rotation ->
            // Arrange.
            val videoOutput = TestVideoOutput(
                mediaSpec = MediaSpec.builder().configureVideo {
                    it.setQualitySelector(QualitySelector.of(QUALITY_LOWEST))
                }.build()
            )
            val videoCapture = VideoCapture.withOutput(videoOutput)

            // Act.
            instrumentation.runOnMainSync {
                cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
            }

            // Assert.
            val surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
            assertWithMessage("Set rotation value by $rotation")
                .that(surfaceRequest.resolution).isEqualTo(targetResolution)

            // Cleanup.
            instrumentation.runOnMainSync {
                cameraUseCaseAdapter.apply {
                    removeUseCases(listOf(videoCapture))
                }
            }
        }
    }

    @Test
    fun useCaseCanBeReused() = runBlocking {
        // Arrange.
        val videoOutput = TestVideoOutput()
        val videoCapture = VideoCapture.withOutput(videoOutput)

        // Act.
        instrumentation.runOnMainSync {
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
        }

        // Assert.
        var surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
        var frameUpdateSemaphore = surfaceRequest.provideUpdatingSurface()
        assertThat(frameUpdateSemaphore.tryAcquire(5, 10, TimeUnit.SECONDS)).isTrue()

        // Act.
        // Reuse use case
        instrumentation.runOnMainSync {
            cameraUseCaseAdapter.apply {
                removeUseCases(listOf(videoCapture))
            }
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
        }

        // Assert.
        surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
        frameUpdateSemaphore = surfaceRequest.provideUpdatingSurface()
        assertThat(frameUpdateSemaphore.tryAcquire(5, 10, TimeUnit.SECONDS)).isTrue()
    }

    private class TestVideoOutput(
        streamState: StreamState = StreamState.ACTIVE,
        mediaSpec: MediaSpec = MediaSpec.builder().build()
    ) : VideoOutput {
        private val surfaceRequests = ArrayBlockingQueue<SurfaceRequest>(10)

        private val streamStateObservable: MutableStateObservable<StreamState> =
            MutableStateObservable.withInitialState(streamState)

        private val mediaSpecObservable: MutableStateObservable<MediaSpec> =
            MutableStateObservable.withInitialState(mediaSpec)

        override fun onSurfaceRequested(surfaceRequest: SurfaceRequest) {
            surfaceRequests.put(surfaceRequest)
        }

        override fun getStreamState(): Observable<StreamState> = streamStateObservable

        override fun getMediaSpec(): Observable<MediaSpec> = mediaSpecObservable

        fun nextSurfaceRequest(timeout: Long, timeUnit: TimeUnit): SurfaceRequest {
            return surfaceRequests.poll(timeout, timeUnit)
        }

        fun setStreamState(streamState: StreamState) = streamStateObservable.setState(streamState)

        fun setMediaSpec(mediaSpec: MediaSpec) = mediaSpecObservable.setState(mediaSpec)
    }

    private suspend fun SurfaceRequest.provideUpdatingSurface(): Semaphore {
        var isReleased = false
        val frameUpdateSemaphore = Semaphore(0)
        val executor = Executors.newFixedThreadPool(1)

        val surfaceTexture = withContext(executor.asCoroutineDispatcher()) {
            SurfaceTexture(0).apply {
                setDefaultBufferSize(640, 480)
                detachFromGLContext()
                attachToGLContext(GLUtil.getTexIdFromGLContext())
                setOnFrameAvailableListener {
                    frameUpdateSemaphore.release()
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

        return frameUpdateSemaphore
    }

    private fun checkUseCasesCombinationSupported(vararg useCases: UseCase): Boolean {
        return try {
            cameraUseCaseAdapter.checkAttachUseCases(listOf(*useCases))
            true
        } catch (e: CameraUseCaseAdapter.CameraException) {
            false
        }
    }
}
