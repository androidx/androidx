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

package androidx.compose.material3

import android.graphics.Rect as ViewRect
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.compose.material3.internal.rememberAccessibilityServiceState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toComposeIntRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.window.PopupProperties

internal actual class WindowBoundsCalculator(private val view: View) {
    actual fun getVisibleWindowBounds(): IntRect = view.getWindowBounds()
}

@Composable
internal actual fun platformWindowBoundsCalculator(): WindowBoundsCalculator {
    val config = LocalConfiguration.current
    val view = LocalView.current
    return remember(config, view) { WindowBoundsCalculator(view) }
}

@Composable
internal actual fun OnPlatformWindowBoundsChange(block: () -> Unit) {
    val view = LocalView.current
    val density = LocalDensity.current
    SoftKeyboardListener(view, density, block)
}

@Composable
internal actual fun popupPropertiesForAnchorType(
    anchorType: ExposedDropdownMenuAnchorType,
    alwaysFocusable: Boolean,
): PopupProperties {
    val a11yServicesEnabled by rememberAccessibilityServiceState()
    var flags =
        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM

    // In order for a11y focus to jump to the menu when opened, it needs to be
    // focusable and touch modal (NOT_FOCUSABLE and NOT_TOUCH_MODAL are *not* set).
    if (!a11yServicesEnabled) {
        flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
    }
    // If typing on the IME is required, the menu should not be focusable
    // in order to prevent stealing focus from the input method.
    val imeRequired =
        anchorType == ExposedDropdownMenuAnchorType.PrimaryEditable ||
            (anchorType == ExposedDropdownMenuAnchorType.SecondaryEditable && !a11yServicesEnabled)
    if (imeRequired && !alwaysFocusable) {
        flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    }

    return PopupProperties(flags = flags)
}

@Composable
private fun SoftKeyboardListener(
    view: View,
    density: Density,
    onKeyboardVisibilityChange: () -> Unit,
) {
    // It would be easier to listen to WindowInsets.ime, but that doesn't work with
    // `setDecorFitsSystemWindows(window, true)`. Instead, listen to the view tree's global layout.
    DisposableEffect(view, density) {
        val listener =
            object : View.OnAttachStateChangeListener, ViewTreeObserver.OnGlobalLayoutListener {
                private var isListeningToGlobalLayout = false

                init {
                    view.addOnAttachStateChangeListener(this)
                    registerOnGlobalLayoutListener()
                }

                override fun onViewAttachedToWindow(p0: View) = registerOnGlobalLayoutListener()

                override fun onViewDetachedFromWindow(p0: View) = unregisterOnGlobalLayoutListener()

                override fun onGlobalLayout() = onKeyboardVisibilityChange()

                private fun registerOnGlobalLayoutListener() {
                    if (isListeningToGlobalLayout || !view.isAttachedToWindow) return
                    view.viewTreeObserver.addOnGlobalLayoutListener(this)
                    isListeningToGlobalLayout = true
                }

                private fun unregisterOnGlobalLayoutListener() {
                    if (!isListeningToGlobalLayout) return
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    isListeningToGlobalLayout = false
                }

                fun dispose() {
                    unregisterOnGlobalLayoutListener()
                    view.removeOnAttachStateChangeListener(this)
                }
            }

        onDispose { listener.dispose() }
    }
}

private fun View.getWindowBounds(): IntRect =
    ViewRect().let {
        this.getWindowVisibleDisplayFrame(it)
        it.toComposeIntRect()
    }
