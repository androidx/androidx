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

package androidx.xr.compose.subspace.layout

/**
 * Interface holding the size and alignment lines of the measured layout, as well as the children
 * positioning logic.
 *
 * Based on [androidx.compose.ui.layout.MeasureResult].
 */
public interface MeasureResult {
    /** The measured width of the layout, in pixels. */
    public val width: Int

    /** The measured height of the layout, in pixels. */
    public val height: Int

    /** The measured depth of the layout, in pixels. */
    public val depth: Int

    /**
     * Used for positioning children. [Placeable.placeAt] should be called on children inside
     * [placeChildren]
     */
    public fun placeChildren(placementScope: Placeable.PlacementScope)
}
