/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.scrollcapture

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.ColorSpace
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.hardware.HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
import android.hardware.HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
import android.media.Image
import android.media.ImageReader
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.view.ScrollCaptureCallback
import android.view.ScrollCaptureSession
import android.view.ScrollCaptureTarget
import android.view.Surface
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlin.test.fail
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Helps tests pretend to be the Android platform performing scroll capture search and image
 * capture. Tests must call [setContent] on this class instead of on [rule].
 */
@RequiresApi(31)
class ScrollCaptureTester(private val rule: ComposeContentTestRule) {
    private var view: View? = null
    private var coroutineScope: CoroutineScope? = null

    fun setContent(content: @Composable () -> Unit) {
        rule.setContent {
            this.view = LocalView.current
            this.coroutineScope = rememberCoroutineScope()
            content()
        }
    }

    /**
     * Calls [View.onScrollCaptureSearch] on the Compose host view, which searches the composition
     * from [setContent] for scroll containers, and returns all the [ScrollCaptureTarget]s produced
     * that would be given to the platform in production.
     */
    fun findCaptureTargets(): List<ScrollCaptureTarget> = rule.runOnIdle {
        val view = checkNotNull(view as? AndroidComposeView) {
            "Must call setContent on ScrollCaptureTester before capturing."
        }
        val localVisibleRect = Rect().also(view::getLocalVisibleRect)
        val windowOffset = view.calculatePositionInWindow(Offset.Zero).roundToPoint()
        val targets = mutableListOf<ScrollCaptureTarget>()
        view.onScrollCaptureSearch(localVisibleRect, windowOffset, targets::add)
        targets
    }

    /**
     * Emulates (roughly) how the platform interacts with [ScrollCaptureCallback] to iteratively
     * assemble a screenshot of the entire contents of the [target]. Unlike the platform, this
     * method will not limit itself to a certain size, it always captures the entire scroll
     * contents, so tests should make sure to use small enough scroll contents or the test might
     * run out of memory.
     *
     * @param captureHeight The height of the capture window. Must not be greater than viewport
     * height.
     */
    fun captureBitmapsVertically(target: ScrollCaptureTarget, captureHeight: Int): List<Bitmap> {
        val scope = rule.runOnIdle {
            checkNotNull(coroutineScope) {
                "Must call setContent on ScrollCaptureTest before capturing."
            }
        }
        val bitmapsFromTop = mutableListOf<Bitmap>()

        // This coroutine will run on the main thread, no need to use runOnUiThread.
        val captureJob = scope.launch {
            runCaptureSession(target, captureHeight, onBitmap = bitmapsFromTop::add)
        }

        rule.waitUntil(3_000) { captureJob.isCompleted }
        return bitmapsFromTop
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun runCaptureSession(
        target: ScrollCaptureTarget,
        captureHeight: Int,
        onBitmap: (Bitmap) -> Unit
    ) {
        val callback = target.callback
        // Use the bounds returned from the callback, not the ones from the target, because that's
        // what the system does.
        val scrollBounds = callback.onScrollCaptureSearch()
        val captureWidth = scrollBounds.width()
        requirePrecondition(captureHeight <= scrollBounds.height()) {
            "Expected windowSize ($captureHeight) ≤ viewport height (${scrollBounds.height()})"
        }

        withSurfaceBitmaps(captureWidth, captureHeight) { surface, bitmapsFromSurface ->
            val session = ScrollCaptureSession(
                surface,
                scrollBounds,
                target.positionInWindow
            )
            callback.onScrollCaptureStart(session)

            var captureOffset = Point(0, 0)
            var goingUp = true
            // Starting with the original viewport, scrolls all the way to the top, then all the way
            // back down, capturing images on the way down until it hits the bottom.
            while (true) {
                val requestedCaptureArea = Rect(
                    captureOffset.x,
                    captureOffset.y,
                    captureOffset.x + captureWidth,
                    captureOffset.y + captureHeight
                )
                val resultCaptureArea =
                    callback.onScrollCaptureImageRequest(session, requestedCaptureArea)

                // Empty results shouldn't produce an image.
                if (!resultCaptureArea.isEmpty) {
                    val bitmap = bitmapsFromSurface.receiveWithTimeout(1_000) {
                        "No bitmap received after 1 second for capture area $resultCaptureArea"
                    }

                    // Only collect the returned images on the way down.
                    if (!goingUp) {
                        onBitmap(bitmap)
                    } else {
                        bitmap.recycle()
                    }
                }

                if (resultCaptureArea != requestedCaptureArea) {
                    // We found the top or bottom.
                    if (goingUp) {
                        // "Bounce" off the top: Change direction and start re-capturing down.
                        goingUp = false
                        captureOffset = Point(0, resultCaptureArea.top)
                    } else {
                        // If we hit the bottom then we're done.
                        break
                    }
                } else {
                    // We can keep going in the same direction, offset the capture window and loop.
                    captureOffset = if (goingUp) {
                        Point(0, resultCaptureArea.top - captureHeight)
                    } else {
                        Point(0, resultCaptureArea.bottom)
                    }
                }
            }
        }

        callback.onScrollCaptureEnd()
    }

    /**
     * Creates a [Surface] passes it to [block] along with a channel that will receive all images
     * written to the [Surface].
     */
    private suspend inline fun withSurfaceBitmaps(
        width: Int,
        height: Int,
        crossinline block: suspend (Surface, ReceiveChannel<Bitmap>) -> Unit
    ) {
        coroutineScope {
            // ImageReader gives us the Surface that we'll provide to the session.
            ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                // Each image is read, processed, and closed before the next request to draw is made,
                // so we don't need multiple images.
                /* maxImages= */ 1,
                USAGE_GPU_SAMPLED_IMAGE or USAGE_GPU_COLOR_OUTPUT
            ).use { imageReader ->
                val bitmapsChannel = Channel<Bitmap>(capacity = Channel.RENDEZVOUS)

                // Must register the OnImageAvailableListener before any code in block runs to avoid
                // race conditions.
                val imageCollectorJob = launch(start = CoroutineStart.UNDISPATCHED) {
                    imageReader.collectImages {
                        val bitmap = it.toSoftwareBitmap()
                        bitmapsChannel.send(bitmap)
                    }
                }

                try {
                    block(imageReader.surface, bitmapsChannel)
                    // ImageReader has no signal that it's finished, so in the happy path we have to
                    // stop the collector job explicitly.
                    imageCollectorJob.cancel()
                } finally {
                    bitmapsChannel.close()
                }
            }
        }
    }

