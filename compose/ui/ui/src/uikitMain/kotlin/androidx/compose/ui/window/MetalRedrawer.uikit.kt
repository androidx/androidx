/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.uikit.utils.CMPMetalDrawablesHandler
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.trace
import androidx.compose.ui.viewinterop.UIKitInteropAction
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import kotlin.math.roundToInt
import kotlinx.cinterop.*
import org.jetbrains.skia.*
import platform.Foundation.NSRunLoop
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSThread
import platform.Metal.MTLCommandBufferProtocol
import platform.QuartzCore.*
import platform.darwin.*
import org.jetbrains.skia.Rect
import platform.Foundation.NSLock
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSTimeInterval
import platform.Metal.MTLCommandQueueProtocol
import platform.Metal.MTLDeviceProtocol

private class DisplayLinkConditions(
    val setPausedCallback: (Boolean) -> Unit
) {
    /**
     * see [MetalRedrawer.ongoingInteractionEventsCount]
     */
    var needsToBeProactive: Boolean = false
        set(value) {
            field = value

            update()
        }

    /**
     * Indicates that application is running foreground now
     */
    var isActive: Boolean = true
        set(value) {
            field = value

            update()
        }

    /**
     * Number of subsequent vsync that will issue a draw
     */
    private var scheduledRedrawsCount = 0
        set(value) {
            field = value

            update()
        }

    /**
     * Handle display link callback by updating internal state and dispatching the draw, if needed.
     */
    inline fun onDisplayLinkTick(draw: () -> Unit) {
        if (scheduledRedrawsCount > 0) {
            scheduledRedrawsCount -= 1
            draw()
        }
    }

    /**
     * Mark next [FRAMES_COUNT_TO_SCHEDULE_ON_NEED_REDRAW] frames to issue a draw dispatch and unpause displayLink if needed.
     */
    fun setNeedsRedraw() {
        scheduledRedrawsCount = FRAMES_COUNT_TO_SCHEDULE_ON_NEED_REDRAW
    }

    private fun update() {
        val isUnpaused = isActive && (needsToBeProactive || scheduledRedrawsCount > 0)
        setPausedCallback(!isUnpaused)
    }

    companion object {
        /**
         * Right now `needRedraw` doesn't reentry from within `draw` callback during animation which leads to a situation where CADisplayLink is first paused
         * and then asynchronously unpaused. This effectively makes Pro Motion display lose a frame before running on highest possible frequency again.
         * To avoid this, we need to render at least two frames (instead of just one) after each `needRedraw` assuming that invalidation comes inbetween them and
         * displayLink is not paused by the end of RuntimeLoop tick.
         */
        const val FRAMES_COUNT_TO_SCHEDULE_ON_NEED_REDRAW = 2
    }
}

internal class InflightCommandBuffers(
    private val maxInflightCount: Int
) {
    private val lock = NSLock()
    private val list = mutableListOf<MTLCommandBufferProtocol>()

    fun waitUntilAllAreScheduled() = lock.doLocked {
        list.fastForEach {
            it.waitUntilScheduled()
        }
    }

    fun add(commandBuffer: MTLCommandBufferProtocol) = lock.doLocked {
        if (list.size == maxInflightCount) {
            list.removeAt(0)
        }

        list.add(commandBuffer)
    }
}

