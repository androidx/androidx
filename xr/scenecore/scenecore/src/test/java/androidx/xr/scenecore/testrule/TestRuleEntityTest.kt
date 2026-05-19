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
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.testing.ActivityPanelEntityTester
import androidx.xr.scenecore.testing.MemoryUtils
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

    @Rule @JvmField val scenecoreTestRule = SceneCoreTestRule()
    private lateinit var session: Session

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var activityPanelEntity: ActivityPanelEntity

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        activityPanelEntity = ActivityPanelEntity.create(session, IntSize2d(640, 480), "test")
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
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
