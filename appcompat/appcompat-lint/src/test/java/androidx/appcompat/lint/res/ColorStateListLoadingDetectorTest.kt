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

package androidx.appcompat.lint.res

import androidx.appcompat.lint.Stubs
import androidx.appcompat.res.ColorStateListLoadingDetector
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class ColorStateListLoadingDetectorTest {
    @Test
    fun testCustomGetColorStateList() {
        val customActivity = kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.content.res.ColorStateList
            import androidx.appcompat.app.AppCompatActivity
            import androidx.appcompat.content.res.AppCompatResources

            class ResourceLoader {
                private fun getColorStateList(resourceId: Int): ColorStateList {
                    return AppCompatResources.getColorStateList(CustomActivity.this, resourceId)
                }
            }

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                   ResourceLoader().getColorStateList(R.color.color_state_list)
                }
            }
            """
        ).indented().within("src")

        // We expect a clean Lint run since the call to getColorStateList in activity's onCreate
        // is on our own custom inner class
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            Stubs.APPCOMPAT_RESOURCES,
            Stubs.COLOR_STATE_LIST,
            customActivity
        ).issues(ColorStateListLoadingDetector.NOT_USING_COMPAT_LOADING)
            .run()
            .expectClean()
    }

    @Test
    fun testCoreGetColorStateList() {
        val customActivity = kotlin(
            "com/example/CustomActivity.kt",
            """
            package com.example

            import android.os.Bundle
            import androidx.appcompat.app.AppCompatActivity

            class CustomActivity: AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    getResources().getColorStateList(R.color.color_state_list)
                }
            }
            """
        ).indented().within("src")

        // We expect the call to Resources.getColorStateList to be flagged
        lint().files(
            Stubs.APPCOMPAT_ACTIVITY,
            Stubs.COLOR_STATE_LIST,
            customActivity
        ).issues(ColorStateListLoadingDetector.NOT_USING_COMPAT_LOADING)
            .run()
            .expect("""
src/com/example/CustomActivity.kt:8: Warning: Use AppCompatResources.getColorStateList() [UseCompatLoading]
        getResources().getColorStateList(R.color.color_state_list)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """.trimIndent())
    }
}
