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
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.AudioUtil
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
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

        recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.of(QualitySelector.QUALITY_HIGHEST)).build()

        // Using Preview so that the surface provider could be set to control when to issue the
        // surface request.
        preview = Preview.Builder().build()

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
            preview,
            surfaceTexturePreview
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
        val outputOptions = FileOutputOptions.builder().setFile(file).build()

        val pendingRecording = recorder.prepareRecording(outputOptions)
        pendingRecording.withEventListener(
            CameraXExecutors.directExecutor(),
            videoRecordEventListener
        )

        val activeRecording = pendingRecording.start()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        activeRecording.stop()

        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        checkFileHasAudioAndVideo(Uri.fromFile(file))

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

        val outputOptions = MediaStoreOutputOptions.builder()
            .setContentResolver(contentResolver)
            .setCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        val pendingRecording = recorder.prepareRecording(outputOptions)
        var uri: Uri = Uri.EMPTY
        pendingRecording.withEventListener(
            CameraXExecutors.directExecutor(),
            {
                if (it is VideoRecordEvent.Status) {
                    statusSemaphore.release()
                }
                if (it is VideoRecordEvent.Finalize) {
                    uri = it.outputResults.outputUri
                    finalizeSemaphore.release()
                }
            }
        )

        val activeRecording = pendingRecording.start()

        assertThat(statusSemaphore.tryAcquire(5, 15000L, TimeUnit.MILLISECONDS)).isTrue()

        activeRecording.stop()

        // Wait for the recording to complete.
        assertThat(finalizeSemaphore.tryAcquire(1000L, TimeUnit.MILLISECONDS)).isTrue()

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
            val outputOptions = FileDescriptorOutputOptions.builder()
                .setParcelFileDescriptor(pfd)
                .build()

            val pendingRecording = recorder.prepareRecording(outputOptions)
            pendingRecording.withEventListener(
                CameraXExecutors.directExecutor(),
                videoRecordEventListener
            )

            val activeRecording = pendingRecording.start()

            val inOrder = inOrder(videoRecordEventListener)
            inOrder.verify(videoRecordEventListener, timeout(1000L))
                .accept(any(VideoRecordEvent.Start::class.java))
            inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
                .accept(any(VideoRecordEvent.Status::class.java))

            activeRecording.stop()

            inOrder.verify(videoRecordEventListener, timeout(1000L))
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
            val outputOptions = FileDescriptorOutputOptions.builder()
                .setParcelFileDescriptor(pfd)
                .build()

            assertThrows(IllegalStateException::class.java) {
                recorder.prepareRecording(outputOptions)
            }
        }

        file.delete()
    }

    @Test
    fun canPauseResume() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val outputOptions = FileOutputOptions.builder().setFile(file).build()

        val pendingRecording = recorder.prepareRecording(outputOptions)
        pendingRecording.withEventListener(
            CameraXExecutors.directExecutor(),
            videoRecordEventListener
        )

        val activeRecording = pendingRecording.start()

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

        activeRecording.stop()

        // Wait for the recording to be finalized.
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        checkFileHasAudioAndVideo(Uri.fromFile(file))

        file.delete()
    }

    @Test
    fun canReceiveRecordingStats() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val outputOptions = FileOutputOptions.builder().setFile(file).build()

        val pendingRecording = recorder.prepareRecording(outputOptions)
        pendingRecording.withEventListener(
            CameraXExecutors.directExecutor(),
            videoRecordEventListener
        )

        val inOrder = inOrder(videoRecordEventListener)

        // Start
        val activeRecording = pendingRecording.start()

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
        activeRecording.stop()

        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        val captor = ArgumentCaptor.forClass(VideoRecordEvent::class.java)
        verify(videoRecordEventListener, atLeastOnce()).accept(captor.capture())
        captor.allValues.run {
            assertThat(size).isAtLeast(
                1 /* Start */ +
                    5 /* Status */ +
                    1 /* Pause */ +
                    1 /* Resume */ +
                    5 /* Status */ +
                    1 /* Stop */
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
    fun checkStreamState() {
        clearInvocations(videoRecordEventListener)
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val outputOptions = FileOutputOptions.builder().setFile(file).build()

        val pendingRecording = recorder.prepareRecording(outputOptions)

        @Suppress("UNCHECKED_CAST")
        val streamStateObserver =
            mock(Observable.Observer::class.java) as Observable.Observer<VideoOutput.StreamState>
        recorder.streamState.addObserver(CameraXExecutors.directExecutor(), streamStateObserver)

        val activeRecording = pendingRecording.start()
        verify(streamStateObserver, timeout(1000L)).onNewData(eq(VideoOutput.StreamState.ACTIVE))

        activeRecording.stop()

        verify(streamStateObserver, timeout(1000L)).onNewData(eq(VideoOutput.StreamState.INACTIVE))

        file.delete()
    }

    @Test
    fun start_throwsExceptionWhenActive() {
        invokeSurfaceRequest()
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val outputOptions = FileOutputOptions.builder().setFile(file).build()

        val pendingRecording1 = recorder.prepareRecording(outputOptions)
        pendingRecording1.start()

        val pendingRecording2 = recorder.prepareRecording(outputOptions)
        assertThrows(java.lang.IllegalStateException::class.java) {
            pendingRecording2.start()
        }

        file.delete()
    }

    @Test
    fun start_beforeSurfaceRequested() {
        clearInvocations(videoRecordEventListener)
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val outputOptions = FileOutputOptions.builder().setFile(file).build()

        val pendingRecording = recorder.prepareRecording(outputOptions)
        pendingRecording.withEventListener(
            CameraXExecutors.directExecutor(),
            videoRecordEventListener
        )

        val activeRecording = pendingRecording.start()

        invokeSurfaceRequest()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
            .accept(any(VideoRecordEvent.Status::class.java))

        activeRecording.stop()
        // Wait for the recording to be finalized.
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Finalize::class.java))
        file.delete()
    }

    @Test
    fun pause_beforeSurfaceRequested() {
        clearInvocations(videoRecordEventListener)
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val outputOptions = FileOutputOptions.builder().setFile(file).build()

        val pendingRecording = recorder.prepareRecording(outputOptions)
        pendingRecording.withEventListener(
            CameraXExecutors.directExecutor(),
            videoRecordEventListener
        )

        val activeRecording = pendingRecording.start()
        activeRecording.pause()

        invokeSurfaceRequest()

        val inOrder = inOrder(videoRecordEventListener)
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Start::class.java))
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Pause::class.java))

        activeRecording.stop()
        // Wait for the recording to be finalized.
        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Finalize::class.java))
        file.delete()
    }

    @Test
    fun stop_beforeSurfaceRequested() {
        clearInvocations(videoRecordEventListener)
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val outputOptions = FileOutputOptions.builder().setFile(file).build()

        val pendingRecording = recorder.prepareRecording(outputOptions)
        pendingRecording.withEventListener(
            CameraXExecutors.directExecutor(),
            videoRecordEventListener
        )

        val activeRecording = pendingRecording.start()
        activeRecording.pause()
        activeRecording.stop()

        invokeSurfaceRequest()

        verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        file.delete()
    }

    @Test
    fun stop_fromAutoCloseable() {
        clearInvocations(videoRecordEventListener)
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val outputOptions = FileOutputOptions.builder().setFile(file).build()

        val pendingRecording = recorder.prepareRecording(outputOptions)
        pendingRecording.withEventListener(
            CameraXExecutors.directExecutor(),
            videoRecordEventListener
        )

        val inOrder = inOrder(videoRecordEventListener)
        // Recording will be stopped by AutoCloseable.close() upon exiting use{} block
        pendingRecording.start().use {
            invokeSurfaceRequest()
            inOrder.verify(videoRecordEventListener, timeout(1000L))
                .accept(any(VideoRecordEvent.Start::class.java))
            inOrder.verify(videoRecordEventListener, timeout(15000L).atLeast(5))
                .accept(any(VideoRecordEvent.Status::class.java))
        }

        inOrder.verify(videoRecordEventListener, timeout(1000L))
            .accept(any(VideoRecordEvent.Finalize::class.java))

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

    private fun invokeSurfaceRequest() {
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider { request: SurfaceRequest ->
                recorder.onSurfaceRequested(request)
            }
        }
    }

    private fun checkFileHasAudioAndVideo(uri: Uri) {
        val mediaRetriever = MediaMetadataRetriever()
        mediaRetriever.apply {
            setDataSource(context, uri)
            val hasAudio = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            val hasVideo = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)

            assertThat(hasAudio).isEqualTo("yes")
            assertThat(hasVideo).isEqualTo("yes")
        }
    }
}