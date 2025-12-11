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

package androidx.compose.ui.scene

import androidx.compose.ui.input.pointer.PointerEventType
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UIEventTypeTouches
import platform.UIKit.UITouch
import platform.UIKit.UIView
import platform.UIKit.UIWindow

/**
 * A backing ComposeSceneLayer view for each Compose scene layer. Its task is to
 * handle events that start outside the bounds of the layer content
 *
 * @param isInsideInteractionBounds a function that returns true if the given point is inside the
 * interactable content part of the layer.
 * @param isInterceptingOutsideEvents a function that returns true if the layer should intercept
 * events that start outside the bounds of the layer content or should let them pass through.
 */
internal class UIKitComposeSceneLayerView(
    private var onDidMoveToWindow: (UIWindow?) -> Unit,
    private var isInsideInteractionBounds: (point: CValue<CGPoint>) -> Boolean,
    private var isInterceptingOutsideEvents: () -> Boolean
): UIView(frame = CGRectZero.readValue()) {
    private var touchesCount: Int = 0
    private var previousSuccessHitTestTimestamp: Double? = null

    internal var onOutsidePointerEvent: ((
        eventType: PointerEventType
    ) -> Unit)? = null

    init {
        setMultipleTouchEnabled(true)
    }

    private fun touchStartedOutside(withEvent: UIEvent?) {
        // hitTest call can happen multiple times for the same touch event, ensure we only send
        // PointerEventType.Press once using the timestamp.
        if (previousSuccessHitTestTimestamp != withEvent?.timestamp) {
            // This workaround needs to send PointerEventType.Press just once
            previousSuccessHitTestTimestamp = withEvent?.timestamp
            onOutsidePointerEvent?.invoke(PointerEventType.Press)
        }
    }

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesBegan(touches, withEvent)

        touchesCount += touches.size
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesCancelled(touches, withEvent)

        touchesCount -= touches.size
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        touchesCount -= touches.size

        // It was the last touch in the sequence, calculate the centroid and if it's outside
        // the bounds, send `onOutsidePointerEvent`. Otherwise, just return.
        if (touchesCount > 0) {
            return
        }

        val location = requireNotNull(
            touches
                .map { it as UITouch }
                .centroidLocationInView(this)
        ) {
            "touchesEnded should not be called with an empty set of touches"
        }

        if (!isInsideInteractionBounds(location)) {
            onOutsidePointerEvent?.invoke(PointerEventType.Release)
        }

        super.touchesEnded(touches, withEvent)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        val isOutsideBounds = !isInsideInteractionBounds(point)

        val result = super.hitTest(point, withEvent)

        if (isOutsideBounds && result == this) {
            if (withEvent?.type == UIEventTypeTouches) {
                touchStartedOutside(withEvent)
            }

            return if (isInterceptingOutsideEvents()) {
                this
            } else {
                null
            }
        }

        return result
    }

    fun dispose() {
        onDidMoveToWindow = {}
        isInsideInteractionBounds = { false }
        isInterceptingOutsideEvents = { false }
        onOutsidePointerEvent = {}
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        onDidMoveToWindow(window)
    }
}

/**
 * Calculate the centroid location of the touches in the given collection.
 *
 * @param view The view in which coordinate space calculation is performed.
 *
 * @return The centroid location of the touches in [this] collection in the coordinate space
 * of the given [view]. Or `null` if [this] is empty.
 */
private fun Collection<UITouch>.centroidLocationInView(view: UIView): CValue<CGPoint>? {
    if (isEmpty()) {
        return null
    }

    var centroidX = 0.0
    var centroidY = 0.0

    for (touch in this) {
        val location = touch.locationInView(view)
        location.useContents {
            centroidX += x
            centroidY += y
        }
    }

    return CGPointMake(
        x = centroidX / size.toDouble(),
        y = centroidY / size.toDouble()
    )
}