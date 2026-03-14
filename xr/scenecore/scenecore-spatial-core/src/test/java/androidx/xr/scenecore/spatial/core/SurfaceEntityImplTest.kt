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
package androidx.xr.scenecore.spatial.core

import android.app.Activity
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.NodeHolder
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.PerceivedResolutionResult
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeScenePose
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import androidx.xr.scenecore.testing.FakeSurfaceFeature
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.space.ShadowSpatialState
import com.google.common.truth.Truth.assertThat
import java.util.function.Supplier
import kotlin.math.atan
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SurfaceEntityImplTest {
    private val activityController: ActivityController<Activity> =
        Robolectric.buildActivity(Activity::class.java)
    private val activity: Activity = activityController.create().start().get()
    private val xrExtensions = requireNotNull(getXrExtensions())
    private val fakeScheduledExecutorService = FakeScheduledExecutorService()
    private val spatialStateProvider = Supplier { ShadowSpatialState.create() }
    private val viewPlaneResolution = PixelDimensions(2000, 1000)
    private val entityManager = EntityManager()
    private val activitySpaceImpl =
        ActivitySpaceImpl(
            xrExtensions.createNode(),
            activity,
            xrExtensions,
            entityManager,
            spatialStateProvider,
            fakeScheduledExecutorService,
        )
    private val fakeSurfaceFeature =
        FakeSurfaceFeature(NodeHolder<Node>(xrExtensions.createNode(), Node::class.java))

    private val surfaceEntity =
        SurfaceEntityImpl(
            activity,
            fakeSurfaceFeature,
            activitySpaceImpl,
            xrExtensions,
            entityManager,
            fakeScheduledExecutorService,
        )
    private val renderViewScenePose = FakeScenePose()
    private val renderViewFov =
        FieldOfView(
            atan(1.0).toFloat(),
            atan(1.0).toFloat(),
            atan(1.0).toFloat(),
            atan(1.0).toFloat(),
        )

    @Before
    fun setUp() {
        val widthAndHeightConfig =
            "+w" + viewPlaneResolution.width + "dp-h" + viewPlaneResolution.height + "dp"
        RuntimeEnvironment.setQualifiers(widthAndHeightConfig)
        entityManager.addSystemSpaceActivityPose(PerceptionSpaceScenePoseImpl(activitySpaceImpl))
        renderViewScenePose.activitySpacePose = Pose(Vector3(0f, 0f, 0f), Quaternion.Identity)
    }

    @After
    fun tearDown() {
        entityManager.clear()
        surfaceEntity.dispose()
        activitySpaceImpl.dispose()
    }

    private fun assertShapeIsSetCorrectly(expectedShape: SurfaceEntity.Shape) {
        surfaceEntity.shape = expectedShape
        val actualShape = surfaceEntity.shape

        assertThat(actualShape).isInstanceOf(expectedShape::class.java)
        assertThat(actualShape.dimensions).isEqualTo(expectedShape.dimensions)
    }

    @Test
    fun setShape_setsShape() {
        assertShapeIsSetCorrectly(SurfaceEntity.Shape.Quad(FloatSize2d(12f, 12f), 1.5f))
        assertThat((surfaceEntity.shape as SurfaceEntity.Shape.Quad).cornerRadius).isEqualTo(1.5f)
        assertShapeIsSetCorrectly(SurfaceEntity.Shape.Sphere(11f))
        assertShapeIsSetCorrectly(SurfaceEntity.Shape.Hemisphere(10f))
    }

    @Test
    fun setStereoMode_setsStereoMode() {
        surfaceEntity.stereoMode = SurfaceEntity.StereoMode.MONO
        assertThat(surfaceEntity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.MONO)

        surfaceEntity.stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM
        assertThat(surfaceEntity.stereoMode).isEqualTo(SurfaceEntity.StereoMode.TOP_BOTTOM)
    }

    @Test
    fun dispose_supports_reentry() {
        // Note that we don't test that dispose prevents manipulating other properties because that
        // is enforced at the API level, rather than the implementation level.
        surfaceEntity.dispose()
        surfaceEntity.dispose() // shouldn't crash
    }

    @Test
    fun setEdgeFeather_forwardsToFeature() {
        val kFeatherRadiusX = 0.14f
        val kFeatherRadiusY = 0.28f
        val expectedFeather: SurfaceEntity.EdgeFeather =
            SurfaceEntity.EdgeFeather.RectangleFeather(kFeatherRadiusX, kFeatherRadiusY)
        surfaceEntity.edgeFeather = expectedFeather
        val returnedFeather = surfaceEntity.edgeFeather

        assertThat(returnedFeather).isEqualTo(expectedFeather)
    }

    @Test
    fun setColliderEnabled_forwardsToFeature() {
        surfaceEntity.setColliderEnabled(true)

        assertThat(fakeSurfaceFeature.colliderEnabled).isTrue()

        surfaceEntity.setColliderEnabled(false)

        assertThat(fakeSurfaceFeature.colliderEnabled).isFalse()
    }

    @Test
    fun getPerceivedResolution_quadInFront_returnsSuccess() {
        val quadShape = SurfaceEntity.Shape.Quad(FloatSize2d(2.0f, 1.0f)) // 2m wide, 1m high
        fakeSurfaceFeature.shape = quadShape

        surfaceEntity.setPose(Pose(Vector3(0f, 0f, -2f), Quaternion.Identity)) // 2m away
        surfaceEntity.setScale(Vector3(1f, 1f, 1f))

        val result = surfaceEntity.getPerceivedResolution(renderViewScenePose, renderViewFov)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success

        assertThat(successResult.perceivedResolution.width).isEqualTo(500)
        assertThat(successResult.perceivedResolution.height).isEqualTo(250)
    }

    @Test
    fun getPerceivedResolution_sphereInFront_returnsSuccess() {
        val sphereShape = SurfaceEntity.Shape.Sphere(1.0f) // radius 1m
        fakeSurfaceFeature.shape = sphereShape

        surfaceEntity.setPose(Pose(Vector3(0f, 0f, -3f), Quaternion.Identity)) //
        // Center 3m away
        surfaceEntity.setScale(Vector3(1f, 1f, 1f))

        val result = surfaceEntity.getPerceivedResolution(renderViewScenePose, renderViewFov)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success

        assertThat(successResult.perceivedResolution.width).isEqualTo(500)
        assertThat(successResult.perceivedResolution.height).isEqualTo(500)
    }

    @Test
    fun getPerceivedResolution_quadTooClose_returnsEntityTooClose() {
        val quadShape = SurfaceEntity.Shape.Quad(FloatSize2d(2.0f, 1.0f))
        fakeSurfaceFeature.shape = quadShape

        val veryCloseDistance = PERCEIVED_RESOLUTION_EPSILON / 2f
        surfaceEntity.setPose(Pose(Vector3(0f, 0f, -veryCloseDistance), Quaternion.Identity))
        surfaceEntity.setScale(Vector3(1f, 1f, 1f))

        val result = surfaceEntity.getPerceivedResolution(renderViewScenePose, renderViewFov)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)
    }

    @Test
    fun getPerceivedResolution_quadWithScale_calculatesCorrectly() {
        val quadShape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)) // 1m x 1m local
        fakeSurfaceFeature.shape = quadShape

        surfaceEntity.setPose(Pose(Vector3(0f, 0f, -2f), Quaternion.Identity)) // 2m away
        surfaceEntity.setScale(Vector3(2f, 3f, 1f)) // Scaled to 2m wide, 3m high

        val result = surfaceEntity.getPerceivedResolution(renderViewScenePose, renderViewFov)
        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success

        // The width and height are flipped because perceivedResolution calculations will
        // always place the largest dimension as the width, and the second as height.
        assertThat(successResult.perceivedResolution.width).isEqualTo(750)
        assertThat(successResult.perceivedResolution.height).isEqualTo(500)
    }

    @Test
    fun getParent_nullParent_returnsNull() {
        surfaceEntity.parent = null
        assertThat(surfaceEntity.parent).isEqualTo(null)
    }

    @Test
    fun getPoseInParentSpace_nullParent_returnsIdentity() {
        surfaceEntity.parent = null
        surfaceEntity.setPose(Pose.Identity)
        assertThat(surfaceEntity.getPose(Space.PARENT)).isEqualTo(Pose.Identity)
    }

    @Test
    fun getPoseInActivitySpace_nullParent_throwsException() {
        surfaceEntity.parent = null
        Assert.assertThrows(IllegalStateException::class.java) {
            surfaceEntity.getPose(Space.ACTIVITY)
        }
    }

    @Test
    fun getPoseInRealWorldSpace_nullParent_throwsException() {
        surfaceEntity.parent = null
        Assert.assertThrows(IllegalStateException::class.java) {
            surfaceEntity.getPose(Space.REAL_WORLD)
        }
    }
}
