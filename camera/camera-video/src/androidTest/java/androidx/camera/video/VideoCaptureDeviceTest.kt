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
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.BIT_DEPTH_10_BIT
import androidx.camera.core.DynamicRange.HDR_UNSPECIFIED_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.MutableStateObservable
import androidx.camera.core.impl.Observable
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.GLUtil
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import androidx.camera.video.VideoOutput.SourceState
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.fail
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
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
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class VideoCaptureDeviceTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var cameraInfo: CameraInfoInternal

    @Before
    fun setUp() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )

        CameraXUtil.initialize(context, cameraConfig).get()

        val cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()

        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
        cameraInfo = cameraUseCaseAdapter.cameraInfo as CameraInfoInternal
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraUseCaseAdapter.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraUseCaseAdapter.apply { removeUseCases(useCases) }
            }
        }

        val timeout = 10.seconds
        withTimeoutOrNull(timeout) { CameraXUtil.shutdown().await() ?: "Shutdown succeeded." }
            ?: fail("Timed out waiting for CameraX to shutdown. Waited $timeout.")
    }

    @Test
    fun addUseCases_canReceiveFrame(): Unit = runBlocking {
        // Arrange.
        val videoOutput = createTestVideoOutput()
        val videoCapture = VideoCapture.withOutput(videoOutput)

        // Act.
        withContext(Dispatchers.Main) { cameraUseCaseAdapter.addUseCases(listOf(videoCapture)) }

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
            createTestVideoOutput(
                streamInfo =
                    StreamInfo.of(StreamInfo.STREAM_ID_ANY, StreamInfo.StreamState.INACTIVE)
            )
        val videoCapture = VideoCapture.withOutput(videoOutput)

        // Act.
        withContext(Dispatchers.Main) { cameraUseCaseAdapter.addUseCases(listOf(videoCapture)) }

        // Assert.
        val surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
        val frameCountFlow = surfaceRequest.provideUpdatingSurface()
        // No frame should be updated by INACTIVE state
        val expectedTimeout = 2.seconds
        withTimeoutOrNull(expectedTimeout) {
            // assertThat should never run since timeout should occur, but if it does,
            // we'll get a nicer error message.
            assertThat(frameCountFlow.dropWhile { frameCount -> frameCount < 1 }.first())
                .isAtMost(0)
        }

        // Act.
        videoOutput.setStreamInfo(
            StreamInfo.of(StreamInfo.STREAM_ID_ANY, StreamInfo.StreamState.ACTIVE)
        )

        // Assert.
        val timeout = 10.seconds
        withTimeoutOrNull(timeout) { frameCountFlow.take(5).last() }
            ?: fail("Timed out waiting for 5 frame updates. Waited $timeout.")
    }

    @Test
    fun addUseCases_setSupportedQuality_getCorrectResolution() = runBlocking {
        assumeExtraCroppingQuirk(implName)

        val videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        videoCapabilities.supportedDynamicRanges.forEach { dynamicRange ->
            assumeTrue(videoCapabilities.getSupportedQualities(dynamicRange).isNotEmpty())
            // Cuttlefish API 29 has inconsistent resolution issue. See b/184015059.
            assumeFalse(Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29)

            // Arrange.
            val qualityList = videoCapabilities.getSupportedQualities(dynamicRange)
            qualityList.forEach loop@{ quality ->
                val profile =
                    videoCapabilities.getProfiles(quality, dynamicRange)!!.defaultVideoProfile
                val targetResolution = profile.resolution
                val videoOutput =
                    createTestVideoOutput(
                        mediaSpec =
                            MediaSpec.builder()
                                .configureVideo {
                                    it.setQualitySelector(QualitySelector.from(quality))
                                }
                                .build(),
                        videoCapabilities = videoCapabilities,
                    )

                // Use custom VideoEncoderInfoFinder which always returns default
                // FakeVideoEncoderInfo, which tolerance typical resolutions.
                val videoCapture =
                    VideoCapture.Builder(videoOutput)
                        .setVideoEncoderInfoFinder { FakeVideoEncoderInfo() }
                        .build()

                // Act.
                if (!cameraUseCaseAdapter.isUseCasesCombinationSupported(videoCapture)) {
                    return@loop
                }
                withContext(Dispatchers.Main) {
                    cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
                }

                // Assert.
                assertWithMessage("Set quality value by $quality")
                    .that(videoCapture.attachedSurfaceResolution)
                    .isEqualTo(targetResolution)

                // Cleanup.
                withContext(Dispatchers.Main) {
                    cameraUseCaseAdapter.apply { removeUseCases(listOf(videoCapture)) }
                }
            }
        }
    }

    @Test
    fun useCaseCanBeReused(): Unit = runBlocking {
        // Arrange.
        val videoOutput = createTestVideoOutput()
        val videoCapture = VideoCapture.withOutput(videoOutput)

        // Act.
        withContext(Dispatchers.Main) { cameraUseCaseAdapter.addUseCases(listOf(videoCapture)) }

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
            cameraUseCaseAdapter.apply { removeUseCases(listOf(videoCapture)) }
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture))
        }

        // Assert.
        surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
        frameCountFlow = surfaceRequest.provideUpdatingSurface()
        withTimeoutOrNull(timeout) {
            frameCountFlow.takeWhile { frameCount -> frameCount <= 5 }.last()
        } ?: fail("Timed out waiting for `frameCount >= 5`. Waited $timeout.")
    }

    @Test
    fun activeStreamingVideoCaptureStaysInactive_afterUnbind(): Unit = runBlocking {
        // Arrange.
        val videoOutput =
            createTestVideoOutput(streamInfo = StreamInfo.of(1, StreamInfo.StreamState.ACTIVE))
        val videoCapture = VideoCapture.withOutput(videoOutput)
        val finalSourceState = CompletableDeferred<SourceState>()
        launch {
            val flowScope = this
            val inactiveWaitTimeMs = 2000L
            videoOutput.sourceStateFlow
                .buffer(Channel.UNLIMITED)
                .dropWhile { it != SourceState.INACTIVE } // Drop all states until next INACTIVE
                .collectIndexed { index, value ->
                    // We should not receive any other states besides INACTIVE
                    if (value != SourceState.INACTIVE) {
                        finalSourceState.complete(value)
                        flowScope.cancel()
                        return@collectIndexed
                    }

                    if (index == 0) {
                        launch {
                            // Cancel collection after waiting for a delay after INACTIVE state.
                            delay(inactiveWaitTimeMs)
                            finalSourceState.complete(SourceState.INACTIVE)
                            flowScope.cancel()
                        }
                    }
                }
        }

        withContext(Dispatchers.Main) { cameraUseCaseAdapter.addUseCases(listOf(videoCapture)) }

        // Act.
        val surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
        val frameCountFlow = surfaceRequest.provideUpdatingSurface()

        // Assert.
        // Frames should be streaming
        var timeout = 10.seconds
        withTimeoutOrNull(timeout) {
            frameCountFlow.takeWhile { frameCount -> frameCount <= 5 }.last()
        } ?: fail("Timed out waiting for `frameCount >= 5`. Waited $timeout.")

        // Act.
        // Send a new StreamInfo with inactive stream state to emulate a recording stopping
        videoOutput.setStreamInfo(StreamInfo.of(1, StreamInfo.StreamState.INACTIVE))

        // Detach use case asynchronously with launch rather than synchronously with withContext
        // so VideoCapture.onStateDetach() is in a race with the StreamInfo observable
        launch(Dispatchers.Main) { cameraUseCaseAdapter.removeUseCases(listOf(videoCapture)) }

        // Send a new StreamInfo delayed to emulate resetting the surface of an encoder
        videoOutput.setStreamInfo(
            StreamInfo.of(StreamInfo.STREAM_ID_ANY, StreamInfo.StreamState.INACTIVE)
        )

        // Assert.
        // Final state should be INACTIVE
        timeout = 5.seconds
        withTimeoutOrNull(timeout) {
            assertThat(finalSourceState.await()).isEqualTo(SourceState.INACTIVE)
        } ?: fail("Timed out waiting for INACTIVE state. Waited $timeout.")
    }

    @Test
    fun defaultDynamicRange_isSdr(): Unit = runBlocking {
        testDynamicRangeSelection { selectedDynamicRange ->
            assertThat(selectedDynamicRange).isEqualTo(SDR)
        }
    }

    @SdkSuppress(minSdkVersion = 33) // HLG10 only supported on API 33+
    @Test
    fun dynamicRangeHlg_selectsHlg(): Unit = runBlocking {
        assumeTrue(
            "Device does not support HLG10",
            cameraInfo.supportedDynamicRanges.contains(HLG_10_BIT),
        )

        testDynamicRangeSelection(requestedDynamicRange = HLG_10_BIT) { selectedDynamicRange ->
            assertThat(selectedDynamicRange).isEqualTo(HLG_10_BIT)
        }
    }

    @SdkSuppress(minSdkVersion = 33) // HLG10 only supported on API 33+
    @Test
    fun dynamicRange_isSetInSessionConfig(): Unit = runBlocking {
        assumeTrue(
            "Device does not support HLG10",
            cameraInfo.supportedDynamicRanges.contains(HLG_10_BIT),
        )

        // Arrange.
        val videoOutput = createTestVideoOutput()
        val videoCapture = VideoCapture.Builder(videoOutput).setDynamicRange(HLG_10_BIT).build()

        // Act.
        withContext(Dispatchers.Main) { cameraUseCaseAdapter.addUseCases(listOf(videoCapture)) }

        // Assert.
        // Wait for surface request to ensure session config was attached
        videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
        val outputConfig = videoCapture.sessionConfig.outputConfigs.first()
        assertThat(outputConfig.dynamicRange).isEqualTo(HLG_10_BIT)
    }

    @SdkSuppress(minSdkVersion = 33) // 10-bit HDR only supported on API 33+
    @Test
    fun dynamicRangeHdrUnspecified10Bit_selectsHdr10Bit(): Unit = runBlocking {
        val supported10BitDynamicRanges =
            cameraInfo.supportedDynamicRanges.filter { it.bitDepth == BIT_DEPTH_10_BIT }
        assumeFalse(
            "Device does not support any 10-bit dynamic ranges",
            supported10BitDynamicRanges.isEmpty(),
        )

        testDynamicRangeSelection(requestedDynamicRange = HDR_UNSPECIFIED_10_BIT) {
            selectedDynamicRange ->
            assertThat(selectedDynamicRange).isIn(supported10BitDynamicRanges)
        }
    }

    @SdkSuppress(minSdkVersion = 33) // 10-bit HDR only supported on API 33+
    @Test
    fun dynamicRangeHlg_selectsAndAppliesHlgForConcurrentPreview(): Unit = runBlocking {
        assumeTrue(
            "Device does not support HLG10",
            cameraInfo.supportedDynamicRanges.contains(HLG_10_BIT),
        )

        // Arrange.
        val videoOutput = createTestVideoOutput()
        val videoCapture = VideoCapture.Builder(videoOutput).setDynamicRange(HLG_10_BIT).build()
        // Preview will derive dynamic range from VideoCapture since it uses
        // DynamicRange.UNSPECIFIED by default.
        val preview = Preview.Builder().build()

        assumeTrue(cameraUseCaseAdapter.isUseCasesCombinationSupported(videoCapture, preview))

        // Act.
        val deferredSurfaceRequest = CompletableDeferred<SurfaceRequest>()
        withContext(Dispatchers.Main) {
            // SurfaceProvider will run on main thread
            preview.setSurfaceProvider { deferredSurfaceRequest.complete(it) }
            cameraUseCaseAdapter.addUseCases(listOf(videoCapture, preview))
        }

        // Assert.
        val timeout = 5.seconds
        val previewSurfaceRequest =
            withTimeoutOrNull(timeout) { deferredSurfaceRequest.await() }
                ?: fail("Timed out waiting for Preview SurfaceRequest. Waited $timeout.")
        val previewOutputConfig = preview.sessionConfig.outputConfigs.first()
        assertThat(previewSurfaceRequest.dynamicRange).isEqualTo(HLG_10_BIT)
        assertThat(previewOutputConfig.dynamicRange).isEqualTo(HLG_10_BIT)
    }

    private suspend fun testDynamicRangeSelection(
        requestedDynamicRange: DynamicRange? = null,
        assertBlock: (selectedDynamicRange: DynamicRange) -> Unit,
    ) {
        // Arrange.
        val videoOutput = createTestVideoOutput()
        val videoCapture =
            VideoCapture.Builder(videoOutput)
                .apply { requestedDynamicRange?.let { setDynamicRange(requestedDynamicRange) } }
                .build()

        // Act.
        withContext(Dispatchers.Main) { cameraUseCaseAdapter.addUseCases(listOf(videoCapture)) }

        // Assert.
        val surfaceRequest = videoOutput.nextSurfaceRequest(5, TimeUnit.SECONDS)
        assertBlock(surfaceRequest.dynamicRange)
    }

    private fun createTestVideoOutput(
        streamInfo: StreamInfo =
            StreamInfo.of(StreamInfo.STREAM_ID_ANY, StreamInfo.StreamState.ACTIVE),
        mediaSpec: MediaSpec = MediaSpec.builder().build(),
        videoCapabilities: VideoCapabilities = Recorder.getVideoCapabilities(cameraInfo),
    ): TestVideoOutput {
        return TestVideoOutput(streamInfo, mediaSpec, videoCapabilities)
    }

    private class TestVideoOutput(
        streamInfo: StreamInfo,
        mediaSpec: MediaSpec,
        private val videoCapabilities: VideoCapabilities,
    ) : VideoOutput {
        private val surfaceRequests = ArrayBlockingQueue<SurfaceRequest>(10)

        private val streamInfoObservable: MutableStateObservable<StreamInfo> =
            MutableStateObservable.withInitialState(streamInfo)

        private val mediaSpecObservable: MutableStateObservable<MediaSpec> =
            MutableStateObservable.withInitialState(mediaSpec)

        private val sourceStateListeners = CopyOnWriteArraySet<(SourceState) -> Unit>()
        val sourceStateFlow = callbackFlow {
            val listener: (SourceState) -> Unit = { sourceState -> trySend(sourceState) }
            sourceStateListeners.add(listener)
            awaitClose { sourceStateListeners.remove(listener) }
        }

        override fun onSurfaceRequested(surfaceRequest: SurfaceRequest) {
            surfaceRequests.put(surfaceRequest)
        }

        override fun getStreamInfo(): Observable<StreamInfo> = streamInfoObservable

        override fun getMediaSpec(): Observable<MediaSpec> = mediaSpecObservable

        override fun getMediaCapabilities(
            cameraInfo: CameraInfo,
            sessionType: Int,
        ): VideoCapabilities {
            return videoCapabilities
        }

        override fun onSourceStateChanged(sourceState: SourceState) {
            for (listener in sourceStateListeners) {
                listener(sourceState)
            }
        }

        fun nextSurfaceRequest(timeout: Long, timeUnit: TimeUnit): SurfaceRequest {
            return surfaceRequests.poll(timeout, timeUnit)
        }

        fun setStreamInfo(streamInfo: StreamInfo) = streamInfoObservable.setState(streamInfo)
    }

    private suspend fun SurfaceRequest.provideUpdatingSurface(): StateFlow<Int> {
        var isReleased = false
        val frameCountFlow = MutableStateFlow(0)
        val executor = Executors.newFixedThreadPool(1)

        val surfaceTexture =
            withContext(executor.asCoroutineDispatcher()) {
                SurfaceTexture(0).apply {
                    setDefaultBufferSize(640, 480)
                    detachFromGLContext()
                    attachToGLContext(GLUtil.getTexIdFromGLContext())
                    setOnFrameAvailableListener {
                        frameCountFlow.getAndUpdate { frameCount -> frameCount + 1 }
                        try {
                            executor.execute {
                                if (!isReleased) {
                                    updateTexImage()
                                }
                            }
                        } catch (_: RejectedExecutionException) {
                            // Ignored since frame updating is no longer needed after surface
                            // and executor are released.
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
