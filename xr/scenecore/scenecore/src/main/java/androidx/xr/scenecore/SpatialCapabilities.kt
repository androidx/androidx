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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/** Representation of the spatial capabilities of the current session. */
public class SpatialCapabilities(@SpatialCapability private val capabilities: Int) {

    public companion object {
        /** The activity can spatialize itself by e.g. adding a spatial panel. */
        public const val SPATIAL_CAPABILITY_UI: Int = 1 shl 0

        /** The activity can create 3D contents. */
        public const val SPATIAL_CAPABILITY_3D_CONTENT: Int = 1 shl 1

        /** The activity can enable or disable passthrough. */
        public const val SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL: Int = 1 shl 2

        /** The activity can set its own environment. */
        public const val SPATIAL_CAPABILITY_APP_ENVIRONMENT: Int = 1 shl 3

        /** The activity can use spatial audio. */
        public const val SPATIAL_CAPABILITY_SPATIAL_AUDIO: Int = 1 shl 4

        /** The activity can spatially embed another activity. */
        public const val SPATIAL_CAPABILITY_EMBED_ACTIVITY: Int = 1 shl 5
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        flag = true,
        value =
            [
                SPATIAL_CAPABILITY_UI,
                SPATIAL_CAPABILITY_3D_CONTENT,
                SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL,
                SPATIAL_CAPABILITY_APP_ENVIRONMENT,
                SPATIAL_CAPABILITY_SPATIAL_AUDIO,
                SPATIAL_CAPABILITY_EMBED_ACTIVITY,
            ],
    )
    internal annotation class SpatialCapability

    public fun hasCapability(@SpatialCapability capability: Int): Boolean =
        (capabilities and capability) != 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialCapabilities) return false

        if (capabilities != other.capabilities) return false

        return true
    }

    override fun hashCode(): Int {
        return capabilities
    }
}
