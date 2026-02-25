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

package androidx.xr.arcore

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.arcore.testing.FakeRuntimeAnchor
import androidx.xr.arcore.testing.FakeRuntimeEye
import androidx.xr.runtime.Config
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class EyeTest {
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        val xrResourcesManager = XrResourcesManager()

        val shadowApplication = shadowOf(activity.application)
        FakeLifecycleManager.TestPermissions.forEach { permission ->
            shadowApplication.grantPermissions(permission)
        }
        FakePerceptionRuntimeFactory.hasCreatePermission = true
        FakeRuntimeAnchor.anchorsCreatedCount = 0
        activityController.create()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(Config(eyeTracking = EyeTrackingMode.COARSE_TRACKING))
        xrResourcesManager.lifecycleManager = session.perceptionRuntime.lifecycleManager
    }

    @Test
    fun update_trackingStateMatchesRuntime() = runBlocking {
        val runtimeEye = FakeRuntimeEye()
        runtimeEye.isOpen = false
        runtimeEye.trackingState = TrackingState.TRACKING
        val underTest = Eye(runtimeEye)
        check(!underTest.state.value.isOpen)

        runtimeEye.isOpen = true
        underTest.update()

        assertThat(underTest.state.value.isOpen).isTrue()
    }

    @Test
    fun update_poseMatchesRuntime() = runBlocking {
        val runtimeEye = FakeRuntimeEye()
        val underTest = Eye(runtimeEye)
        check(
            (underTest.state.value.pose == Pose(Vector3(0f, 0f, 0f), Quaternion(0f, 0f, 0f, 1.0f)))
        )

        val newPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
        runtimeEye.pose = newPose
        underTest.update()

        assertThat(underTest.state.value.pose).isEqualTo(newPose)
    }
}
