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

/**
 * Provides information about the layout that is using a SnapFlingBehavior.
 * The provider should give the following information:
 * 1) Snapping offset: The next snap position offset.
 * 2) Approach offset: An optional offset to be consumed before snapping to a defined bound.
 *
 * In snapping, the approach offset and the snapping offset can be used to control how a snapping
 * animation will look in a given SnappingLayout. The complete snapping animation can be split
 * into 2 phases: Approach and Snapping. In the Approach phase, we'll use an animation to consume
 * all of the offset provided by [calculateApproachOffset], if [Float.NaN] is provided,
 * we'll naturally decay if possible. In the snapping phase, [SnapFlingBehavior] will use an
 * animation to consume all of the offset provided by [calculateSnappingOffset].
 */
interface SnapLayoutInfoProvider {

    /**
     * Calculate the distance to navigate before settling into the next snapping bound. If
     * Float.NaN (the default value) is returned and the velocity is high enough to decay,
     * [SnapFlingBehavior] will decay before snapping. If zero is specified, that means there won't
     * be an approach phase and there will only be snapping.
     *
     * @param initialVelocity The current fling movement velocity. You can use this to calculate a
     * velocity based offset.
     */
    fun calculateApproachOffset(initialVelocity: Float): Float = Float.NaN

    /**
     * Given a target placement in a layout, the snapping offset is the next snapping position
     * this layout can be placed in. The target placement should be in the direction of
     * [currentVelocity].
     *
     * @param currentVelocity The current fling movement velocity. This may change throughout the
     * fling animation.
     */
    fun calculateSnappingOffset(currentVelocity: Float): Float
}
