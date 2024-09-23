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

package androidx.pdf.metrics

import androidx.annotation.RestrictTo
import androidx.pdf.data.Range

@RestrictTo(RestrictTo.Scope.LIBRARY)
public enum class EventState {
    NEW_ASSETS_LOADED,
    ZOOM_CHANGED,
    NO_EVENT
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
public data class PositionState(val stateChanged: EventState, val visiblePages: Range)
