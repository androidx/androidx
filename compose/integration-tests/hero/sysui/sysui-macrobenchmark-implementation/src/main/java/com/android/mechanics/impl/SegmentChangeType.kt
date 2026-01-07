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

package com.android.mechanics.impl

/**
 * Describes how the [currentSegment] is different from last frame's [lastSegment].
 *
 * This affects how the discontinuities are animated and [Guarantee]s applied.
 */
internal enum class SegmentChangeType {
    /**
     * The segment has the same key, this is considered equivalent.
     *
     * Only the [GuaranteeState] needs to be kept updated.
     */
    Same,

    /**
     * The segment's direction changed, however the min / max breakpoints remain the same: This is a
     * direction change within a segment.
     *
     * The delta between the mapping must be animated with the reset spring, and there is no
     * guarantee associated with the change.
     */
    SameOppositeDirection,

    /**
     * The segment and its direction change. This is a direction change that happened over a segment
     * boundary.
     *
     * The direction change might have happened outside the [lastSegment] already, since a segment
     * can't be exited at the entry side.
     */
    Direction,

    /**
     * The segment changed, due to the [currentInput] advancing in the [currentDirection], crossing
     * one or more breakpoints.
     *
     * The guarantees of all crossed breakpoints have to be applied. The [GuaranteeState] must be
     * reset, and a new [DiscontinuityAnimation] is started.
     */
    Traverse,

    /**
     * The spec was changed and added or removed the previous and/or current segment.
     *
     * The [MotionValue] does not have a semantic understanding of this change, hence the difference
     * output produced by the previous and current mapping are animated with the
     * [MotionSpec.resetSpring]
     */
    Spec,
}
