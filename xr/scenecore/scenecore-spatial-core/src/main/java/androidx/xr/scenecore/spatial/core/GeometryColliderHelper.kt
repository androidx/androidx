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

package androidx.xr.scenecore.spatial.core

import androidx.xr.scenecore.runtime.Component
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.InteractableComponent
import androidx.xr.scenecore.runtime.MeshEntity
import androidx.xr.scenecore.runtime.RotatableComponent

internal fun enableGeometryCollider(entity: Entity) {
    when (entity) {
        is GltfEntity -> entity.setColliderEnabled(true)
        is MeshEntity -> entity.setColliderEnabled(true)
    }
}

internal fun disableGeometryColliderIfUnused(
    entity: Entity,
    excludingComponent: Component? = null,
) {
    if (entity !is GltfEntity && entity !is MeshEntity) return
    val stillNeeded =
        entity.getComponents().any { component ->
            if (component === excludingComponent) return@any false
            when (component) {
                is InteractableComponentImpl -> component.entity != null
                is InteractableComponent -> true
                is RotatableComponentImpl -> component.isAttached
                is RotatableComponent -> true
                else -> false
            }
        }
    if (!stillNeeded) {
        when (entity) {
            is GltfEntity -> entity.setColliderEnabled(false)
            is MeshEntity -> entity.setColliderEnabled(false)
        }
    }
}
