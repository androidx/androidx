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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics

/**
 * Properties that are used to configure the behavior of the interop view.
 *
 * @property interactionMode The strategy on how the touches are processed when the user interacts
 * with the interop view.
 *
 * @property isNativeAccessibilityEnabled Indicates whether a11y services should traverse the
 * native view itself, instead of parsing Compose semantics properties.
 *
 * @property placedAsOverlay determines whether the interop view should be placed above or below the
 * Metal drawing canvas.
 * When true, the interop view may have a transparent background and may also overlap other
 * Composables, including those placed on top of the interop view.
 * When false, the interop view will be placed below the Metal drawing canvas, with a transparent
 * cut-out in place of the interop view.
 * The default value is false.
 *
 * If this Composable is within a modifier chain that merges the semantics of its children (such as
 * `Modifier.clickable`), the merged subtree data will be ignored in favor of the native
 * UIAccessibility resolution for the interop view. For example, Compose Button containing
 * [UIKitView] will be invisible for accessibility services, only the interop view will be
 * accessible. In other words, this property taints the whole merged subtree with the native
 * accessibility resolution.
 *
 * To avoid this behavior, set [isNativeAccessibilityEnabled] to `false` and use custom
 * [Modifier.semantics] for `Button` to make the information associated with this view accessible.
 *
 * If there are multiple [UIKitView] or [UIKitViewController] with [isNativeAccessibilityEnabled]
 * set to `true` in the merged tree, only the first one will be accessible.
 *
 * Consider using a single [UIKitView] or [UIKitViewController] with multiple views inside it if you
 * need multiple natively accessible views.
 *
 * In general, [isNativeAccessibilityEnabled] set to `true` is not recommended to use unless
 * you need rich accessibility capabilities of the interop view (such as web views).
 * Consider using [Modifier.semantics] instead.
 *
 * @see Modifier.semantics
 */
@Immutable
class UIKitInteropProperties @ExperimentalComposeUiApi constructor(
    val interactionMode: UIKitInteropInteractionMode? = UIKitInteropInteractionMode.Cooperative(),
    val isNativeAccessibilityEnabled: Boolean = false,
    @property:ExperimentalComposeUiApi val placedAsOverlay: Boolean = false,
) {
    /**
     * Indicates whether the user can interact with the interop component.
     */
    val isInteractive: Boolean
        get() = interactionMode != null

    /**
     * Properties that are used to configure the behavior of the interop view.
     *
     * @param interactionMode The strategy on how the touches are processed when the user
     * interacts with the interop view.
     *
     * @param isNativeAccessibilityEnabled Indicates whether a11y services should traverse the
     * native view itself, instead of parsing Compose semantics properties.
     */
    constructor(
        interactionMode: UIKitInteropInteractionMode? = UIKitInteropInteractionMode.Cooperative(),
        isNativeAccessibilityEnabled: Boolean = false,
    ) : this(
        interactionMode = interactionMode,
        isNativeAccessibilityEnabled,
        placedAsOverlay = false,
    )

    /**
     * @param isInteractive Indicates whether the affected interop component should process touches
     * or not. If true, the default strategy with delay will be used. Otherwise, user interaction is
     * disabled for it. See [UIKitInteropInteractionMode.Cooperative].
     *
     * @param isNativeAccessibilityEnabled Indicates whether native acessibility resolution is
     * enabled for the interop component. For more details see [UIKitInteropProperties].
     */
    constructor(
        isInteractive: Boolean,
        isNativeAccessibilityEnabled: Boolean
    ) : this(
        interactionMode = if (isInteractive) UIKitInteropInteractionMode.Cooperative() else null,
        isNativeAccessibilityEnabled,
        placedAsOverlay = false,
    )

    internal companion object {
        /**
         * Default configuration.
         * - View receives touches with 150ms delay, allowing compose to intercept them.
         * - Native accessibility resolution is disabled
         */
        internal val Default = UIKitInteropProperties()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UIKitInteropProperties) return false

        if (interactionMode != other.interactionMode) return false
        if (isNativeAccessibilityEnabled != other.isNativeAccessibilityEnabled) return false
        if (placedAsOverlay != other.placedAsOverlay) return false

        return true
    }

    override fun hashCode(): Int {
        var result = interactionMode.hashCode()
        result = 31 * result + isNativeAccessibilityEnabled.hashCode()
        result = 31 * result + placedAsOverlay.hashCode()
        return result
    }
}