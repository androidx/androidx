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
 * Provides information about the layout that is using a [snapFlingBehavior]. The provider should
 * give the following information:
 * 1) Snapping offset: The next snap position offset. This needs to be provided by the layout where
 *    the snapping is happened and will represent the final settling position of this layout.
 * 2) Approach offset: An offset to be consumed before snapping to a defined bound. If not
 *    overridden this will provide a decayed snapping behavior.
 *
 * In snapping, the approach offset and the snapping offset can be used to control how a snapping
 * animation will look in a given layout. The complete snapping animation can be split into 2
 * phases: approach and snapping.
 *
 * Approach: animate to the offset returned by [calculateApproachOffset]. This will use a decay
 * animation if possible, otherwise the snap animation. Snapping: once the approach offset is
 * reached, snap to the offset returned by [calculateSnapOffset] using the snap animation.
 */
interface SnapLayoutInfoProvider {

    /**
     * Calculate the distance to navigate before settling into the next snapping bound. By default
     * this is [decayOffset] a suggested offset given by [snapFlingBehavior] to indicate where the
     * animation would naturally decay if using [velocity]. Returning a value higher than
     * [decayOffset] is valid and will force [snapFlingBehavior] to use a target based animation
     * spec to run the approach phase since we won't be able to naturally decay to the proposed
     * offset. If a value smaller than or equal to [decayOffset] is returned [snapFlingBehavior]
     * will run a decay animation until it reaches the returned value. If zero is specified, that
     * means there won't be an approach phase and there will only be snapping.
     *
     * @param velocity The current fling movement velocity. You can use this to calculate a velocity
     *   based offset.
     * @param decayOffset A suggested offset indicating where the animation would naturally decay
     *   to.
     */
    fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float = decayOffset

    /**
     * Given a target placement in a layout, the snapping offset is the next snapping position this
     * layout can be placed in. The target placement should be in the direction of [velocity].
     *
     * @param velocity The current fling movement velocity. This may change throughout the fling
     *   animation.
     */
    fun calculateSnapOffset(velocity: Float): Float
}
