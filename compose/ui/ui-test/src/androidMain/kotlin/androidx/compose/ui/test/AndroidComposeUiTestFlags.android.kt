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

package androidx.compose.ui.test

import kotlin.jvm.JvmField

/**
 * This is a collection of flags which are used to guard against regressions in some of the
 * "riskier" refactors or new feature support that is added to this module. These flags are always
 * "on" in the published artifact of this module, however these flags allow end consumers of this
 * module to toggle them "off" in case this new path is causing a regression.
 *
 * These flags are considered temporary, and there should be no expectation for these flags be
 * around for an extended period of time. If you have a regression that one of these flags fixes, it
 * is strongly encouraged for you to file a bug ASAP.
 *
 * **Usage:**
 *
 * In order to turn a feature off in a debug environment, it is recommended to set this to false in
 * as close to the initial loading of the application as possible. Changing this value after compose
 * library code has already been loaded can result in undefined behavior.
 *
 *      class MyApplication : Application() {
 *          override fun onCreate() {
 *              AndroidComposeUiTestFlags.isInputModeSetForDeviceTests = false
 *              super.onCreate()
 *          }
 *      }
 *
 * In order to turn this off in a release environment, it is recommended to additionally utilize R8
 * rules which force a single value for the entire build artifact. This can result in the new code
 * paths being completely removed from the artifact, which can often have nontrivial positive
 * performance impact.
 *
 *      -assumevalues class androidx.compose.ui.test.AndroidComposeUiTestFlags {
 *          public static boolean isInputModeSetForDeviceTests return false
 *      }
 */
@ExperimentalTestApi
public object AndroidComposeUiTestFlags {
    /**
     * Enables or disables setting the default initial
     * [InputMode][androidx.compose.ui.input.InputMode] in parameterless test setup functions
     * `create*ComposeRule()` and `run*ComposeUiTest()`.
     *
     * When set to `true`, these functions will use the default values provided by new instances of
     * [ComposeUiTestConfig], which sets the initial input mode to
     * [Touch][androidx.compose.ui.input.InputMode.Companion.Touch] at the start of each test.
     *
     * When set to `false`, these functions disable setting the default initial input mode and
     * retain legacy behavior.
     *
     * If you find test failures after updating due to changes in default test behavior regarding
     * initial input mode, you can explicitly set this flag to `false`.
     */
    // TODO(b/508814902): Remove this flag once developers have had sufficient time to migrate their
    // tests to the new ComposeUiTestConfig defaults.
    @JvmField
    @field:Suppress("MutableBareField")
    public var isInputModeSetForDeviceTests: Boolean = true
}
