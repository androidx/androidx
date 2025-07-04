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

import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.Entity as RtEntity
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.math.Pose

// TODO: b/427566816 - Fix HiddenSuperclass suppression
/**
 * An [Entity] that contains no content, but can have an arbitrary number of children. GroupEntity
 * is useful for organizing the placement and movement of a group of child SceneCore Entities.
 */
@Suppress("HiddenSuperclass") // BaseEntity is an internal class
public class GroupEntity private constructor(rtEntity: RtEntity, entityManager: EntityManager) :
    BaseEntity<RtEntity>(rtEntity, entityManager) {
    public companion object {
        /** Factory method to create GroupEntity entities. */
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            name: String,
            pose: Pose = Pose.Identity,
        ): Entity =
            GroupEntity(
                adapter.createGroupEntity(pose, name, adapter.activitySpaceRootImpl),
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
        public fun create(session: Session, name: String, pose: Pose = Pose.Identity): Entity =
            GroupEntity.create(session.platformAdapter, session.scene.entityManager, name, pose)
    }
}
