/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.ui.uikit.utils.CMPMetalLayer
import androidx.compose.ui.uikit.utils.CMPDrawable
import androidx.compose.ui.util.trace
import androidx.compose.ui.viewinterop.UIKitInteropAction
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import kotlin.math.roundToInt
import kotlinx.cinterop.*
import org.jetbrains.skia.*
import platform.Foundation.NSLock
import platform.Foundation.NSRunLoop
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSThread
import platform.QuartzCore.*
import platform.darwin.*
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSTimeInterval
import platform.IOSurface.IOSurfaceGetHeight
import platform.IOSurface.IOSurfaceGetWidth
import platform.Metal.MTLCommandQueueProtocol
import platform.Metal.MTLDeviceProtocol
import platform.posix.QOS_CLASS_USER_INTERACTIVE

internal class DisplayLinkConditions(
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

// https://youtrack.jetbrains.com/issue/CMP-9722
// Copy of the class LegacyMetalRedrawer with a different layer.
// All changes made here must also be implemented in the `LegacyMetalRedrawer`.
internal class SurfaceMetalRedrawer(
    private val metalLayer: CMPMetalLayer,
    private var retrieveInteropTransaction: () -> UIKitInteropTransaction,
    private var render: (Canvas, targetTimestamp: NSTimeInterval) -> Unit,
): MetalRedrawer {
    private val device = metalLayer.device as? MTLDeviceProtocol
        ?: throw IllegalStateException("MetalRedrawer requires MTLDevice")
    private val queue = getCachedCommandQueue(device)
    private val context = DirectContext.makeMetal(device.objcPtr(), queue.objcPtr())
    private var lastRenderTimestamp: NSTimeInterval = CACurrentMediaTime()
    private val pictureRecorder = PictureRecorder()
    private val transactionQueue = InteropTransactionQueue()

    private val inflightCommandBuffersGroup = dispatch_group_create()
    // A guard flag to have proper assertion when draw() method is called recursively.
    private var isDrawRecursiveCall = false

    /**
     * Important! Thread safety instructions.
     *
     * Skiko Context and Surface are not thread-safe by default. All calls to these objects must
     * be synchronized. This is achieved by:
     * - The [renderAndPresentFrame] function never runs in parallel. To call it from the main
     * thread, we must wait until all tasks in the rendering dispatch queue are executed, which
     * guarantees that no new tasks are scheduled during this time, as they can only be scheduled
     * from the main thread. See [awaitRenderingQueueTasksCompletion].
     * - [disposeDrawableAssociatedResources] is used to clear skia surfaces, associated with
     * drawables. Call it in a thread-safe way.
     */
    private val renderingQueue =
        dispatch_queue_create(
            label = "RenderingDispatchQueue",
            attr = dispatch_queue_attr_make_with_qos_class(null, QOS_CLASS_USER_INTERACTIVE, 0)
        )

    var maximumFramesPerSecond: NSInteger = 0

    // https://youtrack.jetbrains.com/issue/CMP-9722
    // Left here for compatibility reasons. Does not make any effect and must be removed.
    override var isForcedToPresentWithTransactionEveryFrame: Boolean = false

    override var preferredFramesPerSecond: NSInteger
        get() = caDisplayLink?.preferredFramesPerSecond ?: 0
        set(value) {
            if (caDisplayLink?.preferredFramesPerSecond == value) return
            caDisplayLink?.preferredFramesPerSecond = value
        }

    override val currentTargetFrameDuration: NSTimeInterval?
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
    override var ongoingInteractionEventsCount: Int = 0
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
                // Rendering into an opaque CMPMetalLayer allows direct-to-screen optimization.
                updateLayerOpacity()
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
        target = SurfaceDisplayLinkProxy {
            val targetTimestamp = currentTargetTimestamp ?: return@SurfaceDisplayLinkProxy

            displayLinkConditions.onDisplayLinkTick {
                draw(waitUntilCompletion = false, targetTimestamp)
            }
        },
        selector = NSSelectorFromString(SurfaceDisplayLinkProxy::handleDisplayLinkTick.name)
    )

    private val currentTargetTimestamp: NSTimeInterval?
        get() = caDisplayLink?.targetTimestamp

    init {
        val caDisplayLink = caDisplayLink
            ?: throw IllegalStateException("caDisplayLink is null during redrawer init")

        caDisplayLink.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)

        updateLayerOpacity()
    }

    override var isActive: Boolean
        get() = displayLinkConditions.isActive
        set(isActive) {
            if (displayLinkConditions.isActive != isActive) {
                displayLinkConditions.isActive = isActive

                if (isActive) {
                    setNeedsRedraw()
                } else {
                    awaitRenderingQueueTasksCompletion()
                    disposeDrawableAssociatedResources(metalLayer.drawablesGeneration.toInt())
                    // If an application enters the background, synchronously wait for inflightCommandBuffersGroup, as per
                    // https://developer.apple.com/documentation/metal/gpu_devices_and_work_submission/preparing_your_metal_app_to_run_in_the_background?language=objc
                    // Set the expiration time to 1 second to ensure that the main thread does not get stuck when the app is suspended.
                    dispatch_group_wait(inflightCommandBuffersGroup, dispatch_time(DISPATCH_TIME_NOW, 1L * NSEC_PER_SEC.toLong()))
                }
            }
        }

    /**
     * Closes all Skia surfaces and render targets that are currently associated with drawables.
     */
    fun drainSkiaSurfaces() {
        check(NSThread.isMainThread) { "SurfaceMetalRedrawer.drainSkiaSurfaces() must be called on main thread" }
        awaitRenderingQueueTasksCompletion()
        metalLayer.drainDrawables()
        disposeDrawableAssociatedResources(metalLayer.drawablesGeneration.toInt())
    }

    override fun dispose() {
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

        awaitRenderingQueueTasksCompletion()
        disposeDrawableAssociatedResources(null)
        pictureRecorder.close()
        context.close()
    }

    /**
     * Marks current state as dirty and unpauses display link if needed and enables draw dispatch operation on
     * next vsync
     */
    override fun setNeedsRedraw() {
        displayLinkConditions.setNeedsRedraw()
    }

    /**
     * Immediately dispatch draw and block the thread until it's finished and presented on the screen.
     */
    override fun draw(waitUntilCompletion: Boolean) {
        if (caDisplayLink == null) {
            return
        }
        draw(waitUntilCompletion, CACurrentMediaTime())
    }

    private var currentFrameRate: Float = Float.NaN

    override fun voteFrameRate(frameRate: Float, frameRateCategory: Float) {
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

    private fun awaitRenderingQueueTasksCompletion() {
        check(NSThread.isMainThread) { "MetalRedrawer.awaitRenderingQueueTasksCompletion() must be called on main thread" }

        // Remove the currently scheduled frame, if any
        submitNextFrameForRenderLoop(null)

        dispatch_sync(renderingQueue) {}
    }

    /**
     * Encodes the frame and presents it on the screen.
     *
     * @param waitUntilCompletion if `true`, the method will block the thread until the frame is
     * presented on the screen. If false, the method will just dispatch GPU workload and return.
     * @param targetTimestamp the target timestamp for the frame to drive vsync-dependant time clock.
     */
    @OptIn(BetaInteropApi::class)
    private fun draw(waitUntilCompletion: Boolean, targetTimestamp: NSTimeInterval) =
        trace("MetalRedrawer:draw") {
            check(NSThread.isMainThread) { "MetalRedrawer.draw() must be called on main thread" }
            check(caDisplayLink != null) { "MetalRedrawer.draw() was called after dispose()" }
            check(!isDrawRecursiveCall) {
                "Attempt to call MetalRedrawer.draw() recursively which may lead to the PictureRecorder corruption."
            }

            isDrawRecursiveCall = true

            try {
                lastRenderTimestamp = maxOf(targetTimestamp, lastRenderTimestamp)

                val (width, height) = metalLayer.drawableSize.useContents {
                    width.roundToInt() to height.roundToInt()
                }

                if (width <= 0 || height <= 0) {
                    return@trace
                }

                // Perform timestep and record all draw commands into [Picture]
                val picture = trace("MetalRedrawer:draw:pictureRecording") {
                    pictureRecorder.beginRecording(
                        left = 0f,
                        top = 0f,
                        right = width.toFloat(),
                        bottom = height.toFloat()
                    ).also { canvas ->
                        render(canvas, lastRenderTimestamp)
                    }

                    pictureRecorder.finishRecordingAsPicture()
                }

                if (!currentFrameRate.isNaN()) {
                    preferredFramesPerSecond = currentFrameRate.toLong()
                    currentFrameRate = Float.NaN
                }

                val transaction = retrieveInteropTransaction()
                isInteropActive = transaction.isInteropActive
                val index = transactionQueue.scheduleTransaction(transaction)

                val frame = Frame(
                    picture = picture,
                    onDisplay = { transactionQueue.performScheduledTransactions(index) },
                    waitUntilCompletion = waitUntilCompletion
                )

                if (waitUntilCompletion) {
                    awaitRenderingQueueTasksCompletion()
                    renderAndPresentFrame(frame)
                } else {
                    if (submitNextFrameForRenderLoop(frame)) {
                        dispatch_async(renderingQueue) {
                            startRenderLoop()
                        }
                    }
                }
            } finally {
                isDrawRecursiveCall = false
            }
        }

    private class Frame(
        val picture: Picture,
        val waitUntilCompletion: Boolean,
        val onDisplay: () -> Unit,
    ) {
        fun dispose() {
            picture.close()
        }
    }

    private var nextFrameForRenderLoop: Frame? = null
    private var isRenderLoopActive: Boolean = false
    private val nextFrameLock = NSLock()

    private fun getNextFrameForRenderLoop(): Frame? {
        dispatch_assert_queue(renderingQueue)

        var frame: Frame? = null

        nextFrameLock.doLocked {
            frame = nextFrameForRenderLoop
            if (nextFrameForRenderLoop != null) {
                nextFrameForRenderLoop = null
                isRenderLoopActive = true
            } else {
                isRenderLoopActive = false
            }
        }

        return frame
    }

    private fun submitNextFrameForRenderLoop(frame: Frame?): Boolean {
        check(NSThread.isMainThread)

        var isDrawing = false

        nextFrameLock.doLocked {
            val hasScheduledFrame = nextFrameForRenderLoop != null
            nextFrameForRenderLoop?.dispose()
            nextFrameForRenderLoop = frame
            isDrawing = isRenderLoopActive || hasScheduledFrame
        }

        return !isDrawing
    }

    @OptIn(BetaInteropApi::class)
    private fun startRenderLoop() {
        dispatch_assert_queue(renderingQueue)

        var frame = getNextFrameForRenderLoop()
        while (frame != null) {
            autoreleasepool {
                renderAndPresentFrame(frame)
            }

            frame = getNextFrameForRenderLoop()
        }
    }

    private fun getSkiaSurface(drawable: CMPDrawable): Surface? {
        (drawable.associatedSkiaSurface as? Surface)
            ?.takeIf { !it.isClosed }
            ?.let { return it }

        val renderTarget = BackendRenderTarget.makeMetal(
            width = IOSurfaceGetWidth(drawable.surface).toInt(),
            height = IOSurfaceGetHeight(drawable.surface).toInt(),
            texturePtr = drawable.drawableTexture().rawValue,
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
            renderTarget.close()
            return null
        }

        drawable.associatedSkiaSurface = surface

        activeDrawableAssociatedResourcesDisposes.add {
            surface.close()
            renderTarget.close()
        }

        return surface
    }

    private var activeDrawablesGeneration: Int? = null
    private val activeDrawableAssociatedResourcesDisposes = mutableListOf<() -> Unit>()
    private fun disposeDrawableAssociatedResources(generation: Int?) {
        if (activeDrawablesGeneration != generation || generation == null) {
            activeDrawableAssociatedResourcesDisposes.forEach {
                it.invoke()
            }
            activeDrawableAssociatedResourcesDisposes.clear()
            activeDrawablesGeneration = generation
        }
    }

    private fun renderAndPresentFrame(frame: Frame) {
        disposeDrawableAssociatedResources(generation = metalLayer.drawablesGeneration.toInt())

        val drawable = trace("MetalRedrawer:draw:nextDrawable") {
            metalLayer.nextDrawable()
        }

        if (drawable == null) {
            frame.dispose()
            return
        }

        val surface = getSkiaSurface(drawable)

        if (surface == null) {
            frame.dispose()
            metalLayer.releaseDrawable(drawable)
            return
        }

        surface.canvas.drawPicture(frame.picture)
        frame.dispose()
        surface.flushAndSubmit()

        val commandBuffer = queue.commandBuffer()
        if (commandBuffer == null) {
            metalLayer.releaseDrawable(drawable)
            return
        }

        commandBuffer.label = "Present"

        metalLayer.prepareDrawableForPresent(drawable, commandBuffer)

        dispatch_group_enter(inflightCommandBuffersGroup)
        commandBuffer.addScheduledHandler {
            dispatch_group_leave(inflightCommandBuffersGroup)
        }
        if (!frame.waitUntilCompletion) {
            commandBuffer.addScheduledHandler {
                metalLayer.presentDrawable(drawable, onDisplay = frame.onDisplay)
            }
        }
        commandBuffer.commit()

        if (frame.waitUntilCompletion) {
            commandBuffer.waitUntilScheduled()
            metalLayer.presentDrawable(drawable, onDisplay = frame.onDisplay)
            commandBuffer.waitUntilCompleted()
        }
    }

    private companion object {
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
            check(NSThread.isMainThread) { "getCachedCommandQueue() must be called on main thread" }
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
            check(NSThread.isMainThread) { "releaseCachedCommandQueue() must be called on main thread" }
            val cached = cachedCommandQueue ?: return
            if (cached.queue == queue) {
                cached.refCount--
                if (cached.refCount == 0) {
                    cachedCommandQueue = null
                }
            }
        }
    }

    /**
     * A ring-buffer queue that preserves the order of [UIKitInteropTransaction.performTransaction]
     * calls relative to the draw order, even when GPU frames are presented out of order or dropped.
     *
     * When a frame is presented, [performScheduledTransactions] executes all transactions scheduled
     * up to and including that frame's index, in the original schedule order.
     *
     * If a newer frame is presented before an older one, its completion drains all pending
     * transactions including the older one, so each transaction is executed exactly once.
     */
    internal class InteropTransactionQueue {
        private val bufferLength = 16
        private var firstScheduledIndex: Long = 0
        private var lastScheduledIndex: Long = 0
        private val scheduledInteropTransactions = Array<UIKitInteropTransaction?>(bufferLength) { null }

        fun scheduleTransaction(transaction: UIKitInteropTransaction): Long {
            if (transaction.actions.isEmpty()) {
                return lastScheduledIndex - 1
            }
            val index = lastScheduledIndex
            lastScheduledIndex++
            if (index - firstScheduledIndex >= bufferLength) {
                // Overflow detected. Perform old transactions anyway
                performScheduledTransactions(firstScheduledIndex)
            }
            scheduledInteropTransactions[(index % bufferLength).toInt()] = transaction
            return index
        }

        fun performScheduledTransactions(index: Long) {
            while (firstScheduledIndex <= index && firstScheduledIndex < lastScheduledIndex) {
                val arrayIndex = (firstScheduledIndex % bufferLength).toInt()
                firstScheduledIndex++
                scheduledInteropTransactions[arrayIndex]?.performTransaction()
                scheduledInteropTransactions[arrayIndex] = null
            }
        }
    }
}

private class SurfaceDisplayLinkProxy(
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
