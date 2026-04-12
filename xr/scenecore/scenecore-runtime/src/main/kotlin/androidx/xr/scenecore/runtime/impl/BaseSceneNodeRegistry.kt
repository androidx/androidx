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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore.runtime.impl

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.ScenePose
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A generic thread-safe manager for mapping a native node of type [K] to an [Entity].
 *
 * It also tracks non-Entity [ScenePose] used as system spaces in the scene.
 *
 * @param K The type of the key used to look up entities or sceneposes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class BaseSceneNodeRegistry<K : Any> {
    private val nodeEntityMap = ConcurrentHashMap<K, Entity>()
    private val systemSpaces = CopyOnWriteArrayList<ScenePose>()

    /**
     * Returns the [Entity] associated with the given node.
     *
     * @param node the node of type [K] to get the associated [Entity] for.
     * @return the [Entity] associated with the given node, or null if no such node exists.
     */
    public fun getEntityForNode(node: K): Entity? {
        return nodeEntityMap[node]
    }

    /**
     * Sets the [Entity] associated with the given node.
     *
     * @param node the node of type [K] to set the associated [Entity] for.
     * @param entity the [Entity] to associate with the given node.
     */
    public fun setEntityForNode(node: K, entity: Entity) {
        nodeEntityMap[node] = entity
    }

    /**
     * Returns a list of all [Entity]s of type `T` (including subtypes of `T`).
     *
     * @param type the type of [Entity] to return.
     * @return a list of all [Entity]s of type `T` (including subtypes of `T`).
     */
    public fun <T : Entity> getEntitiesOfType(type: Class<out T>): List<T> =
        nodeEntityMap.values.distinct().filterIsInstance(type)

    /** Returns a collection of all [Entity]s. */
    public fun getAllEntities(): Collection<Entity> = nodeEntityMap.values.distinct()

    /** Removes the given node of type [K] from the map. */
    public fun removeEntityForNode(node: K) {
        nodeEntityMap.remove(node)
    }

    /** Adds a system space [ScenePose] to the SceneNodeRegistry. */
    public fun addSystemSpaceScenePose(systemSpaceScenePose: ScenePose) {
        systemSpaces.add(systemSpaceScenePose)
    }

    /** Returns a collection of all system space [ScenePose]s. */
    public fun getAllSystemSpaceScenePoses(): List<ScenePose> {
        return systemSpaces
    }

    /**
     * Returns a list of all [ScenePose]s of type `T` (including subtypes of `T`).
     *
     * @param systemSpaceScenePoseClass the type of [ScenePose] to return.
     * @return a list of all [ScenePose]s of type `T` (including subtypes of `T`).
     */
    public fun <T : ScenePose> getSystemSpaceScenePoseOfType(
        systemSpaceScenePoseClass: Class<T>
    ): List<T> = systemSpaces.filterIsInstance(systemSpaceScenePoseClass)

    /** Clears the SceneNodeRegistry. */
    public fun clear() {
        nodeEntityMap.clear()
        systemSpaces.clear()
    }
}
