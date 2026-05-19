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

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.ActivityPanelEntity
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

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class ActivityPanelEntityTesterTest {
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
    fun equalsAndHashCode_behaveCorrectly() {
        val activityPanelEntity =
            ActivityPanelEntity.create(session, IntSize2d(100, 100), "test_panel")
        val tester1 = ActivityPanelEntityTester.create(activityPanelEntity)
        val tester2 = ActivityPanelEntityTester.create(activityPanelEntity)

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }

    @Test
    fun startActivity_updatesStartActivityIntent() {
        val activityPanelEntity =
            ActivityPanelEntity.create(session, IntSize2d(100, 100), "test_panel")
        val tester = testRule.createTester<ActivityPanelEntityTester>(activityPanelEntity)

        assertThat(tester.startActivityIntent).isNull()

        val intent = Intent(activity, ComponentActivity::class.java)
        activityPanelEntity.startActivity(intent)

        assertThat(tester.startActivityIntent).isEqualTo(intent)
    }

    @Test
    fun transferActivity_updatesTransferredActivity() {
        val activityPanelEntity =
            ActivityPanelEntity.create(session, IntSize2d(100, 100), "test_panel")
        val tester = testRule.createTester<ActivityPanelEntityTester>(activityPanelEntity)

        assertThat(tester.transferredActivity).isNull()

        val anotherActivity = Robolectric.buildActivity(Activity::class.java).create().get()
        activityPanelEntity.transferActivity(anotherActivity)

        assertThat(tester.transferredActivity).isEqualTo(anotherActivity)
    }
}
