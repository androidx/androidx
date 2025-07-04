/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.camera.camera2.internal

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult.CONTROL_AE_TARGET_FPS_RANGE
import android.hardware.camera2.params.SessionConfiguration.SESSION_HIGH_SPEED
import android.media.CamcorderProfile
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_1080P
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_2160P
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_480P
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_720P
import android.media.MediaCodec
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
import android.media.MediaFormat
import android.media.MediaFormat.KEY_BIT_RATE
import android.media.MediaFormat.KEY_COLOR_FORMAT
import android.media.MediaFormat.KEY_FRAME_RATE
import android.media.MediaFormat.KEY_I_FRAME_INTERVAL
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.media.MediaFormat.MIMETYPE_VIDEO_H263
import android.media.MediaFormat.MIMETYPE_VIDEO_HEVC
import android.media.MediaFormat.MIMETYPE_VIDEO_MPEG4
import android.media.MediaFormat.MIMETYPE_VIDEO_VP8
import android.media.MediaFormat.MIMETYPE_VIDEO_VP9
import android.media.MediaRecorder.VideoEncoder.H263
import android.media.MediaRecorder.VideoEncoder.H264
import android.media.MediaRecorder.VideoEncoder.HEVC
import android.media.MediaRecorder.VideoEncoder.MPEG_4_SP
import android.media.MediaRecorder.VideoEncoder.VP8
import android.media.MediaRecorder.VideoEncoder.VP9
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.SynchronizedCaptureSession.OpenerBuilder
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.camera2.internal.compat.params.DynamicRangesCompat
import androidx.camera.camera2.internal.compat.quirk.CameraQuirks
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.CameraDeviceHolder
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
class HighSpeedCaptureSessionTest {

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    private lateinit var mCameraDeviceHolder: CameraDeviceHolder
    private lateinit var mCameraCharacteristics: CameraCharacteristicsCompat
    private lateinit var mDynamicRangesCompat: DynamicRangesCompat
    private lateinit var cameraQuirks: Quirks
    private lateinit var captureSessionOpenerBuilder: OpenerBuilder
    private lateinit var cameraId: String

    private val mCaptureSessions = mutableListOf<CaptureSession>()
    private val mDeferrableSurfaces = mutableListOf<DeferrableSurface>()

    private val isHighSpeedSupported: Boolean
        get() {
            val capabilities =
                mCameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            return capabilities?.any {
                it == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
            } == true
        }

    @Before
    fun setup() {
        val handler = Handler(handlerThread.getLooper())
        val executor = CameraXExecutors.newHandlerExecutor(handler)
        val scheduledExecutor = CameraXExecutors.newHandlerExecutor(handler)

        cameraId = CameraUtil.getBackwardCompatibleCameraIdListOrThrow()[0]
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraManager = CameraManagerCompat.from(context, handler)
        try {
            mCameraCharacteristics = cameraManager.getCameraCharacteristicsCompat(cameraId)
        } catch (e: CameraAccessExceptionCompat) {
            throw AssumptionViolatedException("Could not retrieve camera characteristics", e)
        }

        cameraQuirks = CameraQuirks.get(cameraId, mCameraCharacteristics)
        mDynamicRangesCompat = DynamicRangesCompat.fromCameraCharacteristics(mCameraCharacteristics)

        val captureSessionRepository = CaptureSessionRepository(executor)
        captureSessionOpenerBuilder =
            OpenerBuilder(
                executor,
                scheduledExecutor,
                handler,
                captureSessionRepository,
                CameraQuirks.get(cameraId, mCameraCharacteristics),
                DeviceQuirks.getAll(),
            )

        mCameraDeviceHolder =
            CameraUtil.getCameraDevice(cameraId, captureSessionRepository.cameraStateCallback)
    }

