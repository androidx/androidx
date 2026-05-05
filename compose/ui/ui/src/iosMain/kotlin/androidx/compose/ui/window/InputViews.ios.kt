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

import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.navigationevent.UIKitBackGestureRecognizer
import androidx.compose.ui.scene.PointerEventResult
import androidx.compose.ui.uikit.utils.CMPGestureRecognizer
import androidx.compose.ui.uikit.utils.CMPHoverGestureRecognizer
import androidx.compose.ui.uikit.utils.CMPPanGestureRecognizer
import androidx.compose.ui.uikit.utils.CMPScrollView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.toDpOffset
import androidx.compose.ui.viewinterop.InteropWrappingView
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import kotlin.getValue
import kotlin.math.abs
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectIsEmpty
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIEvent
import platform.UIKit.UIEventTypeTouches
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerState
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateCancelled
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UIGestureRecognizerStateFailed
import platform.UIKit.UIGestureRecognizerStatePossible
import platform.UIKit.UIPanGestureRecognizer
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIScreenEdgePanGestureRecognizer
import platform.UIKit.UIScrollTypeMaskAll
import platform.UIKit.UIScrollView
import platform.UIKit.UITouch
import platform.UIKit.UIView
import platform.UIKit.endEditing
import platform.UIKit.setAccessibilityElements
import platform.UIKit.setState

/**
 * A reason for why touches are sent to Compose
 */
internal enum class TouchesEventKind {
    /**
     * [UIEvent] when `touchesBegan`
     */
    BEGAN,

    /**
     * [UIEvent] when `touchesMoved`
     */
    MOVED,

    /**
     * [UIEvent] when `touchesEnded`
     */
    ENDED
}

private val UIGestureRecognizerState.isOngoing: Boolean
    get() =
        when (this) {
            UIGestureRecognizerStateBegan, UIGestureRecognizerStateChanged -> true
            else -> false
        }

/**
 * Implementation of [UIGestureRecognizer] that handles touch events and forwards
 * them. The main difference from the original [UIView] touches based is that it's built on top of
 * [UIGestureRecognizer], which play differently with UIKit touches processing and are required
 * for the correct handling of the touch events in interop scenarios, because they rely on
 * [UIGestureRecognizer] failure requirements and touches interception, which is an exclusive way
 * to control touches delivery to [UIView]s and their [UIGestureRecognizer]s in a fine-grain manner.
 */
