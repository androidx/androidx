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

import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class AnimationSystemSettingsTestRuleTest {
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    @Test
    fun testRestoresValueAfterExecution() {
        val initialValue = getCurrentSetting()

        val targetValue = if (initialValue == "1.0" || initialValue == "1") 0.5f else 1.0f
        val expectedStringValue = targetValue.toString()

        val rule = AnimationSystemSettingsTestRule(targetValue)

        val testStatement =
            object : Statement() {
                override fun evaluate() {
                    assertEquals(
                        "animator_duration_scale should be equal to the target value while the test is running",
                        expectedStringValue,
                        getCurrentSetting(),
                    )
                }
            }

        rule.apply(testStatement, Description.EMPTY).evaluate()

        assertEquals(
            "animator_duration_scale should be restored to its original value after the test is finished",
            initialValue,
            getCurrentSetting(),
        )
    }

    private fun getCurrentSetting(): String {
        return uiAutomation
            .executeShellCommand("settings get global animator_duration_scale")
            .let { pfd ->
                ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use {
                    it.readText().trim()
                }
            }
    }
}
