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

    private val InteractionSourceStub: TestFile =
        bytecodeStub(
            filename = "InteractionSource.kt",
            filepath = "androidx/compose/foundation/interaction",
            checksum = 0xac2a176d,
            source =
                """
        package androidx.compose.foundation.interaction

        interface InteractionSource

        interface MutableInteractionSource : InteractionSource

        fun MutableInteractionSource(): MutableInteractionSource = MutableInteractionSourceImpl()

        private class MutableInteractionSourceImpl : MutableInteractionSource
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgsuNST8xLKcrPTKnQS87PLcgvTtVL
        yy/NS0ksyczP08vMK0ktSkwGsYWEPRGc4PzSouRU7xIubi6WktTiEiHmeO8S
        JQYtBgD0QcfwZQAAAA==
        """,
            """
        androidx/compose/foundation/interaction/InteractionSource.class:
        H4sIAAAAAAAA/52OP0/DMBDF3znQtOFfCq1UvgRuKxbUiQUpUhESSCyZ3MRF
        bhIbxW7VsZ+LAXXmQyGcdGBg4yw9/+5Ouve+vj8+AdxiSLgTOq+Nyrc8M9W7
        sZIvzVrnwimjudJO1iJrOfnlF7OuMxmCCPFKbAQvhX7jT4uVzFyIgNCfF8aV
        SvNH6YS/JWYEVm0Cb0qN9BoBgQo/36qmG3vKJ4ThfteN2IhFLPa0HO13Uzam
        ZjklzOb/TusTeMPBn/lN4QjRgR9UKQnXz2vtVCVflVWLUt5rbVxrYDs+Bo5w
        KIarVi8x8P/EHz/2r5MiSBAm6CboIfKIkwSnOEtBFue4SMEsYov+D8H/p5CF
        AQAA
        """,
            """
        androidx/compose/foundation/interaction/InteractionSourceKt.class:
        H4sIAAAAAAAA/61R308TQRD+5gpXevwqAtoWRbAv8OJR4xvGqBCSi7UmakhM
        n7a9lWx7t0vu9hoe+yf5ZqIJ6bN/lHG2kPDQkJDAPsx8M/vNN7szf//9vgTw
        Gk3CodBxZlR8EfZNem5yGf4whY6FVUaHSluZif4URzf4qymyvvxoyyBCdSBG
        IkyEPgs/9wayz9kSofapsKKXyJkqwvHefvuuTW9TOSQ02yY7CwfS9jKhdB4K
        rY2dKuRhx9hOkSTMOr5vpyg9T8pYIPhvlFb2LaG0t3+6hACLASpYIry7b4sy
        Vghr7aGxiWKWtIKLBT/eS0cl3hM5U3EGBBo64PHlhXLogFHcIqxPxn4wGQde
        zWv41cm44R2Qu3pF+PAQ496YSb4cWsLckYl5p6ttpWWnSHsy++YUCFtfCm1V
        ylojlStOvb/ZDyG4kjhRjlq/pp7OENGChzlc/byOefgcb3PUZO/O4h9Uvv/C
        8gSrP6ek52x99sACdq5x2Q0Mu1P7DC/YtzhbZbm1LkoRHkVYj7CBzQiP8SRC
        DfUuKEcDW114OeZzPP0PDZ9NFzMDAAA=
        """,
            """
        androidx/compose/foundation/interaction/MutableInteractionSource.class:
        H4sIAAAAAAAA/6VQvU7DMBi8L4E2DX8tBCgvQdqKBXUBBqRIRUggsWRyExe5
        SW3UOFXHPhcD6sxDIb6EgYEOlRh8Pp/1ne/8+fX+AeAK54QbodO5UekyTMzs
        zRQynJhSp8Iqo0OlrZyLpOYPpRXjXEa/0rMp54lsggjtqViIMBf6NXwcT2Vi
        m3AJ19t6bzDdJXRGmbG54qelFTwlhgRntnA5OlXQqgAEylhfqurUY5b2CcF6
        5flO16mWN+muVwOnR9XdgHA3+m9lzjHc2mTTdPBHvMwswf/h9yqXhIunUls1
        ky+qUBziVmtja/eiwUWww8UbVX/mpzUGOOO9zzr/HrwYboRWBD/CHvaZ4iDC
        IY5iUIE2OjGcAscFTr4BtDhpnA4CAAA=
        """,
            """
        androidx/compose/foundation/interaction/MutableInteractionSourceImpl.class:
        H4sIAAAAAAAA/61Sy04bMRQ91yEPpqEEyiO0ULa0i06KugOhFhDSSGkrFZQN
        K2fGUJMZG814EMt8S/+AFRILFLHsR6FeT5C66JIufHTOuQ9dX/v34909gE/Y
        JBxKk+RWJ9dhbLNLW6jwzJYmkU5bE2rjVC7jin8tnRymKvprHdsyj1WUXaZN
        EKFzIa9kmEpzHn4fXqjYNVEjfH5u/ybqhMauNtrtEWpb7wZtNNEKMINZwoz7
        qQvCUf9/XGOHsNAfWZdqzlNOcrlkT2RXNV4XeZj1AAKN2L/WXvWYJR8Jm5Nx
        EIiuqM5k3BLdyXhb9Gi//vCrITrCp20T9p89Ko+09I/5YeR4Gwc2UYT5vjbq
        W5kNVX7iOxAW+zaW6UDm2usnM5hWHmkv1n6UxulMDXShOfrFGOuqkQr0IHjZ
        /AjTm/vtM75mFVYaqL+/RXDDROANY6MyA6wztqcJeMHMxzcqXMPb6gMS5jj2
        8hS1CPMROhEWsMgUryIsYfkUVGAFqxwv0C7QLdD6A/UGq929AgAA
        """
        )

    override fun getDetector(): Detector = UnrememberedMutableInteractionSourceDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            UnrememberedMutableInteractionSourceDetector.UnrememberedMutableInteractionSource
        )

    @Test
    fun notRemembered() {
        lint()
            .files(
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
        lint()
            .files(
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
        lint()
            .files(
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
