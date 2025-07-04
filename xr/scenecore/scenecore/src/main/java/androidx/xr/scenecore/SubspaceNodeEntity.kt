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
import androidx.xr.runtime.Session
import androidx.xr.runtime.SubspaceNodeHolder
import androidx.xr.runtime.internal.SubspaceNodeEntity as RtSubspaceNodeEntity
import androidx.xr.runtime.math.FloatSize3d
import com.google.androidxr.splitengine.SubspaceNode

/**
 * Represents an entity that manages a subspace node.
 *
 * <p>This class manages the pose and size of the subspace node enclosed by this entity, and allows
 * for Entity features (such as managing parents and children or attaching user input Components) to
 * be used with split-engine SubspaceNodes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SubspaceNodeEntity
private constructor(rtEntity: RtSubspaceNodeEntity, entityManager: EntityManager) :
    BaseEntity<RtSubspaceNodeEntity>(rtEntity, entityManager) {

    /** The size of the [SubspaceNodeEntity] in meters, in unscaled local space. */
    public var size: FloatSize3d
        get() = rtEntity.size.toFloatSize3d()
        set(value) {
            rtEntity.size = value.toRtDimensions()
        }

    public companion object {
        /**
         * Creates a [SubspaceNodeEntity] from a [SubspaceNode] with a given [FloatSize3d].
         *
         * @param session The [Session].
         * @param subspaceNode The [SubspaceNode] to create the [SubspaceNodeEntity] from.
         * @param size The initial [FloatSize3d] of the [SubspaceNodeEntity] in meters in unscaled
         *   local space.
         * @deprecated Use [create(session, subspaceNodeHolder, size)] instead.
         */
        @JvmStatic
        public fun create(
            session: Session,
            subspaceNode: SubspaceNode,
            size: FloatSize3d,
        ): SubspaceNodeEntity =
            create(session, SubspaceNodeHolder(subspaceNode, SubspaceNode::class.java), size)

        /**
         * Creates a [SubspaceNodeEntity] from a [SubspaceNodeHolder] with a given [FloatSize3d].
         *
         * @param session The [Session].
         * @param subspaceNodeHolder The [SubspaceNodeHolder] to create the [SubspaceNodeEntity]
         *   from.
         * @param size The initial [FloatSize3d] of the [SubspaceNodeEntity] in meters in unscaled
         *   local space.
         */
        @JvmStatic
        public fun create(
            session: Session,
            subspaceNodeHolder: SubspaceNodeHolder<*>,
            size: FloatSize3d,
        ): SubspaceNodeEntity =
            SubspaceNodeEntity(
                session.platformAdapter.createSubspaceNodeEntity(
                    subspaceNodeHolder,
                    size.toRtDimensions(),
                ),
                session.scene.entityManager,
            )
    }
}
