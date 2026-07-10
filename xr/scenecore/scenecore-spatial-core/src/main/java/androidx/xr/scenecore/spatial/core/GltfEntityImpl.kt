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

import android.content.Context
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.GltfAnimationFeature
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.GltfModelNodeFeature
import com.android.extensions.xr.XrExtensions
import java.util.concurrent.ScheduledExecutorService
import java.util.function.Consumer

/**
 * Implementation of a SceneCore GltfEntity.
 *
 * This is used to create an entity that contains a glTF object.
 */
internal class GltfEntityImpl(
    context: Context,
    private val gltfFeature: GltfFeature,
    parentEntity: Entity?,
    extensions: XrExtensions,
    sceneNodeRegistry: SceneNodeRegistry,
    executor: ScheduledExecutorService,
) : BaseRenderingEntity(context, gltfFeature, extensions, sceneNodeRegistry, executor), GltfEntity {
    init {
        parent = parentEntity
    }

    override val nodes: List<GltfModelNodeFeature>
        get() = gltfFeature.nodes

    override val gltfModelBoundingBox: BoundingBox
        get() = gltfFeature.getGltfModelBoundingBox()

    override val animations: List<GltfAnimationFeature>
        get() = gltfFeature.getAnimations(scheduledExecutor)

    override fun setColliderEnabled(enabled: Boolean) {
        gltfFeature.setColliderEnabled(enabled)
    }

    override fun addOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        gltfFeature.addOnBoundsUpdateListener(listener)
    }

    override fun removeOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        gltfFeature.removeOnBoundsUpdateListener(listener)
    }

    override fun setReformAffordanceEnabled(enabled: Boolean, systemMovable: Boolean) {
        gltfFeature.setReformAffordanceEnabled(this, enabled, scheduledExecutor, systemMovable)
    }
}
