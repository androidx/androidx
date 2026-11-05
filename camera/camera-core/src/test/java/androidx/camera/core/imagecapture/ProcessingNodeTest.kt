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

package androidx.camera.core.imagecapture

import android.graphics.ImageFormat.JPEG
import android.graphics.ImageFormat.RAW_SENSOR
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.os.Looper.getMainLooper
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.OUTPUT_FILE_OPTIONS
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.SECONDARY_OUTPUT_FILE_OPTIONS
import androidx.camera.core.imagecapture.Utils.SENSOR_TO_BUFFER
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.imagecapture.Utils.createProcessingRequest
import androidx.camera.core.imagecapture.Utils.createTakePictureRequest
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.utils.executor.CameraXExecutors.isSequentialExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.internal.CameraCaptureResultImageInfo
import androidx.camera.testing.impl.TestImageUtil.createA24ProblematicJpegByteArray
import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import androidx.camera.testing.impl.TestImageUtil.createJpegFakeImageProxy
import androidx.camera.testing.impl.TestImageUtil.createJpegrBytes
import androidx.camera.testing.impl.TestImageUtil.createJpegrFakeImageProxy
import androidx.camera.testing.impl.TestImageUtil.createRawFakeImageProxy
import androidx.camera.testing.impl.fakes.FakeImageInfo
import androidx.camera.testing.impl.fakes.FakeImageProxy
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers.setStaticField

