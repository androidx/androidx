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

package androidx.appcompat.lint.app

import androidx.appcompat.app.SetActionBarDetector
import androidx.appcompat.lint.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

@Suppress("UnstableApiUsage")
class SetActionBarDetectorTest {
    @Test
    fun testExtendCoreActivity() {
        val customActivityClass = LintDetectorTest.kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.app.Activity
            import android.os.Bundle
            import android.widget.Toolbar

            class CustomActivity: Activity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                   setActionBar(Toolbar(this))
                }
            }
            """
        ).indented().within("src")

        // We expect the class extending the core Activity widget to not be flagged
        // in setActionBar call
        TestLintTask.lint().files(
            customActivityClass
        ).issues(SetActionBarDetector.USING_CORE_ACTION_BAR)
            .run()
            .expectClean()
    }

    @Test
    fun testExtendAppCompatActivity() {
        val customActivityClass = LintDetectorTest.kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.os.Bundle
            import android.widget.Toolbar
            import androidx.appcompat.app.AppCompatActivity

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                   setActionBar(Toolbar(this))
                }
            }
            """
        ).indented().within("src")

        // We expect the class extending the AppCompatActivity widget to be flagged
        // in setActionBar call
        /* ktlint-disable max-line-length */
        TestLintTask.lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            customActivityClass
        ).issues(SetActionBarDetector.USING_CORE_ACTION_BAR)
            .run()
            .expect("""
src/com/example/CustomActivity.kt:9: Warning: Use AppCompatActivity.setSupportActionBar [UseSupportActionBar]
       setActionBar(Toolbar(this))
       ~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """.trimIndent())
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testDeepExtendAppCompatActivity() {
        val customActivityClass = LintDetectorTest.kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.os.Bundle
            import android.widget.Toolbar
            import androidx.appcompat.app.AppCompatActivity

            class CustomActivity: AppCompatActivity()
 
            class CustomActivityExt: CustomActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                   setActionBar(Toolbar(this))
                }
            }
            """
        ).indented().within("src")

        // We expect the class extending the AppCompatActivity widget to be flagged
        // in setActionBar call
        /* ktlint-disable max-line-length */
        TestLintTask.lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            customActivityClass
        ).issues(SetActionBarDetector.USING_CORE_ACTION_BAR)
            .run()
            .expect("""
src/com/example/CustomActivity.kt:11: Warning: Use AppCompatActivity.setSupportActionBar [UseSupportActionBar]
       setActionBar(Toolbar(this))
       ~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """.trimIndent())
        /* ktlint-enable max-line-length */
    }
}
