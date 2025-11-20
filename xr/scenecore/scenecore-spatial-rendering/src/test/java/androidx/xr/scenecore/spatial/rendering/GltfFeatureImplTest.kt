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

import androidx.concurrent.futures.ResolvableFuture
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl
import androidx.xr.scenecore.impl.impress.GltfModel
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.impl.impress.Material
import androidx.xr.scenecore.impl.impress.WaterMaterial
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.node.Node
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import com.google.androidxr.splitengine.SubspaceNode
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class GltfFeatureImplTest {

    private val xrExtensions = XrExtensionsProvider.getXrExtensions()!!
    private val executor = FakeScheduledExecutorService()
    private val mockImpressApi = mock(ImpressApi::class.java)
    private val fakeImpressApi = FakeImpressApiImpl()
    private val splitEngineSubspaceManager = Mockito.mock(SplitEngineSubspaceManager::class.java)
    private lateinit var modelImpressNode: ImpressNode

    private val subspaceNode: Node = xrExtensions.createNode()
    private val expectedSubspace = SubspaceNode(SUBSPACE_ID, subspaceNode)

    private lateinit var gltfFeature: GltfFeature

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setUp() {
        `when`(splitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
            .thenReturn(expectedSubspace)

        assertThat(xrExtensions).isNotNull()
        ShadowXrExtensions.extract(xrExtensions)
            .setOpenXrWorldSpaceType(OPEN_XR_REFERENCE_SPACE_TYPE)
        gltfFeature = createGltfFeature()
    }

    @After
    fun tearDown() {
        gltfFeature.dispose()
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun createGltfFeature(): GltfFeature {
        val modelTokenFuture: ListenableFuture<GltfModel> =
            fakeImpressApi.loadGltfAsset("FakeGltfAsset.glb")
        val model = modelTokenFuture.get()
        val modelToken = model.nativeHandle
        modelImpressNode = fakeImpressApi.instanceGltfModel(modelToken)
        `when`(mockImpressApi.createImpressNode()).thenReturn(fakeImpressApi.createImpressNode())
        `when`(mockImpressApi.instanceGltfModel(modelToken)).thenReturn(modelImpressNode)
        `when`(mockImpressApi.getGltfModelBoundingBox(modelImpressNode))
            .thenReturn(fakeImpressApi.getGltfModelBoundingBox(modelImpressNode))

        return GltfFeatureImpl(model, mockImpressApi, splitEngineSubspaceManager, xrExtensions)
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun createWaterMaterial(isAlphaMapVersion: Boolean): MaterialResource {
        val materialResourceFuture = ResolvableFuture.create<MaterialResource>()
        val materialFuture: ListenableFuture<WaterMaterial> =
            fakeImpressApi.createWaterMaterial(isAlphaMapVersion)

        val material = materialFuture.get()
        materialResourceFuture.set(material)

        return materialResourceFuture.get()
    }

    @Test
    fun startAnimation_startsAnimation() {
        val animationName = "test_animation"
        `when`(mockImpressApi.animateGltfModel(modelImpressNode, animationName, true))
            .thenReturn(fakeImpressApi.animateGltfModel(modelImpressNode, animationName, true))
        gltfFeature.startAnimation(/* looping= */ true, animationName, executor)

        assertThat(gltfFeature.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)
        verify(mockImpressApi).animateGltfModel(modelImpressNode, animationName, true)
    }

    @Test
    fun stopAnimation_stopsAnimation() {
        val animationName = "test_animation"
        `when`(mockImpressApi.animateGltfModel(modelImpressNode, animationName, true))
            .thenReturn(fakeImpressApi.animateGltfModel(modelImpressNode, animationName, true))
        gltfFeature.startAnimation(/* looping= */ true, animationName, executor)

        assertThat(gltfFeature.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)

        gltfFeature.stopAnimation()
        fakeImpressApi.stopGltfModelAnimation(modelImpressNode)

        assertThat(gltfFeature.animationState).isEqualTo(GltfEntity.AnimationState.STOPPED)
        verify(mockImpressApi).stopGltfModelAnimation(modelImpressNode)
    }

    @Test
    @Throws(Exception::class)
    fun setMaterialOverrideGltfEntity_materialOverridesNode() {
        val material = createWaterMaterial(/* isAlphaMapVersion= */ false)
        val nativeHandle = (material as Material).nativeHandle

        assertThat(material).isNotNull()

        val nodeName = "fake_node_name"
        val primitiveIndex = 0

        gltfFeature.setMaterialOverride(material, nodeName, primitiveIndex)
        fakeImpressApi.setMaterialOverride(modelImpressNode, nativeHandle, nodeName, primitiveIndex)

        verify(mockImpressApi)
            .setMaterialOverride(modelImpressNode, nativeHandle, nodeName, primitiveIndex)
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
            .hasLength(1)
    }

    @Test
    @Throws(Exception::class)
    fun clearMaterialOverrideGltfEntity_clearsMaterialOverride() {
        val material = createWaterMaterial(/* isAlphaMapVersion= */ false)
        val nodeName = "fake_node_name"
        val primitiveIndex = 0

        gltfFeature.setMaterialOverride(material, nodeName, primitiveIndex)
        gltfFeature.clearMaterialOverride(nodeName, primitiveIndex)

        assertThat(
                fakeImpressApi
                    .getImpressNodes()
                    .keys
                    .stream()
                    .filter { node -> node.materialOverride != null }
                    .toArray()
            )
            .isEmpty()
    }

    @Test
    @Throws(Exception::class)
    fun animationStateListener_isTriggeredOnAnimationStateChanges() {
        val animationName = "test_animation"
        `when`(mockImpressApi.animateGltfModel(modelImpressNode, animationName, true))
            .thenReturn(fakeImpressApi.animateGltfModel(modelImpressNode, animationName, true))
        val latestValue = AtomicReference(GltfEntity.AnimationState.STOPPED)
        gltfFeature.addAnimationStateListener(Runnable::run, latestValue::set)
        gltfFeature.startAnimation(/* looping= */ true, animationName, executor)

        assertThat(latestValue.get()).isEqualTo(GltfEntity.AnimationState.PLAYING)

        gltfFeature.stopAnimation()

        assertThat(latestValue.get()).isEqualTo(GltfEntity.AnimationState.STOPPED)
    }

    @Test
    @Throws(Exception::class)
    fun dispose_clearsOverridesAndDeletesSubspace() {
        val material = createWaterMaterial(/* isAlphaMapVersion= */ false)
        assertThat(material).isNotNull()
        val nativeHandle = (material as Material).nativeHandle
        val nodeName1 = "node1"
        val primitiveIndex1 = 0
        val nodeName2 = "node2"
        val primitiveIndex2 = 1

        gltfFeature.setMaterialOverride(material, nodeName1, primitiveIndex1)
        verify(mockImpressApi)
            .setMaterialOverride(modelImpressNode, nativeHandle, nodeName1, primitiveIndex1)

        gltfFeature.setMaterialOverride(material, nodeName2, primitiveIndex2)
        verify(mockImpressApi)
            .setMaterialOverride(modelImpressNode, nativeHandle, nodeName2, primitiveIndex2)

        gltfFeature.dispose()
        verify(mockImpressApi).clearMaterialOverride(modelImpressNode, nodeName1, primitiveIndex1)
        verify(mockImpressApi).clearMaterialOverride(modelImpressNode, nodeName2, primitiveIndex2)
        verify(splitEngineSubspaceManager).deleteSubspace(SUBSPACE_ID)
    }

    companion object {
        private const val OPEN_XR_REFERENCE_SPACE_TYPE = 1
        private const val SUBSPACE_ID = 5
    }
}
