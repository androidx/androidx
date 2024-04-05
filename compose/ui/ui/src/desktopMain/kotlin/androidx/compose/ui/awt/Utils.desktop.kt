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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import java.awt.Component
import java.awt.Rectangle
import javax.swing.JComponent
import kotlin.math.ceil
import kotlin.math.floor
import org.jetbrains.skiko.GraphicsApi
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

internal fun IntRect.toAwtRectangle(density: Density): Rectangle {
    val left = floor(left / density.density).toInt()
    val top = floor(top / density.density).toInt()
    val right = ceil(right / density.density).toInt()
    val bottom = ceil(bottom / density.density).toInt()
    val width = right - left
    val height = bottom - top
    return Rectangle(
        left, top, width, height
    )
}

internal fun Color.toAwtColor() = java.awt.Color(red, green, blue, alpha)

internal fun getTransparentWindowBackground(
    isWindowTransparent: Boolean,
    renderApi: GraphicsApi
): java.awt.Color? {
    /**
     * There is a hack inside skiko OpenGL and Software redrawers for Windows that makes current
     * window transparent without setting `background` to JDK's window. It's done by getting native
     * component parent and calling `DwmEnableBlurBehindWindow`.
     *
     * FIXME: Make OpenGL work inside transparent window (background == Color(0, 0, 0, 0)) without this hack.
     *
     * See `enableTransparentWindow` (skiko/src/awtMain/cpp/windows/window_util.cc)
     */
    val skikoTransparentWindowHack = hostOs == OS.Windows && renderApi != GraphicsApi.DIRECT3D
    return if (isWindowTransparent && !skikoTransparentWindowHack) java.awt.Color(0, 0, 0, 0) else null
}

internal fun JComponent.setTransparent(transparent: Boolean) {
    /*
     * Windows makes clicks on transparent pixels fall through, but it doesn't work
     * with GPU accelerated rendering since this check requires having access to pixels from CPU.
     *
     * JVM doesn't allow override this behaviour with low-level windows methods, so hack this in this way.
     * Based on tests, it doesn't affect resulting pixel color.
     *
     * Note: Do not set isOpaque = false for this container
     */
    if (transparent && hostOs == OS.Windows) {
        background = java.awt.Color(0, 0, 0, 1)
        isOpaque = true
    } else {
        background = null
        isOpaque = false
    }
}
