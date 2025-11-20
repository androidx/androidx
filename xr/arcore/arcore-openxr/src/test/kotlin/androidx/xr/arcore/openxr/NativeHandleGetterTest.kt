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

package androidx.xr.arcore.openxr

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class NativeHandleGetterTest {
    // TODO: b/439081594 Add unit tests for cases where exceptions are not expected.
    private lateinit var underTest: Session
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

        FakePerceptionRuntimeFactory.hasCreatePermission = true
    }

    @After
    fun tearDown() {
        if (activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            activityController.destroy()
        }
    }

    @Test
    fun getXrSessionPointer_nonOpenXrSession_throwsIllegalArgumentException() {
        activityController.create()
        val session = createSession()

        var exception: IllegalArgumentException? = null
        try {
            getXrSessionPointer(session.runtimes.filterIsInstance<PerceptionRuntime>().single())
        } catch (e: IllegalArgumentException) {
            exception = e
        }

        assertThat(exception).isNotNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun getXrInstancePointer_nonOpenXrSession_throwsIllegalArgumentException() {
        activityController.create()
        val session = createSession()

        var exception: IllegalArgumentException? = null
        try {
            getXrInstancePointer(session.runtimes.filterIsInstance<PerceptionRuntime>().single())
        } catch (e: IllegalArgumentException) {
            exception = e
        }

        assertThat(exception).isNotNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun createSession(coroutineDispatcher: CoroutineDispatcher = testDispatcher): Session {
        val result = Session.create(activity, coroutineDispatcher)
        check(result is SessionCreateSuccess)
        return result.session
    }
}
