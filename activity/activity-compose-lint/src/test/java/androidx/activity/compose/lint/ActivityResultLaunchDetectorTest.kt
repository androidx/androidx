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
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [ActivityResultLaunchDetector]. */
class ActivityResultLaunchDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ActivityResultLaunchDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ActivityResultLaunchDetector.LaunchDuringComposition)

    private val MANAGED_ACTIVITY_RESULT_LAUNCHER =
        bytecodeStub(
            filename = "ActivityResultRegistry.kt",
            filepath = "androidx/activity/compose",
            checksum = 0xef067b97,
            source =
                """
    package androidx.activity.compose

    public class ManagedActivityResultLauncher<I> {
        fun launch(input: I) { }
    }
    """,
            """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgEuXiTs7P1UutSMwtyEkVYgtJLS7x
    LlFi0GIAAJY6UNwvAAAA
    """,
            """
    androidx/activity/compose/ManagedActivityResultLauncher.class:
    H4sIAAAAAAAA/51Ry24TMRQ9dpLJoy2ZtDSk5f2S0i6YNALxqiqVSohBKUhp
    lU1WTmKlbhJPNfZE7S7fwh+wQmKBIpZ8FOJ6mg0tK7w499zjc+177V+/v/8A
    8BxPGV4KPYgjNTgPRN+qqbIXQT+anEVGBodCi6Ec7C/0tjTJ2LZEovsnMs6D
    MdR3wzetUzEVwVjoYfC5dyr79u3edYnBv6rlkWXwdpVWdo8hU9/qLMNDvoQc
    CgxZe6IMw+vWf7ZHN3rjlDOs1683tNUhQ/04TElO6bPEMqz+q/FKaxTZsdLB
    obRiIKwgjU+mGXpA5qDoAAxsRPq5clmD2GCH4cN8Vi3xGi/NZyXuOyhwItzF
    2ny2nS3MZz5r8gZ/l/v5xeN+5uOan93kjVzT871N/mo+S+W8O69JV4TM3bTx
    98htOVTGxhfPRjRC9iAaSIZyS2n5KZn0ZHwsemPpZov6YtwRsXL5QiweqaEW
    NomJl46iJO7L98ptbLQTbdVEdpRR5NzXOrLCqkgb7IDTF7nFqRf6McL7lAXu
    DSjmtr+h+DXdfkDopWIWDwmXLw0oYYlihfIVcrniF6mfhrtamEsLq5ebi0LH
    yvAJH6XuG8gsWAaP03gPTygekKNCJ6x2kQmxFuJmiHVUieJWiBo2umAGm7jd
    RcFgyeCOwV2DvMFySsoGK38AjaXx2SsDAAA=
    """
        )

    @Test
    fun errors() {
        lint()
            .files(
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
        lint()
            .files(
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
