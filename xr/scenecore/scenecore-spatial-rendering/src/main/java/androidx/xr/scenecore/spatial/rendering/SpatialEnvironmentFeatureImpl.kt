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

import android.app.Activity
import android.os.Looper
import androidx.xr.scenecore.impl.impress.ExrImage
import androidx.xr.scenecore.impl.impress.GltfModel
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.impl.impress.Material
import androidx.xr.scenecore.runtime.ExrImageResource
import androidx.xr.scenecore.runtime.GltfModelResource
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.runtime.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.runtime.SpatialEnvironmentFeature
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import com.google.androidxr.splitengine.SubspaceNode
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class SpatialEnvironmentFeatureImpl(
    private val activity: Activity,
    impressApi: ImpressApi,
    splitEngineSubspaceManager: SplitEngineSubspaceManager,
    extensions: XrExtensions,
) :
    BaseRenderingFeature(impressApi, splitEngineSubspaceManager, extensions),
    SpatialEnvironmentFeature,
    Consumer<Consumer<Node>> {

    private val GEOMETRY_NODE_NAME = "EnvironmentGeometryNode"
    private val _spatialEnvironmentPreference = AtomicReference<SpatialEnvironmentPreference?>(null)

    override var preferredSpatialEnvironment: SpatialEnvironmentPreference?
        get() = _spatialEnvironmentPreference.get()
        set(value) {
            setPreferredSpatialEnvironmentInternal(value)
        }

    private var geometrySubspaceSplitEngine: SubspaceNode? = null
    private var geometrySubspaceImpressNode: ImpressNode? = null
    private lateinit var rootEnvironmentNode: Node
    private lateinit var geometryImpressNode: ImpressNode
    private lateinit var materialOverride: Material
    private lateinit var overriddenNodeName: String
    private var onBeforeNodeAttachedListener: Consumer<Node>? = null
    private var isDisposed = false
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        // Use node from parent BaseRenderingFeature
        rootEnvironmentNode = node
    }

    private fun applySkybox(skybox: ExrImageResource?) {
        check(Looper.getMainLooper().isCurrentThread) {
            "This method must be called on the main thread."
        }

        impressApi.clearPreferredEnvironmentIblAsset()
        if (skybox != null) {
            impressApi.setPreferredEnvironmentLight((skybox as ExrImage).nativeHandle)
        }
    }

    private suspend fun applyGeometry(
        geometry: GltfModelResource?,
        material: MaterialResource?,
        nodeName: String?,
        animationName: String?,
    ) {
        check(Looper.getMainLooper().isCurrentThread) {
            "This method must be called on the main thread."
        }

        val subspaceNode = impressApi.createImpressNode()
        geometrySubspaceImpressNode = subspaceNode
        val subspaceName = "geometry_subspace_" + subspaceNode.handle

        geometrySubspaceSplitEngine =
            splitEngineSubspaceManager.createSubspace(subspaceName, subspaceNode.handle)

        geometrySubspaceSplitEngine?.let { geometrySubspace ->
            extensions.createNodeTransaction().use { transaction ->
                transaction
                    .setName(geometrySubspace.subspaceNode, GEOMETRY_NODE_NAME)
                    .setPosition(geometrySubspace.subspaceNode, 0.0f, 0.0f, 0.0f)
                    .setScale(geometrySubspace.subspaceNode, 1.0f, 1.0f, 1.0f)
                    .setOrientation(geometrySubspace.subspaceNode, 0.0f, 0.0f, 0.0f, 1.0f)
                    .apply()
            }
        }

        if (geometry != null) {
            geometryImpressNode =
                impressApi.instanceGltfModel(
                    (geometry as GltfModel).nativeHandle,
                    /* enableCollider= */ false,
                )

            if (material != null && nodeName != null) {
                materialOverride = material as Material
                overriddenNodeName = nodeName
                impressApi.setMaterialOverride(
                    geometryImpressNode,
                    material.nativeHandle,
                    nodeName,
                    /* primitiveIndex= */ 0,
                )
            }
            if (animationName != null) {
                // animateGltfModel is itself an asynchronous call, and it blocks execution
                // until the animation finishes. We need to wrap this call in a coroutine so
                // that we can proceed with the parenting of the environment instead of
                // waiting for the animation to complete.
                coroutineScope.launch {
                    impressApi.animateGltfModel(geometryImpressNode, animationName, true)
                }
            }
            impressApi.setImpressNodeParent(geometryImpressNode, subspaceNode)
        }
    }

    override fun accept(nodeConsumer: Consumer<Node>) {
        onBeforeNodeAttachedListener = nodeConsumer
    }

    private fun setPreferredSpatialEnvironmentInternal(
        newPreference: SpatialEnvironmentPreference?
    ) {
        // This synchronized block makes sure following members are updated atomically:
        // spatialEnvironmentPreference, rootEnvironmentNode, mExtensions,
        // geometrySubspaceSplitEngine, geometrySubspaceImpressNode.
        _spatialEnvironmentPreference.getAndUpdate { prevPreference ->
            if (newPreference == prevPreference) {
                return@getAndUpdate prevPreference
            }

            val newGeometry = newPreference?.geometry
            val prevGeometry = prevPreference?.geometry
            val newSkybox = newPreference?.skybox
            val prevSkybox = prevPreference?.skybox
            val newMaterial = newPreference?.geometryMaterial
            val newNodeName = newPreference?.geometryNodeName
            val newAnimationName = newPreference?.geometryAnimationName

            // TODO: b/392948759 - Fix StrictMode violations triggered whenever skybox is
            // set.
            if (newSkybox != prevSkybox || prevPreference == null) {
                applySkybox(newSkybox)
            }

            if (newPreference == null) {
                // Detaching the app environment to go back to the system environment.
                extensions.detachSpatialEnvironment(activity, Runnable::run) {}
            } else {
                // TODO(b/408276187): Add unit test that verifies that the skybox mode is
                // correctly set.
                var skyboxMode = XrExtensions.ENVIRONMENT_SKYBOX_APP
                if (newSkybox == null) {
                    skyboxMode = XrExtensions.NO_SKYBOX
                }
                // Transitioning to a new app environment.
                val currentRootEnvironmentNode: Node
                if (newGeometry != prevGeometry) {
                    // Environment geometry has changed, create a new environment node and
                    // attach the geometry subspace to it.
                    currentRootEnvironmentNode = extensions.createNode()
                    coroutineScope.launch {
                        applyGeometry(newGeometry, newMaterial, newNodeName, newAnimationName)
                        geometrySubspaceSplitEngine?.let { geometrySubspace ->
                            extensions.createNodeTransaction().use { transaction ->
                                @Suppress("UNUSED_VARIABLE")
                                val unused =
                                    transaction.setParent(
                                        geometrySubspace.subspaceNode,
                                        currentRootEnvironmentNode,
                                    )
                                transaction.apply()
                            }
                        }
                    }
                } else {
                    // Environment geometry has not changed, use the existing environment
                    // node.
                    currentRootEnvironmentNode = rootEnvironmentNode
                }
                onBeforeNodeAttachedListener?.accept(currentRootEnvironmentNode)
                extensions.attachSpatialEnvironment(
                    activity,
                    currentRootEnvironmentNode,
                    skyboxMode,
                    Runnable::run,
                ) {
                    // Update the root environment node to the current root node.
                    rootEnvironmentNode = currentRootEnvironmentNode
                }
            }

            newPreference
        }
    }

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true

        super.dispose()
        if (geometrySubspaceSplitEngine != null) {
            if (::materialOverride.isInitialized) {
                impressApi.clearMaterialOverride(
                    geometryImpressNode,
                    overriddenNodeName,
                    /* primitiveIndex= */ 0,
                )
            }
            extensions.createNodeTransaction().use { transaction ->
                transaction.setParent(geometrySubspaceSplitEngine!!.subspaceNode, null).apply()
            }
            splitEngineSubspaceManager.deleteSubspace(geometrySubspaceSplitEngine!!.subspaceId)
            geometrySubspaceSplitEngine = null
            impressApi.clearPreferredEnvironmentIblAsset()
        }

        geometrySubspaceSplitEngine = null
        geometrySubspaceImpressNode = null
        _spatialEnvironmentPreference.set(null)
        // TODO: b/376934871 - Check async results.
        extensions.detachSpatialEnvironment(activity, Runnable::run) {}
    }
}
