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
import androidx.xr.runtime.XrLog
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.Entity as RtEntity
import androidx.xr.scenecore.runtime.SceneRuntime

/**
 * An [Entity] that contains no content, but can have an arbitrary number of children. GroupEntity
 * is useful for organizing the placement and movement of a group of child SceneCore Entities.
 */
public class GroupEntity private constructor(rtEntity: RtEntity, entityManager: EntityManager) :
    BaseEntity<RtEntity>(rtEntity, entityManager) {
    public companion object {
        /** Factory method to create GroupEntity entities. */
        internal fun create(
            sceneRuntime: SceneRuntime,
            entityManager: EntityManager,
            name: String,
            pose: Pose = Pose.Identity,
            parent: Entity? = entityManager.getEntityForRtEntity(sceneRuntime.activitySpace),
        ): GroupEntity =
            GroupEntity(
                sceneRuntime.createEntity(
                    pose,
                    name,
                    if (parent != null && parent !is BaseEntity<*>) {
                        XrLog.warn(
                            "The provided parent is not a BaseEntity. The GroupEntity will " +
                                "be created without a parent."
                        )
                        null
                    } else {
                        parent?.rtEntity
                    },
                ),
                entityManager,
            )

        /**
         * Public factory method for creating a [GroupEntity].
         *
         * @param session Session to create the GroupEntity in.
         * @param name Name of the entity.
         * @param pose Initial pose of the entity.
         */
        @JvmOverloads
        @JvmStatic
        @Deprecated(
            message =
                "Use Entity.create instead. Creating an Entity without any content is now done from the Entity class",
            replaceWith = ReplaceWith("Entity.create", "androidx.xr.scenecore.Entity"),
        )
        public fun create(session: Session, name: String, pose: Pose = Pose.Identity): GroupEntity =
            create(session.sceneRuntime, session.scene.entityManager, name, pose)

        /**
         * Public factory method for creating a [GroupEntity].
         *
         * @param session Session to create the GroupEntity in.
         * @param name Name of the entity.
         * @param pose Initial pose of the entity. The default value is [Pose.Identity].
         * @param parent Parent entity. If `null`, the entity is created but not attached to the
         *   scene graph and will not be visible until a parent is set. The default value is
         *   [Scene]'s [ActivitySpace].
         */
        @JvmStatic
        // TODO: b/462865943 - Replace @RestrictTo with @JvmOverloads and remove the other overload
        //  once the API proposal is approved.
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public fun create(
            session: Session,
            name: String,
            pose: Pose = Pose.Identity,
            parent: Entity? = session.scene.activitySpace,
        ): GroupEntity =
            create(session.sceneRuntime, session.scene.entityManager, name, pose, parent)
    }
}
