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
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.Entity as RtEntity
import androidx.xr.scenecore.runtime.SceneRuntime

/**
 * An [Entity] that contains no content, but can have an arbitrary number of children. GroupEntity
 * is useful for organizing the placement and movement of a group of child SceneCore Entities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GroupEntity private constructor(rtEntity: RtEntity, entityRegistry: EntityRegistry) :
    Entity(rtEntity, entityRegistry) {
    public companion object {
        /** Factory method to create GroupEntity entities. */
        @Suppress("RestrictedApiAndroidX")
        internal fun create(
            sceneRuntime: SceneRuntime,
            entityRegistry: EntityRegistry,
            name: String,
            pose: Pose = Pose.Identity,
            parent: Entity? = entityRegistry.getEntityForRtEntity(sceneRuntime.activitySpace),
        ): GroupEntity =
            GroupEntity(
                    @Suppress("DEPRECATION")
                    sceneRuntime.createGroupEntity(pose, name, parent?.rtEntity),
                    entityRegistry,
                )
                .also { it.parent = parent }

        /**
         * Public factory method for creating a [GroupEntity].
         *
         * @param session Session to create the GroupEntity in.
         * @param name Name of the entity.
         * @param pose Initial pose of the entity. The default value is [Pose.Identity].
         * @param parent Parent entity. Defaults to `null`. If `null`, the entity is created but not
         *   attached to the scene graph and will be invisible. When a parent entity (e.g.,
         *   [ActivitySpace] or any other [Entity] already present in the scene) is assigned later,
         *   the entity will remain invisible until you explicitly enable it by calling
         *   [Entity.setEnabled] (enabled=true). This allows for [Entity] pre-configuration before
         *   making it visible.
         */
        @JvmOverloads
        @JvmStatic
        @Deprecated(
            message =
                "Use Entity.create instead. Creating an Entity without any content is now done from the Entity class",
            replaceWith = ReplaceWith("Entity.create", "androidx.xr.scenecore.Entity"),
        )
        public fun create(
            session: Session,
            name: String,
            pose: Pose = Pose.Identity,
            parent: Entity? = null,
        ): GroupEntity =
            create(session.sceneRuntime, session.scene.entityRegistry, name, pose, parent)
    }
}
