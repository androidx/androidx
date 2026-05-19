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

package androidx.xr.scenecore.testing

import android.graphics.ImageFormat
import android.media.ImageReader
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.RenderViewpoint
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.ExperimentalSurfaceEntityPixelDimensionsApi
import androidx.xr.scenecore.PerceivedResolutionResult
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
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
class SurfaceEntityTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()
    private val imageReader = ImageReader.newInstance(2, 3, ImageFormat.YUV_420_888, 4)

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session
    private lateinit var surfaceEntity: SurfaceEntity
    private lateinit var tester: SurfaceEntityTester

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(
                context = activity,
                coroutineContext = testDispatcher,
                lifecycleOwner = activity as LifecycleOwner,
            )

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        surfaceEntity =
            SurfaceEntity.create(
                session,
                Pose.Identity,
                SurfaceEntity.Shape.Quad(FloatSize2d(2.0f, 3.0f)),
                SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                parent = session.scene.activitySpace,
            )
        tester = testRule.createTester<SurfaceEntityTester>(surfaceEntity)
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    @ExperimentalSurfaceEntityPixelDimensionsApi
    fun validateInitialValue() {
        assertThat(surfaceEntity.getPerceivedResolution(RenderViewpoint.left(session)))
            .isInstanceOf(PerceivedResolutionResult.InvalidRenderViewpoint::class.java)
    }

    @Test
    fun equalsAndHashCode_behaveCorrectly() {
        val tester1 = SurfaceEntityTester.create(surfaceEntity)
        val tester2 = SurfaceEntityTester.create(surfaceEntity)

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }

    @Test
    fun setSurface_getSurface_setsCorrectly() {
        val expectedSurface = imageReader.surface

        assertThat(surfaceEntity.getSurface()).isNotSameInstanceAs(expectedSurface)

        tester.surface = expectedSurface

        assertThat(surfaceEntity.getSurface()).isSameInstanceAs(expectedSurface)
    }

    @Test
    fun setupPerceivedResolutionResultWithSuccess_getPerceivedResolution_setsCorrectly() {
        val expectedPerceivedResolution = IntSize2d(3, 5)
        val perceivedResolutionResult =
            PerceivedResolutionResult.Success(expectedPerceivedResolution)
        tester.perceivedResolutionResult = perceivedResolutionResult

        assertThat(surfaceEntity.getPerceivedResolution(RenderViewpoint.left(session)))
            .isInstanceOf(PerceivedResolutionResult.Success::class.java)

        val successResult =
            surfaceEntity.getPerceivedResolution(RenderViewpoint.left(session))
                as PerceivedResolutionResult.Success

        assertThat(successResult.perceivedResolution).isEqualTo(expectedPerceivedResolution)
    }

    @Test
    fun setupPerceivedResolutionResultWithEntityTooClose_getPerceivedResolution_setsCorrectly() {
        val perceivedResolutionResult = PerceivedResolutionResult.EntityTooClose()
        tester.perceivedResolutionResult = perceivedResolutionResult

        assertThat(surfaceEntity.getPerceivedResolution(RenderViewpoint.left(session)))
            .isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)
    }
}
