/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.scenecore.spatial.rendering

import androidx.annotation.MainThread
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.impl.impress.GltfModel
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.runtime.GltfAnimationFeature
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.GltfModelNodeFeature
import androidx.xr.scenecore.spatial.core.AndroidXrEntity
import com.android.extensions.xr.XrExtensions
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer
import java.util.Collections
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Implementation of a SceneCore GltfEntity.
 *
 * <p>This is used to create an entity that contains a glTF object.
 */
// TODO: b/375520647 - Add unit tests for this class.
internal class GltfFeatureImpl(
    gltfModel: GltfModel,
    impressApi: ImpressApi,
    splitEngineSubspaceManager: SplitEngineSubspaceManager,
    extensions: XrExtensions,
    private val renderer: ImpSplitEngineRenderer,
) : BaseRenderingFeature(impressApi, splitEngineSubspaceManager, extensions), GltfFeature {

    private val modelImpressNode: ImpressNode = impressApi.instanceGltfModel(gltfModel.nativeHandle)

    private var animationFeatureList: List<GltfAnimationFeature>? = null

    @MainThread
    override fun getAnimations(executor: Executor): List<GltfAnimationFeature> {
        if (animationFeatureList == null) {
            val list = mutableListOf<GltfAnimationFeature>()
            for (i in 0 until impressApi.getGltfModelAnimationCount(modelImpressNode)) {
                list.add(
                    GltfAnimationFeatureImpl(
                        impressApi = impressApi,
                        modelImpressNode = modelImpressNode,
                        index = i,
                        name = impressApi.getGltfModelAnimationName(modelImpressNode, i),
                        duration =
                            impressApi.getGltfModelAnimationDurationSeconds(modelImpressNode, i),
                        executor = executor,
                    )
                )
            }
            animationFeatureList = Collections.unmodifiableList(list)
        }
        return animationFeatureList!!
    }

    private val animationStateListeners: MutableMap<Consumer<Int>, Executor> =
        Collections.synchronizedMap(mutableMapOf())

    private val boundsUpdateListeners: MutableSet<Consumer<BoundingBox>> = CopyOnWriteArraySet()
    private var lastBoundingBox: BoundingBox? = null

    init {
        bindImpressNodeToSubspace("gltf_entity_subspace_", modelImpressNode)
    }

    override val nodes: List<GltfModelNodeFeature> by lazy {
        val count = impressApi.getImpressNodeChildCount(modelImpressNode)
        val nodeList = ArrayList<GltfModelNodeFeature>(count)
        for (i in 0 until count) {
            val childNode = impressApi.getImpressNodeChildAt(modelImpressNode, i)
            val name = impressApi.getImpressNodeName(childNode)
            nodeList.add(GltfModelNodeFeatureImpl(impressApi, childNode, modelImpressNode, name))
        }
        nodeList.toList()
    }

    override val size: FloatSize3d = getGltfModelBoundingBox().halfExtents.times(2f)

    @MainThread
    override fun getGltfModelBoundingBox(): BoundingBox =
        impressApi.getGltfModelBoundingBox(modelImpressNode)

    @MainThread
    override fun setColliderEnabled(enableCollider: Boolean) {
        impressApi.setGltfModelColliderEnabled(modelImpressNode, enableCollider)
    }

    @SuppressWarnings("ObjectToString")
    override fun dispose() {
        nodes.forEach { it.clearMaterialOverrides() }
        renderer.frameListener = null
        boundsUpdateListeners.clear()
        super.dispose()
    }

    @MainThread
    override fun addAnimationStateListener(executor: Executor, listener: Consumer<Int>) {
        // Assigning the result to _ (or ignoring it) ensures the compiler sees Unit as the return.
        animationStateListeners.putIfAbsent(listener, executor)
    }

    @MainThread
    override fun removeAnimationStateListener(listener: Consumer<Int>) {
        // Assigning the result to _ (or ignoring it) ensures the compiler sees Unit as the return.
        animationStateListeners.remove(listener)
    }

    @MainThread
    override fun addOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        if (boundsUpdateListeners.isEmpty()) {
            val frameListener =
                ImpSplitEngineRenderer.FrameListener {
                    // Check if any animation is currently playing
                    val isAnimationPlaying =
                        animationFeatureList?.any {
                            it.animationState == GltfEntity.AnimationState.PLAYING
                        } == true

                    if (isAnimationPlaying) {
                        val boundingBox = getGltfModelBoundingBox()
                        if (boundingBox != lastBoundingBox) {
                            lastBoundingBox = boundingBox
                            boundsUpdateListeners.forEach { it.accept(boundingBox) }
                        }
                    }
                }
            renderer.frameListener = frameListener
        }

        boundsUpdateListeners.add(listener)
    }

    @MainThread
    override fun removeOnBoundsUpdateListener(listener: Consumer<BoundingBox>) {
        boundsUpdateListeners.remove(listener)

        if (boundsUpdateListeners.isEmpty()) {
            renderer.frameListener = null
        }
    }

    @MainThread
    override fun setReformAffordanceEnabled(
        entity: GltfEntity,
        enabled: Boolean,
        executor: Executor,
        systemMovable: Boolean,
    ) {
        impressApi.setGltfReformAffordanceEnabled(modelImpressNode, enabled, systemMovable)
        if (enabled) {
            subspace?.let { splitEngineSubspace ->
                splitEngineSubspace.subspaceNode?.listenForInput(executor) { inputEvent ->
                    splitEngineSubspaceManager.forwardInputEvent(
                        inputEvent,
                        splitEngineSubspace.subspaceId,
                    )
                    (entity as AndroidXrEntity).handleInputEvent(inputEvent)
                }
            }
        } else {
            subspace?.subspaceNode?.stopListeningForInput()
        }
    }
}
