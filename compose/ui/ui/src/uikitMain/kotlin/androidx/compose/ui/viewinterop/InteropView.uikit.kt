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

package androidx.compose.ui.viewinterop

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.AccessibilityKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.uikit.utils.CMPInteropWrappingView
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.interop.UIKitViewController
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIResponder
import platform.UIKit.UIView
import platform.UIKit.UIViewController

/**
 * On iOS [InteropView] is a [UIResponder], which is a base class for [UIView] and [UIViewController]
 * that can be both created in interop API exposed on iOS.
 */
actual typealias InteropView = UIResponder

@Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-37316
internal actual typealias InteropViewGroup = UIView

/**
 * A [UIView] that contains underlying interop element, such as an independent [UIView]
 * or [UIViewController]'s root [UIView].
 *
 * Also contains [actualAccessibilityContainer] property that overrides default (superview)
 * accessibility container to allow proper traversal of a Compose semantics tree containing
 * interop views.
 *
 * @param areTouchesDelayed indicates whether the touches are allowed to be delayed by Compose
 * in attempt to intercept touches, or should get delivered to the interop view immediately without
 * Compose being aware of them.
 */
internal class InteropWrappingView(
    val areTouchesDelayed: Boolean
) : CMPInteropWrappingView(frame = CGRectZero.readValue()) {
    var actualAccessibilityContainer: Any? = null

    init {
        // required to properly clip the content of the wrapping view in case interop unclipped
        // bounds are larger than clipped bounds
        clipsToBounds = true
    }

    override fun accessibilityContainer(): Any? {
        return actualAccessibilityContainer
    }
}

internal val NativeAccessibilityViewSemanticsKey = AccessibilityKey<InteropWrappingView>(
    name = "NativeAccessibilityView",
    mergePolicy = { parentValue, childValue ->
        if (parentValue == null) {
            childValue
        } else {
            println(
                "Warning: Merging accessibility for multiple interop views is not supported. " +
                    "Multiple [UIKitView] are grouped under one node that should be represented as a single accessibility element." +
                    "It isn't recommended because the accessibility system can only recognize the first one. " +
                    "If you need multiple native views for accessibility, make sure to place them inside a single [UIKitView]."
            )

            parentValue
        }
    }
)

private var SemanticsPropertyReceiver.nativeAccessibilityView by NativeAccessibilityViewSemanticsKey

// TODO: align "platform" vs "native" naming
/**
 * Chain [this] with [Modifier.semantics] that sets the [nativeAccessibilityView] of the node to
 * the [interopWrappingView] if [isEnabled] is true.
 * If [isEnabled] is false, [this] is returned as is.
 *
 * See [UIKitView] and [UIKitViewController] accessibility argument for description of effects introduced by this semantics.
 */
internal fun Modifier.nativeAccessibility(isEnabled: Boolean, interopWrappingView: InteropWrappingView) =
    if (isEnabled) {
        this.semantics { nativeAccessibilityView = interopWrappingView }
    } else {
        this
    }