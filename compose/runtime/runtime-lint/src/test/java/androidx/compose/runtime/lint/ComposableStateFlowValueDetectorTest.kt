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

package androidx.compose.runtime.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [ComposableStateFlowValueDetector]. */
class ComposableStateFlowValueDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableStateFlowValueDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposableStateFlowValueDetector.StateFlowValueCalledInComposition)

    /** Combined stub of StateFlow / supertypes */
    private val stateFlowStub: TestFile =
        bytecodeStub(
            filename = "StateFlow.kt",
            filepath = "kotlinx/coroutines/flow",
            checksum = 0xd479b246,
            """
        package kotlinx.coroutines.flow

        interface Flow<out T>

        interface SharedFlow<out T> : Flow<T>

        interface StateFlow<out T> : SharedFlow<T> {
            val value: T
        }
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg0uKSScxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxN1UvLzxfiCkktLnHLyS/3LlFi0GIAAH26nstEAAAA
        """,
            """
        kotlinx/coroutines/flow/Flow.class:
        H4sIAAAAAAAA/31QPU8CQRSctygfJ+rhJybGaGcsPCRWakxsSC7BmAixoVpg
        IQvHXsLuIeX9LgtztT/K+A46Tdxi5r3Z2cy+9/X98QngFnXC6TR2kTbLYBDP
        48Rpo2wwiuL3oMVQAhEuH7p37YlcyCCSZhy89Cdq4O4f/0oE/7dWwgah1l5n
        BM/KyaF0kp1itijwFyiHSg4g0JT1pc67BlfDG8JFlnqeqAuPWfhZWh7Vs/Sq
        WM5Sn86pKRoiNzYJZ+3/5uBA6lKeUe046VSuXU8dodLRYyNdMlcErxMn84Fq
        6Yibk9fEOD1Tb9rqfqSejIn5oY6NLXIiNrE+BRwyCuajFR/geLVYQpE9pR4K
        IcohKiE8bHGJaoht7PRAFrvw+d6iZrFnsf8DtOaVdpUBAAA=
        """,
            """
        kotlinx/coroutines/flow/SharedFlow.class:
        H4sIAAAAAAAA/31RTUtCQRQ992k+fVmpfWlERESLFj2TVilCG0kygpQ2rkYd
        bfQ5D94bzaW/q0W47kdF90kQFLq5554795zhzHx+vX8AuMER4WzkG0/pmdv1
        A39ilJah2/f8N7f5KgLZq3FrgwgPldZtYyimwvWEHrhPnaHsmnL1/6ixyjCy
        qrRa5WqZkPkrsxEnHK+T2kgQsj/u7qM0oieMYC9rPI1xGopKKiog0IjnMxWx
        Ine9a8LFYu44Vt5yGBmS/fxifplILuYZOqVSMhfPWfdUtKLtEuF8ZYzfd+G7
        qUU4WZuYl9JNI4yMyNXIEFJNNdDCTAJJcJr+JOjKmvKYFJ4n2qixfFGh6njy
        TmufhcrXISe3sMG57CgeYjjkajHml3iAwvI7CUneSrURq8OpY7OONLa4xXYd
        O8i0QSGyyPF5iN0QeyH2vwGCO5LBCwIAAA==
        """,
            """
        kotlinx/coroutines/flow/StateFlow.class:
        H4sIAAAAAAAA/41RTU/bQBB9s3YcJ6VgAqUh/VALHIADpqiHilAkLqiRqBAk
        Qkg5bZIlLDG2lF2nHP1bOPAjeqgsjv1RVceU9lCE2svMvNl9b2fefv/x9RuA
        93hNeDtKbKTjq7CfjJPU6liZ8CxKvoRtK63a56oMIhztdLYPLuREhpGMh+Fh
        70L1bXP3YevgUb1zOVaDQnCn02nuNgnB3+QyXMLSvwXK8Aj+UNkTGaWKML+6
        9nAQQml1jV8izN7PFH5WVg6kldwTlxOHLaAiVIoAAo24f6ULtMnV4B1hL8+m
        q6Iuqnl2l4Tv+Gf1PFv3/DwL6A1t+TW3Jj7RpjiuBU5DfMiz09sb9/ba8xqu
        7walQmiLsPy4L7995qmoQ1j5DweL5Sa/dp/6w98YWUKlrYextOmYj6rtJB33
        1b6OGCwep7HVl+pEG92L1F4cJ0zUSWzYS4ES718uXOAf8FFh9IKRQBXOfeXg
        5V1u4BXnj3zjCbOmunBaeNrCdAszCLjEbAs1zHVBBvN41oVnsGDw3KBusGjg
        G1R+AgX9oFiBAgAA
        """
        )

    @Test
    fun errors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import kotlinx.coroutines.flow.*

                val stateFlow: StateFlow<Boolean> = object : StateFlow<Boolean> {
                    override val value = true
                }

                class TestFlow : StateFlow<Boolean> {
                    override val value = true
                }

                val testFlow = TestFlow()

                @Composable
                fun Test() {
                    stateFlow.value
                    testFlow.value
                }

                val lambda = @Composable {
                    stateFlow.value
                    testFlow.value
                }

                val lambda2: @Composable () -> Unit = {
                    stateFlow.value
                    testFlow.value
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        stateFlow.value
                        testFlow.value
                    })
                    LambdaParameter {
                        stateFlow.value
                        testFlow.value
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        stateFlow.value
                        testFlow.value
                    }

                    val localLambda2: @Composable () -> Unit = {
                        stateFlow.value
                        testFlow.value
                    }
                }
            """
                ),
                Stubs.Composable,
                stateFlowStub
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
                    src/androidx/compose/runtime/foo/TestFlow.kt:19: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                    stateFlow.value
                              ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:20: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                    testFlow.value
                             ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:24: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                    stateFlow.value
                              ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:25: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                    testFlow.value
                             ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:29: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                    stateFlow.value
                              ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:30: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                    testFlow.value
                             ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:39: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        stateFlow.value
                                  ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:40: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        testFlow.value
                                 ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:43: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        stateFlow.value
                                  ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:44: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        testFlow.value
                                 ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:50: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        stateFlow.value
                                  ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:51: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        testFlow.value
                                 ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:55: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        stateFlow.value
                                  ~~~~~
src/androidx/compose/runtime/foo/TestFlow.kt:56: Error: StateFlow.value should not be called within composition [StateFlowValueCalledInComposition]
                        testFlow.value
                                 ~~~~~
14 errors, 0 warnings
            """
            )
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import kotlinx.coroutines.flow.*

                val stateFlow: StateFlow<Boolean> = object : StateFlow<Boolean> {
                    override val value = true
                }

                class TestFlow : StateFlow<Boolean> {
                    override val value = true
                }

                val testFlow = TestFlow()

                fun test() {
                    stateFlow.value
                    testFlow.value
                }

                val lambda = {
                    stateFlow.value
                    testFlow.value
                }

                val lambda2: () -> Unit = {
                    stateFlow.value
                    testFlow.value
                }

                fun lambdaParameter(action: () -> Unit) {}

                fun test2() {
                    lambdaParameter(action = {
                        stateFlow.value
                        testFlow.value
                    })
                    lambdaParameter {
                        stateFlow.value
                        testFlow.value
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        stateFlow.value
                        testFlow.value
                    }

                    val localLambda2: () -> Unit = {
                        stateFlow.value
                        testFlow.value
                    }
                }
            """
                ),
                Stubs.Composable,
                stateFlowStub
            )
            .run()
            .expectClean()
    }
}
