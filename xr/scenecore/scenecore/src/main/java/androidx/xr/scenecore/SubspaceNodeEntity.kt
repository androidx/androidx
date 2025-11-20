/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.runtime.SubspaceNodeEntity as RtSubspaceNodeEntity
import androidx.xr.scenecore.spatial.core.SpatialSceneRuntime
import com.android.extensions.xr.node.Node

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
        get() {
            checkNotDisposed()
            return rtEntity!!.size.toFloatSize3d()
        }
        set(value) {
            checkNotDisposed()
            rtEntity!!.size = value.toRtDimensions()
        }

    public companion object {
        /**
         * Creates a [SubspaceNodeEntity] from a [Node] with a given [FloatSize3d].
         *
         * @param session The [Session].
         * @param node [Node] to create the [SubspaceNodeEntity] from.
         * @param size The initial [FloatSize3d] of the [SubspaceNodeEntity] in meters in unscaled
         *   local space.
         */
        @JvmStatic
        public fun create(session: Session, node: Node, size: FloatSize3d): SubspaceNodeEntity {
            val sceneRuntime: SpatialSceneRuntime = session.sceneRuntime as SpatialSceneRuntime
            return SubspaceNodeEntity(
                sceneRuntime.createSubspaceNodeEntity(node, size.toRtDimensions()),
                session.scene.entityManager,
            )
        }
    }
}
