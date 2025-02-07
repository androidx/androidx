/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.java

import android.app.Activity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class CoroutinesTest {
    private lateinit var activity: Activity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @get:Rule val activityScenarioRule = ActivityScenarioRule<Activity>(Activity::class.java)

    @Before
    fun setUp() {
        activityScenarioRule.scenario.onActivity { this.activity = it }
        shadowOf(activity)
            .grantPermissions(
                "android.permission.SCENE_UNDERSTANDING",
                "android.permission.HAND_TRACKING",
            )

        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun toFuture_cancelsFutureWhenSessionIsDestroyed() =
        runTest(testDispatcher) {
            val session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
            var isCoroutineComplete = false

            val future =
                toFuture(session) {
                    delay(1.hours)
                    isCoroutineComplete = true
                }
            session.destroy()
            testScope.advanceUntilIdle()

            assertThat(future.isCancelled).isTrue()
            assertThat(future.isDone).isTrue()
            // Verify that the coroutine was cancelled and did not actually complete.
            assertThat(isCoroutineComplete).isFalse()
        }
}
