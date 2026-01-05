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

package androidx.compose.ui

import androidx.compose.ui.awt.RenderSettings.SkiaSurface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.awt.ComposePanel

internal enum class LayerType {
    OnSameCanvas,
    OnComponent,

    /**
     * TODO known issues:
     *  - [Rendering issues on Linux](https://github.com/JetBrains/compose-multiplatform/issues/4437)
     *  - [Blinking when showing](https://github.com/JetBrains/compose-multiplatform/issues/4475)
     *  - [Resizing the parent window clips the dialog](https://github.com/JetBrains/compose-multiplatform/issues/4484)
     */
    OnWindow;

    companion object {
        fun parse(property: String?): LayerType {
            return when (property) {
                "COMPONENT" -> OnComponent
                "WINDOW" -> OnWindow
                else -> OnSameCanvas
            }
        }
    }
}

/**
 * The helper singleton object that provides the access to feature flags that
 * configure Compose behavior.
 */
internal object ComposeFeatureFlags {

    /**
     * Indicates how the layers will be created.
     * The default value is `OnSameCanvas`, implying that new layers
     * (such as for [Popup] and [Dialog]) are created within the initial canvas.
     */
    val layerType = FeatureFlag {
        LayerType.parse(System.getProperty("compose.layers.type"))
    }

    /**
     * Indicates whether [androidx.compose.ui.awt.ComposePanel] should use Swing graphics for rendering.
     * This prevents transitional rendering issues when panels are being shown, hidden or resized.
     * It also enables proper layering when combining Swing components and compose panels.
     * This variable has no effect when used with [androidx.compose.ui.awt.ComposeWindow] or
     * [androidx.compose.ui.awt.ComposeDialog].
     *
     * Note: This approach requires an additional copy from offscreen texture to Swing graphics
     * on each re-draw, which may result in some performance penalty (proportional to the size)
     * compared to [SkiaSurface].
     *
     * @see androidx.compose.ui.awt.RenderSettings.SwingGraphics
     */
    val useSwingGraphicsInComposePanel = FeatureFlag {
        System.getProperty("compose.swing.render.on.graphics").toBoolean()
    }

    /**
     * Indicates whether interop blending is enabled.
     * It allows drawing compose elements above interop and apply clip/shape modifiers to it.
     *
     * Known limitations:
     * - Works only with Metal, DirectX and offscreen rendering
     * - On DirectX, it cannot overlay another DirectX component (due to OS blending limitation)
     * - On macOS, render and event dispatching order differs. It means that interop view might
     *   catch the mouse event even if visually it renders below Compose content
     */
    val useInteropBlending = FeatureFlag {
        System.getProperty("compose.interop.blending").toBoolean()
    }

    /**
     * Whether to redispatch unconsumed mouse wheel events to the parent heavyweight component.
     * This allows any scrollable components beneath Compose to be scrolled.
     *
     * To configure this behavior per [ComposePanel], set
     * [ComposePanel.redispatchUnconsumedMouseWheelEvents].
     *
     * This flag will be removed in the future, and the default behavior will correspond to a value
     * of `true`.
     *
     * @see ComposePanel.redispatchUnconsumedMouseWheelEvents
     */
    val redispatchUnconsumedMouseWheelEvents = FeatureFlag {
        System.getProperty("compose.swing.redispatchMouseWheelEvents", "true").toBoolean()
    }
}


internal class FeatureFlag<T: Any>(defaultValueGetter: () -> T) {
    private val defaultValue: T by lazy(defaultValueGetter)
    private var override: T? = null
    val value: T
        get() = override ?: defaultValue

    inline fun withOverride(value: T, block: () -> Unit) {
        this.override = value
        try {
            block()
        } finally {
            this.override = null
        }
    }
}
