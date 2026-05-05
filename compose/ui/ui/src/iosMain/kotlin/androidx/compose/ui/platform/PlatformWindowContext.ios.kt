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

package androidx.compose.ui.platform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.uikit.density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.toCGPoint
import androidx.compose.ui.unit.toDpOffset
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.roundToIntSize
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.SceneActiveStateListener
import kotlinx.cinterop.useContents
import platform.UIKit.UIView
import platform.UIKit.UIWindow

/**
 * Tracking a state of window.
 */
internal class PlatformWindowContext {
    private val _windowInfo = WindowInfoImpl()

    private val sceneFocusObserver = SceneActiveStateListener(getScene = { window?.windowScene }) {
        _windowInfo.isWindowFocused = it
    }

    val windowInfo: WindowInfo get() = _windowInfo

    /**
     * A window used to calculate the coordinate space and track the active state of the window
     * scene.
     */
    var window: UIWindow? = null
        set(value) {
            field = value
            _windowInfo.isWindowFocused = sceneFocusObserver.isSceneActive
            updateWindowContainerSize()
        }

    private var isAnimating = false
    fun prepareAndGetSizeTransitionAnimation(
        initialDpSize: DpSize,
        withProgress: suspend ((Float) -> Unit) -> Unit
    ): suspend () -> Unit {
        val windowContainer = window ?: return {}

        isAnimating = true
        val initialSize = initialDpSize.toSize(windowContainer.density).roundToIntSize()

        return Animation@{
            try {
                withProgress { progress ->
                    val windowContainer = window
                    if (windowContainer != null) {
                        val currentSize = windowContainer.bounds.toDpRect().size
                        val currentSizeInPx = currentSize.toSize(windowContainer.density).roundToIntSize()
                        _windowInfo.containerSize = lerp(
                            initialSize.toSize(),
                            currentSizeInPx.toSize(),
                            progress
                        ).roundToIntSize()
                        _windowInfo.containerDpSize = lerp(initialDpSize, currentSize, progress)
                    } else {
                        _windowInfo.containerSize = initialSize
                        _windowInfo.containerDpSize = initialDpSize
                    }
                }
            } finally {
                isAnimating = false
                updateWindowContainerSize()
            }
        }
    }

    fun updateWindowContainerSize() {
        if (isAnimating) return

        val windowContainer = window ?: return
        val size = windowContainer.bounds.toDpRect().size
        val sizeInPx = size.toSize(windowContainer.density).roundToIntSize()
        _windowInfo.containerSize = sizeInPx
        _windowInfo.containerDpSize = size
    }

    fun convertLocalToWindowPosition(container: UIView, localPosition: Offset): Offset {
        val windowContainer = window ?: return localPosition
        return convertPoint(
            point = localPosition,
            fromView = container,
            toView = windowContainer
        )
    }

    fun convertWindowToLocalPosition(container: UIView, positionInWindow: Offset): Offset {
        val windowContainer = window ?: return positionInWindow
        return convertPoint(
            point = positionInWindow,
            fromView = windowContainer,
            toView = container
        )
    }

    fun convertLocalToScreenPosition(container: UIView, localPosition: Offset): Offset {
        val nativeWindow =
            container.window ?: return convertLocalToWindowPosition(container, localPosition)
        val density = container.density
        val positionInNativeWindow = container.convertPoint(
            point = localPosition.toDpOffset(density).toCGPoint(),
            toView = nativeWindow,
        )
        val positionOnScreen = nativeWindow.convertPoint(
            point = positionInNativeWindow,
            toWindow = null
        )
        return positionOnScreen.useContents {
            toDpOffset().toOffset(density)
        }
    }

    fun convertScreenToLocalPosition(container: UIView, positionOnScreen: Offset): Offset {
        val nativeWindow =
            container.window ?: return convertWindowToLocalPosition(container, positionOnScreen)
        val density = container.density
        val positionInNativeWindow = nativeWindow.convertPoint(
            point = positionOnScreen.toDpOffset(density).toCGPoint(),
            fromWindow = null
        )
        val localPosition = container.convertPoint(
            point = positionInNativeWindow,
            fromView = nativeWindow,
        )
        return localPosition.useContents {
            toDpOffset().toOffset(density)
        }
    }

    private fun convertPoint(point: Offset, fromView: UIView, toView: UIView): Offset {
        return if (fromView != toView) {
            val density = fromView.density
            fromView.convertPoint(
                point = point.toDpOffset(density).toCGPoint(),
                toView = toView,
            ).useContents {
                toDpOffset().toOffset(density)
            }
        } else {
            point
        }
    }

    fun dispose() {
        window = null
        sceneFocusObserver.dispose()
    }
}
