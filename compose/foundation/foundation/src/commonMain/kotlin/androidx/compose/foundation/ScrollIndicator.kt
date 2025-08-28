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

package androidx.compose.foundation

import androidx.annotation.IntRange
import androidx.compose.runtime.Stable

/**
 * Represents the state required to draw a scroll indicator (e.g., a scrollbar), which is typically
 * provided by a scrollable component.
 *
 * For scrollable components with a large number of items, such as lazy layouts, implementations may
 * need to provide a reasonable heuristic for properties like content size, as calculating the exact
 * value might be computationally expensive or impossible.
 */
@Stable
interface ScrollIndicatorState {
    /**
     * The current scroll offset of the content from the start, in pixels.
     *
     * For a vertical scrollable component, this is the Y offset. For a horizontal scrollable
     * component, this is the X offset.
     *
     * Implementations should return [Int.MAX_VALUE] if this value is not yet known.
     */
    @get:IntRange(from = 0) val scrollOffset: Int

    /**
     * The total size of the scrollable content, in pixels.
     *
     * For a vertical scrollable component, this is the total height of the content. For a
     * horizontal scrollable component, this is the total width of the content.
     *
     * Implementations should return [Int.MAX_VALUE] if this value is not yet known.
     */
    @get:IntRange(from = 0) val contentSize: Int

    /**
     * The size of the visible portion of the scrollable content, in pixels.
     *
     * For a vertical scrollable component, this is the height of the viewport. For a horizontal
     * scrollable component, this is the width of the viewport.
     *
     * Implementations should return [Int.MAX_VALUE] if this value is not yet known.
     */
    @get:IntRange(from = 0) val viewportSize: Int
}
