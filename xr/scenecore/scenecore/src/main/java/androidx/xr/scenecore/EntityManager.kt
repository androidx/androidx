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

import androidx.xr.runtime.internal.Entity as RtEntity
import java.util.concurrent.ConcurrentHashMap

/** Manages the mapping between [RuntimeEntity] and [Entity] for a given SceneCore [Session]. */
internal class EntityManager {
    private val rtEntityEntityMap = ConcurrentHashMap<RtEntity, Entity>()

    /**
     * Returns the [Entity] associated with the given [RtEntity].
     *
     * @param rtEntity the [RtEntity] to get the associated [Entity] for.
     * @return [java.util.Optional] containing the [Entity] associated with the given [RtEntity], or
     *   empty if no such [Entity] exists.
     */
    internal fun getEntityForRtEntity(rtEntity: RtEntity): Entity? = rtEntityEntityMap[rtEntity]

    /**
     * Sets the [Entity] associated with the given [RtEntity].
     *
     * @param rtEntity the [RtEntity] to set the associated [Entity] for.
     * @param entity the [Entity] to associate with the given [RtEntity].
     */
    internal fun setEntityForRtEntity(rtEntity: RtEntity, entity: Entity) {
        rtEntityEntityMap[rtEntity] = entity
    }

    /**
     * Inline function to get all entities of a given type.
     *
     * @param T the type of [Entity] to return.
     * @return a list of all [Entity]s of type [T] (including subtypes of [T]).
     */
    internal inline fun <reified T : Entity> getEntities(): List<T> {
        return rtEntityEntityMap.values.filterIsInstance<T>()
    }

    /**
     * Returns all [Entity]s of the given type or its subtypes.
     *
     * @param type the type of [Entity] to return.
     * @return a list of all [Entity]s of the given type or its subtypes.
     */
    internal fun <T : Entity> getEntitiesOfType(type: Class<out T>): List<T> =
        rtEntityEntityMap.values.filterIsInstance(type)

    /**
     * Returns a collection of all [Entity]s.
     *
     * @return a collection of all [Entity]s.
     */
    internal fun getAllEntities(): Collection<Entity> {
        return rtEntityEntityMap.values
    }

    /**
     * Removes the given [Entity] from the map.
     *
     * @param entity the [Entity] to remove from the map.
     */
    internal fun removeEntity(entity: Entity) {
        rtEntityEntityMap.remove((entity as BaseEntity<*>).rtEntity)
    }

    /**
     * Removes the given [RtEntity] from the map.
     *
     * @param entity the [RtEntity] to remove from the map.
     */
    internal fun removeEntity(entity: RtEntity) {
        rtEntityEntityMap.remove(entity)
    }

    /** Clears the EntityManager. */
    internal fun clear() {
        rtEntityEntityMap.clear()
    }
}
