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

@file:Suppress("ExperimentalPropertyAnnotation")

package androidx.navigationevent

import androidx.compose.runtime.Immutable

/**
 * Provides contextual information about a navigation state (e.g., a screen or route).
 *
 * Implement this interface on objects that represent a specific state in your UI. A typical
 * implementation should be a data holder (such as a `data class`) that defines meaningful
 * structural equality (`equals`/`hashCode`). This ensures that navigation state comparisons, state
 * flows, and recompositions work as expected. Failing to provide proper equality can lead to
 * unnecessary recompositions or infinite update loops.
 *
 * Guidelines for implementors:
 * - Prefer `data class` to automatically provide equality and `toString`.
 * - Keep instances immutable for predictable behavior in state flows.
 * - Ensure equality reflects the logical identity of the navigation state.
 *
 * This allows you to associate custom, comparable data with a system navigation event emissions.
 */
@Immutable
public abstract class NavigationEventInfo {

    /**
     * Defines the title of the navigation destination.
     *
     * Host environments can use this value to represent the active destination (e.g., a web
     * browser's tab title).
     *
     * Follow these best practices:
     * - **Localization**: Localize or translate the value.
     * - **Resolution**: Resolve resource IDs to strings before returning.
     * - **Formatting**: Interpolate dynamic parameters directly (e.g., `"Details - $id"`).
     *
     * Defaults to `null`, meaning the title remains unchanged.
     */
    @ExperimentalNavigationEventApi public open val title: String? = null

    /**
     * Defines the URL or path representation of the navigation destination.
     *
     * Host environments can use this value to update their location display (e.g., a web browser's
     * address bar).
     *
     * For web environments, this supports:
     * - **Full URL** (e.g., `"https://example.com/home"`): must match the active origin.
     * - **Relative path** (e.g., `"/home"`): appends to the active origin.
     * - **Hash fragment** (e.g., `"#home"`): triggers a hash navigation.
     *
     * Defaults to `null`, meaning the location remains unchanged.
     */
    @ExperimentalNavigationEventApi public open val url: String? = null

    /**
     * A default used when no specific information is associated with a navigation event.
     *
     * This serves as a null object when context about the UI state is unavailable or not needed.
     */
    public object None : NavigationEventInfo()
}
