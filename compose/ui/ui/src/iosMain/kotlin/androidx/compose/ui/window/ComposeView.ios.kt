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

import androidx.compose.ui.unit.asDpSize
import kotlin.math.max
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectEqualToRect
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.UIKit.UIEvent
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIScreen
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewContentMode
import platform.UIKit.UIWindow

internal class ComposeView(
    private val useOpaqueConfiguration: Boolean,
    private val transparentForTouches: Boolean,
): UIView(frame = UIScreen.mainScreen.bounds) {
    init {
        setClipsToBounds(true)
        setOpaque(useOpaqueConfiguration)
        updateBackgroundColor()
    }

    private var metalView: MetalView? = null
    private var onDidMoveToWindow: (UIWindow?) -> Unit = {}
    private var onLayoutSubviews: () -> Unit = {}
    private var foregroundStateListener: SceneForegroundStateListener? = null

    val redrawer: MetalRedrawer? get() = metalView?.redrawer

    override fun canBecomeFirstResponder(): Boolean {
        return true
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)

        updateBackgroundColor()
    }

    private fun updateBackgroundColor() {
        backgroundColor = if (useOpaqueConfiguration) {
            when (traitCollection.userInterfaceStyle) {
                UIUserInterfaceStyle.UIUserInterfaceStyleDark -> UIColor.blackColor
                UIUserInterfaceStyle.UIUserInterfaceStyleLight -> UIColor.whiteColor
                else -> UIColor.whiteColor
            }
        } else {
            UIColor.clearColor
        }
    }

    fun updateMetalView(
        metalView: MetalView?,
        onDidMoveToWindow: (UIWindow?) -> Unit = {},
        onLayoutSubviews: () -> Unit = {}
    ) {
        this.metalView?.dispose()
        this.metalView?.removeFromSuperview()
        this.metalView = metalView

        this.onDidMoveToWindow = onDidMoveToWindow
        this.onLayoutSubviews = onLayoutSubviews

        metalView?.let {
            addSubview(metalView)
        }
        updateLayout()
        window?.let(onDidMoveToWindow)

        if (metalView == null) {
            foregroundStateListener?.dispose()
            foregroundStateListener = null
        } else {
            foregroundStateListener = SceneForegroundStateListener(getScene = {
                window?.windowScene
            }) { isSceneInForeground ->
                metalView.redrawer.isActive = isSceneInForeground
            }
        }
        updateRedrawerState()
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()

        onDidMoveToWindow(window)

        updateRedrawerState()

        // To avoid a situation where a user decided to call [layoutIfNeeded] on the detached view
        // using a certain frame and it will be attached to the window later, so there is a chance
        // that [onLayoutSubviews] will not be called when a [window] is set.
        setNeedsLayout()
    }

    private var isAnimating: Boolean = false

    override fun layoutSubviews() {
        performWithoutAnimation {
            super.layoutSubviews()
        }

        onLayoutSubviews()
        updateLayout()
    }

    override fun safeAreaInsetsDidChange() {
        super.safeAreaInsetsDidChange()

        setNeedsLayout()
    }

    private fun updateRedrawerState() {
        metalView?.redrawer?.isActive = foregroundStateListener?.isSceneInForeground ?: false
    }

    private fun updateLayout() {
        val metalView = metalView ?: return
        if (isAnimating) {
            val oldSize = metalView.frame.useContents { size.asDpSize() }
            val newSize = bounds.useContents { size.asDpSize() }
            val targetRect = CGRectMake(
                0.0,
                0.0,
                max(oldSize.width.value, newSize.width.value).toDouble(),
                max(oldSize.height.value, newSize.height.value).toDouble()
            )
            if (!CGRectEqualToRect(metalView.frame, targetRect)) {
                performWithoutAnimation {
                    metalView.setFrame(targetRect)
                    metalView.setNeedsSynchronousDrawOnNextLayout()
                }
            }
        } else {
            if (!CGRectEqualToRect(metalView.frame, bounds)) {
                performWithoutAnimation {
                    metalView.setFrame(bounds)
                    metalView.setNeedsSynchronousDrawOnNextLayout()
                }
            }
        }
    }

    fun animateCrossFadeTransition(scope: CoroutineScope): () -> Unit {
        val image = viewContentImage()
        val imageView = UIImageView(frame = bounds).also(::addSubview)
        imageView.image = image
        imageView.setContentMode(UIViewContentMode.UIViewContentModeScaleToFill)

        setClipsToBounds(false)

        scope.launch {
            try {
                awaitCancellation()
            } finally {
                setClipsToBounds(true)
                imageView.removeFromSuperview()
            }
        }

        return {
            imageView.alpha = 0.0
        }
    }

    fun animateSizeTransition(scope: CoroutineScope, animations: suspend () -> Unit) {
        val metalView = metalView ?: return
        isAnimating = true
        updateLayout()
        metalView.redrawer.isForcedToPresentWithTransactionEveryFrame = true
        metalView.redrawer.ongoingInteractionEventsCount++
        scope.launch {
            try {
                animations()
            } finally {
                isAnimating = false
                updateLayout()
                metalView.layoutIfNeeded()
                metalView.redrawer.isForcedToPresentWithTransactionEveryFrame = false
                metalView.redrawer.ongoingInteractionEventsCount--
            }
        }
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return super.hitTest(point, withEvent).takeUnless { transparentForTouches && it == this }
    }

    private fun viewContentImage(): UIImage {
        val renderer = UIGraphicsImageRenderer(bounds = bounds)
        return renderer.imageWithActions {
            this.drawViewHierarchyInRect(bounds, false)
        }
    }
}
