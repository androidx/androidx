/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.activity.compose.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.compiledStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [ActivityResultLaunchDetector].
 */
class ActivityResultLaunchDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ActivityResultLaunchDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ActivityResultLaunchDetector.LaunchDuringComposition)

    private val MANAGED_ACTIVITY_RESULT_LAUNCHER = compiledStub(
        filename = "ActivityResultRegistry.kt",
        filepath = "androidx/activity/compose",
        checksum = 0x42f3e9f,
        source = """
    package androidx.activity.compose

    public class ManagedActivityResultLauncher<I> {
        fun launch(input: I) { }
    }
    """,
        """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3AJcrFnZyfq5dakZhbkJMqxBaSWlzi
    XaLEoMUAAIXWemUvAAAA
    """,
        """
    androidx/activity/compose/ManagedActivityResultLauncher.class:
    H4sIAAAAAAAAAJ1Ry24aMRQ9NjAQQsqQNBTo+7EgWXQIatVXFCmNVJWKpBKJ
    2LAyYBEH8ERjD0p2fEv/oKtKXVSoy35U1esJmyZd1Ytzzz0+1/fa/vX7+w8A
    L/CM4ZXQwyhUw4tADKyaKXsZDMLpeWhkcCi0GMnh/lLvSBNPbFvEenAqoywY
    Q3239bZ9JmYimAg9Cj73z+TAvtu7KTH417Us0gzertLK7jGk6lvdAjxk88gg
    x5C2p8owvGn/53jU0ZsknGGzfnOgrS4Z6iethGSUPo8tw/q/Bi+1x6GdKB0c
    SiuGwgrS+HSWogdkDjIMbEzShXJZg9hwh+HjYl7O8wrPL+Z57jvIcSLcxcpi
    vp3OLeY+a/IGf5/5+cXjfurThp+u8Uam6flejb9ezBM5685rUosWo36o/n3b
    jhwpY6PL52OaPn0QDiVDsa20PIqnfRmdiP5EumuFAzHpiki5fCmuHKuRFjaO
    iOePwzgayA/KbVQ7sbZqKrvKKHLuax1aYVWoDXbA6Xfc4jQLfRbhA8oCisw9
    xfY3rHxNth8SeomYxiPCwpUBeaxSLFG+Ri5X/DLx0+WuF2aSwvLV5rLQsSJ8
    wseJ+xZSS5bCkyTex1OKB+Qo0QnrPaRa2GjhNiE2HZRbuINKD8ygiloPOYNV
    g7sG9wyyBoWEFA3W/gCvD9WQJgMAAA==
    """
    )

    @Test
    fun errors() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.compose.runtime.Composable
                import androidx.activity.compose.ManagedActivityResultLauncher

                @Composable
                fun Test() {
                    val launcher = ManagedActivityResultLauncher<String>()
                    launcher.launch("test")
                }

                val lambda = @Composable {
                    val launcher = ManagedActivityResultLauncher<String>()
                    launcher.launch("test")
                }

                val lambda2: @Composable () -> Unit = {
                    val launcher = ManagedActivityResultLauncher<String>()
                    launcher.launch("test")
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        val launcher = ManagedActivityResultLauncher<String>()
                        launcher.launch("test")
                    })
                    LambdaParameter {
                        val launcher = ManagedActivityResultLauncher<String>()
                        launcher.launch("test")
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        val launcher = ManagedActivityResultLauncher<String>()
                        launcher.launch("test")
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val launcher = ManagedActivityResultLauncher<String>()
                        launcher.launch("test")
                    }
                }
            """
            ),
            Stubs.Composable,
            MANAGED_ACTIVITY_RESULT_LAUNCHER
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
src/com/example/test.kt:10: Error: Calls to launch should happen inside of a SideEffect and not during composition [LaunchDuringComposition]
                    launcher.launch("test")
                             ~~~~~~
src/com/example/test.kt:15: Error: Calls to launch should happen inside of a SideEffect and not during composition [LaunchDuringComposition]
                    launcher.launch("test")
                             ~~~~~~
src/com/example/test.kt:20: Error: Calls to launch should happen inside of a SideEffect and not during composition [LaunchDuringComposition]
                    launcher.launch("test")
                             ~~~~~~
src/com/example/test.kt:30: Error: Calls to launch should happen inside of a SideEffect and not during composition [LaunchDuringComposition]
                        launcher.launch("test")
                                 ~~~~~~
src/com/example/test.kt:34: Error: Calls to launch should happen inside of a SideEffect and not during composition [LaunchDuringComposition]
                        launcher.launch("test")
                                 ~~~~~~
src/com/example/test.kt:41: Error: Calls to launch should happen inside of a SideEffect and not during composition [LaunchDuringComposition]
                        launcher.launch("test")
                                 ~~~~~~
src/com/example/test.kt:46: Error: Calls to launch should happen inside of a SideEffect and not during composition [LaunchDuringComposition]
                        launcher.launch("test")
                                 ~~~~~~
7 errors, 0 warnings
            """
            )
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.compose.runtime.Composable
                import androidx.activity.compose.ManagedActivityResultLauncher

                fun test() {
                    val launcher = ManagedActivityResultLauncher<String>()
                    launcher.launch("test")
                }

                val lambda = {
                    val launcher = ManagedActivityResultLauncher<String>()
                    launcher.launch("test")
                }

                val lambda2: () -> Unit = {
                    val launcher = ManagedActivityResultLauncher<String>()
                    launcher.launch("test")
                }

                fun lambdaParameter(action: () -> Unit) {}

                fun test2() {
                    lambdaParameter(action = {
                        val launcher = ManagedActivityResultLauncher<String>()
                        launcher.launch("test")
                    })
                    lambdaParameter {
                        val launcher = ManagedActivityResultLauncher<String>()
                        launcher.launch("test")
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        val launcher = ManagedActivityResultLauncher<String>()
                        launcher.launch("test")
                    }

                    val localLambda2: () -> Unit = {
                        val launcher = ManagedActivityResultLauncher<String>()
                        launcher.launch("test")
                    }
                }
            """
            ),
            Stubs.Composable,
            MANAGED_ACTIVITY_RESULT_LAUNCHER
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
