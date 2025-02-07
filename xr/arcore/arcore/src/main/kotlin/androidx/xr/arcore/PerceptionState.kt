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

package androidx.xr.arcore

import kotlin.time.ComparableTimeMark

/**
 * Represents the state of ARCore for Jetpack XR at an specific point in time.
 *
 * Can be obtained from [CoreState.perceptionState].
 *
 * @property timeMark the time at which the state was computed.
 * @property trackables the trackables that are currently being tracked.
 * @property leftHand the left hand, or null when not supported by the current platform.
 * @property rightHand the right hand, or null when not supported by the current platform.
 */
public class PerceptionState
internal constructor(
    public val timeMark: ComparableTimeMark,
    public val trackables: Collection<Trackable<Trackable.State>>,
    public val leftHand: Hand?,
    public val rightHand: Hand?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PerceptionState) return false
        if (timeMark != other.timeMark) return false
        if (trackables != other.trackables) return false
        if (leftHand != other.leftHand) return false
        if (rightHand != other.rightHand) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timeMark.hashCode()
        result = 31 * result + trackables.hashCode()
        result = 31 * result + leftHand.hashCode()
        result = 31 * result + rightHand.hashCode()
        return result
    }
}
