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

package androidx.xr.compose.spatial

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Defines standardized resting elevation levels for spatial UI elements. These levels provide a
 * consistent visual hierarchy.
 *
 * Elevation levels range from [Level0] (imperceptible, for smooth transitions) to [Level5] (highest
 * recommended).
 */
public object SpatialElevationLevel {

    /**
     * Default elevation for the overall activity or screen background. Usually no elevation. Value:
     * `0.dp`.
     */
    public val ActivityDefault: Dp = 0.dp

    /**
     * The base elevation, used for the main content panel. Visually indistinct from no elevation
     * (0.dp) but crucial for animation and transition purposes.
     */
    public val Level0: Dp = 0.1.dp

    /**
     * Elevation for secondary UI elements like orbiters that float above the main panel. Value:
     * `16.dp`.
     */
    public val Level1: Dp = 16.dp

    /**
     * Elevation for transient UI elements such as Menus (Dropdown, Autocomplete, Select) and
     * Orbiters Menus. Value: `24.dp`.
     */
    public val Level2: Dp = 24.dp

    /**
     * An intermediate elevation. No assigned to default components but available for custom
     * layering. Value: `32.dp`.
     */
    public val Level3: Dp = 32.dp

    /**
     * A higher intermediate elevation. Not assigned to default components but available for custom
     * layering. Value: `40.dp`.
     */
    public val Level4: Dp = 40.dp

    /**
     * The highest standard elevation, typically reserved for elements like `SpatialDialog`. Value:
     * `56.dp`.
     */
    public val Level5: Dp = 56.dp

    /**
     * Default elevation specifically for Dialogs, typically the highest. Mirrors [Level5]. Value:
     * `56.dp`.
     */
    public val DialogDefault: Dp = 56.dp
}
