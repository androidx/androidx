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

/** Constants representing the spatial capabilities of a [Scene]. */
public class SpatialCapability private constructor(private val name: String) {

    public companion object {
        /** The activity can spatialize itself by e.g. adding a spatial panel. */
        @JvmField public val SPATIAL_UI: SpatialCapability = SpatialCapability("UI")

        /** The activity can create 3D content. */
        @JvmField public val SPATIAL_3D_CONTENT: SpatialCapability = SpatialCapability("3D_CONTENT")

        /** The activity can enable or disable passthrough. */
        @JvmField
        public val PASSTHROUGH_CONTROL: SpatialCapability = SpatialCapability("PASSTHROUGH_CONTROL")

        /** The activity can set its own spatial environment. */
        @JvmField
        public val APP_ENVIRONMENT: SpatialCapability = SpatialCapability("APP_ENVIRONMENT")

        /** The activity can use spatial audio. */
        @JvmField public val SPATIAL_AUDIO: SpatialCapability = SpatialCapability("SPATIAL_AUDIO")

        /** The activity can spatially embed another activity. */
        @JvmField public val EMBED_ACTIVITY: SpatialCapability = SpatialCapability("EMBED_ACTIVITY")
    }

    override fun toString(): String = name
}
