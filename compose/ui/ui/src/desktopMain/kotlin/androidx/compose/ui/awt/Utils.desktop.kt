/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.awt

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.size
import androidx.compose.ui.util.fastRoundToInt
import java.awt.Component
import java.awt.EventQueue
import java.awt.Graphics
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JLayeredPane
import javax.swing.RootPaneContainer
import kotlin.math.ceil
import kotlin.math.floor
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs

internal fun Component.isParentOf(component: Component?): Boolean {
    var parent = component?.parent
    while (parent != null) {
        if (parent == this) {
            return true
        }
        parent = parent.parent
    }
    return false
}

internal fun toAwtRectangle(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    density: Float
): Rectangle {
    val rleft = floor(left / density).toInt()
    val rtop = floor(top / density).toInt()
    val rright = ceil(right / density).toInt()
    val rbottom = ceil(bottom / density).toInt()
    val rwidth = rright - rleft
    val rheight = rbottom - rtop
    return Rectangle(rleft, rtop, rwidth, rheight)
}

internal fun IntRect.toAwtRectangle(density: Density) = toAwtRectangle(
    left = left.toFloat(),
    top = top.toFloat(),
    right = right.toFloat(),
    bottom = bottom.toFloat(),
    density = density.density
)

internal fun DpRect.toAwtRectangleRounded(): Rectangle {
    val left = this.left.value.fastRoundToInt()
    val top = this.top.value.fastRoundToInt()
    val right = this.right.value.fastRoundToInt()
    val bottom = this.bottom.value.fastRoundToInt()
    return Rectangle(left, top, right - left, bottom - top)
}

/**
 * Returns a [java.awt.Rectangle] corresponding to this [DpRect], in the given density.
 *
 * The size of the rectangle is rounded up to the nearest integer.
 */
internal fun DpRect.toAwtRectangleSizeRoundedUp(): Rectangle {
    val left = this.left.value.fastRoundToInt()
    val top = this.top.value.fastRoundToInt()
    val size = this.size
    val width = ceil(size.width.value).toInt()
    val height = ceil(size.height.value).toInt()
    return Rectangle(left, top, width, height)
}

/**
 * Returns a [java.awt.Rectangle] corresponding to this [Rect], in the given density.
 *
 * The coordinates are rounded to the nearest integer.
 */
internal fun Rect.toAwtRectangleRounded(density: Density): Rectangle {
    val densityValue = density.density
    val left = (this.left / densityValue).fastRoundToInt()
    val top = (this.top / densityValue).fastRoundToInt()
    val right = (this.right / densityValue).fastRoundToInt()
    val bottom = (this.bottom / densityValue).fastRoundToInt()
    return Rectangle(left, top, right - left, bottom - top)
}

/**
 * Determines whether the window has a shadow on macOS.
 */
// See https://developer.apple.com/library/archive/technotes/tn2007/tn2196.html#WINDOW_SHADOW
internal var RootPaneContainer.hasMacOsShadow: Boolean
    // Delegated properties don't work for extensions https://youtrack.jetbrains.com/issue/KT-6643
    get() = rootPane.getClientProperty("Window.shadow") as? Boolean? ?: false
    set(value) { rootPane.putClientProperty("Window.shadow", value) }

/**
 * Windows makes clicks on transparent pixels fall through, but it doesn't work
 * with GPU accelerated rendering since this check requires having access to pixels from CPU.
 *
 * JVM doesn't allow override this behaviour with low-level windows methods, so hack this by filling
 * the background with an almost transparent color.
 * Based on tests, it doesn't affect resulting pixel color.
 */
internal open class JLayeredPaneWithTransparencyHack: JLayeredPane() {
    override fun paint(g: Graphics) {
        if (!isOpaque && UseTransparencyHack) {
            // Fill the background with an almost transparent color
            g.color = AlmostTransparent
            val r = g.clipBounds
            if (r != null) {
                g.fillRect(r.x, r.y, r.width, r.height)
            } else {
                g.fillRect(0, 0, width, height)
            }
        }

        super.paint(g)
    }

    private companion object {

        @JvmStatic
        val AlmostTransparent = java.awt.Color(0, 0, 0, 1)

        @JvmStatic
        private val UseTransparencyHack = hostOs == OS.Windows

    }
}

/**
 * A utility for running code on the event dispatching thread, making sure it is not queued more
 * than once.
 */
internal class DebouncingEdtExecutor {

    /**
     * Whether any code has been scheduled.
     */
    private val isScheduled = AtomicBoolean(false)

    /**
     * Calls [block] on the event dispatching thread.
     *
     * If the thread calling this function is the event dispatching thread, executes [block] and
     * cancels any previously scheduled blocks. Otherwise, if no block is currently scheduled,
     * schedules [block] to run event dispatching thread. If a block is already scheduled, does
     * nothing.
     *
     * Note that this utility is not intended to run or schedule multiple different blocks of code
     * at the same time, as only one block of code can be scheduled at a time.
     */
    fun runOrScheduleDebounced(block: () -> Unit) {
        if (EventQueue.isDispatchThread()) {
            isScheduled.set(false)
            block()
        } else if (!isScheduled.getAndSet(true)) {
            EventQueue.invokeLater {
                if (isScheduled.getAndSet(false)) {
                    block()
                }
            }
        }
    }
}