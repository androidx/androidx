/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.video.internal.encoder

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaFormat.KEY_CAPTURE_RATE
import android.media.MediaFormat.KEY_OPERATING_RATE
import android.media.MediaFormat.KEY_PRIORITY
import android.os.SystemClock
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_REGULAR
import androidx.camera.core.impl.Timebase
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.IgnoreVideoRecordingProblematicDeviceRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.video.Quality
import androidx.camera.video.Recorder
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.ExtraSupportedResolutionQuirk
import androidx.concurrent.futures.ResolvableFuture
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock

private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
private const val BIT_RATE = 10 * 1024 * 1024 // 10M
private const val COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
private const val FRAME_RATE = 30
private const val I_FRAME_INTERVAL = 1

@LargeTest
@RunWith(Parameterized::class)
@Suppress("DEPRECATION")
class VideoEncoderTest(private val implName: String, private val cameraConfig: CameraXConfig) {

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    @get:Rule val skipRule: TestRule = IgnoreVideoRecordingProblematicDeviceRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()))

        private val INPUT_TIMEBASE = Timebase.UPTIME
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dynamicRange = DynamicRange.SDR

    private lateinit var camera: CameraUseCaseAdapter
    private lateinit var videoEncoder: EncoderImpl
    private lateinit var videoEncoderConfig: VideoEncoderConfig
    private lateinit var videoEncoderCallback: EncoderCallback
    private lateinit var previewForVideoEncoder: Preview
    private lateinit var preview: Preview
    private lateinit var encoderExecutor: Executor
    private lateinit var latestSurfaceReadyToRelease: ResolvableFuture<Void>

    @Before
    fun setUp() {
        val cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()

        // Skip for b/241876294
        assumeFalse(
            "Skip test for devices with ExtraSupportedResolutionQuirk, since the extra" +
                " resolutions cannot be used when the provided surface is an encoder surface.",
            DeviceQuirks.get(ExtraSupportedResolutionQuirk::class.java) != null,
        )

        CameraXUtil.initialize(context, cameraConfig).get()

        camera = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)

        encoderExecutor = CameraXExecutors.ioExecutor()

        // Binding one more preview use case to create a surface texture, this is for testing on
        // Pixel API 26, it needs a surface texture at least.
        preview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
        }

        previewForVideoEncoder = Preview.Builder().build()

        instrumentation.runOnMainSync {
            // Must put preview before previewForVideoEncoder while addUseCases, otherwise an issue
            // on Samsung device will occur. See b/196755459.
            camera.addUseCases(listOf(preview, previewForVideoEncoder))
        }
    }

    @After
    fun tearDown() {
        if (::camera.isInitialized) {
            camera.apply {
                instrumentation.runOnMainSync {
                    removeUseCases(setOf(previewForVideoEncoder, preview))
                }
            }
        }

        if (::latestSurfaceReadyToRelease.isInitialized) {
            latestSurfaceReadyToRelease.addListener(
                {
                    if (::videoEncoder.isInitialized) {
                        videoEncoder.release()
                    }
                },
                CameraXExecutors.directExecutor(),
            )
        }

        // Ensure all cameras are released for the next test
        CameraXUtil.shutdown()[10, TimeUnit.SECONDS]
    }

    @Test
    fun canGetEncoderInfo() {
        initVideoEncoder()

        assertThat(videoEncoder.encoderInfo).isNotNull()
    }

    @Test
    fun canRestartVideoEncoder() {
        // Arrange.
        initVideoEncoder()
        videoEncoder.start()
        var inOrder = inOrder(videoEncoderCallback)
        inOrder.verify(videoEncoderCallback, timeout(5000L)).onEncodeStart()
        inOrder.verify(videoEncoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())
        videoEncoder.stop()
        inOrder.verify(videoEncoderCallback, timeout(5000L)).onEncodeStop()
        clearInvocations(videoEncoderCallback)

        // Act.
        videoEncoder.start()

        // Assert.
        inOrder = inOrder(videoEncoderCallback)
        inOrder.verify(videoEncoderCallback, timeout(5000L)).onEncodeStart()
        inOrder.verify(videoEncoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        // Act.
        videoEncoder.stop()

        // Assert.
        inOrder.verify(videoEncoderCallback, timeout(5000L)).onEncodeStop()
    }

    @Test
    fun canPauseResumeVideoEncoder() {
        initVideoEncoder()

        videoEncoder.start()

        verify(videoEncoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        videoEncoder.pause()

        verify(videoEncoderCallback, timeout(5000L)).onEncodePaused()

        clearInvocations(videoEncoderCallback)

        videoEncoder.start()

        verify(videoEncoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())
    }

    @Test
    fun canPauseStopStartVideoEncoder() {
        initVideoEncoder()

        videoEncoder.start()

        verify(videoEncoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        videoEncoder.pause()

        verify(videoEncoderCallback, timeout(5000L)).onEncodePaused()

        videoEncoder.stop()

        verify(videoEncoderCallback, timeout(5000L)).onEncodeStop()

        clearInvocations(videoEncoderCallback)

        videoEncoder.start()

        verify(videoEncoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())
    }

    @Test
    fun canRestartPauseVideoEncoder() {
        initVideoEncoder()

        videoEncoder.start()
        verify(videoEncoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        videoEncoder.stop()
        videoEncoder.start()
        videoEncoder.pause()

        verify(videoEncoderCallback, timeout(10000L)).onEncodePaused()
    }

    @Test
    fun pauseResumeVideoEncoder_getChronologicalData() {
        initVideoEncoder()

        val inOrder = inOrder(videoEncoderCallback)

        videoEncoder.start()
        inOrder.verify(videoEncoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        videoEncoder.pause()
        inOrder.verify(videoEncoderCallback, timeout(5000L)).onEncodePaused()

        videoEncoder.start()
        inOrder.verify(videoEncoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        val captor = ArgumentCaptor.forClass(EncodedData::class.java)
        verify(videoEncoderCallback, atLeast(/*start*/ 5 + /*resume*/ 5))
            .onEncodedData(captor.capture())
        verifyDataInChronologicalOrder(captor.allValues)
    }

    @Test
    fun startVideoEncoder_firstEncodedDataIsKeyFrame() {
        initVideoEncoder()

        videoEncoder.start()
        val captor = ArgumentCaptor.forClass(EncodedData::class.java)
        verify(videoEncoderCallback, timeout(5000L).atLeastOnce()).onEncodedData(captor.capture())

        assertThat(isKeyFrame(captor.allValues.first().bufferInfo)).isTrue()

        videoEncoder.stop()

        verify(videoEncoderCallback, timeout(5000L)).onEncodeStop()
    }

    @Test
    fun resumeVideoEncoder_firstEncodedDataIsKeyFrame() {
        initVideoEncoder()

        videoEncoder.start()
        verify(videoEncoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        videoEncoder.pause()
        verify(videoEncoderCallback, timeout(5000L)).onEncodePaused()

        clearInvocations(videoEncoderCallback)

        videoEncoder.start()
        val captor = ArgumentCaptor.forClass(EncodedData::class.java)
        verify(videoEncoderCallback, timeout(15000L).atLeastOnce()).onEncodedData(captor.capture())

        assertThat(isKeyFrame(captor.allValues.first().bufferInfo)).isTrue()
    }

    @Test
    fun bufferTimeIsUptime() {
        initVideoEncoder()

        // Skip test if the difference between uptime and realtime is too close to avoid test flaky.
        // Note: Devices such as lab devices always have usb-plugged, so the uptime and realtime
        // may always be the same and be skipped.
        // TODO(b/199486135): Find a way to make the uptime differ from realtime in lab devices
        assumeTrue(abs(SystemClock.elapsedRealtime() - SystemClock.uptimeMillis()) > 3000)

        videoEncoder.start()
        val captor = ArgumentCaptor.forClass(EncodedData::class.java)
        verify(videoEncoderCallback, timeout(15000L).atLeast(5)).onEncodedData(captor.capture())

        val bufferTimeUs = captor.value.presentationTimeUs
        val uptimeUs = TimeUnit.MILLISECONDS.toMicros(SystemClock.uptimeMillis())
        val realtimeUs = TimeUnit.MILLISECONDS.toMicros(SystemClock.elapsedRealtime())
        val isCloseToUptime = abs(bufferTimeUs - uptimeUs) <= abs(bufferTimeUs - realtimeUs)

        assertThat(isCloseToUptime).isTrue()
    }

    @Test
    fun stopVideoEncoder_reachStopTime() {
        initVideoEncoder()

        videoEncoder.start()
        verify(videoEncoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        val stopTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime())

        videoEncoder.stop()
        verify(videoEncoderCallback, timeout(5000L)).onEncodeStop()

        // If the last data timestamp is null, it means the encoding is probably stopped because of
        // timeout.
        // Skip null since it could be a device performance issue which is out of the test scope.
        assumeTrue(videoEncoder.mLastDataStopTimestamp != null)
        assertThat(videoEncoder.mLastDataStopTimestamp).isAtLeast(stopTimeUs)
    }

    @Test
    fun setDifferentCaptureEncodeFrameRates_shouldContainAdditionalKeyValues() {
        val captureFrameRate = FRAME_RATE
        val encodeFrameRate = FRAME_RATE / 2
        initVideoEncoder(captureFrameRate = captureFrameRate, encodeFrameRate = encodeFrameRate)

        val format = videoEncoder.mMediaFormat
        assertThat(format.getInteger(KEY_CAPTURE_RATE)).isEqualTo(captureFrameRate)
        assertThat(format.getInteger(KEY_OPERATING_RATE)).isEqualTo(captureFrameRate)
        assertThat(format.getInteger(KEY_PRIORITY)).isEqualTo(0)
    }

    private fun initVideoEncoder(
        captureFrameRate: Int = FRAME_RATE,
        encodeFrameRate: Int = FRAME_RATE,
    ) {
        // init video encoder
        val cameraInfo = camera.cameraInfo as CameraInfoInternal
        val quality = Quality.LOWEST
        val videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        val resolution = videoCapabilities.getResolution(quality, dynamicRange)!!

        videoEncoderConfig =
            VideoEncoderConfig.builder()
                .setInputTimebase(INPUT_TIMEBASE)
                .setBitrate(BIT_RATE)
                .setColorFormat(COLOR_FORMAT)
                .setCaptureFrameRate(captureFrameRate)
                .setEncodeFrameRate(encodeFrameRate)
                .setIFrameInterval(I_FRAME_INTERVAL)
                .setMimeType(MIME_TYPE)
                .setResolution(resolution)
                .build()

        videoEncoderCallback = mock(EncoderCallback::class.java)
        doAnswer { args: InvocationOnMock ->
                val encodedData: EncodedData = args.getArgument(0)
                encodedData.close()
                null
            }
            .`when`(videoEncoderCallback)
            .onEncodedData(any())

        videoEncoder = EncoderImpl(encoderExecutor, videoEncoderConfig, SESSION_TYPE_REGULAR)

        videoEncoder.setEncoderCallback(videoEncoderCallback, CameraXExecutors.directExecutor())

        val surface = (videoEncoder.input as Encoder.SurfaceInput).surface

        instrumentation.runOnMainSync {
            previewForVideoEncoder.setSurfaceProvider { request: SurfaceRequest ->
                val surfaceReadyToRelease = ResolvableFuture.create<Void>()
                request.provideSurface(surface, CameraXExecutors.directExecutor()) {
                    surfaceReadyToRelease.set(null)
                }
                latestSurfaceReadyToRelease = surfaceReadyToRelease
            }
        }
    }

    private fun verifyDataInChronologicalOrder(encodedDataList: List<EncodedData>) {
        // For each item indexed by n and n+1, verify that the timestamp of n is less than n+1.
        encodedDataList.take(encodedDataList.size - 1).forEachIndexed { index, _ ->
            assertThat(encodedDataList[index].presentationTimeUs)
                .isLessThan(encodedDataList[index + 1].presentationTimeUs)
        }
    }

    private fun isKeyFrame(bufferInfo: MediaCodec.BufferInfo): Boolean {
        return bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0
    }
}
