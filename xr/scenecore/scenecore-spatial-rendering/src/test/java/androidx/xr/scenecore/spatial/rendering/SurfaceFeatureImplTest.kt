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
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.scenecore.impl.impress.FakeImpressApiImpl
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider
import com.google.androidxr.splitengine.SplitEngineSubspaceManager
import com.google.androidxr.splitengine.SubspaceNode
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SurfaceFeatureImplTest {

    private lateinit var surfaceFeature: SurfaceFeatureImpl
    // TODO: Update this test to handle Entity.dispose() for Exceptions when FakeImpress is
    //       updated.
    private lateinit var impressApi: ImpressApi
    private val fakeImpressApi = FakeImpressApiImpl()
    private val xrExtensions = XrExtensionsProvider.getXrExtensions()!!
    private val splitEngineSubspaceManager = mock(SplitEngineSubspaceManager::class.java)

    @Before
    fun setUp() {
        val activityController = Robolectric.buildActivity(Activity::class.java)
        activityController.create().start().get()

        impressApi = mock(ImpressApi::class.java)
        `when`(impressApi.createImpressNode()).thenReturn(fakeImpressApi.createImpressNode())
        `when`(impressApi.createStereoSurface(any(), any(), any()))
            .thenReturn(fakeImpressApi.createImpressNode())

        Assert.assertNotNull(xrExtensions)
        val node = xrExtensions.createNode()
        val expectedSubspaceNode = SubspaceNode(SUBSPACE_ID, node)

        `when`(splitEngineSubspaceManager.createSubspace(anyString(), anyInt()))
            .thenReturn(expectedSubspaceNode)

        createDefaultSurfaceFeature(SurfaceEntity.Shape.Quad(FloatSize2d(1f, 1f), 0.0f))
    }

    @After
    fun tearDown() {
        if (::surfaceFeature.isInitialized) {
            surfaceFeature.dispose()
        }
    }

    private fun createSurfaceFeature(
        surfaceProtection: Int,
        shape: SurfaceEntity.Shape,
    ): SurfaceFeatureImpl {
        val stereoMode = SurfaceEntity.StereoMode.MONO
        val mediaBlendingMode = SurfaceEntity.MediaBlendingMode.TRANSPARENT
        val useSuperSampling = 0

        return SurfaceFeatureImpl(
            impressApi,
            splitEngineSubspaceManager,
            xrExtensions,
            stereoMode,
            mediaBlendingMode,
            shape,
            surfaceProtection,
            useSuperSampling,
        )
    }

    private fun createDefaultSurfaceFeature(shape: SurfaceEntity.Shape): SurfaceFeatureImpl {
        surfaceFeature = createSurfaceFeature(SurfaceEntity.SurfaceProtection.NONE, shape)
        return surfaceFeature
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    fun setShape_setsShape() {
        var expectedShape: SurfaceEntity.Shape =
            SurfaceEntity.Shape.Quad(FloatSize2d(12f, 12f), 0.0f)
        surfaceFeature.shape = expectedShape
        var shape = surfaceFeature.shape

        assertThat(shape.javaClass).isEqualTo(expectedShape.javaClass)
        assertThat(shape.dimensions).isEqualTo(expectedShape.dimensions)
        verify(impressApi)
            .setStereoSurfaceEntityCanvasShapeQuad(surfaceFeature.entityImpressNode, 12f, 12f, 0.0f)

        expectedShape = SurfaceEntity.Shape.Sphere(11f)
        surfaceFeature.shape = expectedShape
        shape = surfaceFeature.shape

        assertThat(shape.javaClass).isEqualTo(expectedShape.javaClass)
        assertThat(shape.dimensions).isEqualTo(expectedShape.dimensions)
        verify(impressApi)
            .setStereoSurfaceEntityCanvasShapeSphere(surfaceFeature.entityImpressNode, 11f)

        expectedShape = SurfaceEntity.Shape.Hemisphere(10f)
        surfaceFeature.shape = expectedShape
        shape = surfaceFeature.shape

        assertThat(shape.javaClass).isEqualTo(expectedShape.javaClass)
        assertThat(shape.dimensions).isEqualTo(expectedShape.dimensions)
        verify(impressApi)
            .setStereoSurfaceEntityCanvasShapeHemisphere(surfaceFeature.entityImpressNode, 10f)
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    fun setStereoMode_setsStereoMode() {
        var expectedStereoMode = SurfaceEntity.StereoMode.MONO
        surfaceFeature.stereoMode = expectedStereoMode
        var stereoMode = surfaceFeature.stereoMode

        assertThat(stereoMode).isEqualTo(expectedStereoMode)
        verify(impressApi)
            .setStereoModeForStereoSurface(surfaceFeature.entityImpressNode, expectedStereoMode)

        expectedStereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM
        surfaceFeature.stereoMode = expectedStereoMode
        stereoMode = surfaceFeature.stereoMode

        assertThat(stereoMode).isEqualTo(expectedStereoMode)
        verify(impressApi)
            .setStereoModeForStereoSurface(surfaceFeature.entityImpressNode, expectedStereoMode)
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    fun dispose_supports_reentry() {
        val quadShape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f), 0.0f) // 1m x 1m local
        surfaceFeature = createDefaultSurfaceFeature(quadShape)

        // Note that we don't test that dispose prevents manipulating other properties because that
        // is enforced at the API level, rather than the implementation level.
        surfaceFeature.dispose()
        surfaceFeature.dispose() // shouldn't crash
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    fun setColliderEnabled_forwardsToImpress() {
        surfaceFeature.setColliderEnabled(true)

        verify(impressApi)
            .setStereoSurfaceEntityColliderEnabled(surfaceFeature.entityImpressNode, true)

        // Set back to false
        surfaceFeature.setColliderEnabled(false)

        verify(impressApi)
            .setStereoSurfaceEntityColliderEnabled(surfaceFeature.entityImpressNode, false)
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    fun setEdgeFeather_forwardsToImpress() {
        val kFeatherRadiusX = 0.14f
        val kFeatherRadiusY = 0.28f
        var expectedFeather: SurfaceEntity.EdgeFeather =
            SurfaceEntity.EdgeFeather.RectangleFeather(kFeatherRadiusX, kFeatherRadiusY)
        surfaceFeature.edgeFeather = expectedFeather
        var returnedFeather = surfaceFeature.edgeFeather

        assertThat(returnedFeather).isEqualTo(expectedFeather)
        verify(impressApi)
            .setFeatherRadiusForStereoSurface(
                surfaceFeature.entityImpressNode,
                kFeatherRadiusX,
                kFeatherRadiusY,
            )

        // Set back to NoFeathering to simulate turning feathering off
        expectedFeather = SurfaceEntity.EdgeFeather.NoFeathering()
        surfaceFeature.edgeFeather = expectedFeather
        returnedFeather = surfaceFeature.edgeFeather

        assertThat(returnedFeather).isEqualTo(expectedFeather)
        verify(impressApi)
            .setFeatherRadiusForStereoSurface(surfaceFeature.entityImpressNode, 0f, 0f)
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    fun createSurfaceEntity_returnsStereoSurface() {
        val kTestWidth = 14.0f
        val kTestHeight = 28.0f
        val kTestSphereRadius = 7.0f
        val kTestHemisphereRadius = 11.0f

        val surfaceEntityQuad =
            SurfaceFeatureImpl(
                fakeImpressApi,
                splitEngineSubspaceManager,
                xrExtensions,
                SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                SurfaceEntity.MediaBlendingMode.TRANSPARENT,
                SurfaceEntity.Shape.Quad(FloatSize2d(kTestWidth, kTestHeight), 0.0f),
                SurfaceEntity.SurfaceProtection.NONE,
                SurfaceEntity.SuperSampling.DEFAULT,
            )
        var quadData =
            fakeImpressApi.getStereoSurfaceEntities()[surfaceEntityQuad.entityImpressNode]!!

        val surfaceEntitySphere =
            SurfaceFeatureImpl(
                fakeImpressApi,
                splitEngineSubspaceManager,
                xrExtensions,
                SurfaceEntity.StereoMode.TOP_BOTTOM,
                SurfaceEntity.MediaBlendingMode.TRANSPARENT,
                SurfaceEntity.Shape.Sphere(kTestSphereRadius),
                SurfaceEntity.SurfaceProtection.NONE,
                SurfaceEntity.SuperSampling.DEFAULT,
            )
        val sphereData =
            fakeImpressApi.getStereoSurfaceEntities()[surfaceEntitySphere.entityImpressNode]!!

        val surfaceEntityHemisphere =
            SurfaceFeatureImpl(
                fakeImpressApi,
                splitEngineSubspaceManager,
                xrExtensions,
                SurfaceEntity.StereoMode.MONO,
                SurfaceEntity.MediaBlendingMode.TRANSPARENT,
                SurfaceEntity.Shape.Hemisphere(kTestHemisphereRadius),
                SurfaceEntity.SurfaceProtection.NONE,
                SurfaceEntity.SuperSampling.DEFAULT,
            )
        val hemisphereData =
            fakeImpressApi.getStereoSurfaceEntities()[surfaceEntityHemisphere.entityImpressNode]!!

        assertThat(fakeImpressApi.getStereoSurfaceEntities()).hasSize(3)

        // TODO: b/366588688 - Move these into tests for SurfaceEntityImpl
        assertThat(quadData.stereoMode).isEqualTo(SurfaceEntity.StereoMode.SIDE_BY_SIDE)
        assertThat(quadData.mediaBlendingMode)
            .isEqualTo(SurfaceEntity.MediaBlendingMode.TRANSPARENT)
        assertThat(quadData.canvasShape)
            .isEqualTo(FakeImpressApiImpl.StereoSurfaceEntityData.CanvasShape.QUAD)
        assertThat(sphereData.stereoMode).isEqualTo(SurfaceEntity.StereoMode.TOP_BOTTOM)
        assertThat(sphereData.canvasShape)
            .isEqualTo(FakeImpressApiImpl.StereoSurfaceEntityData.CanvasShape.VR_360_SPHERE)
        assertThat(hemisphereData.stereoMode).isEqualTo(SurfaceEntity.StereoMode.MONO)
        assertThat(hemisphereData.canvasShape)
            .isEqualTo(FakeImpressApiImpl.StereoSurfaceEntityData.CanvasShape.VR_180_HEMISPHERE)

        assertThat(quadData.width).isEqualTo(kTestWidth)
        assertThat(quadData.height).isEqualTo(kTestHeight)
        val quadDimensions = surfaceEntityQuad.dimensions
        assertThat(quadDimensions.width).isEqualTo(kTestWidth)
        assertThat(quadDimensions.height).isEqualTo(kTestHeight)
        assertThat(quadDimensions.depth).isEqualTo(0.0f)

        assertThat(sphereData.radius).isEqualTo(kTestSphereRadius)
        val sphereDimensions = surfaceEntitySphere.dimensions
        assertThat(sphereDimensions.width).isEqualTo(kTestSphereRadius * 2.0f)
        assertThat(sphereDimensions.height).isEqualTo(kTestSphereRadius * 2.0f)
        assertThat(sphereDimensions.depth).isEqualTo(kTestSphereRadius * 2.0f)

        assertThat(hemisphereData.radius).isEqualTo(kTestHemisphereRadius)
        val hemisphereDimensions = surfaceEntityHemisphere.dimensions
        assertThat(hemisphereDimensions.width).isEqualTo(kTestHemisphereRadius * 2.0f)
        assertThat(hemisphereDimensions.height).isEqualTo(kTestHemisphereRadius * 2.0f)
        assertThat(hemisphereDimensions.depth).isEqualTo(kTestHemisphereRadius)

        assertThat(quadData.surface).isEqualTo(surfaceEntityQuad.surface)
        assertThat(sphereData.surface).isEqualTo(surfaceEntitySphere.surface)
        assertThat(hemisphereData.surface).isEqualTo(surfaceEntityHemisphere.surface)

        // Check that calls to set the Shape and StereoMode after construction call through
        // Change the Quad to a Sphere
        surfaceEntityQuad.shape = SurfaceEntity.Shape.Sphere(kTestSphereRadius)
        // change the StereoMode to Top/Bottom from Side/Side
        surfaceEntityQuad.stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM
        quadData = fakeImpressApi.getStereoSurfaceEntities()[surfaceEntityQuad.entityImpressNode]!!

        assertThat(quadData.canvasShape)
            .isEqualTo(FakeImpressApiImpl.StereoSurfaceEntityData.CanvasShape.VR_360_SPHERE)
        assertThat(quadData.stereoMode).isEqualTo(SurfaceEntity.StereoMode.TOP_BOTTOM)

        val surface = surfaceEntityQuad.surface

        assertThat(surface).isNotNull()
        assertThat(surface).isEqualTo(quadData.surface)
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    fun setPixelDimensions_throwsOnProtectedSurface() {
        val protectedSurfaceFeature =
            createSurfaceFeature(
                SurfaceEntity.SurfaceProtection.PROTECTED,
                SurfaceEntity.Shape.Quad(FloatSize2d(1f, 1f), 0.0f),
            )
        var exceptionThrown = false
        try {
            protectedSurfaceFeature.setSurfacePixelDimensions(14, 14)
        } catch (e: IllegalStateException) {
            exceptionThrown = true
        }
        assertThat(exceptionThrown).isTrue()
    }

    @Ignore // b/428211243 this test currently leaks android.view.Surface
    @Test
    fun setPixelDimensions_forwardsToImpress() {
        val kTestWidth = 100
        val kTestHeight = 200
        surfaceFeature.setSurfacePixelDimensions(kTestWidth, kTestHeight)
        verify(impressApi)
            .setStereoSurfaceEntitySurfaceSize(
                surfaceFeature.entityImpressNode,
                kTestWidth,
                kTestHeight,
            )
    }

    companion object {
        private const val SUBSPACE_ID = 5
    }
}
