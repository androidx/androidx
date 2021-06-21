/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark.junit4

import android.app.Activity
import androidx.benchmark.IsolationActivity
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

fun BenchmarkRule.validateRunWithIsolationActivityHidden() {
    // isolation activity *not* on top
    assertFalse(IsolationActivity.resumed)

    measureRepeated {}
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class ActivityScenarioTest {
    @get:Rule
    val benchmarkRule = BenchmarkRule(enableReport = false)

    private lateinit var activityScenario: ActivityScenario<Activity>

    @Before
    fun setup() {
        activityScenario = ActivityScenario.launch(Activity::class.java)
    }

    @Test
    fun verifyActivityLaunched() {
        activityScenario.onActivity {
            benchmarkRule.validateRunWithIsolationActivityHidden()
        }
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class ActivityScenarioRuleTest {
    @get:Rule
    val benchmarkRule = BenchmarkRule(enableReport = false)

    @get:Rule
    val activityRule = ActivityScenarioRule(Activity::class.java)

    @FlakyTest(bugId = 187106319)
    @UiThreadTest
    @Test
    fun verifyActivityLaunched() {
        benchmarkRule.validateRunWithIsolationActivityHidden()
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class ActivityTestRuleTest {
    @get:Rule
    val benchmarkRule = BenchmarkRule(enableReport = false)

    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(Activity::class.java)

    @UiThreadTest
    @Test
    fun verifyActivityLaunched() {
        benchmarkRule.validateRunWithIsolationActivityHidden()
    }
}
