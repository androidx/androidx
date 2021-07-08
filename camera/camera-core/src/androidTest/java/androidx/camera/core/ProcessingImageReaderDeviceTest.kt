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

package androidx.camera.core

import android.graphics.ImageFormat
import android.media.ImageWriter
import android.util.Pair
import android.util.Size
import android.view.Surface
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CaptureBundle
import androidx.camera.core.impl.CaptureProcessor
import androidx.camera.core.impl.ImageProxyBundle
import androidx.camera.core.impl.ImageReaderProxy
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeCameraCaptureResult
import androidx.camera.testing.fakes.FakeCaptureStage
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23) // This test uses ImageWriter which is supported from api 23.
class ProcessingImageReaderDeviceTest {
    private companion object Bundle {
        private const val CAPTURE_ID_0 = 0
        private const val CAPTURE_ID_1 = 1
        private const val TIMESTAMP_0 = 0L
        private const val TIMESTAMP_1 = 1000L
    }

    // A processor that will generate a garbage image but has the timestamp of the first image in
    // the bundle
    private val mProcessor = object : CaptureProcessor {
        private lateinit var mImageWriter: ImageWriter

        override fun onOutputSurface(surface: Surface, imageFormat: Int) {
            mImageWriter = ImageWriter.newInstance(surface, 2)
        }

        override fun process(bundle: ImageProxyBundle) {
            val image = mImageWriter.dequeueInputImage()
            image.timestamp = bundle.getImageProxy(bundle.captureIds[0]).get().imageInfo.timestamp
            mImageWriter.queueInputImage(image)
        }

        override fun onResolutionUpdate(size: Size) = Unit
    }

    private val mCaptureStage0 = FakeCaptureStage(CAPTURE_ID_0, null)
    private val mCaptureStage1 = FakeCaptureStage(CAPTURE_ID_1, null)

    private lateinit var mCaptureBundle: CaptureBundle

    @Before
    fun setUp() {
        mCaptureBundle = CaptureBundles.createCaptureBundle(mCaptureStage0, mCaptureStage1)
    }

    @Test
    fun processesImage_whenImageInBundleEnqueued() = runBlocking {
        val processingImageReader = ProcessingImageReader.Builder(
            640,
            480,
            ImageFormat.YUV_420_888,
            2,
            mCaptureBundle,
            mProcessor
        ).build()

        val job = async {
            suspendCoroutine<ImageProxy?> { cont ->
                // Waiting on the ProcessingImageReader to produce an ImageProxy
                processingImageReader.setOnImageAvailableListener(
                    ImageReaderProxy.OnImageAvailableListener { imageReader ->
                        cont.resume(imageReader.acquireNextImage())
                    },
                    CameraXExecutors.directExecutor()
                )

                processingImageReader.setCaptureBundle(mCaptureBundle)
                val imageWriter = ImageWriter.newInstance(processingImageReader.surface!!, 2)
                val callback = processingImageReader.cameraCaptureCallback!!

                // Trigger the bundle of images required for processing to occur
                triggerImage(imageWriter, callback, TIMESTAMP_0, CAPTURE_ID_0)
                triggerImage(imageWriter, callback, TIMESTAMP_1, CAPTURE_ID_1)
            }
        }
        val image = job.await()

        // Check the values of the images that are captured
        assertThat(image).isNotNull()
        assertThat(image!!.imageInfo.timestamp).isEqualTo(TIMESTAMP_0)
    }

    private fun triggerImage(
        imageWriter: ImageWriter,
        callback: CameraCaptureCallback,
        timestamp: Long,
        captureId: Int
    ) {
        val image = imageWriter.dequeueInputImage()
        image.timestamp = timestamp
        imageWriter.queueInputImage(image)
        val fakeCameraCaptureResult = FakeCameraCaptureResult()
        fakeCameraCaptureResult.timestamp = timestamp
        val tagBundle = TagBundle.create(
            Pair(
                mCaptureBundle.hashCode().toString(),
                captureId
            )
        )
        fakeCameraCaptureResult.setTag(tagBundle)
        callback.onCaptureCompleted(fakeCameraCaptureResult)
    }
}
