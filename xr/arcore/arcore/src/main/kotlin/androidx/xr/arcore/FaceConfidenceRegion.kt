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

package androidx.xr.arcore

/** Represents the regions of face tracking confidence. */
public class FaceConfidenceRegion private constructor(private val value: Int) {
    public companion object {
        /** Represents the lower region of the face. */
        @JvmField
        public val FACE_CONFIDENCE_REGION_LOWER: FaceConfidenceRegion = FaceConfidenceRegion(0)

        /** Represents the left upper region of the face. */
        @JvmField
        public val FACE_CONFIDENCE_REGION_LEFT_UPPER: FaceConfidenceRegion = FaceConfidenceRegion(1)

        /** Represents the right upper region of the face. */
        @JvmField
        public val FACE_CONFIDENCE_REGION_RIGHT_UPPER: FaceConfidenceRegion =
            FaceConfidenceRegion(2)
    }

    public override fun toString(): String =
        when (value) {
            0 -> "LOWER"
            1 -> "LEFT_UPPER"
            2 -> "RIGHT_UPPER"
            else -> "UNKNOWN"
        }
}
