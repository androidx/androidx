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

import androidx.compose.ui.uikit.utils.CMPMetalDrawablesHandler
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.trace
import androidx.compose.ui.viewinterop.UIKitInteropState
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import androidx.compose.ui.viewinterop.isNotEmpty
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
     * see [MetalRedrawer.needsProactiveDisplayLink]
     */
    var needsToBeProactive: Boolean = false
        set(value) {
            field = value

            update()
        }

    /**
     * Indicates that application is running foreground now
     */
    var isApplicationActive: Boolean = false
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
    fun needRedraw() {
        scheduledRedrawsCount = FRAMES_COUNT_TO_SCHEDULE_ON_NEED_REDRAW
    }

    private fun update() {
        val isUnpaused = isApplicationActive && (needsToBeProactive || scheduledRedrawsCount > 0)
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

internal interface MetalRedrawerCallbacks {
    /**
     * Perform time step and encode draw operations into canvas.
     *
     * @param canvas Canvas to encode draw operations into.
     * @param targetTimestamp Timestamp indicating the expected draw result presentation time. Implementation should forward its internal time clock to this targetTimestamp to achieve smooth visual change cadence.
     */
    fun render(canvas: Canvas, targetTimestamp: NSTimeInterval)

    /**
     * Create an interop transaction by flushing the list of all pending actions
     * that need to be synchronized with Metal rendering using CATransaction mechanism.
     */
    fun retrieveInteropTransaction(): UIKitInteropTransaction
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
    private val callbacks: MetalRedrawerCallbacks
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
    private val device = metalLayer.device as platform.Metal.MTLDeviceProtocol?
        ?: throw IllegalStateException("CAMetalLayer.device can not be null")
    private val queue = getCachedCommandQueue(device)
    private val context = DirectContext.makeMetal(device.objcPtr(), queue.objcPtr())
    private var lastRenderTimestamp: NSTimeInterval = CACurrentMediaTime()
    private val pictureRecorder = PictureRecorder()

    // Semaphore for preventing command buffers count more than swapchain size to be scheduled/executed at the same time
    private val inflightSemaphore =
        dispatch_semaphore_create(metalLayer.maximumDrawableCount.toLong())
    private val inflightCommandBuffers =
        InflightCommandBuffers(metalLayer.maximumDrawableCount.toInt())

    var isForcedToPresentWithTransactionEveryFrame = false

    var maximumFramesPerSecond: NSInteger
        get() = caDisplayLink?.preferredFramesPerSecond ?: 0
        set(value) {
            caDisplayLink?.preferredFramesPerSecond = value
        }

    /**
     * Set to `true` if need always running invalidation-independent displayLink for forcing UITouch
     * events to come at the fastest possible cadence.
     * Otherwise, touch events can come at rate lower than actual display refresh rate.
     */
    var needsProactiveDisplayLink: Boolean
        get() = displayLinkConditions.needsToBeProactive
        set(value) {
            displayLinkConditions.needsToBeProactive = value
        }

    var opaque: Boolean = true
        set(value) {
            field = value

            updateLayerOpacity()
        }

    /**
     * `true` if Metal rendering is synchronized with changes of UIKit interop views, `false` otherwise
     */
    private var isInteropActive = false
        set(value) {
            field = value

            // If active, make metalLayer transparent, opaque otherwise.
            // Rendering into opaque CAMetalLayer allows direct-to-screen optimization.
            updateLayerOpacity()
            metalLayer.drawsAsynchronously = !value
        }

    private fun updateLayerOpacity() {
        metalLayer.setOpaque(!isInteropActive && opaque)
    }

    /**
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

    private val displayLinkConditions = DisplayLinkConditions { paused ->
        caDisplayLink?.paused = paused
    }

    private val applicationForegroundStateListener = ApplicationForegroundStateListener { isApplicationActive ->
        displayLinkConditions.isApplicationActive = isApplicationActive

        if (!isApplicationActive) {
            // If application goes background, synchronously schedule all inflightCommandBuffers, as per
            // https://developer.apple.com/documentation/metal/gpu_devices_and_work_submission/preparing_your_metal_app_to_run_in_the_background?language=objc
            inflightCommandBuffers.waitUntilAllAreScheduled()
        }
    }

    init {
        val caDisplayLink = caDisplayLink
            ?: throw IllegalStateException("caDisplayLink is null during redrawer init")

        // UIApplication can be in UIApplicationStateInactive state (during app launch before it gives control back to run loop)
        // and won't receive UIApplicationWillEnterForegroundNotification
        // so we compare the state with UIApplicationStateBackground instead of UIApplicationStateActive
        displayLinkConditions.isApplicationActive =
            ApplicationForegroundStateListener.isApplicationForeground

        caDisplayLink.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)

        updateLayerOpacity()
    }

    fun dispose() {
        check(caDisplayLink != null) { "MetalRedrawer.dispose() was called more than once" }

        releaseCachedCommandQueue(queue)

        applicationForegroundStateListener.dispose()

        caDisplayLink?.invalidate()
        caDisplayLink = null

        pictureRecorder.close()
        context.close()
    }

    /**
     * Marks current state as dirty and unpauses display link if needed and enables draw dispatch operation on
     * next vsync
     */
    fun needRedraw() = displayLinkConditions.needRedraw()

    /**
     * Immediately dispatch draw and block the thread until it's finished and presented on the screen.
     */
    fun drawSynchronously() {
        if (caDisplayLink == null) {
            return
        }

        draw(waitUntilCompletion = true, CACurrentMediaTime())
    }

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
                    canvas.clear(if (metalLayer.opaque) Color.WHITE else Color.TRANSPARENT)
                    callbacks.render(canvas, lastRenderTimestamp)
                }

                pictureRecorder.finishRecordingAsPicture()
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

            val renderTarget =
                BackendRenderTarget.makeMetal(width, height, metalDrawablesHandler.drawableTexture(metalDrawable).rawValue)

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

            val interopTransaction = callbacks.retrieveInteropTransaction()
            if (interopTransaction.state == UIKitInteropState.BEGAN) {
                isInteropActive = true
            }
            val presentsWithTransaction =
                isForcedToPresentWithTransactionEveryFrame || interopTransaction.isNotEmpty()
            metalLayer.presentsWithTransaction = presentsWithTransaction

            // TODO: encoding on separate thread requires investigation for reported crashes
            //  https://github.com/JetBrains/compose-multiplatform/issues/3862
            //  https://youtrack.jetbrains.com/issue/COMPOSE-608/iOS-reproduce-and-investigate-parallel-rendering-encoding-crash
            // val mustEncodeAndPresentOnMainThread = presentsWithTransaction || waitUntilCompletion
            val mustEncodeAndPresentOnMainThread = true

            val encodeAndPresentBlock = {
                trace("MetalRedrawer:draw:encodeAndPresent") {
                    surface.canvas.drawPicture(picture)
                    picture.close()
                    surface.flushAndSubmit()

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

                        if (interopTransaction.state == UIKitInteropState.ENDED) {
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
                    autoreleasepool {
                        encodeAndPresentBlock()
                    }
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
