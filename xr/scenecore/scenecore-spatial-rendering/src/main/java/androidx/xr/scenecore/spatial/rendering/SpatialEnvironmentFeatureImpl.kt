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
import androidx.xr.runtime.Log
import androidx.xr.scenecore.impl.impress.ExrImage
import androidx.xr.scenecore.impl.impress.GltfModel
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.runtime.ExrImageResource
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfModelResource
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
import kotlinx.coroutines.cancel
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
        geometryResource: GltfModelResource?,
        geometryEntity: GltfEntity?,
        parentNode: Node,
    ) {
        check(Looper.getMainLooper().isCurrentThread) {
            "This method must be called on the main thread."
        }

        var targetSubspaceNode: Node? = null

        if (geometryEntity != null) {
            targetSubspaceNode = geometryEntity.extractedFeature?.subspace?.subspaceNode
            if (targetSubspaceNode == null) {
                Log.error(
                    "GltfModelEntity does not have a valid subspace, can't use it for the" +
                        " environment geometry."
                )
                return
            }
            geometryEntity.setHidden(false)
        } else if (geometryResource != null) {
            val subspaceNode = impressApi.createImpressNode()
            geometrySubspaceImpressNode = subspaceNode
            val subspaceName = "geometry_subspace_" + subspaceNode.handle

            geometrySubspaceSplitEngine =
                splitEngineSubspaceManager.createSubspace(subspaceName, subspaceNode.handle)

            targetSubspaceNode = geometrySubspaceSplitEngine?.subspaceNode

            val geometryImpressNode =
                impressApi.instanceGltfModel(
                    (geometryResource as GltfModel).nativeHandle,
                    /* enableCollider= */ false,
                )
            impressApi.setImpressNodeParent(geometryImpressNode, subspaceNode)
        }

        targetSubspaceNode?.let { subspace ->
            extensions.createNodeTransaction().use { transaction ->
                transaction
                    .setName(subspace, GEOMETRY_NODE_NAME)
                    .setPosition(subspace, 0.0f, 0.0f, 0.0f)
                    .setScale(subspace, 1.0f, 1.0f, 1.0f)
                    .setOrientation(subspace, 0.0f, 0.0f, 0.0f, 1.0f)
                    .setParent(subspace, parentNode)
                    .apply()
            }
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
            val newGeometryEntity = newPreference?.geometryEntity
            val prevGeometryEntity = prevPreference?.geometryEntity

            // TODO: b/392948759 - Fix StrictMode violations triggered whenever skybox is
            // set.
            if (newSkybox != prevSkybox || prevPreference == null) {
                applySkybox(newSkybox)
            }

            if (newPreference == null) {
                // Detaching the app environment to go back to the system environment.
                extensions.detachSpatialEnvironment(activity, Runnable::run) {}
            } else {
                // TODO(b/408276187): Add unit test that verifies that the skybox mode is correctly
                // set.
                var skyboxMode = XrExtensions.ENVIRONMENT_SKYBOX_APP
                if (newSkybox == null) {
                    skyboxMode = XrExtensions.NO_SKYBOX
                }
                // Transitioning to a new app environment.
                val currentRootEnvironmentNode: Node
                if (newGeometry != prevGeometry || newGeometryEntity != prevGeometryEntity) {
                    // Environment geometry has changed, create a new environment node and attach
                    // the geometry subspace to it.
                    currentRootEnvironmentNode = extensions.createNode()
                    if (newGeometry != null || newGeometryEntity != null) {
                        coroutineScope.launch {
                            applyGeometry(
                                geometryResource = newGeometry,
                                geometryEntity = newGeometryEntity,
                                parentNode = currentRootEnvironmentNode,
                            )
                        }
                    }
                } else {
                    // Environment geometry has not changed, use the existing environment node.
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

        coroutineScope.cancel()

        val currentEntity = _spatialEnvironmentPreference.get()?.geometryEntity
        currentEntity?.extractedFeature?.subspace?.subspaceNode?.let { subspaceNode ->
            extensions.createNodeTransaction().use { transaction ->
                transaction.setParent(subspaceNode, null).apply()
            }
        }

        if (geometrySubspaceSplitEngine != null) {
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

    // This is a workaround with a low blast radius since it will only ever be used by restricted
    // APIs.
    // TODO(b/486200886): Revisit glTF Entity/Feature architecture to avoid having to access feature
    // fields by reflection.
    private val GltfEntity.extractedFeature: BaseRenderingFeature?
        get() =
            try {
                val field = this.javaClass.getDeclaredField("gltfFeature")
                field.isAccessible = true
                field.get(this) as? BaseRenderingFeature
            } catch (e: Exception) {
                null
            }
}
