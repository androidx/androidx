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

package androidx.compose.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LambdaStructuralEqualityDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = LambdaStructuralEqualityDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(LambdaStructuralEqualityDetector.ISSUE)

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package test

                val lambda1 = { 1 }
                val lambda2 = { 2 }

                fun test() {
                    lambda1 === lambda2
                    lambda1 !== lambda2
                }
            """
                )
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

                val lambda1 = { 1 }
                val lambda2 = { 2 }
                val lambda3: (() -> Unit)? = null

                fun test() {
                    lambda1 == lambda2
                    lambda1 != lambda2
                    lambda1.equals(lambda2)
                    lambda3?.equals(lambda2)
                }
            """
                )
            )
            .run()
            .expect(
                """
src/test/test.kt:9: Error: Checking lambdas for structural equality, instead of checking for referential equality [LambdaStructuralEquality]
                    lambda1 == lambda2
                            ~~
src/test/test.kt:10: Error: Checking lambdas for structural equality, instead of checking for referential equality [LambdaStructuralEquality]
                    lambda1 != lambda2
                            ~~
src/test/test.kt:11: Error: Checking lambdas for structural equality, instead of checking for referential equality [LambdaStructuralEquality]
                    lambda1.equals(lambda2)
                            ~~~~~~
src/test/test.kt:12: Error: Checking lambdas for structural equality, instead of checking for referential equality [LambdaStructuralEquality]
                    lambda3?.equals(lambda2)
                             ~~~~~~
4 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Autofix for src/test/test.kt line 9: Change to ===:
@@ -9 +9
-                     lambda1 == lambda2
+                     lambda1 === lambda2
Autofix for src/test/test.kt line 10: Change to !==:
@@ -10 +10
-                     lambda1 != lambda2
+                     lambda1 !== lambda2
                """
            )
    }
}
