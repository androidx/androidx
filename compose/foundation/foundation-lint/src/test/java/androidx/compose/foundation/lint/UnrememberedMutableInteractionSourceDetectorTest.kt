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

package androidx.compose.foundation.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UnrememberedMutableInteractionSourceDetectorTest : LintDetectorTest() {

    private val InteractionSourceStub: TestFile = bytecodeStub(
        filename = "InteractionSource.kt",
        filepath = "androidx/compose/foundation/interaction",
        checksum = 0x7b8a9d44,
        source = """
        package androidx.compose.foundation.interaction

        interface InteractionSource

        interface MutableInteractionSource : InteractionSource

        fun MutableInteractionSource(): MutableInteractionSource = MutableInteractionSourceImpl()

        private class MutableInteractionSourceImpl : MutableInteractionSource
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGIOBijgsuNST8xLKcrPTKnQS87PLcgvTtVL
        yy/NS0ksyczP08vMK0ktSkwGsYWEPRGc4PzSouRU7xIuNS4JDP1FpXklmbmp
        QlxBqbmpuUmpRUB1fFwsJanFJUJsIUDSu0SJQYsBAEnB5baQAAAA
        """,
        """
        androidx/compose/foundation/interaction/InteractionSource.class:
        H4sIAAAAAAAA/52OP0/DMBDF3znQhPAvhVYqX4K0FQvqxIIUqQgJJJZMbuIi
        N4mNYqfq2M/VAXXmQyGcdGBg4yw9/+5Ouve+vnefAO4wJNxzldda5ps409WH
        NiJe6kbl3EqtYqmsqHnWcfLLr7qpM+GDCNGKr3lccvUePy9WIrM+PEJ/Xmhb
        ShU/CcvdLT4jsGrtOVNqJWgFBCrcfCPbbuwonxCG+20QshELWeRoOdpvp2xM
        7XJKmM3/ndYlcIaDP/PbwhLCAz/KUhBuXhplZSXepJGLUjwopW1nYHouBo5w
        KIbrTq8wcP/EHT92r5fCS+AnCBKcIHSI0wRnOE9BBhe4TMEMIoP+DyYeb+yF
        AQAA
        """,
        """
        androidx/compose/foundation/interaction/InteractionSourceKt.class:
        H4sIAAAAAAAA/61R30sbQRD+5qIXPW2NP5vE/s6Lvnim+GaRtkrhaJpCW4SS
        p01uK5vc7crdXvAxf1LfBAXJc/8ocTYKPoSCYPdh5pvZb77Znfl7fXEFYA8N
        wr7QcWZUfBb2THpqchn+NoWOhVVGh0pbmYneBEf3+Icpsp78YssgQqUvhiJM
        hD4Jv3X7ssfZEqH6tbCim8ipKsLR1nbroU3/pbJPaLRMdhL2pe1mQuk8FFob
        O1HIw7ax7SJJmHX02E5RepqUMUfw3yut7AGhtLV9vIgACwHmsUj48NgWZTwl
        LLcGxiaKWdIKLhb8eC8dlnhP5MycMyDQwAGPL8+UQ7uM4iZhdTzyg/Eo8Kpe
        3a+MR3Vvl9zVO8Kn/zHutankzsASZg5NzDtdaikt20XaldlPp0DY/F5oq1LW
        Gqpccerj/X4Iwa3EZ+WotTvq8RQRTXiYwe3Pa5iFz/FLjhrs3Vm4xPyvczwZ
        Y+nPhPSKrc+eB4bXd7jsBoY3E/sCb9k3OVthueUOShFWIqxGWMN6hA08i1BF
        rQPKUcdmB16O2RzPbwCz5fAqMwMAAA==
        """,
        """
        androidx/compose/foundation/interaction/MutableInteractionSource.class:
        H4sIAAAAAAAA/6VQvU7DMBi8L4E2hL8WApSXIG3VBXUBBqRIRUggsXRyExe5
        SWyUOFXHPhcD6sxDIb6UgYEOlRh8Pp/1ne/8+fX+AWCAC8KN0ElhVLIIY5O/
        mVKGU1PpRFhldKi0lYWI1/yhsmKSyehXejZVEcsmiNCaibkIM6Ffw8fJTMa2
        CZdwva33BtNdQnuUGpspflpawVNiSHDyucvRqQavBhAoZX2h6lOXWdIjBKul
        5zsdp17etLNa9p0u1Xd9wt3ov5U5x3Brk03TwR/xKrUE/4ffq0wSLp8qbVUu
        X1SpOMSt1sau3csGF8EOF2/U/ZmfrTHAOe891vn34I3hRtiL4EfYxwFTHEY4
        wvEYVKKF9hhOiZMSp9/lIS6TDgIAAA==
        """,
        """
        androidx/compose/foundation/interaction/MutableInteractionSourceImpl.class:
        H4sIAAAAAAAA/61Sy04bMRQ91yGPTgMESiG0PLa0i06KugNVvIQ0UgpSi7Jh
        5cy4xWTGRjMexDLf0j/oColFFXXJR1VcTyp1wRIWPjrn3Ieur33/9+43gE/Y
        JBxJk+RWJzdhbLMrW6jwuy1NIp22JtTGqVzGFf9SOjlMVfTf+mbLPFZRdpU2
        QYTOpbyWYSrNj/B0eKli10SNsPfU/k3UCY1dbbT7TKhtvRu00UQrwAxeEGbc
        hS4Ix/3nuMYOYaE/si7VnKec5HLJnsiua7wu8tDyAAKN2L/RXvWYJR8Jm5Nx
        EIiuqM5k3BLdyXhb9Oig/udnQ3SET9smHDx5VB5p6ZH5YeR4G4c2UYT5vjbq
        pMyGKj/zHQiLfRvLdCBz7fU/M5hWHmsvVr+WxulMDXShObpvjHXVSAV6ELxs
        foTpzf32Gd+wCisN1N/fIvjFROAtY6MyA6wxtqcJeMnMx9crXMVG9QEJsxyb
        O0ctwnyEToQFLDLFqwhLeH0OKrCMFY4XaBfoFmg9AJ+La3K9AgAA
        """
    )

    override fun getDetector(): Detector = UnrememberedMutableInteractionSourceDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(
        UnrememberedMutableInteractionSourceDetector.UnrememberedMutableInteractionSource
    )

    @Test
    fun notRemembered() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.foundation.interaction.*
                import androidx.compose.runtime.*

                @Composable
                fun Test() {
                    val interactionSource = MutableInteractionSource()
                }

                val lambda = @Composable {
                    val interactionSource = MutableInteractionSource()
                }

                val lambda2: @Composable () -> Unit = {
                    val interactionSource = MutableInteractionSource()
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        val interactionSource = MutableInteractionSource()
                    })
                    LambdaParameter {
                        val interactionSource = MutableInteractionSource()
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        val interactionSource = MutableInteractionSource()
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val interactionSource = MutableInteractionSource()
                    }
                }

                @Composable
                fun Test4() {
                    val localObject = object {
                        val interactionSource = MutableInteractionSource()
                    }
                }
            """
            ),
            InteractionSourceStub,
            Stubs.Composable,
        )
            .run()
            .expect(
                """
