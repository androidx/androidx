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

package androidx.xr.scenecore

/**
 * Specifies the icon that is rendered at the pointer's location on entities in the spatialized
 * scene.
 */
public class SpatialPointerIcon private constructor(private val value: Int) {

    public companion object {
        private const val NONE_VALUE = 0
        private const val DEFAULT_VALUE = 1
        private const val CIRCLE_VALUE = 2

        /**
         * Do not render an icon for the pointer; this option can be used to hide the pointer icon,
         * either because the client wants it to be invisible or to implement custom icon rendering.
         */
        @JvmField public val NONE: SpatialPointerIcon = SpatialPointerIcon(NONE_VALUE)

        /** Use the default pointer icon, as determined by the system. */
        @JvmField public val DEFAULT: SpatialPointerIcon = SpatialPointerIcon(DEFAULT_VALUE)

        /** Renders the icon for the pointer as a circle. */
        @JvmField public val CIRCLE: SpatialPointerIcon = SpatialPointerIcon(CIRCLE_VALUE)
    }
}
