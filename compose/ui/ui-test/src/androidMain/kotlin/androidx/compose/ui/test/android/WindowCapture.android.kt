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

package androidx.compose.ui.test.android

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.TestContext
import androidx.core.graphics.createBitmap
import androidx.test.platform.graphics.HardwareRendererCompat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.O)
internal fun Window.captureRegionToImage(
    testContext: TestContext,
    boundsInWindow: Rect,
): ImageBitmap {
    lateinit var imageBitmap: ImageBitmap
    runWithRetryWhenNoData {
        // Turn on hardware rendering, if necessary
        imageBitmap = withDrawingEnabled {
            // First force drawing to happen
            decorView.forceRedraw(testContext)
            // Then we generate the bitmap
            generateBitmap(boundsInWindow).asImageBitmap()
        }
        // We cannot rely entirely on addOnDrawListener (used in forceRedraw) because it only
        // signals that the UI thread has finished its draw pass, not that the RenderThread has
        // finished outputting pixels to the buffer. On API < 29 or without hardware acceleration,
        // this race condition can result in a fully transparent bitmap, so we must explicitly
        // check for it.
        if (
            (Build.VERSION.SDK_INT < 29 || !decorView.isHardwareAccelerated) &&
                isEntirelyTransparent(imageBitmap)
        ) {
            throw TransparentBitmapException()
        }
    }
    return imageBitmap
}

/**
 * Executes a given block of code with a retry mechanism, specifically designed to handle transient
 * rendering issues during [PixelCopy] operations.
 *
 * It attempts to run the [retryBlock] up to 3 times, handling two known transient states:
 * 1. [PixelCopy.ERROR_SOURCE_NO_DATA]: Retries immediately without a delay.
 * 2. [TransparentBitmapException]: Retries after a brief delay to allow the RenderThread to catch
 *    up.
 *
 * @throws PixelCopyException If the block fails with `ERROR_SOURCE_NO_DATA` after max retries.
 * @throws Exception Any unexpected exception thrown by the [retryBlock].
 */
@Suppress("BanThreadSleep", "InlinedApi")
@VisibleForTesting
internal fun runWithRetryWhenNoData(retryBlock: () -> Unit) {
    var retryAttempts = 0
    val maxAttempts = 3
    var currentDelayMs = 16L
    while (retryAttempts < maxAttempts) {
        retryAttempts++
        try {
            retryBlock()
            return
        } catch (e: Exception) {
            val isSourceNoData =
                e is PixelCopyException && e.copyResultStatus == PixelCopy.ERROR_SOURCE_NO_DATA
            val isTransparent = e is TransparentBitmapException
            if (!isSourceNoData && !isTransparent) throw e

            if (retryAttempts == maxAttempts) {
                if (isSourceNoData) {
                    throw PixelCopyException(
                        e.copyResultStatus,
                        "PixelCopy failed with result ERROR_SOURCE_NO_DATA after 3 retry attempts!",
                    )
                }
                // Swallow TransparentBitmapException on the final attempt and exit cleanly.
                return
            }
            // Give RenderThread time to catch up
            if (isTransparent) {
                Thread.sleep(currentDelayMs)
                currentDelayMs *= 2
            }
        }
    }
}

private fun <R> withDrawingEnabled(block: () -> R): R {
    val wasDrawingEnabled = HardwareRendererCompat.isDrawingEnabled()
    try {
        if (!wasDrawingEnabled) {
            HardwareRendererCompat.setDrawingEnabled(true)
        }
        return block.invoke()
    } finally {
        if (!wasDrawingEnabled) {
            HardwareRendererCompat.setDrawingEnabled(false)
        }
    }
}

internal fun View.forceRedraw(testContext: TestContext) {
    var drawDone = false
    handler.post {
        if (Build.VERSION.SDK_INT >= 29 && isHardwareAccelerated) {
            FrameCommitCallbackHelper.registerFrameCommitCallback(viewTreeObserver) {
                drawDone = true
            }
        } else {
            viewTreeObserver.addOnDrawListener(
                object : ViewTreeObserver.OnDrawListener {
                    var handled = false

                    override fun onDraw() {
                        if (!handled) {
                            handled = true
                            handler.postAtFrontOfQueue {
                                drawDone = true
                                viewTreeObserver.removeOnDrawListener(this)
                            }
                        }
                    }
                }
            )
        }
        invalidate()
    }

    testContext.testOwner.mainClock.waitUntil(timeoutMillis = 2_000) { drawDone }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Window.generateBitmap(boundsInWindow: Rect): Bitmap {
    val destBitmap = createBitmap(boundsInWindow.width(), boundsInWindow.height())
    generateBitmapFromPixelCopy(boundsInWindow, destBitmap)
    return destBitmap
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Window.generateBitmapFromPixelCopy(boundsInWindow: Rect, destBitmap: Bitmap) {
    val latch = CountDownLatch(1)
    var copyResult = 0
    val onCopyFinished =
        PixelCopy.OnPixelCopyFinishedListener { result ->
            copyResult = result
            latch.countDown()
        }
    PixelCopyHelper.request(
        this,
        boundsInWindow,
        destBitmap,
        onCopyFinished,
        Handler(Looper.getMainLooper()),
    )

    if (!latch.await(1, TimeUnit.SECONDS)) {
        throw AssertionError("Failed waiting for PixelCopy!")
    }
    if (copyResult != PixelCopy.SUCCESS) {
        throw PixelCopyException(copyResultStatus = copyResult)
    }
}

internal class PixelCopyException(val copyResultStatus: Int, message: String? = null) :
    RuntimeException(message ?: "PixelCopy failed with result $copyResultStatus!")

internal class TransparentBitmapException : RuntimeException()

// Unfortunately this is a copy-paste from AndroidComposeTestRule. At this moment it is a bit
// tricky to share this method. We can expose it on TestOwner in theory.
@Suppress("BanThreadSleep")
private fun MainTestClock.waitUntil(timeoutMillis: Long, condition: () -> Boolean) {
    val startTime = System.nanoTime()
    while (!condition()) {
        if (autoAdvance) {
            advanceTimeByFrame()
        }
        // Let Android run measure, draw and in general any other async operations.
        Thread.sleep(10)
        if (System.nanoTime() - startTime > timeoutMillis * 1_000_000) {
            throw ComposeTimeoutException("Condition still not satisfied after $timeoutMillis ms")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private object FrameCommitCallbackHelper {
    fun registerFrameCommitCallback(viewTreeObserver: ViewTreeObserver, runnable: Runnable) {
        viewTreeObserver.registerFrameCommitCallback(runnable)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private object PixelCopyHelper {
    fun request(
        source: Window,
        srcRect: Rect?,
        dest: Bitmap,
        listener: PixelCopy.OnPixelCopyFinishedListener,
        listenerThread: Handler,
    ) {
        PixelCopy.request(source, srcRect, dest, listener, listenerThread)
    }
}

private fun isEntirelyTransparent(imageBitmap: ImageBitmap): Boolean {
    if (!imageBitmap.hasAlpha) return false

    val width = imageBitmap.width
    val height = imageBitmap.height
    val pixels = IntArray(width)

    for (y in 0 until height) {
        imageBitmap.readPixels(buffer = pixels, startY = y, width = width, height = 1)
        for (pixel in pixels) {
            if ((pixel ushr 24) > 0) {
                return false
            }
        }
    }
    return true
}