private class TouchesGestureRecognizer(
    private var onTouchesEvent: (touches: Set<*>, event: UIEvent?, phase: TouchesEventKind) -> PointerEventResult,
    private var onCancelAllTouches: (touches: Set<*>) -> Unit,
    private var canIgnoreDragGesture: (UIGestureRecognizer) -> Boolean,
    private var ignoreTouchesChanges: () -> Boolean
) : CMPGestureRecognizer(target = null, action = null) {
    /**
     * Touches that are currently tracked by the gesture recognizer.
     */
    private val trackedTouches: MutableMap<UITouch, UIView?> = mutableMapOf()

    val hasTrackedTouches: Boolean get() = trackedTouches.isNotEmpty()

    /**
     * Scheduled job for the gesture recognizer failure.
     */
    private var failureJob: Job? = null

    init {
        // When recognized, immediately cancel all touches in the subviews.
        // This scenario shouldn't happen due to `delaysTouchesBegan`, so it's
        // more of a defensive line.
        cancelsTouchesInView = true

        // Delays touches reception by underlying views until the gesture recognizer is explicitly
        // stated as failed (aka, the touch sequence is targeted to the interop view).
        delaysTouchesBegan = true
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent) {
        super.touchesBegan(touches, withEvent)

        if (ignoreTouchesChanges()) {
            return
        }

        val touchesToInteractionMode = touches.associate { touch ->
            touch as UITouch
            val point = touch.locationInView(view)
            val hitTestResult = view?.hitTest(point, withEvent)?.takeIf { it != view }
            touch to hitTestResult
        }

        fun startTouchesEvent() {
            val isInitialTouches = trackedTouches.isEmpty()
            trackedTouches.putAll(touchesToInteractionMode)
            onTouchesEvent(trackedTouches.keys, withEvent, TouchesEventKind.BEGAN)
            if (isInitialTouches) {
                setState(UIGestureRecognizerStatePossible)
            } else if (state.isOngoing) {
                setState(UIGestureRecognizerStateChanged)
            }
        }

        val interactionMode = touchesToInteractionMode.map {
            it.value?.findAncestorInteractionMode(it.key)
        }.findMostRestrictedInteractionMode()
        when (interactionMode) {
            is UIKitInteropInteractionMode.Cooperative -> {
                startTouchesEvent()
                scheduleTouchesFailureIfNeeded(interactionMode.delayMillis)
            }

            UIKitInteropInteractionMode.NonCooperative -> {
                cancelAllTrackedTouches()
            }

            null -> {
                startTouchesEvent()
            }
        }
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent) {
        super.touchesMoved(touches, withEvent)

        if (ignoreTouchesChanges()) {
            return
        }

        fun processGesture() {
            if (trackedTouches.isEmpty()) {
                return
            }
            val result = onTouchesEvent(trackedTouches.keys, withEvent, TouchesEventKind.MOVED)
            if (result.anyMovementConsumed) {
                if (!state.isOngoing) {
                    setState(UIGestureRecognizerStateBegan)
                    cancelTouchesFailure()
                }
            }
        }

        // The TouchesGestureRecognizer receives touches earlier than its interop scroll views,
        // if any. If an interop scroll view is involved in tracking touches, we let it capture
        // the pan gesture first in order to prioritise the scrolling gesture of the child scroll
        // view.
        val postponeGesture = state == UIGestureRecognizerStatePossible &&
            touches.any { trackedTouches[it].hasTrackingUIScrollView() }
        if (postponeGesture) {
            CoroutineScope(Dispatchers.Main).launch { processGesture() }
        } else {
            processGesture()
        }
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent) {
        super.touchesEnded(touches, withEvent)

        if (ignoreTouchesChanges()) {
            cancelAllTrackedTouches()
            return
        }

        fun endTouchesEvent() {
            onTouchesEvent(trackedTouches.keys, withEvent, TouchesEventKind.ENDED)
            stopTrackingTouches(touches)
            if (trackedTouches.isEmpty()) {
                setState(UIGestureRecognizerStateEnded)
            }
        }

        if (state.isOngoing) {
            endTouchesEvent()
        } else {
            val hasHitTestResult = touches.firstNotNullOfOrNull { trackedTouches[it] } != null
            if (hasHitTestResult) {
                cancelAllTrackedTouches()
            } else {
                endTouchesEvent()
            }
        }
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent) {
        super.touchesCancelled(touches, withEvent)

        cancelAllTrackedTouches()
    }

    private fun cancelAllTrackedTouches() {
        setState(UIGestureRecognizerStateCancelled)
        onCancelAllTouches(trackedTouches.keys)
        trackedTouches.clear()
        cancelTouchesFailure()
    }

    private fun Collection<UIKitInteropInteractionMode?>.findMostRestrictedInteractionMode() =
        minBy {
            when (it) {
                UIKitInteropInteractionMode.NonCooperative -> 0
                is UIKitInteropInteractionMode.Cooperative -> it.delayMillis
                null -> Int.MAX_VALUE
            }
        }

    override fun canBePreventedByGestureRecognizer(
        preventingGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        return if (preventingGestureRecognizer is UIKitBackGestureRecognizer) {
            cancelAllTrackedTouches()
            true
        } else if (canIgnoreDragGesture(preventingGestureRecognizer)) {
            false
        } else if (preventingGestureRecognizer is ScrollGestureRecognizer
            && preventingGestureRecognizer.state.isOngoing) {
            cancelAllTrackedTouches()
            true
        } else if (isInChildHierarchy(preventingGestureRecognizer.view)) {
            if ((state == UIGestureRecognizerStatePossible || state.isOngoing) &&
                isScrollViewAtTheEndOfScrollableContent(preventingGestureRecognizer)
            ) {
                false
            } else {
                cancelAllTrackedTouches()
                true
            }
        } else {
            if (state.isOngoing || !preventingGestureRecognizer.state.isOngoing) {
                false
            } else {
                cancelAllTrackedTouches()
                true
            }
        }
    }

    override fun canPreventGestureRecognizer(
        preventedGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        return if (isInChildHierarchy(preventedGestureRecognizer.view)) {
            super.canPreventGestureRecognizer(preventedGestureRecognizer)
        } else if (preventedGestureRecognizer is UIScreenEdgePanGestureRecognizer) {
            false
        } else {
            state == UIGestureRecognizerStatePossible || state.isOngoing
        }
    }

    private val activeGestureStates = listOf(
        UIGestureRecognizerStatePossible,
        UIGestureRecognizerStateBegan,
        UIGestureRecognizerStateChanged
    )
    override fun shouldRequireFailureOfGestureRecognizer(
        otherGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        return (otherGestureRecognizer is UIKitBackGestureRecognizer &&
            otherGestureRecognizer.state in activeGestureStates) ||
            super.shouldRequireFailureOfGestureRecognizer(otherGestureRecognizer)
    }

    /**
     * Checks if compose can get priority over interop view with UIScrollView on it.
     *
     * @return return true if UIScrollView can no longer scroll content in the direction of the user
     * gesture that UIScrollView detected.
     */
    private fun isScrollViewAtTheEndOfScrollableContent(recognizer: UIGestureRecognizer): Boolean {
        val pan = recognizer as? UIPanGestureRecognizer ?: return false
        val scrollView = recognizer.view as? UIScrollView ?: return false

        val (diffX, diffY) = pan.translationInView(scrollView).useContents { x to y }
        val (offsetX, offsetY) = scrollView.contentOffset.useContents { x to y }
        val (contentWidth, contentHeight) = scrollView.contentSize.useContents { width to height }
        val (scrollWidth, scrollHeight) = scrollView.bounds.useContents { size.width to size.height }
        val insets = scrollView.contentInset.useContents { this }

        // If the scroll view has no scrollable content in a direction, it's always at the "end"
        // in that direction (e.g. OverlayInputView which is a UIScrollView with empty contentSize).
        val canScrollHorizontally = contentWidth > scrollWidth - insets.left - insets.right
        val canScrollVertically = contentHeight > scrollHeight - insets.top - insets.bottom

        val endOfHorizontal = !canScrollHorizontally ||
            (diffX >= 0 && offsetX.equalWithinPixelTolerance(-insets.left)) ||
            (diffX <= 0 && offsetX.equalWithinPixelTolerance(contentWidth - scrollWidth + insets.right))

        val endOfVertical = !canScrollVertically ||
            (diffY >= 0 && offsetY.equalWithinPixelTolerance(-insets.top)) ||
            (diffY <= 0 && offsetY.equalWithinPixelTolerance(contentHeight - scrollHeight + insets.bottom))

        return endOfHorizontal && endOfVertical
    }

    private fun isInChildHierarchy(child: UIView?): Boolean {
        val view = view ?: return false
        var iteratingView = child
        while (iteratingView != null) {
            if (view == iteratingView) {
                return true
            }
            iteratingView = iteratingView.superview
        }
        return false
    }

    /**
     * Intentionally clean up all dependencies to prevent retain cycles that
     * can be caused by implicit capture of the view by UIKit objects (such as [UIEvent]) in
     * some rare scenarios.
     */
    fun dispose() {
        cancelTouchesFailure()
        onTouchesEvent = { _, _, _ -> PointerEventResult(anyMovementConsumed = false) }
        onCancelAllTouches = {}
        canIgnoreDragGesture = { false }
        ignoreTouchesChanges = { false }
        trackedTouches.clear()
    }

    /**
     * Schedule the gesture recognizer failure after [delayMills].
     *
     * We still pass the touches to the interop view
     * until the gesture recognizer is explicitly failed.
     *
     * But when failure happens,
     * all tracked touches are forwarded to runtime as
     * and stop receiving touches from the system.
     *
     * This only happens if the hitTest is not the [OverlayInputView] itself.
     *
     * @see [cancelTouchesFailure]
     */
    private fun scheduleTouchesFailureIfNeeded(delayMills: Int) {
        failureJob?.cancel()

        if (delayMills != Int.MAX_VALUE) {
            failureJob = CoroutineScope(Dispatchers.Main).launch {
                delay(delayMills.toLong())

                cancelAllTrackedTouches()
            }
        }
    }

    private fun cancelTouchesFailure() {
        failureJob?.cancel()
        failureJob = null
    }

    /**
     * Stops tracking the given touches associated with [UIEvent]. If those are the last touches,
     * end the gesture and reset the internal state.
     */
    private fun stopTrackingTouches(touches: Set<*>) {
        for (touch in touches) {
            trackedTouches.remove(touch as UITouch)
        }
    }
}

