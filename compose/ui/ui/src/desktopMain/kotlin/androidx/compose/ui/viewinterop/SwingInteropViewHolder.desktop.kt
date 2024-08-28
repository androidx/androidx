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
import androidx.compose.ui.awt.InteropFocusSwitcher
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.awt.isFocusGainedHandledBySwingPanel
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.util.fastForEach
import java.awt.Component
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import kotlin.math.ceil
import kotlin.math.floor
import org.jetbrains.skiko.ClipRectangle

internal class SwingInteropViewHolder<T : Component>(
    factory: () -> T,
    container: InteropContainer,
    group: InteropViewGroup,
    focusSwitcher: InteropFocusSwitcher,
    compositeKeyHash: Int,
) : TypedInteropViewHolder<T>(
    factory,
    container,
    group,
    compositeKeyHash,
    MeasurePolicy { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {}
    }
), ClipRectangle {
    private var clipBounds: IntRect? = null

    val focusListener = object : FocusListener {
        override fun focusGained(e: FocusEvent) {
            if (e.isFocusGainedHandledBySwingPanel(group)) {
                when (e.cause) {
                    FocusEvent.Cause.TRAVERSAL_FORWARD -> focusSwitcher.moveForward()
                    FocusEvent.Cause.TRAVERSAL_BACKWARD -> focusSwitcher.moveBackward()
                    else -> Unit
                }
            }
        }

        override fun focusLost(e: FocusEvent) = Unit
    }

    override fun getInteropView(): InteropView =
        typedInteropView

    init {
        group.add(typedInteropView)

        platformModifier = Modifier
            .pointerInteropFilter(this)
            .drawBehind {
                // Clear interop area to make visible the component under our canvas.
                drawRect(
                    color = Color.Transparent,
                    blendMode = BlendMode.Clear
                )
            }
    }

    override fun layoutAccordingTo(layoutCoordinates: LayoutCoordinates) {
        val rootCoordinates = layoutCoordinates.findRootCoordinates()

        val clippedBounds = rootCoordinates
            .localBoundingBoxOf(layoutCoordinates, clipBounds = true)
            .round(density)

        val bounds = rootCoordinates
            .localBoundingBoxOf(layoutCoordinates, clipBounds = false)
            .round(density)

        clipBounds = clippedBounds // Clipping area for skia canvas

        // Swing clips children based on parent's bounds, so use our container for clipping
        container.scheduleUpdate {
            group.isVisible = !clippedBounds.isEmpty // Hide if it's fully clipped

            group.setBounds(
                /* x = */ clippedBounds.left,
                /* y = */ clippedBounds.top,
                /* width = */ clippedBounds.width,
                /* height = */ clippedBounds.height
            )

            // The real size and position should be based on not-clipped bounds
            typedInteropView.setBounds(
                /* x = */ bounds.left - clippedBounds.left, // Local position relative to container
                /* y = */ bounds.top - clippedBounds.top,
                /* width = */ bounds.width,
                /* height = */ bounds.height
            )
        }
    }

    override fun insertInteropView(root: InteropViewGroup, index: Int) {
        root.add(group, index)
        super.insertInteropView(root, index)
        container.root.addFocusListener(focusListener)
    }

    override fun changeInteropViewIndex(root: InteropViewGroup, index: Int) {
        root.setComponentZOrder(group, index)
    }

    override fun removeInteropView(root: InteropViewGroup) {
        root.remove(group)
        super.removeInteropView(root)
        container.root.removeFocusListener(focusListener)
    }

    override val x: Float
        get() = (clipBounds?.left ?: group.x).toFloat()
    override val y: Float
        get() = (clipBounds?.top ?: group.y).toFloat()
    override val width: Float
        get() = (clipBounds?.width ?: group.width).toFloat()
    override val height: Float
        get() = (clipBounds?.height ?: group.height).toFloat()

    override fun dispatchToView(pointerEvent: PointerEvent) {
        val e = pointerEvent.awtEventOrNull ?: return
        when (e.id) {
            // Do not redispatch Enter/Exit events since they are related exclusively
            // to original component.
            MouseEvent.MOUSE_ENTERED, MouseEvent.MOUSE_EXITED -> return
        }
        if (SwingUtilities.isDescendingFrom(e.component, group)) {
            // Do not redispatch the event if it originally from this interop view.
            return
        }
        val component = getDeepestComponentForEvent(e)
        if (component != null) {
            component.dispatchEvent(SwingUtilities.convertMouseEvent(e.component, e, component))
            pointerEvent.changes.fastForEach {
                it.consume()
            }
        }
    }

    private fun getDeepestComponentForEvent(event: MouseEvent): Component? {
        val point = SwingUtilities.convertPoint(
            /* source = */event.component,
            /* aPoint = */event.point,
            /* destination = */typedInteropView
        )
        return SwingUtilities.getDeepestComponentAt(typedInteropView, point.x, point.y)
    }
}

private fun Rect.round(density: Density): IntRect {
    val left = floor(left / density.density).toInt()
    val top = floor(top / density.density).toInt()
    val right = ceil(right / density.density).toInt()
    val bottom = ceil(bottom / density.density).toInt()
    return IntRect(left, top, right, bottom)
}