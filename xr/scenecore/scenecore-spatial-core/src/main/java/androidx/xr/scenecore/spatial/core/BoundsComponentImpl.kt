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

import android.util.Log
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.scenecore.runtime.BoundsComponent
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfEntity
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.function.Consumer

internal class BoundsComponentImpl : BoundsComponent {
    private var entity: Entity? = null

    // Use a ConcurrentHashMap to store multiple listeners and their executors.
    private val listeners = ConcurrentHashMap<Consumer<BoundingBox>, Executor>()

    private val frameListener =
        Consumer<BoundingBox> { boundingBox ->
            for ((listener, executor) in listeners) {
                executor.execute { listener.accept(boundingBox) }
            }
        }

    override fun addOnBoundsUpdateListener(executor: Executor, listener: Consumer<BoundingBox>) {
        val wasEmpty = listeners.isEmpty()
        listeners[listener] = executor

        if (entity is GltfEntity) {
            val gltfEntity = entity as GltfEntity
            if (wasEmpty) {
                gltfEntity.addOnBoundsUpdateListener(frameListener)
            }

            val currentBox = gltfEntity.gltfModelBoundingBox
            executor.execute { listener.accept(currentBox) }
        }
    }

    override fun removeOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        listeners.remove(listener)

        if (entity is GltfEntity && listeners.isEmpty()) {
            (entity as GltfEntity).removeOnBoundsUpdateListener(frameListener)
        }
    }

    override fun onAttach(entity: Entity): Boolean {
        if (this.entity != null) {
            return false
        }
        if (entity !is GltfEntity) {
            Log.w(TAG, "BoundsComponent can be attached to GltfEntity only.")
            return false
        }
        this.entity = entity

        if (listeners.isNotEmpty()) {
            val gltfEntity = entity as GltfEntity
            gltfEntity.addOnBoundsUpdateListener(frameListener)

            val currentBox = gltfEntity.gltfModelBoundingBox
            for ((listener, executor) in listeners) {
                executor.execute { listener.accept(currentBox) }
            }
        }

        return true
    }

    override fun onDetach(entity: Entity) {
        if (entity is GltfEntity) {
            entity.removeOnBoundsUpdateListener(frameListener)
        }

        this.entity = null
    }

    private companion object {
        const val TAG = "BoundsComponentImpl"
    }
}
