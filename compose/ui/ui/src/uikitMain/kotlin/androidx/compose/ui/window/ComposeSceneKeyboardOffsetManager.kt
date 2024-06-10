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

package androidx.compose.ui.window

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.ui.scene.ComposeSceneMediator
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.uikit.systemDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toDpRect
import kotlin.math.max
import kotlin.math.min
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectGetMinY
import platform.CoreGraphics.CGRectIsEmpty
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSDefaultRunLoopMode
import platform.Foundation.NSRunLoop
import platform.QuartzCore.CADisplayLink
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionBeginFromCurrentState
import platform.UIKit.UIViewAnimationOptionCurveEaseInOut
import platform.UIKit.UIViewAnimationOptions
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.sel_registerName

internal class ComposeSceneKeyboardOffsetManager(
    private val configuration: ComposeUIViewControllerConfiguration,
    private val keyboardOverlapHeightState: MutableState<Dp>,
    private val viewProvider: () -> UIView,
    private val composeSceneMediatorProvider: () -> ComposeSceneMediator?,
    private val onComposeSceneOffsetChanged: (Double) -> Unit,
) : KeyboardVisibilityObserver {
    private var isDisposed: Boolean = false

    val view get() = viewProvider()

    fun start() {
        KeyboardVisibilityListener.addObserver(this)

        adjustViewBounds(
            KeyboardVisibilityListener.keyboardFrame,
            KeyboardVisibilityListener.keyboardFrame,
            0.0,
            UIViewAnimationOptionCurveEaseInOut
        )
    }

    fun stop() {
        KeyboardVisibilityListener.removeObserver(this)
    }

    fun dispose() {
        check (!isDisposed) { "ComposeSceneKeyboardOffsetManager is already disposed" }
        isDisposed = true
        stop()
    }

    /**
     * Invisible view to track system keyboard animation
     */
    private val animationView: UIView by lazy {
        UIView(CGRectZero.readValue()).apply {
            hidden = true
        }
    }
    private var keyboardAnimationListener: CADisplayLink? = null

    override fun keyboardWillShow(
        targetFrame: CValue<CGRect>,
        duration: Double,
        animationOptions: UIViewAnimationOptions
    ) {
    }

    override fun keyboardWillChangeFrame(
        targetFrame: CValue<CGRect>,
        duration: Double,
        animationOptions: UIViewAnimationOptions
    ) {
        adjustViewBounds(
            KeyboardVisibilityListener.keyboardFrame,
            targetFrame,
            max(0.1, duration),
            animationOptions
        )
    }

    override fun keyboardWillHide(
        targetFrame: CValue<CGRect>,
        duration: Double,
        animationOptions: UIViewAnimationOptions
    ) {
    }

    private fun adjustViewBounds(
        currentFrame: CValue<CGRect>,
        targetFrame: CValue<CGRect>,
        duration: Double,
        animationOptions: UIViewAnimationOptions
    ) {
        val screen = view.window?.screen ?: return

        fun keyboardHeight(frame: CValue<CGRect>): Double {
            return if (CGRectIsEmpty(frame)) {
                0.0
            } else {
                max(0.0, screen.bounds.useContents { size.height } - CGRectGetMinY(frame))
            }
        }

        val bottomIndent = run {
            val screenHeight = screen.bounds.useContents { size.height }
            val composeViewBottomY = screen.coordinateSpace.convertPoint(
                point = CGPointMake(0.0, view.frame.useContents { size.height }),
                fromCoordinateSpace = view.coordinateSpace
            ).useContents { y }
            screenHeight - composeViewBottomY - viewBottomOffset
        }

        animateKeyboard(
            previousKeyboardHeight = keyboardHeight(currentFrame),
            keyboardHeight = keyboardHeight(targetFrame),
            viewBottomIndent = bottomIndent,
            duration = duration,
            animationOptions = animationOptions
        )
    }

    @OptIn(ExperimentalComposeApi::class)
    private fun animateKeyboard(
        previousKeyboardHeight: Double,
        keyboardHeight: Double,
        viewBottomIndent: Double,
        duration: Double,
        animationOptions: UIViewAnimationOptions
    ) {
        // Animate view from 0 to [animationTargetSize] and normalize to animation progress with
        // range of [0..1] to follow UIKit animation curve values.
        val animationTargetSize = 1000.0
        val animationTargetFrame = CGRectMake(0.0, 0.0, 0.0, animationTargetSize)
        fun getCurrentAnimationProgress(): Double {
            val layer = animationView.layer.presentationLayer() ?: return 0.0
            return layer.frame.useContents { size.height / animationTargetSize }
        }

        fun updateAnimationValues(progress: Double) {
            val currentHeight = previousKeyboardHeight +
                (keyboardHeight - previousKeyboardHeight) * progress
            val currentOverlapHeight = max(0.0, currentHeight - viewBottomIndent)

            val targetBottomOffset = calcFocusedBottomOffsetY(currentOverlapHeight)
            viewBottomOffset += (targetBottomOffset - viewBottomOffset) * progress

            keyboardOverlapHeightState.value = currentOverlapHeight.dp
        }

        //attach to root view if needed
        if (animationView.superview == null) {
            view.addSubview(animationView)
        }

        //cancel previous animation
        animationView.layer.removeAllAnimations()
        keyboardAnimationListener?.invalidate()

        UIView.performWithoutAnimation {
            animationView.setFrame(CGRectZero.readValue())
        }

        //animation listener
        val keyboardDisplayLink = CADisplayLink.displayLinkWithTarget(
            target = object : NSObject() {
                @OptIn(BetaInteropApi::class)
                @Suppress("unused")
                @ObjCAction
                fun animationDidUpdate() {
                    updateAnimationValues(getCurrentAnimationProgress())
                }
            },
            selector = sel_registerName("animationDidUpdate")
        )
        keyboardAnimationListener = keyboardDisplayLink

        fun completeAnimation() {
            animationView.removeFromSuperview()
            if (keyboardAnimationListener == keyboardDisplayLink) {
                keyboardAnimationListener = null
            }
            updateAnimationValues(1.0)
        }

        UIView.animateWithDuration(
            duration = duration,
            delay = 0.0,
            options = animationOptions or UIViewAnimationOptionBeginFromCurrentState,
            animations = {
                animationView.setFrame(animationTargetFrame)
            },
            completion = { isFinished ->
                keyboardDisplayLink.invalidate()
                if (isFinished) {
                    completeAnimation()
                }
            }
        )
        // HACK: Add display link observer to run loop in the next run loop cycle to fix issue
        // where view's presentationLayer sometimes gets end bounds on the first animation frame
        // instead of the initial one.
        dispatch_async(dispatch_get_main_queue()) {
            keyboardDisplayLink.addToRunLoop(NSRunLoop.mainRunLoop(), NSDefaultRunLoopMode)
        }
    }

    private fun calcFocusedBottomOffsetY(overlappingHeight: Double): Double {
        if (configuration.onFocusBehavior != OnFocusBehavior.FocusableAboveKeyboard) {
            return 0.0
        }
        val mediator = composeSceneMediatorProvider()
        val focusedRect =
            mediator?.focusManager?.getFocusRect()?.toDpRect(view.systemDensity) ?: return 0.0

        val viewHeight = view.frame.useContents { size.height }

        val hiddenPartOfFocusedElement = overlappingHeight - viewHeight + focusedRect.bottom.value
        return if (hiddenPartOfFocusedElement > 0) {
            // If focused element is partially hidden by the keyboard, we need to lift it upper
            val focusedTopY = focusedRect.top.value
            val isFocusedElementRemainsVisible = hiddenPartOfFocusedElement < focusedTopY
            if (isFocusedElementRemainsVisible) {
                // We need to lift focused element to be fully visible
                min(overlappingHeight, hiddenPartOfFocusedElement)
            } else {
                // In this case focused element height is bigger than remain part of the screen after showing the keyboard.
                // Top edge of focused element should be visible. Same logic on Android.
                min(overlappingHeight, maxOf(focusedTopY, 0f).toDouble())
            }
        } else {
            // Focused element is not hidden by the keyboard.
            0.0
        }
    }

    private var viewBottomOffset: Double = 0.0
        set(newValue) {
            field = newValue

            // In certain edge cases the scene might be disposed before updateAnimationValues is called
            // Simply don't forward the offset change in this case to avoid calling anything on closed ComposeScene.
            if (!isDisposed) {
                onComposeSceneOffsetChanged(newValue)
            }
        }
}
