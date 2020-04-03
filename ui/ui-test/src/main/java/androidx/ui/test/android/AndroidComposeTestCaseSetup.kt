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

package androidx.ui.test.android

import androidx.activity.ComponentActivity
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.ComposeExecutionControl
import androidx.ui.test.ComposeTestCaseSetup
import androidx.ui.test.runOnUiThread
import androidx.ui.test.setupContent

class AndroidComposeTestCaseSetup(
    private val testCase: ComposeTestCase,
    private val activity: ComponentActivity
) : ComposeTestCaseSetup {
    override fun performTestWithEventsControl(block: ComposeExecutionControl.() -> Unit) {
        runOnUiThread {
            // TODO: Ensure that no composition exists at this stage!
            val runner = AndroidComposeTestCaseRunner({ testCase }, activity)
            var cause: Throwable? = null
            try {
                runner.setupContent()
                block.invoke(runner)
            } catch (t: Throwable) {
                cause = t
                throw t
            } finally {
                // It is currently unsafe to dispose a composition if an exception was thrown
                // during sensitive phases of that composition. Don't lose the original exception
                // from the test if we encounter problems.
                try {
                    runner.disposeContent()
                } catch (t: Throwable) {
                    cause?.addSuppressed(t) ?: throw t
                }
            }
        }
    }
}