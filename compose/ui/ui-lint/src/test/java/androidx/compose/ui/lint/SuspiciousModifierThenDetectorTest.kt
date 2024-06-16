/*
 * Copyright 2024 The Android Open Source Project
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
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Test for [SuspiciousModifierThenDetector]. */
@RunWith(JUnit4::class)
class SuspiciousModifierThenDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = SuspiciousModifierThenDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(SuspiciousModifierThenDetector.SuspiciousModifierThen)

    @Test
    fun clean() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                fun Modifier.test(): Modifier = this.then(TestModifier)

                fun Modifier.test2() = this.then(Modifier.test())

                fun Modifier.test3() = this.then(with(Modifier) { test() })

                fun Modifier.test4() = this.then(if (true) TestModifier else TestModifier)
                fun Modifier.test5() = this.then(if (true) Modifier.test() else Modifier.test())

                // We don't know what the receiver will be inside an arbitrary lambda like here, so
                // we shouldn't warn for any calls inside a lambda
                fun Modifier.composed(
                    factory: Modifier.() -> Modifier
                ): Modifier = then(with(Modifier) { factory() })

                fun Modifier.test6() = this.then(Modifier.composed { TestModifier })
                fun Modifier.test7() = this.then(Modifier.composed { test() })

                fun Modifier.test8(): Modifier {
                    val lambda: Modifier.() -> Modifier = {
                        Modifier.test()
                    }
                    return this.then(lambda())
                }
"""
                ),
                Stubs.Modifier
            )
            .run()
            .expectClean()
    }

    @Test
    fun errors() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.Modifier

                object TestModifier : Modifier.Element

                fun Modifier.test(): Modifier = this.then(TestModifier)

                fun Modifier.test2() = this.then(test())

                fun Modifier.test3() = this.then(with(1) { test() })

                fun Modifier.test4() = this.then(if (true) test() else TestModifier)
"""
                ),
                Stubs.Modifier
            )
            .run()
            .expect(
                """
src/test/TestModifier.kt:10: Error: Using Modifier.then with a Modifier factory function with an implicit receiver [SuspiciousModifierThen]
                fun Modifier.test2() = this.then(test())
                                                 ~~~~
src/test/TestModifier.kt:12: Error: Using Modifier.then with a Modifier factory function with an implicit receiver [SuspiciousModifierThen]
                fun Modifier.test3() = this.then(with(1) { test() })
                                                           ~~~~
src/test/TestModifier.kt:14: Error: Using Modifier.then with a Modifier factory function with an implicit receiver [SuspiciousModifierThen]
                fun Modifier.test4() = this.then(if (true) test() else TestModifier)
                                                           ~~~~
3 errors, 0 warnings
            """
            )
    }
}
