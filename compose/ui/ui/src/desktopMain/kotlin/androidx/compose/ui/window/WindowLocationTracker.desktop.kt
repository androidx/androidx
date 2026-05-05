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

package androidx.compose.ui.window

import java.awt.Dimension
import java.awt.GraphicsConfiguration
import java.awt.GraphicsDevice
import java.awt.Point
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

/**
 * Track the position of all opened windows and provide an appropriate location for newly created
 * windows.
 *
 * Needed to place windows in cascade, and on the same screen.
 *
 * Singleton because we have only the single platform.
 * We basically override the standard behavior of the window manager.
 */
internal object WindowLocationTracker {
    private val cascadeOffset = Point(48, 48)

    private var windowsOrderedByLastFocused = mutableSetOf<Window>()

    private val focusListener = object : WindowFocusListener {
        override fun windowGainedFocus(e: WindowEvent) {
            // put window on the top of the set
            windowsOrderedByLastFocused.remove(e.window)
            windowsOrderedByLastFocused.add(e.window)
        }

        override fun windowLostFocus(e: WindowEvent) = Unit
    }

    fun onWindowCreated(window: Window) {
        window.addWindowFocusListener(focusListener)
    }

    fun onWindowDisposed(window: Window) {
        window.removeWindowFocusListener(focusListener)
        windowsOrderedByLastFocused.remove(window)
    }

    val lastActiveGraphicsConfiguration: GraphicsConfiguration? get() =
        windowsOrderedByLastFocused.lastOrNull()?.graphicsConfiguration

    fun getCascadeLocationFor(window: Window): Point {
        return getCascadeLocationFor(
            graphicsDevice = window.graphicsConfiguration.device,
            windowSize = window.size
        )
    }

    fun getCascadeLocationFor(
        graphicsDevice: GraphicsDevice,
        windowSize: Dimension
    ): Point {
        val lastFocusedWindow = windowsOrderedByLastFocused.lastOrNull {
            it.graphicsConfiguration.device == graphicsDevice
        }

        val graphicsConfiguration = graphicsDevice.defaultConfiguration
        val screenBounds = graphicsConfiguration.bounds
        val screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration)
        val screenLeftTop = screenBounds.topLeft + Point(screenInsets.left, screenInsets.top)
        val screenBottomRight = screenBounds.bottomRight - Point(screenInsets.right, screenInsets.bottom)

        val lastLocation = lastFocusedWindow?.location ?: screenLeftTop
        var location = lastLocation + cascadeOffset
        val bottomRight = location + windowSize.bottomRight
        if (bottomRight.x > screenBottomRight.x || bottomRight.y > screenBottomRight.y) {
            location = screenLeftTop + cascadeOffset
        }
        return location
    }
}
