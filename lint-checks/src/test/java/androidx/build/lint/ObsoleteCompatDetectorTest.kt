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
class ObsoleteCompatDetectorTest :
    AbstractLintDetectorTest(
        useDetector = ObsoleteCompatDetector(),
        useIssues = listOf(ObsoleteCompatDetector.ISSUE),
        stubs =
            arrayOf(
                java(
                    """
        package androidx.annotation;

        import static java.lang.annotation.ElementType.CONSTRUCTOR;
        import static java.lang.annotation.ElementType.FIELD;
        import static java.lang.annotation.ElementType.METHOD;

        import java.lang.annotation.Target;

        @Target({METHOD, FIELD, CONSTRUCTOR})
        public @interface ReplaceWith {
            String expression();
        }
    """
                )
            )
    ) {

    @Test
    fun `Obsolete compat method`() {
        val input =
            arrayOf(
                javaSample("androidx.ObsoleteCompatMethod"),
            )

        val expected =
            """
src/androidx/ObsoleteCompatMethod.java:33: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCode(Object obj) {
                       ~~~~~~~~
src/androidx/ObsoleteCompatMethod.java:38: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCodeNoDoc(Object obj) {
                       ~~~~~~~~~~~~~
2 errors, 0 warnings
        """
                .trimIndent()

        val expectedAutoFix =
            """
Fix for src/androidx/ObsoleteCompatMethod.java line 33: Replace obsolete compat method:
@@ -20 +20
+ import androidx.annotation.ReplaceWith;
@@ -32 +33
+      * @deprecated Call {@link Object#hashCode()} directly.
@@ -33 +35
+     @Deprecated
+     @ReplaceWith(expression = "obj.hashCode()")
Fix for src/androidx/ObsoleteCompatMethod.java line 38: Replace obsolete compat method:
@@ -20 +20
+ import androidx.annotation.ReplaceWith;
@@ -38 +39
+     @Deprecated
+     @ReplaceWith(expression = "obj.hashCode()")
+     /** @deprecated Call {@link Object#hashCode()} directly. */
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedAutoFix)
    }

    @Test
    fun `Obsolete compat method missing @ReplaceWith`() {
        val input =
            arrayOf(
                javaSample("androidx.ObsoleteCompatMethodMissingReplaceWith"),
            )

        val expected =
            """
src/androidx/ObsoleteCompatMethodMissingReplaceWith.java:32: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCode(Object obj) {
                       ~~~~~~~~
1 errors, 0 warnings
        """
                .trimIndent()

        val expectedAutoFix =
            """
Autofix for src/androidx/ObsoleteCompatMethodMissingReplaceWith.java line 32: Replace obsolete compat method:
@@ -18 +18
+ import androidx.annotation.ReplaceWith;
@@ -31 +32
+     @ReplaceWith(expression = "obj.hashCode()")
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedAutoFix)
    }

    @Test
    fun `Obsolete compat method missing multi-line @ReplaceWith`() {
        val input =
            arrayOf(
                javaSample("androidx.ObsoleteCompatMethodMissingMultiLineReplaceWith"),
            )

        val expected =
            """
src/androidx/ObsoleteCompatMethodMissingMultiLineReplaceWith.java:32: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCode(Object obj) {
                       ~~~~~~~~
1 errors, 0 warnings
        """
                .trimIndent()

        val expectedAutoFix =
            """
Autofix for src/androidx/ObsoleteCompatMethodMissingMultiLineReplaceWith.java line 32: Replace obsolete compat method:
@@ -18 +18
+ import androidx.annotation.ReplaceWith;
@@ -31 +32
+     @ReplaceWith(expression = "obj.hashCode()")
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedAutoFix)
    }

    @Test
    fun `Obsolete compat methods missing @Deprecated`() {
        val input =
            arrayOf(
                javaSample("androidx.ObsoleteCompatMethodMissingDeprecated"),
            )

        val expected =
            """
src/androidx/ObsoleteCompatMethodMissingDeprecated.java:37: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCode(Object obj) {
                       ~~~~~~~~
1 errors, 0 warnings
        """
                .trimIndent()

        val expectedAutoFix =
            """
Autofix for src/androidx/ObsoleteCompatMethodMissingDeprecated.java line 37: Replace obsolete compat method:
@@ -36 +36
+     @Deprecated
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedAutoFix)
    }

    @Test
    fun `Obsolete compat methods missing javadoc`() {
        val input =
            arrayOf(
                javaSample("androidx.ObsoleteCompatMethodMissingJavadoc"),
            )

        val expected =
            """
src/androidx/ObsoleteCompatMethodMissingJavadoc.java:37: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCode(Object obj) {
                       ~~~~~~~~
src/androidx/ObsoleteCompatMethodMissingJavadoc.java:44: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCodeNoDoc(Object obj) {
                       ~~~~~~~~~~~~~
2 errors, 0 warnings
        """
                .trimIndent()

        val expectedAutoFix =
            """
Autofix for src/androidx/ObsoleteCompatMethodMissingJavadoc.java line 37: Replace obsolete compat method:
@@ -34 +34
+      * @deprecated Call {@link Object#hashCode()} directly.
Autofix for src/androidx/ObsoleteCompatMethodMissingJavadoc.java line 44: Replace obsolete compat method:
@@ -42 +42
+     /** @deprecated Call {@link Object#hashCode()} directly. */
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedAutoFix)
    }

    @Test
    fun `Obsolete compat methods missing Deprecated and javadoc`() {
        val input =
            arrayOf(
                javaSample("androidx.ObsoleteCompatMethodMissingDeprecatedAndJavadoc"),
            )

        val expected =
            """
src/androidx/ObsoleteCompatMethodMissingDeprecatedAndJavadoc.java:36: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCode(Object obj) {
                       ~~~~~~~~
src/androidx/ObsoleteCompatMethodMissingDeprecatedAndJavadoc.java:42: Error: Obsolete compat method should provide replacement [ObsoleteCompatMethod]
    public static long hashCodeNoDoc(Object obj) {
                       ~~~~~~~~~~~~~
2 errors, 0 warnings
        """
                .trimIndent()

        val expectedAutoFix =
            """
Fix for src/androidx/ObsoleteCompatMethodMissingDeprecatedAndJavadoc.java line 36: Replace obsolete compat method:
@@ -34 +34
+      * @deprecated Call {@link Object#hashCode()} directly.
@@ -35 +36
+     @Deprecated
Fix for src/androidx/ObsoleteCompatMethodMissingDeprecatedAndJavadoc.java line 42: Replace obsolete compat method:
@@ -41 +41
+     @Deprecated
+     /** @deprecated Call {@link Object#hashCode()} directly. */
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedAutoFix)
    }
}
