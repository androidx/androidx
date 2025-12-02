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

package androidx.compose.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import java.awt.GraphicsEnvironment
import kotlin.test.assertTrue

/**
 * Marker interface for tests that should be run in headless mode.
 */
interface HeadlessTest


/**
 * Runs [runComposeUiTest] but first verifies that the test is executed in headless mode.
 */
@OptIn(ExperimentalTestApi::class)
internal fun runHeadlessComposeUiTest(block: suspend ComposeUiTest.() -> Unit) = runComposeUiTest {
    assertTrue(GraphicsEnvironment.isHeadless(), "This is a headless test, but it's run not in headless mode")
    block()
}
