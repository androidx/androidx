/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test

import androidx.activity.ComponentActivity
import androidx.compose.Composable
import androidx.compose.mutableStateOf
import androidx.compose.state
import androidx.test.espresso.AppNotIdleException
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingPolicy
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.filters.LargeTest
import androidx.ui.core.Modifier
import androidx.ui.core.onPositioned
import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Stack
import androidx.ui.test.android.createAndroidComposeRule
import androidx.ui.test.android.ComposeNotIdleException
import androidx.ui.test.util.expectError
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(JUnit4::class)
class TimeOutTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var idlingResourcePolicy: IdlingPolicy? = null
    private var masterPolicy: IdlingPolicy? = null
    private val expectedErrorDueToRecompositions =
        ".*ComposeIdlingResource is busy due to pending recompositions.*"

    @Before
    fun backupTimeOutPolicies() {
        idlingResourcePolicy = IdlingPolicies.getDynamicIdlingResourceErrorPolicy()
        masterPolicy = IdlingPolicies.getMasterIdlingPolicy()
    }

    @After
    fun restoreTimeOutPolicies() {
        IdlingRegistry.getInstance().unregister(InfiniteResource)
        IdlingPolicies.setIdlingResourceTimeout(
            idlingResourcePolicy!!.idleTimeout, idlingResourcePolicy!!.idleTimeoutUnit)
        IdlingPolicies.setMasterPolicyTimeout(
            masterPolicy!!.idleTimeout, masterPolicy!!.idleTimeoutUnit)
    }

    @Composable
    fun infiniteCase() {
        Stack {
            val infiniteCounter = state { 0 }
            Box(Modifier.onPositioned {
                infiniteCounter.value += 1
            }) {
                Text("Hello")
            }

            Text("Hello ${infiniteCounter.value}")
        }
    }

    @Test(timeout = 5000)
    fun infiniteRecompositions_resourceTimeout() {
        IdlingPolicies.setIdlingResourceTimeout(300, TimeUnit.MILLISECONDS)

        expectError<ComposeNotIdleException>(expectedMessage = expectedErrorDueToRecompositions) {
            composeTestRule.setContent {
                infiniteCase()
            }
        }
    }

    @Test(timeout = 5000)
    fun infiniteRecompositions_masterTimeout() {
        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.MILLISECONDS)

        expectError<ComposeNotIdleException>(expectedMessage = expectedErrorDueToRecompositions) {
            composeTestRule.setContent {
                infiniteCase()
            }
        }
    }

    @Test(timeout = 5000)
    fun delayInfiniteTrigger() {
        // This test checks that we properly time out on infinite recompositions that happen later
        // down the road (not right during setContent).
        val count = mutableStateOf(0)
        composeTestRule.setContent {
            Text("Hello ${count.value}")
            if (count.value > 0) {
                count.value++
            }
        }

        onNodeWithText("Hello 0").assertExists()

        count.value++ // Start infinite re-compositions

        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.MILLISECONDS)
        expectError<ComposeNotIdleException>(expectedMessage = expectedErrorDueToRecompositions) {
            onNodeWithText("Hello").assertExists()
        }
    }

    @Test(timeout = 10_000)
    fun emptyComposition_masterTimeout_fromIndependentIdlingResource() {
        // This test checks that if we fail to sync on some unrelated idling resource we don't
        // override the vanilla errors from Espresso.

        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.MILLISECONDS)
        IdlingRegistry.getInstance().register(InfiniteResource)

        expectError<AppNotIdleException> {
            composeTestRule.setContent { }
        }
    }

    @Test(timeout = 5000)
    fun timeout_testIsolation_check() {
        // This test is here to guarantee that even if we crash on infinite recompositions after
        // we set a content. We still recover and the old composition is no longer running in the
        // background causing further delays. This verifies that our tests run in isolation.
        val androidTestRule = composeTestRule

        // Start infinite case and die on infinite recompositions
        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.MILLISECONDS)
        expectError<ComposeNotIdleException> {
            composeTestRule.setContent {
                infiniteCase()
            }
        }

        // Act like we are tearing down the test
        runOnUiThread {
            androidTestRule.disposeContentHook!!.invoke()
            androidTestRule.disposeContentHook = null
        }

        // Kick of simple composition again (like if we run new test)
        composeTestRule.setContent {
            Text("Hello")
        }

        // No timeout should happen this time
        onNodeWithText("Hello").assertExists()
    }

    private object InfiniteResource : IdlingResource {
        override fun getName(): String {
            return "InfiniteResource"
        }

        override fun isIdleNow(): Boolean {
            return false
        }

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {}
    }
}