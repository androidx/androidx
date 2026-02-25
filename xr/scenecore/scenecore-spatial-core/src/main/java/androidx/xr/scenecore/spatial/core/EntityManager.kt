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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore.spatial.core

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.ScenePose
import com.android.extensions.xr.node.Node
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/** Manages the mapping between [Node] and [Entity]. */
@RestrictTo(
    RestrictTo.Scope.LIBRARY_GROUP
) // TODO(b/452961674): Review RestrictTo annotations in SceneCore.
public class EntityManager public constructor() {
    private val nodeEntityMap = ConcurrentHashMap<Node, Entity>()
    private val systemSpaces = CopyOnWriteArrayList<ScenePose>()

    /**
     * Returns the [Entity] associated with the given [Node].
     *
     * @param node the [Node] to get the associated [Entity] for.
     * @return the [Entity] associated with the given [Node], or null if no such [ ] exists.
     */
    public fun getEntityForNode(node: Node): Entity? {
        return nodeEntityMap[node]
    }

    /**
     * Sets the [Entity] associated with the given [Node].
     *
     * @param node the [Node] to set the associated [Entity] for.
     * @param entity the [Entity] to associate with the given [Node].
     */
    public fun setEntityForNode(node: Node, entity: Entity) {
        nodeEntityMap[node] = entity
    }

    /**
     * Returns a list of all [Entity]s of type `T` (including subtypes of `T`).
     *
     * @param type the type of [Entity] to return.
     * @return a list of all [Entity]s of type `T` (including subtypes of `T`).
     */
    public fun <T : Entity> getEntitiesOfType(type: Class<out T>): List<T> =
        nodeEntityMap.values.distinct().filterIsInstance(type).toList()

    /** Returns a collection of all [Entity]s. */
    public fun getAllEntities(): Collection<Entity> = nodeEntityMap.values.distinct()

    /** Removes the given [Node] from the map. */
    public fun removeEntityForNode(node: Node) {
        nodeEntityMap.remove(node)
    }

    /** Adds a system space activity pose to the EntityManager. */
    public fun addSystemSpaceActivityPose(systemSpaceScenePose: ScenePose) {
        systemSpaces.add(systemSpaceScenePose)
    }

    /** Returns a collection of all system space activity poses. */
    public fun getAllSystemSpaceActivityPoses(): List<ScenePose> {
        return systemSpaces
    }

    /**
     * Returns a list of all [ScenePose]s of type `T` (including subtypes of `T`).
     *
     * @param systemSpaceScenePoseClass the type of [ScenePose] to return.
     * @return a list of all [ScenePose]s of type `T` (including subtypes of `T`).
     */
    public fun <T : ScenePose> getSystemSpaceActivityPoseOfType(
        systemSpaceScenePoseClass: Class<T>
    ): List<T> = systemSpaces.filterIsInstance(systemSpaceScenePoseClass).toList()

    /** Clears the EntityManager. */
    public fun clear() {
        nodeEntityMap.clear()
        systemSpaces.clear()
    }
}
