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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.GltfAnimationFeature
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.GltfModelNodeFeature
import java.util.concurrent.Executor
import java.util.function.Consumer

/** Test-only implementation of [androidx.xr.scenecore.runtime.GltfEntity] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class FakeGltfEntity(
    private val feature: GltfFeature? = null,
    private val executor: Executor? = null,
) : FakeEntity(), GltfEntity {
    override val nodes: List<GltfModelNodeFeature>
        get() = feature?.nodes ?: emptyList()

    override val gltfModelBoundingBox: BoundingBox =
        BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)

    private val _animations = mutableListOf<GltfAnimationFeature>()

    override val animations: List<GltfAnimationFeature>
        get() = (feature?.getAnimations(executor!!) ?: emptyList()) + _animations

    override fun setColliderEnabled(enabled: Boolean) {
        feature?.setColliderEnabled(enabled)
    }

    override fun addOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        feature?.addOnBoundsUpdateListener(listener)
    }

    override fun removeOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        feature?.removeOnBoundsUpdateListener(listener)
    }

    override fun setReformAffordanceEnabled(enabled: Boolean, systemMovable: Boolean) {
        feature?.setReformAffordanceEnabled(this, enabled, executor!!, systemMovable)
    }

    /**
     * Adds an animation to the list of animations.
     *
     * @param animation The animation to add.
     */
    public fun addAnimation(animation: GltfAnimationFeature) {
        _animations.add(animation)
    }
}
