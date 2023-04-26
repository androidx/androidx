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

import android.graphics.ImageFormat
import android.graphics.Rect
import android.os.Build
import android.os.Looper.getMainLooper
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.OUTPUT_FILE_OPTIONS
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.SENSOR_TO_BUFFER
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.imagecapture.Utils.createProcessingRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors.isSequentialExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.TestImageUtil.createJpegBytes
import androidx.camera.testing.TestImageUtil.createJpegFakeImageProxy
import androidx.camera.testing.fakes.FakeImageInfo
import androidx.camera.testing.fakes.FakeImageProxy
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers.setStaticField

/**
 * Unit tests for [ProcessingNode].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ProcessingNodeTest {

    private lateinit var processingNodeIn: ProcessingNode.In

    private val node = ProcessingNode(mainThreadExecutor())

    @Before
    fun setUp() {
        processingNodeIn = ProcessingNode.In.of(ImageFormat.JPEG, ImageFormat.JPEG)
        node.transform(processingNodeIn)
    }

    @Test
    fun processAbortedRequest_noOps() {
        // Arrange: create a request with aborted callback.
        val callback = FakeTakePictureCallback()
        callback.aborted = true
        val request = ProcessingRequest(
            { listOf() },
            OUTPUT_FILE_OPTIONS,
            Rect(0, 0, WIDTH, HEIGHT),
            ROTATION_DEGREES,
            /*jpegQuality=*/100,
            SENSOR_TO_BUFFER,
            callback,
            Futures.immediateFuture(null)
        )

        // Act: process the request.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val image = createJpegFakeImageProxy(jpegBytes)
        processingNodeIn.edge.accept(ProcessingNode.InputPacket.of(request, image, false))
        shadowOf(getMainLooper()).idle()

        // Assert: the image is not saved.
        assertThat(callback.onDiskResult).isNull()
    }

    @Test
    fun saveIncorrectImage_getsErrorCallback() {
        // Arrange: create an invalid ImageProxy.
        val takePictureCallback = FakeTakePictureCallback()
        val image = FakeImageProxy(FakeImageInfo())
        val processingRequest = createProcessingRequest(takePictureCallback)
        val input = ProcessingNode.InputPacket.of(processingRequest, image, false)

        // Act: send input to the edge and wait for callback
        processingNodeIn.edge.accept(input)
        shadowOf(getMainLooper()).idle()

        // Assert: receives a process failure.
        assertThat(takePictureCallback.processFailure)
            .isInstanceOf(ImageCaptureException::class.java)
    }

    @Test
    fun singleExecutorForLowMemoryQuirkEnabled() {
        listOf("sm-a520w", "motog3").forEach { model ->
            setStaticField(Build::class.java, "MODEL", model)
            assertThat(
                isSequentialExecutor(ProcessingNode(mainThreadExecutor()).mBlockingExecutor)
            ).isTrue()
        }
    }
}
