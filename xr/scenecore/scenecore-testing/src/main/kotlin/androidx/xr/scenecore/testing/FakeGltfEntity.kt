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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.scenecore.runtime.GltfAnimationFeature
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfModelNodeFeature
import androidx.xr.scenecore.runtime.NodeHolder
import androidx.xr.scenecore.testing.internal.FakeGltfEntity as InternalFakeGltfEntity
import java.util.concurrent.Executor
import java.util.function.Consumer

/** Test-only implementation of [androidx.xr.scenecore.runtime.GltfEntity] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class FakeGltfEntity
internal constructor(
    private val feature: FakeGltfFeature = createDefaultFeature(),
    private val executor: Executor = FakeScheduledExecutorService(),
    fakeInternal: InternalFakeGltfEntity,
) : FakeEntity(fakeInternal = fakeInternal), GltfEntity {

    internal companion object {
        private fun createDefaultFeature() =
            FakeGltfFeature(NodeHolder<FakeNode>(object : FakeNode {}, FakeNode::class.java))
    }

    public constructor(
        feature: FakeGltfFeature = createDefaultFeature(),
        executor: Executor = FakeScheduledExecutorService(),
    ) : this(feature, executor, InternalFakeGltfEntity(feature.fakeInternal, executor))

    override val nodes: List<GltfModelNodeFeature>
        get() = feature.nodes

    /**
     * Adds a node to the list of nodes in this fake glTF entity.
     *
     * @param node The [GltfModelNodeFeature] to add.
     */
    public fun addNode(node: GltfModelNodeFeature) {
        feature.addNode(node)
    }

    override val gltfModelBoundingBox: BoundingBox
        get() = feature.getGltfModelBoundingBox()

    /**
     * Sets the axis-aligned bounding box (AABB) for this fake glTF entity.
     *
     * @param boundingBox The [BoundingBox] to set.
     */
    public fun setGltfModelBoundingBox(boundingBox: BoundingBox) {
        feature.setGltfModelBoundingBox(boundingBox)
    }

    override val animations: List<GltfAnimationFeature>
        get() = feature.getAnimations(executor)

    /**
     * Adds an animation to the list of animations available in this fake glTF entity.
     *
     * @param animation The [GltfAnimationFeature] to add.
     */
    public fun addAnimation(animation: GltfAnimationFeature) {
        feature.addAnimation(animation)
    }

    override fun setColliderEnabled(enabled: Boolean) {
        feature.setColliderEnabled(enabled)
    }

    override fun addOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        feature.addOnBoundsUpdateListener(listener)
    }

    override fun removeOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        feature.removeOnBoundsUpdateListener(listener)
    }

    override fun setReformAffordanceEnabled(enabled: Boolean, systemMovable: Boolean) {
        feature.setReformAffordanceEnabled(this, enabled, executor, systemMovable)
    }
}
