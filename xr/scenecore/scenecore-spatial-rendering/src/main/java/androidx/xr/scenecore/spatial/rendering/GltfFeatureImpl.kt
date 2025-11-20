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
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.impl.impress.GltfModel
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.impl.impress.Material
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.MaterialResource
import com.android.extensions.xr.XrExtensions
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import com.google.common.util.concurrent.ListenableFuture
import java.util.Collections
import java.util.HashMap
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
) : BaseRenderingFeature(impressApi, splitEngineSubspaceManager, extensions), GltfFeature {

    private val modelImpressNode: ImpressNode = impressApi.instanceGltfModel(gltfModel.nativeHandle)

    private val meshOverrides = mutableMapOf<String, Int>()

    private val animationStateListeners: MutableMap<Consumer<Int>, Executor> =
        Collections.synchronizedMap(mutableMapOf())

    init {
        bindImpressNodeToSubspace("gltf_entity_subspace_", modelImpressNode)
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

        val future: ListenableFuture<Void?> =
            impressApi.animateGltfModel(modelImpressNode, animationName, loop)
        animationState = GltfEntity.AnimationState.PLAYING

        // At the moment, we don't do anything interesting on failure except for logging. If we
        // didn't care about logging the failure, we could just not register a listener at all if
        // the animation is looping, since it will never terminate normally.
        future.addListener(
            {
                try {
                    future.get()
                    // The animation played to completion and has stopped
                } catch (e: Exception) {
                    if (e is InterruptedException) {
                        // If this happened, then it's likely Impress is shutting down and we
                        // need to shut down as well.
                        Thread.currentThread().interrupt()
                    } else {
                        // Some other error happened.  Log it and stop the animation.
                        Log.e("GltfEntityImpl", "Could not start animation: $e")
                    }
                } finally {
                    animationState = GltfEntity.AnimationState.STOPPED
                }
            },
            executor,
        )
    }

    @MainThread
    override fun stopAnimation() {
        if (animationState == GltfEntity.AnimationState.PLAYING) {
            impressApi.stopGltfModelAnimation(modelImpressNode)
            animationState = GltfEntity.AnimationState.STOPPED
        }
    }

    @MainThread
    override fun setMaterialOverride(
        material: MaterialResource,
        nodeName: String,
        primitiveIndex: Int,
    ) {
        require(material is Material) { "MaterialResource is not a Material" }
        impressApi.setMaterialOverride(
            modelImpressNode,
            (material as Material).nativeHandle,
            nodeName,
            primitiveIndex,
        )
        meshOverrides[nodeName] = primitiveIndex
    }

    @MainThread
    override fun clearMaterialOverride(nodeName: String, primitiveIndex: Int) {
        impressApi.clearMaterialOverride(modelImpressNode, nodeName, primitiveIndex)
        meshOverrides.remove(nodeName, primitiveIndex)
    }

    @MainThread
    override fun setColliderEnabled(enableCollider: Boolean) {
        impressApi.setGltfModelColliderEnabled(modelImpressNode, enableCollider)
    }

    @SuppressWarnings("ObjectToString")
    override fun dispose() {
        for ((key, value) in HashMap(meshOverrides)) {
            impressApi.clearMaterialOverride(modelImpressNode, key, value)
        }
        meshOverrides.clear()
        super.dispose()
    }

    @MainThread
    override fun addAnimationStateListener(executor: Executor, listener: Consumer<Int>): Unit {
        // Assigning the result to _ (or ignoring it) ensures the compiler sees Unit as the return.
        val unused = animationStateListeners.putIfAbsent(listener, executor)
    }

    @MainThread
    override fun removeAnimationStateListener(listener: Consumer<Int>): Unit {
        // Assigning the result to _ (or ignoring it) ensures the compiler sees Unit as the return.
        val unused = animationStateListeners.remove(listener)
    }
}
