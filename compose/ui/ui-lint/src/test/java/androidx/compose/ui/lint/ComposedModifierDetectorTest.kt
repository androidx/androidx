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

package androidx.compose.ui.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */

/**
 * Test for [ComposedModifierDetector].
 */
@RunWith(JUnit4::class)
class ComposedModifierDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposedModifierDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposedModifierDetector.UnnecessaryComposedModifier)

    /**
     * Simplified Modifier.composed stub
     */
    private val composedStub = bytecodeStub(
        filename = "ComposedModifier.kt",
        filepath = "androidx/compose/ui",
        checksum = 0xc6ba0d09,
        """
            package androidx.compose.ui

            import androidx.compose.runtime.Composable

            fun Modifier.composed(
                inspectorInfo: () -> Unit = {},
                factory: @Composable Modifier.() -> Modifier
            ): Modifier = this
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuOSSMxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxNFeIKSs1NzU1KLfIu4dLkEsZQV5opJOQMYaf45qdkpmWClfJxsZSk
        FpcIsYUASe8SJQYtBgBxwST5ewAAAA==
        """,
        """
        androidx/compose/ui/ComposedModifierKt＄composed＄1.class:
        H4sIAAAAAAAA/5VU6U4TURT+7rR0GYoti7KIO2ILyrTg3oaENBAnFEwEmxh+
        3XYGuHR6x8xMG/zHK/gKPoFoIokmhvjThzKeO20NbqCTzJ2Tc77v7He+fvv4
        GcBdPGYocGl5rrD2jbrbfOn6ttESRrkjWmuuJbaF7a0GU12rNVWIgzGsVhpu
        4Ahp7LWbhpCB7UnuGBXerFm8eNK23ZL1QLjSN1a6Ur7Usz+XIiguFhkm/u4s
        jijD5dMdxhFjiJUEuVtkiGRzVYZo1sxVU0hA19GHflIEu8JnWKj8d8GUYEzI
        ttuwGUayucoeb3PD4XLHeFrbs+tBMYU0kjo0DDL0n6gtjmGGhLm+sbm0Xl5m
        GPip8BTO40ISIxglUKnuhOmrjENXE8p8LknSJMNgj7hmB9ziAaeUtGY7QkNk
        6kiqAwysoYQIGfeFkvIkWQWGyeODhH58oGsZjT6Z44MJLc+e6F/exLSEpjDz
        lHiJS1e+arotn5pIzqb/rVFx3GbI/OiWZW/zlhMwvM7+sdM94llLcoa9UDR/
        n0Pu9IgpzMFgGP61hrkGpRstuxbNd6ji1rlT5Z7gNcfeVAdDuiKkvd5q1myv
        q0mZUtpe2eG+b9NSpZdl3XF9IXdoQLuuxZDcEDuSBy2PwPqG2/Lq9opQzPFn
        LRmIpl0VviBXS1K6AQ9rQ56G3UeNpwuGcTV9mmCUXtoI0syTNEUImg1iM5Ej
        pA7DmS/QmepoMRByBtU6dhmzIYZeBdboyiuYUsRPEFmHmFkiYqZLnFfrpILP
        fMDQO4y9PYWf6AZOUNq9wKOEVk//J2gvjnDxPS4dhoo+3KNTJ1gHMIb7YZ13
        qP4HYZAIHobfAh6Ffym6/8S6soWIiasmrpm4jhsmNeOmiWnc2gLzkUWO7D5m
        fMz6SH8Hu1pp5uIEAAA=
        """,
        """
        androidx/compose/ui/ComposedModifierKt.class:
        H4sIAAAAAAAA/7VUUU8bRxD+9mzss2OIsUlCHEJo4yRgSM4maZvWhAShIJ1q
        3CqmvPC0+NZk8XkP3Z0ReYn4C33sa39B1aeoDxXqY6X+paqz53MggHClqjrd
        7MzOzLffzs7un3//9juAZ1hjeMiV43vSObLaXu/AC4TVl9b6QHU2PUd2pPC/
        DdNgDPl9fsgtl6s967vdfdGm2QSDGSc6DO/nG5fBDWHqja4XulJZ+4c9q9NX
        7VB6KrA2Yq06wl+rL1wNz/DXfyOwMvT/oGRYXx3FZ+Xx1astXu1eHb2f+w3P
        37P2Rbjrc0lLc6W8kA9oNL2w2XddikqthG9lsGoiwzB7hrJUofAVdy1bhT6l
        y3aQxjWGG+23ot2N87/nPu8JCmR4NN84f8L1MzMtDbJXX9jOYRwTWeRwnWGc
        cA8o0PNt1fFMTDKkO1zb70wUGSbKmlv5tEdmR+15blSXjAypUUh+uGLZER3e
        d0OGH//n7rQvVm/kAdf+3fX7WL9yLY07dOfsZmtrrbn+muHppUtcCVHP4S5m
        M5jBvU8b5pJdp/FZDmNIZWHgPsPksAibIuQODzntwegdJug5YVpktAAD62rF
        IOeR1FqVNKfGwE+OZ7Mnx1lj2hgORv5UHfyl5/mT45JRZRX6lydMiiiZhWTB
        qCarieWZ/FhpOrLYQFZTf/ycMsx0JE290DJDYUj0bN/gknndLA8u1tDvq1D2
        RFxIvuuKuu7dOPn1USjoPnlqiLL17kAHFM/X/UmXGi+57jmC4XpDKtHs93aF
        v6UBNRmvzd1t7kttx5OZltxTPOz7pN95M6Bhq0MZSHKvnT4ADOXz3o93+ZOw
        8VbI291NfhAvkLOVEv66y4NAkDvb8vp+W2xI7bsdQ25fWA416oCkPl0ab+uW
        IOsrst6QrY94qlLIfkB+sVAguVSYIln5JYp+TjKla48MviZ9bhCPG7gZ4U1h
        ErfIr7UipinjmygvjXqcadK4Qn8xERtnZD5DdEqkazIvCHpMA91Nvv8J2V8x
        d4LPG5XFpQ8oD8i8IEko4xGriYhJir40vWkpslbJzhLYTMRsGi+jpC/xisYN
        mn9A8A93kLDxyMa8jQVUbCxiycZjPNkBC2ChuoNMgLEANwNMUuECLAcoBnga
        4FmAL/4BKp/c6X8HAAA=
        """
    )

    @Test
    fun noComposableCalls() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.composed
                import androidx.compose.runtime.Composable

                fun Modifier.test(): Modifier = composed {
                    this@test
                }

                fun Modifier.test2(): Modifier {
                    return composed {
                        this@test
                    }
                }

                fun Modifier.test3(): Modifier = composed(factory = {
                    this@test3
                })

                fun Modifier.test4(): Modifier = composed({}, { this@test4})
            """
            ),
            composedStub,
            Stubs.Composable,
            Stubs.Modifier
        )
            .skipTestModes(TestMode.WHITESPACE) // b/202187519, remove when upgrading to 7.1.0
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Unnecessary use of Modifier.composed [UnnecessaryComposedModifier]
                fun Modifier.test(): Modifier = composed {
                                                ~~~~~~~~
src/test/test.kt:13: Warning: Unnecessary use of Modifier.composed [UnnecessaryComposedModifier]
                    return composed {
                           ~~~~~~~~
src/test/test.kt:18: Warning: Unnecessary use of Modifier.composed [UnnecessaryComposedModifier]
                fun Modifier.test3(): Modifier = composed(factory = {
                                                 ~~~~~~~~
src/test/test.kt:22: Warning: Unnecessary use of Modifier.composed [UnnecessaryComposedModifier]
                fun Modifier.test4(): Modifier = composed({}, { this@test4})
                                                 ~~~~~~~~
0 errors, 4 warnings
            """
            )
    }

    @Test
    fun composableCalls() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.composed
                import androidx.compose.runtime.*

                inline fun <T> scopingFunction(lambda: () -> T): T {
                    return lambda()
                }

                fun Modifier.test1(): Modifier = composed {
                    val foo = remember { true }
                    this@test1
                }

                @Composable
                fun composableFunction() {}

                fun Modifier.test2(): Modifier = composed {
                    composableFunction()
                    this@test2
                }

                fun Modifier.test3(): Modifier = composed {
                    scopingFunction {
                        val foo = remember { true }
                        this@test3
                    }
                }

                @Composable
                fun <T> composableScopingFunction(lambda: @Composable () -> T): T {
                    return lambda()
                }

                fun Modifier.test4(): Modifier = composed {
                    composableScopingFunction {
                        this@test4
                    }
                }

                val composableProperty: Boolean
                    @Composable
                    get() = true

                fun Modifier.test5(): Modifier = composed {
                    composableProperty
                    this@test5
                }

                // Test for https://youtrack.jetbrains.com/issue/KT-46795
                fun Modifier.test6(): Modifier = composed {
                    val nullable: Boolean? = null
                    val foo = nullable ?: false
                    val bar = remember { true }
                    this@test6
                }
            """
            ),
            composedStub,
            Stubs.Composable,
            Stubs.Modifier,
            Stubs.Remember
        )
            .skipTestModes(TestMode.WHITESPACE) // b/202187519, remove when upgrading to 7.1.0
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
