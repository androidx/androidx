/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.activity.compose.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BackHandlerOnBackPressedDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = BackHandlerOnBackPressedDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(BackHandlerOnBackPressedDetector.InvalidOnBackPressed)

    @Test
    fun expectPassOnBackPressed() {
        lint()
            .files(
                kotlin(
                    """
                package com.example

                import androidx.compose.runtime.Composable
                import androidx.activity.ComponentActivity
                import androidx.activity.OnBackPressedDispatcher

                @Composable
                fun Test() {
                    val activity = ComponentActivity()
                    activity.onBackPressed()
                    val dispatcher = OnBackPressedDispatcher()
                    dispatcher.onBackPressed()
                }
            """
                ),
                Stubs.Composable,
                COMPONENT_ACTIVITY,
                ON_BACK_PRESSED_DISPATCHER,
            )
            .run()
            .expectClean()
    }

    @Test
    fun errors() {
        lint()
            .files(
                kotlin(
                    """
                package com.example

                import androidx.compose.runtime.Composable
                import androidx.activity.compose.BackHandler
                import androidx.activity.compose.PredictiveBackHandler
                import androidx.activity.ComponentActivity
                import androidx.activity.OnBackPressedDispatcher

                @Composable
                fun Test() {
                    PredictiveBackHandler { progress ->
                        progress.collect()
                        val activity = ComponentActivity()
                        activity.onBackPressed()
                        val dispatcher = OnBackPressedDispatcher()
                        dispatcher.onBackPressed()
                    }

                    BackHandler {
                        val activity = ComponentActivity()
                        activity.onBackPressed()
                        val dispatcher = OnBackPressedDispatcher()
                        dispatcher.onBackPressed()
                    }
                }
            """
                ),
                Stubs.Composable,
                BACK_HANDLER,
                COMPONENT_ACTIVITY,
                ON_BACK_PRESSED_DISPATCHER,
                PREDICTIVE_BACK_HANDLER
            )
            .run()
            .expect(
                """
src/com/example/test.kt:15: Warning: Should not call onBackPressed inside of BackHandler [OnBackPressedInsideOfBackHandler]
                        activity.onBackPressed()
                                 ~~~~~~~~~~~~~
src/com/example/test.kt:17: Warning: Should not call onBackPressed inside of BackHandler [OnBackPressedInsideOfBackHandler]
                        dispatcher.onBackPressed()
                                   ~~~~~~~~~~~~~
src/com/example/test.kt:22: Warning: Should not call onBackPressed inside of BackHandler [OnBackPressedInsideOfBackHandler]
                        activity.onBackPressed()
                                 ~~~~~~~~~~~~~
src/com/example/test.kt:24: Warning: Should not call onBackPressed inside of BackHandler [OnBackPressedInsideOfBackHandler]
                        dispatcher.onBackPressed()
                                   ~~~~~~~~~~~~~
0 errors, 4 warnings
            """
            )
    }
}
