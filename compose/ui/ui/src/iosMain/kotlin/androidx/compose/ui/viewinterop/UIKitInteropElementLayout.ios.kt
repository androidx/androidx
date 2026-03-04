/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UILayoutFittingCompressedSize
import platform.UIKit.UILayoutPriorityRequired
import platform.UIKit.UIView

/**
 * UIKit-side layout implementation for a Compose interop element.
 *
 * **Hierarchy**:
 *  - [group] is the clipping viewport (clipsToBounds = true on [InteropWrappingView]).
 *    It is positioned by setting its frame to the *clipped* rect from Compose. [group] is placed to hierarchy
 *    in [InteropViewHolder.insertInteropView].
 *  - [userComponentHostView] is the "unclipped content container". It is positioned by setting
 *    its frame to the *unclipped* rect relative to [group].
 *  - [userComponent] is pinned to the edges of [userComponentHostView] with [NSLayoutConstraint]s.
 *
 * This design allows the interop view to keep a stable window position while the visible area is
 * clipped by Compose, without requiring frequent Auto Layout constraint updates
 * for scrolling/positioning.
 *
 * **Important**:
 *  - [group] and [userComponentHostView] are frame driven (translatesAutoresizingMaskIntoConstraints = true).
 *  - [userComponent] is Auto Layout–driven inside the host (translatesAutoresizingMaskIntoConstraints = false).
 *
 * @param group clipping viewport that wraps the [userComponent]
 * @param userComponent actual UIKit interop view being embedded
 */
internal class UIKitInteropElementLayout(
    private val group: InteropViewGroup,
    private val userComponent: UIView,
) {
    /**
     * Frame-driven container that defines the "unclipped content bounds".
     * The [userComponent] is constrained to fill this host using Auto Layout.
     */
    private val userComponentHostView = UIView(frame = CGRectZero.readValue())
        .also {
            it.translatesAutoresizingMaskIntoConstraints = true
            it.backgroundColor = null
        }

    val measurePolicy = object : MeasurePolicy {
        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureResult {
            if (constraints.hasFixedWidth && constraints.hasFixedHeight) {
                return layout(constraints.maxWidth, constraints.maxHeight) {}
            }

            val minW = constraints.minWidth.toDp()
            val minH = constraints.minHeight.toDp()
            val maxW = constraints.maxWidth.toDp()
            val maxH = constraints.maxHeight.toDp()

            val fixedW = if (constraints.hasFixedWidth) minW else null
            val fixedH = if (constraints.hasFixedHeight) minH else null

            val measuredSize = userComponentHostView.measureFittingSize(
                fixedWidth = fixedW,
                fixedHeight = fixedH,
                minWidth = minW,
                minHeight = minH,
                maxWidth = maxW,
                maxHeight = maxH,
            )

            return layout(measuredSize.width.roundToPx(), measuredSize.height.roundToPx()) {}
        }
    }

    /**
     * Attaches [userComponent] into [group] once and installs edge pinning constraints.
     *
     * Note: takes ownership of `translatesAutoresizingMaskIntoConstraints`:
     *  - [group] and [userComponentHostView] are frame-driven
     *  - [userComponent] is Auto Layout–driven inside the host
     */
    fun attachUserComponent() {
        if (userComponentHostView.superview != null) return
        if (userComponent.superview == userComponentHostView) return

        userComponentHostView.addSubview(userComponent)
        group.addSubview(userComponentHostView)

        group.translatesAutoresizingMaskIntoConstraints = true
        userComponent.translatesAutoresizingMaskIntoConstraints = false

        NSLayoutConstraint.activateConstraints(
            listOf(
                userComponent.leftAnchor.constraintEqualToAnchor(userComponentHostView.leftAnchor),
                userComponent.rightAnchor.constraintEqualToAnchor(userComponentHostView.rightAnchor),
                userComponent.topAnchor.constraintEqualToAnchor(userComponentHostView.topAnchor),
                userComponent.bottomAnchor.constraintEqualToAnchor(userComponentHostView.bottomAnchor),
            )
        )
    }

    /**
     * Sets the frame of the clipping viewport ([group]) in its superview coordinate space (managed by [UIKitInteropContainer]).
     * This rect corresponds to the clipped visible bounds of the element in Compose.
     */
    fun updateGroupFrame(rect: CValue<CGRect>) {
        group.setFrame(rect)
    }

    /**
     * Sets the frame of the unclipped content host inside [group]'s coordinate space.
     * The origin is typically negative when the element is partially clipped.
     */
    fun updateUserComponentFrame(rect: CValue<CGRect>) {
        userComponentHostView.setFrame(rect)
    }
}

/**
 * Measures UIKit view's Auto Layout compressed fitting size under the given Dp size constraints.
 *
 * The measurement is performed by temporarily applying size constraints to bound the solve:
 *  - For fixed axes: `== fixed`
 *  - For wrap axes: `<= max`
 */
internal fun UIView.measureFittingSize(
    fixedWidth: Dp? = null,
    fixedHeight: Dp? = null,
    minWidth: Dp = 0.dp,
    minHeight: Dp = 0.dp,
    maxWidth: Dp,
    maxHeight: Dp,
): DpSize {
    val widthConstraint = if (fixedWidth != null) {
        widthAnchor.constraintEqualToConstant(fixedWidth.value.toDouble())
    } else {
        widthAnchor.constraintLessThanOrEqualToConstant(maxWidth.value.toDouble())
    }.apply {
        priority = UILayoutPriorityRequired
        active = true
    }

    val heightConstraint = if (fixedHeight != null) {
        heightAnchor.constraintEqualToConstant(fixedHeight.value.toDouble())
    } else {
        heightAnchor.constraintLessThanOrEqualToConstant(maxHeight.value.toDouble())
    }.apply {
        priority = UILayoutPriorityRequired
        active = true
    }

    return try {
        systemLayoutSizeFittingSize(UILayoutFittingCompressedSize.readValue())
            .useContents {
                DpSize(
                    width.dp.coerceIn(minWidth, maxWidth),
                    height.dp.coerceIn(minHeight, maxHeight)
                )
            }
            .let { if (it.width == 0.dp || it.height == 0.dp) DpSize.Zero else it }
    } finally {
        widthConstraint.active = false
        heightConstraint.active = false
    }
}