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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.scenecore.runtime.BoundsComponent
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfEntity
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.function.Consumer

/** Test-only implementation of [BoundsComponent] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeBoundsComponent : FakeComponent(), BoundsComponent {
    public var entity: GltfEntity? = null
        private set

    public var listeners: ConcurrentHashMap<Consumer<BoundingBox>, Executor> =
        ConcurrentHashMap<Consumer<BoundingBox>, Executor>()
        private set

    override fun onAttach(entity: Entity): Boolean {
        if (entity is GltfEntity) {
            this.entity = entity
        }
        return (this.entity != null)
    }

    override fun onDetach(entity: Entity) {
        this.entity = null
    }

    override fun addOnBoundsUpdateListener(executor: Executor, listener: Consumer<BoundingBox>) {
        listeners[listener] = executor
    }

    override fun removeOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        listeners.remove(listener)
    }

    /**
     * Simulates a bounds update event from the runtime, notifying all registered listeners.
     *
     * This function is intended for testing purposes to allow manual triggering of the update
     * mechanism. It iterates through all currently registered listeners and invokes their
     * `onBoundsUpdate` method on their respective [Executor]s.
     *
     * @param boundingBox The new [BoundingBox] to be sent in the simulated event.
     */
    public fun onBoundsUpdate(boundingBox: BoundingBox) {
        for (entry in listeners.entries) {
            val executor = entry.value
            val listener = entry.key
            executor.execute(Runnable { listener.accept(boundingBox) })
        }
    }
}
