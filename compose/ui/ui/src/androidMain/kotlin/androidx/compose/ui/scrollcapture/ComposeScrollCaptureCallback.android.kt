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

import android.graphics.BlendMode
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect as AndroidRect
import android.os.CancellationSignal
import android.util.Log
import android.view.ScrollCaptureCallback
import android.view.ScrollCaptureSession
import androidx.annotation.RequiresApi
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeIntRect
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.semantics.SemanticsActions.ScrollByOffset
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange
import androidx.compose.ui.unit.IntRect
import java.util.function.Consumer
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

private const val DEBUG = false
private const val TAG = "ScrollCapture"

/**
 * Implementation of [ScrollCaptureCallback] that captures Compose scroll containers.
 *
 * This callback interacts with the scroll container via semantics, namely [ScrollByOffset], and
 * supports any container that publishes that action – whether the size of the scroll contents are
 * known or not (e.g. `LazyColumn`). Pixels are captured by drawing the node directly after each
 * scroll operation.
 */
@RequiresApi(31)
internal class ComposeScrollCaptureCallback(
    private val node: SemanticsNode,
    private val viewportBoundsInWindow: IntRect,
    coroutineScope: CoroutineScope,
    private val listener: ScrollCaptureSessionListener,
) : ScrollCaptureCallback {
    // Don't animate scrollByOffset calls.
    private val coroutineScope = coroutineScope + DisableAnimationMotionDurationScale

    private val scrollTracker =
        RelativeScroller(
            viewportSize = viewportBoundsInWindow.height,
            scrollBy = { delta ->
                val scrollByOffset = checkPreconditionNotNull(node.scrollCaptureScrollByAction)
                val reverseScrolling = node.unmergedConfig[VerticalScrollAxisRange].reverseScrolling

                val actualDelta = if (reverseScrolling) -delta else delta
                if (DEBUG)
                    Log.d(
                        TAG,
                        "scrolling by delta $actualDelta " +
                            "(reverseScrolling=$reverseScrolling, requested delta=$delta)"
                    )

                // This action may animate, ensure any calls to this RelativeScroll are done with a
                // coroutine context that disables animations.
                val consumed = scrollByOffset(Offset(0f, actualDelta))
                if (reverseScrolling) -consumed.y else consumed.y
            }
        )

    /** Only used when [DEBUG] is true. */
    private var requestCount = 0

    override fun onScrollCaptureSearch(signal: CancellationSignal, onReady: Consumer<AndroidRect>) {
        val bounds = viewportBoundsInWindow
        onReady.accept(bounds.toAndroidRect())
    }

    override fun onScrollCaptureStart(
        session: ScrollCaptureSession,
        signal: CancellationSignal,
        onReady: Runnable
    ) {
        scrollTracker.reset()
        requestCount = 0
        listener.onSessionStarted()
        onReady.run()
    }

    override fun onScrollCaptureImageRequest(
        session: ScrollCaptureSession,
        signal: CancellationSignal,
        captureArea: AndroidRect,
        onComplete: Consumer<AndroidRect>
    ) {
        coroutineScope.launchWithCancellationSignal(signal) {
            val result = onScrollCaptureImageRequest(session, captureArea.toComposeIntRect())
            onComplete.accept(result.toAndroidRect())
        }
    }

    private suspend fun onScrollCaptureImageRequest(
        session: ScrollCaptureSession,
        captureArea: IntRect,
    ): IntRect {
        // Scroll the requested capture area into the viewport so we can draw it.
        val targetMin = captureArea.top
        val targetMax = captureArea.bottom
        if (DEBUG) Log.d(TAG, "capture request for $targetMin..$targetMax")
        scrollTracker.scrollRangeIntoView(targetMin, targetMax)

        // Wait a frame to allow layout to respond to the scroll.
        withFrameNanos {}

        // Calculate the viewport-relative coordinates of the capture area, clipped to
        // the viewport.
        val viewportClippedMin = scrollTracker.mapOffsetToViewport(targetMin)
        val viewportClippedMax = scrollTracker.mapOffsetToViewport(targetMax)
        if (DEBUG) Log.d(TAG, "drawing viewport $viewportClippedMin..$viewportClippedMax")
        val viewportClippedRect =
            captureArea.copy(top = viewportClippedMin, bottom = viewportClippedMax)

        if (viewportClippedMin == viewportClippedMax) {
            // Requested capture area is outside the bounds of scrollable content,
            // nothing to capture.
            return IntRect.Zero
        }

        // Draw a single frame of the content to a buffer that we can stamp out.
        val coordinator =
            checkNotNull(node.findCoordinatorToGetBounds()) {
                "Could not find coordinator for semantics node."
            }

        val androidCanvas = session.surface.lockHardwareCanvas()
        try {
            // Clear any pixels left over from a previous request.
            androidCanvas.drawColor(Color.TRANSPARENT, BlendMode.CLEAR)

            if (DEBUG) {
                androidCanvas.drawDebugBackground()
            }

            val canvas = Canvas(androidCanvas)
            canvas.translate(
                dx = -viewportClippedRect.left.toFloat(),
                dy = -viewportClippedRect.top.toFloat()
            )
            coordinator.draw(canvas, graphicsLayer = null)

            if (DEBUG) {
                canvas.translate(
                    dx = viewportClippedRect.left.toFloat(),
                    dy = viewportClippedRect.top.toFloat(),
                )
                androidCanvas.drawDebugOverlay()
            }
        } finally {
            session.surface.unlockCanvasAndPost(androidCanvas)
        }

        // Translate back to "original" coordinates to report.
        val resultRect = viewportClippedRect.translate(0, scrollTracker.scrollAmount.roundToInt())
        if (DEBUG) Log.d(TAG, "captured rectangle $resultRect")
        return resultRect
    }

    override fun onScrollCaptureEnd(onReady: Runnable) {
        coroutineScope.launch(NonCancellable) {
            scrollTracker.scrollTo(0f)
            listener.onSessionEnded()
            onReady.run()
        }
    }

    private fun AndroidCanvas.drawDebugBackground() {
        drawColor(
            androidx.compose.ui.graphics.Color.hsl(
                    hue = Random.nextFloat() * 360f,
                    saturation = 0.75f,
                    lightness = 0.5f,
                    alpha = 1f
                )
                .toArgb()
        )
    }

    private fun AndroidCanvas.drawDebugOverlay() {
        val circleRadius = 20f
        val circlePaint =
            Paint().apply {
                color = Color.RED
                textSize = 48f
            }
        drawCircle(0f, 0f, circleRadius, circlePaint)
        drawCircle(width.toFloat(), 0f, circleRadius, circlePaint)
        drawCircle(width.toFloat(), height.toFloat(), circleRadius, circlePaint)
        drawCircle(0f, height.toFloat(), circleRadius, circlePaint)

        drawText(requestCount.toString(), width / 2f, height / 2f, circlePaint)
        requestCount++
    }

    interface ScrollCaptureSessionListener {
        fun onSessionStarted()

        fun onSessionEnded()
    }
}

