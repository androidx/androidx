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

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ObsoleteCompatDetectorTest : AbstractLintDetectorTest(
    useDetector = ObsoleteCompatDetector(),
    useIssues = listOf(ObsoleteCompatDetector.ISSUE),
    stubs = arrayOf(java("""
        package androidx.annotation;

        import static java.lang.annotation.ElementType.CONSTRUCTOR;
        import static java.lang.annotation.ElementType.FIELD;
        import static java.lang.annotation.ElementType.METHOD;

        import java.lang.annotation.Target;

        @Target({METHOD, FIELD, CONSTRUCTOR})
        public @interface ReplaceWith {
            String expression();
        }
    """))
) {

    @Test
    fun `Obsolete compat method`() {
        val input = arrayOf(
            javaSample("androidx.ObsoleteCompatMethod"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ObsoleteCompatMethod.java:33: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCode(Object obj) {
                       ~~~~~~~~
src/androidx/ObsoleteCompatMethod.java:38: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCodeNoDoc(Object obj) {
                       ~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()

        val expectedAutoFix = """
Fix for src/androidx/ObsoleteCompatMethod.java line 33: Add @deprecated Javadoc annotation:
@@ -32 +32
+      * @deprecated Call {@link Object#hashCode()} directly.
+     @Deprecated
+     @androidx.annotation.ReplaceWith(expression = "obj.hashCode()")
Fix for src/androidx/ObsoleteCompatMethod.java line 38: Add @deprecated Javadoc annotation:
@@ -38 +38
+     @Deprecated
+     @androidx.annotation.ReplaceWith(expression = "obj.hashCode()")
+     /** @deprecated Call {@link Object#hashCode()} directly. */
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedAutoFix)
    }

    @Test
    fun `Obsolete compat method missing @ReplaceWith`() {
        val input = arrayOf(
            javaSample("androidx.ObsoleteCompatMethodMissingReplaceWith"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ObsoleteCompatMethodMissingReplaceWith.java:32: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCode(Object obj) {
                       ~~~~~~~~
1 errors, 0 warnings
        """.trimIndent()

        val expectedAutoFix = """
Fix for src/androidx/ObsoleteCompatMethodMissingReplaceWith.java line 32: Annotate with @ReplaceWith:
@@ -31 +31
+     @androidx.annotation.ReplaceWith(expression = "obj.hashCode()")
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedAutoFix)
    }

    @Test
    fun `Obsolete compat methods missing @Deprecated`() {
        val input = arrayOf(
            javaSample("androidx.ObsoleteCompatMethodMissingDeprecated"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ObsoleteCompatMethodMissingDeprecated.java:37: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCode(Object obj) {
                       ~~~~~~~~
1 errors, 0 warnings
        """.trimIndent()

        val expectedAutoFix = """
Fix for src/androidx/ObsoleteCompatMethodMissingDeprecated.java line 37: Annotate with @Deprecated:
@@ -36 +36
+     @Deprecated
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedAutoFix)
    }

    @Test
    fun `Obsolete compat methods missing javadoc`() {
        val input = arrayOf(
            javaSample("androidx.ObsoleteCompatMethodMissingJavadoc"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ObsoleteCompatMethodMissingJavadoc.java:37: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCode(Object obj) {
                       ~~~~~~~~
src/androidx/ObsoleteCompatMethodMissingJavadoc.java:44: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCodeNoDoc(Object obj) {
                       ~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()

        val expectedAutoFix = """
Fix for src/androidx/ObsoleteCompatMethodMissingJavadoc.java line 37: Add @deprecated Javadoc annotation:
@@ -34 +34
+      * @deprecated Call {@link Object#hashCode()} directly.
Fix for src/androidx/ObsoleteCompatMethodMissingJavadoc.java line 44: Add @deprecated Javadoc annotation:
@@ -42 +42
+     /** @deprecated Call {@link Object#hashCode()} directly. */
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedAutoFix)
    }

    @Test
    fun `Obsolete compat methods missing Deprecated and javadoc`() {
        val input = arrayOf(
            javaSample("androidx.ObsoleteCompatMethodMissingDeprecatedAndJavadoc"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ObsoleteCompatMethodMissingDeprecatedAndJavadoc.java:36: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCode(Object obj) {
                       ~~~~~~~~
src/androidx/ObsoleteCompatMethodMissingDeprecatedAndJavadoc.java:42: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCodeNoDoc(Object obj) {
                       ~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()

        val expectedAutoFix = """
Fix for src/androidx/ObsoleteCompatMethodMissingDeprecatedAndJavadoc.java line 36: Add @deprecated Javadoc annotation:
@@ -34 +34
+      * @deprecated Call {@link Object#hashCode()} directly.
+     @Deprecated
Fix for src/androidx/ObsoleteCompatMethodMissingDeprecatedAndJavadoc.java line 42: Add @deprecated Javadoc annotation:
@@ -41 +41
+     @Deprecated
+     /** @deprecated Call {@link Object#hashCode()} directly. */
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedAutoFix)
    }
}
