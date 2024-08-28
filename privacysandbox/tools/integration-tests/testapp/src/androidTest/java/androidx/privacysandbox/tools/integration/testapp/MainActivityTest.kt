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

package androidx.privacysandbox.tools.integration.testapp

import androidx.privacysandbox.tools.integration.testsdk.MySdk
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.resume
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @get:Rule val scenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Before fun setUp() = runBlocking { getActivity().loadSdk() }

    @After fun tearDown() = runTest { scenarioRule.scenario.close() }

    @Test
    fun loadSdk_works() = runTest {
        val sdk = getActivity().sdk

        assertThat(sdk).isNotNull()
    }

    @Test
    fun doSum_works() = runTest {
        val sum = getSdk().doSum(5, 6)

        assertThat(sum).isEqualTo(11)
    }

    @Test
    fun remoteRendering_works(): Unit = runTest {
        onView(withId(R.id.sandboxedSdkView)).check(matches(hasChildCount(0)))

        getActivity().renderAd()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        onView(withId(R.id.sandboxedSdkView)).check(matches(hasChildCount(1)))
    }

    private suspend fun getActivity(): MainActivity = suspendCancellableCoroutine {
        scenarioRule.scenario.onActivity { activity -> it.resume(activity) }
    }

    private suspend fun getSdk(): MySdk = getActivity().sdk!!
}
