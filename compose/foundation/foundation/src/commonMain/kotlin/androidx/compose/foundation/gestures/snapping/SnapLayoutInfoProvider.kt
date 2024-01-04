/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.gestures.snapping

import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * Provides information about the layout that is using a SnapFlingBehavior.
 * The provider should give the following information:
 * 1) Snapping offset: The next snap position offset.
 * 2) Approach offset: An offset to be consumed before snapping to a defined bound.
 *
 * In snapping, the approach offset and the snapping offset can be used to control how a snapping
 * animation will look in a given SnappingLayout. The complete snapping animation can be split
 * into 2 phases: Approach and Snapping. In the Approach phase, we'll use an animation to consume
 * all of the offset provided by [calculateApproachOffset]. In the snapping phase,
 * [SnapFlingBehavior] will use an animation to consume all of the offset
 * provided by [calculateSnappingOffset].
 */
@ExperimentalFoundationApi
interface SnapLayoutInfoProvider {

    /**
     * Calculate the distance to navigate before settling into the next snapping bound.
     *
     * @param initialVelocity The current fling movement velocity. You can use this tho calculate a
     * velocity based offset.
     */
    fun calculateApproachOffset(initialVelocity: Float): Float

    /**
     * Given a target placement in a layout, the snapping offset is the next snapping position
     * this layout can be placed in. If this is a short snapping, [currentVelocity] is guaranteed
     * to be 0.If it is a long snapping, this method  will be called
     * after [calculateApproachOffset].
     *
     * @param currentVelocity The current fling movement velocity. This may change throughout the
     * fling animation.
     */
    fun calculateSnappingOffset(currentVelocity: Float): Float
}
