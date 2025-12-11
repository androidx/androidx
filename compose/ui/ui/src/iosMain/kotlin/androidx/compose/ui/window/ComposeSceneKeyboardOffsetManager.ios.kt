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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectGetMinY
import platform.CoreGraphics.CGRectIsEmpty
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionCurveEaseInOut
import platform.UIKit.UIViewAnimationOptions

internal class ComposeSceneKeyboardOffsetManager(
    private val view: UIView,
    private val keyboardOverlapHeightChanged: (Dp) -> Unit
) : KeyboardVisibilityObserver {
    private var isDisposed: Boolean = false

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
        check(!isDisposed) { "ComposeSceneKeyboardOffsetManager is already disposed" }
        isDisposed = true
        stop()
    }

    private val animationViews = mutableListOf<UIView>()

    private var keyboardAnimationListener: DisplayLinkListener? = null

    val isAnimating get() = keyboardAnimationListener != null

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
            currentFrame = KeyboardVisibilityListener.keyboardFrame,
            targetFrame = targetFrame,
            duration = duration,
            animationOptions = animationOptions
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

        val viewBottomIndent = run {
            val screenHeight = screen.bounds.useContents { size.height }
            val composeViewBottomY = screen.coordinateSpace.convertPoint(
                point = CGPointMake(0.0, view.frame.useContents { size.height }),
                fromCoordinateSpace = view.coordinateSpace
            ).useContents { y }
            screenHeight - composeViewBottomY + view.frame.useContents { origin.y }
        }

        animateKeyboard(
            previousKeyboardHeight = keyboardHeight(currentFrame),
            keyboardHeight = keyboardHeight(targetFrame),
            viewBottomIndent = viewBottomIndent,
            duration = duration,
            animationOptions = animationOptions
        )
    }

    private fun animateKeyboard(
        previousKeyboardHeight: Double,
        keyboardHeight: Double,
        viewBottomIndent: Double,
        duration: Double,
        animationOptions: UIViewAnimationOptions
    ) {
        UIView.performWithoutAnimation {
            animationViews.forEach {
                it.layer.removeAllAnimations()
                it.setFrame(CGRectZero.readValue())
                it.removeFromSuperview()
            }
        }
        keyboardAnimationListener?.invalidate()

        fun updateAnimationValues(progress: Double) {
            val currentHeight = previousKeyboardHeight +
                (keyboardHeight - previousKeyboardHeight) * progress
            keyboardOverlapHeightChanged(max(0.0, currentHeight - viewBottomIndent).dp)
        }

        if (previousKeyboardHeight == keyboardHeight) {
            updateAnimationValues(1.0)
            return
        }

        val animationView = UIView()
        view.addSubview(animationView)
        animationViews.add(animationView)

        // Animate view from 0 to [animationTargetSize] and normalize to animation progress with
        // range of [0..1] to follow UIKit animation curve values.
        val animationTargetSize = 1000.0
        val animationTargetFrame = CGRectMake(0.0, 0.0, 0.0, animationTargetSize)

        fun getCurrentAnimationProgress(): Double {
            val layer = animationView.layer.presentationLayer() ?: return 0.0
            return layer.frame.useContents { size.height / animationTargetSize }
        }

        val keyboardDisplayLink = DisplayLinkListener {
            updateAnimationValues(getCurrentAnimationProgress())
        }
        keyboardAnimationListener = keyboardDisplayLink

        UIView.animateWithDuration(
            duration = duration,
            delay = 0.0,
            options = animationOptions,
            animations = {
                animationView.setFrame(animationTargetFrame)
            },
            completion = { isFinished ->
                keyboardDisplayLink.invalidate()
                animationView.removeFromSuperview()
                if (isFinished) {
                    if (keyboardAnimationListener == keyboardDisplayLink) {
                        keyboardAnimationListener = null
                    }
                    updateAnimationValues(1.0)
                }
            }
        )
        keyboardDisplayLink.start()
    }
}
