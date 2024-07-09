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

import androidx.compose.ui.uikit.utils.CMPGestureRecognizer
import androidx.compose.ui.uikit.utils.CMPGestureRecognizerHandlerProtocol
import androidx.compose.ui.viewinterop.InteropView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIView
import platform.UIKit.UITouchPhase
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIPress
import platform.darwin.NSObject

/**
 * Subset of [UITouchPhase] reflecting immediate phase when event is received by the [UIView] or
 * [UIGestureRecognizer].
 */
internal enum class CupertinoTouchesPhase {
    BEGAN, MOVED, ENDED, CANCELLED
}

private class GestureRecognizerHandlerImpl(
    private var onTouchesEvent: (view: UIView, touches: Set<*>, event: UIEvent, phase: CupertinoTouchesPhase) -> Unit,
    private var view: UIView?,
    private val onTouchesCountChanged: (by: Int) -> Unit

): NSObject(), CMPGestureRecognizerHandlerProtocol {
    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        onTouchesCountChanged(touches.size)

        val view = view ?: return
        val event = withEvent ?: return

        onTouchesEvent(view, touches, event, CupertinoTouchesPhase.BEGAN)
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        val view = view ?: return
        val event = withEvent ?: return

        onTouchesEvent(view, touches, event, CupertinoTouchesPhase.MOVED)
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        onTouchesCountChanged(-touches.size)

        val view = view ?: return
        val event = withEvent ?: return

        onTouchesEvent(view, touches, event, CupertinoTouchesPhase.ENDED)
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        onTouchesCountChanged(-touches.size)

        val view = view ?: return
        val event = withEvent ?: return

        onTouchesEvent(view, touches, event, CupertinoTouchesPhase.CANCELLED)
    }

    override fun shouldRecognizeSimultaneously(first: UIGestureRecognizer, withOther: UIGestureRecognizer): Boolean {
        return true
    }

    fun dispose() {
        onTouchesEvent = { _, _, _, _ -> }
    }
}

/**
 * [UIView] subclass that handles touches and keyboard presses events and forwards them
 * to the Compose runtime.
 *
 * @param hitTestInteropView A callback to find an [InteropView] at the given point.
 * @param onTouchesEvent A callback to notify the Compose runtime about touch events.
 * @param onTouchesCountChange A callback to notify the Compose runtime about the number of tracked
 * touches.
 * @param inInteractionBounds A callback to check if the given point is within the interaction
 * bounds as defined by the owning implementation.
 * @param onKeyboardPresses A callback to notify the Compose runtime about keyboard presses.
 * The parameter is a [Set] of [UIPress] objects. Erasure happens due to K/N not supporting Obj-C
 * lightweight generics.
 */
internal class InteractionUIView(
    private var hitTestInteropView: (point: CValue<CGPoint>, event: UIEvent?) -> InteropView?,
    onTouchesEvent: (view: UIView, touches: Set<*>, event: UIEvent, phase: CupertinoTouchesPhase) -> Unit,
    private var onTouchesCountChange: (count: Int) -> Unit,
    private var inInteractionBounds: (CValue<CGPoint>) -> Boolean,
    private var onKeyboardPresses: (Set<*>) -> Unit,
) : UIView(CGRectZero.readValue()) {
    private val gestureRecognizerHandler = GestureRecognizerHandlerImpl(
        view = this,
        onTouchesEvent = onTouchesEvent,
        onTouchesCountChanged = { _touchesCount += it }
    )

    private val gestureRecognizer = CMPGestureRecognizer()

    /**
     * When there at least one tracked touch, we need notify redrawer about it. It should schedule
     * CADisplayLink which affects frequency of polling UITouch events on high frequency display
     * and forces it to match display refresh rate.
     */
    private var _touchesCount = 0
        set(value) {
            field = value
            onTouchesCountChange(value)
        }

    init {
        multipleTouchEnabled = true
        userInteractionEnabled = true

        addGestureRecognizer(gestureRecognizer)
        gestureRecognizer.handler = gestureRecognizerHandler
    }

    override fun canBecomeFirstResponder() = true

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        onKeyboardPresses(presses)
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        onKeyboardPresses(presses)
        super.pressesEnded(presses, withEvent)
    }

    // TODO: inspect if touches should be forwarded further up the responder chain
    //  via super call or they considered to be consumed by this view

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesBegan(touches, withEvent)
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesMoved(touches, withEvent)
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesEnded(touches, withEvent)
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        super.touchesCancelled(touches, withEvent)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        if (!inInteractionBounds(point)) {
            return null
        }

        // Find if a scene contains a node [InteropViewModifier] at the given point.
        // Native [hitTest] happens after [pointInside] is checked. If hit testing
        // inside ComposeScene didn't yield any interop view, then we should return [this]
        val interopView = hitTestInteropView(point, withEvent) ?: return this

        // Transform the point to the interop view's coordinate system.
        // And perform native [hitTest] on the interop view.
        val hitTestView = interopView.hitTest(
            point = convertPoint(point, toView = interopView),
            withEvent = withEvent)

        return hitTestView ?: this
    }

    /**
     * Intentionally clean up all dependencies of InteractionUIView to prevent retain cycles that
     * can be caused by implicit capture of the view by UIKit objects (such as UIEvent).
     */
    fun dispose() {
        gestureRecognizerHandler.dispose()
        gestureRecognizer.handler = null
        removeGestureRecognizer(gestureRecognizer)

        hitTestInteropView = { _, _ -> null }

        onTouchesCountChange = {}
        inInteractionBounds = { false }
        onKeyboardPresses = {}
    }
}
