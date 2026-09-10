/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.wear.ui.selection

import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.ui.selection.GlanceSurface

/** Standard Wear OS glanceable surfaces on which Glance Adaptive widgets can be placed. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class WearGlanceSurface(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override val tag: String
) : GlanceSurface {
    /** Wear OS active Tile carousel. */
    TILE("wear_tile"),

    /** Wear OS watch face complication slot. */
    COMPLICATION("wear_complication");

    public companion object {
        /** Wear OS active Tile carousel alias. */
        @JvmField public val WEAR_TILE: WearGlanceSurface = TILE

        /** Wear OS watch face complication slot alias. */
        @JvmField public val WEAR_COMPLICATION: WearGlanceSurface = COMPLICATION
    }
}
