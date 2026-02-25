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
import android.graphics.Rect
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.ActivityPanelEntity
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.node.NodeRepository
import com.android.extensions.xr.space.ShadowActivityPanel
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ActivityPanelEntityImplTest {
    private val xrExtensions = getXrExtensions()
    private val activityController: ActivityController<Activity> =
        Robolectric.buildActivity(Activity::class.java)
    private val hostActivity: Activity = activityController.create().start().get()
    private val windowBoundsPx = PixelDimensions(640, 480)
    private val fakeExecutor = FakeScheduledExecutorService()
    private val nodeRepository: NodeRepository = NodeRepository.getInstance()
    private lateinit var fakeRuntime: SceneRuntime

    @Before
    fun setUp() {
        fakeRuntime =
            SpatialSceneRuntime.create(hostActivity, fakeExecutor, xrExtensions!!, EntityManager())
    }

    @After
    fun tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        fakeRuntime.destroy()
    }

    private fun createActivityPanelEntity(
        windowBoundsPx: PixelDimensions = this.windowBoundsPx
    ): ActivityPanelEntity {
        val mPose = Pose()

        return fakeRuntime.createActivityPanelEntity(
            mPose,
            windowBoundsPx,
            "test",
            hostActivity,
            fakeRuntime.activitySpace,
        )
    }

    @Test
    fun createActivityPanelEntity_returnsActivityPanelEntity() {
        val activityPanelEntity = createActivityPanelEntity()

        Truth.assertThat(activityPanelEntity).isNotNull()
    }

    @Test
    fun createActivityPanelEntity_setsCornersTo32dp() {
        val activityPanelEntity = createActivityPanelEntity()

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter. Validate that the
        // corner radius is set to 32dp.
        Truth.assertThat(activityPanelEntity.cornerRadius).isEqualTo(32.0f)
        Truth.assertThat(
                nodeRepository.getCornerRadius(
                    (activityPanelEntity as ActivityPanelEntityImpl).getNode()
                )
            )
            .isEqualTo(32.0f)
    }

    @Test
    fun createPanel_smallPanelWidth_setsCornerRadiusToPanelSize() {
        val activityPanelEntity = createActivityPanelEntity(PixelDimensions(40, 1000))

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to half the width.
        Truth.assertThat(activityPanelEntity.cornerRadius).isEqualTo(20f)
        Truth.assertThat(
                nodeRepository.getCornerRadius(
                    (activityPanelEntity as ActivityPanelEntityImpl).getNode()
                )
            )
            .isEqualTo(20f)
    }

    @Test
    fun createPanel_smallPanelHeight_setsCornerRadiusToPanelSize() {
        val activityPanelEntity = createActivityPanelEntity(PixelDimensions(1000, 40))

        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter.
        // Validate that the corner radius is set to half the height.
        Truth.assertThat(activityPanelEntity.cornerRadius).isEqualTo(20f)
        Truth.assertThat(
                nodeRepository.getCornerRadius(
                    (activityPanelEntity as ActivityPanelEntityImpl).getNode()
                )
            )
            .isEqualTo(20f)
    }

    @Test
    fun activityPanelEntityStartActivity_callsActivityPanel() {
        val activityPanelEntity = createActivityPanelEntity()
        val launchIntent = activityController.getIntent()
        activityPanelEntity.launchActivity(launchIntent, null)

        val panel =
            ShadowActivityPanel.extract(
                ShadowXrExtensions.extract(xrExtensions).getActivityPanelForHost(hostActivity)
            )

        Truth.assertThat(panel.launchIntent).isEqualTo(launchIntent)
        Truth.assertThat(panel.bundle).isNull()
        Truth.assertThat(panel.bounds)
            .isEqualTo(Rect(0, 0, windowBoundsPx.width, windowBoundsPx.height))
    }

    @Test
    fun activityPanelEntityMoveActivity_callActivityPanel() {
        val activityPanelEntity = createActivityPanelEntity()
        activityPanelEntity.moveActivity(hostActivity)

        val panel =
            ShadowActivityPanel.extract(
                ShadowXrExtensions.extract(xrExtensions).getActivityPanelForHost(hostActivity)
            )

        Truth.assertThat(panel.activity).isEqualTo(hostActivity)

        Truth.assertThat(panel.bounds)
            .isEqualTo(Rect(0, 0, windowBoundsPx.width, windowBoundsPx.height))
    }

    @Test
    fun activityPanelEntitySetSize_callsSetSizeInPixels() {
        val activityPanelEntity = createActivityPanelEntity()
        val dimensions = Dimensions(400f, 300f, 0f)
        activityPanelEntity.size = dimensions

        val panel = ShadowXrExtensions.extract(xrExtensions).getActivityPanelForHost(hostActivity)

        Truth.assertThat(ShadowActivityPanel.extract(panel).bounds)
            .isEqualTo(Rect(0, 0, dimensions.width.toInt(), dimensions.height.toInt()))

        // SetSize redirects to setSizeInPixels, so we check the same thing here.
        val viewDimensions = activityPanelEntity.sizeInPixels

        Truth.assertThat(viewDimensions.width).isEqualTo(dimensions.width.toInt())
        Truth.assertThat(viewDimensions.height).isEqualTo(dimensions.height.toInt())
    }

    @Test
    fun activityPanelEntity_setSizeInPixels_callActivityPanel() {
        val activityPanelEntity = createActivityPanelEntity()
        val dimensions = PixelDimensions(400, 300)
        activityPanelEntity.sizeInPixels = dimensions

        val panel = ShadowXrExtensions.extract(xrExtensions).getActivityPanelForHost(hostActivity)

        Truth.assertThat(ShadowActivityPanel.extract(panel).bounds)
            .isEqualTo(Rect(0, 0, dimensions.width, dimensions.height))

        val viewDimensions = activityPanelEntity.sizeInPixels

        Truth.assertThat(viewDimensions.width).isEqualTo(dimensions.width)
        Truth.assertThat(viewDimensions.height).isEqualTo(dimensions.height)
    }

    @Test
    fun activityPanelEntityDispose_callsActivityPanelDelete() {
        val activityPanelEntity = createActivityPanelEntity()
        activityPanelEntity.dispose()

        val panel = ShadowXrExtensions.extract(xrExtensions).getActivityPanelForHost(hostActivity)

        Truth.assertThat(ShadowActivityPanel.extract(panel).isDeleted).isTrue()
    }

    @Test
    fun transformPixelCoordinatesToLocalPosition_topLeft_returnsCorrectPosition() {
        val activityPanelEntity = createActivityPanelEntity()
        activityPanelEntity.size = Dimensions(1f, 1f, 0f)
        val position = activityPanelEntity.transformPixelCoordinatesToLocalPosition(Vector2(0f, 0f))
        val expected = Vector3(-0.5f, 0.5f, 0.0f)
        Truth.assertThat(position).isEqualTo(expected)
    }

    @Test
    fun transformNormalizedCoordinatesToLocalPosition_center_returnsZeroVector() {
        val activityPanelEntity = createActivityPanelEntity()
        val position =
            activityPanelEntity.transformNormalizedCoordinatesToLocalPosition(Vector2(0f, 0f))
        Truth.assertThat(position).isEqualTo(Vector3.Zero)
    }

    @Test
    fun transformNormalizedCoordinatesToLocalPosition_topLeft_returnsCorrectPosition() {
        val activityPanelEntity = createActivityPanelEntity()
        activityPanelEntity.size = Dimensions(1f, 1f, 0f)
        val position =
            activityPanelEntity.transformNormalizedCoordinatesToLocalPosition(Vector2(-1f, 1f))
        val expected = Vector3(-0.5f, 0.5f, 0.0f)
        Truth.assertThat(position).isEqualTo(expected)
    }

    @Test
    fun getParent_nullParent_returnsNull() {
        val activityPanelEntity =
            fakeRuntime.createActivityPanelEntity(
                Pose(),
                windowBoundsPx,
                "test",
                hostActivity,
                /* parent= */ null,
            )

        Truth.assertThat(activityPanelEntity.parent).isEqualTo(null)
    }

    @Test
    fun getPoseInParentSpace_nullParent_returnsIdentity() {
        val activityPanelEntity =
            fakeRuntime.createActivityPanelEntity(
                Pose(),
                windowBoundsPx,
                "test",
                hostActivity,
                /* parent= */ null,
            )

        activityPanelEntity.setPose(Pose.Identity)
        Truth.assertThat(activityPanelEntity.getPose(Space.PARENT)).isEqualTo(Pose.Identity)
    }

    @Test
    fun getPoseInActivitySpace_nullParent_throwsException() {
        val activityPanelEntity =
            fakeRuntime.createActivityPanelEntity(
                Pose(),
                windowBoundsPx,
                "test",
                hostActivity,
                /* parent= */ null,
            )

        Assert.assertThrows(IllegalStateException::class.java) {
            activityPanelEntity.getPose(Space.ACTIVITY)
        }
    }

    @Test
    fun getPoseInRealWorldSpace_nullParent_throwsException() {
        val activityPanelEntity =
            fakeRuntime.createActivityPanelEntity(
                Pose(),
                windowBoundsPx,
                "test",
                hostActivity,
                /* parent= */ null,
            )

        Assert.assertThrows(IllegalStateException::class.java) {
            activityPanelEntity.getPose(Space.REAL_WORLD)
        }
    }
}
