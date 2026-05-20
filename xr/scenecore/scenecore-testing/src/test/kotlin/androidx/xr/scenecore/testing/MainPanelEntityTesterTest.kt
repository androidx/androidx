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
import androidx.xr.arcore.RenderViewpoint
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.PerceivedResolutionResult
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
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class MainPanelEntityTesterTest {
    @get:Rule val testRule = SceneCoreTestRule()
    private lateinit var session: Session

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun triggerOnPerceivedResolutionChanged_triggersListener() {
        val mainPanelEntity = session.scene.mainPanelEntity
        val tester = testRule.mainPanelEntityTester
        val renderViewpoint = RenderViewpoint.left(session)

        var capturedDimensions: IntSize2d? = null
        mainPanelEntity.addPerceivedResolutionChangedListener { capturedDimensions = it }

        val newDimensions = IntSize2d(1920, 1080)
        tester.triggerOnPerceivedResolutionChanged(newDimensions)
        ShadowLooper.idleMainLooper()

        assertThat(capturedDimensions).isEqualTo(newDimensions)
        assertThat(mainPanelEntity.getPerceivedResolution(renderViewpoint))
            .isEqualTo(PerceivedResolutionResult.Success(newDimensions))
    }

    @Test
    fun perceivedResolutionResult_inheritedFromBase_worksCorrectly() {
        val mainPanelEntity = session.scene.mainPanelEntity
        val tester = testRule.mainPanelEntityTester
        val renderViewpoint = RenderViewpoint.left(session)

        val expectedResult = PerceivedResolutionResult.Success(IntSize2d(2560, 1440))

        // This confirms inheritance from PanelEntityTester
        tester.perceivedResolutionResult = expectedResult

        assertThat(tester.perceivedResolutionResult).isEqualTo(expectedResult)
        assertThat(mainPanelEntity.getPerceivedResolution(renderViewpoint))
            .isEqualTo(expectedResult)
    }

    @Test
    fun mainPanelEntityTester_isConsistentInstance() {
        val tester1 = testRule.mainPanelEntityTester
        val tester2 = testRule.mainPanelEntityTester

        assertThat(tester1).isSameInstanceAs(tester2)
        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }
}
