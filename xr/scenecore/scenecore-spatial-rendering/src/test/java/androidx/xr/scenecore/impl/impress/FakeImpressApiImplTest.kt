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

import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl.StereoSurfaceEntityData
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl.StereoSurfaceEntityData.CanvasShape
import androidx.xr.scenecore.impl.impress.ImpressApi.ContentSecurityLevel
import androidx.xr.scenecore.impl.impress.ImpressApi.MediaBlendingMode
import androidx.xr.scenecore.impl.impress.ImpressApi.StereoMode
import androidx.xr.scenecore.runtime.KhronosPbrMaterialSpec
import androidx.xr.scenecore.runtime.TextureSampler
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
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
    fun loadImageBasedLightingAsset_returnsImageFuture() {
        runBlocking {
            val model = fakeImpressApi.loadImageBasedLightingAsset("fakeEnvironment.zip")

            assertThat(model).isNotNull()

            model.releaseBindingsResource(model.nativeHandle)
        }
    }

    @Test
    fun loadImageBasedLightingAsset_withByteArrayAndKey_returnsFuture() {
        val byteArray = byteArrayOf()

        runBlocking {
            val model = fakeImpressApi.loadImageBasedLightingAsset(byteArray, "fakeEnvironment.zip")

            assertThat(model).isNotNull()

            model.releaseBindingsResource(model.nativeHandle)
        }
    }

    @Test
    fun releaseImageBasedLightingAsset_releasesImage() = runBlocking {
        val image = fakeImpressApi.loadImageBasedLightingAsset("fakeEnvironment.zip")
        var images = fakeImpressApi.getImageBasedLightingAssets()
        assertThat(images).isNotNull()
        assertThat(images).hasSize(1)

        val imageToken: Long = image.nativeHandle
        fakeImpressApi.releaseImageBasedLightingAsset(imageToken)

        images = fakeImpressApi.getImageBasedLightingAssets()
        assertThat(images).isEmpty()
    }

    @Test
    fun loadGltfAsset_returnsModelFuture() {
        runBlocking {
            val modelFuture = fakeImpressApi.loadGltfAsset("FakeAsset.glb")

            assertThat(modelFuture).isNotNull()
        }
    }

    @Test
    fun loadGltfAsset_withByteArrayAndKey_returnsModelFuture() {
        val byteArray = byteArrayOf()

        runBlocking {
            val model = fakeImpressApi.loadGltfAsset(byteArray, "FakeAsset.glb")

            assertThat(model).isNotNull()
        }
    }

    @Test
    fun getImpressNodesForToken_returnsNodes() = runBlocking {
        val model = fakeImpressApi.loadGltfAsset("FakeAsset.glb")
        val modelToken = model.nativeHandle
        val nodes = fakeImpressApi.getImpressNodesForToken(modelToken)
        assertThat(nodes).isNotNull()
    }

    @Test
    fun releaseGltfAsset_releasesModel() = runBlocking {
        val model = fakeImpressApi.loadGltfAsset("FakeAsset.glb")
        val modelToken = model.nativeHandle
        var nodes = fakeImpressApi.getImpressNodesForToken(modelToken)
        assertThat(nodes).isNotNull()
        fakeImpressApi.releaseGltfAsset(modelToken)
        nodes = fakeImpressApi.getImpressNodesForToken(modelToken)
        assertThat(nodes).isNull()
    }

    @Test
    fun instanceGltfModel_withCollider_returnsEntityId() = runBlocking {
        val model = fakeImpressApi.loadGltfAsset("FakeAsset.glb")
        val modelToken = model.nativeHandle
        val entityNode = fakeImpressApi.instanceGltfModel(modelToken, enableCollider = true)
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
    fun animateGltfModel_withChannel_animatesModel() {
        val entityNode = fakeImpressApi.createImpressNode()
        val channel = 1
        runBlocking {
            fakeImpressApi.animateGltfModel(
                entityNode,
                "animation_name",
                looping = true,
                speed = 1.0f,
                startTime = 0.0f,
                channel = channel,
            )
        }
        val channelAnimations = fakeImpressApi.getChannelAnimations(entityNode)
        assertThat(channelAnimations).isNotNull()
        assertThat(channelAnimations).containsKey(channel)
        val animation = channelAnimations!![channel]
        assertThat(animation).isNotNull()
        assertThat(animation!!.name).isEqualTo("animation_name")
        assertThat(animation.looping).isTrue()
        assertThat(animation.speed).isEqualTo(1.0f)
        assertThat(animation.startTime).isEqualTo(0.0f)
        assertThat(animation.channel).isEqualTo(channel)
    }

    @Test
    fun stopGltfModelAnimation_withChannel_stopsModel() = runBlocking {
        val entityNode = fakeImpressApi.createImpressNode()
        val channel = 1
        fakeImpressApi.animateGltfModel(
            entityNode,
            "animation_name",
            looping = true,
            speed = 1.0f,
            startTime = 0.0f,
            channel = channel,
        )
        fakeImpressApi.stopGltfModelAnimation(entityNode, channel)
        val channelAnimations = fakeImpressApi.getChannelAnimations(entityNode)
        assertThat(channelAnimations).isNull()
    }

    @Test
    fun toggleGltfModelAnimation_withChannel_togglesModel() = runBlocking {
        val entityNode = fakeImpressApi.createImpressNode()
        val channel = 1
        fakeImpressApi.animateGltfModel(
            entityNode,
            "animation_name",
            looping = true,
            speed = 1.0f,
            startTime = 0.0f,
            channel = channel,
        )
        fakeImpressApi.toggleGltfModelAnimation(entityNode, false, channel)
        val channelAnimations = fakeImpressApi.getChannelAnimations(entityNode)
        val animation = channelAnimations!![channel]
        assertThat(animation!!.paused).isTrue()
        fakeImpressApi.toggleGltfModelAnimation(entityNode, true, channel)
        assertThat(animation.paused).isFalse()
    }

    @Test
    fun setGltfModelAnimationPlaybackTime_setsPlaybackTime() = runBlocking {
        val entityNode = fakeImpressApi.createImpressNode()
        val channel = 1
        fakeImpressApi.animateGltfModel(
            entityNode,
            "animation_name",
            looping = true,
            speed = 1.0f,
            startTime = 0.0f,
            channel = channel,
        )
        val playbackTime = 10.0f
        fakeImpressApi.setGltfModelAnimationPlaybackTime(entityNode, playbackTime, channel)
        val channelAnimations = fakeImpressApi.getChannelAnimations(entityNode)
        val animation = channelAnimations!![channel]
        assertThat(animation!!.playbackTime).isEqualTo(playbackTime)
    }

    @Test
    fun setGltfModelAnimationSpeed_setsSpeed() = runBlocking {
        val entityNode = fakeImpressApi.createImpressNode()
        val channel = 1
        fakeImpressApi.animateGltfModel(
            entityNode,
            "animation_name",
            looping = true,
            speed = 1.0f,
            startTime = 0.0f,
            channel = channel,
        )
        val speed = 2.0f
        fakeImpressApi.setGltfModelAnimationSpeed(entityNode, speed, channel)
        val channelAnimations = fakeImpressApi.getChannelAnimations(entityNode)
        val animation = channelAnimations!![channel]
        assertThat(animation!!.speed).isEqualTo(speed)
    }

    @Test
    fun getGltfModelAnimationCount_returnsZero() {
        val entityNode = fakeImpressApi.createImpressNode()
        val count = fakeImpressApi.getGltfModelAnimationCount(entityNode)
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun getGltfModelAnimationName_returnsEmptyString() {
        val entityNode = fakeImpressApi.createImpressNode()
        val name = fakeImpressApi.getGltfModelAnimationName(entityNode, 0)
        assertThat(name).isEmpty()
    }

    @Test
    fun getGltfModelAnimationDurationSeconds_returnsZero() {
        val entityNode = fakeImpressApi.createImpressNode()
        val duration = fakeImpressApi.getGltfModelAnimationDurationSeconds(entityNode, 0)
        assertThat(duration).isEqualTo(0f)
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

        val parentNode = fakeImpressApi.getImpressNodeParent(childEntityNode)
        assertThat(parentNode.handle).isEqualTo(parentEntityNode.handle)
    }

    @Test
    fun getImpressNodeParent_whenParentIsNotSet_returnsNegativeOne() {
        val childEntityNode = fakeImpressApi.createImpressNode()

        val parentNode = fakeImpressApi.getImpressNodeParent(childEntityNode)
        assertThat(parentNode.handle).isEqualTo(-1)
    }

    @Test
    fun createStereoSurface_createsStereoSurface() {
        val stereoMode = StereoMode.MONO
        val stereoSurfaceNode = fakeImpressApi.createStereoSurface(stereoMode)
        val stereoSurface = fakeImpressApi.getStereoSurfaceEntities()
        val stereoSurfaceData = stereoSurface[stereoSurfaceNode]
        assertNotNull(stereoSurfaceData)
        val stereoMode2 = stereoSurfaceData.stereoMode
        assertThat(stereoMode).isEqualTo(stereoMode2)
        val surface = stereoSurfaceData.surface
        assertThat(surface).isNotNull()
    }

    @Test
    fun createStereoSurface_withBlendingMode_createsStereoSurface() {
        val stereoMode = StereoMode.MONO
        val blendingMode = MediaBlendingMode.OPAQUE
        val contentSecurityLevel = ContentSecurityLevel.NONE
        val stereoSurfaceNode =
            fakeImpressApi.createStereoSurface(
                stereoMode,
                blendingMode,
                contentSecurityLevel,
                useSuperSampling = false,
            )
        val stereoSurface = fakeImpressApi.getStereoSurfaceEntities()
        val stereoSurfaceData = stereoSurface[stereoSurfaceNode]
        assertNotNull(stereoSurfaceData)
        assertThat(stereoSurfaceData.mediaBlendingMode).isEqualTo(blendingMode)
    }

    @Test
    fun setStereoSurfaceEntityCanvasShapeQuad_setsCanvasShapeQuad() {
        val stereoMode = StereoMode.MONO
        val stereoSurfaceNode = fakeImpressApi.createStereoSurface(stereoMode)
        fakeImpressApi.setStereoSurfaceEntityCanvasShapeQuad(stereoSurfaceNode, 11.0f, 11.0f, 1.0f)
        val stereoSurface = fakeImpressApi.getStereoSurfaceEntities()
        val stereoSurfaceData = stereoSurface[stereoSurfaceNode]
        assertNotNull(stereoSurfaceData)
        val canvasShape = stereoSurfaceData.canvasShape
        assertThat(canvasShape).isEqualTo(CanvasShape.QUAD)
        val width = stereoSurfaceData.width
        assertThat(width).isEqualTo(11.0f)
        val height = stereoSurfaceData.height
        assertThat(height).isEqualTo(11.0f)
        val cornerRadius = stereoSurfaceData.cornerRadius
        assertThat(cornerRadius).isEqualTo(1.0f)
    }

    @Test
    fun setStereoSurfaceEntityCanvasShapeSphere_setsCanvasShapeSphere() {
        val stereoMode = StereoMode.MONO
        val stereoSurfaceNode = fakeImpressApi.createStereoSurface(stereoMode)
        fakeImpressApi.setStereoSurfaceEntityCanvasShapeSphere(stereoSurfaceNode, 11.0f)
        val stereoSurface = fakeImpressApi.getStereoSurfaceEntities()
        val stereoSurfaceData = stereoSurface[stereoSurfaceNode]
        assertNotNull(stereoSurfaceData)
        val canvasShape = stereoSurfaceData.canvasShape
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
        val canvasShape = stereoSurfaceData.canvasShape
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
        val stereoSurfaceData: StereoSurfaceEntityData = stereoSurface[stereoSurfaceNode]!!
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
        val featherRadiusX = stereoSurfaceData.featherRadiusX
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
        var stereoMode2 = stereoSurfaceData.stereoMode
        assertThat(stereoMode).isEqualTo(stereoMode2)
        stereoMode = StereoMode.TOP_BOTTOM
        fakeImpressApi.setStereoModeForStereoSurface(stereoSurfaceNode, stereoMode)
        stereoSurface = fakeImpressApi.getStereoSurfaceEntities()
        stereoSurfaceData = stereoSurface[stereoSurfaceNode]
        assertNotNull(stereoSurfaceData)
        stereoMode2 = stereoSurfaceData.stereoMode
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
        assertThat(stereoSurfaceData.colliderEnabled).isTrue()

        // Disable collider
        fakeImpressApi.setStereoSurfaceEntityColliderEnabled(stereoSurfaceNode, false)
        assertThat(stereoSurfaceData.colliderEnabled).isFalse()
    }

    @Test
    fun loadTexture_loadsTexture() {
        runBlocking {
            val texture = fakeImpressApi.loadTexture("FakeAsset.png")

            assertThat(texture).isNotNull()
        }
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
    fun createWaterMaterial_returnsWaterMaterialFuture() {
        runBlocking {
            val waterMaterial = fakeImpressApi.createWaterMaterial(true)

            assertThat(waterMaterial).isNotNull()
        }
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
    fun createKhronosPbrMaterial_createsKhronosPbrMaterial() {
        val spec = KhronosPbrMaterialSpec(0, 0, 0)

        runBlocking {
            val material = fakeImpressApi.createKhronosPbrMaterial(spec)

            assertThat(material).isNotNull()

            val materials = fakeImpressApi.getMaterials()

            assertThat(materials).containsKey(material.nativeHandle)
        }
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
    fun destroyNativeObject_destroysNativeWaterMaterialObject() = runBlocking {
        val waterMaterial = fakeImpressApi.createWaterMaterial(true)
        val nativeHandle = waterMaterial.nativeHandle
        val initialMaterialCount = fakeImpressApi.getMaterials().size
        fakeImpressApi.destroyNativeObject(nativeHandle)
        val finalMaterialCount = fakeImpressApi.getMaterials().size
        assertThat(finalMaterialCount).isEqualTo(initialMaterialCount - 1)
    }

    @Test
    fun destroyNativeObject_destroysNativeTextureObject() = runBlocking {
        val texture = fakeImpressApi.loadTexture("FakeAsset.exr")
        val nativeHandle = texture.nativeHandle
        val initialTextureCount = fakeImpressApi.getTextureImages().size
        fakeImpressApi.destroyNativeObject(nativeHandle)
        val finalTextureCount = fakeImpressApi.getTextureImages().size
        assertThat(finalTextureCount).isEqualTo(initialTextureCount - 1)
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
    fun disposeAllResources_disposesAllResources() = runBlocking {
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

    @Test
    fun getImpressNodeChildCount_returnsCorrectCount() {
        val parent = fakeImpressApi.createImpressNode()
        val child1 = fakeImpressApi.createImpressNode()
        val child2 = fakeImpressApi.createImpressNode()

        assertThat(fakeImpressApi.getImpressNodeChildCount(parent)).isEqualTo(0)

        fakeImpressApi.setImpressNodeParent(child1, parent)
        fakeImpressApi.setImpressNodeParent(child2, parent)

        assertThat(fakeImpressApi.getImpressNodeChildCount(parent)).isEqualTo(2)
    }

    @Test
    fun getImpressNodeChildAt_returnsCorrectChild() {
        val parent = fakeImpressApi.createImpressNode()
        val child1 = fakeImpressApi.createImpressNode()
        val child2 = fakeImpressApi.createImpressNode()

        fakeImpressApi.setImpressNodeParent(child1, parent)
        fakeImpressApi.setImpressNodeParent(child2, parent)

        // In the fake implementation, children are usually added in order
        val fetchedChild0 = fakeImpressApi.getImpressNodeChildAt(parent, 0)
        val fetchedChild1 = fakeImpressApi.getImpressNodeChildAt(parent, 1)

        assertThat(fetchedChild0.handle).isEqualTo(child1.handle)
        assertThat(fetchedChild1.handle).isEqualTo(child2.handle)
    }

    @Test
    fun getImpressNodeChildAt_throwsOnInvalidIndex() {
        val parent = fakeImpressApi.createImpressNode()

        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                fakeImpressApi.getImpressNodeChildAt(parent, 0)
            }
        assertThat(thrown).hasMessageThat().contains("Invalid child index")
    }

    @Test
    fun getImpressNodeName_returnsDefaultName() {
        val node = fakeImpressApi.createImpressNode()
        // Default name in fake is empty string
        assertThat(fakeImpressApi.getImpressNodeName(node)).isEmpty()
    }

    @Test
    fun setAndGetImpressNodeLocalTransform_worksCorrectly() {
        val node = fakeImpressApi.createImpressNode()
        val translation = Vector3(1f, 2f, 3f)
        val rotation = Quaternion(0f, 0f, 0f, 1f)
        val scale = Vector3(2f, 2f, 2f)
        val transform = Matrix4.fromTrs(translation, rotation, scale)

        fakeImpressApi.setImpressNodeLocalTransform(node, transform)

        val outTransform = fakeImpressApi.getImpressNodeLocalTransform(node)

        assertThat(outTransform.pose.translation).isEqualTo(translation)
        assertThat(outTransform.pose.rotation).isEqualTo(rotation)
        assertThat(outTransform.scale).isEqualTo(scale)
    }

    @Test
    fun setAndGetImpressNodeRelativeTransform_worksCorrectly() {
        val node = fakeImpressApi.createImpressNode()
        val relative = fakeImpressApi.createImpressNode()
        val translation = Vector3(1f, 2f, 3f)
        val rotation = Quaternion(0f, 0f, 0f, 1f)
        val scale = Vector3(2f, 2f, 2f)
        val transform = Matrix4.fromTrs(translation, rotation, scale)

        fakeImpressApi.setImpressNodeRelativeTransform(node, relative, transform)

        val outTransform = fakeImpressApi.getImpressNodeRelativeTransform(node, relative)

        assertThat(outTransform.pose.translation).isEqualTo(translation)
        assertThat(outTransform.pose.rotation).isEqualTo(rotation)
        assertThat(outTransform.scale).isEqualTo(scale)
    }

    @Test
    fun getImpressNodeRelativeTransform_identityWhenSameNode() {
        val node = fakeImpressApi.createImpressNode()

        val outTransform = fakeImpressApi.getImpressNodeRelativeTransform(node, node)

        assertThat(outTransform).isEqualTo(Matrix4.Identity)
    }

    @Test
    fun scheduleGltfReskinning_setsFlagInInternalState() {
        val node = fakeImpressApi.createImpressNode()
        fakeImpressApi.scheduleGltfReskinning(node)

        val nodes = fakeImpressApi.getImpressNodes()
        val nodeData = nodes.keys.firstOrNull { it.entityId == node.handle }
        assertNotNull(nodeData)
        assertThat(nodeData.isReskinningScheduled).isTrue()
    }

    @Test
    fun setGltfModelNodeMaterialOverride_setsOverride() = runBlocking {
        val node = fakeImpressApi.createImpressNode()
        val material = fakeImpressApi.createWaterMaterial(true)
        val primIndex = 0

        fakeImpressApi.setGltfModelNodeMaterialOverride(node, material.nativeHandle, primIndex)

        val nodes = fakeImpressApi.getImpressNodes()
        val nodeData = nodes.keys.firstOrNull { it.entityId == node.handle }
        assertNotNull(nodeData)

        val storedMat = nodeData.nodeMaterialOverrides[primIndex]
        assertNotNull(storedMat)
        assertThat(storedMat.materialHandle).isEqualTo(material.nativeHandle)
    }

    @Test
    fun clearGltfModelNodeMaterialOverride_clearsOverride() = runBlocking {
        val node = fakeImpressApi.createImpressNode()
        val material = fakeImpressApi.createWaterMaterial(true)
        val primIndex = 0

        fakeImpressApi.setGltfModelNodeMaterialOverride(node, material.nativeHandle, primIndex)
        fakeImpressApi.clearGltfModelNodeMaterialOverride(node, primIndex)

        val nodes = fakeImpressApi.getImpressNodes()
        val nodeData = nodes.keys.firstOrNull { it.entityId == node.handle }
        assertNotNull(nodeData)
        assertThat(nodeData.nodeMaterialOverrides).doesNotContainKey(primIndex)
    }
}
