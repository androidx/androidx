/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.complications

import android.graphics.RectF
import androidx.wear.complications.data.ComplicationType

/**
 * ComplicationSlotBounds are defined by fractional screen space coordinates in unit-square [0..1].
 * These bounds will be subsequently clamped to the unit square and converted to screen space
 * coordinates. NB 0 and 1 are included in the unit square.
 *
 * One bound is expected per [ComplicationType] to allow [androidx.wear.watchface.ComplicationSlot]s
 * to change shape depending on the type.
 */
public class ComplicationSlotBounds(
    /** Per [ComplicationType] fractional unit-square screen space complication bounds. */
    public val perComplicationTypeBounds: Map<ComplicationType, RectF>
) {
    /**
     * Constructs a ComplicationSlotBounds where all complication types have the same screen space
     * unit-square bounds.
     */
    public constructor(bounds: RectF) : this(ComplicationType.values().associateWith { bounds })

    init {
        for (type in ComplicationType.values()) {
            require(perComplicationTypeBounds.containsKey(type)) {
                "Missing bounds for $type"
            }
        }
    }
}