    /**
     * Reads all images from this [ImageReader] and passes them to [onImage]. The [Image] will
     * automatically be closed when [onImage] returns.
     *
     * Propagates backpressure to the [ImageReader] – only one image will be acquired from the
     * [ImageReader] at a time, and the next image won't be acquired until [onImage] returns.
     */
    private suspend inline fun ImageReader.collectImages(onImage: (Image) -> Unit): Nothing {
        val imageAvailableChannel = Channel<Unit>(capacity = Channel.CONFLATED)
        setOnImageAvailableListener(
            { imageAvailableChannel.trySend(Unit) },
            Handler(Looper.getMainLooper())
        )
        val context = currentCoroutineContext()

        try {
            // Read all images until cancelled.
            while (true) {
                context.ensureActive()
                // Fast path – if an image is immediately available, don't suspend.
                var image: Image? = acquireNextImage()
                // If no image was available, suspend until the callback fires.
                while (image == null) {
                    imageAvailableChannel.receive()
                    image = acquireNextImage()
                }
                image.use { onImage(image) }
            }
        } finally {
            setOnImageAvailableListener(null, null)
        }
    }

    /**
     * Helper function for converting an [Image] to a [Bitmap] by copying the hardware buffer into
     * a software bitmap.
     */
    private fun Image.toSoftwareBitmap(): Bitmap {
        val hardwareBuffer = checkPreconditionNotNull(hardwareBuffer) { "No hardware buffer" }
        hardwareBuffer.use {
            val hardwareBitmap = Bitmap.wrapHardwareBuffer(
                hardwareBuffer,
                ColorSpace.get(ColorSpace.Named.SRGB)
            ) ?: error("wrapHardwareBuffer returned null")
            try {
                return hardwareBitmap.copy(ARGB_8888, false)
            } finally {
                hardwareBitmap.recycle()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend inline fun <E> ReceiveChannel<E>.receiveWithTimeout(
        timeoutMillis: Long,
        crossinline timeoutMessage: () -> String
    ): E = select {
        onReceive { it }
        onTimeout(timeoutMillis) { fail(timeoutMessage()) }
    }
}

/**
 * Helper for calling [ScrollCaptureCallback.onScrollCaptureSearch] from a suspend function.
 * The [CancellationSignal] and continuation callback are generated from the coroutine.
 */
@RequiresApi(31)
suspend fun ScrollCaptureCallback.onScrollCaptureSearch(): Rect =
    suspendCancellableCoroutine { continuation ->
        onScrollCaptureSearch(continuation.createCancellationSignal()) {
            continuation.resume(it)
        }
    }

/**
 * Helper for calling [ScrollCaptureCallback.onScrollCaptureStart] from a suspend function.
 * The [CancellationSignal] and continuation callback are generated from the coroutine.
 */
@RequiresApi(31)
suspend fun ScrollCaptureCallback.onScrollCaptureStart(session: ScrollCaptureSession) {
    suspendCancellableCoroutine { continuation ->
        onScrollCaptureStart(session, continuation.createCancellationSignal()) {
            continuation.resume(Unit)
        }
    }
}

/**
 * Helper for calling [ScrollCaptureCallback.onScrollCaptureImageRequest] from a suspend function.
 * The [CancellationSignal] and continuation callback are generated from the coroutine.
 */
@RequiresApi(31)
suspend fun ScrollCaptureCallback.onScrollCaptureImageRequest(
    session: ScrollCaptureSession,
    captureArea: Rect
): Rect = suspendCancellableCoroutine { continuation ->
    onScrollCaptureImageRequest(
        session,
        continuation.createCancellationSignal(),
        captureArea
    ) {
        continuation.resume(it)
    }
}

/**
 * Helper for calling [ScrollCaptureCallback.onScrollCaptureEnd] from a suspend function.
 * The [CancellationSignal] and continuation callback are generated from the coroutine.
 */
@RequiresApi(31)
suspend fun ScrollCaptureCallback.onScrollCaptureEnd() {
    suspendCancellableCoroutine { continuation ->
        onScrollCaptureEnd {
            continuation.resume(Unit)
        }
    }
}

fun Offset.roundToPoint(): Point = Point(x.roundToInt(), y.roundToInt())

/**
 * Creates a [CancellationSignal] and wires up cancellation bidirectionally to the coroutine's
 * job: cancelling either one will automatically cancel the other.
 */
private fun CancellableContinuation<*>.createCancellationSignal(): CancellationSignal {
    val signal = CancellationSignal()
    signal.setOnCancelListener(this::cancel)
    invokeOnCancellation { signal.cancel() }
    return signal
}
