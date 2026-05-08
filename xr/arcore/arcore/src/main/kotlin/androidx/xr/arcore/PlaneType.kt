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

package androidx.xr.arcore

/** A simple summary of the normal vector of a [Plane]. */
public class PlaneType private constructor(internal val value: Int) {
    public companion object {
        /** A horizontal plane facing upward (e.g. floor or tabletop). */
        @JvmField public val HORIZONTAL_UPWARD_FACING: PlaneType = PlaneType(0)

        /** A horizontal plane facing downward (e.g. a ceiling). */
        @JvmField public val HORIZONTAL_DOWNWARD_FACING: PlaneType = PlaneType(1)

        /** A vertical plane (e.g. a wall). */
        @JvmField public val VERTICAL: PlaneType = PlaneType(2)
    }
}