private class ScrollGestureRecognizer(
    private var onScrollEvent: (position: DpOffset, delta: DpOffset, event: UIEvent?, eventKind: TouchesEventKind) -> Unit,
    private var onCancelScroll: () -> Unit
) : CMPPanGestureRecognizer(target = null, action = null) {

    init {
        setDelaysTouchesBegan(false)
        setDelaysTouchesEnded(false)
        setCancelsTouchesInView(false)
        setAllowedScrollTypesMask(UIScrollTypeMaskAll)
        addTarget(this, NSSelectorFromString(::onPan.name + ":"))
    }

    private var cursorPosition: DpOffset? = null
    private var previousPosition: DpOffset? = null
    private var event: UIEvent? = null

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun onPan(gestureRecognizer: UIPanGestureRecognizer) {
        val position = gestureRecognizer.locationInView(view).toDpOffset()

        when (gestureRecognizer.state) {
            UIGestureRecognizerStateBegan -> {
                onScrollEvent(position, DpOffset.Zero, event, TouchesEventKind.BEGAN)
                cursorPosition = position
                previousPosition = position
            }

            UIGestureRecognizerStateChanged -> {
                val delta = (previousPosition ?: position) - position
                onScrollEvent(cursorPosition ?: position, delta, event, TouchesEventKind.MOVED)
                previousPosition = position
            }

            UIGestureRecognizerStateEnded -> {
                val delta = (previousPosition ?: position) - position
                onScrollEvent(cursorPosition ?: position, delta, event, TouchesEventKind.ENDED)
                cursorPosition = null
                previousPosition = null
                event = null
            }

            UIGestureRecognizerStateCancelled, UIGestureRecognizerStateFailed -> {
                onCancelScroll()
                cursorPosition = null
                previousPosition = null
                event = null
            }

            else -> {}
        }
    }

    override fun shouldReceiveEvent(event: UIEvent): Boolean {
        this.event = event
        return super.shouldReceiveEvent(event)
    }

    fun dispose() {
        removeTarget(this, null)
        onScrollEvent = { _, _, _, _  -> }
        onCancelScroll = {}
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent) {
        // Gesture recognizer only works with the trackpad. All touches should be cancelled.
        setState(UIGestureRecognizerStateFailed)
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent) {
        // Do nothing. No need to handle touches for scroll gesture
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent) {
        // Do nothing. No need to handle touches for scroll gesture
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent) {
        // Do nothing. No need to handle touches for scroll gesture
    }
}

