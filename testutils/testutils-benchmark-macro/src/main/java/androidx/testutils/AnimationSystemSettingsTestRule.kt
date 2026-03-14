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

package androidx.testutils

import android.app.UiAutomation
import android.os.ParcelFileDescriptor
import androidx.annotation.FloatRange
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * TestRule that sets the animator_duration_scale to a particular value for the duration of a test,
 * then restores it to its original value afterward. Note that when multiple TestRules are used, you
 * must use a RuleChain with RuleChain.outerRule(AnimationSystemSettingsTestRule()) to ensure the
 * animator_duration_scale is set before anything else runs, and is restored after everything else
 * is finished.
 *
 * @param animator_duration_scale Float within the range 0.0 to 1.0 that animator_duration_scale
 *   will be set to for the duration of the test.
 */
class AnimationSystemSettingsTestRule(
    @FloatRange(from = 0.0, to = 1.0) val animator_duration_scale: Float
) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val uiAutomation = instrumentation.uiAutomation

                val initialAnimatorDurationScale = getSetting(uiAutomation)

                if (initialAnimatorDurationScale.toFloat() == animator_duration_scale) {
                    return
                }

                try {
                    setSetting(uiAutomation, animator_duration_scale.toString())
                    base.evaluate()
                } finally {
                    setSetting(uiAutomation, initialAnimatorDurationScale)
                }
            }
        }
    }

    private fun getSetting(uiAutomation: UiAutomation): String {
        return uiAutomation
            .executeShellCommand("settings get global animator_duration_scale")
            .use { pfd ->
                ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { reader ->
                    reader.readText().trim()
                }
            }
    }

    private fun setSetting(uiAutomation: UiAutomation, value: String) {
        // Read the setting to ensure the shell command finished before continuing.
        uiAutomation
            .executeShellCommand("settings put global animator_duration_scale $value")
            .use { pfd ->
                ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { reader ->
                    reader.readText()
                }
            }
    }
}
