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
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.NodeRepository
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class MainPanelEntityImplTest {
    private val xrExtensions: XrExtensions = getXrExtensions()!!
    private val activityController: ActivityController<Activity> =
        Robolectric.buildActivity(Activity::class.java)
    private val hostActivity: Activity = activityController.create().start().get()
    private val fakeExecutor = FakeScheduledExecutorService()
    private lateinit var sceneRuntime: SpatialSceneRuntime
    private lateinit var mainPanelEntity: MainPanelEntityImpl

    @Before
    fun setUp() {
        sceneRuntime =
            SpatialSceneRuntime.create(hostActivity, fakeExecutor, xrExtensions, EntityManager())

        mainPanelEntity = sceneRuntime.mainPanelEntity as MainPanelEntityImpl
    }

    @After
    fun tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        sceneRuntime.destroy()
    }

    @Test
    fun runtimeGetMainPanelEntity_returnsPanelEntityImpl() {
        Truth.assertThat(mainPanelEntity).isNotNull()
    }

    @Test
    fun mainPanelEntitySetSizeInPixels_callsExtensions() {
        val kTestPixelDimensions = PixelDimensions(14, 14)
        mainPanelEntity.sizeInPixels = kTestPixelDimensions

        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)
        Truth.assertThat(shadowXrExtensions.getMainWindowWidth(hostActivity))
            .isEqualTo(kTestPixelDimensions.width)
        Truth.assertThat(shadowXrExtensions.getMainWindowHeight(hostActivity))
            .isEqualTo(kTestPixelDimensions.height)
    }

    @Test
    fun mainPanelEntitySetSize_callsExtensions() {
        val kTestDimensions = Dimensions(123.0f, 123.0f, 123.0f)
        mainPanelEntity.size = kTestDimensions

        val shadowXrExtensions = ShadowXrExtensions.extract(xrExtensions)
        Truth.assertThat(shadowXrExtensions.getMainWindowWidth(hostActivity))
            .isEqualTo(kTestDimensions.width.toInt())
        Truth.assertThat(shadowXrExtensions.getMainWindowHeight(hostActivity))
            .isEqualTo(kTestDimensions.height.toInt())
    }

    @Test
    fun createActivityPanelEntity_setsCornersTo32Dp() {
        // The (FakeXrExtensions) test default pixel density is 1 pixel per meter. Validate that the
        // corner radius is set to 32dp.
        Truth.assertThat(mainPanelEntity.cornerRadius).isEqualTo(32.0f)
        Truth.assertThat(NodeRepository.getInstance().getCornerRadius(mainPanelEntity.getNode()))
            .isEqualTo(32.0f)
    }

    @Test
    fun transformPixelCoordinatesToLocalPosition_topLeft_returnsCorrectPosition() {
        val position = mainPanelEntity.transformPixelCoordinatesToLocalPosition(Vector2(0f, 0f))
        val expected =
            Vector3(mainPanelEntity.size.width * -0.5f, mainPanelEntity.size.height * 0.5f, 0.0f)
        Truth.assertThat(position).isEqualTo(expected)
    }

    @Test
    fun transformNormalizedCoordinatesToLocalPosition_center_returnsZeroVector() {
        val position =
            mainPanelEntity.transformNormalizedCoordinatesToLocalPosition(Vector2(0f, 0f))
        Truth.assertThat(position).isEqualTo(Vector3.Zero)
    }

    @Test
    fun transformNormalizedCoordinatesToLocalPosition_topLeft_returnsCorrectPosition() {
        val position =
            mainPanelEntity.transformNormalizedCoordinatesToLocalPosition(Vector2(-1f, 1f))
        val expected =
            Vector3(mainPanelEntity.size.width * -0.5f, mainPanelEntity.size.height * 0.5f, 0.0f)
        Truth.assertThat(position).isEqualTo(expected)
    }
}
