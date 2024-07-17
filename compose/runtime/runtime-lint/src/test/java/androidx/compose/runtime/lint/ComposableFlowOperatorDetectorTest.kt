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

/** Test for [ComposableFlowOperatorDetector]. */
class ComposableFlowOperatorDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableFlowOperatorDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposableFlowOperatorDetector.FlowOperatorInvokedInComposition)

    /** Combined stub of some Flow APIs */
    private val flowStub: TestFile =
        bytecodeStub(
            filename = "Flow.kt",
            filepath = "kotlinx/coroutines/flow",
            checksum = 0xab106046,
            """
        package kotlinx.coroutines.flow

        interface Flow<out T>

        inline fun <T, R> Flow<T>.map(crossinline transform: suspend (value: T) -> R): Flow<R> {
            return object : Flow<R> {}
        }

        fun <T> Flow<T>.drop(count: Int): Flow<T> = this
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgUuOSScxLKcrPTKnQS87PLcgvTtUr
        Ks0rycxN1UvLzxdiC0ktLvEu4VLkEs/OL8nJzAMpK8ovLcnMSy3WS8vJLxdi
        cwOS3iVKDFoMAPfFl7BjAAAA
        """,
            """
        kotlinx/coroutines/flow/Flow.class:
        H4sIAAAAAAAA/31QPUsDQRSct9Fccn5d/Iwgop1YeDFYqQg2gYOIYIJNqk2y
        CWsue5Ddiynvd1nI1f4o8V3SKbjFzJvZgXm7X98fnwBuUCecTBIXa7MIB8ks
        SZ02yoajOHkPWwweiHBx371tv8m5DGNpxuFz/00N3N3DX4sQ/PY8rBFq7VVH
        +KScHEonOSmm8xKvQAVUCwCBJuwvdKEaPA2vCed55vuiLnxmEeRZZVTPs8ty
        Jc8COqOmaIgi2CSctv97BxdSl4oOr5BXE0eodvTYSJfOFMHvJOlsoFo6ZnH8
        khqnp+pVW92P1aMxiZNOJ8aWuQzrWJ0SDhgF8+GS93G0/FNCmTNeD6UIlQjV
        CD42eMRmhC1s90AWOwj43qJmsWux9wOUHf5akAEAAA==
        """,
            """
        kotlinx/coroutines/flow/FlowKt＄map＄1.class:
        H4sIAAAAAAAA/7VSW28SQRT+ZqEFRuwFq7a2tmixtrS6bqOJkaZJU0tCStWU
        pi8kJgNs6cAya3ZnkUd+kj6Z+GB49kcZzy6QGDUbX3yYb87tO5eZ8/3H128A
        nuMJQ6Hrakeqgdl0PTfQUtm+eeW4H80ywaku9MSHgpUCY3hR7Yi+MB2h2ubb
        Rsdu6lI1jnxwcV46LDEs/E5LIcmwFkdNYZZh9kAqqQ8ZEts7l1mkkeGYAWdI
        6mvpM2zFVp+2Tg0sTgLNM1uLltCCbEavn6AnYCFkQgAD65J9IEPtE0kti2F9
        NMzw0ZAby0aRjYZpvjwa7qdzyZzxcjR8xsKofYqK7YSqrcd3msI9mpLaZXi/
        HZ9rOkqn3zOvAtXU0lW+WZ5I+6WdeHoWa7jPkAqVp13NUPw1oVTa9pRwzJob
        eE37td0I2icDbSufctMYM33hBDaDqJ0dveOTHPw0SsCLtfxUKvPdvJWf+v/l
        l3ixyq1Na8+yXpF8En7ysduiUvNVIrwJeg3buxANhyy5qtsUzqXwZKhPjNmK
        UrZ37Ajft2k15k9U03F9qdr05dduiyFTk20ldOBRMB+PV5Yhc+lvszKsnAdK
        y559KX1JFY6UcrWIHpthdeKrqP4fXlgwaEvpiccrFa4t4SZpZqQDM8UvuPGZ
        BAMFwtnImMIjwuw4gO6bkW2LkJPNCBcUK3hMmMQq1rEd8RPYie4NFOn+/9+C
        XSpjUUNz1OR8HYkKFipYrCCHWyRiqYLbuFMH83EXy3UYfiiuYI9oc0TL03kQ
        uR/+BILNGH6EBAAA
        """,
            """
        kotlinx/coroutines/flow/FlowKt.class:
        H4sIAAAAAAAA/41T32/bVBT+rpM6jpu2jteVJhulbIalLZ3TbvxasoxRqapF
        KaiNCqgScJu6nVvHRr5O2GOf+EP4C3gDMQlFe+SV/wdxruOEbC0JDz6/7nfO
        +c69x3/+/fsfAB5ii2HpIox9L3hut8Io7MRe4Ar71A9/tLdJfBbnwBiMc97l
        ts+DM/uL43O3RdEMQ6bNf2D4trI7rkItPbXPu237tBO0Yi8MhL2dWpu1lfHp
        DH/Vm492XydQ278m1hhPpb7WbNYakwjV1wm1PgCN1NkKAzI6XKIItE+l1q7h
        MGGgepLIcHc3jM7sczc+jrhH/XkQhDHvc9kL472O7xNKrcfPPNHQkB8+VMLb
        C2I3CrhvO0EcUbrXEjlMM9xsPXNbF2n+lzzibZeADPcqV5mORA5kkbPaymEB
        M5jVUcAcQz6OeCBOw6itochgjd8Ti5bB2sjhhiTtBV7coAWpyIo3saBjHm8w
        3B5XIocSg2Z51qmV7BVziIIlx+/7S5P2ZHnSpjFkT6KQSj2ZsLLOxJ38/tqd
        /H/7N6F6PQEx6P3Z+4ynWmEniBmKgxk/d2N+wmNOQKXdzdDPzKTISwG6vAtp
        KHT43JNWlayTDYYXvcuK3rvUlUVFVzT6DI10pu8PYlIbBOpdli3SZc3MmsqO
        UmV3slrv0lBWh4FN1ciUlWq2/J0xlcLUIYwUM9QR8AMta+TK6xozb5jFHeXl
        z2pBy5uapptZTatMm7o5KFEwVTO7yKozVW3n5U8aIRVjVo6wSaM1WTLhPoM5
        uI3RN14YBIfrv0dKPn5Amq6yy/2OO7Iu//GLU0ZOvsf9C7r37FZ4QjlzuwTb
        67SP3ajJj31XUghb3D/kkSf9NJg/8M4CHncism/t08N5bdcJup7w6Pjpvz86
        /VSvnw5JvwIrOEHgRls+F8IlVz8IO1HL3fZks1Ja4vBKeWxAQVYuBOkSpqCS
        94i8r8iXW1FaNfXfYKyZJsnM4xeY/+ZXLPZQ/iVJqZFU6apnkUOd7GVKmsU0
        buE2nVI63sRSUr6EIt4i5OMkL4dGmqmRfkLfjJI6fVmiUm+nXOqUIItpfS6r
        r7ZWoSetF/oY3E0barCIxKDhnSsN84OGIM6fkNTJn0+5Pk2SPsanpL+m+Dt0
        Ne8eIePgnoOKgxWsOljDew7Wcf8ITMBG9QhzAssCS3StApbApsCUwAOBosBD
        gfcFPhD4UOCj5Ej9ByMF+2JjBwAA
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

                val emptyFlow: Flow<Unit> = object : Flow<Unit> {}

                fun <T> Flow<T>.customOperator(param: Boolean): Flow<T> = this

                @Composable
                fun Test() {
                    emptyFlow
                        .map { true }
                        .customOperator(true)
                        .drop(0)
                }

                val lambda = @Composable {
                    emptyFlow
                        .map { true }
                        .customOperator(true)
                        .drop(0)
                }

                val lambda2: @Composable () -> Unit = {
                    emptyFlow
                        .map { true }
                        .customOperator(true)
                        .drop(0)
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    })
                    LambdaParameter {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    }

                    val localLambda2: @Composable () -> Unit = {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    }
                }
            """
                ),
                Stubs.Composable,
                flowStub
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
                    src/androidx/compose/runtime/foo/test.kt:14: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .map { true }
                         ~~~
