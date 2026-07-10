/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.spec

/**
 * Handler to allow for custom segment-change logic.
 *
 * This handler is called whenever the new input (position or direction) does not match
 * [currentSegment] anymore (see [SegmentData.isValidForInput]).
 *
 * This is intended to implement custom effects on direction-change.
 *
 * Implementations can return:
 * 1. [currentSegment] to delay/suppress segment change.
 * 2. `null` to use the default segment lookup based on [newPosition] and [newDirection]
 * 3. manually looking up segments on this [MotionSpec]
 * 4. create a [SegmentData] that is not in the spec.
 */
typealias OnChangeSegmentHandler =
    MotionSpec.(
        currentSegment: SegmentData, newPosition: Float, newDirection: InputDirection,
    ) -> SegmentData?

/** Generic change segment handlers. */
object ChangeSegmentHandlers {
    /** Prevents direction changes, as long as the input is still valid on the current segment. */
    val PreventDirectionChangeWithinCurrentSegment: OnChangeSegmentHandler =
        { currentSegment, newInput, newDirection ->
            currentSegment.takeIf {
                // Keep the previous segment as long as the input moves in the opposite direction,
                // AND the input is strictly within the segment. Specifically, do not use
                // SegmentData#isValidForInput, since this assumes validity when exiting on the
                // entry side; with direction suppression, this assumption does not hold.
                newDirection != it.direction && newInput in it.range
            }
        }

    /**
     * When changing direction, modifies the mapping of the reverse segments so that the output
     * values
     *
     * at the min/max breakpoint are the same, yet the value at the direction change position maps
     * the current output value.
     */
    val DirectionChangePreservesCurrentValue: OnChangeSegmentHandler =
        { currentSegment, newInput, newDirection ->
            val nextSegment = segmentAtInput(newInput, newDirection)
            val minLimit = nextSegment.minBreakpoint.position
            val maxLimit = nextSegment.maxBreakpoint.position

            if (
                currentSegment.direction == newDirection ||
                    minLimit == newInput && newInput == maxLimit
            ) {
                nextSegment
            } else {
                val modifiedMapping =
                    LinearMappings.linearMappingWithPivot(
                        minLimit,
                        nextSegment.mapping.map(minLimit),
                        newInput,
                        currentSegment.mapping.map(newInput),
                        maxLimit,
                        nextSegment.mapping.map(maxLimit),
                    )
                nextSegment.copy(mapping = modifiedMapping)
            }
        }
}
