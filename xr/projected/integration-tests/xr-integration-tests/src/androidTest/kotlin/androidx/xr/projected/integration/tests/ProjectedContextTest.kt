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

package androidx.xr.projected.integration.tests

import android.content.Context
import android.content.Intent
import android.view.Display
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/** End-to-end integration test verifying projected activity launch on a paired Projected device. */
@RunWith(AndroidJUnit4::class)
@LargeTest
@OptIn(ExperimentalProjectedApi::class)
class ProjectedContextTest {

    @get:Rule val timeout: Timeout = Timeout.seconds(60)

    @Test
    fun launchProjectedActivity_resumesOnProjectedDisplay() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        requireProjectedDeviceConnected(targetContext)

        val projectedOptions = ProjectedContext.createProjectedActivityOptions(targetContext)
        val expectedLaunchDisplayId = projectedOptions.launchDisplayId
        assertThat(expectedLaunchDisplayId).isNotEqualTo(Display.DEFAULT_DISPLAY)
        assertThat(expectedLaunchDisplayId).isNotEqualTo(Display.INVALID_DISPLAY)

        val intent = Intent(targetContext, ProjectedDisplayTestActivity::class.java)
        ActivityScenario.launch<ProjectedDisplayTestActivity>(intent, projectedOptions.toBundle())
            .use { scenario ->
                var isFinishing = true
                var isProjectedContext = false
                var displayId: Int? = null
                scenario.onActivity { activity ->
                    isFinishing = activity.isFinishing
                    isProjectedContext = ProjectedContext.isProjectedDeviceContext(activity)
                    displayId = activity.display?.displayId
                }
                assertThat(isFinishing).isFalse()
                assertThat(isProjectedContext).isTrue()
                assertThat(displayId).isEqualTo(expectedLaunchDisplayId)
            }
    }

    private fun requireProjectedDeviceConnected(context: Context) {
        val requireProjectedDevice =
            InstrumentationRegistry.getArguments()
                .getString(ARG_REQUIRE_PROJECTED_DEVICE)
                .equals("true", ignoreCase = true)

        val isConnected = runBlocking {
            if (requireProjectedDevice) {
                withTimeoutOrNull(CONNECTION_TIMEOUT_SECONDS.seconds) {
                    ProjectedContext.isProjectedDeviceConnected(context, Dispatchers.Default)
                        .first { it }
                } == true
            } else {
                ProjectedContext.isProjectedDeviceConnected(context, Dispatchers.Default).first()
            }
        }

        if (requireProjectedDevice) {
            assertWithMessage(
                    "Projected device did not connect within ${CONNECTION_TIMEOUT_SECONDS}s " +
                        "($ARG_REQUIRE_PROJECTED_DEVICE=true)"
                )
                .that(isConnected)
                .isTrue()
        } else {
            assumeTrue(
                "No paired Projected device connected; pass " +
                    "-e $ARG_REQUIRE_PROJECTED_DEVICE true to fail instead of skipping.",
                isConnected,
            )
        }
    }

    private companion object {
        const val ARG_REQUIRE_PROJECTED_DEVICE = "requireProjectedDevice"
        const val CONNECTION_TIMEOUT_SECONDS = 15L
    }
}