src/androidx/compose/runtime/foo/test.kt:15: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .customOperator(true)
                         ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:16: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .drop(0)
                         ~~~~
src/androidx/compose/runtime/foo/test.kt:21: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .map { true }
                         ~~~
src/androidx/compose/runtime/foo/test.kt:22: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .customOperator(true)
                         ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:23: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .drop(0)
                         ~~~~
src/androidx/compose/runtime/foo/test.kt:28: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .map { true }
                         ~~~
src/androidx/compose/runtime/foo/test.kt:29: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .customOperator(true)
                         ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:30: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                        .drop(0)
                         ~~~~
src/androidx/compose/runtime/foo/test.kt:40: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .map { true }
                             ~~~
src/androidx/compose/runtime/foo/test.kt:41: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .customOperator(true)
                             ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:42: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .drop(0)
                             ~~~~
src/androidx/compose/runtime/foo/test.kt:46: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .map { true }
                             ~~~
src/androidx/compose/runtime/foo/test.kt:47: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .customOperator(true)
                             ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:48: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .drop(0)
                             ~~~~
src/androidx/compose/runtime/foo/test.kt:55: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .map { true }
                             ~~~
src/androidx/compose/runtime/foo/test.kt:56: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .customOperator(true)
                             ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:57: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .drop(0)
                             ~~~~
src/androidx/compose/runtime/foo/test.kt:62: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .map { true }
                             ~~~
src/androidx/compose/runtime/foo/test.kt:63: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .customOperator(true)
                             ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/test.kt:64: Error: Flow operator functions should not be invoked within composition [FlowOperatorInvokedInComposition]
                            .drop(0)
                             ~~~~
21 errors, 0 warnings
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

                val emptyFlow: Flow<Unit> = object : Flow<Unit> {}

                fun <T> Flow<T>.customOperator(param: Boolean): Flow<T> = this

                @Composable
                fun <T> Flow<T>.customComposableOperator(param: Boolean): Flow<T> = this

                fun test() {
                    emptyFlow
                        .map { true }
                        .customOperator(true)
                        .drop(0)
                }

                val lambda = {
                    emptyFlow
                        .map { true }
                        .customOperator(true)
                        .drop(0)
                }

                val lambda2: () -> Unit = {
                    emptyFlow
                        .map { true }
                        .customOperator(true)
                        .drop(0)
                }

                fun lambdaParameter(action: () -> Unit) {}

                fun test2() {
                    lambdaParameter(action = {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    })
                    lambdaParameter {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    }

                    val localLambda2: () -> Unit = {
                        emptyFlow
                            .map { true }
                            .customOperator(true)
                            .drop(0)
                    }
                }

                @Composable
                fun test4() {
                    emptyFlow
                        .customComposableOperator(true)
                }
            """
                ),
                Stubs.Composable,
                flowStub
            )
            .run()
            .expectClean()
    }
}
