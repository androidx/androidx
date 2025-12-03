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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.CompositeKeyHashCode
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import kotlin.js.js
import kotlin.math.ceil
import kotlin.math.floor
import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

internal abstract class WebInteropElementHolder<T : HTMLElement>(
    factory: () -> T,
    interopContainer: InteropContainer,
    private val interopWrapper: HTMLElement,
    compositeKeyHashCode: CompositeKeyHashCode,
) : TypedInteropViewHolder<T>(
    factory = factory,
    interopContainer = interopContainer,
    group = InteropViewGroup(interopWrapper),
    compositeKeyHashCode = compositeKeyHashCode,
) {
    constructor(
        factory: () -> T,
        interopContainer: InteropContainer,
        compositeKeyHashCode: CompositeKeyHashCode,
    ) : this(
        factory = factory,
        interopContainer = interopContainer,
        interopWrapper =
            (document.createElement("div") as HTMLDivElement)
                .apply {
                    style.position = "absolute"
                    // hide it until it's properly positioned,
                    // otherwise it can briefly flash at 0,0
                    toggleVisibility(this, isHidden = true)
                },
        compositeKeyHashCode = compositeKeyHashCode
    )

    private var isPositioned = false

    private var isHidden: Boolean = false

    protected abstract var userComponentRect: String

    override val measurePolicy: MeasurePolicy = MeasurePolicy { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {
            // No-op, no children are expected
        }
    }

    private fun Rect.round(density: Density): IntRect {
        val left = floor(left / density.density).toInt()
        val top = floor(top / density.density).toInt()
        val right = ceil(right / density.density).toInt()
        val bottom = ceil(bottom / density.density).toInt()

        return IntRect(left, top, right, bottom)
    }

    override fun layoutAccordingTo(layoutCoordinates: LayoutCoordinates) {
        val newPosition = layoutCoordinates.positionInWindow()

        val rootCoordinates = layoutCoordinates.findRootCoordinates()

        val unclippedRect = rootCoordinates
            .localBoundingBoxOf(layoutCoordinates, clipBounds = false)
            .round(density)

        val clippedRect = rootCoordinates
            .localBoundingBoxOf(layoutCoordinates, clipBounds = true)
            .round(density)

        // update the css properties only for visible interop views
        if (!clippedRect.isEmpty) {
            setSizeAndPosition(
                interopWrapper,
                newPosition.x.toDouble() / density.density,
                newPosition.y.toDouble() / density.density,
                unclippedRect.width,
                unclippedRect.height
            )
            updateClipPath(clippedRect, unclippedRect)
            if (!isPositioned) {
                isPositioned = true
                toggleVisibility(interopWrapper, isHidden = false)
            }
        } else if (!isHidden) {
            toggleVisibility(interopWrapper, isHidden = true)
            isHidden = true
        }
    }

    override fun changeInteropViewIndex(root: InteropViewGroup, index: Int) {
        val referenceNode = root.htmlElement.children.item(index)

        root.htmlElement.insertBefore(group.htmlElement, referenceNode)
    }

    private fun updateClipPath(clippedRect: IntRect, unclippedRect: IntRect) {
        if (interopWrapper.offsetWidth <= 0 || interopWrapper.offsetHeight <= 0) return

        val topClip = maxOf(clippedRect.top - unclippedRect.top, 0)
        val leftClip = maxOf(clippedRect.left - unclippedRect.left, 0)
        val bottomClip = maxOf(unclippedRect.bottom - clippedRect.bottom, 0)
        val rightClip = maxOf(unclippedRect.right - clippedRect.right, 0)

        val newHiddenState = topClip >= interopWrapper.offsetHeight.toFloat() || leftClip >= interopWrapper.offsetWidth.toFloat()

        if (newHiddenState != isHidden) {
             toggleVisibility(interopWrapper, newHiddenState)
            isHidden = newHiddenState
        }

        setClipPath(interopWrapper, topClip, rightClip, bottomClip, leftClip)
    }
}

private fun toggleVisibility(element: HTMLElement, isHidden: Boolean) {
    // language=javascript
    js("""
       element.style.visibility = isHidden ? "hidden" : "visible";
    """)
}

private fun setClipPath(element: HTMLElement, top: Int, right: Int, bottom: Int, left: Int) {
    // language=javascript
    js("""
       element.style.setProperty("clip-path", "inset(" + top + "px " + right + "px " + bottom + "px " + left + "px)"); 
    """)
}

private fun setSizeAndPosition(element: HTMLElement, left: Double, top: Double, width: Int, height: Int) {
    // language=javascript
    js(
        """
       element.style.left = "" + left + "px";
       element.style.top = "" + top + "px";
       element.style.width = "" + width + "px";
       element.style.height = "" + height + "px";
    """
    )
}