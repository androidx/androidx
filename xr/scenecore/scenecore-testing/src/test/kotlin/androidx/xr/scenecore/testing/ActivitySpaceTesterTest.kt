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

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivitySpace
import androidx.xr.scenecore.HitTestResult
import androidx.xr.scenecore.ScenePose.HitTestFilter
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.toHitTestSurfaceType
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import java.util.function.Consumer
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
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
class ActivitySpaceTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session
    private lateinit var activitySpace: ActivitySpace
    private lateinit var tester: ActivitySpaceTester

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
        activitySpace = session.scene.activitySpace
        tester = testRule.activitySpaceTester
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun setHitTestResult_hitTest_returnsCorrectly() {
        val origin = Vector3(1f, 2f, 3f)
        val direction = Vector3(4f, 5f, 6f)
        val hitTestFilter = HitTestFilter.SELF_SCENE
        val hitPosition = Vector3(7f, 8f, 9f)
        val surfaceNormal = Vector3(10f, 11f, 12f)
        val distance = 7f
        val surfaceType = HitTestResult.SurfaceType.PLANE
        val expectedHitTestResult =
            HitTestResult(hitPosition, surfaceNormal, surfaceType.toHitTestSurfaceType(), distance)

        // Set the hit test results.
        tester.hitTestResult = expectedHitTestResult

        runTest(testDispatcher) {
            assertThat(activitySpace.hitTest(origin, direction, hitTestFilter))
                .isEqualTo(expectedHitTestResult)
        }
    }

    @Test
    fun setHitTestResultToNull_hitTest_returnsNull() {
        val origin = Vector3(1f, 2f, 3f)
        val direction = Vector3(4f, 5f, 6f)
        val hitTestFilter = HitTestFilter.SELF_SCENE

        // Set the hit test results.
        tester.hitTestResult = null

        runTest(testDispatcher) {
            assertThat(activitySpace.hitTest(origin, direction, hitTestFilter)).isNull()
        }
    }

    @Test
    fun triggerOnBoundsChanged_addBoundsChangedListener_receivesBoundsChangedCallback() {
        val boundsChangedListener =
            Consumer<FloatSize3d> { newBounds ->
                assertThat(newBounds.width).isEqualTo(0.3f)
                assertThat(newBounds.height).isEqualTo(0.2f)
                assertThat(newBounds.depth).isEqualTo(0.1f)
            }

        activitySpace.addBoundsChangedListener(directExecutor(), boundsChangedListener)

        // Simulates a runtime callback.
        tester.triggerOnBoundsChanged(FloatSize3d(0.3f, 0.2f, 0.1f))
    }

    @Test
    fun setRecommendedContentBoxInFullSpace_returnsCorrectBoundingBox() {
        val expectedResult: BoundingBox =
            BoundingBox.fromMinMax(
                min = Vector3(-1.73f / 2, -1.61f / 2, -0.5f / 2),
                max = Vector3(1.73f / 2, 1.61f / 2, 0.5f / 2),
            )

        tester.recommendedContentBoxInFullSpace = expectedResult
        val recommendedContentBoxInFullSpace = activitySpace.recommendedContentBoxInFullSpace

        assertThat(recommendedContentBoxInFullSpace.min).isEqualTo(expectedResult.min)
        assertThat(recommendedContentBoxInFullSpace.max).isEqualTo(expectedResult.max)
    }

    @Test
    fun addOriginChangedListener_receivesRuntimeSetOnOriginChangedListenerCallbacks() {
        var listenerCalled = false
        activitySpace.addOriginChangedListener(directExecutor()) { listenerCalled = true }

        // Simulates a runtime callback.
        tester.triggerOnOriginChanged()

        assertThat(listenerCalled).isTrue()
    }
}
