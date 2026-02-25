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
package androidx.xr.runtime

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionRuntime
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
@Suppress("deprecation")
class XrDeviceTest {

    private lateinit var session: Session
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        val shadowApplication = shadowOf(activity.application)
        FakeLifecycleManager.TestPermissions.forEach { permission ->
            shadowApplication.grantPermissions(permission)
        }
    }

    @Test
    fun getPreferredDisplayBlendMode_returnsGivenDisplayBlendMode() {
        activityController.create()
        session = createSession()
        session.runtimes
            .filterIsInstance<FakePerceptionRuntime>()
            .single()
            .xrDevicePreferredDisplayBlendMode = DisplayBlendMode.ADDITIVE

        assertThat(XrDevice.getCurrentDevice(session).getPreferredDisplayBlendMode())
            .isEqualTo(DisplayBlendMode.ADDITIVE)
    }

    @OptIn(ExperimentalXrDeviceLifecycleApi::class)
    @Test
    fun lifecycle_returnsLifecycleFromSession() {
        val session = createSession()
        val xrDevice = XrDevice.getCurrentDevice(session)

        assertThat(xrDevice.getLifecycle()).isEqualTo((session.lifecycleOwner.lifecycle))
    }

    private fun createSession(coroutineDispatcher: CoroutineDispatcher = testDispatcher): Session {
        val result = Session.create(activity, coroutineDispatcher)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        return (result as SessionCreateSuccess).session
    }
}
