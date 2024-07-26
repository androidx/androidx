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

import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.util.fastForEach
import java.awt.Component
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import org.jetbrains.skiko.ClipRectangle

internal open class SwingInteropViewHolder(
    container: InteropContainer,
    group: InteropViewGroup,
) : InteropViewHolder(container, group), ClipRectangle {
    protected var clipBounds: IntRect? = null

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
        val userComponent = getInteropView()?.asAwtComponent ?: return null
        val point = SwingUtilities.convertPoint(event.component, event.point, userComponent)
        return SwingUtilities.getDeepestComponentAt(userComponent, point.x, point.y)
    }
}