/**
 * The application can place interop views above and below the rendering canvas which is implemented
 * by using [OverlayInputView] and [BackgroundInputView].
 * The [OverlayInputView] is used to intercept all interaction events except the touches that
 * addressed to the interop views located below the rendering canvas (see [OverlayInputView.hitTest]
 * and  [BackgroundInputView.hitTest] for more details).
 */
internal class OverlayInputView(
    private var hitTestInteropView: (point: CValue<CGPoint>) -> UIView?,
    private var isPointInsideInteractionBounds: (CValue<CGPoint>) -> Boolean,
    private var onTouchesEvent: (touches: Set<*>, event: UIEvent?, phase: TouchesEventKind) -> PointerEventResult,
    private var onCancelAllTouches: (touches: Set<*>) -> Unit,
    onScrollEvent: (position: DpOffset, delta: DpOffset, event: UIEvent?, eventKind: TouchesEventKind) -> Unit,
    onCancelScroll: () -> Unit,
    private var onHoverEvent: (position: DpOffset, event: UIEvent?, eventKind: TouchesEventKind) -> Unit,
    private var onKeyboardPresses: (Set<*>) -> Unit,
    ignoreTouchChanges: () -> Boolean,
) : CMPScrollView(CGRectZero.readValue()) {
    /**
     * Gesture recognizer responsible for processing touches
     * and sending them to the Compose runtime.
     *
     * Also involved in the decision-making process of whether the touch sequence should be
     * passed to the Compose runtime or to the interop view.
     */
    private val touchesGestureRecognizer = TouchesGestureRecognizer(
        onTouchesEvent = ::handleTouchesEvent,
        onCancelAllTouches = ::handleCancelAllTouches,
        canIgnoreDragGesture = { canIgnoreDragGesture(it) },
        ignoreTouchesChanges = ignoreTouchChanges
    )

    private val scrollGestureRecognizer by lazy {
        if (available(OS.Ios to OSVersion(major = 13, minor = 4))) {
            ScrollGestureRecognizer(
                onScrollEvent = onScrollEvent,
                onCancelScroll = onCancelScroll
            )
        } else {
            null
        }
    }

    private val hoverGestureRecognizer by lazy {
        CMPHoverGestureRecognizer(this, NSSelectorFromString(::onHover.name + ":")).apply {
            delaysTouchesBegan = false
            delaysTouchesEnded = false
            cancelsTouchesInView = false
        }
    }

    /**
     * See [androidx.compose.ui.draganddrop.UIKitDragAndDropManager] for more context
     */
    var canIgnoreDragGesture: (UIGestureRecognizer) -> Boolean = { false }

    var isInterceptingOutsideEvents: Boolean = false
    var onOutsidePointerEvent: (PointerEventType) -> Unit = {}

    init {
        multipleTouchEnabled = true

        addGestureRecognizer(touchesGestureRecognizer)
        scrollGestureRecognizer?.let {
            addGestureRecognizer(it)
        }

        addGestureRecognizer(hoverGestureRecognizer)

        showsHorizontalScrollIndicator = false
        showsVerticalScrollIndicator = false
        delaysContentTouches = false
        panGestureRecognizer.setEnabled(false)
        panGestureRecognizer.delaysTouchesBegan = false
        panGestureRecognizer.delaysTouchesEnded = false
        bounces = false
        scrollsToTop = false
    }

    override fun canBecomeFirstResponder() = true

    override fun canBecomeFocused(): Boolean = false

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        onKeyboardPresses(presses)
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        onKeyboardPresses(presses)
        super.pressesEnded(presses, withEvent)
    }

    private val trackedTouchesOutside: MutableSet<UITouch> = mutableSetOf()
    private fun handleTouchesEvent(
        touches: Set<*>, event: UIEvent?, phase: TouchesEventKind
    ): PointerEventResult {
        if (isInterceptingOutsideEvents && event?.type == UIEventTypeTouches) {
            when (phase) {
                TouchesEventKind.BEGAN -> {
                    touches.forEach { touch ->
                        touch as UITouch
                        if (!isPointInsideInteractionBounds(touch.locationInView(this))) {
                            val isNewPressEvent = trackedTouchesOutside.isEmpty()
                            trackedTouchesOutside.add(touch)
                            if (isNewPressEvent) {
                                onOutsidePointerEvent(PointerEventType.Press)
                            }
                        }
                    }
                }

                TouchesEventKind.ENDED -> {
                    touches.forEach { touch ->
                        touch as UITouch
                        if (touch in trackedTouchesOutside) {
                            trackedTouchesOutside.remove(touch)
                            if (trackedTouchesOutside.isEmpty()) {
                                onOutsidePointerEvent(PointerEventType.Release)
                            }
                        }
                    }
                }

                TouchesEventKind.MOVED -> {}
            }
        }

        return onTouchesEvent(touches, event, phase)
    }

    private fun handleCancelAllTouches(touches: Set<*>) {
        trackedTouchesOutside.clear()
        onCancelAllTouches(touches)
    }

    private var previousSuccessHitTestTimestamp: Double? = null

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        if (!isPointInsideInteractionBounds(point)) {
            if (withEvent?.type != UIEventTypeTouches) {
                return null
            }
            if (isInterceptingOutsideEvents) {
                return this
            }
            if (previousSuccessHitTestTimestamp != withEvent.timestamp) {
                // This workaround needs to send PointerEventType.Press just once
                previousSuccessHitTestTimestamp = withEvent.timestamp
                onOutsidePointerEvent(PointerEventType.Press)
                onOutsidePointerEvent(PointerEventType.Release)
            }
            return null
        }
        if (withEvent?.type != UIEventTypeTouches) {
            return super.hitTest(point, withEvent)
        }
        val interopViewHitTest = hitTestInteropView(point)
        if (interopViewHitTest != null && interopViewHitTest.superview != this) {
            // Interop view is located inside another container.
            return null
        }
        val nativeTextInputViewHitTest = subviews.firstNotNullOfOrNull { it ->
            (it as? NativeTextInputScrollView)?.let {
                val inputPoint = convertPoint(point, toView = it)
                it.hitTest(inputPoint, withEvent)
            }
        }
        if (nativeTextInputViewHitTest != null) {
            return nativeTextInputViewHitTest
        }
        return super.hitTest(point, withEvent)
    }

    private var lastHoverPosition: DpOffset? = null
    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun onHover(gestureRecognizer: CMPHoverGestureRecognizer) {
        val position = gestureRecognizer.locationInView(this).toDpOffset()
        val lastEvent = hoverGestureRecognizer.lastReceivedEvent
        when (gestureRecognizer.state) {
            UIGestureRecognizerStateBegan ->
                onHoverEvent(position, lastEvent, TouchesEventKind.BEGAN)

            UIGestureRecognizerStateChanged ->
                if (lastHoverPosition != position && !touchesGestureRecognizer.hasTrackedTouches) {
                    onHoverEvent(position, lastEvent, TouchesEventKind.MOVED)
                }

            UIGestureRecognizerStateEnded ->
                onHoverEvent(position, lastEvent, TouchesEventKind.ENDED)

            UIGestureRecognizerStateCancelled,
            UIGestureRecognizerStateFailed ->
                onHoverEvent(lastHoverPosition ?: position, lastEvent, TouchesEventKind.ENDED)

            else -> {}
        }
        lastHoverPosition = position
    }

    fun dispose() {
        endEditing(force = true)
        removeGestureRecognizer(touchesGestureRecognizer)
        touchesGestureRecognizer.dispose()
        scrollGestureRecognizer?.let {
            removeGestureRecognizer(it)
            it.dispose()
        }
        removeGestureRecognizer(hoverGestureRecognizer)
        onHoverEvent = { _, _, _ -> }

        hitTestInteropView = { null }
        isPointInsideInteractionBounds = { false }
        canIgnoreDragGesture = { false }
        onKeyboardPresses = {}
        onOutsidePointerEvent = {}
        onTouchesEvent = { _, _, _ -> PointerEventResult() }
        onCancelAllTouches = {}
        trackedTouchesOutside.clear()
    }
}

