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
package androidx.camera.core

import android.graphics.ImageFormat
import android.media.ImageWriter
import android.os.Handler
import android.os.HandlerThread
import android.util.Pair
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.CaptureProcessor
import androidx.camera.core.impl.CaptureStage
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImageProxyBundle
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.fakes.FakeCameraCaptureResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 23) // This test uses ImageWriter which is supported from api 23.
class ProcessingSurfaceTest {
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val captureStage: CaptureStage = CaptureStage.DefaultCaptureStage()
    private val processingSurfaces: MutableList<ProcessingSurface> = ArrayList()
    private val captureProcessor: FakeCaptureProcessor = FakeCaptureProcessor(captureStage.id)

    @Before
    fun setup() {
        backgroundThread = HandlerThread("CallbackThread")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    @After
    fun tearDown() {
        for (processingSurface in processingSurfaces) {
            processingSurface.close()
        }
        processingSurfaces.clear()
        backgroundThread!!.looper.quitSafely()
    }

    @Test
    fun validInputSurface() {
        val processingSurface = createProcessingSurface(
            newImmediateSurfaceDeferrableSurface()
        )
        val surface = processingSurface.surface.get()
        assertThat(surface).isNotNull()
    }

    @Test
    fun writeToInputSurface_userOutputSurfaceReceivesFrame() {
        // Arrange.
        val frameReceivedSemaphore = Semaphore(0)
        val imageReaderProxy = ImageReaderProxys.createIsolatedReader(
            RESOLUTION.width, RESOLUTION.height,
            ImageFormat.YUV_420_888, 2
        )
        imageReaderProxy.setOnImageAvailableListener(
            { frameReceivedSemaphore.release() },
            CameraXExecutors.directExecutor()
        )

        // Create ProcessingSurface with user Surface.
        val processingSurface = createProcessingSurface(
            object : DeferrableSurface() {
                override fun provideSurface(): ListenableFuture<Surface> {
                    return Futures.immediateFuture(imageReaderProxy.surface)
                }
            })

        // Act: Send one frame to processingSurface.
        triggerImage(processingSurface, 1)

        // Assert: verify that the frame has been received or time-out after 3 second.
        assertThat(frameReceivedSemaphore.tryAcquire(3, TimeUnit.SECONDS)).isTrue()
    }

    // Exception should be thrown here
    @Test
    fun getSurfaceThrowsExceptionWhenClosed() {
        val processingSurface = createProcessingSurface(newImmediateSurfaceDeferrableSurface())
        processingSurface.close()

        // Exception should be thrown here
        val futureSurface = processingSurface.surface
        var cause: Throwable? = null
        try {
            futureSurface.get()
        } catch (e: ExecutionException) {
            cause = e.cause
        } catch (e: InterruptedException) {
            cause = e.cause
        }
        assertThat(cause).isInstanceOf(DeferrableSurface.SurfaceClosedException::class.java)
    }

    // Exception should be thrown here
    @Test(expected = IllegalStateException::class)
    fun getCameraCaptureCallbackThrowsExceptionWhenReleased() {
        val processingSurface = createProcessingSurface(newImmediateSurfaceDeferrableSurface())
        processingSurface.close()

        // Exception should be thrown here
        processingSurface.cameraCaptureCallback
    }

    @Test
    fun completeTerminationFutureAfterProcessIsFinished() {
        // Arrange.
        val processingSurface = createProcessingSurface(newImmediateSurfaceDeferrableSurface())

        // Sets up the processor blocker to block the CaptureProcessor#process() function execution.
        captureProcessor.setupProcessorBlocker()

        // Monitors whether the ProcessingSurface termination future has been completed.
        val terminationCountDownLatch = CountDownLatch(1)
        processingSurface.terminationFuture.addListener({
            terminationCountDownLatch.countDown()
        }, Executors.newSingleThreadExecutor())

        // Act: Sends one frame to processingSurface.
        triggerImage(processingSurface, 1)

        // Waits for that the CaptureProcessor#process() function is called. Otherwise, the
        // following ProcessingSurface#close() function call may directly close the
        // ProcessingSurface.
        assertThat(captureProcessor.awaitProcessingState(1000, TimeUnit.MILLISECONDS)).isTrue()

        // Act: triggers the ProcessingSurface close flow.
        processingSurface.close()

        // Assert: verify that the termination future won't be completed before
        // CaptureProcessor#process() execution is finished.
        assertThat(terminationCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isFalse()

        // Act: releases the processor blocker to finish the CaptureProcessor#process() execution.
        captureProcessor.releaseProcessorBlocker()

        // Assert: verify that the termination future is completed after CaptureProcessor#process()
        // execution is finished.
        assertThat(terminationCountDownLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue()
    }

    @RequiresApi(23)
    @Throws(ExecutionException::class, InterruptedException::class)
    private fun triggerImage(processingSurface: ProcessingSurface, timestamp: Long) {
        val surface = processingSurface.surface.get()
        val imageWriter = ImageWriter.newInstance(surface, 2)
        val image = imageWriter.dequeueInputImage()
        image.timestamp = timestamp
        imageWriter.queueInputImage(image)
        val callback = processingSurface.cameraCaptureCallback
        val cameraCaptureResult = FakeCameraCaptureResult()
        cameraCaptureResult.timestamp = timestamp
        cameraCaptureResult.setTag(
            TagBundle.create(
                Pair(Integer.toString(captureStage.hashCode()), captureStage.id)
            )
        )
        callback!!.onCaptureCompleted(cameraCaptureResult)
    }

    private fun createProcessingSurface(
        deferrableSurface: DeferrableSurface
    ): ProcessingSurface {
        val processingSurface = ProcessingSurface(
            RESOLUTION.width,
            RESOLUTION.height,
            FORMAT,
            backgroundHandler,
            captureStage,
            captureProcessor,
            deferrableSurface, Integer.toString(captureStage.hashCode())
        )
        processingSurfaces.add(processingSurface)
        return processingSurface
    }

    private fun newImmediateSurfaceDeferrableSurface(): DeferrableSurface {
        val imageReaderProxy = ImageReaderProxys.createIsolatedReader(
            RESOLUTION.width, RESOLUTION.height,
            ImageFormat.YUV_420_888, 2
        )

        val deferrableSurface = ImmediateSurface(imageReaderProxy.surface!!)

        deferrableSurface.terminationFuture.addListener(
            { imageReaderProxy.close() },
            CameraXExecutors.directExecutor()
        )

        return deferrableSurface
    }

    companion object {
        private val RESOLUTION: Size by lazy { Size(640, 480) }
        private const val FORMAT = ImageFormat.YUV_420_888
    }

    /**
     * Capture processor that can write out an empty image to exercise the pipeline.
     *
     * <p>The fake capture processor can be controlled to be blocked in the processing state and
     * then release the blocker to complete it.
     */
    @RequiresApi(23)
    private class FakeCaptureProcessor(private val captureStageId: Int) : CaptureProcessor {
        private val processingCountDownLatch = CountDownLatch(1)
        private var processorBlockerCountDownLatch: CountDownLatch? = null

        var imageWriter: ImageWriter? = null
        override fun onOutputSurface(surface: Surface, imageFormat: Int) {
            imageWriter = ImageWriter.newInstance(surface, 2)
        }

        override fun process(bundle: ImageProxyBundle) {
            processingCountDownLatch.countDown()
            try {
                val imageProxyListenableFuture = bundle.getImageProxy(captureStageId)
                val imageProxy = imageProxyListenableFuture[100, TimeUnit.MILLISECONDS]
                val image = imageWriter!!.dequeueInputImage()
                image.timestamp = imageProxy.imageInfo.timestamp
                imageWriter!!.queueInputImage(image)

                processorBlockerCountDownLatch?.await()
            } catch (_: ExecutionException) {
            } catch (_: TimeoutException) {
            } catch (_: InterruptedException) {
            }
        }

        override fun onResolutionUpdate(size: Size) {}

        fun awaitProcessingState(timeout: Long, timeUnit: TimeUnit): Boolean {
            return processingCountDownLatch.await(timeout, timeUnit)
        }

        fun setupProcessorBlocker() {
            if (processorBlockerCountDownLatch == null) {
                processorBlockerCountDownLatch = CountDownLatch(1)
            }
        }

        fun releaseProcessorBlocker() {
            processorBlockerCountDownLatch?.countDown()
        }
    }
}
