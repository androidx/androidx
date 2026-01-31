/*
 * Copyright 2025 The Android Open Source Project
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

import kotlin.jvm.JvmField

/**
 * The object holding the **feature flags**.
 *
 * Developers can enable or disable experimental or migration-based features by modifying the
 * boolean values within this object. These flags are considered temporary. They are expected to be
 * removed and should not be relied upon as permanent configuration.
 *
 * **Usage:**
 *
 * ```
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         ComposeMaterial3Flags.someFeatureEnabled = true
 *         super.onCreate()
 *     }
 * }
 * ```
 */
@ExperimentalMaterial3Api
object ComposeMaterial3Flags {
    /**
     * When the flag is `true`, the [Checkbox] will use Material Design 3 styling, including updated
     * colors and container sizing. When `false`, it uses older Material Design 2 styling. See the
     * [Material Design 2 Checkboxes Specs](https://m2.material.io/components/checkboxes#specs) and
     * the [Material Design 3 Checkboxes Specs](https://m3.material.io/components/checkbox/specs).
     */
    @field:Suppress("MutableBareField") @JvmField var isCheckboxStylingFixEnabled: Boolean = false

    /**
     * When this flag is `true`, the [Snackbar] component will use an updated layout implementation
     * that correctly handles vertical alignment for multi-line text.
     */
    @field:Suppress("MutableBareField") @JvmField var isSnackbarStylingFixEnabled: Boolean = false

    /**
     * When this flag is true and a precision pointer is present, components are resized accordingly
     */
    @field:Suppress("MutableBareField")
    @JvmField
    var isPrecisionPointerComponentSizingEnabled: Boolean = false

    /**
     * This flag affects Material3 components that use
     * [androidx.compose.ui.Modifier.anchoredDraggable]. Those are: [BottomSheetScaffold],
     * [ModalBottomSheet], [SwipeToDismissBox] and [WideNavigationRail].
     *
     * When this flag is set to true, these components will require their internal offset to be
     * initialized during measurement before they are placed, throwing an exception if the offset
     * was not initialized in placement. When this flag is set to false, the component will not
     * throw an exception and won't place the content while their internal offset is not
     * initialized. The content will be placed as soon as the offset is initialized.
     *
     * This flag can be helpful if you are encountering a crash with an uninitialized offset (such
     * as https://issuetracker.google.com/issues/477038695) and will be removed when the associated
     * bugs are fixed.
     */
    @field:Suppress("MutableBareField")
    @JvmField
    var isAnchoredDraggableComponentsStrictOffsetCheckEnabled: Boolean = true

    /**
     * This flag affects Material3 components that use
     * [androidx.compose.ui.Modifier.anchoredDraggable]. Those are: [BottomSheetScaffold],
     * [ModalBottomSheet], [SwipeToDismissBox] and [WideNavigationRail].
     *
     * When this flag is set to true, these components will recalculate their anchor points when the
     * instance of their respective state changes and remeasure. When this flag is set to false, no
     * additional remeasure is performed when the state instance changes.
     */
    @field:Suppress("MutableBareField")
    @JvmField
    var isAnchoredDraggableComponentsInvalidationFixEnabled: Boolean = true
}
