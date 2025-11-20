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

package androidx.xr.scenecore.impl.impress

import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl.StereoSurfaceEntityData
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl.StereoSurfaceEntityData.CanvasShape
import androidx.xr.scenecore.impl.impress.ImpressApi.StereoMode
import androidx.xr.scenecore.runtime.KhronosPbrMaterialSpec
import androidx.xr.scenecore.runtime.TextureSampler
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class FakeImpressApiImplTest {
    private lateinit var fakeImpressApi: FakeImpressApiImpl
    private lateinit var resourceManager: BindingsResourceManager
    private val textureSampler: TextureSampler = TextureSampler(0, 0, 0, 0, 0, 0, 0, 0)

    @Before
    fun setUp() {
        fakeImpressApi = FakeImpressApiImpl()
        resourceManager = Mockito.mock(BindingsResourceManager::class.java)
    }

    @Test
    fun loadImageBasedLightingAssetTemp_returnsImageFuture() {
        runBlocking {
            val model = fakeImpressApi.loadImageBasedLightingAssetTemp("fakeEnvironment.zip")

            assertThat(model).isNotNull()
        }
    }

    @Test
    fun loadImageBasedLightingAsset_returnsImageFuture() {
        val modelFuture = fakeImpressApi.loadImageBasedLightingAsset("fakeEnvironment.zip")
        assertThat(modelFuture).isNotNull()
    }

    @Test
    fun loadImageBasedLightingAssetTemp_withByteArrayAndKey_returnsFuture() {
        val byteArray = byteArrayOf()

        runBlocking {
            val model =
                fakeImpressApi.loadImageBasedLightingAssetTemp(byteArray, "fakeEnvironment.zip")

            assertThat(model).isNotNull()
        }
    }

    @Test
    fun loadImageBasedLightingAsset_withByteArrayAndKey_returnsFuture() {
        val byteArray = byteArrayOf()
        val modelFuture =
            fakeImpressApi.loadImageBasedLightingAsset(byteArray, "fakeEnvironment.zip")
        assertThat(modelFuture).isNotNull()
    }

    @Test
    fun releaseImageBasedLightingAsset_releasesImage() {
        val imageFuture = fakeImpressApi.loadImageBasedLightingAsset("fakeEnvironment.zip")
        var images = fakeImpressApi.getImageBasedLightingAssets()
        assertThat(images).isNotNull()
        assertThat(images).hasSize(1)

        val imageToken: Long = imageFuture.get().nativeHandle
        fakeImpressApi.releaseImageBasedLightingAsset(imageToken)

        images = fakeImpressApi.getImageBasedLightingAssets()
        assertThat(images).isEmpty()
    }

    @Test
    fun loadGltfAssetTemp_returnsModelFuture() {
        runBlocking {
            val modelFuture = fakeImpressApi.loadGltfAssetTemp("FakeAsset.glb")

            assertThat(modelFuture).isNotNull()
        }
    }

    @Test
    fun loadGltfAsset_returnsModelFuture() {
        val modelFuture = fakeImpressApi.loadGltfAsset("FakeAsset.glb")
        assertThat(modelFuture).isNotNull()
    }

    @Test
    fun loadGltfAssetTemp_withByteArrayAndKey_returnsModelFuture() {
        val byteArray = byteArrayOf()

        runBlocking {
            val model = fakeImpressApi.loadGltfAssetTemp(byteArray, "FakeAsset.glb")

            assertThat(model).isNotNull()
        }
    }

    @Test
    fun loadGltfAsset_withByteArrayAndKey_returnsModelFuture() {
        val byteArray = byteArrayOf()
        val modelFuture = fakeImpressApi.loadGltfAsset(byteArray, "FakeAsset.glb")
        assertThat(modelFuture).isNotNull()
    }

    @Test
    fun getImpressNodesForToken_returnsNodes() {
        val modelFuture = fakeImpressApi.loadGltfAsset("FakeAsset.glb")
        val modelToken = modelFuture.get().nativeHandle
        val nodes = fakeImpressApi.getImpressNodesForToken(modelToken)
        assertThat(nodes).isNotNull()
    }

    @Test
    fun releaseGltfAsset_releasesModel() {
        val modelFuture = fakeImpressApi.loadGltfAsset("FakeAsset.glb")
        val modelToken = modelFuture.get().nativeHandle
        var nodes = fakeImpressApi.getImpressNodesForToken(modelToken)
        assertThat(nodes).isNotNull()
        fakeImpressApi.releaseGltfAsset(modelToken)
        nodes = fakeImpressApi.getImpressNodesForToken(modelToken)
        assertThat(nodes).isNull()
    }

    @Test
    fun instanceGltfModel_withCollider_returnsEntityId() {
        val modelFuture = fakeImpressApi.loadGltfAsset("FakeAsset.glb")
        val modelToken = modelFuture.get().nativeHandle
        val entityNode = fakeImpressApi.instanceGltfModel(modelToken, enableCollider = true)
        assertThat(entityNode.handle).isNotEqualTo(0)
    }

    @Test
    fun instanceGltfModel_withoutCollider_returnsEntityId() {
        val modelFuture = fakeImpressApi.loadGltfAsset("FakeAsset.glb")
        val modelToken = modelFuture.get().nativeHandle
        val entityNode = fakeImpressApi.instanceGltfModel(modelToken, enableCollider = false)
        assertThat(entityNode.handle).isNotEqualTo(0)
    }

    @Test
    fun createImpressNode_returnsEntityId() {
        val entityNode = fakeImpressApi.createImpressNode()
        assertThat(entityNode.handle).isNotEqualTo(0)
        val entityNode2 = fakeImpressApi.createImpressNode()
        // The entityId is incremented by 1 whenever a new node is created.
        assertThat(entityNode2.handle).isEqualTo(entityNode.handle + 1)
    }

    @Test
    fun getImpressNodes_returnsNodes() {
        val entityNode = fakeImpressApi.createImpressNode()
        val nodes = fakeImpressApi.getImpressNodes()
        assertThat(nodes).hasSize(1)
        val lastNode = nodes.keys.lastOrNull()
        assertThat(lastNode).isNotNull()
        assertThat(lastNode!!.entityId).isEqualTo(entityNode.handle)
    }

    @Test
    fun destroyImpressNode_destroysNode() {
        val entityNode = fakeImpressApi.createImpressNode()
        val nodes = fakeImpressApi.getImpressNodes()
        val nodesSize = nodes.size
        fakeImpressApi.destroyImpressNode(entityNode)
        assertThat(nodes.size).isEqualTo(nodesSize - 1)
    }

    @Test
    fun setGltfModelColliderEnabled_enablesCollider() {
        val entityNode = fakeImpressApi.createImpressNode()
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setGltfModelColliderEnabled(entityNode, enableCollider = true)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setGltfModelColliderEnabled_disablesCollider() {
        val entityNode = fakeImpressApi.createImpressNode()
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setGltfModelColliderEnabled(entityNode, enableCollider = false)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun animateGltfModelTemp_animatesModelWithoutLooping() {
        val entityNode = fakeImpressApi.createImpressNode()
        val animatingSize = fakeImpressApi.impressNodeAnimatingSize()

        runBlocking {
            val coroutine =
                fakeImpressApi.animateGltfModel(entityNode, "animation_name", looping = false)

            assertThat(coroutine).isNotNull()
        }

        val animatingSize2 = fakeImpressApi.impressNodeAnimatingSize()
        assertThat(animatingSize2).isEqualTo(animatingSize + 1)
    }

    @Test
    fun animateGltfModel_animatesModelWithoutLooping() {
        val entityNode = fakeImpressApi.createImpressNode()
        val animatingSize = fakeImpressApi.impressNodeAnimatingSize()
        val future = fakeImpressApi.animateGltfModel(entityNode, "animation_name", looping = false)
        assertThat(future).isNotNull()
        val animatingSize2 = fakeImpressApi.impressNodeAnimatingSize()
        assertThat(animatingSize2).isEqualTo(animatingSize + 1)
    }

    @Test
    fun animateGltfModelTemp_animatesModelWithLooping() {
        val entityNode = fakeImpressApi.createImpressNode()
        val animatingSize = fakeImpressApi.impressNodeLoopAnimatingSize()

        runBlocking {
            val coroutine =
                fakeImpressApi.animateGltfModel(entityNode, "animation_name", looping = true)

            assertThat(coroutine).isNotNull()
        }

        val animatingSize2 = fakeImpressApi.impressNodeLoopAnimatingSize()
        assertThat(animatingSize2).isEqualTo(animatingSize + 1)
    }

    @Test
    fun animateGltfModel_animatesModelWithLooping() {
        val entityNode = fakeImpressApi.createImpressNode()
        val animatingSize = fakeImpressApi.impressNodeLoopAnimatingSize()
        val future = fakeImpressApi.animateGltfModel(entityNode, "animation_name", looping = true)
        assertThat(future).isNotNull()
        val animatingSize2 = fakeImpressApi.impressNodeLoopAnimatingSize()
        assertThat(animatingSize2).isEqualTo(animatingSize + 1)
    }

    @Test
    fun stopGltfModelAnimation_stopsModelWithoutLooping() {
        val entityNode = fakeImpressApi.createImpressNode()
        val future = fakeImpressApi.animateGltfModel(entityNode, "animation_name", looping = false)
        assertThat(future).isNotNull()
        val animatingSize = fakeImpressApi.impressNodeAnimatingSize()
        fakeImpressApi.stopGltfModelAnimation(entityNode)
        val animatingSize2 = fakeImpressApi.impressNodeAnimatingSize()
        assertThat(animatingSize2).isEqualTo(animatingSize - 1)
    }

    @Test
    fun stopGltfModelAnimation_stopsModelWithLooping() {
        val entityNode = fakeImpressApi.createImpressNode()
        val future = fakeImpressApi.animateGltfModel(entityNode, "animation_name", looping = true)
        assertThat(future).isNotNull()
        val animatingSize = fakeImpressApi.impressNodeLoopAnimatingSize()
        fakeImpressApi.stopGltfModelAnimation(entityNode)
        val animatingSize2 = fakeImpressApi.impressNodeLoopAnimatingSize()
        assertThat(animatingSize2).isEqualTo(animatingSize - 1)
    }

    @Test
    fun impressNodeHasParent_byDefault_returnsFalse() {
        val entityNode = fakeImpressApi.createImpressNode()
        val hasParent = fakeImpressApi.impressNodeHasParent(entityNode)
        assertThat(hasParent).isFalse()
    }

    @Test
    fun impressNodeHasParent_whenParentIsSet_returnsTrue() {
        val childEntityNode = fakeImpressApi.createImpressNode()
        val parentEntityNode = fakeImpressApi.createImpressNode()
        fakeImpressApi.setImpressNodeParent(childEntityNode, parentEntityNode)
        val hasParent = fakeImpressApi.impressNodeHasParent(childEntityNode)
        assertThat(hasParent).isTrue()
    }

    @Test
    fun getImpressNodeParent_returnsParent() {
        val childEntityNode = fakeImpressApi.createImpressNode()
        val parentEntityNode = fakeImpressApi.createImpressNode()
        fakeImpressApi.setImpressNodeParent(childEntityNode, parentEntityNode)
        val entityId = fakeImpressApi.getImpressNodeParent(childEntityNode)
        assertThat(entityId).isEqualTo(parentEntityNode.handle)
    }

    @Test
    fun getImpressNodeParent_whenParentIsNotSet_returnsNegativeOne() {
        val childEntityNode = fakeImpressApi.createImpressNode()
        val entityId = fakeImpressApi.getImpressNodeParent(childEntityNode)
        assertThat(entityId).isEqualTo(-1)
    }

    @Test
    fun createStereoSurface_createsStereoSurface() {
        val stereoMode = StereoMode.MONO
        val stereoSurfaceNode = fakeImpressApi.createStereoSurface(stereoMode)
        val stereoSurface = fakeImpressApi.getStereoSurfaceEntities()
        val stereoSurfaceData = stereoSurface[stereoSurfaceNode]
        assertNotNull(stereoSurfaceData)
        val stereoMode2 = stereoSurfaceData!!.stereoMode
        assertThat(stereoMode).isEqualTo(stereoMode2)
        val surface = stereoSurfaceData.surface
        assertThat(surface).isNotNull()
    }

    @Test
    fun setStereoSurfaceEntityCanvasShapeQuad_setsCanvasShapeQuad() {
        val stereoMode = StereoMode.MONO
        val stereoSurfaceNode = fakeImpressApi.createStereoSurface(stereoMode)
        fakeImpressApi.setStereoSurfaceEntityCanvasShapeQuad(stereoSurfaceNode, 11.0f, 11.0f)
        val stereoSurface = fakeImpressApi.getStereoSurfaceEntities()
        val stereoSurfaceData = stereoSurface[stereoSurfaceNode]
        assertNotNull(stereoSurfaceData)
        val canvasShape = stereoSurfaceData!!.canvasShape
        assertThat(canvasShape).isEqualTo(CanvasShape.QUAD)
        val width = stereoSurfaceData.width
        assertThat(width).isEqualTo(11.0f)
        val height = stereoSurfaceData.height
        assertThat(height).isEqualTo(11.0f)
    }

    @Test
    fun setStereoSurfaceEntityCanvasShapeSphere_setsCanvasShapeSphere() {
        val stereoMode = StereoMode.MONO
        val stereoSurfaceNode = fakeImpressApi.createStereoSurface(stereoMode)
        fakeImpressApi.setStereoSurfaceEntityCanvasShapeSphere(stereoSurfaceNode, 11.0f)
        val stereoSurface = fakeImpressApi.getStereoSurfaceEntities()
        val stereoSurfaceData = stereoSurface[stereoSurfaceNode]
        assertNotNull(stereoSurfaceData)
        val canvasShape = stereoSurfaceData!!.canvasShape
        assertThat(canvasShape).isEqualTo(CanvasShape.VR_360_SPHERE)
        val radius = stereoSurfaceData.radius
        assertThat(radius).isEqualTo(11.0f)
    }

    @Test
    fun setStereoSurfaceEntityCanvasShapeHemisphere_setsCanvasShapeHemisphere() {
        val stereoMode = StereoMode.MONO
        val stereoSurfaceNode = fakeImpressApi.createStereoSurface(stereoMode)
        fakeImpressApi.setStereoSurfaceEntityCanvasShapeHemisphere(stereoSurfaceNode, 11.0f)
        val stereoSurface = fakeImpressApi.getStereoSurfaceEntities()
        val stereoSurfaceData = stereoSurface[stereoSurfaceNode]
        assertNotNull(stereoSurfaceData)
        val canvasShape = stereoSurfaceData!!.canvasShape
        assertThat(canvasShape).isEqualTo(CanvasShape.VR_180_HEMISPHERE)
        val radius = stereoSurfaceData.radius
        assertThat(radius).isEqualTo(11.0f)
    }

    @Test
    fun getSurfaceFromStereoSurface_returnsSurface() {
        val stereoMode = StereoMode.MONO
        val stereoSurfaceNode = fakeImpressApi.createStereoSurface(stereoMode)
        val surface = fakeImpressApi.getSurfaceFromStereoSurface(stereoSurfaceNode)
        assertThat(surface).isNotNull()
    }

    @Test
    fun getSurfaceFromStereoSurface_whenSurfaceDoesNotExist_throwsException() {
        val stereoSurfaceNode = ImpressNode(12345)
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.getSurfaceFromStereoSurface(stereoSurfaceNode)
            }
        assertThat(thrown).hasMessageThat().contains("Couldn't find stereo surface entity!")
    }

    @Test
    fun setStereoSurfaceEntitySurfaceSize_whenSizeIsNegative_throwsException() {
        val stereoMode = StereoMode.MONO
        val stereoSurfaceNode: ImpressNode = fakeImpressApi.createStereoSurface(stereoMode)
        val thrown =
            assertThrows(
                java.lang.IllegalArgumentException::class.java,
                { fakeImpressApi.setStereoSurfaceEntitySurfaceSize(stereoSurfaceNode, -1, -1) },
            )
        assertThat(thrown).hasMessageThat().contains("Surface dimensions must be positive!")
    }

    @Test
    fun setStereoSurfaceEntitySurfaceSize_whenNodeDoesNotExist_throwsException() {
        val stereoSurfaceNode = ImpressNode(12345)
        val thrown =
            assertThrows(
                java.lang.IllegalArgumentException::class.java,
                { fakeImpressApi.setStereoSurfaceEntitySurfaceSize(stereoSurfaceNode, 640, 480) },
            )
        assertThat(thrown).hasMessageThat().contains("Couldn't find stereo surface entity!")
    }

    @Test
    fun setStereoSurfaceEntitySurfaceSize_updatesDimensions() {
        val stereoMode = StereoMode.MONO
        val kWidth = 640
        val kHeight = 480
        val stereoSurfaceNode: ImpressNode = fakeImpressApi.createStereoSurface(stereoMode)
        fakeImpressApi.setStereoSurfaceEntitySurfaceSize(stereoSurfaceNode, kWidth, kHeight)
        val stereoSurface: MutableMap<ImpressNode, StereoSurfaceEntityData> =
            fakeImpressApi.getStereoSurfaceEntities()
        val stereoSurfaceData: StereoSurfaceEntityData = stereoSurface.get(stereoSurfaceNode)!!
        assertNotNull(stereoSurfaceData)
        val width = stereoSurfaceData.surfaceWidth
        val height = stereoSurfaceData.surfaceHeight
        assertThat(width).isEqualTo(kWidth)
        assertThat(height).isEqualTo(kHeight)
    }

    @Test
    fun setFeatherRadiusForStereoSurface_setsFeatherRadius() {
        val stereoMode = StereoMode.MONO
        val stereoSurfaceNode = fakeImpressApi.createStereoSurface(stereoMode)
        val radiusX = 11.0f
        val radiusY = 12.0f
        fakeImpressApi.setFeatherRadiusForStereoSurface(stereoSurfaceNode, radiusX, radiusY)
        val stereoSurface = fakeImpressApi.getStereoSurfaceEntities()
        val stereoSurfaceData = stereoSurface[stereoSurfaceNode]
        assertNotNull(stereoSurfaceData)
        val featherRadiusX = stereoSurfaceData!!.featherRadiusX
        val featherRadiusY = stereoSurfaceData.featherRadiusY
        assertThat(featherRadiusX).isEqualTo(radiusX)
        assertThat(featherRadiusY).isEqualTo(radiusY)
    }

    @Test
    fun setStereoModeForStereoSurface_setsStereoMode() {
        var stereoMode = StereoMode.MONO
        val stereoSurfaceNode = fakeImpressApi.createStereoSurface(stereoMode)
        stereoMode = StereoMode.SIDE_BY_SIDE
        fakeImpressApi.setStereoModeForStereoSurface(stereoSurfaceNode, stereoMode)
        var stereoSurface = fakeImpressApi.getStereoSurfaceEntities()
        var stereoSurfaceData = stereoSurface[stereoSurfaceNode]
        assertNotNull(stereoSurfaceData)
        var stereoMode2 = stereoSurfaceData!!.stereoMode
        assertThat(stereoMode).isEqualTo(stereoMode2)
        stereoMode = StereoMode.TOP_BOTTOM
        fakeImpressApi.setStereoModeForStereoSurface(stereoSurfaceNode, stereoMode)
        stereoSurface = fakeImpressApi.getStereoSurfaceEntities()
        stereoSurfaceData = stereoSurface[stereoSurfaceNode]
        assertNotNull(stereoSurfaceData)
        stereoMode2 = stereoSurfaceData!!.stereoMode
        assertThat(stereoMode).isEqualTo(stereoMode2)
    }

    @Test
    fun setStereoSurfaceEntityColliderEnabled_setsColliderEnabled() {
        val stereoMode = StereoMode.MONO
        val stereoSurfaceNode = fakeImpressApi.createStereoSurface(stereoMode)

        // Enable collider
        fakeImpressApi.setStereoSurfaceEntityColliderEnabled(stereoSurfaceNode, true)
        val stereoSurfaces = fakeImpressApi.getStereoSurfaceEntities()
        val stereoSurfaceData = stereoSurfaces[stereoSurfaceNode]
        assertNotNull(stereoSurfaceData)
        assertThat(stereoSurfaceData!!.colliderEnabled).isTrue()

        // Disable collider
        fakeImpressApi.setStereoSurfaceEntityColliderEnabled(stereoSurfaceNode, false)
        assertThat(stereoSurfaceData.colliderEnabled).isFalse()
    }

    @Test
    fun loadTextureTemp_loadsTexture() {
        runBlocking {
            val texture = fakeImpressApi.loadTexture("FakeAsset.png")

            assertThat(texture).isNotNull()
        }
    }

    @Test
    fun loadTexture_loadsTexture() {
        val textureFuture = fakeImpressApi.loadTexture("FakeAsset.png")
        assertThat(textureFuture).isNotNull()
        val texture = textureFuture.get()
        assertThat(texture).isNotNull()
    }

    @Test
    fun borrowReflectionTexture_returnsTexture() {
        val texture = fakeImpressApi.borrowReflectionTexture()
        assertThat(texture).isNotNull()
        val texture2 =
            Texture.Builder()
                .setImpressApi(fakeImpressApi)
                .setNativeTexture(texture.nativeHandle)
                .build()
        assertThat(texture2).isNotNull()
    }

    @Test
    fun getReflectionTextureFromIbl_returnsTexture() {
        val texture = fakeImpressApi.getReflectionTextureFromIbl(0)
        assertThat(texture).isNotNull()
        val texture2 =
            Texture.Builder()
                .setImpressApi(fakeImpressApi)
                .setNativeTexture(texture.nativeHandle)
                .build()
        assertThat(texture2).isNotNull()
    }

    @Test
    fun createWaterMaterialTemp_returnsWaterMaterialFuture() {
        runBlocking {
            val waterMaterial = fakeImpressApi.createWaterMaterial(true)

            assertThat(waterMaterial).isNotNull()
        }
    }

    @Test
    fun createWaterMaterial_returnsWaterMaterialFuture() {
        val waterMaterialFuture = fakeImpressApi.createWaterMaterial(true)
        assertThat(waterMaterialFuture).isNotNull()
        val waterMaterial = waterMaterialFuture.get()
        assertThat(waterMaterial).isNotNull()
    }

    @Test
    fun setReflectionMapOnWaterMaterial_setsReflectionMapOnWaterMaterial() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setReflectionMapOnWaterMaterial(0, 0, textureSampler)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setNormalMapOnWaterMaterial_setsNormalMapOnWaterMaterial() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setNormalMapOnWaterMaterial(0, 0, textureSampler)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setNormalTilingOnWaterMaterial_setsNormalTilingOnWaterMaterial() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setNormalTilingOnWaterMaterial(0, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setNormalSpeedOnWaterMaterial_setsNormalSpeedOnWaterMaterial() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setNormalTilingOnWaterMaterial(0, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setAlphaStepMultiplierOnWaterMaterial_setsAlphaStepMultiplierOnWaterMaterial() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setAlphaStepMultiplierOnWaterMaterial(0, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setAlphaMapOnWaterMaterial_setsAlphaMapOnWaterMaterial() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setAlphaMapOnWaterMaterial(0, 0, textureSampler)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setNormalZOnWaterMaterial_setsNormalZOnWaterMaterial() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setNormalZOnWaterMaterial(0, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setNormalBoundaryOnWaterMaterial_setsNormalBoundaryOnWaterMaterial() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setNormalBoundaryOnWaterMaterial(0, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setAlphaStepUOnWaterMaterial_setsAlphaStepUOnWaterMaterial() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setAlphaStepUOnWaterMaterial(0, 0f, 0f, 0f, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setAlphaStepVOnWaterMaterial_setsAlphaStepVOnWaterMaterial() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setAlphaStepVOnWaterMaterial(0, 0f, 0f, 0f, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun createKhronosPbrMaterialTemp_createsKhronosPbrMaterial() {
        val spec = KhronosPbrMaterialSpec(0, 0, 0)

        runBlocking {
            val material = fakeImpressApi.createKhronosPbrMaterialTemp(spec)

            assertThat(material).isNotNull()

            val materials = fakeImpressApi.getMaterials()

            assertThat(materials).containsKey(material.nativeHandle)
        }
    }

    @Test
    fun createKhronosPbrMaterial_createsKhronosPbrMaterial() {
        val spec = KhronosPbrMaterialSpec(0, 0, 0)
        val materialFuture = fakeImpressApi.createKhronosPbrMaterial(spec)
        assertThat(materialFuture).isNotNull()
        val material = materialFuture.get()
        assertThat(material).isNotNull()
        val materials = fakeImpressApi.getMaterials()
        assertThat(materials).containsKey(material.nativeHandle)
    }

    @Test
    fun setBaseColorTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setBaseColorTextureOnKhronosPbrMaterial(0, 0, textureSampler)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setBaseColorUvTransformOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setBaseColorUvTransformOnKhronosPbrMaterial(
                    0,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                )
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setBaseColorFactorsOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setBaseColorFactorsOnKhronosPbrMaterial(0, 0f, 0f, 0f, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setMetallicRoughnessTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setMetallicRoughnessTextureOnKhronosPbrMaterial(0, 0, textureSampler)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setMetallicRoughnessUvTransformOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
                    0,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                )
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setMetallicFactorOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setMetallicFactorOnKhronosPbrMaterial(0, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setRoughnessFactorOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setRoughnessFactorOnKhronosPbrMaterial(0, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setNormalTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setNormalTextureOnKhronosPbrMaterial(0, 0, textureSampler)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setNormalUvTransformOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setNormalUvTransformOnKhronosPbrMaterial(
                    0,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                )
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setNormalFactorOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setNormalFactorOnKhronosPbrMaterial(0, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setAmbientOcclusionTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setAmbientOcclusionTextureOnKhronosPbrMaterial(0, 0, textureSampler)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setAmbientOcclusionUvTransformOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
                    0,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                )
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setAmbientOcclusionFactorOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setAmbientOcclusionFactorOnKhronosPbrMaterial(0, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setEmissiveTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setEmissiveTextureOnKhronosPbrMaterial(0, 0, textureSampler)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setEmissiveUvTransformOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setEmissiveUvTransformOnKhronosPbrMaterial(
                    0,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                )
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setEmissiveFactorsOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setEmissiveFactorsOnKhronosPbrMaterial(0, 0f, 0f, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setClearcoatTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setClearcoatTextureOnKhronosPbrMaterial(0, 0, textureSampler)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setClearcoatNormalTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setClearcoatNormalTextureOnKhronosPbrMaterial(0, 0, textureSampler)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setClearcoatRoughnessTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setClearcoatRoughnessTextureOnKhronosPbrMaterial(
                    0,
                    0,
                    textureSampler,
                )
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setClearcoatFactorsOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setClearcoatFactorsOnKhronosPbrMaterial(0, 0f, 0f, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setSheenColorTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setSheenColorTextureOnKhronosPbrMaterial(0, 0, textureSampler)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setSheenColorFactorsOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setSheenColorFactorsOnKhronosPbrMaterial(0, 0f, 0f, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setSheenRoughnessTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setSheenRoughnessTextureOnKhronosPbrMaterial(0, 0, textureSampler)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setSheenRoughnessFactorOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setSheenRoughnessFactorOnKhronosPbrMaterial(0, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setTransmissionTextureOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setTransmissionTextureOnKhronosPbrMaterial(0, 0, textureSampler)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setTransmissionUvTransformOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setTransmissionUvTransformOnKhronosPbrMaterial(
                    0,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                )
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setTransmissionFactorOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setTransmissionFactorOnKhronosPbrMaterial(0, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setIndexOfRefractionOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setIndexOfRefractionOnKhronosPbrMaterial(0, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setAlphaCutoffOnKhronosPbrMaterial_throwsUnimplementedError() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setAlphaCutoffOnKhronosPbrMaterial(0, 0f)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun destroyNativeObject_destroysNativeWaterMaterialObject() {
        val waterMaterial = fakeImpressApi.createWaterMaterial(true).get()!!
        val nativeHandle = waterMaterial.nativeHandle
        val initialMaterialCount = fakeImpressApi.getMaterials().size
        fakeImpressApi.destroyNativeObject(nativeHandle)
        val finalMaterialCount = fakeImpressApi.getMaterials().size
        assertThat(finalMaterialCount).isEqualTo(initialMaterialCount - 1)
    }

    @Test
    fun destroyNativeObject_destroysNativeTextureObject() {
        val texture = fakeImpressApi.loadTexture("FakeAsset.exr").get()!!
        val nativeHandle = texture.nativeHandle
        val initialTextureCount = fakeImpressApi.getTextureImages().size
        fakeImpressApi.destroyNativeObject(nativeHandle)
        val finalTextureCount = fakeImpressApi.getTextureImages().size
        assertThat(finalTextureCount).isEqualTo(initialTextureCount - 1)
    }

    @Test
    fun setMaterialOverride_setsMaterialOverride() {
        val entityNode = fakeImpressApi.createImpressNode()
        val material = fakeImpressApi.createWaterMaterial(true).get()!!
        val nodeName = "fake_node_name"
        val primitiveIndex = 0

        fakeImpressApi.setMaterialOverride(
            entityNode,
            material.nativeHandle,
            nodeName,
            primitiveIndex,
        )
        val nodes = fakeImpressApi.getImpressNodes()
        val foundMaterial =
            nodes.keys.any { node ->
                node.entityId == entityNode.handle &&
                    node.materialOverride?.materialHandle == material.nativeHandle
            }
        assertThat(foundMaterial).isTrue()
    }

    @Test
    fun clearMaterialOverride_clearsMaterialOverride() {
        val entityNode = fakeImpressApi.createImpressNode()
        val material = fakeImpressApi.createWaterMaterial(true).get()!!
        val nodeName = "fake_node_name"
        val primitiveIndex = 0

        fakeImpressApi.setMaterialOverride(
            entityNode,
            material.nativeHandle,
            nodeName,
            primitiveIndex,
        )
        fakeImpressApi.clearMaterialOverride(entityNode, nodeName, primitiveIndex)

        val nodes = fakeImpressApi.getImpressNodes()
        val overrideWasCleared =
            nodes.keys.any { node ->
                node.entityId == entityNode.handle && node.materialOverride == null
            }
        assertThat(overrideWasCleared).isTrue()
    }

    @Test
    fun setPreferredEnvironmentLight_setsPreferredEnvironmentLight() {
        val iblToken = 11L
        fakeImpressApi.setPreferredEnvironmentLight(iblToken)
        val currentEnvironmentLightId = fakeImpressApi.getCurrentEnvironmentLight()
        assertThat(currentEnvironmentLightId).isEqualTo(iblToken)
    }

    @Test
    fun clearPreferredEnvironmentIblAsset_clearsPreferredEnvironmentIblAsset() {
        val iblToken = 11L
        fakeImpressApi.setPreferredEnvironmentLight(iblToken)
        fakeImpressApi.clearPreferredEnvironmentIblAsset()
        val currentEnvironmentLightId = fakeImpressApi.getCurrentEnvironmentLight()
        // Returns -1 as an id when there's no current environment light id.
        assertThat(currentEnvironmentLightId).isEqualTo(-1L)
    }

    @Test
    fun setPrimaryAlphaMaskForStereoSurface_setsPrimaryAlphaMaskForStereoSurface() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setPrimaryAlphaMaskForStereoSurface(ImpressNode(0), 0)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun setAuxiliaryAlphaMaskForStereoSurface_setsAuxiliaryAlphaMaskForStereoSurface() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.setAuxiliaryAlphaMaskForStereoSurface(ImpressNode(0), 0)
            }
        assertThat(thrown).hasMessageThat().contains("not implemented")
    }

    @Test
    fun disposeAllResources_disposesAllResources() {
        fakeImpressApi.loadImageBasedLightingAsset("fakeEnvironment.zip")
        fakeImpressApi.createImpressNode()
        fakeImpressApi.loadGltfAsset("fakeAsset.glb")
        fakeImpressApi.loadTexture("FakeAsset.png")
        fakeImpressApi.createWaterMaterial(false)
        fakeImpressApi.disposeAllResources()
        assertThat(fakeImpressApi.getImageBasedLightingAssets()).isEmpty()
        assertThat(fakeImpressApi.getImpressNodes()).isEmpty()
        assertThat(fakeImpressApi.getGltfModels()).isEmpty()
        assertThat(fakeImpressApi.getTextureImages()).isEmpty()
        assertThat(fakeImpressApi.getMaterials()).isEmpty()
    }
}