internal class MetalRedrawer(
    private val metalLayer: CAMetalLayer,
    private var retrieveInteropTransaction: () -> UIKitInteropTransaction,
    private val useSeparateRenderThreadWhenPossible: Boolean,
    private var render: (Canvas, targetTimestamp: NSTimeInterval) -> Unit,
) {
    /**
     * A wrapper around CAMetalLayer that allows to perform operations on its drawables without
     * exposing the objects to Kotlin/Native runtime and thus allowing explicit lifetime control of them.
     *
     * See ObjC implementation of [CMPMetalDrawablesHandler] for more details.
     */
    private val metalDrawablesHandler = CMPMetalDrawablesHandler(metalLayer)
    // Workaround for KN compiler bug
    // Type mismatch: inferred type is objcnames.protocols.MTLDeviceProtocol but platform.Metal.MTLDeviceProtocol was expected
    @Suppress("USELESS_CAST")
    private val device = metalLayer.device as MTLDeviceProtocol?
        ?: throw IllegalStateException("CAMetalLayer.device can not be null")
    private val queue = getCachedCommandQueue(device)
    private val context = DirectContext.makeMetal(device.objcPtr(), queue.objcPtr())
    private var lastRenderTimestamp: NSTimeInterval = CACurrentMediaTime()
    private val pictureRecorder = PictureRecorder()

    // Semaphore for preventing command buffers count more than swapchain size to be scheduled/executed at the same time
    private val inflightSemaphore =
        dispatch_semaphore_create(metalLayer.maximumDrawableCount.toLong())
    private val drawCanvasSemaphore = dispatch_semaphore_create(1)
    private val inflightCommandBuffers =
        InflightCommandBuffers(metalLayer.maximumDrawableCount.toInt())

    var isForcedToPresentWithTransactionEveryFrame = false

    var maximumFramesPerSecond: NSInteger = 0

    var preferredFramesPerSecond: NSInteger
        get() = caDisplayLink?.preferredFramesPerSecond ?: 0
        set(value) {
            if (caDisplayLink?.preferredFramesPerSecond == value) return
            caDisplayLink?.preferredFramesPerSecond = value
        }

    val currentTargetFrameDuration: NSTimeInterval?
        get() {
            val currentTargetTimestamp = currentTargetTimestamp ?: return null
            val currentTimestamp = caDisplayLink?.timestamp ?: return null
            return currentTargetTimestamp - currentTimestamp
        }

    private val displayLinkConditions = DisplayLinkConditions { paused ->
        caDisplayLink?.paused = paused
    }

    /**
     * Runs invalidation-independent displayLink for forcing UITouch events to come at the fastest
     * possible cadence. Otherwise, touch events can come at rate lower than actual display refresh
     * rate.
     */
    var ongoingInteractionEventsCount: Int = 0
        set(value) {
            field = value
            displayLinkConditions.needsToBeProactive = value > 0
        }

    /**
     * True if Metal layer can be opaque. In this case if no interop views are present, Metal
     * rendering will be optimized for direct-to-screen rendering.
     *
     * In some scenarios like using this layer as a canvas for dialog and popup layers, it's never the
     * case.
     */
    var canBeOpaque: Boolean = true
        set(value) {
            field = value

            updateLayerOpacity()
        }

    /**
     * `true` if Metal rendering is synchronized with changes of UIKit interop views, `false` otherwise
     */
    private var isInteropActive = false
        set(value) {
            if (field != value) {
                field = value
                // If active, make metalLayer transparent, opaque otherwise.
                // Rendering into opaque CAMetalLayer allows direct-to-screen optimization.
                updateLayerOpacity()
                metalLayer.drawsAsynchronously = !value
            }
        }

    private fun updateLayerOpacity() {
        metalLayer.setOpaque(!isInteropActive && canBeOpaque)
    }

    /**
     * Display link for driving the rendering loop.
     * null after [dispose] call
     */
    private var caDisplayLink: CADisplayLink? = CADisplayLink.displayLinkWithTarget(
        target = DisplayLinkProxy {
            val targetTimestamp = currentTargetTimestamp ?: return@DisplayLinkProxy

            displayLinkConditions.onDisplayLinkTick {
                draw(waitUntilCompletion = false, targetTimestamp)
            }
        },
        selector = NSSelectorFromString(DisplayLinkProxy::handleDisplayLinkTick.name)
    )

    private val currentTargetTimestamp: NSTimeInterval?
        get() = caDisplayLink?.targetTimestamp

    init {
        val caDisplayLink = caDisplayLink
            ?: throw IllegalStateException("caDisplayLink is null during redrawer init")

        caDisplayLink.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)

        updateLayerOpacity()
    }

    var isActive: Boolean = true
        set(newValue) {
            if (field == newValue) {
                field = newValue
                setNeedsRedraw()

                displayLinkConditions.isActive = newValue
                if (!newValue) {
                    inflightCommandBuffers.waitUntilAllAreScheduled()
                }
            }
        }

    fun dispose() {
        check(caDisplayLink != null) { "MetalRedrawer.dispose() was called more than once" }

        retrieveInteropTransaction = {
            object : UIKitInteropTransaction {
                override val isInteropActive: Boolean = false
                override val actions = emptyList<UIKitInteropAction>()
            }
        }

        render = { _, _ -> }

        releaseCachedCommandQueue(queue)

        caDisplayLink?.invalidate()
        caDisplayLink = null

        pictureRecorder.close()
        context.close()
    }

    /**
     * Marks current state as dirty and unpauses display link if needed and enables draw dispatch operation on
     * next vsync
     */
    fun setNeedsRedraw() {
        displayLinkConditions.setNeedsRedraw()
    }

    /**
     * Immediately dispatch draw and block the thread until it's finished and presented on the screen.
     */
    fun draw(waitUntilCompletion: Boolean) {
        if (caDisplayLink == null) {
            return
        }

        draw(waitUntilCompletion, CACurrentMediaTime())
    }

    private var currentFrameRate: Float = Float.NaN

    fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
        val frameRateCategoryValue = when (frameRateCategory) {
            FrameRateCategory.Default.value -> CAFrameRateRangeDefault.preferred
            FrameRateCategory.Normal.value -> 60f
            FrameRateCategory.High.value -> maximumFramesPerSecond.toFloat()
            else -> Float.NaN
        }

        val resolvedFrameRate = when {
            !frameRate.isNaN() && !frameRateCategoryValue.isNaN() -> maxOf(frameRate, frameRateCategoryValue)
            !frameRate.isNaN() -> frameRate
            !frameRateCategoryValue.isNaN() -> frameRateCategoryValue
            else -> return
        }

        if (currentFrameRate.isNaN() || resolvedFrameRate > currentFrameRate) {
            currentFrameRate = resolvedFrameRate
        }
    }

    /**
     * Encodes the frame and presents it on the screen.
     *
     * @param waitUntilCompletion if `true`, the method will block the thread until the frame is
     * presented on the screen. If false, the method will just dispatch GPU workload and return.
     * @param targetTimestamp the target timestamp for the frame to drive vsync-dependant time clock.
     */
    @OptIn(BetaInteropApi::class)
    private fun draw(waitUntilCompletion: Boolean, targetTimestamp: NSTimeInterval) = trace("MetalRedrawer:draw") {
        check(NSThread.isMainThread)

        lastRenderTimestamp = maxOf(targetTimestamp, lastRenderTimestamp)

        autoreleasepool {
            val (width, height) = metalLayer.drawableSize.useContents {
                width.roundToInt() to height.roundToInt()
            }

            if (width <= 0 || height <= 0) {
                return@autoreleasepool
            }

            // Perform timestep and record all draw commands into [Picture]
            val picture = trace("MetalRedrawer:draw:pictureRecording") {
                pictureRecorder.beginRecording(
                    Rect(
                        left = 0f,
                        top = 0f,
                        width.toFloat(),
                        height.toFloat()
                    )
                ).also { canvas ->
                    render(canvas, lastRenderTimestamp)
                }

                pictureRecorder.finishRecordingAsPicture()
            }

            if (!currentFrameRate.isNaN()) {
                preferredFramesPerSecond = currentFrameRate.toLong()
                currentFrameRate = Float.NaN
            }

            trace("MetalRedrawer:draw:waitInflightSemaphore") {
                dispatch_semaphore_wait(inflightSemaphore, DISPATCH_TIME_FOREVER)
            }

            val metalDrawable = trace("MetalRedrawer:draw:nextDrawable") {
                metalDrawablesHandler.nextDrawable()
            }

            if (metalDrawable == null) {
                // TODO: anomaly, log
                // Logger.warn { "'metalLayer.nextDrawable()' returned null. 'metalLayer.allowsNextDrawableTimeout' should be set to false. Skipping the frame." }
                picture.close()
                dispatch_semaphore_signal(inflightSemaphore)
                return@autoreleasepool
            }

            val renderTarget = BackendRenderTarget.makeMetal(
                width,
                height,
                texturePtr = metalDrawablesHandler.drawableTexture(metalDrawable).rawValue
            )

            val surface = Surface.makeFromBackendRenderTarget(
                context,
                renderTarget,
                SurfaceOrigin.TOP_LEFT,
                SurfaceColorFormat.BGRA_8888,
                ColorSpace.sRGB,
                SurfaceProps(pixelGeometry = PixelGeometry.UNKNOWN)
            )

            if (surface == null) {
                // TODO: anomaly, log
                // Logger.warn { "'Surface.makeFromBackendRenderTarget' returned null. Skipping the frame." }
                picture.close()
                renderTarget.close()
                metalDrawablesHandler.releaseDrawable(metalDrawable)
                dispatch_semaphore_signal(inflightSemaphore)
                return@autoreleasepool
            }

            val interopTransaction = retrieveInteropTransaction()

            val presentsWithTransaction =
                isForcedToPresentWithTransactionEveryFrame
                    || interopTransaction.actions.isNotEmpty()
                    || isInteropActive != interopTransaction.isInteropActive
            metalLayer.presentsWithTransaction = presentsWithTransaction

            if (interopTransaction.isInteropActive) {
                isInteropActive = true
            }

            val mustEncodeAndPresentOnMainThread = presentsWithTransaction || waitUntilCompletion || !useSeparateRenderThreadWhenPossible

            val encodeAndPresentBlock = {
                trace("MetalRedrawer:draw:encodeAndPresent") {
                    if (useSeparateRenderThreadWhenPossible) {
                        dispatch_semaphore_wait(drawCanvasSemaphore, DISPATCH_TIME_FOREVER)
                    }

                    surface.canvas.drawPicture(picture)
                    picture.close()
                    surface.flushAndSubmit()

                    if (useSeparateRenderThreadWhenPossible) {
                        dispatch_semaphore_signal(drawCanvasSemaphore)
                    }

                    val commandBuffer = queue.commandBuffer()!!
                    commandBuffer.label = "Present"

                    if (!presentsWithTransaction) {
                        // scheduleDrawablePresentation consumes metalDrawable
                        // don't use metalDrawable after this call
                        metalDrawablesHandler.scheduleDrawablePresentation(metalDrawable, commandBuffer)
                    }

                    commandBuffer.addCompletedHandler {
                        // Signal work finish, allow a new command buffer to be scheduled
                        dispatch_semaphore_signal(inflightSemaphore)
                    }
                    commandBuffer.commit()

                    if (presentsWithTransaction) {
                        // If there are pending changes in UIKit interop, [waitUntilScheduled](https://developer.apple.com/documentation/metal/mtlcommandbuffer/1443036-waituntilscheduled) is called
                        // to ensure that transaction is available
                        trace("MetalRedrawer:draw:waitTransaction") {
                            commandBuffer.waitUntilScheduled()
                        }

                        // presentDrawable consumes metalDrawable
                        // don't use metalDrawable after this call
                        metalDrawablesHandler.presentDrawable(metalDrawable)

                        interopTransaction.actions.fastForEach {
                            it.invoke()
                        }

                        if (interopTransaction.isInteropActive.not()) {
                            isInteropActive = false
                        }
                    }

                    surface.close()
                    renderTarget.close()

                    // Track current inflight command buffers to synchronously wait for their schedule in case app goes background
                    inflightCommandBuffers.add(commandBuffer)

                    if (waitUntilCompletion) {
                        trace("MetalRedrawer:draw:waitUntilCompleted") {
                            commandBuffer.waitUntilCompleted()
                        }
                    }
                }
            }

            if (mustEncodeAndPresentOnMainThread) {
                encodeAndPresentBlock()
            } else {
                dispatch_async(renderingDispatchQueue) {
                    encodeAndPresentBlock()
                }
            }
        }
    }

    companion object {
        private val renderingDispatchQueue =
            dispatch_queue_create("RenderingDispatchQueue", null)

        private class CachedCommandQueue(
            val queue: MTLCommandQueueProtocol,
            var refCount: Int = 1
        )

        /**
         * Cached command queue record. Assumed to be associated with default MTLDevice.
         */
        private var cachedCommandQueue: CachedCommandQueue? = null

        /**
         * Get an existing command queue associated with the device or create a new one and cache it.
         * Assumed to be run on the main thread.
         */
        private fun getCachedCommandQueue(device: MTLDeviceProtocol): MTLCommandQueueProtocol {
            val cached = cachedCommandQueue
            if (cached != null) {
                cached.refCount++
                return cached.queue
            } else {
                val queue = device.newCommandQueue() ?: throw IllegalStateException("MTLDevice.newCommandQueue() returned null")
                cachedCommandQueue = CachedCommandQueue(queue)
                return queue
            }
        }

        /**
         * Release the cached command queue. Release the cache if refCount reaches 0.
         * Assumed to be run on the main thread.
         */
        private fun releaseCachedCommandQueue(queue: MTLCommandQueueProtocol) {
            val cached = cachedCommandQueue ?: return
            if (cached.queue == queue) {
                cached.refCount--
                if (cached.refCount == 0) {
                    cachedCommandQueue = null
                }
            }
        }
    }
}

private class DisplayLinkProxy(
    private val callback: () -> Unit
) : NSObject() {
    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun handleDisplayLinkTick() {
        callback()
    }
}

private inline fun <T> NSLock.doLocked(block: () -> T): T {
    lock()

    try {
        return block()
    } finally {
        unlock()
    }
}
