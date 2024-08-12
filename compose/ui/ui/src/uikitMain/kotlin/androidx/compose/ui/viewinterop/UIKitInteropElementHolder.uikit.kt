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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toRect
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGRect

internal abstract class UIKitInteropElementHolder<T : InteropView>(
    factory: () -> T,
    interopContainer: InteropContainer,
    group: InteropWrappingView,
    isInteractive: Boolean,
    isNativeAccessibilityEnabled: Boolean,
    compositeKeyHash: Int,
) : TypedInteropViewHolder<T>(
    factory = factory,
    interopContainer = interopContainer,
    group = group,
    compositeKeyHash = compositeKeyHash,
    measurePolicy = MeasurePolicy { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {
            // No-op, no children are expected
            // TODO: attempt to calculate the size of the wrapped view using constraints
            //  and autolayout system if possible
            //  https://youtrack.jetbrains.com/issue/CMP-5873/iOS-investigate-intrinsic-sizing-of-interop-elements
        }
    },
    isInteractive = isInteractive,
    platformModifier = Modifier
        // Make the canvas transparent in that area to make the interop view behind visible
        .drawBehind {
            drawRect(
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )
        }
        .nativeAccessibility(isNativeAccessibilityEnabled, group)
) {

    private var currentUnclippedRect: IntRect? = null
    private var currentClippedRect: IntRect? = null
    private var currentUserComponentRect: IntRect? = null

    override fun layoutAccordingTo(layoutCoordinates: LayoutCoordinates) {
        val rootCoordinates = layoutCoordinates.findRootCoordinates()

        val unclippedRect = rootCoordinates
            .localBoundingBoxOf(
                sourceCoordinates = layoutCoordinates,
                clipBounds = false
            ).roundToIntRect()

        val clippedRect = rootCoordinates
            .localBoundingBoxOf(
                sourceCoordinates = layoutCoordinates,
                clipBounds = true
            ).roundToIntRect()

        if (currentUnclippedRect == unclippedRect && currentClippedRect == clippedRect) {
            return
        }

        // wrapping view itself is always using the clipped rect
        // don't issue a redundant update, if the clipped rect is the same
        if (clippedRect != currentClippedRect) {
            val groupFrame = clippedRect
                .toRect()
                .toDpRect(density)
                .asCGRect()

            container.scheduleUpdate {
                group.setFrame(groupFrame)
            }
        }

        // user component is always updated if the unclipped or clipped rect changes,
        // because it needs to be moved inside the clipping view to keep the frame
        // in window coordinates the same
        if (currentUnclippedRect != unclippedRect || currentClippedRect != clippedRect) {
            // offset to move the component to the correct position inside the wrapping view, so
            // its root space frame stays the same if the wrapping view is clipped

            val userComponentRect = IntRect(
                offset = unclippedRect.topLeft - clippedRect.topLeft,
                size = unclippedRect.size
            )

            // update the user component frame only if it changes
            if (userComponentRect != currentUserComponentRect) {
                currentUserComponentRect = userComponentRect

                val userComponentFrame =
                    userComponentRect
                        .toRect()
                        .toDpRect(density)
                        .asCGRect()

                container.scheduleUpdate {
                    setUserComponentFrame(userComponentFrame)
                }
            }
        }

        currentUnclippedRect = unclippedRect
        currentClippedRect = clippedRect

    }

    abstract fun setUserComponentFrame(rect: CValue<CGRect>)


    override fun dispatchToView(pointerEvent: PointerEvent) {
        // No-op, we can't dispatch events to UIView or UIViewController directly, see
        // [InteractionUIView] logic
    }

    /**
     * This logic is similar for both interop view and view controller holders.
     */
    override fun changeInteropViewIndex(root: InteropViewGroup, index: Int) {
        root.insertSubview(view = group, atIndex = index.toLong())
    }
}