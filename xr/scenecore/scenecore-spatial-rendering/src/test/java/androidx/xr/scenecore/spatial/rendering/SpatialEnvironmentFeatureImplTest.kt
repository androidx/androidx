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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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
import org.robolectric.shadows.ShadowLooper.runUiThreadTasks

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
    private suspend fun fakeLoadEnvironment(name: String): ExrImageResource? {
        try {
            return fakeImpressApi.loadImageBasedLightingAsset(name)
        } catch (e: Exception) {
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            return null
        }
    }

    @SuppressWarnings("FutureReturnValueIgnored", "AndroidJdkLibsChecker")
    private suspend fun fakeLoadGltfAsset(name: String): GltfModelResource? {
        try {
            return fakeImpressApi.loadGltfAsset(name)
        } catch (e: Exception) {
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            return null
        }
    }

    @SuppressWarnings("FutureReturnValueIgnored", "AndroidJdkLibsChecker")
    private suspend fun fakeLoadMaterial(isAlphaMapVersion: Boolean): MaterialResource? {
        try {
            return fakeImpressApi.createWaterMaterial(isAlphaMapVersion)
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
    fun setPreferredSpatialEnvironmentNull_removesEnvironment() = runBlocking {
        val exr = fakeLoadEnvironment("fakeEnvironmentAsset")
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")

        // Ensure that an environment is set.
        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(exr, gltf)

        val initialSkybox = fakeImpressApi.getCurrentEnvironmentLight()
        val geometryNodes = fakeImpressApi.getImpressNodesForToken((gltf as GltfModel).nativeHandle)
        val materials = fakeImpressApi.getMaterials()
        val animatingNodes = fakeImpressApi.impressNodeAnimatingSize()
        val loopingAnimatingNodes = fakeImpressApi.impressNodeLoopAnimatingSize()
        runUiThreadTasks()

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
    fun setPreferredSpatialEnvironmentWithNullSkyboxAndNullGeometry_doesNotDetachEnvironment() =
        runBlocking {
            val exr = fakeLoadEnvironment("fakeEnvironmentAsset")
            val gltf = fakeLoadGltfAsset("fakeGltfAsset")

            // Ensure that an environment is set.
            environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(exr, gltf)

            val initialSkybox = fakeImpressApi.getCurrentEnvironmentLight()
            val geometryNodes =
                fakeImpressApi.getImpressNodesForToken((gltf as GltfModel).nativeHandle)
            runUiThreadTasks()

            assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID)
            assertThat(geometryNodes).isNotEmpty()

            assertThat(fakeImpressApi.impressNodeHasParent(ImpressNode(geometryNodes!![0])))
                .isTrue()

            // Ensure environment is not removed if both skybox and geometry are updated to null.
            environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(null, null)

            val finalSkybox = fakeImpressApi.getCurrentEnvironmentLight()

            assertThat(finalSkybox).isEqualTo(INVALID_SPLIT_ENGINE_ID)
            assertThat(ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity))
                .isNotNull()
        }

    @Test
    fun setPreferredSpatialEnvWithSkyboxAndGeoWithNodeAndAnimation_doesNotDetachEnvironment() =
        runBlocking {
            val exr = fakeLoadEnvironment("fakeEnvironment")
            val gltf = fakeLoadGltfAsset("fakeGltfAsset")
            // Create dummy regular version of the water material.
            val material = fakeLoadMaterial(false)
            val nodeName = "fakeNode"
            val animationName = "fakeAnimation"

            // Ensure that an environment is set.
            environment.preferredSpatialEnvironment =
                SpatialEnvironmentPreference(exr, gltf, material, nodeName, animationName)
            runUiThreadTasks()

            val initialSkybox = fakeImpressApi.getCurrentEnvironmentLight()
            val geometryNodes =
                fakeImpressApi.getImpressNodesForToken((gltf as GltfModel).nativeHandle)
            val materials = fakeImpressApi.getMaterials()
            val animatingNodes = fakeImpressApi.impressNodeAnimatingSize()
            val loopingAnimatingNodes = fakeImpressApi.impressNodeLoopAnimatingSize()

            assertThat(initialSkybox).isNotEqualTo(INVALID_SPLIT_ENGINE_ID)
            assertThat(geometryNodes).isNotEmpty()
            assertThat(fakeImpressApi.impressNodeHasParent(ImpressNode(geometryNodes!![0])))
                .isTrue()
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
    fun setPreferredSpatialEnvFromNullPrefToNullSkyboxAndGeometry_doesNotDetachEnvironment() =
        runBlocking {
            val gltf = fakeLoadGltfAsset("fakeGltfAsset")

            // Ensure that an environment is set.
            environment.preferredSpatialEnvironment = null

            val initialSkybox = fakeImpressApi.getCurrentEnvironmentLight()
            val geometryNodes =
                fakeImpressApi.getImpressNodesForToken((gltf as GltfModel).nativeHandle)

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
    fun setNewSpatialEnvironmentPreference_replacesOldSpatialEnvironmentPreference() = runBlocking {
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
        runUiThreadTasks()

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
    fun setNewSpatialEnvironmentPreference_callsOnBeforeNodeAttachedListener() = runBlocking {
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")
        val timesCalled = AtomicInteger()

        environment.accept { timesCalled.getAndIncrement() }

        // Ensure that an environment is set a first time.
        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(null, gltf)

        assertThat(timesCalled.get()).isEqualTo(1)
    }

    @Test
    fun setPreferredSpatialEnvironmentGeometryWithMaterialAndNodeName_materialIsOverridden() =
        runTest {
            val exr = fakeLoadEnvironment("fakeEnvironment")
            val gltf = fakeLoadGltfAsset("fakeGltfAsset")
            // Create dummy regular version of the water material.
            val material = fakeLoadMaterial(false)
            val nodeName = "fakeNode"
            val animationName = "fakeAnimation"

            // Ensure that an environment is set.
            environment.preferredSpatialEnvironment =
                SpatialEnvironmentPreference(exr, gltf, material, nodeName, animationName)
            runUiThreadTasks()

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
    fun setPreferredSpatialEnvGeometryWithMaterialAndNoNodeName_materialIsNotOverridden() =
        runBlocking {
            val exr = fakeLoadEnvironment("fakeEnvironment")
            val gltf = fakeLoadGltfAsset("fakeGltfAsset")
            // Create dummy regular version of the water material.
            val material = fakeLoadMaterial(false)
            val animationName = "fakeAnimation"

            // Ensure that an environment is set.
            environment.preferredSpatialEnvironment =
                SpatialEnvironmentPreference(exr, gltf, material, null, animationName)

            val materials = fakeImpressApi.getMaterials()
            runUiThreadTasks()

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
    fun setPreferredSpatialEnvGeometryWithNoMaterialAndNodeName_materialIsNotOverridden() =
        runBlocking {
            val exr = fakeLoadEnvironment("fakeEnvironment")
            val gltf = fakeLoadGltfAsset("fakeGltfAsset")
            val nodeName = "fakeNode"
            val animationName = "fakeAnimation"

            // Ensure that an environment is set.
            environment.preferredSpatialEnvironment =
                SpatialEnvironmentPreference(exr, gltf, null, nodeName, animationName)

            val materials = fakeImpressApi.getMaterials()
            runUiThreadTasks()

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
    fun setPreferredSpatialEnvironmentGeometryWithNoAnimationName_geometryIsNotAnimating() =
        runTest {
            val exr = fakeLoadEnvironment("fakeEnvironment")
            val gltf = fakeLoadGltfAsset("fakeGltfAsset")
            val animationName = "fakeAnimation"

            // Ensure that an environment is set.
            environment.preferredSpatialEnvironment =
                SpatialEnvironmentPreference(exr, gltf, null, null, animationName)
            runUiThreadTasks()

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
    fun setPreferredSpatialEnvGeometry_setsSubspaceAsParentOfGltfNode() = runBlocking {
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")

        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(null, gltf)
        val subspaceHandleCaptor = ArgumentCaptor.forClass(Int::class.java)
        runUiThreadTasks()

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
    fun setPreferredSpatialEnvironment_asyncAnimation_startsAnimation() = runBlocking {
        val exr = fakeLoadEnvironment("fakeEnvironment")
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")
        val animationName = "fakeAnimation"

        environment.preferredSpatialEnvironment =
            SpatialEnvironmentPreference(exr, gltf, null, null, animationName)

        // Will execute animateGltfModel.
        runUiThreadTasks()

        val loopingAnimatingNodes = fakeImpressApi.impressNodeLoopAnimatingSize()
        assertThat(loopingAnimatingNodes).isEqualTo(1)
    }

    @Test
    fun setPreferredSpatialEnvironment_createsNewRootNode_whenGeometryChanges() = runBlocking {
        val gltf1 = fakeLoadGltfAsset("fakeGltfAsset1")
        val gltf2 = fakeLoadGltfAsset("fakeGltfAsset2")

        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(null, gltf1)
        runUiThreadTasks()

        val firstEnvNode = ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity)
        assertThat(firstEnvNode).isNotNull()

        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(null, gltf2)
        runUiThreadTasks()

        val secondEnvNode = ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity)
        assertThat(secondEnvNode).isNotNull()

        // Verify a new root node was attached.
        assertThat(secondEnvNode).isNotEqualTo(firstEnvNode)
    }

    @Test
    fun setPreferredSpatialEnvironment_reusesRootNode_whenGeometryIsSame() = runBlocking {
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")
        val skybox1 = fakeLoadEnvironment("fakeEnvironment1")
        val skybox2 = fakeLoadEnvironment("fakeEnvironment2")

        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(skybox1, gltf)
        runUiThreadTasks()
        val firstEnvNode = ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity)

        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(skybox2, gltf)
        runUiThreadTasks()
        val secondEnvNode = ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity)

        assertThat(secondEnvNode).isEqualTo(firstEnvNode)
    }

    @Test
    fun setPreferredSpatialEnvironment_detaches_whenPreferenceIsNull() = runBlocking {
        val gltf = fakeLoadGltfAsset("fakeGltfAsset")

        environment.preferredSpatialEnvironment = SpatialEnvironmentPreference(null, gltf)
        runUiThreadTasks()

        assertThat(ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity))
            .isNotNull()

        environment.preferredSpatialEnvironment = null
        runUiThreadTasks()

        assertThat(ShadowXrExtensions.extract(xrExtensions).getEnvironmentNode(activity)).isNull()
    }

    @Test
    fun dispose_clearsResources() = runBlocking {
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
        runUiThreadTasks()

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
