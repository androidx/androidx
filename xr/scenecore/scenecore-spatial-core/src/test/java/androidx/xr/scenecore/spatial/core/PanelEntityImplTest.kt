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
import android.hardware.display.DisplayManager
import android.view.View
import android.view.ViewGroup
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PerceivedResolutionResult
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeScenePose
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.node.NodeRepository
import com.google.common.truth.Truth
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
class PanelEntityImplTest {
    private val xrExtensions = getXrExtensions()
    private val activityController: ActivityController<Activity> =
        Robolectric.buildActivity(Activity::class.java)
    private val activity: Activity = activityController.create().start().get()
    private val fakeExecutor = FakeScheduledExecutorService()
    private val entityManager = EntityManager()
    private val nodeRepository: NodeRepository = NodeRepository.getInstance()
    private val pixelDimensions = PixelDimensions(2000, 1000)
    private lateinit var sceneRuntime: SpatialSceneRuntime
    private var renderViewScenePose: FakeScenePose = FakeScenePose()
    private lateinit var renderViewFov: FieldOfView

    @Before
    fun setUp() {
        val widthAndHeightConfig =
            "+w" + pixelDimensions.width + "dp-h" + pixelDimensions.height + "dp"
        RuntimeEnvironment.setQualifiers(widthAndHeightConfig)
        sceneRuntime =
            SpatialSceneRuntime.create(activity, fakeExecutor, xrExtensions!!, entityManager)
        renderViewScenePose.activitySpacePose = Pose(Vector3(0f, 0f, 0f), Quaternion.Identity)
        renderViewFov =
            FieldOfView(
                atan(1.0).toFloat(),
                atan(1.0).toFloat(),
                atan(1.0).toFloat(),
                atan(1.0).toFloat(),
            )
    }

