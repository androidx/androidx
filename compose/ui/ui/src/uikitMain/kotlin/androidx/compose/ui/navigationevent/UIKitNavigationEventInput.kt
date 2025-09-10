/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.navigationevent

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.uikit.utils.CMPScreenEdgePanGestureRecognizer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.asDpRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.width
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventInput
import kotlin.math.abs
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CACurrentMediaTime
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateCancelled
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UIGestureRecognizerStateFailed
import platform.UIKit.UIRectEdgeLeft
import platform.UIKit.UIRectEdgeRight
import platform.UIKit.UIScreenEdgePanGestureRecognizer
import platform.UIKit.UIView
import platform.UIKit.UIWindow
import platform.darwin.NSObject

internal class UIKitNavigationEventInput(
    private val density: Density,
    private val getTopLeftOffsetInWindow: () -> IntOffset
) : NavigationEventInput() {
    companion object {
        private const val BACK_GESTURE_SCREEN_SIZE = 0.3
        private const val BACK_GESTURE_VELOCITY = 100
    }

    private val iosGestureHandler = UiKitScreenEdgePanGestureHandler()

    private val leftEdgePanGestureRecognizer = UIKitBackGestureRecognizer(
        target = iosGestureHandler,
        action = NSSelectorFromString(UiKitScreenEdgePanGestureHandler::handleEdgePan.name + ":")
    ).apply {
        edges = UIRectEdgeLeft
    }

    private val rightEdgePanGestureRecognizer = UIKitBackGestureRecognizer(
        target = iosGestureHandler,
        action = NSSelectorFromString(UiKitScreenEdgePanGestureHandler::handleEdgePan.name + ":")
    ).apply {
        edges = UIRectEdgeRight
    }

    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        super.onAdded(dispatcher)
        updateRecognizersEnabledState(dispatcher.hasEnabledCallbacks())
    }

    override fun onHasEnabledCallbacksChanged(hasEnabledCallbacks: Boolean) {
        super.onHasEnabledCallbacksChanged(hasEnabledCallbacks)
        updateRecognizersEnabledState(hasEnabledCallbacks)
    }

    override fun onRemoved() {
        super.onRemoved()
        updateRecognizersEnabledState(false)
    }

    private fun updateRecognizersEnabledState(isEnabled: Boolean) {
        leftEdgePanGestureRecognizer.enabled = isEnabled
        rightEdgePanGestureRecognizer.enabled = isEnabled
    }

    private val activeGestureStates = listOf(
        UIGestureRecognizerStateBegan,
        UIGestureRecognizerStateChanged
    )

    val isBackGestureActive: Boolean
        get() =
            leftEdgePanGestureRecognizer.state in activeGestureStates ||
                rightEdgePanGestureRecognizer.state in activeGestureStates

    fun onDidMoveToWindow(window: UIWindow?, composeRootView: UIView) {
        removeGestureListeners()
        if (window != null) {
            var view: UIView = composeRootView
            while (view.superview != window) {
                view = requireNotNull(view.superview) {
                    "Window is not null, but superview is null for ${view.debugDescription}"
                }
            }
            addGestureListeners(view)
        }
    }

    private fun addGestureListeners(view: UIView) {
        view.addGestureRecognizer(leftEdgePanGestureRecognizer)
        view.addGestureRecognizer(rightEdgePanGestureRecognizer)
    }

    private fun removeGestureListeners() {
        leftEdgePanGestureRecognizer.view?.removeGestureRecognizer(leftEdgePanGestureRecognizer)
        rightEdgePanGestureRecognizer.view?.removeGestureRecognizer(rightEdgePanGestureRecognizer)
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
            dispatchOnCompleted()
            return true
        } else {
            return false
        }
    }

    @OptIn(BetaInteropApi::class, ExperimentalComposeUiApi::class)
    inner class UiKitScreenEdgePanGestureHandler : NSObject() {
        @ObjCAction
        fun handleEdgePan(recognizer: UIScreenEdgePanGestureRecognizer) {
            val view = recognizer.view ?: return
            when (recognizer.state) {
                UIGestureRecognizerStateBegan -> {
                    val touch = recognizer.locationOfTouch(0u, view).asDpOffset()
                    val eventOffset =
                        touch.toOffset(density) - getTopLeftOffsetInWindow().toOffset()
                    dispatchOnStarted(
                        NavigationEvent(
                            touchX = eventOffset.x,
                            touchY = eventOffset.y,
                            swipeEdge = if (recognizer.edges == UIRectEdgeLeft) {
                                NavigationEvent.EDGE_LEFT
                            } else {
                                NavigationEvent.EDGE_RIGHT
                            },
                            progress = 0f,
                            frameTimeMillis = CACurrentMediaTime().toLong() * 1000
                        )
                    )
                }

                UIGestureRecognizerStateChanged -> {
                    val touch = recognizer.locationOfTouch(0u, view).asDpOffset()
                    val eventOffset =
                        touch.toOffset(density) - getTopLeftOffsetInWindow().toOffset()
                    val leftEdge = recognizer.edges == UIRectEdgeLeft
                    val bounds = view.bounds.asDpRect()
                    val progress = if (leftEdge) {
                        touch.x / bounds.width
                    } else {
                        (bounds.width - touch.x) / bounds.width
                    }
                    dispatchOnProgressed(
                        NavigationEvent(
                            touchX = eventOffset.x,
                            touchY = eventOffset.y,
                            swipeEdge = if (leftEdge) {
                                NavigationEvent.EDGE_LEFT
                            } else {
                                NavigationEvent.EDGE_RIGHT
                            },
                            progress = progress,
                            frameTimeMillis = CACurrentMediaTime().toLong() * 1000
                        )
                    )
                }

                UIGestureRecognizerStateEnded -> {
                    val translation = recognizer.translationInView(view = view)
                    val velocity = recognizer.velocityInView(view)

                    velocity.useContents velocity@{
                        translation.useContents {
                            view.bounds.useContents {
                                val edge = recognizer.edges
                                val velX =
                                    if (edge == UIRectEdgeLeft) this@velocity.x else -this@velocity.x
                                when {
                                    //if movement is fast in the right direction
                                    velX > BACK_GESTURE_VELOCITY -> dispatchOnCompleted()
                                    //if movement is backward
                                    velX < -10 -> dispatchOnCancelled()
                                    //if there is no movement, or the movement is slow,
                                    //but the touch is already more than BACK_GESTURE_SCREEN_SIZE
                                    abs(x) >= size.width * BACK_GESTURE_SCREEN_SIZE -> dispatchOnCompleted()
                                    else -> dispatchOnCancelled()
                                }
                            }
                        }
                    }
                }

                UIGestureRecognizerStateCancelled -> {
                    dispatchOnCancelled()
                }

                UIGestureRecognizerStateFailed -> {
                    dispatchOnCompleted()
                }
            }
        }
    }
}

/**
 * A special gesture recognizer that can cancel touches in the Compose scene.
 * See [androidx.compose.ui.window.TouchesGestureRecognizer.canBePreventedByGestureRecognizer]
 */
internal class UIKitBackGestureRecognizer(
    target: Any?, action: CPointer<out CPointed>?
) : CMPScreenEdgePanGestureRecognizer(target = target, action = action) {
    init {
        setDelaysTouchesBegan(true)
        setDelaysTouchesEnded(true)
        setCancelsTouchesInView(true)
    }

    override fun canBePreventedByGestureRecognizer(
        preventingGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        return false
    }

    override fun canPreventGestureRecognizer(
        preventedGestureRecognizer: UIGestureRecognizer
    ): Boolean {
        return true
    }
}