src/test/{.kt:9: Error: Creating a MutableInteractionSource during composition without using remember [UnrememberedMutableInteractionSource]
                    val interactionSource = MutableInteractionSource()
                                            ~~~~~~~~~~~~~~~~~~~~~~~~
src/test/{.kt:13: Error: Creating a MutableInteractionSource during composition without using remember [UnrememberedMutableInteractionSource]
                    val interactionSource = MutableInteractionSource()
                                            ~~~~~~~~~~~~~~~~~~~~~~~~
src/test/{.kt:17: Error: Creating a MutableInteractionSource during composition without using remember [UnrememberedMutableInteractionSource]
                    val interactionSource = MutableInteractionSource()
                                            ~~~~~~~~~~~~~~~~~~~~~~~~
src/test/{.kt:26: Error: Creating a MutableInteractionSource during composition without using remember [UnrememberedMutableInteractionSource]
                        val interactionSource = MutableInteractionSource()
                                                ~~~~~~~~~~~~~~~~~~~~~~~~
src/test/{.kt:29: Error: Creating a MutableInteractionSource during composition without using remember [UnrememberedMutableInteractionSource]
                        val interactionSource = MutableInteractionSource()
                                                ~~~~~~~~~~~~~~~~~~~~~~~~
src/test/{.kt:35: Error: Creating a MutableInteractionSource during composition without using remember [UnrememberedMutableInteractionSource]
                        val interactionSource = MutableInteractionSource()
                                                ~~~~~~~~~~~~~~~~~~~~~~~~
src/test/{.kt:39: Error: Creating a MutableInteractionSource during composition without using remember [UnrememberedMutableInteractionSource]
                        val interactionSource = MutableInteractionSource()
                                                ~~~~~~~~~~~~~~~~~~~~~~~~
src/test/{.kt:46: Error: Creating a MutableInteractionSource during composition without using remember [UnrememberedMutableInteractionSource]
                        val interactionSource = MutableInteractionSource()
                                                ~~~~~~~~~~~~~~~~~~~~~~~~
8 errors, 0 warnings
            """
            )
    }

    @Test
    fun rememberedInsideComposableBody() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.foundation.interaction.*
                import androidx.compose.runtime.*

                @Composable
                fun Test() {
                    val interactionSource = remember { MutableInteractionSource() }
                }

                val lambda = @Composable {
                    val interactionSource = remember { MutableInteractionSource() }
                }

                val lambda2: @Composable () -> Unit = {
                    val interactionSource = remember { MutableInteractionSource() }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        val interactionSource = remember { MutableInteractionSource() }
                    })
                    LambdaParameter {
                        val interactionSource = remember { MutableInteractionSource() }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        val interactionSource = remember { MutableInteractionSource() }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val interactionSource = remember { MutableInteractionSource() }
                    }
                }
            """
            ),
            InteractionSourceStub,
            Stubs.Composable,
            Stubs.Remember
        )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.foundation.interaction.*
                import androidx.compose.runtime.*

                fun test() {
                    val interactionSource = MutableInteractionSource()
                }

                val lambda = {
                    val interactionSource = MutableInteractionSource()
                }

                val lambda2: () -> Unit = {
                    val interactionSource = MutableInteractionSource()
                }

                fun LambdaParameter(content: () -> Unit) {}

                fun test2() {
                    LambdaParameter(content = {
                        val interactionSource = MutableInteractionSource()
                    })
                    LambdaParameter {
                        val interactionSource = MutableInteractionSource()
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        val interactionSource = MutableInteractionSource()
                    }

                    val localLambda2: () -> Unit = {
                        val interactionSource = MutableInteractionSource()
                    }
                }

                fun test3() {
                    class Foo {
                        val interactionSource = MutableInteractionSource()
                    }

                    val localObject = object {
                        val interactionSource = MutableInteractionSource()
                    }
                }

                @Composable
                fun Test4() {
                    class Foo {
                        val interactionSource = MutableInteractionSource()
                    }
                }
            """
            ),
            InteractionSourceStub,
            Stubs.Composable
        )
            .run()
            .expectClean()
    }
}
