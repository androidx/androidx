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

package androidx.compose.animation.core.lint

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

/** Test for [UnrememberedAnimatableDetector]. */
class UnrememberedAnimatableDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = UnrememberedAnimatableDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(UnrememberedAnimatableDetector.UnrememberedAnimatable)

    // Simplified Animatable Color function stub, from androidx.compose.animation
    private val AnimatableColorStub =
        bytecodeStub(
            filename = "SingleValueAnimation.kt",
            filepath = "androidx/compose/animation",
            checksum = 0x98c0a447,
            """
            package androidx.compose.animation

            import androidx.compose.animation.core.Animatable
            import androidx.compose.ui.graphics.Color

            fun Animatable(initialValue: Color): Animatable<Color, Any> = Animatable(initialValue)
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/3XLvQvCQAwF8IiiGEThBhERBBfBoS6Cszh2s+KetqE9uI9y
        TcE/3xN1KgZehsf7AcAQAAYxU/gennBNrgxel8+k8LbxLSfktCXR3qllpl1l
        +EGm48uvTQWPuP2vYhNYzT57yg1HcMZVD4TOibas8MaWbc4hFbXIHDVt7SUT
        kjfc46YHO51UgZpaF62aXL3xUeIcR8KtqPE9/lR2cIAXLlZThPEAAAA=
        """,
            """
        androidx/compose/animation/SingleValueAnimationKt.class:
        H4sIAAAAAAAA/5VTXU8TQRQ9s223Za20VFEoigooHwLTEh8wJUSCMSkWTMQ0
        MTyYaTvWabczZHa24ZH4T/QX+CbRxBB880cZZ1sIiSUKD3vn3Dvnzv3cX7+/
        /QDwBJSgyGRDK9E4oHXV2VcBp0yKDjNCSborZNPnVeaHfOPM+NIkQQiyLdZl
        1GeySV/VWrxurTGCXJ/Gaj5fWn23WvT9DQI6tzVf+UeUutKcnjuWCJpX81gb
        5IaCNjXb/yDqAd1UvtKlyt8Jl9ZtpOmK0k3a4qammZCBjSGV6QUJ6I4yO6Hv
        W9bC5XNJYohg6v/5JHGNIFVTB0uis+8TzF5Y8mAZaVzHsIc0MgTumpDCrBNU
        5warG7SUK21lfCFpq9uhQhquJfPpc/6ehb7ZtBUbHdaN0ttMt7kuzVfTGEHO
        g4cbBOkolGB+bxkIyBbByNl729ywhi3fdsrpdGN2s0gkhiIBy21HwLGXByJC
        BYsaRYLt48Ocd3zoOWNO/8tGSiqWn7EgP5yL55yC05OxAllxs/G81X8ekePD
        k89uPJXIuicfnaSXSJ18miyQ6NEVAu98FASLV1u7mcsMgEQl3b7oz1huG4L4
        pmrYyJmKkHwn7NS4ftPPJVdR9ah9WkT6qXFoVzQlM6G2eOJ1KI3o8LLsikDY
        643zXbR17apQ1/kLEbmNn1KrA0QU4SCOfuPHkYCLGOas9tQix56Z7/DeTh4h
        SxLkK25+iSaDeStd6wAksWBluoczGMUtez7ucZJYPGWlevpST85i2Z7PopbY
        YGN7iJUxXka+jAncKeMuJsu4h/t7IAEeYGoPyQDTAWYCPAzwKMBogEQA9w+x
        P7r8kQQAAA==
        """
        )

    @Test
    fun notRemembered() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.animation.*
                import androidx.compose.animation.core.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.graphics.*

                @Composable
                fun Test() {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = Animatable<Boolean, Any>(false)
                    val animatable2 = Animatable(0f)
                    // Color function from androidx.compose.animation
                    val animatable3 = Animatable(Color.Red)
                }

                val lambda = @Composable {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = Animatable<Boolean, Any>(false)
                    val animatable2 = Animatable(0f)
                    // Color function from androidx.compose.animation
                    val animatable3 = Animatable(Color.Red)
                }

                val lambda2: @Composable () -> Unit = {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = Animatable<Boolean, Any>(false)
                    val animatable2 = Animatable(0f)
                    // Color function from androidx.compose.animation
                    val animatable3 = Animatable(Color.Red)
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    })
                    LambdaParameter {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }

                    val localLambda2: @Composable () -> Unit = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }

                @Composable
                fun Test4() {
                    val localObject = object {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }
            """
                ),
                Stubs.Animatable,
                AnimatableColorStub,
                Stubs.Color,
                Stubs.Composable,
                Stubs.Remember,
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
src/test/{.kt:12: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable = Animatable<Boolean, Any>(false)
                                     ~~~~~~~~~~
src/test/{.kt:13: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable2 = Animatable(0f)
                                      ~~~~~~~~~~
src/test/{.kt:15: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable3 = Animatable(Color.Red)
                                      ~~~~~~~~~~
src/test/{.kt:20: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable = Animatable<Boolean, Any>(false)
                                     ~~~~~~~~~~
src/test/{.kt:21: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable2 = Animatable(0f)
                                      ~~~~~~~~~~
src/test/{.kt:23: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable3 = Animatable(Color.Red)
                                      ~~~~~~~~~~
src/test/{.kt:28: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable = Animatable<Boolean, Any>(false)
                                     ~~~~~~~~~~
src/test/{.kt:29: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable2 = Animatable(0f)
                                      ~~~~~~~~~~
src/test/{.kt:31: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                    val animatable3 = Animatable(Color.Red)
                                      ~~~~~~~~~~
src/test/{.kt:41: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable = Animatable<Boolean, Any>(false)
                                         ~~~~~~~~~~
src/test/{.kt:42: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable2 = Animatable(0f)
                                          ~~~~~~~~~~
src/test/{.kt:44: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable3 = Animatable(Color.Red)
                                          ~~~~~~~~~~
src/test/{.kt:48: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable = Animatable<Boolean, Any>(false)
                                         ~~~~~~~~~~
src/test/{.kt:49: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable2 = Animatable(0f)
                                          ~~~~~~~~~~
src/test/{.kt:51: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable3 = Animatable(Color.Red)
                                          ~~~~~~~~~~
src/test/{.kt:58: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable = Animatable<Boolean, Any>(false)
                                         ~~~~~~~~~~
src/test/{.kt:59: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable2 = Animatable(0f)
                                          ~~~~~~~~~~
src/test/{.kt:61: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable3 = Animatable(Color.Red)
                                          ~~~~~~~~~~
src/test/{.kt:66: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable = Animatable<Boolean, Any>(false)
                                         ~~~~~~~~~~
src/test/{.kt:67: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable2 = Animatable(0f)
                                          ~~~~~~~~~~
src/test/{.kt:69: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable3 = Animatable(Color.Red)
                                          ~~~~~~~~~~
src/test/{.kt:77: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable = Animatable<Boolean, Any>(false)
                                         ~~~~~~~~~~
src/test/{.kt:78: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable2 = Animatable(0f)
                                          ~~~~~~~~~~
src/test/{.kt:80: Error: Creating an Animatable during composition without using remember [UnrememberedAnimatable]
                        val animatable3 = Animatable(Color.Red)
                                          ~~~~~~~~~~
24 errors, 0 warnings
            """
            )
    }

    @Test
    fun rememberedInsideComposableBody() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.animation.*
                import androidx.compose.animation.core.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.graphics.*

                @Composable
                fun Test() {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = remember { Animatable(0f) }
                    val animatable2 = remember { Animatable<Boolean, Any>(false) }
                    // Color function from androidx.compose.animation
                    val animatable3 = remember { Animatable(Color.Red) }
                }

                val lambda = @Composable {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = remember { Animatable(0f) }
                    val animatable2 = remember { Animatable<Boolean, Any>(false) }
                    // Color function from androidx.compose.animation
                    val animatable3 = remember { Animatable(Color.Red) }
                }

                val lambda2: @Composable () -> Unit = {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = remember { Animatable(0f) }
                    val animatable2 = remember { Animatable<Boolean, Any>(false) }
                    // Color function from androidx.compose.animation
                    val animatable3 = remember { Animatable(Color.Red) }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = remember { Animatable(0f) }
                        val animatable2 = remember { Animatable<Boolean, Any>(false) }
                        // Color function from androidx.compose.animation
                        val animatable3 = remember { Animatable(Color.Red) }
                    })
                    LambdaParameter {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = remember { Animatable(0f) }
                        val animatable2 = remember { Animatable<Boolean, Any>(false) }
                        // Color function from androidx.compose.animation
                        val animatable3 = remember { Animatable(Color.Red) }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = remember { Animatable(0f) }
                        val animatable2 = remember { Animatable<Boolean, Any>(false) }
                        // Color function from androidx.compose.animation
                        val animatable3 = remember { Animatable(Color.Red) }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = remember { Animatable(0f) }
                        val animatable2 = remember { Animatable<Boolean, Any>(false) }
                        // Color function from androidx.compose.animation
                        val animatable3 = remember { Animatable(Color.Red) }
                    }
                }
            """
                ),
                Stubs.Animatable,
                AnimatableColorStub,
                Stubs.Color,
                Stubs.Composable,
                Stubs.Remember,
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker
            )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.animation.*
                import androidx.compose.animation.core.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.graphics.*

                fun test() {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = Animatable<Boolean, Any>(false)
                    val animatable2 = Animatable(0f)
                    // Color function from androidx.compose.animation
                    val animatable3 = Animatable(Color.Red)
                }

                val lambda = {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = Animatable<Boolean, Any>(false)
                    val animatable2 = Animatable(0f)
                    // Color function from androidx.compose.animation
                    val animatable3 = Animatable(Color.Red)
                }

                val lambda2: () -> Unit = {
                    // Float function and constructor from androidx.compose.animation.core
                    val animatable = Animatable<Boolean, Any>(false)
                    val animatable2 = Animatable(0f)
                    // Color function from androidx.compose.animation
                    val animatable3 = Animatable(Color.Red)
                }

                fun LambdaParameter(content: () -> Unit) {}

                fun test2() {
                    LambdaParameter(content = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    })
                    LambdaParameter {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }

                    val localLambda2: () -> Unit = {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }

                fun test3() {
                    class Foo {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }

                    val localObject = object {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }

                @Composable
                fun Test4() {
                    class Foo {
                        // Float function and constructor from androidx.compose.animation.core
                        val animatable = Animatable<Boolean, Any>(false)
                        val animatable2 = Animatable(0f)
                        // Color function from androidx.compose.animation
                        val animatable3 = Animatable(Color.Red)
                    }
                }
            """
                ),
                Stubs.Animatable,
                AnimatableColorStub,
                Stubs.Color,
                Stubs.Composable,
                Stubs.Remember,
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker
            )
            .run()
            .expectClean()
    }
}
