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

package androidx.camera.camera2.pipe

import androidx.annotation.RestrictTo
import kotlinx.atomicfu.atomic

/**
 * Identifier for a specific [CameraGraph] that can be used to standardize toString methods and as a
 * key in maps without holding a reference to a [CameraGraph] object, which could lead to accidental
 * memory leaks and circular dependencies.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CameraGraphId private constructor(private val name: String) {
    override fun toString(): String = name

    public companion object {
        private val cameraGraphIds = atomic(0)

        /**
         * Create the next CameraGraphId based on a global incrementing counter. This is
         * intentionally worded as "CameraGraph" instead of "CameraGraphId" since it is used
         * directly as the toString representation for a [CameraGraph].
         */
        @JvmStatic
        public fun nextId(): CameraGraphId {
            return CameraGraphId("CameraGraph-${cameraGraphIds.incrementAndGet()}")
        }
    }
}
