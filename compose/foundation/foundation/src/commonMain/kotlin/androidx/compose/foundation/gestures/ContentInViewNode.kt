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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Static field to turn on a bunch of verbose logging to debug animations. Since this is a constant,
 * any log statements guarded by this value should be removed by the compiler when it's false.
 */
private const val DEBUG = false
private const val TAG = "ContentInViewModifier"

/** A minimum amount of delta that it is considered a valid scroll. */
private const val MinScrollThreshold = 0.5f

/**
 * A [Modifier] to be placed on a scrollable container (i.e. [Modifier.scrollable]) that animates
 * the [ScrollableState] to handle [BringIntoViewRequester] requests and keep the currently-focused
 * child in view when the viewport shrinks.
 */
// TODO(b/242732126) Make this logic reusable for TV's mario scrolling implementation.
@OptIn(ExperimentalFoundationApi::class)
internal class ContentInViewNode(
    private var orientation: Orientation,
    private val scrollingLogic: ScrollingLogic,
    private var reverseDirection: Boolean,
    private var bringIntoViewSpec: BringIntoViewSpec?
) :
    Modifier.Node(),
    BringIntoViewResponder,
    LayoutAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    override val shouldAutoInvalidate: Boolean = false

    /**
     * Ongoing requests from [bringChildIntoView], with the invariant that it is always sorted by
     * overlapping order: each item's [Rect] completely overlaps the next item.
     *
     * May contain requests whose bounds are too big to fit in the current viewport. This is for a
     * few reasons:
     * 1. The viewport may shrink after a request was enqueued, causing a request that fit at the
     *    time it was enqueued to no longer fit.
     * 2. The size of the bounds of a request may change after it's added, causing it to grow larger
     *    than the viewport.
     * 3. Having complete information about too-big requests allows us to make the right decision
     *    about what part of the request to bring into view when smaller requests are also present.
     */
    private val bringIntoViewRequests = BringIntoViewRequestPriorityQueue()

    private var focusedChild: LayoutCoordinates? = null

    /**
     * The previous bounds of the [focusedChild] used by [onRemeasured] to calculate when the
     * focused child is first clipped when scrolling is reversed.
     */
    private var focusedChildBoundsFromPreviousRemeasure: Rect? = null

    /**
     * Set to true when this class is actively animating the scroll to keep the focused child in
     * view.
     */
    private var trackingFocusedChild = false

    /** The size of the scrollable container. */
    internal var viewportSize = IntSize.Zero
        private set

    private var isAnimationRunning = false

    override fun calculateRectForParent(localRect: Rect): Rect {
        checkPrecondition(viewportSize != IntSize.Zero) {
            "Expected BringIntoViewRequester to not be used before parents are placed."
        }
        // size will only be zero before the initial measurement.
        return computeDestination(localRect, viewportSize)
    }

    private fun requireBringIntoViewSpec(): BringIntoViewSpec {
        return bringIntoViewSpec ?: currentValueOf(LocalBringIntoViewSpec)
    }

    override suspend fun bringChildIntoView(localRect: () -> Rect?) {
        // Avoid creating no-op requests and no-op animations if the request does not require
        // scrolling or returns null.
        if (localRect()?.isMaxVisible() != false) return

        suspendCancellableCoroutine { continuation ->
            val request = Request(currentBounds = localRect, continuation = continuation)
            if (DEBUG) println("[$TAG] Registering bringChildIntoView request: $request")
            // Once the request is enqueued, even if it returns false, the queue will take care of
            // handling continuation cancellation so we don't need to do that here.
            if (bringIntoViewRequests.enqueue(request) && !isAnimationRunning) {
                launchAnimation()
            }
        }
    }

    fun onFocusBoundsChanged(newBounds: LayoutCoordinates?) {
        focusedChild = newBounds
    }

    override fun onRemeasured(size: IntSize) {
        val oldSize = viewportSize
        viewportSize = size

        // Don't care if the viewport grew.
        if (size >= oldSize) return

        if (DEBUG) println("[$TAG] viewport shrunk: $oldSize -> $size")

        getFocusedChildBounds()?.let { focusedChild ->
            if (DEBUG) println("[$TAG] focused child bounds: $focusedChild")
            val previousFocusedChildBounds = focusedChildBoundsFromPreviousRemeasure ?: focusedChild
            if (
                !isAnimationRunning &&
                    !trackingFocusedChild &&
                    // Resize caused it to go from being fully visible to at least partially
                    // clipped. Need to use the lastFocusedChildBounds to compare with the old size
                    // only to handle the case where scrolling direction is reversed: in that case,
                    // when
                    // the child first goes out-of-bounds, it will be out of bounds regardless of
                    // which
                    // size we pass in, so the only way to detect the change is to use the previous
                    // bounds.
                    previousFocusedChildBounds.isMaxVisible(oldSize) &&
                    !focusedChild.isMaxVisible(size)
            ) {
                if (DEBUG)
                    println("[$TAG] focused child was clipped by viewport shrink: $focusedChild")
                trackingFocusedChild = true
                launchAnimation()
            }

            this.focusedChildBoundsFromPreviousRemeasure = focusedChild
        }
    }

    private fun getFocusedChildBounds(): Rect? {
        if (!isAttached) return null
        val coordinates = requireLayoutCoordinates()
        val focusedChild = this.focusedChild?.takeIf { it.isAttached } ?: return null
        return coordinates.localBoundingBoxOf(focusedChild, clipBounds = false)
    }

    private fun launchAnimation() {
        val bringIntoViewSpec = requireBringIntoViewSpec()
        checkPrecondition(!isAnimationRunning) {
            "launchAnimation called when previous animation was running"
        }

        if (DEBUG) println("[$TAG] launchAnimation")
        val animationState = UpdatableAnimationState(BringIntoViewSpec.DefaultScrollAnimationSpec)
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            var cancellationException: CancellationException? = null
            val animationJob = coroutineContext.job

            try {
                isAnimationRunning = true
                scrollingLogic.scroll(scrollPriority = MutatePriority.Default) {
                    animationState.value = calculateScrollDelta(bringIntoViewSpec)
                    if (DEBUG)
                        println(
                            "[$TAG] Starting scroll animation down from ${animationState.value}…"
                        )
                    animationState.animateToZero(
                        // This lambda will be invoked on every frame, during the choreographer
                        // callback.
                        beforeFrame = { delta ->
                            // reverseDirection is actually opposite of what's passed in through the
                            // (vertical|horizontal)Scroll modifiers.
                            val scrollMultiplier = if (reverseDirection) 1f else -1f
                            val adjustedDelta = scrollMultiplier * delta
                            if (DEBUG)
                                println(
                                    "[$TAG] Scroll target changed by Δ$delta to " +
                                        "${animationState.value}, scrolling by $adjustedDelta " +
                                        "(reverseDirection=$reverseDirection)"
                                )
                            val consumedScroll =
                                with(scrollingLogic) {
                                    scrollMultiplier *
                                        scrollBy(
                                                offset = adjustedDelta.toOffset().reverseIfNeeded(),
                                                source = NestedScrollSource.UserInput
                                            )
                                            .reverseIfNeeded()
                                            .toFloat()
                                }
                            if (DEBUG) println("[$TAG] Consumed $consumedScroll of scroll")
                            if (consumedScroll.absoluteValue < delta.absoluteValue) {
                                // If the scroll state didn't consume all the scroll on this frame,
                                // it probably won't consume any more later either (we might have
                                // hit the scroll bounds). This is a terminal condition for the
                                // animation: If we don't cancel it, it could loop forever asking
                                // for a scroll that will never be consumed.
                                // Note this will cancel all pending BIV jobs.
                                // TODO(b/239671493) Should this trigger nested scrolling?
                                animationJob.cancel(
                                    "Scroll animation cancelled because scroll was not consumed " +
                                        "($consumedScroll < $delta)"
                                )
                            }
                        },
                        // This lambda will be invoked on every frame, but will be dispatched to run
                        // after the choreographer callback, and after any composition and layout
                        // passes for the frame. This means that the scroll performed in the above
                        // lambda will have been applied to the layout nodes.
                        afterFrame = {
                            if (DEBUG) println("[$TAG] afterFrame")

                            // Complete any BIV requests that were satisfied by this scroll
                            // adjustment.
                            bringIntoViewRequests.resumeAndRemoveWhile { bounds ->
                                // If a request is no longer attached, remove it.
                                if (bounds == null) return@resumeAndRemoveWhile true
                                bounds.isMaxVisible().also { visible ->
                                    if (DEBUG && visible) {
                                        println("[$TAG] Completed BIV request with bounds $bounds")
                                    }
                                }
                            }

                            // Stop tracking any KIV requests that were satisfied by this scroll
                            // adjustment.
                            if (
                                trackingFocusedChild &&
                                    getFocusedChildBounds()?.isMaxVisible() == true
                            ) {
                                if (DEBUG)
                                    println("[$TAG] Completed tracking focused child request")
                                trackingFocusedChild = false
                            }

                            // Compute a new scroll target taking into account any resizes,
                            // replacements, or added/removed requests since the last frame.
                            animationState.value = calculateScrollDelta(bringIntoViewSpec)
                            if (DEBUG)
                                println("[$TAG] scroll target after frame: ${animationState.value}")
                        }
                    )
                }

                // Complete any BIV requests if the animation didn't need to run, or if there were
                // requests that were too large to satisfy. Note that if the animation was
                // cancelled, this won't run, and the requests will be cancelled instead.
                if (DEBUG)
                    println(
                        "[$TAG] animation completed successfully, resuming" +
                            " ${bringIntoViewRequests.size} remaining BIV requests…"
                    )
                bringIntoViewRequests.resumeAndRemoveAll()
            } catch (e: CancellationException) {
                cancellationException = e
                throw e
            } finally {
                if (DEBUG) {
                    println(
                        "[$TAG] animation completed with ${bringIntoViewRequests.size} " +
                            "unsatisfied BIV requests"
                    )
                    cancellationException?.printStackTrace()
                }
                isAnimationRunning = false
                // Any BIV requests that were not completed should be considered cancelled.
                bringIntoViewRequests.cancelAndRemoveAll(cancellationException)
                trackingFocusedChild = false
            }
        }
    }

    /**
     * Calculates how far we need to scroll to satisfy all existing BringIntoView requests and the
     * focused child tracking.
     */
    private fun calculateScrollDelta(bringIntoViewSpec: BringIntoViewSpec): Float {
        if (viewportSize == IntSize.Zero) return 0f

        val rectangleToMakeVisible: Rect =
            findBringIntoViewRequest()
                ?: (if (trackingFocusedChild) getFocusedChildBounds() else null)
                ?: return 0f

        val size = viewportSize.toSize()
        return when (orientation) {
            Vertical ->
                bringIntoViewSpec.calculateScrollDistance(
                    rectangleToMakeVisible.top,
                    rectangleToMakeVisible.bottom - rectangleToMakeVisible.top,
                    size.height
                )
            Horizontal ->
                bringIntoViewSpec.calculateScrollDistance(
                    rectangleToMakeVisible.left,
                    rectangleToMakeVisible.right - rectangleToMakeVisible.left,
                    size.width
                )
        }
    }

    /** Find the largest BIV request that can completely fit inside the viewport. */
    private fun findBringIntoViewRequest(): Rect? {
        var rectangleToMakeVisible: Rect? = null
        bringIntoViewRequests.forEachFromSmallest { bounds ->
            // Ignore detached requests for now. They'll be removed later.
            if (bounds == null) return@forEachFromSmallest
            if (bounds.size <= viewportSize.toSize()) {
                rectangleToMakeVisible = bounds
            } else {
                // Found a request that doesn't fit, use the next-smallest one.
                // TODO(klippenstein) if there is a request that's too big to fit in the current
                //  bounds, we should try to fit the largest part of it that contains the
                //  next-smallest request.
                // if rectangleToMakeVisible is null, return the current bounds even if it is
                // oversized.
                return rectangleToMakeVisible ?: bounds
            }
        }
        return rectangleToMakeVisible
    }

    /**
     * Compute the destination given the source rectangle and current bounds.
     *
     * @param childBounds The bounding box of the item that sent the request to be brought into
     *   view.
     * @return the destination rectangle.
     */
    private fun computeDestination(childBounds: Rect, containerSize: IntSize): Rect {
        return childBounds.translate(-relocationOffset(childBounds, containerSize))
    }

    /**
     * Returns true if this [Rect] is as visible as it can be given the [size] of the viewport. This
     * means either it's fully visible or too big to fit in the viewport all at once and already
     * filling the whole viewport.
     */
    private fun Rect.isMaxVisible(size: IntSize = viewportSize): Boolean {
        val relocationOffset = relocationOffset(this, size)
        return abs(relocationOffset.x) <= MinScrollThreshold &&
            abs(relocationOffset.y) <= MinScrollThreshold
    }

    private fun relocationOffset(childBounds: Rect, containerSize: IntSize): Offset {
        val size = containerSize.toSize()
        return when (orientation) {
            Vertical ->
                Offset(
                    x = 0f,
                    y =
                        requireBringIntoViewSpec()
                            .calculateScrollDistance(
                                childBounds.top,
                                childBounds.bottom - childBounds.top,
                                size.height
                            )
                )
            Horizontal ->
                Offset(
                    x =
                        requireBringIntoViewSpec()
                            .calculateScrollDistance(
                                childBounds.left,
                                childBounds.right - childBounds.left,
                                size.width
                            ),
                    y = 0f
                )
        }
    }

    private operator fun IntSize.compareTo(other: IntSize): Int =
        when (orientation) {
            Horizontal -> width.compareTo(other.width)
            Vertical -> height.compareTo(other.height)
        }

    private operator fun Size.compareTo(other: Size): Int =
        when (orientation) {
            Horizontal -> width.compareTo(other.width)
            Vertical -> height.compareTo(other.height)
        }

    fun update(
        orientation: Orientation,
        reverseDirection: Boolean,
        bringIntoViewSpec: BringIntoViewSpec?
    ) {
        this.orientation = orientation
        this.reverseDirection = reverseDirection
        this.bringIntoViewSpec = bringIntoViewSpec
    }

    /**
     * A request to bring some [Rect] in the scrollable viewport.
     *
     * @param currentBounds A function that returns the current bounds that the request wants to
     *   make visible.
     * @param continuation The [CancellableContinuation] from the suspend function used to make the
     *   request.
     */
    internal class Request(
        val currentBounds: () -> Rect?,
        val continuation: CancellableContinuation<Unit>,
    ) {
        override fun toString(): String {
            // Include the coroutine name in the string, if present, to help debugging.
            val name = continuation.context[CoroutineName]?.name
            return "Request@${hashCode().toString(radix = 16)}" +
                (name?.let { "[$it](" } ?: "(") +
                "currentBounds()=${currentBounds()}, " +
                "continuation=$continuation)"
        }
    }
}
