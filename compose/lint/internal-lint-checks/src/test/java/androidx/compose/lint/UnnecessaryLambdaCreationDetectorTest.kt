/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.kotlinAndBytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UnnecessaryLambdaCreationDetectorTest(
    @Suppress("unused") private val parameterizedDebugString: String,
    private val stub: TestFile
) : LintDetectorTest() {
    companion object {
        private val stub =
            kotlinAndBytecodeStub(
                filename = "Stub.kt",
                filepath = "test",
                checksum = 0x8a5a4526,
                source =
                    """
                package test

                import androidx.compose.runtime.Composable

                fun function() {}

                @Composable
                fun ComposableFunction(content: @Composable () -> Unit) {
                    content()
                }

                @Composable
                inline fun InlineComposableFunction(content: @Composable () -> Unit) {
                    content()
                }

                @Composable
                inline fun <reified T> ReifiedComposableFunction(content: @Composable () -> Unit) {
                    content()
                }
            """,
                """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgEuNiKUktLhFiCy4pTfIG0iFAnneJ
            EoMWAwBxHEvpMAAAAA==
            """,
                """
            test/StubKt.class:
            H4sIAAAAAAAA/51UXU8TQRQ9s4XuUoosFZQWxa8iBT+21G8hJoaEsLGiAcQH
            nqbbBYe2s2Z32vhIfPE3+GTiP/BNfTAE3/xRxjtbiiBV1CZ7Z+bec+bcO3em
            375//gLgJu4x9Cs/Us6KalYeKROMwd7iLe7Uudx0nlS2fI+8CQZroyk9JQLJ
            kChMrTFk5oPGyyDilbq/sB+6VCjXAlUX0tlqNZwOJXI6iOKsppaOQ8114s+k
            ULMPYtJEmctqGIjqK8eLlX0nbEolGr7zM5NZSqEchJvOlq8qIRe0KZcyULwt
            sBSopWa9TijTC6TypbKQZhg/kI0gdyh53XGlCokvvMjECYYR74Xv1fY2eMpD
            3vAJyDBZKP96XrMHPCt6k00qIA0bQykMInNYr0v1JoYZkkK2gprPMFyYOqqQ
            ximc7scIRhnOH3fkDKOuJIDfrWNn8yK/kf99nLkM2WVfbAi/2i2+OLd6/2h+
            D/6nw+NxKn/QGuqQHvuKV7niVJrRaCXoKjNt+rQB5VzTE4OCr4SeFWlWnWF4
            t7M9nNrZThm2EQ+jRvuzenJj9s52ziiy0pBt5AYyPRmaFxO775MUXDRz5+ze
            30Z331hfPzKiT9pJDbqYtHa2bXP0GHTStjR697Vhpnqt3belItNplpiuINOp
            9GAX2SrdW/1Qr9cUQ898UKXrMVimxi01GxU/XNWnpbmBx+trPBR6vefsWxGb
            kqtmSPOx5fa7cWVLRILCD38+EYb8r9H9y34IlloJmqHnLwi9e3aPs3ZkP8zA
            QI9uC9ksepGk6mZolUX7xz7ELSuR1SFtszBhEVzD5oiuvSemMwMfcXL6E7IM
            zzXHiDkpGpNkB9CPG7ROt9E05uI/OI3rw6293VM03qbPpMMkgs5ibF9qkaAG
            jXZbKjHXVcyiJzxIb1mLnYrXFs7gbCxr/6WsncU40RL/JDtMsiOHZM93lT13
            SNbAndgWcZfGBfJeoCZcXEfCxSUXeRcTuOxiEgUXU5heB4twBVfX0RfBjHAt
            QjrC9QipCGMRxiM4EXp/AGJveMs/BgAA
            """
            )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): Array<Any> =
            arrayOf(arrayOf("Source stubs", stub.kotlin), arrayOf("Compiled stubs", stub.bytecode))
    }

    override fun getDetector(): Detector = UnnecessaryLambdaCreationDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(UnnecessaryLambdaCreationDetector.ISSUE)

    private fun check(@Language("kotlin") code: String): TestLintResult {
        return lint()
            .files(kotlin(code.trimIndent()), stub, Stubs.Composable)
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
    }

    @Test
    fun warnsForSingleExpressions() {
        check(
                """
            package test

            import androidx.compose.runtime.Composable

            val lambda = @Composable { }
            val anonymousFunction = @Composable fun() {}
            val lambdaWithReceiver = @Composable { number: Int -> }
            val anonymousFunctionWithReceiver = @Composable fun(number: Int) {}

            @Composable
            fun Test() {
                ComposableFunction {
                    lambda()
                }

                InlineComposableFunction {
                    lambda()
                }

                ReifiedComposableFunction<Any> {
                    lambda()
                }

                ComposableFunction {
                    anonymousFunction()
                }

                ComposableFunction {
                    lambdaWithReceiver(10)
                }

                ComposableFunction {
                    anonymousFunctionWithReceiver(10)
                }

                ComposableFunction {
                    function()
                }
            }
        """
            )
            .expect(
                """
src/test/test.kt:13: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        lambda()
        ~~~~~~
src/test/test.kt:17: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        lambda()
        ~~~~~~
src/test/test.kt:21: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        lambda()
        ~~~~~~
src/test/test.kt:25: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        anonymousFunction()
        ~~~~~~~~~~~~~~~~~
4 errors, 0 warnings
        """
            )
    }

    @Test
    fun warnsForMultipleLambdas() {
        check(
                """
            package test

            import androidx.compose.runtime.Composable

            val lambda = @Composable { }

            @Composable
            fun MultipleChildComposableFunction(
                firstChild: @Composable () -> Unit,
                secondChild: @Composable () -> Unit
            ) {}

            @Composable
            fun Test() {
                MultipleChildComposableFunction( { lambda() }) {
                    lambda()
                }
            }
        """
            )
            .expect(
                """
src/test/test.kt:15: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
    MultipleChildComposableFunction( { lambda() }) {
                                       ~~~~~~
src/test/test.kt:16: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        lambda()
        ~~~~~~
2 errors, 0 warnings
        """
            )
    }

    @Test
    fun ignoresMultipleExpressions() {
        check(
                """
            package test

            import androidx.compose.runtime.Composable

            val lambda = @Composable { }

            @Composable
            fun Test() {
                ComposableFunction {
                    lambda()
                    lambda()
                }
            }
        """
            )
            .expectClean()
    }

    @Test
    fun ignoresPropertyAssignment() {
        check(
                """
            package test

            import androidx.compose.runtime.Composable

            val lambda = @Composable { }

            val property: @Composable () -> Unit = {
                lambda()
            }
        """
            )
            .expectClean()
    }

    @Test
    fun ignoresDifferentFunctionalTypes_parameters() {
        check(
                """
            package test

            import androidx.compose.runtime.Composable

            val lambda = @Composable { }

            @Composable
            fun ComposableFunctionWithParams(
                child: @Composable (child: @Composable () -> Unit) -> Unit
            ) {}

            @Composable
            fun Test() {
                ComposableFunctionWithParams { child ->
                    lambda()
                }
            }

            val parameterizedLambda: (@Composable () -> Unit) -> Unit = { it() }
            val differentlyParameterizedLambda: (Int) -> Unit = { }

            @Composable
            fun Test1() {
                ComposableFunctionWithParams { child ->
                    parameterizedLambda(child)
                }
            }

            @Composable
            fun Test2() {
                ComposableFunctionWithParams { child ->
                    differentlyParameterizedLambda(5)
                }
            }
        """
            )
            .expectClean()
    }

    @Test
    fun ignoresDifferentFunctionalTypes_receiverScopes() {
        check(
                """
            package test

            import androidx.compose.runtime.Composable

            class SomeScope
            class OtherScope

            @Composable
            fun ScopedComposableFunction(content: @Composable SomeScope.() -> Unit) {
                SomeScope().content()
            }

            @Composable
            fun Test() {
                val unscopedLambda: () -> Unit = {}
                val scopedLambda: @Composable SomeScope.() -> Unit = {}
                val differentlyScopedLambda: @Composable OtherScope.() -> Unit = {}

                ScopedComposableFunction {
                    unscopedLambda()
                }

                ScopedComposableFunction {
                    scopedLambda()
                }

                ScopedComposableFunction {
                    OtherScope().differentlyScopedLambda()
                }
            }
        """
            )
            .expect(
                """
src/test/SomeScope.kt:24: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        scopedLambda()
        ~~~~~~~~~~~~
1 errors, 0 warnings
        """
            )
    }

    @Test
    fun ignoresMismatchedComposability() {
        check(
                """
            package test

            import androidx.compose.runtime.Composable

            fun uncomposableLambdaFunction(child: () -> Unit) {}

            val lambda = @Composable { }
            val uncomposableLambda = {}

            @Composable
            fun Test() {
                uncomposableLambdaFunction {
                    lambda()
                }

                ComposableFunction {
                    uncomposableLambda()
                }
            }
        """
            )
            .expectClean()
    }

    @Test
    fun warnsForFunctionsReturningALambda() {
        check(
                """
            package test

            import androidx.compose.runtime.Composable

            fun returnsLambda(): () -> Unit = {}
            fun returnsComposableLambda(): @Composable () -> Unit = {}

            @Composable
            fun Test() {
                ComposableFunction {
                    returnsLambda()()
                }

                InlineComposableFunction {
                    returnsLambda()()
                }

                ReifiedComposableFunction<Any> {
                    returnsLambda()()
                }

                ComposableFunction {
                    returnsComposableLambda()()
                }

                InlineComposableFunction {
                    returnsComposableLambda()()
                }

                ReifiedComposableFunction<Any> {
                    returnsComposableLambda()()
                }
            }
        """
            )
            .expect(
                """
src/test/test.kt:23: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        returnsComposableLambda()()
                                 ~
src/test/test.kt:27: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        returnsComposableLambda()()
                                 ~
src/test/test.kt:31: Error: Creating an unnecessary lambda to emit a captured lambda [UnnecessaryLambdaCreation]
        returnsComposableLambda()()
                                 ~
3 errors, 0 warnings
        """
            )
    }
}
