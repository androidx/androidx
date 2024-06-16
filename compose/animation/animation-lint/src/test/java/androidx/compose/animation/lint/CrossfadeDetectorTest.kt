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

package androidx.compose.animation.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [CrossfadeDetector]. */
class CrossfadeDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = CrossfadeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(CrossfadeDetector.UnusedCrossfadeTargetStateParameter)

    // Simplified Transition.kt stubs
    private val CrossfadeStub =
        bytecodeStub(
            filename = "Transition.kt",
            filepath = "androidx/compose/animation",
            checksum = 0x15a87088,
            """
            package androidx.compose.animation

            import androidx.compose.runtime.Composable

            @Composable
            fun <T> Crossfade(
                targetState: T,
                content: @Composable (T) -> Unit
            ) {}
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uKSSsxLKcrPTKnQS87PLcgvTtVL
        zMvMTSzJzM8T4gkpSswrzgSxvUu4eLmY0/LzhdhCUotLvEuUGLQYAEjDUx5T
        AAAA
        """,
            """
        androidx/compose/animation/TransitionKt.class:
        H4sIAAAAAAAA/4VSW08TQRT+Zlq67QKylHvxglwERNxC8MUSE9OE0FjB2MoL
        T9PtUqeXWbM7bXjsb/Ef+GZ8MI2P/ijjmW0VBBMe5syZ73znmzPnzM9f374D
        OMAuw6ZQ9TCQ9UvXCzqfgsh3hZIdoWWg3GooVCSN+0ZbYAxOU/SE2xaq4Z7W
        mr5HaIIhUwyDKLoQdZ/hxVb5JqdQbgW6LZXb7HXci67yjGDkHo28vcL2GcPp
        YfXl7cxXW9XqXemHu9c4H5SkrFhxo3zrZWFXadnx3WJ8FrW2X2BYKwdhw236
        uhYKScJCqUCL4SUngT7pttvEsrxAaV/pNGyGh9cqkgSHSrTdktIh5UsvsjDB
        MOd99L3WSOCdCEXHJyL1+38NukIqRqRBD5jAPUzZmITDMK5F2PB1hcqiFmdv
        CzCs3NVkhuk/lLe+FnWhBWG800vQT2DGZIwBA2sZh1PwUhovT159j6Ey6M/a
        g77NHW7zdMLmizxeg37uwCHD82w1mR70Hb6fchI5fsz3F5xkbiabzJJvbJ7l
        x358TvF06tgyu2MZ6X0W31o1LxtVeL3syatf+LylGZLFwHy0qbJU/km3U/PD
        qpmkyQ480T4ToTTnEZipyIYSuhuSv/x+OP+S6slIUvj11agZ1m9G/w7tH5pd
        Cbqh5x9Jo740yjm7pYc9cCQx7OgSxpCi0wadCoRz2q2d7PhXTH8xrcYTsiki
        pmBjk/z5IQVZzMQSFuGzFN+K2Ra2R/w07U/N6Hh8TyYO78R2Hc9oLxI6R7fP
        nyNRwkIJiyWqJlfCMu6X8AAPz8EiPMLKOdIRxiI8jrAaIRvBjrAWGTD1G1Xc
        nh8uBAAA
        """
        )

    @Test
    fun unreferencedParameters() {
        lint()
            .files(
                kotlin(
                    """
                package foo

                import androidx.compose.animation.*
                import androidx.compose.runtime.*

                val foo = false

                @Composable
                fun Test() {
                    Crossfade(foo) { if (foo) { /**/ } else { /**/ } }
                    Crossfade(foo, content = { if (foo) { /**/ } else { /**/ } })
                    Crossfade(foo) { param -> if (foo) { /**/ } else { /**/ } }
                    Crossfade(foo, content = { param -> if (foo) { /**/ } else { /**/ } })
                    Crossfade(foo) { _ -> if (foo) { /**/ } else { /**/ } }
                    Crossfade(foo, content = { _ -> if (foo) { /**/ } else { /**/ } })
                }
            """
                ),
                CrossfadeStub,
                Stubs.Composable
            )
            .run()
            .expect(
                """
src/foo/test.kt:11: Error: Target state parameter it is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo) { if (foo) { /**/ } else { /**/ } }
                                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:12: Error: Target state parameter it is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo, content = { if (foo) { /**/ } else { /**/ } })
                                             ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:13: Error: Target state parameter param is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo) { param -> if (foo) { /**/ } else { /**/ } }
                                     ~~~~~
src/foo/test.kt:14: Error: Target state parameter param is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo, content = { param -> if (foo) { /**/ } else { /**/ } })
                                               ~~~~~
src/foo/test.kt:15: Error: Target state parameter _ is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo) { _ -> if (foo) { /**/ } else { /**/ } }
                                     ~
src/foo/test.kt:16: Error: Target state parameter _ is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo, content = { _ -> if (foo) { /**/ } else { /**/ } })
                                               ~
6 errors, 0 warnings
            """
            )
    }

    @Test
    fun unreferencedParameter_shadowedNames() {
        lint()
            .files(
                kotlin(
                    """
                package foo

                import androidx.compose.animation.*
                import androidx.compose.runtime.*

                val foo = false

                @Composable
                fun Test() {
                    Crossfade(foo) {
                        foo.let {
                            // These `it`s refer to the `let`, not the `Crossfade`, so we
                            // should still report an error
                            it.let {
                                if (it) { /**/ } else { /**/ }
                            }
                        }
                    }
                    Crossfade(foo) { param ->
                        foo.let { param ->
                            // This `param` refers to the `let`, not the `Crossfade`, so we
                            // should still report an error
                            if (param) { /**/ } else { /**/ }
                        }
                    }
                }
            """
                ),
                CrossfadeStub,
                Stubs.Composable
            )
            .run()
            .expect(
                """
src/foo/test.kt:11: Error: Target state parameter it is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo) {
                                   ^
src/foo/test.kt:20: Error: Target state parameter param is not used [UnusedCrossfadeTargetStateParameter]
                    Crossfade(foo) { param ->
                                     ~~~~~
2 errors, 0 warnings
            """
            )
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
            package foo

            import androidx.compose.animation.*
            import androidx.compose.runtime.*

            val foo = false

            @Composable
            fun Test() {
                Crossfade(foo) { if (it) { /**/ } else { /**/ } }
                Crossfade(foo, content = { if (it) { /**/ } else { /**/ } })
                Crossfade(foo) { param -> if (param) { /**/ } else { /**/ } }
                Crossfade(foo, content = { param -> if (param) { /**/ } else { /**/ } })

                val content : @Composable (Boolean) -> Unit = {}
                Crossfade(foo, content = content)

                Crossfade(foo) { param ->
                    foo.let {
                        it.let {
                            if (param && it) { /**/ } else { /**/ }
                        }
                    }
                }

                Crossfade(foo) {
                    foo.let { param ->
                        it.let { param ->
                            if (param && it) { /**/ } else { /**/ }
                        }
                    }
                }

                Crossfade(foo) {
                    foo.run {
                        run {
                            if (this && it) { /**/ } else { /**/ }
                        }
                    }
                }

                fun multipleParameterLambda(lambda: (Boolean, Boolean) -> Unit) {}

                Crossfade(foo) {
                    multipleParameterLambda { _, _ ->
                        multipleParameterLambda { param1, _ ->
                            if (param1 && it) { /**/ } else { /**/ }
                        }
                    }
                }
            }
        """
                ),
                CrossfadeStub,
                Stubs.Composable
            )
            .run()
            .expectClean()
    }
}
