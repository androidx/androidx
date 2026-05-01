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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore

import androidx.xr.scenecore.runtime.Entity as RtEntity
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the mapping between [androidx.xr.scenecore.runtime.Entity] and [Entity] for a given
 * SceneCore [androidx.xr.runtime.Session].
 */
internal class EntityRegistry {
    private val rtEntityEntityMap = ConcurrentHashMap<RtEntity, WeakReference<Entity>>()

    /**
     * Returns the [Entity] associated with the given [androidx.xr.scenecore.runtime.Entity].
     *
     * @param rtEntity the [androidx.xr.scenecore.runtime.Entity] to get the associated [Entity]
     *   for.
     * @return [java.util.Optional] containing the [Entity] associated with the given
     *   [androidx.xr.scenecore.runtime.Entity], or empty if no such [Entity] exists.
     */
    internal fun getEntityForRtEntity(rtEntity: RtEntity): Entity? =
        rtEntityEntityMap[rtEntity]?.get()

    /**
     * Sets the [Entity] associated with the given [androidx.xr.scenecore.runtime.Entity].
     *
     * @param rtEntity the [androidx.xr.scenecore.runtime.Entity] to set the associated [Entity]
     *   for.
     * @param entity the [Entity] to associate with the given
     *   [androidx.xr.scenecore.runtime.Entity].
     */
    internal fun setEntityForRtEntity(rtEntity: RtEntity, entity: Entity) {
        rtEntityEntityMap[rtEntity] = WeakReference(entity)
    }

    /**
     * Inline function to get all entities of a given type.
     *
     * @param T the type of [Entity] to return.
     * @return a list of all [Entity]s of type [T] (including subtypes of [T]).
     */
    internal inline fun <reified T : Entity> getEntities(): List<T> {
        return rtEntityEntityMap.values.mapNotNull { it.get() }.filterIsInstance<T>()
    }

    /**
     * Returns all [Entity]s of the given type or its subtypes.
     *
     * @param type the type of [Entity] to return.
     * @return a list of all [Entity]s of the given type or its subtypes.
     */
    internal fun <T : Entity> getEntitiesOfType(type: Class<out T>): List<T> =
        rtEntityEntityMap.values.mapNotNull { it.get() }.filterIsInstance(type)

    /**
     * Returns a collection of all [Entity]s.
     *
     * @return a collection of all [Entity]s.
     */
    internal fun getAllEntities(): Collection<Entity> {
        return rtEntityEntityMap.values.mapNotNull { it.get() }
    }

    /**
     * Removes the given [Entity] from the map.
     *
     * @param entity the [Entity] to remove from the map.
     */
    internal fun removeEntity(entity: Entity) {
        rtEntityEntityMap.remove(entity.rtEntity)
    }

    /**
     * Removes the given [androidx.xr.scenecore.runtime.Entity] from the map.
     *
     * @param entity the [androidx.xr.scenecore.runtime.Entity] to remove from the map.
     */
    internal fun removeEntity(entity: RtEntity) {
        rtEntityEntityMap.remove(entity)
    }

    /**
     * Returns true if the given [androidx.xr.scenecore.runtime.Entity] is in the map.
     *
     * @param rtEntity the [androidx.xr.scenecore.runtime.Entity] to check for.
     * @return true if the given [androidx.xr.scenecore.runtime.Entity] is in the map.
     */
    internal fun containsRtEntity(rtEntity: RtEntity): Boolean =
        rtEntityEntityMap.containsKey(rtEntity)

    /** Clears the EntityRegistry. */
    internal fun clear() {
        rtEntityEntityMap.clear()
    }
}
