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

package androidx.wear.protolayout.material3

import androidx.wear.protolayout.LayoutElementBuilders.ArcLine
import androidx.wear.protolayout.LayoutElementBuilders.DashedArcLine
import androidx.wear.protolayout.expression.VersionBuilders.VersionInfo

/** Helper functions for specifying whether some renderer features are available or not. */
internal object Versions {
    /**
     * Checks whether the renderer has support for asymmetrical corners, which is added in version
     * 1.202.
     */
    fun VersionInfo.hasExpandWithWeightSupport() = major > 1 || (major == 1 && minor >= 300)

    /**
     * Checks whether the renderer has support for
     * [androidx.wear.protolayout.LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE] which is added in
     * version 1.300.
     */
    fun VersionInfo.hasTextOverflowEllipsizeSupport() = major > 1 || (major == 1 && minor >= 300)

    /**
     * Checks whether the renderer has support for asymmetrical corners, which is added in version
     * 1.303.
     */
    fun VersionInfo.hasAsymmetricalCornersSupport() = major > 1 || (major == 1 && minor >= 303)

    /**
     * For renderer with version lower than 1.403, there is no [DashedArcLine] support. In this
     * case, the progress indicator will fallback to use [ArcLine]
     */
    fun VersionInfo.hasDashedArcLineSupport() = major > 1 || (major == 1 && minor >= 403)
}
