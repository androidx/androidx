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

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.AudioUtil
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.GarbageCollectionUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
import androidx.camera.video.internal.compat.quirk.DeactivateEncoderSurfaceBeforeStopEncoderQuirk
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeNoException
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

private const val FINALIZE_TIMEOUT = 5000L

@LargeTest
@RunWith(AndroidJUnit4::class)
class RecorderTest {

    @get:Rule
    var cameraRule: TestRule = CameraUtil.grantCameraPermissionAndPreTest()

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    @Suppress("UNCHECKED_CAST")
    private val videoRecordEventListener = mock(Consumer::class.java) as Consumer<VideoRecordEvent>

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var recorder: Recorder
    private lateinit var preview: Preview
    private lateinit var surfaceTexturePreview: Preview

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
        // Skip for b/168175357
        assumeFalse(
            "Cuttlefish has MediaCodec dequeueInput/Output buffer fails issue. Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29
        )
        assumeTrue(AudioUtil.canStartAudioRecord(MediaRecorder.AudioSource.CAMCORDER))

        CameraX.initialize(context, Camera2Config.defaultConfig()).get()
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)

        recorder = Recorder.Builder().build()

        // Using Preview so that the surface provider could be set to control when to issue the
        // surface request.
        val resolution = QualitySelector.getResolution(
            cameraUseCaseAdapter.cameraInfo,
            QualitySelector.QUALITY_LOWEST
        )
        assumeTrue(resolution != null)
        val resolutions: List<android.util.Pair<Int, Array<Size>>> =
            listOf<android.util.Pair<Int, Array<Size>>>(
                android.util.Pair.create(
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    arrayOf(resolution!!)
                )
            )
        preview = Preview.Builder().setSupportedResolutions(resolutions).build()

        // Add another Preview to provide an additional surface for b/168187087.
        surfaceTexturePreview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            surfaceTexturePreview.setSurfaceProvider(
                SurfaceTextureProvider.createSurfaceTextureProvider(
                    object : SurfaceTextureProvider.SurfaceTextureCallback {
                        override fun onSurfaceTextureReady(
                            surfaceTexture: SurfaceTexture,
                            resolution: Size
                        ) {
                            // No-op
                        }

                        override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                            surfaceTexture.release()
                        }
                    }
                )
            )
        }

        try {
            cameraUseCaseAdapter.checkAttachUseCases(listOf(preview, surfaceTexturePreview))
        } catch (e: CameraUseCaseAdapter.CameraException) {
            assumeNoException("The device doesn't support the use cases combination.", e)
        }

        cameraUseCaseAdapter = CameraUtil.createCameraAndAttachUseCase(
            context,
            cameraSelector,
            // Must put surfaceTexturePreview before preview while addUseCases, otherwise
            // an issue on Samsung device will occur. See b/196755459.
            surfaceTexturePreview,
            preview
        )
    }

    @After
    fun tearDown() {
        if (this::cameraUseCaseAdapter.isInitialized) {
            instrumentation.runOnMainSync {
                cameraUseCaseAdapter.removeUseCases(cameraUseCaseAdapter.useCases)
            }
        }

        CameraX.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun canRecordToFile() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        activeRecording.stopSafely()

        inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        val uri = Uri.fromFile(file)
        checkFileHasAudioAndVideo(uri)

        // Check the output Uri from the finalize event match the Uri from the given file.
        val captor = ArgumentCaptor.forClass(VideoRecordEvent::class.java)
        verify(videoRecordEventListener, atLeastOnce()).accept(captor.capture())
        val finalize = captor.value as VideoRecordEvent.Finalize
        assertThat(finalize.outputResults.outputUri).isEqualTo(uri)

        file.delete()
    }

    @Test
    fun canRecordToMediaStore() {
        invokeSurfaceRequest()
        val statusSemaphore = Semaphore(0)
        val finalizeSemaphore = Semaphore(0)
        val context: Context = ApplicationProvider.getApplicationContext()
        val contentResolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        var uri: Uri = Uri.EMPTY
        val activeRecording = recorder.prepareRecording(context, outputOptions)
            .withEventListener(CameraXExecutors.directExecutor()) {
                if (it is VideoRecordEvent.Status) {
                    statusSemaphore.release()
                }
                if (it is VideoRecordEvent.Finalize) {
                    uri = it.outputResults.outputUri
                    finalizeSemaphore.release()
                }
            }
            .withAudioEnabled()
            .start()

        assertThat(statusSemaphore.tryAcquire(5, 15000L, TimeUnit.MILLISECONDS)).isTrue()

        activeRecording.stopSafely()

        // Wait for the recording to complete.
        assertThat(finalizeSemaphore.tryAcquire(FINALIZE_TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()

        assertThat(uri).isNotEqualTo(Uri.EMPTY)

        checkFileHasAudioAndVideo(uri)

        contentResolver.delete(uri, null, null)
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun canRecordToFileDescriptor() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_WRITE
        ).use { pfd ->
            val activeRecording = recorder
                .prepareRecording(context, FileDescriptorOutputOptions.Builder(pfd).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

            val inOrder = inOrder(videoRecordEventListener)
            inOrder.verify(videoRecordEventListener, timeout(1000L))
                .accept(any(VideoRecordEvent.Start::class.java))
            inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
                .accept(any(VideoRecordEvent.Status::class.java))

            activeRecording.stopSafely()

            inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
                .accept(any(VideoRecordEvent.Finalize::class.java))

            checkFileHasAudioAndVideo(Uri.fromFile(file))
        }

        file.delete()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    @SuppressLint("NewApi") // Intentionally testing behavior of calling from invalid API level
    fun prepareRecordingWithFileDescriptor_throwsExceptionBeforeApi26() {
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_WRITE
        ).use { pfd ->
            assertThrows(UnsupportedOperationException::class.java) {
                recorder.prepareRecording(context, FileDescriptorOutputOptions.Builder(pfd).build())
            }
        }

        file.delete()
    }

    @Test
    fun canPauseResume() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        activeRecording.pause()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Pause::class.java))

        activeRecording.resume()

        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Resume::class.java))
        // Check there are data being encoded after resuming.
        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        activeRecording.stopSafely()

        // Wait for the recording to be finalized.
        inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        checkFileHasAudioAndVideo(Uri.fromFile(file))

        file.delete()
    }

    @Test
    fun canStartRecordingPaused_whenRecorderInitializing() {
        clearInvocations(videoRecordEventListener)

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        activeRecording.pause()

        // Only invoke surface request after pause() has been called
        invokeSurfaceRequest()

        val inOrder = inOrder(videoRecordEventListener)

        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))

        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Pause::class.java))

        activeRecording.stopSafely()

        file.delete()
    }

    // TODO:(b/187266265): Encoder surface cannot be reused on API level < 23 on some devices
    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun canStartNextRecordingPausedAfterFirstRecordingFinalized() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()

        val file1 = File.createTempFile("CameraX1", ".tmp").apply { deleteOnExit() }
        val file2 = File.createTempFile("CameraX2", ".tmp").apply { deleteOnExit() }

        // Start and stop a recording to ensure recorder is idling
        val inOrder = inOrder(videoRecordEventListener)
        val activeRecording1 =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file1).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))

        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        activeRecording1.stopSafely()

        inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        // First recording is now finalized. Try starting second recording paused.
        val activeRecording2 =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file2).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        activeRecording2.pause()

        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))

        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Pause::class.java))

        activeRecording2.stopSafely()

        file1.delete()
        file2.delete()
    }

    @Test
    fun canReceiveRecordingStats() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val inOrder = inOrder(videoRecordEventListener)
        // Start
        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))

        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        // Pause
        activeRecording.pause()

        verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Pause::class.java))

        // Resume
        activeRecording.resume()

        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Resume::class.java))

        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        // Stop
        activeRecording.stopSafely()

        inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        val captor = ArgumentCaptor.forClass(VideoRecordEvent::class.java)
        verify(videoRecordEventListener, atLeastOnce()).accept(captor.capture())
        captor.allValues.run {
            assertThat(size).isAtLeast(
                (
                    1 /* Start */ +
                        5 /* Status */ +
                        1 /* Pause */ +
                        1 /* Resume */ +
                        5 /* Status */ +
                        1 /* Stop */
                    )
            )

            // Ensure duration and bytes are increasing
            take(size - 1).mapIndexed { index, _ ->
                Pair(get(index).recordingStats, get(index + 1).recordingStats)
            }.forEach { (former: RecordingStats, latter: RecordingStats) ->
                assertThat(former.numBytesRecorded).isAtMost(latter.numBytesRecorded)
                assertThat(former.recordedDurationNanos).isAtMost((latter.recordedDurationNanos))
            }

            // Ensure they are not all zero by checking last stats
            last().recordingStats.also {
                assertThat(it.numBytesRecorded).isGreaterThan(0L)
                assertThat(it.recordedDurationNanos).isGreaterThan(0L)
            }
        }

        file.delete()
    }

    @Test
    fun setFileSizeLimit() {
        val fileSizeLimit = 500L * 1024L // 500 KB
        runFileSizeLimitTest(fileSizeLimit)
    }

    // Sets the file size limit to 1 byte, which will be lower than the initial data sent from
    // the encoder. This will ensure that the recording will be finalized even if it has no data
    // written to it.
    @Test
    fun setFileSizeLimitLowerThanInitialDataSize() {
        val fileSizeLimit = 1L // 1 byte
        runFileSizeLimitTest(fileSizeLimit)
    }

    @Test
    fun checkStreamState() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        @Suppress("UNCHECKED_CAST")
        val streamStateObserver =
            mock(Observable.Observer::class.java) as Observable.Observer<VideoOutput.StreamState>
        val inOrder = inOrder(streamStateObserver)
        recorder.streamState.addObserver(CameraXExecutors.directExecutor(), streamStateObserver)

        // Recorder should start in INACTIVE stream state before any recordings
        inOrder.verify(streamStateObserver, timeout(1000L)).onNewData(
            eq(VideoOutput.StreamState.INACTIVE)
        )

        // Start
        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()
        // Starting recording should move Recorder to ACTIVE stream state
        inOrder.verify(streamStateObserver, timeout(1000L)).onNewData(
            eq(VideoOutput.StreamState.ACTIVE)
        )

        activeRecording.stopSafely()

        // Stopping recording should eventually move to INACTIVE stream state
        inOrder.verify(streamStateObserver, timeout(FINALIZE_TIMEOUT)).onNewData(
            eq(VideoOutput.StreamState.INACTIVE)
        )

        file.delete()
    }

    @Test
    fun start_throwsExceptionWhenActive() {
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val outputOptions = FileOutputOptions.Builder(file).build()

        val activeRecording = recorder.prepareRecording(context, outputOptions).start()

        val pendingRecording = recorder.prepareRecording(context, outputOptions)
        assertThrows(java.lang.IllegalStateException::class.java) {
            pendingRecording.start()
        }

        activeRecording.close()
        file.delete()
    }

    @Test
    fun start_beforeSurfaceRequested() {
        clearInvocations(videoRecordEventListener)
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        invokeSurfaceRequest()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        activeRecording.stopSafely()
        // Wait for the recording to be finalized.
        inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))
        file.delete()
    }

    @Test
    fun start_finalizeImmediatelyWhenSourceInactive() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        recorder.onSourceStateChanged(VideoOutput.SourceState.INACTIVE)

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))
        verifyNoMoreInteractions(videoRecordEventListener)
        val captor = ArgumentCaptor.forClass(VideoRecordEvent::class.java)
        verify(videoRecordEventListener, atLeastOnce()).accept(captor.capture())
        val finalize = captor.value as VideoRecordEvent.Finalize
        assertThat(finalize.error).isEqualTo(ERROR_SOURCE_INACTIVE)

        activeRecording.close()
        file.delete()
    }

    @Test
    fun pause_beforeSurfaceRequested() {
        clearInvocations(videoRecordEventListener)
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        activeRecording.pause()

        invokeSurfaceRequest()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Pause::class.java))

        activeRecording.stopSafely()
        // Wait for the recording to be finalized.
        inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))
        file.delete()
    }

    @Test
    fun pause_noOpWhenAlreadyPaused() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        activeRecording.pause()
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Pause::class.java))

        activeRecording.pause()

        activeRecording.stopSafely()

        inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        // As described in b/197416199, there might be encoded data in flight which will trigger
        // Status event after pausing. So here it checks there's only one Pause event.
        val captor = ArgumentCaptor.forClass(VideoRecordEvent::class.java)
        verify(videoRecordEventListener, atLeastOnce()).accept(captor.capture())
        assertThat(captor.allValues.count { it is VideoRecordEvent.Pause }).isAtMost(1)

        file.delete()
    }

    @Test
    fun pause_throwsExceptionWhenStopping() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        activeRecording.stopSafely()
        assertThrows(IllegalStateException::class.java) {
            activeRecording.pause()
        }

        file.delete()
    }

    @Test
    fun resume_noOpWhenNotPaused() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        activeRecording.resume()

        // Shouldn't receive an Resume event.
        inOrder.verifyNoMoreInteractions()

        activeRecording.stopSafely()

        inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        file.delete()
    }

    @Test
    fun resume_throwsExceptionWhenStopping() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        activeRecording.stopSafely()
        assertThrows(IllegalStateException::class.java) {
            activeRecording.resume()
        }

        file.delete()
    }

    @Test
    fun stop_beforeSurfaceRequested() {
        clearInvocations(videoRecordEventListener)
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        activeRecording.pause()
        activeRecording.stopSafely()

        invokeSurfaceRequest()

        verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        file.delete()
    }

    @Test
    fun stop_fromAutoCloseable() {
        clearInvocations(videoRecordEventListener)
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val inOrder = inOrder(videoRecordEventListener)
        // Recording will be stopped by AutoCloseable.close() upon exiting use{} block
        val pendingRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
        pendingRecording.withEventListener(
            CameraXExecutors.directExecutor(),
            videoRecordEventListener
        ).start().use {
            invokeSurfaceRequest()
            inOrder.verify(videoRecordEventListener, timeout(1000L))
                .accept(any(VideoRecordEvent.Start::class.java))
            inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
                .accept(any(VideoRecordEvent.Status::class.java))
        }

        inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        file.delete()
    }

    @Test
    fun stop_WhenUseCaseDetached() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        instrumentation.runOnMainSync {
            cameraUseCaseAdapter.removeUseCases(listOf(preview))
        }

        inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        activeRecording.stop()
        file.delete()
    }

    @Suppress("UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
    @Test
    fun stop_whenActiveRecordingIsGarbageCollected() {
        clearInvocations(videoRecordEventListener)
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val inOrder = inOrder(videoRecordEventListener)
        var activeRecording: ActiveRecording? = recorder
            .prepareRecording(context, FileOutputOptions.Builder(file).build())
            .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
            .withAudioEnabled()
            .start()

        // First ensure the recording gets some status events
        invokeSurfaceRequest()
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        // Remove reference to active recording and run GC. The recording should be stopped once
        // the ActiveRecording's finalizer runs.
        activeRecording = null
        GarbageCollectionUtil.runFinalization()

        // Ensure the event listener gets a finalize event. Note: the word "finalize" is very
        // overloaded here. This event means the recording has finished, but does not relate to the
        // finalizer that runs during garbage collection. However, that is what causes the
        // recording to finish.
        inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        file.delete()
    }

    @Test
    fun stop_noOpWhenStopping() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .withAudioEnabled()
                .start()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        activeRecording.stopSafely()
        activeRecording.stopSafely()

        inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))
        inOrder.verifyNoMoreInteractions()

        file.delete()
    }

    @Test
    fun optionsOverridesDefaults() {
        val qualitySelector = QualitySelector.of(QualitySelector.QUALITY_HIGHEST)
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()

        assertThat(recorder.qualitySelector).isEqualTo(qualitySelector)
    }

    @Test
    fun canRetrieveProvidedExecutorFromRecorder() {
        val myExecutor = Executor { command -> command?.run() }
        val recorder = Recorder.Builder()
            .setExecutor(myExecutor)
            .build()

        assertThat(recorder.executor).isSameInstanceAs(myExecutor)
    }

    @Test
    fun cannotRetrieveExecutorWhenExecutorNotProvided() {
        val recorder = Recorder.Builder().build()

        assertThat(recorder.executor).isNull()
    }

    @Test
    fun canRecordWithoutAudio() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val activeRecording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
                .start()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        // Check the audio information reports state as disabled.
        val captor = ArgumentCaptor.forClass(VideoRecordEvent::class.java)
        verify(videoRecordEventListener, atLeastOnce()).accept(captor.capture())
        assertThat(captor.value).isInstanceOf(VideoRecordEvent.Status::class.java)
        val status = captor.value as VideoRecordEvent.Status
        assertThat(status.recordingStats.audioStats.audioState)
            .isEqualTo(AudioStats.AUDIO_STATE_DISABLED)

        activeRecording.stopSafely()

        verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        checkFileAudio(Uri.fromFile(file), false)
        checkFileVideo(Uri.fromFile(file), true)

        file.delete()
    }

    @Test
    fun cannotStartMultiplePendingRecordingsWhileInitializing() {
        val file1 = File.createTempFile("CameraX1", ".tmp").apply { deleteOnExit() }
        val file2 = File.createTempFile("CameraX2", ".tmp").apply { deleteOnExit() }
        try {
            // We explicitly do not invoke the surface request so the recorder is initializing.
            recorder.prepareRecording(context, FileOutputOptions.Builder(file1).build()).start()
                .use {
                    assertThrows<IllegalStateException> {
                        recorder.prepareRecording(context, FileOutputOptions.Builder(file2).build())
                            .start()
                    }
                }
        } finally {
            file1.delete()
            file2.delete()
        }
    }

    // TODO:(b/187266265): Encoder surface cannot be reused on API level < 23 on some devices
    @SdkSuppress(minSdkVersion = 23)
    @Test
    fun nextRecordingCanBeStartedAfterLastRecordingStopped() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()
        val file1 = File.createTempFile("CameraX1", ".tmp").apply { deleteOnExit() }
        val file2 = File.createTempFile("CameraX2", ".tmp").apply { deleteOnExit() }

        val inOrder = inOrder(videoRecordEventListener)
        try {
            recorder.prepareRecording(context, FileOutputOptions.Builder(file1).build())
                .withEventListener(
                    CameraXExecutors.directExecutor(),
                    videoRecordEventListener
                ).start().use {
                    inOrder.verify(videoRecordEventListener, timeout(1000L))
                        .accept(any(VideoRecordEvent.Start::class.java))
                    inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
                        .accept(any(VideoRecordEvent.Status::class.java))
                }

            recorder.prepareRecording(context, FileOutputOptions.Builder(file2).build())
                .withEventListener(
                    CameraXExecutors.directExecutor(),
                    videoRecordEventListener
                ).start().use {
                    inOrder.verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
                        .accept(any(VideoRecordEvent.Finalize::class.java))
                    inOrder.verify(videoRecordEventListener, timeout(1000L))
                        .accept(any(VideoRecordEvent.Start::class.java))
                    inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
                        .accept(any(VideoRecordEvent.Status::class.java))
                }

            verify(videoRecordEventListener, timeout(FINALIZE_TIMEOUT))
                .accept(any(VideoRecordEvent.Finalize::class.java))
        } finally {
            file1.delete()
            file2.delete()
        }
    }

    private fun invokeSurfaceRequest() {
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider { request: SurfaceRequest ->
                recorder.onSurfaceRequested(request)
            }
        }
    }

    private fun checkFileHasAudioAndVideo(uri: Uri) {
        checkFileAudio(uri, true)
        checkFileVideo(uri, true)
    }

    private fun checkFileAudio(uri: Uri, hasAudio: Boolean) {
        val mediaRetriever = MediaMetadataRetriever()
        mediaRetriever.apply {
            setDataSource(context, uri)
            val value = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)

            assertThat(value).isEqualTo(
                if (hasAudio) {
                    "yes"
                } else {
                    null
                }
            )
        }
    }

    private fun checkFileVideo(uri: Uri, hasVideo: Boolean) {
        val mediaRetriever = MediaMetadataRetriever()
        mediaRetriever.apply {
            setDataSource(context, uri)
            val value = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)

            assertThat(value).isEqualTo(
                if (hasVideo) {
                    "yes"
                } else {
                    null
                }
            )
        }
    }

    // It fails on devices with certain chipset if the codec is stopped when the camera is still
    // producing frames to the provided surface. This method first stop the camera from
    // producing frames then stops the recording safely on the problematic devices.
    private fun ActiveRecording.stopSafely() {
        val deactivateSurfaceBeforeStop =
            DeviceQuirks.get(DeactivateEncoderSurfaceBeforeStopEncoderQuirk::class.java) != null
        if (deactivateSurfaceBeforeStop) {
            instrumentation.runOnMainSync {
                preview.setSurfaceProvider(null)
            }
        }
        stop()
        if (deactivateSurfaceBeforeStop && Build.VERSION.SDK_INT >= 23) {
            invokeSurfaceRequest()
        }
    }

    private fun runFileSizeLimitTest(fileSizeLimit: Long) {
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val outputOptions = FileOutputOptions.Builder(file)
            .setFileSizeLimit(fileSizeLimit)
            .build()

        val activeRecording = recorder
            .prepareRecording(context, outputOptions)
            .withEventListener(CameraXExecutors.directExecutor(), videoRecordEventListener)
            .start()

        verify(
            videoRecordEventListener,
            timeout(60000L)
        ).accept(any(VideoRecordEvent.Finalize::class.java))

        val captor = ArgumentCaptor.forClass(VideoRecordEvent::class.java)
        verify(videoRecordEventListener, atLeastOnce()).accept(captor.capture())

        assertThat(captor.value).isInstanceOf(VideoRecordEvent.Finalize::class.java)
        val finalize = captor.value as VideoRecordEvent.Finalize
        assertThat(finalize.error).isEqualTo(ERROR_FILE_SIZE_LIMIT_REACHED)
        assertThat(file.length()).isLessThan(fileSizeLimit)

        activeRecording.close()
        file.delete()
    }
}