/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.runtime

import androidx.annotation.RestrictTo

/** Represents the regions of face tracking confidence. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public enum class FaceConfidenceRegionType {
    /** Represents the lower region of the face. */
    LOWER,
    /** Represents the left upper region of the face. */
    LEFT_UPPER,
    /** Represents the right upper region of the face. */
    RIGHT_UPPER,
}
