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

import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
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
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private var currentAnimationJob: Job? = null

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

    @get:GltfEntity.AnimationStateValue
    override var animationState: Int = GltfEntity.AnimationState.STOPPED
        private set(value) {
            if (field != value) {
                field = value
                synchronized(animationStateListeners) {
                    animationStateListeners.forEach { (listener, executor) ->
                        executor.execute { listener.accept(value) }
                    }
                }
            }
        }

    @MainThread
    override fun getGltfModelBoundingBox(): BoundingBox =
        impressApi.getGltfModelBoundingBox(modelImpressNode)

    @MainThread
    override fun startAnimation(loop: Boolean, animationName: String?, executor: Executor) {
        // TODO: b/362826747 - Add a listener interface so that the application can be
        // notified that the animation has stopped, been cancelled (by starting another animation)
        // and / or shown an error state if something went wrong.

        currentAnimationJob?.cancel()
        val coroutineDispatcher = executor.asCoroutineDispatcher()
        animationState = GltfEntity.AnimationState.PLAYING
        currentAnimationJob =
            CoroutineScope(coroutineDispatcher).launch {
                try {
                    // The @MainThread annotation is a "Lint" check. As soon as you call launch, you
                    // are creating a new asynchronous task. The Dispatcher you pass to launch
                    // decides where that task runs. If you try to access that context from a
                    // background thread (which is where executor put you), the native code looks
                    // for the context, doesn't find it (or finds a mismatch), and fails or crashes
                    withContext(Dispatchers.Main) {
                        impressApi.animateGltfModel(modelImpressNode, animationName, loop)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    // Some other error happened.  Log it and stop the animation.
                    Log.e("GltfFeatureImpl", "Could not start animation: $e")
                } finally {
                    if (currentAnimationJob === coroutineContext[Job]) {
                        animationState = GltfEntity.AnimationState.STOPPED
                    }
                }
            }
    }

    @MainThread
    override fun stopAnimation() {
        if (
            animationState == GltfEntity.AnimationState.PLAYING ||
                animationState == GltfEntity.AnimationState.PAUSED
        ) {
            impressApi.stopGltfModelAnimation(modelImpressNode)
            animationState = GltfEntity.AnimationState.STOPPED
        }
    }

    @MainThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun pauseAnimation() {
        if (animationState == GltfEntity.AnimationState.PLAYING) {
            impressApi.toggleGltfModelAnimation(modelImpressNode, /* playing= */ false)
            animationState = GltfEntity.AnimationState.PAUSED
        }
    }

    @MainThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun resumeAnimation() {
        if (animationState == GltfEntity.AnimationState.PAUSED) {
            impressApi.toggleGltfModelAnimation(modelImpressNode, /* playing= */ true)
            animationState = GltfEntity.AnimationState.PLAYING
        }
    }

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
                    if (animationState == GltfEntity.AnimationState.PLAYING) {
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
