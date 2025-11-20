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
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO - b/382119583: Remove the @SdkSuppress annotation once "androidx.xr.runtime.openxr.test"
// supports a
// lower SDK version.
@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenXrTimeSourceTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    private lateinit var underTest: OpenXrTimeSource
    private lateinit var openXrManager: OpenXrManager

    @Before
    fun setUp() {
        underTest = OpenXrTimeSource()
    }

    @Test
    // TODO - b/346615429: Control the values returned by the OpenXR stub instead of relying on the
    // stub's current implementation.
    fun read_usesTheOpenXrClock() = initOpenXrManagerAndRunTest {
        // The OpenXR stub returns a different value for each call to [OpenXrTimeSource::read] in
        // increments of 1000ns when `xrConvertTimespecTimeToTimeKHR` is executed. The first call
        // returns 1000ns and is the value associated with [timeMark]. The second call returns
        // 2000ns
        // and is the value associated with [AbstractLongTimeSource::zero], which is calculated
        // automatically with the first call to [OpenXrTimeSource::markNow].
        // Note that this is just an idiosyncrasy of the test stub and not how OpenXR works in
        // practice,
        // where the second call would return an almost identical value to the first call's value.
        val timeMark = underTest.markNow()

        // The third call happens with the call to [elapsedNow] and returns 3000ns. Thus, the
        // elapsed
        // time is 3000ns (i.e. "now") -  1000ns (i.e. "the start time") = 2000ns.
        assertThat(timeMark.elapsedNow().inWholeNanoseconds).isEqualTo(2000L)
    }

    @Test
    // TODO - b/346615429: Control the values returned by the OpenXR stub instead of relying on the
    // stub's current implementation.
    fun getXrTime_returnsTheOpenXrTime() = initOpenXrManagerAndRunTest {
        // The OpenXR stub returns a different value for each call to [OpenXrTimeSource::read] in
        // increments of 1000ns when `xrConvertTimespecTimeToTimeKHR` is executed. The first call
        // returns 1000ns and is the value associated with [firstTimeMark]. The second call returns
        // 2000ns and is the value associated with [AbstractLongTimeSource::zero], which is
        // calculated
        // automatically with the first call to [OpenXrTimeSource::markNow].
        // Note that this is just an idiosyncrasy of the test stub and not how OpenXR works in
        // practice,
        // where the second call would return an almost identical value to the first call's value.
        val firstTimeMark = underTest.getXrTime(underTest.markNow())
        // The third call returns 3000ns and is the value associated with [secondTimeMark].
        val secondTimeMark = underTest.getXrTime(underTest.markNow())
        // The fourth call returns 4000ns and is the value associated with [thirdTimeMark].
        val thirdTimeMark = underTest.getXrTime(underTest.markNow())

        assertThat(secondTimeMark).isEqualTo(firstTimeMark + 2000L)
        assertThat(thirdTimeMark).isEqualTo(firstTimeMark + 3000L)
    }

    private fun initOpenXrManagerAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            openXrManager = OpenXrManager(it, OpenXrPerceptionManager(underTest), underTest)
            openXrManager.create()
            openXrManager.resume()

            testBody()

            // Pause and stop the OpenXR manager here in lieu of an @After method to ensure that the
            // calls to the OpenXR manager are coming from the same thread.
            openXrManager.pause()
            openXrManager.stop()
        }
    }
}
