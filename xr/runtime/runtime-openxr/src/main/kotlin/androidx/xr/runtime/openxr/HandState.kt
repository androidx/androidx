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

package androidx.xr.runtime.openxr

import androidx.xr.runtime.math.Pose

/** Represents the current state of a [Hand] instance. */
public class HandState(
    internal val isActive: Boolean = false,
    internal val handJoints: List<Pose> = listOf(),
) {
    init {
        require(isActive == false || handJoints.isNotEmpty()) {
            "Hand joints cannot be empty if the hand is active."
        }
    }
}
