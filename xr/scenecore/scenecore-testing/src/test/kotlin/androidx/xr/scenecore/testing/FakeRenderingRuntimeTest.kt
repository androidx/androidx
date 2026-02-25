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

package androidx.xr.scenecore.testing

import android.app.Activity
import androidx.kruth.assertThat
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Matrix3
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.Vector4
import androidx.xr.scenecore.runtime.KhronosPbrMaterialSpec
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.TextureSampler
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FakeRenderingRuntimeTest {
    private lateinit var sceneRuntime: SceneRuntime
    private lateinit var renderingRuntime: RenderingRuntime
    private lateinit var fakeRenderingRuntime: FakeRenderingRuntime

    @Before
    fun setUp() {
        val activityController = Robolectric.buildActivity<Activity?>(Activity::class.java)
        val activity: Activity? = activityController.create().start().get()

        assertThat(activity).isNotNull()

        val fakeSceneRuntime = FakeSceneRuntime(FakeScheduledExecutorService())
        sceneRuntime = fakeSceneRuntime
        fakeRenderingRuntime = FakeRenderingRuntime(sceneRuntime)
        renderingRuntime = fakeRenderingRuntime
    }

    @After
    fun tearDown() {
        // RenderingRuntime must be destroyed first
        renderingRuntime.destroy()
        sceneRuntime.destroy()
    }

    @Test
    fun getState_whenCreated_returnsCreatedState() {
        assertThat(fakeRenderingRuntime.state).isEqualTo(FakeRenderingRuntime.State.CREATED)
    }

    @Test
    fun getState_whenStarted_returnsStartedState() {
        fakeRenderingRuntime.resume()

        assertThat(fakeRenderingRuntime.state).isEqualTo(FakeRenderingRuntime.State.STARTED)
    }

    @Test
    fun getState_whenStopped_returnsPausedState() {
        fakeRenderingRuntime.pause()

        assertThat(fakeRenderingRuntime.state).isEqualTo(FakeRenderingRuntime.State.PAUSED)
    }

    @Test
    fun setReflectionTexture_checkReturnedValue() {
        check(fakeRenderingRuntime.reflectionTexture == null)

        val resource = FakeResource(0)
        fakeRenderingRuntime.reflectionTexture = resource

        Truth.assertThat(renderingRuntime.borrowReflectionTexture()).isEqualTo(resource)
    }

    @Test
    fun destroyTexture_checkReflectionTexture() {
        check(fakeRenderingRuntime.reflectionTexture == null)

        val resource = FakeResource(0)
        fakeRenderingRuntime.reflectionTexture = resource
        renderingRuntime.destroyTexture(resource)

        Truth.assertThat(fakeRenderingRuntime.reflectionTexture).isNull()
    }

    @Test
    fun createWaterMaterial_checkCreatedWaterMaterials_setCreatedWaterMaterialProperties() =
        runBlocking {
            check(fakeRenderingRuntime.createdWaterMaterials.isEmpty())

            val material = fakeRenderingRuntime.createWaterMaterial(true)
            val reflectionMap = FakeResource(0)
            val normalMap = FakeResource(1)
            val alphaMap = FakeResource(2)
            val sampler =
                TextureSampler(
                    wrapModeS = TextureSampler.WrapMode.CLAMP_TO_EDGE,
                    wrapModeT = TextureSampler.WrapMode.CLAMP_TO_EDGE,
                    wrapModeR = TextureSampler.WrapMode.CLAMP_TO_EDGE,
                    minFilter = TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
                    magFilter = TextureSampler.MagFilter.LINEAR,
                    compareMode = TextureSampler.CompareMode.NONE,
                    compareFunc = TextureSampler.CompareFunc.LE,
                    anisotropyLog2 = 0,
                )

            fakeRenderingRuntime.setReflectionMapOnWaterMaterial(material, reflectionMap, sampler)
            fakeRenderingRuntime.setNormalMapOnWaterMaterial(material, normalMap, sampler)
            fakeRenderingRuntime.setNormalTilingOnWaterMaterial(material, 1.0f)
            fakeRenderingRuntime.setNormalSpeedOnWaterMaterial(material, 2.0f)
            fakeRenderingRuntime.setAlphaStepMultiplierOnWaterMaterial(material, 3.0f)
            fakeRenderingRuntime.setAlphaMapOnWaterMaterial(material, alphaMap, sampler)
            fakeRenderingRuntime.setNormalZOnWaterMaterial(material, 4.0f)
            fakeRenderingRuntime.setNormalBoundaryOnWaterMaterial(material, 5.0f)

            assertThat(fakeRenderingRuntime.createdWaterMaterials.size).isEqualTo(1)
            assertThat(fakeRenderingRuntime.createdWaterMaterials[0])
                .isInstanceOf<FakeRenderingRuntime.FakeWaterMaterial>()
            assertThat(fakeRenderingRuntime.createdWaterMaterials[0].isAlphaMapVersion).isTrue()
            assertThat(fakeRenderingRuntime.createdWaterMaterials[0].reflectionMap)
                .isEqualTo(reflectionMap)
            assertThat(fakeRenderingRuntime.createdWaterMaterials[0].normalMap).isEqualTo(normalMap)
            assertThat(fakeRenderingRuntime.createdWaterMaterials[0].normalTiling).isEqualTo(1.0f)
            assertThat(fakeRenderingRuntime.createdWaterMaterials[0].normalSpeed).isEqualTo(2.0f)
            assertThat(fakeRenderingRuntime.createdWaterMaterials[0].alphaStepMultiplier)
                .isEqualTo(3.0f)
            assertThat(fakeRenderingRuntime.createdWaterMaterials[0].alphaMap).isEqualTo(alphaMap)
            assertThat(fakeRenderingRuntime.createdWaterMaterials[0].normalZ).isEqualTo(4.0f)
            assertThat(fakeRenderingRuntime.createdWaterMaterials[0].normalBoundary).isEqualTo(5.0f)

            fakeRenderingRuntime.destroyWaterMaterial(material)

            assertThat(fakeRenderingRuntime.createdWaterMaterials).isEmpty()
        }

    @Test
    fun createKhronosPbrMaterial_checkCreatedKhronosPbrMaterials_setCreatedKhronosPbrMaterialProperties() =
        runBlocking {
            check(fakeRenderingRuntime.createdKhronosPbrMaterials.isEmpty())

            val spec = KhronosPbrMaterialSpec(1, 2, 3)
            val material = fakeRenderingRuntime.createKhronosPbrMaterial(spec)
            val baseColor = FakeResource(0)
            val baseColorUvTransform = Matrix3(FloatArray(9))
            val baseColorFactors = Vector4(1f, 2f, 3f, 4f)
            val metallicRoughness = FakeResource(1)
            val metallicRoughnessUvTransform = Matrix3(FloatArray(9))
            val normal = FakeResource(2)
            val normalUvTransform = Matrix3(FloatArray(9))
            val ambientOcclusion = FakeResource(3)
            val ambientOcclusionUvTransform = Matrix3(FloatArray(9))
            val emissive = FakeResource(4)
            val emissiveUvTransform = Matrix3(FloatArray(9))
            val emissiveFactors = Vector3(5f, 6f, 7f)
            val clearcoat = FakeResource(5)
            val clearcoatNormal = FakeResource(6)
            val clearcoatRoughness = FakeResource(7)
            val sheenColor = FakeResource(8)
            val sheenColorFactors = Vector3(11f, 12f, 13f)
            val sheenRoughness = FakeResource(9)
            val transmission = FakeResource(10)
            val transmissionUvTransform = Matrix3(FloatArray(9))
            val sampler =
                TextureSampler(
                    wrapModeS = TextureSampler.WrapMode.CLAMP_TO_EDGE,
                    wrapModeT = TextureSampler.WrapMode.CLAMP_TO_EDGE,
                    wrapModeR = TextureSampler.WrapMode.CLAMP_TO_EDGE,
                    minFilter = TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
                    magFilter = TextureSampler.MagFilter.LINEAR,
                    compareMode = TextureSampler.CompareMode.NONE,
                    compareFunc = TextureSampler.CompareFunc.LE,
                    anisotropyLog2 = 0,
                )

            fakeRenderingRuntime.setBaseColorTextureOnKhronosPbrMaterial(
                material,
                baseColor,
                sampler,
            )
            fakeRenderingRuntime.setBaseColorUvTransformOnKhronosPbrMaterial(
                material,
                baseColorUvTransform,
            )
            fakeRenderingRuntime.setBaseColorFactorsOnKhronosPbrMaterial(material, baseColorFactors)
            fakeRenderingRuntime.setMetallicRoughnessTextureOnKhronosPbrMaterial(
                material,
                metallicRoughness,
                sampler,
            )
            fakeRenderingRuntime.setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
                material,
                metallicRoughnessUvTransform,
            )
            fakeRenderingRuntime.setMetallicFactorOnKhronosPbrMaterial(material, 5.0f)
            fakeRenderingRuntime.setRoughnessFactorOnKhronosPbrMaterial(material, 6.0f)
            fakeRenderingRuntime.setNormalTextureOnKhronosPbrMaterial(material, normal, sampler)
            fakeRenderingRuntime.setNormalUvTransformOnKhronosPbrMaterial(
                material,
                normalUvTransform,
            )
            fakeRenderingRuntime.setNormalFactorOnKhronosPbrMaterial(material, 7.0f)
            fakeRenderingRuntime.setAmbientOcclusionTextureOnKhronosPbrMaterial(
                material,
                ambientOcclusion,
                sampler,
            )
            fakeRenderingRuntime.setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
                material,
                ambientOcclusionUvTransform,
            )
            fakeRenderingRuntime.setAmbientOcclusionFactorOnKhronosPbrMaterial(material, 8.0f)
            fakeRenderingRuntime.setEmissiveTextureOnKhronosPbrMaterial(material, emissive, sampler)
            fakeRenderingRuntime.setEmissiveUvTransformOnKhronosPbrMaterial(
                material,
                emissiveUvTransform,
            )
            fakeRenderingRuntime.setEmissiveFactorsOnKhronosPbrMaterial(material, emissiveFactors)
            fakeRenderingRuntime.setClearcoatTextureOnKhronosPbrMaterial(
                material,
                clearcoat,
                sampler,
            )
            fakeRenderingRuntime.setClearcoatNormalTextureOnKhronosPbrMaterial(
                material,
                clearcoatNormal,
                sampler,
            )
            fakeRenderingRuntime.setClearcoatRoughnessTextureOnKhronosPbrMaterial(
                material,
                clearcoatRoughness,
                sampler,
            )
            fakeRenderingRuntime.setClearcoatFactorsOnKhronosPbrMaterial(
                material,
                8.0f,
                9.0f,
                10.0f,
            )
            fakeRenderingRuntime.setSheenColorTextureOnKhronosPbrMaterial(
                material,
                sheenColor,
                sampler,
            )
            fakeRenderingRuntime.setSheenColorFactorsOnKhronosPbrMaterial(
                material,
                sheenColorFactors,
            )
            fakeRenderingRuntime.setSheenRoughnessTextureOnKhronosPbrMaterial(
                material,
                sheenRoughness,
                sampler,
            )
            fakeRenderingRuntime.setSheenRoughnessFactorOnKhronosPbrMaterial(material, 14.0f)
            fakeRenderingRuntime.setTransmissionTextureOnKhronosPbrMaterial(
                material,
                transmission,
                sampler,
            )
            fakeRenderingRuntime.setTransmissionUvTransformOnKhronosPbrMaterial(
                material,
                transmissionUvTransform,
            )
            fakeRenderingRuntime.setTransmissionFactorOnKhronosPbrMaterial(material, 15.0f)
            fakeRenderingRuntime.setIndexOfRefractionOnKhronosPbrMaterial(material, 16.0f)
            fakeRenderingRuntime.setAlphaCutoffOnKhronosPbrMaterial(material, 17.0f)

            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials.size).isEqualTo(1)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0])
                .isInstanceOf<FakeRenderingRuntime.FakeKhronosPbrMaterial>()
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].spec).isEqualTo(spec)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].baseColorTexture)
                .isEqualTo(baseColor)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].baseColorUvTransform)
                .isEqualTo(baseColorUvTransform)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].baseColorFactors)
                .isEqualTo(baseColorFactors)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].metallicRoughnessTexture)
                .isEqualTo(metallicRoughness)
            assertThat(
                    fakeRenderingRuntime.createdKhronosPbrMaterials[0].metallicRoughnessUvTransform
                )
                .isEqualTo(metallicRoughnessUvTransform)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].metallicFactor)
                .isEqualTo(5.0f)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].roughnessFactor)
                .isEqualTo(6.0f)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].normalTexture)
                .isEqualTo(normal)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].normalUvTransform)
                .isEqualTo(normalUvTransform)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].normalFactor)
                .isEqualTo(7.0f)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].ambientOcclusionTexture)
                .isEqualTo(ambientOcclusion)
            assertThat(
                    fakeRenderingRuntime.createdKhronosPbrMaterials[0].ambientOcclusionUvTransform
                )
                .isEqualTo(ambientOcclusionUvTransform)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].ambientOcclusionFactor)
                .isEqualTo(8.0f)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].emissiveTexture)
                .isEqualTo(emissive)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].emissiveUvTransform)
                .isEqualTo(emissiveUvTransform)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].emissiveFactors)
                .isEqualTo(emissiveFactors)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].clearcoatTexture)
                .isEqualTo(clearcoat)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].clearcoatNormalTexture)
                .isEqualTo(clearcoatNormal)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].clearcoatRoughnessTexture)
                .isEqualTo(clearcoatRoughness)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].clearcoatIntensity)
                .isEqualTo(8.0f)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].clearcoatRoughness)
                .isEqualTo(9.0f)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].clearcoatNormalFactor)
                .isEqualTo(10.0f)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].sheenColorTexture)
                .isEqualTo(sheenColor)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].sheenColorFactors)
                .isEqualTo(sheenColorFactors)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].sheenRoughnessTexture)
                .isEqualTo(sheenRoughness)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].sheenRoughnessFactor)
                .isEqualTo(14.0f)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].transmissionTexture)
                .isEqualTo(transmission)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].transmissionUvTransform)
                .isEqualTo(transmissionUvTransform)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].transmissionFactor)
                .isEqualTo(15.0f)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].indexOfRefraction)
                .isEqualTo(16.0f)
            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials[0].alphaCutoff)
                .isEqualTo(17.0f)

            fakeRenderingRuntime.destroyKhronosPbrMaterial(material)

            assertThat(fakeRenderingRuntime.createdKhronosPbrMaterials).isEmpty()
        }

    @Test
    fun createGltfEntity_returnsInitialValue() {
        val pose = Pose.Identity
        val loadedGltf = FakeGltfModelResource(0)
        val parentEntity = FakeEntity()
        val gltfEntity = fakeRenderingRuntime.createGltfEntity(pose, loadedGltf, parentEntity)

        assertThat(gltfEntity).isInstanceOf<FakeGltfEntity>()
        assertThat(gltfEntity.getPose()).isEqualTo(pose)
        assertThat(gltfEntity.parent).isEqualTo(parentEntity)
    }

    @Test
    fun createGltfEntity_returnGltfEntity() {
        val pose = Pose.Identity
        val parent = sceneRuntime.activitySpace
        val gltfEntity = renderingRuntime.createGltfEntity(pose, FakeGltfModelResource(0), parent)

        assertThat(gltfEntity).isNotNull()
        assertThat(gltfEntity.getPose()).isEqualTo(pose)
        assertThat(gltfEntity.parent).isEqualTo(parent)

        gltfEntity.dispose()
    }

    @Test
    fun createSurfaceEntity_returnsInitialValue() {
        val stereoMode = 0
        val mediaBlendingMode = 0
        val pose = Pose.Identity
        val canvasShape = SurfaceEntity.Shape.Sphere(1.0f)
        val contentSecurityLevel = 0
        val superSampling = 0
        val parentEntity = FakeEntity()
        val surfaceEntity =
            fakeRenderingRuntime.createSurfaceEntity(
                stereoMode,
                mediaBlendingMode,
                pose,
                canvasShape,
                contentSecurityLevel,
                superSampling,
                parentEntity,
            )

        assertThat(surfaceEntity).isInstanceOf<FakeSurfaceEntity>()
        assertThat(surfaceEntity.stereoMode).isEqualTo(stereoMode)
        // mediaBlendingMode is currently not a public property of SurfaceEntity so
        // we need to cast the surfaceEntity to FakeSurfaceEntity to access it.
        assertThat((surfaceEntity as FakeSurfaceEntity).mediaBlendingMode)
            .isEqualTo(mediaBlendingMode)
        assertThat(surfaceEntity.getPose()).isEqualTo(pose)
        assertThat(surfaceEntity.shape).isEqualTo(canvasShape)
        assertThat(surfaceEntity.parent).isEqualTo(parentEntity)
    }

    @Test
    fun createSurfaceEntity_returnSurfaceEntity() {
        val stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE
        val mediaBlendingMode = SurfaceEntity.MediaBlendingMode.TRANSPARENT
        val pose = Pose.Identity
        val canvasShape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f))
        val contentSecurityLevel = SurfaceEntity.SurfaceProtection.NONE
        val superSampling = 1

        val surfaceEntity =
            renderingRuntime.createSurfaceEntity(
                stereoMode,
                mediaBlendingMode,
                pose,
                canvasShape,
                contentSecurityLevel,
                superSampling,
                sceneRuntime.activitySpace,
            )

        assertThat(surfaceEntity).isNotNull()

        assertThat(surfaceEntity.stereoMode).isEqualTo(stereoMode)
        // mediaBlendingMode is currently not a public property of SurfaceEntity so
        // we need to cast the surfaceEntity to FakeSurfaceEntity to access it.
        assertThat((surfaceEntity as FakeSurfaceEntity).mediaBlendingMode)
            .isEqualTo(mediaBlendingMode)
        assertThat(surfaceEntity.getPose()).isEqualTo(pose)
        assertThat(surfaceEntity.shape.dimensions).isEqualTo(canvasShape.dimensions)
        assertThat(surfaceEntity.parent).isEqualTo(sceneRuntime.activitySpace)

        surfaceEntity.dispose()
    }
}
