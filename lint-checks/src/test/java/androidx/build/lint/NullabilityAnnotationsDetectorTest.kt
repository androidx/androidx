/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.lint

import androidx.build.lint.Stubs.Companion.JetBrainsAnnotations
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NullabilityAnnotationsDetectorTest : AbstractLintDetectorTest(
    useDetector = NullabilityAnnotationsDetector(),
    useIssues = listOf(NullabilityAnnotationsDetector.ISSUE),
    ) {
    @Test
    fun `Detection of Jetbrains nullability usage in Java sources`() {
        val input = arrayOf(
            javaSample("androidx.NullabilityAnnotationsJava"),
            JetBrainsAnnotations
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/NullabilityAnnotationsJava.java:31: Error: Use @androidx.annotation.NonNull instead of @org.jetbrains.annotations.NotNull [NullabilityAnnotationsDetector]
    private void method1(@NotNull String arg) {
                         ~~~~~~~~
src/androidx/sample/NullabilityAnnotationsJava.java:38: Error: Use @androidx.annotation.Nullable instead of @org.jetbrains.annotations.Nullable [NullabilityAnnotationsDetector]
    private void method2(@Nullable String arg) {
                         ~~~~~~~~~
2 errors, 0 warnings
    """.trimIndent()

        val expectFixDiffs = """
Autofix for src/androidx/sample/NullabilityAnnotationsJava.java line 31: Replace with `@androidx.annotation.NonNull`:
@@ -31 +31
-     private void method1(@NotNull String arg) {
+     private void method1(@androidx.annotation.NonNull String arg) {
Autofix for src/androidx/sample/NullabilityAnnotationsJava.java line 38: Replace with `@androidx.annotation.Nullable`:
@@ -38 +38
-     private void method2(@Nullable String arg) {
+     private void method2(@androidx.annotation.Nullable String arg) {
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input)
            .expect(expected)
            .expectFixDiffs(expectFixDiffs)
    }

    @Test
    fun `JetBrains annotations allowed in Kotlin sources`() {
        val input = arrayOf(
            ktSample("androidx.NullabilityAnnotationsKotlin"),
            JetBrainsAnnotations
        )
        check(*input).expectClean()
    }
}
