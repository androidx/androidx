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

package androidx.appcompat.lint.widget

import androidx.appcompat.lint.Stubs
import androidx.appcompat.widget.SwitchUsageCodeDetector
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

@Suppress("UnstableApiUsage")
class SwitchUsageCodeDetectorTest {
    @Test
    fun testExtendCoreSwitch() {
        val customSwitchClass = LintDetectorTest.kotlin(
            "com/example/CustomSwitch.kt",
            """
            package com.example

            import android.content.Context
            import android.widget.Switch

            class CustomSwitch(context: Context): Switch(context)
            """
        ).indented().within("src")

        // We expect the class extending the core Switch widget to be flagged
        /* ktlint-disable max-line-length */
        TestLintTask.lint().files(
            customSwitchClass
        ).issues(SwitchUsageCodeDetector.USING_CORE_SWITCH_CODE)
            .run()
            .expect(
                """
src/com/example/CustomSwitch.kt:6: Warning: Use SwitchCompat from AppCompat or MaterialSwitch from Material library [UseSwitchCompatOrMaterialCode]
class CustomSwitch(context: Context): Switch(context)
                                      ~~~~~~
0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testCreateCoreSwitch() {
        val customSwitchClass = LintDetectorTest.kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example
 
            import android.os.Bundle
            import android.widget.Switch
            import androidx.appcompat.app.AppCompatActivity

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    val mySwitch = Switch(this)
                    mySwitch.setChecked(true)
                }
            }
            """
        ).indented().within("src")

        // We expect the class instantiating the core Switch widget to be flagged
        /* ktlint-disable max-line-length */
        TestLintTask.lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            customSwitchClass
        ).issues(SwitchUsageCodeDetector.USING_CORE_SWITCH_CODE)
            .run()
            .expect(
                """
src/com/example/CustomActivity.kt:9: Warning: Use SwitchCompat from AppCompat or MaterialSwitch from Material library [UseSwitchCompatOrMaterialCode]
        val mySwitch = Switch(this)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }
}
