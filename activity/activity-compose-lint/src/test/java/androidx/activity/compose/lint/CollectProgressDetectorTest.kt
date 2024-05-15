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

    @Test
    fun noErrorsCollectIndexed() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.compose.runtime.Composable
                import androidx.activity.compose.PredictiveBackHandler

                @Composable
                fun Test() {
                    PredictiveBackHandler { progress ->
                        progress.collectIndexed()
                    }
                }

                val lambda = @Composable {
                    PredictiveBackHandler { progress ->
                        progress.collectIndexed()
                    }
                }

                val lambda2: @Composable () -> Unit = {
                    PredictiveBackHandler { progress ->
                        progress.collectIndexed()
                    }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        PredictiveBackHandler { progress ->
                            progress.collectIndexed()
                        }
                    })
                    LambdaParameter {
                        PredictiveBackHandler { progress ->
                            progress.collectIndexed()
                        }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        PredictiveBackHandler { progress ->
                            progress.collectIndexed()
                        }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        PredictiveBackHandler { progress ->
                            progress.collectIndexed()
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

    @Test
    fun noErrorsCollectLatest() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.compose.runtime.Composable
                import androidx.activity.compose.PredictiveBackHandler

                @Composable
                fun Test() {
                    PredictiveBackHandler { progress ->
                        progress.collectLatest()
                    }
                }

                val lambda = @Composable {
                    PredictiveBackHandler { progress ->
                        progress.collectLatest()
                    }
                }

                val lambda2: @Composable () -> Unit = {
                    PredictiveBackHandler { progress ->
                        progress.collectLatest()
                    }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        PredictiveBackHandler { progress ->
                            progress.collectLatest()
                        }
                    })
                    LambdaParameter {
                        PredictiveBackHandler { progress ->
                            progress.collectLatest()
                        }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        PredictiveBackHandler { progress ->
                            progress.collectLatest()
                        }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        PredictiveBackHandler { progress ->
                            progress.collectLatest()
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
