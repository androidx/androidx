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

package androidx.xr.arcore

import androidx.activity.ComponentActivity
import androidx.kruth.assertThrows
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(UnstableNativeResourceApi::class)
class NativeDataExtTest {

    private lateinit var session: Session
    private val testDispatcher = StandardTestDispatcher()

    // TODO(b/467096822) : Have these tests use the FakePerceptionRuntime once it is implemented.
    @Test
    fun getNativeData_unsupportedPerceptionRuntime_throwsIllegalStateException() =
        createTestSessionAndRunTest {
            assertThrows<IllegalStateException> { session.getNativeData() }
        }

    private fun createTestSessionAndRunTest(testBody: () -> Unit) {
        ActivityScenario.launch(ComponentActivity::class.java).use {
            it.onActivity { activity ->
                session =
                    (Session.create(activity, StandardTestDispatcher()) as SessionCreateSuccess)
                        .session

                testBody()
            }
        }
    }
}
