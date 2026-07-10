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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.ActivitySpace
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.testing.internal.FakeEntity as InternalFakeEntity

/**
 * A test-only accessor for [Entity] that enables direct manipulation and inspection of its internal
 * state.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EntityTester
internal constructor(private val rtEntity: InternalFakeEntity, internal val entity: Entity) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [Entity].
         *
         * This function provides a [EntityTester] instance, which can be used to inspect and
         * manipulate its underlying data in the test environment.
         *
         * @param entity The entity for which to retrieve the test data accessor.
         * @return A [EntityTester] instance for the given entity.
         */
        internal fun create(entity: Entity): EntityTester {
            return EntityTester(
                @Suppress("DEPRECATION") (entity.rtEntity as FakeEntity).fakeInternal
                    as InternalFakeEntity,
                entity,
            )
        }
    }

    /**
     * Sets the current [Pose] of this entity within the Activity Space.
     *
     * This pose represents the entity's position and orientation relative to the root of the
     * [ActivitySpace]. The value set here can be retrieved using `Entity.poseInActivitySpace`.
     *
     * @param pose The [Pose] to set, expressed in Activity Space coordinates.
     */
    public fun setPoseInActivitySpace(pose: Pose) {
        rtEntity.activitySpacePose = pose
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntityTester

        if (rtEntity != other.rtEntity) return false
        if (entity != other.entity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtEntity.hashCode()
        result = 31 * result + entity.hashCode()
        return result
    }
}
