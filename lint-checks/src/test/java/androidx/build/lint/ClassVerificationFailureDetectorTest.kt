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

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.manifest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("UnstableApiUsage")
@RunWith(JUnit4::class)
class ClassVerificationFailureDetectorTest {

    private fun check(
        vararg testFiles: TestFile,
        minSdkVersion: Int = 14,
    ): TestLintResult {
        return lint()
            .files(
                manifest().minSdk(minSdkVersion),
                *testFiles,
            )
            .issues(ClassVerificationFailureDetector.ISSUE)
            .run()
    }

    @Test
    fun `Detection of unsafe references in Java sources`() {
        val input = arrayOf(
            javaSample("androidx.ClassVerificationFailureFromJava")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ClassVerificationFailureFromJava.java:38: Error: This call references a method added in API level 21; however, the containing class androidx.ClassVerificationFailureFromJava is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            view.setBackgroundTintList(tint);
                 ~~~~~~~~~~~~~~~~~~~~~
src/androidx/ClassVerificationFailureFromJava.java:47: Error: This call references a method added in API level 17; however, the containing class androidx.ClassVerificationFailureFromJava is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            return View.generateViewId();
                        ~~~~~~~~~~~~~~
src/androidx/ClassVerificationFailureFromJava.java:58: Error: This call references a method added in API level 23; however, the containing class androidx.ClassVerificationFailureFromJava is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return view.getAccessibilityClassName();
                    ~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/ClassVerificationFailureFromJava.java:80: Error: This call references a method added in API level 28; however, the containing class androidx.ClassVerificationFailureFromJava.Api28Impl is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            return view.getAccessibilityPaneTitle();
                        ~~~~~~~~~~~~~~~~~~~~~~~~~
4 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Detection and auto-fix of unsafe references in real-world Java sources`() {
        val input = arrayOf(
            javaSample("androidx.core.widget.ListViewCompat")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/core/widget/ListViewCompat.java:39: Error: This call references a method added in API level 19; however, the containing class androidx.core.widget.ListViewCompat is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            listView.scrollListBy(y);
                     ~~~~~~~~~~~~
src/androidx/core/widget/ListViewCompat.java:69: Error: This call references a method added in API level 19; however, the containing class androidx.core.widget.ListViewCompat is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            return listView.canScrollList(direction);
                            ~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()

        val expectedFix = """
Fix for src/androidx/core/widget/ListViewCompat.java line 39: Extract to static inner class:
@@ -39 +39
-             listView.scrollListBy(y);
+             Api19Impl.scrollListBy(listView, y);
@@ -91 +91
- }
+ @androidx.annotation.RequiresApi(19)
+ static class Api19Impl {
+     private Api19Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static void scrollListBy(AbsListView absListView, int y) {
+         absListView.scrollListBy(y);
+     }
@@ -93 +102
+ }}
+
Fix for src/androidx/core/widget/ListViewCompat.java line 69: Extract to static inner class:
@@ -69 +69
-             return listView.canScrollList(direction);
+             return Api19Impl.canScrollList(listView, direction);
@@ -91 +91
- }
+ @androidx.annotation.RequiresApi(19)
+ static class Api19Impl {
+     private Api19Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static boolean canScrollList(AbsListView absListView, int direction) {
+         return absListView.canScrollList(direction);
+     }
@@ -93 +102
+ }}
+
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe void-type method reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeVoidMethodReferenceJava")
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeVoidMethodReferenceJava.java line 34: Extract to static inner class:
@@ -34 +34
-             view.setBackgroundTintList(new ColorStateList(null, null));
+             Api21Impl.setBackgroundTintList(view, new ColorStateList(null, null));
@@ -37 +37
- }
+ @annotation.RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @annotation.DoNotInline
+     static void setBackgroundTintList(View view, ColorStateList tint) {
+         view.setBackgroundTintList(tint);
+     }
@@ -39 +48
+ }}
+
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe constructor reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeConstructorReferenceJava")
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeConstructorReferenceJava.java line 35: Extract to static inner class:
@@ -35 +35
-             AccessibilityNodeInfo node = new AccessibilityNodeInfo(new View(context), 1);
+             AccessibilityNodeInfo node = Api30Impl.createAccessibilityNodeInfo(new View(context), 1);
@@ -38 +38
- }
+ @annotation.RequiresApi(30)
+ static class Api30Impl {
+     private Api30Impl() {
+         // This class is not instantiable.
+     }
+
+     @annotation.DoNotInline
+     static AccessibilityNodeInfo createAccessibilityNodeInfo(View root, int virtualDescendantId) {
+         return new AccessibilityNodeInfo(root, virtualDescendantId);
+     }
@@ -40 +49
+ }}
+
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe static method reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeStaticMethodReferenceJava")
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeStaticMethodReferenceJava.java line 33: Extract to static inner class:
@@ -33 +33
-             return View.generateViewId();
+             return Api17Impl.generateViewId();
@@ -37 +37
- }
+ @annotation.RequiresApi(17)
+ static class Api17Impl {
+     private Api17Impl() {
+         // This class is not instantiable.
+     }
+
+     @annotation.DoNotInline
+     static int generateViewId() {
+         return View.generateViewId();
+     }
@@ -39 +48
+ }}
+
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe generic-type method reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeGenericMethodReferenceJava")
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeGenericMethodReferenceJava.java line 34: Extract to static inner class:
@@ -34 +34
-             return context.getSystemService(serviceClass);
+             return Api23Impl.getSystemService(context, serviceClass);
@@ -38 +38
- }
+ @annotation.RequiresApi(23)
+ static class Api23Impl {
+     private Api23Impl() {
+         // This class is not instantiable.
+     }
+
+     @annotation.DoNotInline
+     static <T> T getSystemService(Context context, Class<T> serviceClass) {
+         return context.getSystemService(serviceClass);
+     }
@@ -40 +49
+ }}
+
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe reference in Java source with existing inner class`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeReferenceWithExistingClassJava")
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeReferenceWithExistingClassJava.java line 36: Extract to static inner class:
@@ -36 +36
-             view.setBackgroundTintList(new ColorStateList(null, null));
+             Api21Impl.setBackgroundTintList(view, new ColorStateList(null, null));
@@ -46 +46
- }
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static void setBackgroundTintList(View view, ColorStateList tint) {
+         view.setBackgroundTintList(tint);
+     }
@@ -48 +57
+ }}
+
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    /**
     * Loads a [TestFile] from Java source code included in the JAR resources.
     */
    private fun javaSample(className: String): TestFile = TestFiles.java(
        javaClass.getResource("/java/${className.replace('.', '/')}.java").readText()
    )

    /**
     * Loads a [TestFile] from Kotlin source code included in the JAR resources.
     */
    private fun ktSample(className: String): TestFile = TestFiles.kotlin(
        javaClass.getResource("/java/${className.replace('.', '/')}.kt").readText()
    )
}