    @After
    fun tearDown() {
        // Ensure all capture sessions are fully closed
        val releaseFutures = mutableListOf<ListenableFuture<Void?>?>()
        for (captureSession in mCaptureSessions) {
            releaseFutures.add(captureSession.release(/* abortInFlightCaptures= */ false))
        }
        mCaptureSessions.clear()
        Futures.allAsList<Void?>(releaseFutures).get(10L, TimeUnit.SECONDS)

        if (this::mCameraDeviceHolder.isInitialized) {
            CameraUtil.releaseCameraDevice(mCameraDeviceHolder)
        }

        for (deferrableSurface in mDeferrableSurfaces) {
            deferrableSurface.close()
        }
    }

    @Test
    fun issueCaptureRequest_forRecording_canIssueRepeatingAndSingleRequests() {
        // Arrange: check capability and get supported high speed size and fps range.
        assumeTrue(isHighSpeedSupported)

        val profile =
            getHighSpeedCamcorderProfile()
                ?: throw AssumptionViolatedException("No CamcorderProfile")
        val profileInfo =
            "video codec:${profile.videoCodec}, " +
                "size:${profile.videoSize}, " +
                "frame rate:${profile.videoFrameRate}, " +
                "bit rate:${profile.videoBitRate}"
        Log.d(TAG, "Selected profile $profileInfo")

        // Create SessionConfig for high-speed capture
        val repeatingLatch = CountDownLatch(2)
        val templateType = TEMPLATE_RECORD
        val previewSurface = createSurfaceTextureDeferrableSurface(profile.videoSize)
        val videoSurface = createMediaCodecDeferrableSurface(profile)
        val fpsRange = Range.create(profile.videoFrameRate, profile.videoFrameRate)
        val sessionConfig =
            SessionConfig.Builder()
                .apply {
                    setSessionType(SESSION_HIGH_SPEED)
                    setTemplateType(templateType)
                    addSurface(previewSurface)
                    addSurface(videoSurface)
                    setExpectedFrameRateRange(fpsRange)
                    addCameraCaptureCallback(
                        object : CameraCaptureCallback() {
                            override fun onCaptureCompleted(
                                captureConfigId: Int,
                                cameraCaptureResult: CameraCaptureResult,
                            ) {
                                val fps =
                                    cameraCaptureResult.captureResult?.get(
                                        CONTROL_AE_TARGET_FPS_RANGE
                                    )
                                if (repeatingLatch.count > 0L) {
                                    Log.d(TAG, "Repeating onCaptureCompleted: fps = $fps")
                                }
                                if (fps == fpsRange) {
                                    repeatingLatch.countDown()
                                }
                            }
                        }
                    )
                }
                .build()

        // Act: open capture session and issue repeating request via SessionConfig.
        val captureSession =
            createCaptureSession().apply {
                open(
                    sessionConfig,
                    mCameraDeviceHolder.get()!!,
                    captureSessionOpenerBuilder.build(),
                )
                this.sessionConfig = sessionConfig
            }

        // Assert.
        assertWithMessage("Failed to issue repeating request by $profileInfo")
            .that(repeatingLatch.await(10, TimeUnit.SECONDS))
            .isTrue()

        // Arrange: create CaptureConfig.
        val captureId = 100
        val captureLatch = CountDownLatch(1)
        val captureConfig =
            CaptureConfig.Builder()
                .apply {
                    this.templateType = templateType
                    addSurface(previewSurface)
                    addSurface(videoSurface)
                    setId(captureId)
                    addCameraCaptureCallback(
                        object : CameraCaptureCallback() {
                            override fun onCaptureCompleted(
                                captureConfigId: Int,
                                cameraCaptureResult: CameraCaptureResult,
                            ) {
                                // Count down when the request is proceeded and fps is applied.
                                val fps =
                                    cameraCaptureResult.captureResult?.get(
                                        CONTROL_AE_TARGET_FPS_RANGE
                                    )
                                Log.d(
                                    TAG,
                                    "Single capture onCaptureCompleted: " +
                                        "captureConfigId = $captureConfigId, fps = $fps",
                                )
                                if (captureId == captureConfigId && fps == fpsRange) {
                                    captureLatch.countDown()
                                }
                            }
                        }
                    )
                }
                .build()

        // Act. issue single request.
        captureSession.issueCaptureRequests(listOf(captureConfig))

        // Assert.
        assertWithMessage("Failed to issue single capture request by $profileInfo")
            .that(captureLatch.await(5, TimeUnit.SECONDS))
            .isTrue()
    }