/**
 * The [BackgroundInputView] handles only touch events that occur in the areas of the interop views
 * located below the rendering canvas.
 * All other user input events should be handled by the [OverlayInputView] or with its help.
 */
internal class BackgroundInputView(
    private var onLayoutSubviews: () -> Unit,
    private var hitTestInteropView: (point: CValue<CGPoint>) -> UIView?,
    private var isPointInsideInteractionBounds: (CValue<CGPoint>) -> Boolean,
    onTouchesEvent: (touches: Set<*>, event: UIEvent?, phase: TouchesEventKind) -> PointerEventResult,
    onCancelAllTouches: (touches: Set<*>) -> Unit,
    ignoreTouchChanges: () -> Boolean,
) : UIView(CGRectZero.readValue()) {

    private var onAppeared: (() -> Unit)? = null

    fun runOnceOnAppeared(block: () -> Unit) {
        onAppeared = {
            onAppeared = null
            block()
        }

        runOnAppearedIfEligible()
    }

    private fun runOnAppearedIfEligible() {
        if (window != null && !CGRectIsEmpty(frame)) {
            onAppeared?.invoke()
        }
    }

    override fun canBecomeFocused(): Boolean = false

    override fun layoutSubviews() {
        super.layoutSubviews()

        onLayoutSubviews()
        runOnAppearedIfEligible()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()

        setNeedsLayout()
    }

    private val touchesGestureRecognizer = TouchesGestureRecognizer(
        onTouchesEvent = onTouchesEvent,
        onCancelAllTouches = onCancelAllTouches,
        canIgnoreDragGesture = { false },
        ignoreTouchesChanges = ignoreTouchChanges
    )

    init {
        multipleTouchEnabled = true

        addGestureRecognizer(touchesGestureRecognizer)

        setAccessibilityElements(emptyList<Any>())
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        if (!isPointInsideInteractionBounds(point)) {
            return null
        }
        return hitTestInteropView(point)
            ?.takeIf { it.superview == this }
            ?.let {
                it.hitTest(
                    point = convertPoint(point, toView = it),
                    withEvent = withEvent
                )
            }
    }

    fun dispose() {
        endEditing(force = true)
        removeGestureRecognizer(touchesGestureRecognizer)
        touchesGestureRecognizer.dispose()

        hitTestInteropView = { null }
        isPointInsideInteractionBounds = { false }
        onLayoutSubviews = {}
        onAppeared = null
    }
}

