/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.pointerinput

import androidx.ui.core.Timestamp
import androidx.ui.core.PointerInputData

// TODO(shepshapard): PointerInputEvent probably doesn't need it's own Timestamp because each
// PointerInputData has a timestamp associated with it.  Going to wait to refactor sometime later
// after more things are set (and after things like API review) to avoid thrashing.
/**
 * The normalized data structure for pointer input event information that is taken in processed by
 * Crane (via the [PointerInputEventProcessor]).
 */
internal data class PointerInputEvent(
    val timestamp: Timestamp,
    val pointers: List<PointerInputEventData>
)

/**
 * Data that describes a particular pointer
 */
data class PointerInputEventData(
    val id: Int,
    val pointerInputData: PointerInputData
)
