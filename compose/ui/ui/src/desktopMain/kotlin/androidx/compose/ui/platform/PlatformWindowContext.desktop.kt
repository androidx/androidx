/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.density
import androidx.compose.ui.window.sizeInPx
import androidx.compose.ui.window.toDpOffset
import androidx.compose.ui.window.toDpSize
import java.awt.Component
import java.awt.Container
import java.awt.Frame
import java.awt.Point
import javax.swing.SwingUtilities
import javax.swing.SwingUtilities.isEventDispatchThread

/**
 * Tracking a state of window.
 */
internal class PlatformWindowContext {
    private val _windowInfo = WindowInfoImpl()
    val windowInfo: WindowInfo get() = _windowInfo

    private var _windowContainer: Container? = null

    /**
     * Indicates whether the window is transparent or not.
     * Used for determining the right blending mode for [Dialog]'s scrim.
     */
    var isWindowTransparent: Boolean by mutableStateOf(false)

    fun setKeyboardModifiers(modifiers: PointerKeyboardModifiers) {
        _windowInfo.keyboardModifiers = modifiers
    }

    fun setWindowFocused(focused: Boolean) {
        _windowInfo.isWindowFocused = focused
    }

    fun setWindowContainer(windowContainer: Container) {
        _windowContainer = windowContainer
    }

    fun setContainerSizeFromComponent(component: Component) {
        _windowInfo.containerSize = component.sizeInPx.roundToIntSize()
        _windowInfo.containerDpSize = component.size.toDpSize()
    }

    private inline fun withOffsetInWindowOrUnspecified(
        component: Component,
        block: (offsetInWindow: Offset) -> Offset
    ): Offset {
        if (_windowContainer == null || SwingUtilities.getWindowAncestor(component) == null) {
            return Offset.Unspecified
        }
        return block(locationInWindow(component).toDpOffset().toOffset(component.density))
    }

    fun convertLocalToWindowPosition(container: Component, localPosition: Offset): Offset {
        return withOffsetInWindowOrUnspecified(container) {
            localPosition + it
        }
    }

    fun convertWindowToLocalPosition(container: Component, positionInWindow: Offset): Offset {
        return withOffsetInWindowOrUnspecified(container) {
            positionInWindow - it
        }
    }

    /**
     * If the [component]'s location on screen is undefined, returns [Offset.Unspecified]; otherwise
     * passes its location on screen to [block] and returns its return value.
     */
    private inline fun withLocationOnScreenOrUnspecified(
        component: Component,
        block: (locationOnScreen: Offset) -> Offset
    ): Offset {
        check(isEventDispatchThread()) {
            "Swing hierarchy should be accessed only from AWT Event Dispatch Thread"
        }
        if (!component.isShowing) return Offset.Unspecified

        val frame = SwingUtilities.getWindowAncestor(component) as? Frame
        if ((frame != null) && (frame.state == Frame.ICONIFIED)) return Offset.Unspecified

        return block(component.locationOnScreen.toDpOffset().toOffset(component.density))
    }

    fun convertLocalToScreenPosition(container: Component, localPosition: Offset): Offset {
        return withLocationOnScreenOrUnspecified(container) {
            localPosition + it
        }
    }

    fun convertScreenToLocalPosition(container: Component, locationOnScreen: Offset): Offset {
        return withLocationOnScreenOrUnspecified(container) {
            locationOnScreen - it
        }
    }

    /**
     * Calculates the offset of the given [component] within the window.
     * It uses [_windowContainer] as a reference for window coordinate space.
     *
     * @param component The component whose offset needs to be calculated.
     * @return The offset of the component within the window as an [Point] object.
     */
    fun locationInWindow(component: Component): Point {
        if (_windowContainer == null) return Point(0, 0)
        return  SwingUtilities.convertPoint(
            /* source = */ component,
            /* aPoint = */ Point(0, 0),
            /* destination = */ _windowContainer,
        )
    }
}
