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

import androidx.compose.runtime.MutableState
import androidx.compose.ui.scene.ComposeSceneFocusManager
import androidx.compose.ui.scene.ComposeSceneMediator
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.toDpRect
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDefaultRunLoopMode
import platform.Foundation.NSNotification
import platform.Foundation.NSRunLoop
import platform.Foundation.NSValue
import platform.QuartzCore.CADisplayLink
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.CGRectValue
import platform.UIKit.UIKeyboardAnimationDurationUserInfoKey
import platform.UIKit.UIKeyboardFrameEndUserInfoKey
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.sel_registerName

internal interface KeyboardVisibilityListener {
    fun keyboardWillShow(arg: NSNotification)
    fun keyboardWillHide(arg: NSNotification)
}

internal class KeyboardVisibilityListenerImpl(
    private val configuration: ComposeUIViewControllerConfiguration,
    private val keyboardOverlapHeightState: MutableState<Float>,
    private val viewProvider: () -> UIView,
    private val densityProvider: () -> Density,
    private val composeSceneMediatorProvider: () -> ComposeSceneMediator,
    private val focusManager: ComposeSceneFocusManager,
) : KeyboardVisibilityListener {

    val view get() = viewProvider()

    //invisible view to track system keyboard animation
    private val keyboardAnimationView: UIView by lazy {
        UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
            hidden = true
        }
    }
    private var keyboardAnimationListener: CADisplayLink? = null

    override fun keyboardWillShow(arg: NSNotification) {
        animateKeyboard(arg, true)

        val mediator = composeSceneMediatorProvider()
        val userInfo = arg.userInfo ?: return
        val keyboardInfo = userInfo[UIKeyboardFrameEndUserInfoKey] as NSValue
        val keyboardHeight = keyboardInfo.CGRectValue().useContents { size.height }
        if (configuration.onFocusBehavior == OnFocusBehavior.FocusableAboveKeyboard) {
            val focusedRect = focusManager.getFocusRect()?.toDpRect(densityProvider())

            if (focusedRect != null) {
                updateViewBounds(
                    offsetY = calcFocusedLiftingY(mediator, focusedRect, keyboardHeight)
                )
            }
        }
    }

    override fun keyboardWillHide(arg: NSNotification) {
        animateKeyboard(arg, false)

        if (configuration.onFocusBehavior == OnFocusBehavior.FocusableAboveKeyboard) {
            updateViewBounds(offsetY = 0.0)
        }
    }

    private fun animateKeyboard(arg: NSNotification, isShow: Boolean) {
        val userInfo = arg.userInfo!!

        //return actual keyboard height during animation
        fun getCurrentKeyboardHeight(): CGFloat {
            val layer = keyboardAnimationView.layer.presentationLayer() ?: return 0.0
            return layer.frame.useContents { origin.y }
        }

        //attach to root view if needed
        if (keyboardAnimationView.superview == null) {
            view.addSubview(keyboardAnimationView)
        }

        //cancel previous animation
        keyboardAnimationView.layer.removeAllAnimations()
        keyboardAnimationListener?.invalidate()

        //synchronize actual keyboard height with keyboardAnimationView without animation
        val current = getCurrentKeyboardHeight()
        CATransaction.begin()
        CATransaction.setValue(true, kCATransactionDisableActions)
        keyboardAnimationView.setFrame(CGRectMake(0.0, current, 0.0, 0.0))
        CATransaction.commit()

        //animation listener
        keyboardAnimationListener = CADisplayLink.displayLinkWithTarget(
            target = object : NSObject() {
                val bottomIndent: CGFloat

                init {
                    val screenHeight = UIScreen.mainScreen.bounds.useContents { size.height }
                    val composeViewBottomY = UIScreen.mainScreen.coordinateSpace.convertPoint(
                        point = CGPointMake(0.0, view.frame.useContents { size.height }),
                        fromCoordinateSpace = view.coordinateSpace
                    ).useContents { y }
                    bottomIndent = screenHeight - composeViewBottomY
                }

                @Suppress("unused")
                @ObjCAction
                fun animationDidUpdate() {
                    val currentHeight = getCurrentKeyboardHeight()
                    if (bottomIndent < currentHeight) {
                        keyboardOverlapHeightState.value = (currentHeight - bottomIndent).toFloat()
                    }
                }
            },
            selector = sel_registerName("animationDidUpdate")
        ).apply {
            addToRunLoop(NSRunLoop.mainRunLoop(), NSDefaultRunLoopMode)
        }

        //start system animation with duration
        val duration = userInfo[UIKeyboardAnimationDurationUserInfoKey] as? Double ?: 0.0
        val toValue: CGFloat = if (isShow) {
            val keyboardInfo = userInfo[UIKeyboardFrameEndUserInfoKey] as NSValue
            keyboardInfo.CGRectValue().useContents { size.height }
        } else {
            0.0
        }
        UIView.animateWithDuration(
            duration = duration,
            animations = {
                //set final destination for animation
                keyboardAnimationView.setFrame(CGRectMake(0.0, toValue, 0.0, 0.0))
            },
            completion = { isFinished ->
                if (isFinished) {
                    keyboardAnimationListener?.invalidate()
                    keyboardAnimationListener = null
                    keyboardAnimationView.removeFromSuperview()
                } else {
                    //animation was canceled by other animation
                }
            }
        )
    }

    private fun calcFocusedLiftingY(
        composeSceneMediator: ComposeSceneMediator,
        focusedRect: DpRect,
        keyboardHeight: Double
    ): Double {
        val viewHeight = composeSceneMediator.getViewHeight()
        val hiddenPartOfFocusedElement: Double =
            keyboardHeight - viewHeight + focusedRect.bottom.value
        return if (hiddenPartOfFocusedElement > 0) {
            // If focused element is partially hidden by the keyboard, we need to lift it upper
            val focusedTopY = focusedRect.top.value
            val isFocusedElementRemainsVisible = hiddenPartOfFocusedElement < focusedTopY
            if (isFocusedElementRemainsVisible) {
                // We need to lift focused element to be fully visible
                hiddenPartOfFocusedElement
            } else {
                // In this case focused element height is bigger than remain part of the screen after showing the keyboard.
                // Top edge of focused element should be visible. Same logic on Android.
                maxOf(focusedTopY, 0f).toDouble()
            }
        } else {
            // Focused element is not hidden by the keyboard.
            0.0
        }
    }

    private fun updateViewBounds(offsetX: Double = 0.0, offsetY: Double = 0.0) {
        view.layer.setBounds(
            view.frame.useContents {
                CGRectMake(
                    x = offsetX,
                    y = offsetY,
                    width = size.width,
                    height = size.height
                )
            }
        )
    }
}
