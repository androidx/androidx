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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.runtime.GltfAnimationFeature
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.GltfModelNodeFeature
import androidx.xr.scenecore.runtime.NodeHolder
import androidx.xr.scenecore.testing.internal.FakeGltfAnimationFeature as InternalFakeGltfAnimationFeature
import androidx.xr.scenecore.testing.internal.FakeGltfEntity as InternalFakeGltfEntity
import androidx.xr.scenecore.testing.internal.FakeGltfFeature as InternalFakeGltfFeature
import androidx.xr.scenecore.testing.internal.FakeGltfModelNodeFeature as InternalFakeGltfModelNodeFeature
import java.util.concurrent.Executor
import java.util.function.Consumer

/** Test-only implementation of [androidx.xr.scenecore.runtime.GltfFeature] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeGltfFeature
internal constructor(
    nodeHolder: NodeHolder<*>,
    internal var fakeInternal: InternalFakeGltfFeature,
) : FakeBaseRenderingFeature(nodeHolder), GltfFeature {

    public constructor(
        nodeHolder: NodeHolder<*>
    ) : this(nodeHolder, InternalFakeGltfFeature(nodeHolder))

    private val nodeWrappers =
        mutableMapOf<InternalFakeGltfModelNodeFeature, FakeGltfModelNodeFeature>()

    override val nodes: List<GltfModelNodeFeature>
        get() =
            fakeInternal.nodes.map { internalNode ->
                val typedInternalNode = internalNode as InternalFakeGltfModelNodeFeature
                nodeWrappers.getOrPut(typedInternalNode) {
                    FakeGltfModelNodeFeature(typedInternalNode.name, typedInternalNode)
                }
            }

    public fun addNode(node: GltfModelNodeFeature) {
        val fakeNode = node as FakeGltfModelNodeFeature
        nodeWrappers[fakeNode.fakeInternal] = fakeNode
        fakeInternal.addNode(fakeNode.fakeInternal)
    }

    override val size: FloatSize3d
        get() = fakeInternal.size

    private val animationWrappers =
        mutableMapOf<InternalFakeGltfAnimationFeature, FakeGltfAnimationFeature>()

    public fun addAnimation(animation: GltfAnimationFeature) {
        val fakeAnimation = animation as FakeGltfAnimationFeature
        animationWrappers[fakeAnimation.fakeInternal] = fakeAnimation
        fakeInternal.addAnimation(fakeAnimation.fakeInternal)
    }

    override fun getAnimations(executor: Executor): List<GltfAnimationFeature> {
        return fakeInternal.getAnimations(executor).map { internalAnim ->
            val typedInternalAnim = internalAnim as InternalFakeGltfAnimationFeature
            animationWrappers.getOrPut(typedInternalAnim) {
                FakeGltfAnimationFeature(
                    animationName = typedInternalAnim.animationName,
                    animationIndex = typedInternalAnim.animationIndex,
                    animationDuration = typedInternalAnim.animationDuration,
                    fakeInternal = typedInternalAnim,
                )
            }
        }
    }

    public fun setGltfModelBoundingBox(boundingBox: BoundingBox) {
        fakeInternal.setGltfModelBoundingBox(boundingBox)
    }

    override fun getGltfModelBoundingBox(): BoundingBox {
        return fakeInternal.getGltfModelBoundingBox()
    }

    public val enableCollider: Boolean
        get() = fakeInternal.enableCollider

    override fun setColliderEnabled(enableCollider: Boolean) {
        fakeInternal.setColliderEnabled(enableCollider)
    }

    public val animationStateListeners: Map<Consumer<Int>, Executor>
        get() = fakeInternal.animationStateListeners

    override fun addAnimationStateListener(executor: Executor, listener: Consumer<Int>) {
        fakeInternal.addAnimationStateListener(executor, listener)
    }

    override fun removeAnimationStateListener(listener: Consumer<Int>) {
        fakeInternal.removeAnimationStateListener(listener)
    }

    public val boundsUpdateListeners: Set<Consumer<BoundingBox>>
        get() = fakeInternal.boundsUpdateListeners

    override fun addOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        fakeInternal.addOnBoundsUpdateListener(listener)
    }

    override fun removeOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        fakeInternal.removeOnBoundsUpdateListener(listener)
    }

    public val reformAffordanceEnabled: Boolean
        get() = fakeInternal.reformAffordanceEnabled

    override fun setReformAffordanceEnabled(
        entity: GltfEntity,
        enabled: Boolean,
        executor: Executor,
        systemMovable: Boolean,
    ) {
        fakeInternal.setReformAffordanceEnabled(
            ((entity as FakeGltfEntity).fakeInternal as InternalFakeGltfEntity),
            enabled,
            executor,
            systemMovable,
        )
    }

    override fun dispose() {
        fakeInternal.dispose()
    }

    public companion object {
        public fun createWithMockFeature(
            feature: GltfFeature,
            nodeHolder: NodeHolder<*>,
        ): GltfFeature {
            return FakeGltfFeature(nodeHolder)
        }
    }
}
