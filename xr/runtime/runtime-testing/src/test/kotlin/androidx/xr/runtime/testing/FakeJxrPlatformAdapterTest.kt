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

package androidx.xr.runtime.testing

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.AnchorPlacement
import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.internal.InputEvent
import androidx.xr.runtime.internal.InputEventListener
import androidx.xr.runtime.internal.KhronosPbrMaterialSpec
import androidx.xr.runtime.internal.PixelDimensions
import androidx.xr.runtime.internal.PlaneSemantic
import androidx.xr.runtime.internal.PlaneType
import androidx.xr.runtime.internal.PointerCaptureComponent.PointerCaptureState
import androidx.xr.runtime.internal.PointerCaptureComponent.StateListener
import androidx.xr.runtime.internal.SpatialCapabilities
import androidx.xr.runtime.internal.SpatialVisibility
import androidx.xr.runtime.internal.SurfaceEntity
import androidx.xr.runtime.math.Matrix3
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.Vector4
import androidx.xr.runtime.testing.FakeJxrPlatformAdapter.FakeKhronosPbrMaterial
import androidx.xr.runtime.testing.FakeJxrPlatformAdapter.FakeWaterMaterial
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.util.UUID
import java.util.concurrent.Executor
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FakeJxrPlatformAdapterTest {
    private lateinit var adapter: FakeJxrPlatformAdapter

    @Before
    fun setUp() {
        adapter = FakeJxrPlatformAdapter()
    }

    @Test
    fun getState_whenCreated_returnsCreatedState() {
        assertThat(adapter.state).isEqualTo(FakeJxrPlatformAdapter.State.CREATED)
    }

    @Test
    fun getState_whenStarted_returnsStartedState() {
        adapter.startRenderer()

        assertThat(adapter.state).isEqualTo(FakeJxrPlatformAdapter.State.STARTED)
    }

    @Test
    fun getState_whenStopped_returnsPausedState() {
        adapter.stopRenderer()

        assertThat(adapter.state).isEqualTo(FakeJxrPlatformAdapter.State.PAUSED)
    }

    @Test
    fun getState_whenDisposed_returnsDestroyedState() {
        adapter.dispose()

        assertThat(adapter.state).isEqualTo(FakeJxrPlatformAdapter.State.DESTROYED)
    }

    @Test
    fun setReflectionTexture_checkReturnedValue() {
        check(adapter.reflectionTexture == null)

        val resource = FakeResource(0)
        adapter.reflectionTexture = resource

        assertThat(adapter.borrowReflectionTexture()).isEqualTo(resource)
        assertThat(adapter.getReflectionTextureFromIbl(resource)).isEqualTo(resource)
    }

    @Test
    fun destroyTexture_checkReflectionTexture() {
        check(adapter.reflectionTexture == null)

        val resource = FakeResource(0)
        adapter.reflectionTexture = resource
        adapter.destroyTexture(resource)

        assertThat(adapter.reflectionTexture).isNull()
    }

    @Test
    fun createWaterMaterial_checkCreatedWaterMaterials_setCreatedWaterMaterialProperties() {
        check(adapter.createdWaterMaterials.isEmpty())

        val material = adapter.createWaterMaterial(true)
        val reflectionMap = FakeResource(0)
        val normalMap = FakeResource(1)
        val alphaMap = FakeResource(2)

        adapter.setReflectionMapOnWaterMaterial(material!!.get(), reflectionMap)
        adapter.setNormalMapOnWaterMaterial(material.get(), normalMap)
        adapter.setNormalTilingOnWaterMaterial(material.get(), 1.0f)
        adapter.setNormalSpeedOnWaterMaterial(material.get(), 2.0f)
        adapter.setAlphaStepMultiplierOnWaterMaterial(material.get(), 3.0f)
        adapter.setAlphaMapOnWaterMaterial(material.get(), alphaMap)
        adapter.setNormalZOnWaterMaterial(material.get(), 4.0f)
        adapter.setNormalBoundaryOnWaterMaterial(material.get(), 5.0f)

        assertThat(adapter.createdWaterMaterials.size).isEqualTo(1)
        assertThat(adapter.createdWaterMaterials[0]).isInstanceOf(FakeWaterMaterial::class.java)
        assertThat(adapter.createdWaterMaterials[0].isAlphaMapVersion).isTrue()
        assertThat(adapter.createdWaterMaterials[0].reflectionMap).isEqualTo(reflectionMap)
        assertThat(adapter.createdWaterMaterials[0].normalMap).isEqualTo(normalMap)
        assertThat(adapter.createdWaterMaterials[0].normalTiling).isEqualTo(1.0f)
        assertThat(adapter.createdWaterMaterials[0].normalSpeed).isEqualTo(2.0f)
        assertThat(adapter.createdWaterMaterials[0].alphaStepMultiplier).isEqualTo(3.0f)
        assertThat(adapter.createdWaterMaterials[0].alphaMap).isEqualTo(alphaMap)
        assertThat(adapter.createdWaterMaterials[0].normalZ).isEqualTo(4.0f)
        assertThat(adapter.createdWaterMaterials[0].normalBoundary).isEqualTo(5.0f)

        adapter.destroyWaterMaterial(material.get())

        assertThat(adapter.createdWaterMaterials).isEmpty()
    }

    @Test
    fun createKhronosPbrMaterial_checkCreatedKhronosPbrMaterials_setCreatedKhronosPbrMaterialProperties() {
        check(adapter.createdKhronosPbrMaterials.isEmpty())

        val spec = KhronosPbrMaterialSpec(1, 2, 3)
        val material = adapter.createKhronosPbrMaterial(spec)
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

        adapter.setBaseColorTextureOnKhronosPbrMaterial(material!!.get(), baseColor)
        adapter.setBaseColorUvTransformOnKhronosPbrMaterial(material.get(), baseColorUvTransform)
        adapter.setBaseColorFactorsOnKhronosPbrMaterial(material.get(), baseColorFactors)
        adapter.setMetallicRoughnessTextureOnKhronosPbrMaterial(material.get(), metallicRoughness)
        adapter.setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
            material.get(),
            metallicRoughnessUvTransform,
        )
        adapter.setMetallicFactorOnKhronosPbrMaterial(material.get(), 5.0f)
        adapter.setRoughnessFactorOnKhronosPbrMaterial(material.get(), 6.0f)
        adapter.setNormalTextureOnKhronosPbrMaterial(material.get(), normal)
        adapter.setNormalUvTransformOnKhronosPbrMaterial(material.get(), normalUvTransform)
        adapter.setNormalFactorOnKhronosPbrMaterial(material.get(), 7.0f)
        adapter.setAmbientOcclusionTextureOnKhronosPbrMaterial(material.get(), ambientOcclusion)
        adapter.setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
            material.get(),
            ambientOcclusionUvTransform,
        )
        adapter.setAmbientOcclusionFactorOnKhronosPbrMaterial(material.get(), 8.0f)
        adapter.setEmissiveTextureOnKhronosPbrMaterial(material.get(), emissive)
        adapter.setEmissiveUvTransformOnKhronosPbrMaterial(material.get(), emissiveUvTransform)
        adapter.setEmissiveFactorsOnKhronosPbrMaterial(material.get(), emissiveFactors)
        adapter.setClearcoatTextureOnKhronosPbrMaterial(material.get(), clearcoat)
        adapter.setClearcoatNormalTextureOnKhronosPbrMaterial(material.get(), clearcoatNormal)
        adapter.setClearcoatRoughnessTextureOnKhronosPbrMaterial(material.get(), clearcoatRoughness)
        adapter.setClearcoatFactorsOnKhronosPbrMaterial(material.get(), 8.0f, 9.0f, 10.0f)
        adapter.setSheenColorTextureOnKhronosPbrMaterial(material.get(), sheenColor)
        adapter.setSheenColorFactorsOnKhronosPbrMaterial(material.get(), sheenColorFactors)
        adapter.setSheenRoughnessTextureOnKhronosPbrMaterial(material.get(), sheenRoughness)
        adapter.setSheenRoughnessFactorOnKhronosPbrMaterial(material.get(), 14.0f)
        adapter.setTransmissionTextureOnKhronosPbrMaterial(material.get(), transmission)
        adapter.setTransmissionUvTransformOnKhronosPbrMaterial(
            material.get(),
            transmissionUvTransform,
        )
        adapter.setTransmissionFactorOnKhronosPbrMaterial(material.get(), 15.0f)
        adapter.setIndexOfRefractionOnKhronosPbrMaterial(material.get(), 16.0f)
        adapter.setAlphaCutoffOnKhronosPbrMaterial(material.get(), 17.0f)

        assertThat(adapter.createdKhronosPbrMaterials.size).isEqualTo(1)
        assertThat(adapter.createdKhronosPbrMaterials[0])
            .isInstanceOf(FakeKhronosPbrMaterial::class.java)
        assertThat(adapter.createdKhronosPbrMaterials[0].spec).isEqualTo(spec)
        assertThat(adapter.createdKhronosPbrMaterials[0].baseColorTexture).isEqualTo(baseColor)
        assertThat(adapter.createdKhronosPbrMaterials[0].baseColorUvTransform)
            .isEqualTo(baseColorUvTransform)
        assertThat(adapter.createdKhronosPbrMaterials[0].baseColorFactors)
            .isEqualTo(baseColorFactors)
        assertThat(adapter.createdKhronosPbrMaterials[0].metallicRoughnessTexture)
            .isEqualTo(metallicRoughness)
        assertThat(adapter.createdKhronosPbrMaterials[0].metallicRoughnessUvTransform)
            .isEqualTo(metallicRoughnessUvTransform)
        assertThat(adapter.createdKhronosPbrMaterials[0].metallicFactor).isEqualTo(5.0f)
        assertThat(adapter.createdKhronosPbrMaterials[0].roughnessFactor).isEqualTo(6.0f)
        assertThat(adapter.createdKhronosPbrMaterials[0].normalTexture).isEqualTo(normal)
        assertThat(adapter.createdKhronosPbrMaterials[0].normalUvTransform)
            .isEqualTo(normalUvTransform)
        assertThat(adapter.createdKhronosPbrMaterials[0].normalFactor).isEqualTo(7.0f)
        assertThat(adapter.createdKhronosPbrMaterials[0].ambientOcclusionTexture)
            .isEqualTo(ambientOcclusion)
        assertThat(adapter.createdKhronosPbrMaterials[0].ambientOcclusionUvTransform)
            .isEqualTo(ambientOcclusionUvTransform)
        assertThat(adapter.createdKhronosPbrMaterials[0].ambientOcclusionFactor).isEqualTo(8.0f)
        assertThat(adapter.createdKhronosPbrMaterials[0].emissiveTexture).isEqualTo(emissive)
        assertThat(adapter.createdKhronosPbrMaterials[0].emissiveUvTransform)
            .isEqualTo(emissiveUvTransform)
        assertThat(adapter.createdKhronosPbrMaterials[0].emissiveFactors).isEqualTo(emissiveFactors)
        assertThat(adapter.createdKhronosPbrMaterials[0].clearcoatTexture).isEqualTo(clearcoat)
        assertThat(adapter.createdKhronosPbrMaterials[0].clearcoatNormalTexture)
            .isEqualTo(clearcoatNormal)
        assertThat(adapter.createdKhronosPbrMaterials[0].clearcoatRoughnessTexture)
            .isEqualTo(clearcoatRoughness)
        assertThat(adapter.createdKhronosPbrMaterials[0].clearcoatIntensity).isEqualTo(8.0f)
        assertThat(adapter.createdKhronosPbrMaterials[0].clearcoatRoughness).isEqualTo(9.0f)
        assertThat(adapter.createdKhronosPbrMaterials[0].clearcoatNormalFactor).isEqualTo(10.0f)
        assertThat(adapter.createdKhronosPbrMaterials[0].sheenColorTexture).isEqualTo(sheenColor)
        assertThat(adapter.createdKhronosPbrMaterials[0].sheenColorFactors)
            .isEqualTo(sheenColorFactors)
        assertThat(adapter.createdKhronosPbrMaterials[0].sheenRoughnessTexture)
            .isEqualTo(sheenRoughness)
        assertThat(adapter.createdKhronosPbrMaterials[0].sheenRoughnessFactor).isEqualTo(14.0f)
        assertThat(adapter.createdKhronosPbrMaterials[0].transmissionTexture)
            .isEqualTo(transmission)
        assertThat(adapter.createdKhronosPbrMaterials[0].transmissionUvTransform)
            .isEqualTo(transmissionUvTransform)
        assertThat(adapter.createdKhronosPbrMaterials[0].transmissionFactor).isEqualTo(15.0f)
        assertThat(adapter.createdKhronosPbrMaterials[0].indexOfRefraction).isEqualTo(16.0f)
        assertThat(adapter.createdKhronosPbrMaterials[0].alphaCutoff).isEqualTo(17.0f)

        adapter.destroyKhronosPbrMaterial(material.get())

        assertThat(adapter.createdKhronosPbrMaterials).isEmpty()
    }

    @Test
    fun createGltfEntity_returnsInitialValue() {
        val pose = Pose.Identity
        val loadedGltf = FakeResource(0)
        val parentEntity = FakeEntity()
        val gltfEntity = adapter.createGltfEntity(pose, loadedGltf, parentEntity)

        assertThat(gltfEntity).isInstanceOf(FakeGltfEntity::class.java)
        assertThat(gltfEntity.getPose()).isEqualTo(pose)
        assertThat(gltfEntity.parent).isEqualTo(parentEntity)
    }

    @Test
    fun createSurfaceEntity_returnsInitialValue() {
        val stereoMode = 0
        val pose = Pose.Identity
        val canvasShape = SurfaceEntity.CanvasShape.Vr360Sphere(1.0f)
        val contentSecurityLevel = 0
        val superSampling = 0
        val parentEntity = FakeEntity()
        val surfaceEntity =
            adapter.createSurfaceEntity(
                stereoMode,
                pose,
                canvasShape,
                contentSecurityLevel,
                superSampling,
                parentEntity,
            )

        assertThat(surfaceEntity).isInstanceOf(FakeSurfaceEntity::class.java)
        assertThat(surfaceEntity.stereoMode).isEqualTo(stereoMode)
        assertThat(surfaceEntity.getPose()).isEqualTo(pose)
        assertThat(surfaceEntity.canvasShape).isEqualTo(canvasShape)
        assertThat(surfaceEntity.parent).isEqualTo(parentEntity)
    }

    @Test
    fun enablePanelDepthTest_setsValueCorrectly() {
        check(!adapter.enabledPanelDepthTest)

        adapter.enablePanelDepthTest(true)

        assertThat(adapter.enabledPanelDepthTest).isTrue()
    }

    @Test
    fun addRemoveSpatialCapabilitiesChangedListener_spatialCapabilitiesChangedMapUpdated() {
        check(adapter.spatialCapabilitiesChangedMap.isEmpty())

        val consumer = Consumer<SpatialCapabilities> {}
        adapter.addSpatialCapabilitiesChangedListener({ command -> command.run() }, consumer)

        assertThat(adapter.spatialCapabilitiesChangedMap).hasSize(1)

        adapter.removeSpatialCapabilitiesChangedListener(consumer)

        assertThat(adapter.spatialCapabilitiesChangedMap).isEmpty()
    }

    @Test
    fun setClearSpatialVisibilityChangedListener_spatialVisibilityChangedMapUpdated() {
        check(adapter.spatialVisibilityChangedMap.isEmpty())

        val consumer = Consumer<SpatialVisibility> {}
        adapter.setSpatialVisibilityChangedListener({ command -> command.run() }, consumer)

        assertThat(adapter.spatialVisibilityChangedMap).hasSize(1)

        adapter.clearSpatialVisibilityChangedListener()

        assertThat(adapter.spatialVisibilityChangedMap).isEmpty()
    }

    @Test
    fun addRemovePerceivedResolutionChangedListener_perceivedResolutionChangedMapUpdated() {
        check(adapter.perceivedResolutionChangedMap.isEmpty())

        val consumer = Consumer<PixelDimensions> {}
        adapter.addPerceivedResolutionChangedListener({ command -> command.run() }, consumer)

        assertThat(adapter.perceivedResolutionChangedMap).hasSize(1)

        adapter.removePerceivedResolutionChangedListener(consumer)

        assertThat(adapter.perceivedResolutionChangedMap).isEmpty()
    }

    @Test
    fun requestFullSpaceMode_requested() {
        check(!adapter.requestedFullSpaceMode)

        adapter.requestFullSpaceMode()

        assertThat(adapter.requestedFullSpaceMode).isTrue()
    }

    @Test
    fun requestHomeSpaceMode_requested() {
        check(!adapter.requestedHomeSpaceMode)

        adapter.requestHomeSpaceMode()

        assertThat(adapter.requestedHomeSpaceMode).isTrue()
    }

    @Test
    fun createPanelEntity_withDimensionsInMeter_returnsInitialValue() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
        val pose = Pose.Identity
        val view = View(activity)
        val dimensions = Dimensions(2f, 1f, 0f)
        val name = "test_panel"
        val parent = FakeEntity()
        val panelEntity = adapter.createPanelEntity(activity, pose, view, dimensions, name, parent)

        assertThat(panelEntity).isInstanceOf(FakePanelEntity::class.java)
        assertThat(panelEntity.getPose()).isEqualTo(pose)
        assertThat(panelEntity.size).isEqualTo(dimensions)
        assertThat(panelEntity.parent).isEqualTo(parent)
    }

    @Test
    fun createPanelEntity_withDimensionsInPixel_returnsInitialValue() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
        val pose = Pose.Identity
        val view = View(activity)
        val pixelDimensions = PixelDimensions(640, 480)
        val name = "test_panel"
        val parent = FakeEntity()
        val panelEntity =
            adapter.createPanelEntity(activity, pose, view, pixelDimensions, name, parent)

        assertThat(panelEntity).isInstanceOf(FakePanelEntity::class.java)
        assertThat(panelEntity.getPose()).isEqualTo(pose)
        assertThat(panelEntity.sizeInPixels).isEqualTo(pixelDimensions)
        assertThat(panelEntity.parent).isEqualTo(parent)
    }

    @Test
    fun createActivityPanelEntity_returnsInitialValue() {
        val pose = Pose.Identity
        val windowBoundsPx = PixelDimensions(640, 480)
        val name = "test_activity_panel"
        val hostActivity = Robolectric.buildActivity(Activity::class.java).create().start().get()
        val parent = FakeEntity()
        val activityPanelEntity =
            adapter.createActivityPanelEntity(pose, windowBoundsPx, name, hostActivity, parent)

        assertThat(activityPanelEntity).isInstanceOf(FakeActivityPanelEntity::class.java)
        assertThat(activityPanelEntity.getPose()).isEqualTo(pose)
        assertThat(activityPanelEntity.sizeInPixels).isEqualTo(windowBoundsPx)
        assertThat(activityPanelEntity.parent).isEqualTo(parent)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun createAnchorEntity_withPlaneAttributes_returnsInitialValue() {
        val bounds = Dimensions(2f, 1f, 0f)
        val planeType = PlaneType.HORIZONTAL
        val planeSemantic = PlaneSemantic.FLOOR
        val searchTimeout = Duration.ofMillis(100)
        val anchorEntity =
            adapter.createAnchorEntity(bounds, planeType, planeSemantic, searchTimeout)

        assertThat(anchorEntity).isInstanceOf(FakeAnchorEntity::class.java)
        assertThat(anchorEntity.anchorCreationData.bounds).isEqualTo(bounds)
        assertThat(anchorEntity.anchorCreationData.planeType).isEqualTo(planeType)
        assertThat(anchorEntity.anchorCreationData.planeSemantic).isEqualTo(planeSemantic)
        assertThat(anchorEntity.anchorCreationData.searchTimeout).isEqualTo(searchTimeout)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun createAnchorEntity_withAnAnchor_returnsInitialValue() {
        val anchor =
            FakeRuntimeAnchor(
                Pose.Identity,
                FakeRuntimePlane(trackingState = TrackingState.STOPPED),
            )
        val anchorEntity = adapter.createAnchorEntity(anchor)

        assertThat(anchorEntity).isInstanceOf(FakeAnchorEntity::class.java)
        assertThat(anchorEntity.anchor).isEqualTo(anchor)
    }

    @Test
    fun createGroupEntity_returnsInitialValue() {
        val pose = Pose.Identity
        val name = "test_entity"
        val parent = FakeEntity()
        val groupEntity = adapter.createGroupEntity(pose, name, parent)

        assertThat(groupEntity).isInstanceOf(FakeEntity::class.java)
        assertThat(groupEntity.getPose()).isEqualTo(pose)
        assertThat(groupEntity.parent).isEqualTo(parent)
    }

    @Test
    fun createInteractableComponent_returnsInitialValue() {
        val listener = TestInputEventListener()

        assertThat(adapter.createInteractableComponent({ command -> command.run() }, listener))
            .isInstanceOf(FakeInteractableComponent::class.java)
    }

    @Test
    fun createMovableComponent_returnsInitialValue() {
        val anchorPlacement: Set<@JvmSuppressWildcards AnchorPlacement> =
            setOf(
                adapter.createAnchorPlacementForPlanes(
                    setOf(PlaneType.HORIZONTAL),
                    setOf(PlaneSemantic.TABLE, PlaneSemantic.FLOOR),
                )
            )

        assertThat(
                adapter.createMovableComponent(
                    systemMovable = false,
                    scaleInZ = false,
                    anchorPlacement = anchorPlacement,
                    shouldDisposeParentAnchor = false,
                )
            )
            .isInstanceOf(FakeMovableComponent::class.java)
    }

    @Test
    fun createResizableComponent_returnsInitialValue() {
        val minimumSize = Dimensions(1.0f, 1.0f, 0.0f)
        val maximumSize = Dimensions(2.0f, 2.0f, 2.0f)
        val resizableComponent = adapter.createResizableComponent(minimumSize, maximumSize)

        assertThat(resizableComponent).isInstanceOf(FakeResizableComponent::class.java)
        assertThat(resizableComponent.minimumSize).isEqualTo(minimumSize)
        assertThat(resizableComponent.maximumSize).isEqualTo(maximumSize)
    }

    @Test
    fun createPointerCaptureComponent_returnsInitialValue() {
        val executor = Executor { command -> command.run() }
        val stateListener: StateListener =
            object : StateListener {
                override fun onStateChanged(@PointerCaptureState newState: Int) {}
            }
        val inputListener = TestInputEventListener()
        val pointerCaptureComponent =
            adapter.createPointerCaptureComponent(executor, stateListener, inputListener)

        assertThat(pointerCaptureComponent).isInstanceOf(FakePointerCaptureComponent::class.java)
        assertThat(pointerCaptureComponent.executor).isEqualTo(executor)
        assertThat(pointerCaptureComponent.stateListener).isEqualTo(stateListener)
    }

    @Test
    fun createSpatialPointerComponent_returnsInitialValue() {
        assertThat(adapter.createSpatialPointerComponent())
            .isInstanceOf(FakeSpatialPointerComponent::class.java)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun createPersistedAnchorEntity_returnsInitialValue() {
        val uuid = UUID(0L, 0L)
        val searchTimeout = Duration.ofMillis(100)
        val persistedAnchorEntity = adapter.createPersistedAnchorEntity(uuid, searchTimeout)

        assertThat(persistedAnchorEntity).isInstanceOf(FakeAnchorEntity::class.java)
        assertThat(persistedAnchorEntity.anchorCreationData.uuid).isEqualTo(uuid)
        assertThat(persistedAnchorEntity.anchorCreationData.searchTimeout).isEqualTo(searchTimeout)
    }

    @Test
    fun setFullSpaceMode_returnsSameBundle() {
        val bundle = Bundle().apply { putString("testkey", "testval") }

        assertThat(adapter.setFullSpaceMode(bundle)).isEqualTo(bundle)
    }

    @Test
    fun setFullSpaceModeWithEnvironmentInherited_returnsSameBundle() {
        val bundle = Bundle().apply { putString("testkey", "testval") }

        assertThat(adapter.setFullSpaceModeWithEnvironmentInherited(bundle)).isEqualTo(bundle)
    }

    @Test
    fun setPreferredAspectRatio_setsLastActivityAndRatio() {
        check(adapter.lastSetPreferredAspectRatioActivity == null)
        check(adapter.lastSetPreferredAspectRatioRatio == -1f)

        val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
        val preferredRatio = 1.23f
        adapter.setPreferredAspectRatio(activity, preferredRatio)

        assertThat(adapter.lastSetPreferredAspectRatioActivity).isEqualTo(activity)
        assertThat(adapter.lastSetPreferredAspectRatioRatio).isEqualTo(preferredRatio)
    }

    private class TestInputEventListener : InputEventListener {
        override fun onInputEvent(event: InputEvent) {}
    }
}
