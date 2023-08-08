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

package androidx.compose.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
class SteppedForLoopDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = SteppedForLoopDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(
        SteppedForLoopDetector.ISSUE
    )

    @Test
    fun skippedOnRegularLoops() {
        lint().files(
            kotlin(
                """
                package test

                fun test(a: Int, b: Int) {
                    for (i in a..b) {
                        println(i)
                    }
                    for (i in a until b) {
                        println(i)
                    }
                    for (i in a downTo b) {
                        println(i)
                    }
                }
            """
            )
        )
            .run()
            .expectClean()
    }

    @Test
    fun skippedOnInclusiveSteppedLoops() {
        lint().files(
            kotlin(
                """
                package test

                fun test(a: Int, b: Int) {
                    for (i in a..b step 2) {
                        println(i)
                    }
                    for (i in a downTo b step 2) {
                        println(i)
                    }
                }
            """
            )
        )
            .run()
            .expectClean()
    }

    @Ignore("b/289814619")
    @Test
    fun calledOnSteppedLoop() {
        lint().files(
            kotlin(
                """
                package test

                fun test(a: Int, b: Int) {
                    for (i in a until b step 2) {
                        println(i)
                    }
                    for (i in a..<b step 2) {
                        println(i)
                    }
                }
            """
            )
        )
            .run()
            .expect(
                """
src/test/test.kt:5: Error: stepping the integer range by 2. [SteppedForLoop]
                    for (i in a until b step 2) {
                              ~~~~~~~~~~~~~~~~
src/test/test.kt:8: Error: stepping the integer range by 2. [SteppedForLoop]
                    for (i in a..<b step 2) {
                              ~~~~~~~~~~~~
2 errors, 0 warnings
            """
            )
    }

    @Ignore("b/289814619")
    @Test
    fun calledOnConstantSteppedLoop() {
        lint().files(
            kotlin(
                """
                package test

                fun test() {
                    for (i in 0 until 10 step 2) {
                        println(i)
                    }
                    for (i in 0..<10 step 2) {
                        println(i)
                    }
                }
            """
            )
        )
            .run()
            .expect(
                """
src/test/test.kt:5: Error: stepping the integer range by 2. [SteppedForLoop]
                    for (i in 0 until 10 step 2) {
                              ~~~~~~~~~~~~~~~~~
src/test/test.kt:8: Error: stepping the integer range by 2. [SteppedForLoop]
                    for (i in 0..<10 step 2) {
                              ~~~~~~~~~~~~~
2 errors, 0 warnings
            """
            )
    }

    @Ignore("b/289814619")
    @Test
    fun calledOnUnitSteppedLoop() {
        lint().files(
            kotlin(
                """
                package test

                fun test(a: Int, b: Int) {
                    for (i in a until b step 1) {
                        println(i)
                    }
                    for (i in a..<b step 1) {
                        println(i)
                    }
                }
            """
            )
        )
            .run()
            .expect(
                """
src/test/test.kt:5: Error: stepping the integer range by 1. [SteppedForLoop]
                    for (i in a until b step 1) {
                              ~~~~~~~~~~~~~~~~
src/test/test.kt:8: Error: stepping the integer range by 1. [SteppedForLoop]
                    for (i in a..<b step 1) {
                              ~~~~~~~~~~~~
2 errors, 0 warnings
            """
            )
    }

    @Ignore("b/289814619")
    @Test
    fun calledOnExpressionSteppedLoop() {
        lint().files(
            kotlin(
                """
                package test

                fun test(a: Int, b: Int, c: Int) {
                    for (i in a until b step (c / 2)) {
                        println(i)
                    }
                    for (i in a..<b step (c / 2)) {
                        println(i)
                    }
                }
            """
            )
        )
            .run()
            .expect(
                """
src/test/test.kt:5: Error: stepping the integer range by (c / 2). [SteppedForLoop]
                    for (i in a until b step (c / 2)) {
                              ~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:8: Error: stepping the integer range by (c / 2). [SteppedForLoop]
                    for (i in a..<b step (c / 2)) {
                              ~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
            """
            )
    }

    @Test
    fun calledOnNonIntSteppedLoop() {
        lint().files(
            kotlin(
                """
                package test

                fun test(a: UInt, b: UInt) {
                    for (i in a until b step 2) {
                        println(i)
                    }
                }
                fun test(a: Char, b: Char) {
                    for (i in a until b step 2) {
                        println(i)
                    }
                }
                fun test(a: Long, b: Long) {
                    for (i in a until b step 2L) {
                        println(i)
                    }
                }
                fun test(a: ULong, b: ULong) {
                    for (i in a until b step 2L) {
                        println(i)
                    }
                }
            """
            )
        )
            .run()
            .expect(
                """
src/test/test.kt:5: Error: stepping the integer range by 2. [SteppedForLoop]
                    for (i in a until b step 2) {
                              ~~~~~~~~~~~~~~~~
src/test/test.kt:10: Error: stepping the integer range by 2. [SteppedForLoop]
                    for (i in a until b step 2) {
                              ~~~~~~~~~~~~~~~~
src/test/test.kt:15: Error: stepping the integer range by 2L. [SteppedForLoop]
                    for (i in a until b step 2L) {
                              ~~~~~~~~~~~~~~~~~
src/test/test.kt:20: Error: stepping the integer range by 2L. [SteppedForLoop]
                    for (i in a until b step 2L) {
                              ~~~~~~~~~~~~~~~~~
4 errors, 0 warnings
            """
            )
    }
}
/* ktlint-enable max-line-length */