    @After
    fun tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        sceneRuntime.destroy()
        entityManager.clear()
    }

    private fun createPanelEntity(surfaceDimensionsPx: Dimensions): PanelEntityImpl {
        val display = activity.getSystemService(DisplayManager::class.java).displays[0]
        val displayContext = activity.createDisplayContext(display!!)
        val view = View(displayContext)
        view.setLayoutParams(ViewGroup.LayoutParams(640, 480))
        val node = xrExtensions!!.createNode()

        val panelEntity =
            PanelEntityImpl(
                displayContext,
                node,
                view,
                xrExtensions,
                entityManager,
                PixelDimensions(
                    surfaceDimensionsPx.width.toInt(),
                    surfaceDimensionsPx.height.toInt(),
                ),
                "panel",
                fakeExecutor,
            )

        // TODO(b/352829122): introduce a TestRootEntity which can serve as a parent
        panelEntity.parent = sceneRuntime.activitySpace
        return panelEntity
    }

    @Test
    fun getSizeForPanelEntity_returnsSizeInMeters() {
        val panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX)

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        Truth.assertThat(panelEntity.size.width).isEqualTo(640f)
        Truth.assertThat(panelEntity.size.height).isEqualTo(480f)
        Truth.assertThat(panelEntity.size.depth).isEqualTo(0f)
    }

    @Test
    fun setSizeForPanelEntity_setsSize() {
        val panelEntity = createPanelEntity(K_HD_RESOLUTION_PX)

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        Truth.assertThat(panelEntity.size.width).isEqualTo(1280f)
        Truth.assertThat(panelEntity.size.height).isEqualTo(720f)
        Truth.assertThat(panelEntity.size.depth).isEqualTo(0f)

        panelEntity.size = K_VGA_RESOLUTION_PX

        Truth.assertThat(panelEntity.size.width).isEqualTo(640f)
        Truth.assertThat(panelEntity.size.height).isEqualTo(480f)
        Truth.assertThat(panelEntity.size.depth).isEqualTo(0f)
    }

    @Test
    fun setSizeForPanelEntity_updatesPixelDimensions() {
        val panelEntity = createPanelEntity(K_HD_RESOLUTION_PX)

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        Truth.assertThat(panelEntity.size.width).isEqualTo(1280f)
        Truth.assertThat(panelEntity.size.height).isEqualTo(720f)
        Truth.assertThat(panelEntity.size.depth).isEqualTo(0f)

        panelEntity.size = K_VGA_RESOLUTION_PX

        Truth.assertThat(panelEntity.size.width).isEqualTo(640f)
        Truth.assertThat(panelEntity.size.height).isEqualTo(480f)
        Truth.assertThat(panelEntity.size.depth).isEqualTo(0f)
        Truth.assertThat(panelEntity.sizeInPixels.width).isEqualTo(640)
        Truth.assertThat(panelEntity.sizeInPixels.height).isEqualTo(480)
    }

    @Test
    fun createPanel_setsCornerRadius() {
        val panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX)

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to 32dp.
        Truth.assertThat(panelEntity.cornerRadius).isEqualTo(32.0f)
        Truth.assertThat(nodeRepository.getCornerRadius(panelEntity.getNode())).isEqualTo(32.0f)
    }

    @Test
    fun createPanel_smallPanelWidth_setsCornerRadiusToPanelSize() {
        val panelEntity = createPanelEntity(Dimensions(40f, 1000f, 0f))

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to 32dp.
        Truth.assertThat(panelEntity.cornerRadius).isEqualTo(20f)
        Truth.assertThat(nodeRepository.getCornerRadius(panelEntity.getNode())).isEqualTo(20f)
    }

    @Test
    fun createPanel_smallPanelHeight_setsCornerRadiusToPanelSize() {
        val panelEntity = createPanelEntity(Dimensions(1000f, 40f, 0f))

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to 32dp.
        Truth.assertThat(panelEntity.cornerRadius).isEqualTo(20f)
        Truth.assertThat(nodeRepository.getCornerRadius(panelEntity.getNode())).isEqualTo(20f)
    }

    @Test
    fun getPerceivedResolution_validCameraAndPanelInFront_returnsSuccess() {
        // Panel created with PixelDimensions(2,1). With pixel density 1.0, size is 2m x 1m.
        val panelEntity = createPanelEntity(Dimensions(2f, 1f, 0f))

        // Place panel 2m in front of camera. Camera is at (0,0,0). Panel at (0,0,-2).
        // Panel is parented to ActivitySpaceRoot (identity pose and scale by default).
        // Panel's local pose becomes its activity space pose.
        // Panel's local scale is (1,1,1) by default from FakeXrExtensions.
        // So, panelEntity.getScale(Space.ACTIVITY) should be (1,1,1).
        panelEntity.setPose(Pose(Vector3(0f, 0f, -2f), Quaternion.Identity))

        val result = panelEntity.getPerceivedResolution(renderViewScenePose, renderViewFov)

        Truth.assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)

        val successResult = result as PerceivedResolutionResult.Success

        // Expected calculation:
        // Panel size: 2m width, 1m height (since pixel density is 1.0)
        // Panel scale in activity space: (1,1,1)
        // Effective panel size in activity space: 2m x 1m
        // Panel distance: 2m
        // Camera FOV: 90deg H & V. Display: 1000x1000px.
        // View plane at 2m distance: width = 2 * (tan(45) + tan(45)) = 2 * (1+1) = 4m
        //                             height = 2 * (tan(45) + tan(45)) = 2 * (1+1) = 4m
        // Panel width ratio in view plane = 2m / 4m = 0.5
        // Panel height ratio in view plane = 1m / 4m = 0.25
        // Perceived pixel width = 0.5 * 1000px = 500
        // Perceived pixel height = 0.25 * 1000px = 250
        Truth.assertThat(successResult.perceivedResolution.width).isEqualTo(500)
        Truth.assertThat(successResult.perceivedResolution.height).isEqualTo(250)
    }

    @Test
    fun getPerceivedResolution_panelTooClose_returnsEntityTooClose() {
        val panelEntity = createPanelEntity(Dimensions(2f, 1f, 0f))

        // Place panel very close to the camera (distance < EPSILON)
        val veryCloseDistance = PERCEIVED_RESOLUTION_EPSILON / 2f
        panelEntity.setPose(Pose(Vector3(0f, 0f, -veryCloseDistance), Quaternion.Identity))

        val result = panelEntity.getPerceivedResolution(renderViewScenePose, renderViewFov)

        Truth.assertThat(result).isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)
    }

    @Test
    fun getPerceivedResolution_panelAtEpsilonDistance_returnsEntityTooClose() {
        val panelEntity = createPanelEntity(Dimensions(2f, 1f, 0f))

        // Place panel exactly at EPSILON distance
        panelEntity.setPose(
            Pose(Vector3(0f, 0f, -PERCEIVED_RESOLUTION_EPSILON), Quaternion.Identity)
        )

        val result = panelEntity.getPerceivedResolution(renderViewScenePose, renderViewFov)

        Truth.assertThat(result).isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)
    }

    @Test
    fun getPerceivedResolution_panelWithScale_calculatesCorrectly() {
        val panelEntity = createPanelEntity(Dimensions(1f, 1f, 0f)) // 1m x 1m

        // local size
        panelEntity.setPose(Pose(Vector3(0f, 0f, -2f), Quaternion.Identity))
        panelEntity.setScale(Vector3(2f, 3f, 1f)) // Scale the panel

        val result = panelEntity.getPerceivedResolution(renderViewScenePose, renderViewFov)

        Truth.assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)

        val successResult = result as PerceivedResolutionResult.Success

        // Expected calculation:
        // Local panel size: 1m width, 1m height
        // Panel scale in activity space: (2,3,1)
        // Effective panel size in activity space: 1m*2f = 2m width, 1m*3f = 3m height
        // Panel distance: 2m
        // View plane at 2m distance: 4m x 4m
        // Panel width ratio in view plane = 2m / 4m = 0.5
        // Panel height ratio in view plane = 3m / 4m = 0.75
        // Perceived pixel width = 0.5 * 1000px = 500
        // Perceived pixel height = 0.75 * 1000px = 750
        Truth.assertThat(successResult.perceivedResolution.width).isEqualTo(500)
        Truth.assertThat(successResult.perceivedResolution.height).isEqualTo(750)
    }

    @Test
    fun transformPixelCoordinatesToLocalPosition_center_returnsZeroVector() {
        val panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX) // 640px x 480px
        val position = panelEntity.transformPixelCoordinatesToLocalPosition(Vector2(320f, 240f))
        Truth.assertThat(position).isEqualTo(Vector3.Zero)
    }

    @Test
    fun transformPixelCoordinatesToLocalPosition_topLeft_returnsCorrectPosition() {
        val panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX) // 640px x 480px
        val position = panelEntity.transformPixelCoordinatesToLocalPosition(Vector2(0f, 0f))
        val expected = Vector3(panelEntity.size.width * -0.5f, panelEntity.size.height * 0.5f, 0.0f)
        Truth.assertThat(position).isEqualTo(expected)
    }

    @Test
    fun transformPixelCoordinatesToLocalPosition_bottomRight_returnsCorrectPosition() {
        val panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX) // 640px x 480px
        val position = panelEntity.transformPixelCoordinatesToLocalPosition(Vector2(640f, 480f))
        val expected = Vector3(panelEntity.size.width * 0.5f, panelEntity.size.height * -0.5f, 0.0f)
        Truth.assertThat(position).isEqualTo(expected)
    }

    @Test
    fun transformNormalizedCoordinatesToLocalPosition_center_returnsZeroVector() {
        val panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX)
        val position = panelEntity.transformNormalizedCoordinatesToLocalPosition(Vector2(0f, 0f))
        Truth.assertThat(position).isEqualTo(Vector3.Zero)
    }

    @Test
    fun transformNormalizedCoordinatesToLocalPosition_topLeft_returnsCorrectPosition() {
        val panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX)
        val width = 10.0f
        val height = 20.0f
        panelEntity.size = Dimensions(width, height, 0.0f)
        val position = panelEntity.transformNormalizedCoordinatesToLocalPosition(Vector2(-1f, 1f))
        val expected = Vector3(-width / 2, height / 2, 0.0f)
        Truth.assertThat(position).isEqualTo(expected)
    }

    @Test
    fun transformNormalizedCoordinatesToLocalPosition_bottomRight_returnsCorrectPosition() {
        val panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX)
        val width = 10.0f
        val height = 20.0f
        panelEntity.size = Dimensions(width, height, 0.0f)
        val position = panelEntity.transformNormalizedCoordinatesToLocalPosition(Vector2(1f, -1f))
        val expected = Vector3(width / 2, -height / 2, 0.0f)
        Truth.assertThat(position).isEqualTo(expected)
    }

    @Test
    fun getParent_nullParent_returnsNull() {
        val panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX)
        panelEntity.parent = null
        Truth.assertThat(panelEntity.parent).isEqualTo(null)
    }

    @Test
    fun getPoseInParentSpace_nullParent_returnsIdentity() {
        val panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX)
        panelEntity.parent = null
        panelEntity.setPose(Pose.Identity)
        Truth.assertThat(panelEntity.getPose(Space.PARENT)).isEqualTo(Pose.Identity)
    }

    @Test
    fun getPoseInActivitySpace_nullParent_throwsException() {
        val panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX)
        panelEntity.parent = null
        Assert.assertThrows(IllegalStateException::class.java) {
            panelEntity.getPose(Space.ACTIVITY)
        }
    }

    @Test
    fun getPoseInRealWorldSpace_nullParent_throwsException() {
        val panelEntity = createPanelEntity(K_VGA_RESOLUTION_PX)
        panelEntity.parent = null
        Assert.assertThrows(IllegalStateException::class.java) {
            panelEntity.getPose(Space.REAL_WORLD)
        }
    }

    companion object {
        private val K_VGA_RESOLUTION_PX = Dimensions(640f, 480f, 0f)
        private val K_HD_RESOLUTION_PX = Dimensions(1280f, 720f, 0f)
    }
}
