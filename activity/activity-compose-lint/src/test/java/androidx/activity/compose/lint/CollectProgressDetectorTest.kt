/*
 * Copyright 2023 The Android Open Source Project
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
class CollectProgressDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = CollectProgressDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(CollectProgressDetector.NoCollectCallFound)

    private val PREDICTIVE_BACK_HANDLER = bytecode(
        "libs/predictivebackhandler.jar",
        kotlin(
            """
    package androidx.activity.compose

    public fun PredictiveBackHandler(
        enabled: Boolean = true,
        onBack: (progress: String) -> Unit) { }
    
    """
    ).indented(),
        0xd7427505,
    """
    META-INF/main.kotlin_module:
    H4sIAAAAAAAA/2NgYGBmYGBgBGJWKM3AZcIlmZiXUpSfmVKhl5hcklmWWVKp
    l5yfW5BfnCokHlCUmpIJEk11SkzO9gCqzEkt8i7hEuXiBqrRS61IzC3ISRVi
    C0ktLvEuUWLQYgAAnvRwIWUAAAA=
    """,
    """
    androidx/activity/compose/PredictiveBackHandlerKt.class:
    H4sIAAAAAAAA/4VSXU8TQRQ9s9222/JVFkGogCAoIMIWNOGhxkRNiI2lElEe
    4GnYDnXodpbsTht8MfwNX/0HvhEfDPHRH2W8sy0fURLa5N479557zt078/vP
    j58AnmGNYY2rehTK+onHfS07Un/2/LB1HMbC245EXZqkeMX95hsCBiJ6q7Ng
    DIUj3uFewFXDe3dwJHzKphhGb2xhmF/cqzZDHUjlHXVa3mFbESZUsbfZi9bK
    S7sMm7fCnq9Ur4R3dCRVo3zR8lFJXX6REM1Vw6jhHQl9EHFJ/VypUPMuVy3U
    tXYQlBkyoTJTOsgxTF/TlUqLSPHAqyijEEs/zqKPvs7/JPxmr3+bR7wltPm6
    hcXqv9so/z/m0m4/BjCYRz+GGLJC8YNA1BnYHsPMbdthmLpxtfN1ccjbgWbY
    uH3Flf/HNEOlkcnDwhjD8AXDltC8zjUnXavVSdFbYcakadqmCSzKn0gTlSiq
    0zPaPz+dzp+f5q2C1XV9iRu3KBzsescqPi2cnxatElt3HAJSlFqfLdjFKXfE
    HS5lfn3L9DtZ13Ec13acxZxru4QtpY3EOqMZ4F4MeH0zYxfJyyupkaOCrcgz
    OMdR2IhEHDOM37jE1Sbtz34d1gk8VJVK1NqtAxF9MBdkNEOfB7s8kubcS+Z2
    ZENx3Y4onn/fVlq2REV1ZCypfDnHy6tnxzCwo0lyix/3KPI7YTvyxaY0h4ke
    x26X4Voj1uhubJifhQlzWUhhiU5lOlvks8tu/gyF7wngMdkMLSpD/2WKx7oQ
    DMNNKLLIYYTqTxJ0FivkcwZCawIKOdzBKMWGf6OnOzhpf/mKtF0uLp/hbldm
    lWwKzEn0BmEegk2KaSKxqewloEWUyFeIbpwqE/tIVVCs4B5ZTFYwhekK7mNm
    HyzGLB7sIx8jHWMuxnBiczHmk+BhjEcxFv4C1+LeOLYEAAA=
    """
    )

    @Test
    fun errors() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.compose.runtime.Composable
                import androidx.activity.compose.PredictiveBackHandler

                @Composable
                fun Test() {
                    PredictiveBackHandler { progress -> }
                }

                val lambda = @Composable {
                    PredictiveBackHandler { progress -> }
                }

                val lambda2: @Composable () -> Unit = {
                    PredictiveBackHandler { progress -> }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        PredictiveBackHandler { progress -> }
                    })
                    LambdaParameter {
                        PredictiveBackHandler { progress -> }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        PredictiveBackHandler { progress -> }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        PredictiveBackHandler { progress -> }
                    }
                }
            """
            ),
            Stubs.Composable,
            PREDICTIVE_BACK_HANDLER
        )
            .run()
            .expect(
                """
src/com/example/test.kt:9: Error: You must call collect() on Flow progress [NoCollectCallFound]
                    PredictiveBackHandler { progress -> }
                                            ~~~~~~~~
src/com/example/test.kt:13: Error: You must call collect() on Flow progress [NoCollectCallFound]
                    PredictiveBackHandler { progress -> }
                                            ~~~~~~~~
src/com/example/test.kt:17: Error: You must call collect() on Flow progress [NoCollectCallFound]
                    PredictiveBackHandler { progress -> }
                                            ~~~~~~~~
src/com/example/test.kt:26: Error: You must call collect() on Flow progress [NoCollectCallFound]
                        PredictiveBackHandler { progress -> }
                                                ~~~~~~~~
src/com/example/test.kt:29: Error: You must call collect() on Flow progress [NoCollectCallFound]
                        PredictiveBackHandler { progress -> }
                                                ~~~~~~~~
src/com/example/test.kt:35: Error: You must call collect() on Flow progress [NoCollectCallFound]
                        PredictiveBackHandler { progress -> }
                                                ~~~~~~~~
src/com/example/test.kt:39: Error: You must call collect() on Flow progress [NoCollectCallFound]
                        PredictiveBackHandler { progress -> }
                                                ~~~~~~~~
7 errors, 0 warnings
            """
            )
    }

    @Test
    fun errorWithNoCollect() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.compose.runtime.Composable
                import androidx.activity.compose.PredictiveBackHandler

                @Composable
                fun Test() {
                    PredictiveBackHandler { progress ->
                        progress
                    }
                }
            """
            ),
            Stubs.Composable,
            PREDICTIVE_BACK_HANDLER
        )
            .run()
            .expect(
                """
src/com/example/test.kt:10: Error: You must call collect() on Flow null [NoCollectCallFound]
                        progress
                        ~~~~~~~~
1 errors, 0 warnings
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
                import androidx.activity.compose.PredictiveBackHandler

                @Composable
                fun Test() {
                    PredictiveBackHandler { progress ->
                        progress.collect()
                    }
                }

                val lambda = @Composable {
                    PredictiveBackHandler { progress ->
                        progress.collect()
                    }
                }

                val lambda2: @Composable () -> Unit = {
                    PredictiveBackHandler { progress ->
                        progress.collect()
                    }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        PredictiveBackHandler { progress ->
                            progress.collect()
                        }
                    })
                    LambdaParameter {
                        PredictiveBackHandler { progress ->
                            progress.collect()
                        }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        PredictiveBackHandler { progress ->
                            progress.collect()
                        }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        PredictiveBackHandler { progress ->
                            progress.collect()
                        }
                    }
                }
            """
            ),
            Stubs.Composable,
            PREDICTIVE_BACK_HANDLER
        )
            .run()
            .expectClean()
    }
}
