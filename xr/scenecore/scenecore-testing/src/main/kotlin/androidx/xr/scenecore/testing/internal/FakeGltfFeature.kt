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

package androidx.xr.scenecore.testing.internal

import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.GltfAnimationFeature
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.GltfModelNodeFeature
import androidx.xr.scenecore.runtime.GltfModelResource
import androidx.xr.scenecore.runtime.NodeHolder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executor
import java.util.function.Consumer

/** Test-only implementation of [androidx.xr.scenecore.runtime.GltfFeature] */
internal class FakeGltfFeature(nodeHolder: NodeHolder<*>) :
    FakeBaseRenderingFeature(nodeHolder), GltfFeature {

    internal var loadedGltf: GltfModelResource? = null

    private val _nodes: MutableList<GltfModelNodeFeature> = mutableListOf()

    override val nodes: List<GltfModelNodeFeature>
        get() = _nodes

    fun addNode(node: GltfModelNodeFeature) {
        _nodes.add(node)
    }

    override val size: FloatSize3d = FloatSize3d(1f, 1f, 1f)

    private val animationFeatureList = mutableListOf<GltfAnimationFeature>()

    fun addAnimation(animation: GltfAnimationFeature) {
        animationFeatureList.add(animation)
    }

    override fun getAnimations(executor: Executor): List<GltfAnimationFeature> {
        return animationFeatureList
    }

    private var boundingBox: BoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)

    fun setGltfModelBoundingBox(boundingBox: BoundingBox) {
        this.boundingBox = boundingBox
    }

    override fun getGltfModelBoundingBox(): BoundingBox {
        return boundingBox
    }

    var enableCollider: Boolean = false
        private set

    override fun setColliderEnabled(enableCollider: Boolean) {
        this.enableCollider = enableCollider
    }

    private val _animationStateListeners: MutableMap<Consumer<Int>, Executor> = ConcurrentHashMap()
    val animationStateListeners: Map<Consumer<Int>, Executor>
        get() = _animationStateListeners

    override fun addAnimationStateListener(executor: Executor, listener: Consumer<Int>) {
        _animationStateListeners[listener] = executor
    }

    override fun removeAnimationStateListener(listener: Consumer<Int>) {
        _animationStateListeners.remove(listener)
    }

    private val _boundsUpdateListeners: MutableSet<Consumer<BoundingBox>> = CopyOnWriteArraySet()
    val boundsUpdateListeners: Set<Consumer<BoundingBox>>
        get() = _boundsUpdateListeners

    override fun addOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        _boundsUpdateListeners.add(listener)
    }

    override fun removeOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        _boundsUpdateListeners.remove(listener)
    }

    var reformAffordanceEnabled: Boolean = false

    override fun setReformAffordanceEnabled(
        entity: GltfEntity,
        enabled: Boolean,
        executor: Executor,
        systemMovable: Boolean,
    ) {
        reformAffordanceEnabled = enabled
    }

    override fun dispose() {
        _animationStateListeners.clear()
        _boundsUpdateListeners.clear()
    }
}
