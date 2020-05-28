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

package androidx.ui.test

import android.util.DisplayMetrics
import androidx.activity.ComponentActivity
import androidx.compose.Composable
import androidx.compose.Recomposer
import androidx.ui.unit.Density
import androidx.ui.test.android.AndroidComposeTestRule
import org.junit.rules.TestRule

/**
 * Enables to run tests of individual composables without having to do manual setup. For Android
 * tests see [AndroidComposeTestRule]. Normally this rule is obtained by using [createComposeRule]
 * factory that provides proper implementation (depending if running host side or Android side).
 *
 * However if you really need Android specific dependencies and don't want your test to be abstract
 * you can still create [AndroidComposeTestRule] directly and access its underlying ActivityTestRule
 */
interface ComposeTestRule : TestRule {
    /**
     * Current device screen's density.
     */
    val density: Density

    /**
     * A test rule that allows you to control the animation clock
     */
    val clockTestRule: AnimationClockTestRule

    /**
     * Sets the given composable as a content of the current screen.
     *
     * Use this in your tests to setup the UI content to be tested. This should be called exactly
     * once per test.
     *
     * @throws IllegalStateException if called more than once per test.
     */
    fun setContent(composable: @Composable () -> Unit)

    /**
     * Takes the given content and prepares it for execution-controlled test via
     * [ComposeTestCaseSetup].
     */
    fun forGivenContent(composable: @Composable () -> Unit): ComposeTestCaseSetup

    /**
     * Takes the given test case and prepares it for execution-controlled test via
     * [ComposeTestCaseSetup].
     */
    fun forGivenTestCase(testCase: ComposeTestCase): ComposeTestCaseSetup

    // TODO(pavlis): Provide better abstraction for host side reusability
    val displayMetrics: DisplayMetrics get
}

/**
 * Helper interface to run execution-controlled test via [ComposeTestRule].
 */
interface ComposeTestCaseSetup {
    /**
     * Takes the content provided via [ComposeTestRule#setContent] and runs the given test
     * instruction. The test is executed on the main thread and prevents interference from Activity
     * so the frames can be controlled manually. See [ComposeExecutionControl] for available
     * methods.
     */
    fun performTestWithEventsControl(block: ComposeExecutionControl.() -> Unit)
}

/**
 * Factory method to provide implementation of [ComposeTestRule].
 *
 * This method is useful for tests in compose libraries where no custom Activity is usually
 * needed. For app tests or launching custom activities, see [AndroidComposeTestRule].
 *
 * For Android this will use the default Activity (android.app.Activity). You need to add a
 * reference to this activity into the manifest file of the corresponding tests (usually in
 * androidTest/AndroidManifest.xml).
 */
fun createComposeRule(
    recomposer: Recomposer? = null,
    disableTransitions: Boolean = false
): ComposeTestRule = AndroidComposeTestRule<ComponentActivity>(recomposer, disableTransitions)