/**
 * There is no way to associate [InteropWrappingView.interactionMode] with a given [UIView.hitTest]
 * query. This extension method allows finding the nearest [InteropWrappingView] up the view
 * hierarchy and request the value retroactively.
 */
private fun UIView.findAncestorInteractionMode(touch: UITouch): UIKitInteropInteractionMode? {
    var view: UIView? = this
    while (view != null) {
        if (view is InteropWrappingView) {
            return view.interactionMode
        }
        if (view is NativeTextInputScrollView) {
            return view.interactionModeAt(touch.locationInView(view))
        }
        view = view.superview
    }
    return null
}

private fun Double.equalWithinPixelTolerance(other: Double): Boolean {
    return abs(other - this) < 0.1 // Any number smaller than a pixel size is sufficient here
}

private fun UIView?.hasTrackingUIScrollView(): Boolean {
    var view: UIView? = this
    while (view != null) {
        if (view is InteropWrappingView) {
            return false
        }
        if (view is UIScrollView &&
            view.userInteractionEnabled &&
            view.scrollEnabled &&
            view.panGestureRecognizer.isEnabled()) {
            if ((view.panGestureRecognizer.state == UIGestureRecognizerStatePossible ||
                    view.panGestureRecognizer.state == UIGestureRecognizerStateBegan) &&
                view.isTracking()
            ) {
                return true
            }
        }
        view = view.superview
    }
    return false
}
