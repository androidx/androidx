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

package androidx.wear.compose.material3.onehandedgesture

import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A state object used to coordinate visual feedback between a [oneHandedGesture] modifier and a
 * gesture indicator.
 *
 * Developers should set [isIndicatorActive] to `true` within the `onGestureAvailable` callback
 * provided by [oneHandedGesture] modifier to signal that an indication event has occurred. The
 * associated indicator observes this value to initiate its animation. Once the indicator completes
 * its animation sequence, it must set [isIndicatorActive] back to false.
 */
@Stable
public class OneHandedGestureIndicatorState @RememberInComposition constructor() {
    /**
     * Whether the gesture indicator associated with this state should be displayed.
     * - To show the indicator: Set this to `true` (typically within the `onGestureAvailable`
     *   callback provided by [oneHandedGesture] modifier).
     * - To reset the indicator: Indicators observing this value must set it back to false once they
     *   are no longer active, such as when their animation sequence has finished.
     */
    public var isIndicatorActive: Boolean by mutableStateOf(false)
}
