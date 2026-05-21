/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.testrule

import android.content.Intent
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.RenderViewpoint
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PerceivedResolutionResult
import androidx.xr.scenecore.testing.ActivityPanelEntityTester
import androidx.xr.scenecore.testing.MemoryUtils
import androidx.xr.scenecore.testing.PanelEntityTester
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import java.lang.ref.WeakReference
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class TestRuleEntityTest {

    @get:Rule val scenecoreTestRule = SceneCoreTestRule()

    private lateinit var session: Session

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var renderViewpoint: RenderViewpoint
    private lateinit var panelEntity: PanelEntity
    private lateinit var activityPanelEntity: ActivityPanelEntity

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session

        renderViewpoint = RenderViewpoint.left(session)

        panelEntity =
            PanelEntity.create(
                session,
                view = TextView(activity),
                pixelDimensions = IntSize2d(720, 480),
                name = "test",
            )

        activityPanelEntity = ActivityPanelEntity.create(session, IntSize2d(640, 480), "test")
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun panelEntityGetPerceivedResolution_callsRuntimeAndConverts() {
        val tester = scenecoreTestRule.createTester<PanelEntityTester>(panelEntity)
        val perceivedResolution = IntSize2d(100, 200)

        tester.perceivedResolutionResult = PerceivedResolutionResult.Success(perceivedResolution)

        val result = panelEntity.getPerceivedResolution(renderViewpoint)

        assertThat(result).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        val successResult = result as PerceivedResolutionResult.Success
        assertThat(successResult.perceivedResolution).isEqualTo(perceivedResolution)
    }

    @Test
    fun activityPanelEntityLaunchActivity_callsImplLaunchActivity() {
        val tester = scenecoreTestRule.createTester<ActivityPanelEntityTester>(activityPanelEntity)
        val launchIntent = Intent(session.context, ComponentActivity::class.java)

        activityPanelEntity.startActivity(launchIntent)

        assertThat(tester.startActivityIntent).isEqualTo(launchIntent)
    }

    @Test
    fun activityPanelEntityTransferActivity_callsImplMoveActivity() {
        val tester = scenecoreTestRule.createTester<ActivityPanelEntityTester>(activityPanelEntity)

        activityPanelEntity.transferActivity(activity)

        assertThat(tester.transferredActivity).isEqualTo(activity)
    }

    @Test
    fun transformPixelCoordinatesToLocalPosition_returnsExpectedPositions() {
        // Panel is 720x480. Center (360, 240) should be (0, 0, 0)
        val centerInput = Vector2(360f, 240f)
        assertThat(panelEntity.transformPixelCoordinatesToLocalPosition(centerInput))
            .isEqualTo(Vector3.Zero)

        // Top-left (0, 0) for 720x480 pixels @ 2000 dp/m (0.36m x 0.24m) should be (-0.18, 0.12, 0)
        val topLeftInput = Vector2(0f, 0f)
        assertVector3(
            panelEntity.transformPixelCoordinatesToLocalPosition(topLeftInput),
            Vector3(-0.18f, 0.12f, 0f),
        )

        // ActivityPanel is 640x480 (0.32m x 0.24m).
        // Bottom-right (640, 480) should be (0.16, -0.12, 0)
        val bottomRightInput = Vector2(640f, 480f)
        assertVector3(
            activityPanelEntity.transformPixelCoordinatesToLocalPosition(bottomRightInput),
            Vector3(0.16f, -0.12f, 0f),
        )
    }

    @Test
    fun transformNormalizedCoordinatesToLocalPosition_returnsExpectedPositions() {
        // Center (0, 0) should be (0, 0, 0)
        assertThat(panelEntity.transformNormalizedCoordinatesToLocalPosition(Vector2(0f, 0f)))
            .isEqualTo(Vector3.Zero)

        // (1, 1) on a 0.36m x 0.24m panel should be (0.18, 0.12, 0)
        assertVector3(
            panelEntity.transformNormalizedCoordinatesToLocalPosition(Vector2(1f, 1f)),
            Vector3(0.18f, 0.12f, 0f),
        )
    }

    private fun assertVector3(actual: Vector3, expected: Vector3, epsilon: Float = 1e-5f) {
        assertThat(actual.x).isWithin(epsilon).of(expected.x)
        assertThat(actual.y).isWithin(epsilon).of(expected.y)
        assertThat(actual.z).isWithin(epsilon).of(expected.z)
    }

    @Test
    fun panelEntity_garbageCollection_disposesEntity() {
        fun createPanelEntity(): WeakReference<PanelEntity> {
            val entity =
                PanelEntity.create(
                    session,
                    view = TextView(activity),
                    pixelDimensions = IntSize2d(720, 480),
                    name = "test",
                    parent = null,
                )
            return WeakReference(entity)
        }

        val entityRef = createPanelEntity()
        assertThat(entityRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(entityRef)
    }

    @Test
    fun activityPanelEntity_garbageCollection_disposesEntity() {
        fun createActivityPanelEntity(): WeakReference<ActivityPanelEntity> {
            val entity =
                ActivityPanelEntity.create(session, IntSize2d(320, 240), "test", parent = null)
            return WeakReference(entity)
        }

        val entityRef = createActivityPanelEntity()
        assertThat(entityRef.get()).isNotNull()

        MemoryUtils.assertGarbageCollected(entityRef)
    }
}