    private fun createCaptureSession() =
        CaptureSession(mDynamicRangesCompat, cameraQuirks).also { mCaptureSessions.add(it) }

    private fun getHighSpeedCamcorderProfile(): CamcorderProfile? {
        return listOf(
                QUALITY_HIGH_SPEED_480P,
                QUALITY_HIGH_SPEED_720P,
                QUALITY_HIGH_SPEED_1080P,
                QUALITY_HIGH_SPEED_2160P,
            )
            .filter { CamcorderProfile.hasProfile(it) }
            .firstNotNullOfOrNull { quality ->
                @Suppress("DEPRECATION") CamcorderProfile.get(cameraId.toInt(), quality)
            }
    }

    private fun createSurfaceTextureDeferrableSurface(size: Size): DeferrableSurface {
        val surfaceTexture =
            SurfaceTexture(0).apply {
                setDefaultBufferSize(size.width, size.height)
                detachFromGLContext()
            }
        val surface = Surface(surfaceTexture)
        return ImmediateSurface(surface).apply {
            terminationFuture.addListener(
                Runnable {
                    surface.release()
                    surfaceTexture.release()
                },
                CameraXExecutors.directExecutor(),
            )
            mDeferrableSurfaces.add(this)
        }
    }

    private fun createMediaCodecDeferrableSurface(profile: CamcorderProfile): DeferrableSurface {
        val surface = MediaCodec.createPersistentInputSurface()
        val mimeType = profile.videoMime
        val codec = MediaCodec.createEncoderByType(mimeType)
        codec.setCallback(emptyCodecCallback)
        val format =
            MediaFormat.createVideoFormat(
                    mimeType,
                    profile.videoFrameWidth,
                    profile.videoFrameHeight,
                )
                .apply {
                    setInteger(KEY_COLOR_FORMAT, COLOR_FormatSurface)
                    setInteger(KEY_FRAME_RATE, profile.videoFrameRate)
                    setInteger(KEY_BIT_RATE, profile.videoBitRate) // 4 Mbps
                    setInteger(KEY_I_FRAME_INTERVAL, 1) // 1 second
                }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.setInputSurface(surface)
        codec.start()

        return ImmediateSurface(surface).apply {
            terminationFuture.addListener(
                Runnable {
                    codec.stop()
                    codec.release()
                    surface.release()
                },
                CameraXExecutors.directExecutor(),
            )
            mDeferrableSurfaces.add(this)
        }
    }

    private val CamcorderProfile.videoMime
        get() =
            when (videoCodec) {
                H263 -> MIMETYPE_VIDEO_H263
                H264 -> MIMETYPE_VIDEO_AVC
                MPEG_4_SP -> MIMETYPE_VIDEO_MPEG4
                HEVC -> MIMETYPE_VIDEO_HEVC
                VP8 -> MIMETYPE_VIDEO_VP8
                VP9 -> MIMETYPE_VIDEO_VP9
                else -> throw IllegalArgumentException("Unsupported video codec: $videoCodec")
            }

    private val CamcorderProfile.videoSize
        get() = Size(videoFrameWidth, videoFrameHeight)

    private val emptyCodecCallback by lazy {
        object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}

            override fun onOutputBufferAvailable(
                codec: MediaCodec,
                index: Int,
                info: MediaCodec.BufferInfo,
            ) {
                codec.getOutputBuffer(index)
                codec.releaseOutputBuffer(index, false)
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                fail(e.message)
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {}
        }
    }

    private companion object {
        private const val TAG = "HighSpeedCaptureSessionTest"
        private lateinit var handlerThread: HandlerThread

        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            handlerThread = HandlerThread("HighSpeedCaptureSessionTest")
            handlerThread.start()
        }

        @AfterClass
        @JvmStatic
        fun tearDownClass() {
            if (this::handlerThread.isInitialized) {
                handlerThread.quitSafely()
            }
        }
    }
}
