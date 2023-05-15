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

package androidx.camera.core.processing

import android.graphics.PixelFormat
import android.os.Build
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProcessor
import androidx.camera.core.ImageProcessor.Response
import androidx.camera.core.ProcessingException
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.testing.fakes.FakeImageEffect
import androidx.camera.testing.fakes.FakeImageInfo
import androidx.camera.testing.fakes.FakeImageProxy
import com.google.common.truth.Truth.assertThat
import java.lang.Thread.currentThread
import java.util.concurrent.Executors.newSingleThreadExecutor
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [InternalImageProcessor].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class InternalImageProcessorTest {

    companion object {
        private const val THREAD_NAME = "thread_name"
    }

    @Test
    fun processorThrowsError_errorIsPropagatedToCameraX() {
        // Arrange.
        val exception = ProcessingException()
        var errorReceived: Throwable? = null
        val cameraEffect = FakeImageEffect(
            directExecutor(),
            {
                throw exception
            },
            {
                errorReceived = it
            })
        val imageProcessor = InternalImageProcessor(cameraEffect)

        // Act.
        try {
            imageProcessor.safeProcess(
                ImageProcessorRequest(
                    FakeImageProxy(FakeImageInfo()),
                    PixelFormat.RGBA_8888
                )
            )
            fail("Processor should throw exception")
        } catch (ex: ImageCaptureException) {
            // Assert.
            assertThat(ex.cause).isEqualTo(exception)
        }
        assertThat(errorReceived).isEqualTo(exception)
    }

    @Test
    fun process_appCallbackInvokedOnAppExecutor() {
        // Arrange.
        val imageToEffect = FakeImageProxy(FakeImageInfo())
        val imageFromEffect = FakeImageProxy(FakeImageInfo())
        var calledThreadName = ""
        val processor = ImageProcessor {
            calledThreadName = currentThread().name
            Response {
                imageFromEffect
            }
        }
        val executor = newSingleThreadExecutor { Thread(it, THREAD_NAME) }
        val cameraEffect = FakeImageEffect(executor, processor)
        val imageProcessor = InternalImageProcessor(cameraEffect)

        // Act.
        val outputImage = imageProcessor.safeProcess(
            ImageProcessorRequest(imageToEffect, PixelFormat.RGBA_8888)
        ).outputImage

        // Assert.
        assertThat(outputImage).isEqualTo(imageFromEffect)
        assertThat(calledThreadName).isEqualTo(THREAD_NAME)
    }
}