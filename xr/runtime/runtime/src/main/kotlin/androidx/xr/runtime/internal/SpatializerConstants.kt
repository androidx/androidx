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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo

/** Contains the constants used to spatialize audio in XR Runtime. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SpatializerConstants {
    public annotation class AmbisonicsOrder {
        public companion object {
            public const val AMBISONICS_ORDER_FIRST_ORDER: Int = 0
            public const val AMBISONICS_ORDER_SECOND_ORDER: Int = 1
            public const val AMBISONICS_ORDER_THIRD_ORDER: Int = 2
        }
    }

    public annotation class SourceType {
        public companion object {
            public const val SOURCE_TYPE_BYPASS: Int = 0
            public const val SOURCE_TYPE_POINT_SOURCE: Int = 1
            public const val SOURCE_TYPE_SOUND_FIELD: Int = 2
        }
    }

    public companion object {
        /** Specifies spatial rendering using First Order Ambisonics */
        public const val AMBISONICS_ORDER_FIRST_ORDER: Int = 0
        /** Specifies spatial rendering using Second Order Ambisonics */
        public const val AMBISONICS_ORDER_SECOND_ORDER: Int = 1
        /** Specifies spatial rendering using Third Order Ambisonics */
        public const val AMBISONICS_ORDER_THIRD_ORDER: Int = 2

        /** The sound source has not been spatialized with the Spatial Audio SDK. */
        public const val SOURCE_TYPE_BYPASS: Int = 0
        /** The sound source has been spatialized as a 3D point source. */
        public const val SOURCE_TYPE_POINT_SOURCE: Int = 1
        /** The sound source is an ambisonics sound field. */
        public const val SOURCE_TYPE_SOUND_FIELD: Int = 2
    }
}
