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
import android.widget.FrameLayout
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.impl.impress.ExrImage
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl
import androidx.xr.scenecore.impl.impress.GltfModel
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.impl.impress.Material
import androidx.xr.scenecore.impl.impress.Texture
import androidx.xr.scenecore.runtime.GltfEntity
import androidx.xr.scenecore.runtime.MaterialResource
import androidx.xr.scenecore.runtime.RenderingEntityFactory
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.TextureResource
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider
import androidx.xr.scenecore.testing.FakeSceneRuntime
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.ShadowXrExtensions
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [SpatialRenderingRuntime]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
// TODO: b/441552980 - add unit tests for gltf animations
class SpatialRenderingRuntimeTest {

    // Use @JvmField for rules to avoid needing a getter
    @Rule @JvmField val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private lateinit var sceneRuntime: SceneRuntime
    private lateinit var renderingRuntime: RenderingRuntime
    private lateinit var spatialRenderingRuntime: SpatialRenderingRuntime
    private lateinit var renderingEntityFactory: RenderingEntityFactory
    private lateinit var activity: Activity

    private val fakeExecutor = FakeScheduledExecutorService()
    private val fakeImpressApi = FakeImpressApiImpl()

    @Mock private lateinit var splitEngineSubspaceManager: SplitEngineSubspaceManager
    @Mock private lateinit var splitEngineRenderer: ImpSplitEngineRenderer

    private val xrExtensions = XrExtensionsProvider.getXrExtensions()!!
    private var modelImpressNode: ImpressNode? = null

    companion object {
        private const val OPEN_XR_REFERENCE_SPACE_TYPE = 1
    }

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
        activity.setContentView(FrameLayout(activity))
        ShadowXrExtensions.extract(xrExtensions)
            .setOpenXrWorldSpaceType(OPEN_XR_REFERENCE_SPACE_TYPE)
        val fakeSceneRuntime = FakeSceneRuntime(fakeExecutor)
        sceneRuntime = fakeSceneRuntime

        assertThat(fakeSceneRuntime).isNotNull()

