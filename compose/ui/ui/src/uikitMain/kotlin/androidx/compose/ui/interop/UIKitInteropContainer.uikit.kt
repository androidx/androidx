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

package androidx.compose.ui.interop

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.InteropContainer
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.TrackInteropModifierElement
import androidx.compose.ui.node.TrackInteropModifierNode
import androidx.compose.ui.node.countInteropComponentsBelow
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UIView

/**
 * Providing interop container as composition local, so [UIKitView]/[UIKitViewController] can use it
 * to add native views to the hierarchy.
 */
internal val LocalUIKitInteropContainer = staticCompositionLocalOf<UIKitInteropContainer> {
    error("UIKitInteropContainer not provided")
}

/**
 * A container that controls interop views/components.
 */
internal class UIKitInteropContainer(
    private val interopContext: UIKitInteropContext
): InteropContainer<UIView> {
    val containerView: UIView = UIKitInteropContainerView()
    override var rootModifier: TrackInteropModifierNode<UIView>? = null
    override var interopViews = mutableSetOf<UIView>()
        private set

    override fun placeInteropView(nativeView: UIView) = interopContext.deferAction {
        val index = countInteropComponentsBelow(nativeView)
        if (nativeView in interopViews) {
            // Place might be called multiple times
            nativeView.removeFromSuperview()
        } else {
            interopViews.add(nativeView)
        }
        containerView.insertSubview(nativeView, index.toLong())
    }

    override fun removeInteropView(nativeView: UIView) {
        nativeView.removeFromSuperview()
        interopViews.remove(nativeView)
    }
}

private class UIKitInteropContainerView: UIView(CGRectZero.readValue()) {
    /**
     * We used a simple solution to make only this view not touchable.
     * Another view added to this container will be touchable.
     */
    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? =
        super.hitTest(point, withEvent).takeIf {
            it != this
        }
}

/**
 * Modifier to track interop view inside [LayoutNode] hierarchy.
 *
 * @param view The [UIView] that matches the current node.
 */
internal fun Modifier.trackUIKitInterop(
    container: UIKitInteropContainer,
    view: UIView
): Modifier = this then TrackInteropModifierElement(
    container = container,
    nativeView = view
)
