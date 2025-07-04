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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Vector3

/**
 * Defines an intersection between a ray and the scene.
 *
 * This can be obtained by running [hitTest] or [hitTestAsync] on an [ActivityPose].
 *
 * @property hitPosition the [Vector3] position of the intersection between a ray and the Scene.
 *   This will be null if nothing was hit
 * @property surfaceNormal The normal of the surface of the entity that was hit. This will be null
 *   if nothing was hit
 * @property surfaceType the [HitTestSurfaceType] that was hit.
 * @property distance the distance from the origin to the hit location. If nothing was hit the
 *   distance will be POSITIVE_INFINITY.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class HitTestResult(
    public val hitPosition: Vector3?,
    public val surfaceNormal: Vector3?,
    @HitTestSurfaceTypeValue public val surfaceType: Int,
    public val distance: Float,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HitTestResult) return false

        if (surfaceType != other.surfaceType) return false
        if (hitPosition != other.hitPosition) return false
        if (surfaceNormal != other.surfaceNormal) return false
        if (distance != other.distance) return false
        return true
    }

    override fun hashCode(): Int {
        var result = surfaceType.hashCode()
        result = 31 * result + hitPosition.hashCode()
        result = 31 * result + surfaceNormal.hashCode()
        result = 31 * result + distance.hashCode()
        return result
    }

    @IntDef(
        HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN,
        HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE,
        HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_OBJECT,
    )
    @Retention(AnnotationRetention.SOURCE)
    @Suppress("PublicTypedef")
    public annotation class HitTestSurfaceTypeValue

    /** The type of the surface that was hit. */
    public object HitTestSurfaceType {
        /** The ray hit an unknown surface or did not hit anything */
        public const val HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN: Int = 0
        /** The ray hit a flat surface such as a PanelEntity. */
        public const val HIT_TEST_RESULT_SURFACE_TYPE_PLANE: Int = 1
        /** The ray hit an object that is not a flat surface such as a gltfEntity. */
        public const val HIT_TEST_RESULT_SURFACE_TYPE_OBJECT: Int = 2
    }
}