        spatialRenderingRuntime =
            SpatialRenderingRuntime.create(
                sceneRuntime,
                activity,
                fakeImpressApi,
                splitEngineSubspaceManager,
                splitEngineRenderer,
            )
        renderingRuntime = spatialRenderingRuntime
        renderingEntityFactory = sceneRuntime as RenderingEntityFactory
    }

    @After
    fun tearDown() {
        // Dispose the runtime between test cases to clean up lingering references.
        try {
            renderingRuntime.destroy()
            sceneRuntime.destroy()
        } catch (e: Exception) {
            // Tests which already call dispose will cause a NPE here due to Activity being null
            // when detaching from the scene.
        }
    }

    private fun createGltfEntity(
        pose: Pose = Pose(),
        impressApi: ImpressApi = fakeImpressApi,
    ): GltfEntity {
        var feature: GltfFeatureImpl? = null

        runBlocking {
            val gltfModel = renderingRuntime.loadGltfByAssetName("FakeAsset.glb")

            assertThat(gltfModel).isNotNull()

            if (impressApi == fakeImpressApi) {
                val modelToken = (gltfModel as GltfModel).nativeHandle
                modelImpressNode = fakeImpressApi.instanceGltfModel(modelToken)
            } else {
                val modelToken = (gltfModel as GltfModel).nativeHandle
                modelImpressNode = fakeImpressApi.instanceGltfModel(modelToken)
                `when`(impressApi.createImpressNode())
                    .thenReturn(fakeImpressApi.createImpressNode())
                `when`(impressApi.instanceGltfModel(modelToken)).thenReturn(modelImpressNode)
                `when`(impressApi.getGltfModelBoundingBox(modelImpressNode!!))
                    .thenReturn(fakeImpressApi.getGltfModelBoundingBox(modelImpressNode!!))
            }

            feature =
                GltfFeatureImpl(
                    gltfModel,
                    impressApi,
                    splitEngineSubspaceManager,
                    xrExtensions,
                    splitEngineRenderer,
                )
        }

        return renderingEntityFactory.createGltfEntity(feature!!, pose, sceneRuntime.activitySpace)
    }

    private fun loadTexture(): TextureResource {
        var texture: TextureResource? = null
        runBlocking {
            texture = renderingRuntime.loadTexture("FakeTexture.png")

            assertThat(texture).isNotNull()
        }
        return texture!!
    }

    private fun createWaterMaterial(): MaterialResource {
        var material: MaterialResource? = null
        runBlocking {
            material = renderingRuntime.createWaterMaterial(/* isAlphaMapVersion= */ false)

            assertThat(material).isNotNull()
        }
        return material!!
    }

    @Test
    fun loadExrImageByAssetName_returnsModel() {
        runBlocking {
            val image = renderingRuntime.loadExrImageByAssetName("FakeAsset.zip")
            val imageImpl = image as ExrImage

            assertThat(image).isNotNull()
            assertThat(imageImpl).isNotNull()

            val token = imageImpl.nativeHandle

            assertThat(token).isEqualTo(1)
        }
    }

    @Test
    fun loadExrImageByByteArray_returnsModel() {
        runBlocking {
            val image =
                renderingRuntime.loadExrImageByByteArray(byteArrayOf(1, 2, 3), "FakeAsset.zip")
            val imageImpl = image as ExrImage

            assertThat(image).isNotNull()
            assertThat(imageImpl).isNotNull()

            val token = imageImpl.nativeHandle

            assertThat(token).isEqualTo(1)
        }
    }

    @Test
    fun loadGltfByByteArray_returnsModel() {
        val model = runBlocking {
            renderingRuntime.loadGltfByByteArray(byteArrayOf(1, 2, 3), "FakeAsset.glb")
        }
        assertThat(model).isNotNull()

        val modelImpl = model as GltfModel
        assertThat(modelImpl).isNotNull()
        val token = modelImpl.nativeHandle
        assertThat(token).isEqualTo(1)
    }

    @Test
    fun createGltfEntity_returnsEntity() {
        assertThat(createGltfEntity(impressApi = mock(ImpressApi::class.java))).isNotNull()
    }

    @Test
    fun animateGltfEntity_gltfEntityIsAnimating() {
        val mockImpressApi = mock(ImpressApi::class.java)

        runBlocking {
            Mockito.doAnswer { COROUTINE_SUSPENDED }
                .`when`(mockImpressApi)
                .animateGltfModel(
                    any(ImpressNode::class.java) ?: fakeImpressApi.createImpressNode(),
                    Mockito.anyString(),
                    Mockito.eq(false),
                )
        }

        val gltfEntity = createGltfEntity(impressApi = mockImpressApi)
        gltfEntity.startAnimation(false, "animation_name")
        fakeExecutor.runAll()
        val loopingAnimatingNodes = fakeImpressApi.impressNodeLoopAnimatingSize()

        // The fakeJniApi returns a future which immediately fires, which makes it seem like the
        // animation is done immediately. This makes it look like the animation stopped right away.
        assertThat(gltfEntity.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)
        assertThat(loopingAnimatingNodes).isEqualTo(0)
    }

    @Test
    fun animateLoopGltfEntity_gltfEntityIsAnimatingInLoop() {
        val mockImpressApi = mock(ImpressApi::class.java)

        runBlocking {
            Mockito.doAnswer { COROUTINE_SUSPENDED }
                .`when`(mockImpressApi)
                .animateGltfModel(
                    any(ImpressNode::class.java) ?: fakeImpressApi.createImpressNode(),
                    Mockito.anyString(),
                    Mockito.eq(true),
                )
        }

        val gltfEntity = createGltfEntity(impressApi = mockImpressApi)
        gltfEntity.startAnimation(true, "animation_name")
        val animatingNodes = fakeImpressApi.impressNodeAnimatingSize()

        assertThat(gltfEntity.animationState).isEqualTo(GltfEntity.AnimationState.PLAYING)
        assertThat(animatingNodes).isEqualTo(0)
    }

    @Test
    fun stopAnimateGltfEntity_gltfEntityStopsAnimating() {
        val mockImpressApi = mock(ImpressApi::class.java)

        runBlocking {
            Mockito.doAnswer { COROUTINE_SUSPENDED }
                .`when`(mockImpressApi)
                .animateGltfModel(
                    any(ImpressNode::class.java) ?: fakeImpressApi.createImpressNode(),
                    Mockito.anyString(),
                    Mockito.eq(false),
                )
        }

        val gltfEntity = createGltfEntity(impressApi = mockImpressApi)
        gltfEntity.startAnimation(true, "animation_name")
        gltfEntity.stopAnimation()
        val animatingNodes = fakeImpressApi.impressNodeAnimatingSize()
        val loopingAnimatingNodes = fakeImpressApi.impressNodeLoopAnimatingSize()

        assertThat(gltfEntity.animationState).isEqualTo(GltfEntity.AnimationState.STOPPED)
        assertThat(animatingNodes).isEqualTo(0)
        assertThat(loopingAnimatingNodes).isEqualTo(0)
    }

    @Test
    fun createSurfaceEntity_returnsStereoSurface() {
        val kTestWidth = 14.0f
        val kTestHeight = 28.0f
        val kTestSphereRadius = 7.0f
        val kTestHemisphereRadius = 11.0f

        val surfaceEntityQuad =
            renderingRuntime.createSurfaceEntity(
                SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                SurfaceEntity.MediaBlendingMode.TRANSPARENT,
                Pose(),
                SurfaceEntity.Shape.Quad(FloatSize2d(kTestWidth, kTestHeight)),
                SurfaceEntity.SurfaceProtection.NONE,
                SurfaceEntity.SuperSampling.DEFAULT,
                sceneRuntime.activitySpace,
            )

        assertThat(surfaceEntityQuad).isNotNull()

        val surfaceEntitySphere =
            renderingRuntime.createSurfaceEntity(
                SurfaceEntity.StereoMode.TOP_BOTTOM,
                SurfaceEntity.MediaBlendingMode.TRANSPARENT,
                Pose(),
                SurfaceEntity.Shape.Sphere(kTestSphereRadius),
                SurfaceEntity.SurfaceProtection.NONE,
                SurfaceEntity.SuperSampling.DEFAULT,
                sceneRuntime.activitySpace,
            )

        assertThat(surfaceEntitySphere).isNotNull()

        val surfaceEntityHemisphere =
            renderingRuntime.createSurfaceEntity(
                SurfaceEntity.StereoMode.MONO,
                SurfaceEntity.MediaBlendingMode.TRANSPARENT,
                Pose(),
                SurfaceEntity.Shape.Hemisphere(kTestHemisphereRadius),
                SurfaceEntity.SurfaceProtection.NONE,
                SurfaceEntity.SuperSampling.DEFAULT,
                sceneRuntime.activitySpace,
            )

        assertThat(surfaceEntityHemisphere).isNotNull()
        assertThat(fakeImpressApi.getStereoSurfaceEntities()).hasSize(3)
    }

    @Test
    fun loadTexture_returnsTexture() {
        assertThat(loadTexture()).isNotNull()
    }

    @Test
    fun destroyTexture_removesTexture() {
        val texture = loadTexture() as Texture
        val initialTextureCount = fakeImpressApi.getTextureImages().size

        fakeImpressApi.destroyNativeObject(texture.nativeHandle)

        val finalTextureCount = fakeImpressApi.getTextureImages().size
        assertThat(finalTextureCount).isEqualTo(initialTextureCount - 1)
    }

    @Test
    fun createWaterMaterial_returnsWaterMaterial() {
        assertThat(createWaterMaterial()).isNotNull()
    }

    @Test
    fun destroyWaterMaterial_removesWaterMaterial() {
        val material = createWaterMaterial() as Material
        val initialMaterialCount = fakeImpressApi.getMaterials().size

        fakeImpressApi.destroyNativeObject(material.nativeHandle)

        val finalMaterialCount = fakeImpressApi.getMaterials().size
        assertThat(finalMaterialCount).isEqualTo(initialMaterialCount - 1)
    }

    @Test
    fun setMaterialOverrideGltfEntity_materialOverridesNode() {
        val gltfEntity = createGltfEntity()
        val material = createWaterMaterial()
        val primitiveIndex = 0
        val modelToken = fakeImpressApi.getGltfModels().keys.first()
        val instanceId = fakeImpressApi.getGltfModels()[modelToken]!!.last()
        val rootNode = ImpressNode(instanceId)
        val childNode = fakeImpressApi.createImpressNode()
        fakeImpressApi.setImpressNodeParent(childNode, rootNode)

        gltfEntity.nodes.first().setMaterialOverride(material, primitiveIndex)

        val overriddenNodes =
            fakeImpressApi.getImpressNodes().keys.filter { node ->
                node.nodeMaterialOverrides.containsKey(primitiveIndex) &&
                    node.nodeMaterialOverrides[primitiveIndex]?.type ==
                        FakeImpressApiImpl.MaterialData.Type.WATER
            }
        assertThat(overriddenNodes).hasSize(1)
    }

    @Test
    fun clearMaterialOverrideGltfEntity_clearsMaterialOverride() {
        val gltfEntity = createGltfEntity()
        val material = createWaterMaterial()
        val primitiveIndex = 0
        val modelToken = fakeImpressApi.getGltfModels().keys.first()
        val instanceId = fakeImpressApi.getGltfModels()[modelToken]!!.last()
        val rootNode = ImpressNode(instanceId)
        val childNode = fakeImpressApi.createImpressNode()
        fakeImpressApi.setImpressNodeParent(childNode, rootNode)

        val nodeFeature = gltfEntity.nodes.first()
        nodeFeature.setMaterialOverride(material, primitiveIndex)
        nodeFeature.clearMaterialOverride(primitiveIndex)

        val overriddenNodes =
            fakeImpressApi.getImpressNodes().keys.filter {
                it.nodeMaterialOverrides.containsKey(primitiveIndex)
            }
        assertThat(overriddenNodes).isEmpty()
    }

    @Test
    fun dispose_clearsAllApiResources() = runBlocking {
        renderingRuntime.loadExrImageByAssetName("FakeAsset.zip")
        renderingRuntime.loadGltfByAssetName("FakeAsset.glb")
        createWaterMaterial()
        createGltfEntity()

        fakeExecutor.runAll()

        assertThat(fakeImpressApi.getImageBasedLightingAssets()).isNotEmpty()
        assertThat(fakeImpressApi.getGltfModels()).isNotEmpty()
        assertThat(fakeImpressApi.getMaterials()).isNotEmpty()
        assertThat(fakeImpressApi.getImpressNodes()).isNotEmpty()

        renderingRuntime.destroy()

        assertThat(fakeImpressApi.getImageBasedLightingAssets()).isEmpty()
        assertThat(fakeImpressApi.getGltfModels()).isEmpty()
        assertThat(fakeImpressApi.getMaterials()).isEmpty()
        assertThat(fakeImpressApi.getImpressNodes()).isEmpty()
    }

    @Test
    fun resumeAndPauseRuntime_renderingStatusUpdated() {
        assertThat(spatialRenderingRuntime.isFrameLoopStarted()).isFalse()

        renderingRuntime.resume()

        assertThat(spatialRenderingRuntime.isFrameLoopStarted()).isTrue()

        renderingRuntime.pause()

        assertThat(spatialRenderingRuntime.isFrameLoopStarted()).isFalse()

        renderingRuntime.resume()

        assertThat(spatialRenderingRuntime.isFrameLoopStarted()).isTrue()
    }

    @Test
    fun destroy_disposeInvoked() {
        renderingRuntime.destroy()

        assertThat(spatialRenderingRuntime.isFrameLoopStarted()).isFalse()
        verify(splitEngineSubspaceManager).destroy()
    }
}
