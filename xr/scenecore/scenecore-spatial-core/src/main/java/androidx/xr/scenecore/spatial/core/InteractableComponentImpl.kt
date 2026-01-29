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
package androidx.xr.scenecore.spatial.core

import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.InputEventListener
import androidx.xr.scenecore.runtime.InteractableComponent
import java.util.concurrent.Executor

internal class InteractableComponentImpl(val executor: Executor, val consumer: InputEventListener) :
    InteractableComponent {
    var entity: Entity? = null

    override fun onAttach(entity: Entity): Boolean {
        if (this.entity != null) {
            return false
        }
        this.entity = entity
        when (entity) {
            is GltfEntityImpl -> entity.setColliderEnabled(true)
            is SurfaceEntityImpl -> entity.setColliderEnabled(true)
        }
        // InputEvent type translation happens here.
        entity.addInputEventListener(executor, consumer)
        return true
    }

    override fun onDetach(entity: Entity) {
        when (entity) {
            is GltfEntityImpl -> entity.setColliderEnabled(false)
            is SurfaceEntityImpl -> entity.setColliderEnabled(false)
        }
        entity.removeInputEventListener(consumer)
        this.entity = null
    }
}
