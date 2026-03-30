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

package androidx.camera.viewfinder.core

import androidx.annotation.RestrictTo

/** Information about a tap-to-focus action. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface TapToFocusInfo {
    /** Returns the state of the focus and metering action. */
    @get:FocusMeteringIntState public val status: Int
}
