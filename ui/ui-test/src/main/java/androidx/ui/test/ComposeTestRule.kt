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
import androidx.compose.Composable
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Density
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
    val density: Density get

    /**
     * Sets the given composable as a content of the current screen.
     */
    fun setContent(composable: @Composable() () -> Unit)

    /**
     * Runs action on UI thread with a guarantee that any operations modifying Compose data model
     * are safe to do in this block.
     */
    fun runOnUiThread(action: () -> Unit)

    // TODO(pavlis): Provide better abstraction for host side reusability
    val displayMetrics: DisplayMetrics get
}

/**
 * Factory method to provide implementation of [ComposeTestRule].
 */
fun createComposeRule(disableTransitions: Boolean = false): ComposeTestRule {
    return createComposeRule(disableTransitions, throwOnRecomposeTimeout = false)
}

/**
 * Internal factory method to provide implementation of [ComposeTestRule].
 */
internal fun createComposeRule(
    disableTransitions: Boolean = false,
    throwOnRecomposeTimeout: Boolean = false
): ComposeTestRule {
    // TODO(pavlis): Plug-in host side rule here in the future.
    return AndroidComposeTestRule(disableTransitions, throwOnRecomposeTimeout)
}