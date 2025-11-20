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

import androidx.annotation.RestrictTo

/** Coordinate spaces in which to apply transformation values. */
public class Space private constructor(private val name: String) {
    public companion object {
        /**
         * The coordinate space of an [Entity]'s parent, such that the child Entity's pose, scale,
         * etc., are expressed relative to the parent.
         */
        @JvmField public val PARENT: Space = Space("PARENT")

        /** The global coordinate space, at the root of the scene graph for the activity. */
        @JvmField public val ACTIVITY: Space = Space("ACTIVITY")

        /**
         * The global coordinate space, unscaled, at the root of the scene graph of the activity.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        // TODO - b/415320653: This will be removed, for now restrict it for internal use.
        @JvmField
        public val REAL_WORLD: Space = Space("REAL WORLD")
    }

    public override fun toString(): String = name
}
