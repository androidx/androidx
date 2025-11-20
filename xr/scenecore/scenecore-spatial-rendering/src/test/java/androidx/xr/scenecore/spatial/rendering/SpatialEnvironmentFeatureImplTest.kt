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
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl
import androidx.xr.scenecore.impl.impress.GltfModel
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.runtime.ExrImageResource
import androidx.xr.scenecore.runtime.GltfModelResource
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.runtime.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.environment.EnvironmentVisibilityState
import com.android.extensions.xr.environment.PassthroughVisibilityState
import com.android.extensions.xr.environment.ShadowEnvironmentVisibilityState
import com.android.extensions.xr.environment.ShadowPassthroughVisibilityState
import com.android.extensions.xr.space.ShadowSpatialState
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import com.google.androidxr.splitengine.SubspaceNode
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Technically this doesn't need to be a Robolectric test, since it doesn't directly depend on
// any Android subsystems. However, we're currently using an Android test runner for consistency
// with other Android XR impl tests in this directory.
/** Unit tests for the AndroidXR implementation of JXRCore's SpatialEnvironment module. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SpatialEnvironmentFeatureImplTest {

    private val fakeImpressApi = FakeImpressApiImpl()
    private lateinit var activity: Activity
    private val xrExtensions = XrExtensionsProvider.getXrExtensions()!!
    private lateinit var expectedSubspace: SubspaceNode
    private lateinit var environment: SpatialEnvironmentFeatureImpl
    private lateinit var splitEngineSubspaceManager: SplitEngineSubspaceManager

    @Before
    fun setUp() {
        val activityController = Robolectric.buildActivity(Activity::class.java)
        activity = activityController.create().start().get()
        // Reset our state.
        xrExtensions.createNode()
        val subspaceNode = xrExtensions.createNode()
        expectedSubspace = SubspaceNode(SUBSPACE_ID, subspaceNode)

        splitEngineSubspaceManager = Mockito.mock(SplitEngineSubspaceManager::class.java)
        `when`(splitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
            .thenReturn(expectedSubspace)

        environment =
            SpatialEnvironmentFeatureImpl(
                activity,
                fakeImpressApi,
                splitEngineSubspaceManager,
                xrExtensions,
            )
    }

    @SuppressWarnings("FutureReturnValueIgnored", "AndroidJdkLibsChecker")
    private fun fakeLoadEnvironment(name: String): ExrImageResource? {
        try {
            return fakeImpressApi.loadImageBasedLightingAsset(name).get()
        } catch (e: Exception) {
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            return null
        }
    }

    @SuppressWarnings("FutureReturnValueIgnored", "AndroidJdkLibsChecker")
    private fun fakeLoadGltfAsset(name: String): GltfModelResource? {
        try {
            return fakeImpressApi.loadGltfAsset(name).get()
        } catch (e: Exception) {
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            return null
        }
    }

    @SuppressWarnings("FutureReturnValueIgnored", "AndroidJdkLibsChecker")
    private fun fakeLoadMaterial(isAlphaMapVersion: Boolean): MaterialResource? {
        try {
            return fakeImpressApi.createWaterMaterial(isAlphaMapVersion).get()
        } catch (e: Exception) {
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            return null
        }
    }

    @Test
    fun getPreferredSpatialEnvironment_returnsSetPreferredSpatialEnvironment() {
        val preference = SpatialEnvironmentPreference(null, null)
        environment.preferredSpatialEnvironment = preference

        assertThat(environment.preferredSpatialEnvironment).isEqualTo(preference)
    }

    @Test
    fun setPreferredSpatialEnvironmentNull_removesEnvironment() {
        val exr = fakeLoadEnvironment("fakeEnvironmentAsset")
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")

        // Ensure that an environment is set.
        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(exr, gltf)

        val initialSkybox = fakeImpressApi.getCurrentEnvironmentLight()
        val geometryNodes = fakeImpressApi.getImpressNodesForToken((gltf as GltfModel).nativeHandle)
        val materials = fakeImpressApi.getMaterials()
        val animatingNodes = fakeImpressApi.impressNodeAnimatingSize()
        val loopingAnimatingNodes = fakeImpressApi.impressNodeLoopAnimatingSize()

        assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID)
        assertThat(geometryNodes).isNotEmpty()
        assertThat(materials).isEmpty()
        assertThat(animatingNodes).isEqualTo(0)
        assertThat(loopingAnimatingNodes).isEqualTo(0)

        assertThat(fakeImpressApi.impressNodeHasParent(ImpressNode(geometryNodes!![0]))).isTrue()

        // Ensure environment is removed
        environment.preferredSpatialEnvironment = null

        val finalSkybox = fakeImpressApi.getCurrentEnvironmentLight()

        assertThat(finalSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID)
        assertThat(ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity)).isNull()
    }

    @Test
    fun setPreferredSpatialEnvironmentWithNullSkyboxAndNullGeometry_doesNotDetachEnvironment() {
        val exr = fakeLoadEnvironment("fakeEnvironmentAsset")
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")

        // Ensure that an environment is set.
        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(exr, gltf)

        val initialSkybox = fakeImpressApi.getCurrentEnvironmentLight()
        val geometryNodes = fakeImpressApi.getImpressNodesForToken((gltf as GltfModel).nativeHandle)

        assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID)
        assertThat(geometryNodes).isNotEmpty()

        assertThat(fakeImpressApi.impressNodeHasParent(ImpressNode(geometryNodes!![0]))).isTrue()

        // Ensure environment is not removed if both skybox and geometry are updated to null.
        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(null, null)

        val finalSkybox = fakeImpressApi.getCurrentEnvironmentLight()

        assertThat(finalSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID)
        assertThat(ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity))
            .isNotNull()
    }

    @Test
    fun setPreferredSpatialEnvWithSkyboxAndGeoWithNodeAndAnimation_doesNotDetachEnvironment() {
        val exr = fakeLoadEnvironment("fakeEnvironment")
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")
        // Create dummy regular version of the water material.
        val material = fakeLoadMaterial(false)
        val nodeName = "fakeNode"
        val animationName = "fakeAnimation"

        // Ensure that an environment is set.
        environment.preferredSpatialEnvironment =
            SpatialEnvironmentPreference(exr, gltf, material, nodeName, animationName)

        val initialSkybox = fakeImpressApi.getCurrentEnvironmentLight()
        val geometryNodes = fakeImpressApi.getImpressNodesForToken((gltf as GltfModel).nativeHandle)
        val materials = fakeImpressApi.getMaterials()
        val animatingNodes = fakeImpressApi.impressNodeAnimatingSize()
        val loopingAnimatingNodes = fakeImpressApi.impressNodeLoopAnimatingSize()

        assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID)
        assertThat(geometryNodes).isNotEmpty()
        assertThat(fakeImpressApi.impressNodeHasParent(ImpressNode(geometryNodes!![0]))).isTrue()
        assertThat(materials).isNotEmpty()
        assertThat(materials.keys.toTypedArray()[0]).isEqualTo(WATER_MATERIAL_ID)
        assertThat(materials[WATER_MATERIAL_ID]!!.type)
            .isEqualTo(FakeImpressApiImpl.MaterialData.Type.WATER)
        assertThat(animatingNodes).isEqualTo(0)
        assertThat(loopingAnimatingNodes).isEqualTo(1)

        // Ensure environment is not removed if both skybox and geometry are updated to null.
        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(null, null)

        val finalSkybox = fakeImpressApi.getCurrentEnvironmentLight()

        assertThat(finalSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID)
        assertThat(ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity))
            .isNotNull()
    }

    @Test
    fun setPreferredSpatialEnvFromNullPrefToNullSkyboxAndGeometry_doesNotDetachEnvironment() {
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")

        // Ensure that an environment is set.
        environment.preferredSpatialEnvironment = null

        val initialSkybox = fakeImpressApi.getCurrentEnvironmentLight()
        val geometryNodes = fakeImpressApi.getImpressNodesForToken((gltf as GltfModel).nativeHandle)

        assertThat(initialSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID)
        assertThat(geometryNodes).isEmpty()

        // Ensure environment is not removed if both skybox and geometry are updated to null.
        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(null, null)

        val finalSkybox = fakeImpressApi.getCurrentEnvironmentLight()

        assertThat(finalSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID)
        assertThat(ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity))
            .isNotNull()
    }

    @Test
    fun setNewSpatialEnvironmentPreference_replacesOldSpatialEnvironmentPreference() {
        val exr = fakeLoadEnvironment("fakeEnvironment")
        val newExr = fakeLoadEnvironment("newFakeEnvironment")
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")
        val newGltf = fakeLoadGltfAsset("newFakeGltfAsset")

        // Ensure that an environment is set a first time.
        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(exr, gltf)

        val initialSkybox = fakeImpressApi.getCurrentEnvironmentLight()
        val geometryNodes = fakeImpressApi.getImpressNodesForToken((gltf as GltfModel).nativeHandle)

        // Ensure that an environment is set a second time.
        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(newExr, newGltf)

        val newSkybox = fakeImpressApi.getCurrentEnvironmentLight()
        val newGeometryNodes =
            fakeImpressApi.getImpressNodesForToken((newGltf as GltfModel).nativeHandle)

        // None of the nodes should be null.
        assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID)
        assertThat(geometryNodes).isNotEmpty()
        assertThat(newSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID)
        assertThat(newGeometryNodes).isNotEmpty()
        // Only the new nodes should have a parent.
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();
        assertThat(fakeImpressApi.impressNodeHasParent(ImpressNode(newGeometryNodes!![0]))).isTrue()
        // The resources should be different.
        assertThat(initialSkybox).isNotEqualTo(newSkybox)
        assertThat(geometryNodes!![0]).isNotEqualTo(newGeometryNodes!![0])
        // The environment node should still be attached.
        assertThat(ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity))
            .isNotNull()
    }

    @Test
    fun setNewSpatialEnvironmentPreference_callsOnBeforeNodeAttachedListener() {
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")
        val timesCalled = AtomicInteger()

        environment.accept { timesCalled.getAndIncrement() }

        // Ensure that an environment is set a first time.
        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(null, gltf)

        assertThat(timesCalled.get()).isEqualTo(1)
    }

    @Test
    fun setPreferredSpatialEnvironmentGeometryWithMaterialAndNodeName_materialIsOverriden() {
        val exr = fakeLoadEnvironment("fakeEnvironment")
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")
        // Create dummy regular version of the water material.
        val material = fakeLoadMaterial(false)
        val nodeName = "fakeNode"
        val animationName = "fakeAnimation"

        // Ensure that an environment is set.
        environment.preferredSpatialEnvironment =
            SpatialEnvironmentPreference(exr, gltf, material, nodeName, animationName)

        val materials = fakeImpressApi.getMaterials()
        val loopingAnimatingNodes = fakeImpressApi.impressNodeLoopAnimatingSize()

        assertThat(
                fakeImpressApi
                    .getImpressNodes()
                    .keys
                    .stream()
                    .filter { node ->
                        node.materialOverride != null &&
                            node.materialOverride!!.type ==
                                FakeImpressApiImpl.MaterialData.Type.WATER
                    }
                    .toArray()
            )
            .hasLength(1) // 1 glTF node that should be overridden with the water material.
        assertThat(materials).isNotEmpty()
        assertThat(materials.keys.toTypedArray()[0]).isEqualTo(WATER_MATERIAL_ID)
        assertThat(materials[WATER_MATERIAL_ID]!!.type)
            .isEqualTo(FakeImpressApiImpl.MaterialData.Type.WATER)
        assertThat(loopingAnimatingNodes).isEqualTo(1)
    }

    @Test
    fun setPreferredSpatialEnvGeometryWithMaterialAndNoNodeName_materialIsNotOverriden() {
        val exr = fakeLoadEnvironment("fakeEnvironment")
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")
        // Create dummy regular version of the water material.
        val material = fakeLoadMaterial(false)
        val animationName = "fakeAnimation"

        // Ensure that an environment is set.
        environment.preferredSpatialEnvironment =
            SpatialEnvironmentPreference(exr, gltf, material, null, animationName)

        val materials = fakeImpressApi.getMaterials()

        // 2 nodes are subspace (parent) and glTF (child) used for the environment. Both have no
        // material override so we expect the length of the filter to be 2.
        assertThat(
                fakeImpressApi
                    .getImpressNodes()
                    .keys
                    .stream()
                    .filter { node -> node.materialOverride == null }
                    .toArray()
            )
            .hasLength(2)
        assertThat(materials).isNotEmpty()
        assertThat(materials.keys.toTypedArray()[0]).isEqualTo(WATER_MATERIAL_ID)
        assertThat(materials[WATER_MATERIAL_ID]!!.type)
            .isEqualTo(FakeImpressApiImpl.MaterialData.Type.WATER)
    }

    @Test
    fun setPreferredSpatialEnvGeometryWithNoMaterialAndNodeName_materialIsNotOverriden() {
        val exr = fakeLoadEnvironment("fakeEnvironment")
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")
        val nodeName = "fakeNode"
        val animationName = "fakeAnimation"

        // Ensure that an environment is set.
        environment.preferredSpatialEnvironment =
            SpatialEnvironmentPreference(exr, gltf, null, nodeName, animationName)

        val materials = fakeImpressApi.getMaterials()

        // 2 nodes are subspace (parent) and glTF (child) used for the environment. Both have no
        // material override so we expect the length of the filter to be 2.
        assertThat(
                fakeImpressApi
                    .getImpressNodes()
                    .keys
                    .stream()
                    .filter { node -> node.materialOverride == null }
                    .toArray()
            )
            .hasLength(2)
        assertThat(materials).isEmpty()
    }

    @Test
    fun setPreferredSpatialEnvironmentGeometryWithNoAnimationName_geometryIsNotAnimating() {
        val exr = fakeLoadEnvironment("fakeEnvironment")
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")
        val animationName = "fakeAnimation"

        // Ensure that an environment is set.
        environment.preferredSpatialEnvironment =
            SpatialEnvironmentPreference(exr, gltf, null, null, animationName)

        val loopingAnimatingNodes = fakeImpressApi.impressNodeLoopAnimatingSize()
        val materials = fakeImpressApi.getMaterials()

        assertThat(loopingAnimatingNodes).isEqualTo(1)
        // 2 nodes are subspace (parent) and glTF (child) used for the environment. Both have no
        // material override so we expect the length of the filter to be 2.
        assertThat(
                fakeImpressApi
                    .getImpressNodes()
                    .keys
                    .stream()
                    .filter { node -> node.materialOverride == null }
                    .toArray()
            )
            .hasLength(2)
        assertThat(materials).isEmpty()
    }

    @Test
    fun setPreferredSpatialEnvGeometry_setsSubspaceAsParentOfGltfNode() {
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")

        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(null, gltf)
        val subspaceHandleCaptor = ArgumentCaptor.forClass(Int::class.java)
        verify(splitEngineSubspaceManager)
            .createSubspace(anyString(), subspaceHandleCaptor.capture())
        val expectedParentHandle = subspaceHandleCaptor.value
        val geometryNodes = fakeImpressApi.getImpressNodesForToken((gltf as GltfModel).nativeHandle)
        val gltfNode = ImpressNode(geometryNodes!![0])
        val actualParentHandle = fakeImpressApi.getImpressNodeParent(gltfNode)

        assertThat(actualParentHandle).isNotEqualTo(-1)
        assertThat(actualParentHandle).isNotEqualTo(gltfNode.handle)
        assertThat(actualParentHandle).isEqualTo(expectedParentHandle)
    }

    @Test
    fun dispose_clearsResources() {
        val exr = fakeLoadEnvironment("fakeEnvironment")
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")
        val spatialState = ShadowSpatialState.create()
        ShadowSpatialState.extract(spatialState)
            .setEnvironmentVisibilityState(
                /* environmentVisibilityState= */ ShadowEnvironmentVisibilityState.create(
                    EnvironmentVisibilityState.APP_VISIBLE
                )
            )
        ShadowSpatialState.extract(spatialState)
            .setPassthroughVisibilityState(
                /* passthroughVisibilityState= */ ShadowPassthroughVisibilityState.create(
                    PassthroughVisibilityState.APP,
                    0.5f,
                )
            )

        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(exr, gltf)

        val initialSkybox = fakeImpressApi.getCurrentEnvironmentLight()
        val geometryNodes = fakeImpressApi.getImpressNodesForToken((gltf as GltfModel).nativeHandle)

        assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID)
        assertThat(geometryNodes).isNotEmpty()
        assertThat(fakeImpressApi.impressNodeHasParent(ImpressNode(geometryNodes!![0]))).isTrue()
        assertThat(environment.preferredSpatialEnvironment).isNotNull()

        environment.dispose()

        val finalSkybox = fakeImpressApi.getCurrentEnvironmentLight()

        assertThat(finalSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID)
        // TODO: b/354711945 - Uncomment when we can test the SetGeometrySplitEngine(null) path.
        // assertThat(fakeImpressApi.impressNodeHasParent(geometryNodes.get(0))).isFalse();
        assertThat(ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity)).isNull()
        assertThat(environment.preferredSpatialEnvironment).isNull()
    }

    companion object {
        private const val SUBSPACE_ID = 5
        private const val INVALID_SPLIT_ENGINE_ID = -1
        private const val WATER_MATERIAL_ID = 1L
    }
}