/** Unit tests for [ProcessingNode]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class ProcessingNodeTest {

    private lateinit var processingNodeIn: ProcessingNode.In
    private var cameraCharacteristics: CameraCharacteristics =
        mock(CameraCharacteristics::class.java)

    private var node = ProcessingNode(mainThreadExecutor(), cameraCharacteristics)

    @Before
    fun setUp() {
        processingNodeIn = ProcessingNode.In.of(JPEG, listOf(JPEG))
        node.transform(processingNodeIn)
    }

    @After
    fun tearDown() {
        // Process any pending looper updates to prevent leaks
        shadowOf(getMainLooper()).idle()
    }

    @Test
    fun processRequest_hasDiskResult() {
        // Arrange: create a request with callback.
        val callback = FakeTakePictureCallback()
        val request =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    OUTPUT_FILE_OPTIONS,
                    null,
                    Rect(0, 0, WIDTH, HEIGHT),
                    SENSOR_TO_BUFFER,
                    ROTATION_DEGREES,
                    /*jpegQuality=*/ 100,
                ),
                callback,
                Futures.immediateFuture(null),
            )

        // Act: process the request.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val image = createJpegFakeImageProxy(jpegBytes)
        processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, image))
        shadowOf(getMainLooper()).idle()

        // Assert: the image is saved.
        assertThat(callback.onDiskResult).isNotNull()
    }

    @Config(minSdk = 34)
    @Test
    fun processRequest_hasDiskResult_whenFormatIsJpegr() {
        // Arrange: create a request with callback.
        val callback = FakeTakePictureCallback()
        val request =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    OUTPUT_FILE_OPTIONS,
                    null,
                    Rect(0, 0, WIDTH, HEIGHT),
                    SENSOR_TO_BUFFER,
                    ROTATION_DEGREES,
                    /*jpegQuality=*/ 100,
                ),
                callback,
                Futures.immediateFuture(null),
            )

        // Act: process the request.
        val jpegBytes = createJpegrBytes(WIDTH, HEIGHT)
        val image = createJpegrFakeImageProxy(jpegBytes)
        processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, image))
        shadowOf(getMainLooper()).idle()

        // Assert: the image is saved.
        assertThat(callback.onDiskResult).isNotNull()
    }

    @Test
    fun processRequest_hasDiskResult_whenFormatIsRaw() {
        // Arrange: create a request with callback.
        processingNodeIn = ProcessingNode.In.of(RAW_SENSOR, listOf(RAW_SENSOR))
        node.transform(processingNodeIn)

        val callback = FakeTakePictureCallback()
        val request =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    OUTPUT_FILE_OPTIONS,
                    null,
                    Rect(0, 0, WIDTH, HEIGHT),
                    SENSOR_TO_BUFFER,
                    ROTATION_DEGREES,
                    /*jpegQuality=*/ 100,
                ),
                callback,
                Futures.immediateFuture(null),
            )

        // Act: process the request.
        val rawImage =
            createRawFakeImageProxy(
                captureResultImageInfo(mock(CaptureResult::class.java)),
                WIDTH,
                HEIGHT,
            )
        val dngImage2Disk = mock(DngImage2Disk::class.java)
        node.mDngImage2Disk = dngImage2Disk
        `when`(dngImage2Disk.apply(any(DngImage2Disk.In::class.java)))
            .thenReturn(mock(ImageCapture.OutputFileResults::class.java))
        processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, rawImage))
        shadowOf(getMainLooper()).idle()

        // Assert: the image is saved.
        assertThat(callback.onDiskResult).isNotNull()
    }

    @Test
    fun processRequest_hasDiskResult_whenSimultaneousCaptureEnabled() {
        // Arrange: create a request with callback.
        processingNodeIn = ProcessingNode.In.of(RAW_SENSOR, listOf(RAW_SENSOR, JPEG))
        node.transform(processingNodeIn)

        val callback = FakeTakePictureCallback()
        val request =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    OUTPUT_FILE_OPTIONS,
                    SECONDARY_OUTPUT_FILE_OPTIONS,
                    Rect(0, 0, WIDTH, HEIGHT),
                    SENSOR_TO_BUFFER,
                    ROTATION_DEGREES,
                    /*jpegQuality=*/ 100,
                    isSimultaneousCapture = true,
                ),
                callback,
                Futures.immediateFuture(null),
            )

        // Act: process the JPEG image.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val jpegImage = createJpegFakeImageProxy(jpegBytes)
        processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, jpegImage))
        shadowOf(getMainLooper()).idle()

        // Assert: JPEG on-disk result is delivered.
        assertThat(callback.onDiskResults.size).isEqualTo(1)

        // Act: process the RAW image.
        val rawImage =
            createRawFakeImageProxy(
                captureResultImageInfo(mock(CaptureResult::class.java)),
                WIDTH,
                HEIGHT,
            )
        val dngImage2Disk = mock(DngImage2Disk::class.java)
        node.mDngImage2Disk = dngImage2Disk
        `when`(dngImage2Disk.apply(any(DngImage2Disk.In::class.java)))
            .thenReturn(mock(ImageCapture.OutputFileResults::class.java))
        processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, rawImage))
        shadowOf(getMainLooper()).idle()

        // Assert: RAW on-disk result is also delivered, making total results 2.
        assertThat(callback.onDiskResults.size).isEqualTo(2)
    }

    @Test
    fun consecutiveRawCaptures_useCurrentCaptureResult() {
        verifyConsecutiveRawCaptures(isSimultaneousCapture = false)
    }

    @Test
    fun consecutiveRawJpegCaptures_useCurrentCaptureResult() {
        verifyConsecutiveRawCaptures(isSimultaneousCapture = true)
    }

    private fun verifyConsecutiveRawCaptures(isSimultaneousCapture: Boolean) {
        val formats = if (isSimultaneousCapture) listOf(RAW_SENSOR, JPEG) else listOf(RAW_SENSOR)
        processingNodeIn = ProcessingNode.In.of(RAW_SENSOR, formats)
        node.transform(processingNodeIn)
        val inputs = mutableListOf<DngImage2Disk.In>()
        val operation = mock(DngImage2Disk::class.java)
        node.mDngImage2Disk = operation
        `when`(operation.apply(any(DngImage2Disk.In::class.java))).thenAnswer { invocation ->
            inputs.add(invocation.getArgument(0))
            mock(ImageCapture.OutputFileResults::class.java)
        }
        val results = List(2) { mock(CaptureResult::class.java) }

        // A missing result after successful captures must not reuse the last metadata.
        (results + listOf<CaptureResult?>(null)).forEachIndexed { index, result ->
            val callback = FakeTakePictureCallback()
            val request =
                ProcessingRequest(
                    { listOf() },
                    createTakePictureRequest(
                        OUTPUT_FILE_OPTIONS,
                        if (isSimultaneousCapture) SECONDARY_OUTPUT_FILE_OPTIONS else null,
                        Rect(0, 0, WIDTH, HEIGHT),
                        SENSOR_TO_BUFFER,
                        ROTATION_DEGREES,
                        /*jpegQuality=*/ 100,
                        isSimultaneousCapture = isSimultaneousCapture,
                    ),
                    callback,
                    Futures.immediateFuture(null),
                )
            if (isSimultaneousCapture) {
                val jpeg = createJpegFakeImageProxy(createJpegBytes(WIDTH, HEIGHT))
                processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, jpeg))
            }
            val raw = createRawFakeImageProxy(captureResultImageInfo(result), WIDTH, HEIGHT)
            processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, raw))
            shadowOf(getMainLooper()).idle()

            if (result == null) {
                assertThat(callback.processFailure?.imageCaptureError)
                    .isEqualTo(ImageCapture.ERROR_UNKNOWN)
                assertThat(inputs).hasSize(results.size)
                assertThat(raw.isClosed).isTrue()
            } else {
                assertThat(callback.processFailure).isNull()
                assertThat(callback.onDiskResults).hasSize(formats.size)
                assertThat(inputs[index].captureResult).isSameInstanceAs(result)
            }
        }
    }

    private fun captureResultImageInfo(result: CaptureResult?) =
        CameraCaptureResultImageInfo(
            object : CameraCaptureResult by CAMERA_CAPTURE_RESULT {
                override fun getCaptureResult(): CaptureResult? = result
            }
        )

    @Test
    fun processRequest_hasInMemoryResult_whenSimultaneousCaptureEnabled() {
        // Arrange: create a request with callback.
        processingNodeIn = ProcessingNode.In.of(RAW_SENSOR, listOf(RAW_SENSOR, JPEG))
        node.transform(processingNodeIn)

        val callback = FakeTakePictureCallback()
        val request =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    null,
                    null,
                    Rect(0, 0, WIDTH, HEIGHT),
                    SENSOR_TO_BUFFER,
                    ROTATION_DEGREES,
                    /*jpegQuality=*/ 100,
                    isSimultaneousCapture = true,
                ),
                callback,
                Futures.immediateFuture(null),
            )

        // Act: process the JPEG image.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val jpegImage = createJpegFakeImageProxy(jpegBytes)
        processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, jpegImage))
        shadowOf(getMainLooper()).idle()

        // Assert: JPEG in-memory result is delivered.
        assertThat(callback.inMemoryResults.size).isEqualTo(1)
        assertThat(callback.inMemoryResults[0].format).isEqualTo(JPEG)

        // Act: process the RAW image.
        val rawImage =
            createRawFakeImageProxy(
                CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
                WIDTH,
                HEIGHT,
            )
        processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, rawImage))
        shadowOf(getMainLooper()).idle()

        // Assert: RAW in-memory result is also delivered, making total results 2.
        assertThat(callback.inMemoryResults.size).isEqualTo(2)
        assertThat(callback.inMemoryResults[1].format).isEqualTo(RAW_SENSOR)
    }

    @Test
    fun processAbortedRequest_noOps() {
        // Arrange: create a request with aborted callback.
        val callback = FakeTakePictureCallback()
        callback.aborted = true
        val request =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    OUTPUT_FILE_OPTIONS,
                    null,
                    Rect(0, 0, WIDTH, HEIGHT),
                    SENSOR_TO_BUFFER,
                    ROTATION_DEGREES,
                    /*jpegQuality=*/ 100,
                ),
                callback,
                Futures.immediateFuture(null),
            )

        // Act: process the request.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val image = createJpegFakeImageProxy(jpegBytes)
        processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, image))
        shadowOf(getMainLooper()).idle()

        // Assert: the image is not saved.
        assertThat(callback.onDiskResult).isNull()
    }

    @Test
    fun processRequest_postviewImagePropagated() {
        // Arrange: create a request with callback.
        val callback = FakeTakePictureCallback()
        val request =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    OUTPUT_FILE_OPTIONS,
                    null,
                    Rect(0, 0, WIDTH, HEIGHT),
                    SENSOR_TO_BUFFER,
                    ROTATION_DEGREES,
                    /*jpegQuality=*/ 100,
                ),
                callback,
                Futures.immediateFuture(null),
            )

        // Act: input the postview image.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val image = createJpegFakeImageProxy(jpegBytes)
        processingNodeIn.postviewEdge.accept(ProcessingNode.InputPacket.of(request, image))
        shadowOf(getMainLooper()).idle()

        // Assert: postview image is received.
        assertThat(callback.onPostviewBitmapAvailable).isNotNull()
    }

    @Test
    fun processAbortedRequest_postviewNotImagePropagated() {
        // Arrange: create a request with aborted callback.
        val callback = FakeTakePictureCallback()
        callback.aborted = true
        val request =
            ProcessingRequest(
                { listOf() },
                createTakePictureRequest(
                    OUTPUT_FILE_OPTIONS,
                    null,
                    Rect(0, 0, WIDTH, HEIGHT),
                    SENSOR_TO_BUFFER,
                    ROTATION_DEGREES,
                    /*jpegQuality=*/ 100,
                ),
                callback,
                Futures.immediateFuture(null),
            )

        // Act: input the postview image.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val image = createJpegFakeImageProxy(jpegBytes)
        processingNodeIn.postviewEdge.accept(ProcessingNode.InputPacket.of(request, image))
        shadowOf(getMainLooper()).idle()

        // Assert: the postview image is not received.
        assertThat(callback.onPostviewBitmapAvailable).isNull()
    }

    @Test
    fun saveIncorrectImage_getsErrorCallback() {
        // Arrange: create an invalid ImageProxy.
        val takePictureCallback = FakeTakePictureCallback()
        val image = FakeImageProxy(FakeImageInfo())
        val processingRequest = createProcessingRequest(takePictureCallback)
        val input = ProcessingNode.InputPacket.of(processingRequest, image)

        // Act: send input to the edge and wait for callback
        processingNodeIn.edge.accept(input)
        shadowOf(getMainLooper()).idle()

        // Assert: receives a process failure and closes the input ImageProxy.
        assertThat(takePictureCallback.processFailure)
            .isInstanceOf(ImageCaptureException::class.java)
        assertThat(image.isClosed).isTrue()
    }

    @Test
    fun singleExecutorForLowMemoryQuirkEnabled() {
        listOf("sm-a520w", "motog3").forEach { model ->
            setStaticField(Build::class.java, "MODEL", model)
            assertThat(
                    isSequentialExecutor(
                        ProcessingNode(mainThreadExecutor(), cameraCharacteristics)
                            .mBlockingExecutor
                    )
                )
                .isTrue()
        }
    }

    @Test
    fun canProcessOnDiskCaptureForA24ProblematicJpegMetadata() {
        setStaticField(Build::class.java, "BRAND", "SAMSUNG")
        setStaticField(Build::class.java, "DEVICE", "a24")

        // Creates the ProcessingNode after updating the device name to load the correct quirks
        node = ProcessingNode(mainThreadExecutor(), cameraCharacteristics)

        processingNodeIn = ProcessingNode.In.of(JPEG, listOf(JPEG))
        node.transform(processingNodeIn)

        // Arrange: create an invalid ImageProxy.
        val takePictureCallback = FakeTakePictureCallback()
        val brokenJpegByteArray = createA24ProblematicJpegByteArray(WIDTH, HEIGHT)
        val image =
            createJpegFakeImageProxy(
                CameraCaptureResultImageInfo(CAMERA_CAPTURE_RESULT),
                brokenJpegByteArray,
                WIDTH,
                HEIGHT,
            )
        val processingRequest = createProcessingRequest(takePictureCallback)
        val input = ProcessingNode.InputPacket.of(processingRequest, image)

        // Act: send input to the edge and wait for callback
        processingNodeIn.edge.accept(input)
        shadowOf(getMainLooper()).idle()

        // Assert: can process the problematic A24 JPEG byte array successfully.
        assertThat(takePictureCallback.processFailure).isNull()
    }
}
