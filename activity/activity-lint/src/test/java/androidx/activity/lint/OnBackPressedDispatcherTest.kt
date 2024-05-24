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

package androidx.activity.lint

import androidx.activity.lint.stubs.STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OnBackPressedDispatcherTest : LintDetectorTest() {
    override fun getDetector(): Detector = OnBackPressedDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(OnBackPressedDetector.ISSUE)

    @Test
    fun expectPassOnBackPressed() {
        lint()
            .files(
                kotlin(
                    """
                package com.example

                import androidx.activity.ComponentActivity
                import androidx.activity.OnBackPressedDispatcher

                fun test() {
                    val activity = ComponentActivity()
                    activity.onBackPressed()
                    val dispatcher = OnBackPressedDispatcher()
                    dispatcher.onBackPressed()
                }
            """
                ),
                *STUBS
            )
            .run()
            .expectClean()
    }

    @Test
    fun expectFailOnBackPressed() {
        lint()
            .files(
                kotlin(
                    """
                package com.example

                import androidx.activity.ComponentActivity
                import androidx.activity.OnBackPressedCallback
                import androidx.activity.OnBackPressedDispatcher

                fun test() {
                    object: OnBackPressedCallback {
                        override fun handledOnBackPressed() {
                            val activity = ComponentActivity()
                            activity.onBackPressed()
                            val dispatcher = OnBackPressedDispatcher()
                            dispatcher.onBackPressed()
                        }
                    }
                }
            """
                ),
                *STUBS
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:12: Warning: Should not call onBackPressed inside of OnBackPressedCallback.handledOnBackPressed [InvalidUseOfOnBackPressed]
                                            activity.onBackPressed()
                                            ~~~~~~~~~~~~~~~~~~~~~~~~
                src/com/example/test.kt:14: Warning: Should not call onBackPressed inside of OnBackPressedCallback.handledOnBackPressed [InvalidUseOfOnBackPressed]
                                            dispatcher.onBackPressed()
                                            ~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 2 warnings
                """
            )
    }
}
