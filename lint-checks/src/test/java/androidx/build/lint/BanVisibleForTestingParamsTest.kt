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

package androidx.build.lint

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanVisibleForTestingParamsTest : AbstractLintDetectorTest(
    useDetector = BanVisibleForTestingParams(),
    useIssues = listOf(BanVisibleForTestingParams.ISSUE),
    stubs = arrayOf(Stubs.VisibleForTesting),
) {

    @Test
    fun `Detection of @VisibleForTesting usage in Java sources`() {
        val input = arrayOf(
            javaSample("androidx.VisibleForTestingUsageJava"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/VisibleForTestingUsageJava.java:23: Error: Found non-default otherwise value for @VisibleForTesting [UsesNonDefaultVisibleForTesting]
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/VisibleForTestingUsageJava.java:26: Error: Found non-default otherwise value for @VisibleForTesting [UsesNonDefaultVisibleForTesting]
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/VisibleForTestingUsageJava.java:29: Error: Found non-default otherwise value for @VisibleForTesting [UsesNonDefaultVisibleForTesting]
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/VisibleForTestingUsageJava.java:32: Error: Found non-default otherwise value for @VisibleForTesting [UsesNonDefaultVisibleForTesting]
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
4 errors, 0 warnings
        """.trimIndent()

        val fixDiffs = """
Fix for src/androidx/VisibleForTestingUsageJava.java line 23: Remove non-default `otherwise` value:
@@ -23 +23
-     @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
+     @VisibleForTesting
Fix for src/androidx/VisibleForTestingUsageJava.java line 26: Remove @VisibleForTesting annotation:
@@ -26 +26
-     @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
Fix for src/androidx/VisibleForTestingUsageJava.java line 29: Remove @VisibleForTesting annotation:
@@ -29 +29
-     @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
Fix for src/androidx/VisibleForTestingUsageJava.java line 32: Remove non-default `otherwise` value:
@@ -32 +32
-     @VisibleForTesting(otherwise = VisibleForTesting.NONE)
+     @VisibleForTesting
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input)
            .expect(expected)
            .expectFixDiffs(fixDiffs)
    }
    @Test
    fun `Detection of @VisibleForTesting usage in Kotlin sources`() {
        val input = arrayOf(
            ktSample("androidx.VisibleForTestingUsageKotlin"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/VisibleForTestingUsageKotlin.kt:26: Error: Found non-default otherwise value for @VisibleForTesting [UsesNonDefaultVisibleForTesting]
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/VisibleForTestingUsageKotlin.kt:29: Error: Found non-default otherwise value for @VisibleForTesting [UsesNonDefaultVisibleForTesting]
    @VisibleForTesting(otherwise = VisibleForTesting.Companion.PRIVATE)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/VisibleForTestingUsageKotlin.kt:32: Error: Found non-default otherwise value for @VisibleForTesting [UsesNonDefaultVisibleForTesting]
    @VisibleForTesting(VisibleForTesting.PRIVATE)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/VisibleForTestingUsageKotlin.kt:35: Error: Found non-default otherwise value for @VisibleForTesting [UsesNonDefaultVisibleForTesting]
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/VisibleForTestingUsageKotlin.kt:38: Error: Found non-default otherwise value for @VisibleForTesting [UsesNonDefaultVisibleForTesting]
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/VisibleForTestingUsageKotlin.kt:41: Error: Found non-default otherwise value for @VisibleForTesting [UsesNonDefaultVisibleForTesting]
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/VisibleForTestingUsageKotlin.kt:47: Error: Found non-default otherwise value for @VisibleForTesting [UsesNonDefaultVisibleForTesting]
    @get:VisibleForTesting(NONE)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
7 errors, 0 warnings
        """.trimIndent()

        val fixDiffs = """
Fix for src/androidx/VisibleForTestingUsageKotlin.kt line 26: Remove non-default `otherwise` value:
@@ -26 +26
-     @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
+     @VisibleForTesting
Fix for src/androidx/VisibleForTestingUsageKotlin.kt line 29: Remove non-default `otherwise` value:
@@ -29 +29
-     @VisibleForTesting(otherwise = VisibleForTesting.Companion.PRIVATE)
+     @VisibleForTesting
Fix for src/androidx/VisibleForTestingUsageKotlin.kt line 32: Remove non-default `otherwise` value:
@@ -32 +32
-     @VisibleForTesting(VisibleForTesting.PRIVATE)
+     @VisibleForTesting
Fix for src/androidx/VisibleForTestingUsageKotlin.kt line 35: Remove @VisibleForTesting annotation:
@@ -35 +35
-     @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
Fix for src/androidx/VisibleForTestingUsageKotlin.kt line 38: Remove @VisibleForTesting annotation:
@@ -38 +38
-     @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
Fix for src/androidx/VisibleForTestingUsageKotlin.kt line 41: Remove non-default `otherwise` value:
@@ -41 +41
-     @VisibleForTesting(otherwise = VisibleForTesting.NONE)
+     @VisibleForTesting
Fix for src/androidx/VisibleForTestingUsageKotlin.kt line 47: Remove non-default `otherwise` value:
@@ -47 +47
-     @get:VisibleForTesting(NONE)
+     @get:VisibleForTesting
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input)
            .expect(expected)
            .expectFixDiffs(fixDiffs)
    }
}