private fun CoroutineScope.launchWithCancellationSignal(
    signal: CancellationSignal,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val job = launch(block = block)
    job.invokeOnCompletion { cause ->
        if (cause != null) {
            signal.cancel()
        }
    }
    signal.setOnCancelListener { job.cancel() }
    return job
}

/**
 * Helper class for scrolling to specific offsets relative to an original scroll position and
 * mapping those offsets to the current viewport coordinates.
 */
private class RelativeScroller(
    private val viewportSize: Int,
    private val scrollBy: suspend (Float) -> Float
) {
    var scrollAmount = 0f
        private set

    fun reset() {
        scrollAmount = 0f
    }

    /**
     * Scrolls so that the range ([min], [max]) is in the viewport. The range must fit inside the
     * viewport.
     */
    suspend fun scrollRangeIntoView(min: Int, max: Int) {
        if (DEBUG) Log.d(TAG, "scrollRangeIntoView(min=$min, max=$max)")
        require(min <= max) { "Expected min=$min ≤ max=$max" }
        require(max - min <= viewportSize) {
            "Expected range (${max - min}) to be ≤ viewportSize=$viewportSize"
        }

        if (min >= scrollAmount && max <= scrollAmount + viewportSize) {
            // Already visible, no need to scroll.
            if (DEBUG) Log.d(TAG, "requested range already in view, not scrolling")
            return
        }

        // Scroll to the nearest edge.
        val target = if (min < scrollAmount) min else max - viewportSize
        if (DEBUG) Log.d(TAG, "scrolling to $target")
        scrollTo(target.toFloat())
    }

    /**
     * Given [offset] relative to the original scroll position, maps it to the current offset in the
     * viewport. Values are clamped to the viewport.
     *
     * This is an identity map for values inside the viewport before any scrolling has been done
     * after calling `scrollTo(0f)`.
     */
    fun mapOffsetToViewport(offset: Int): Int {
        return (offset - scrollAmount.roundToInt()).coerceIn(0, viewportSize)
    }

    /** Try to scroll to [offset] pixels past the original scroll position. */
    suspend fun scrollTo(offset: Float) {
        scrollBy(offset - scrollAmount)
    }

    private suspend fun scrollBy(delta: Float) {
        val consumed = scrollBy.invoke(delta)
        scrollAmount += consumed
        if (DEBUG)
            Log.d(TAG, "scrolled $consumed of requested $delta, after scrollAmount=$scrollAmount")
    }
}

private object DisableAnimationMotionDurationScale : MotionDurationScale {
    override val scaleFactor: Float
        get() = 0f
}
