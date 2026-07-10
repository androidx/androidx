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

import androidx.xr.runtime.math.Matrix4
import androidx.xr.scenecore.MeshEntity
import androidx.xr.scenecore.testing.internal.FakeMeshEntity as InternalFakeMeshEntity

/**
 * A test data accessor for a [MeshEntity] to inspect and manipulate underlying fake data.
 *
 * In a test environment, each entity created via [MeshEntity.create] is backed by corresponding
 * fake data in the fake runtime. This class provides a way to bridge the public [MeshEntity] API
 * with its internal fake state.
 */
public class MeshEntityTester
internal constructor(
    private val rtEntity: InternalFakeMeshEntity,
    internal val meshEntity: MeshEntity,
) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [MeshEntity].
         *
         * This function provides a [MeshEntityTester] instance, which can be used to inspect and
         * manipulate its underlying data in the test environment.
         *
         * @param meshEntity The entity for which to retrieve the test data accessor.
         * @return A [MeshEntityTester] instance for the given entity.
         */
        internal fun create(meshEntity: MeshEntity): MeshEntityTester {
            return MeshEntityTester(
                @Suppress("DEPRECATION") (meshEntity.rtEntity as FakeMeshEntity).fakeInternal
                    as InternalFakeMeshEntity,
                meshEntity,
            )
        }
    }

    /**
     * A list of [Matrix4] objects representing the value set in [MeshEntity.setBoneTransforms]. The
     * order in the list corresponds to the bone indices. The number of transforms can be less than
     * [MeshEntity.boneCount], in which case only the provided bones are updated. Any extra
     * transforms beyond [MeshEntity.boneCount] will be ignored.
     */
    public val boneTransforms: List<Matrix4>
        get() = rtEntity.boneTransforms

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshEntityTester

        if (rtEntity != other.rtEntity) return false
        if (meshEntity != other.meshEntity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtEntity.hashCode()
        result = 31 * result + meshEntity.hashCode()
        return result
    }
}
