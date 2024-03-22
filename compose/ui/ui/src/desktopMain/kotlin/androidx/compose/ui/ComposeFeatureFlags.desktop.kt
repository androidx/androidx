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

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup

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
 * configure Compose behaviour.
 */
internal object ComposeFeatureFlags {

    /**
     * Indicates how the layers will be created.
     * The default value is `OnSameCanvas`, implying that new layers
     * (such as for [Popup] and [Dialog]) are created within the initial canvas.
     */
    val layerType: LayerType
        get() = LayerType.parse(System.getProperty("compose.layers.type"))

    /**
     * Indicates whether the Compose should use Swing graphics for rendering.
     * This prevents transitional rendering issues when panels are being shown, hidden, or resized.
     * It also enables proper layering when combining Swing components and compose panels.
     *
     * Please note that it requires additional copy from offscreen texture to Swing graphics,
     * so it has some performance penalty.
     */
    val useSwingGraphics: Boolean
        get() = System.getProperty("compose.swing.render.on.graphics").toBoolean()

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
    val useInteropBlending: Boolean
        get() = System.getProperty("compose.interop.blending").toBoolean()
}
