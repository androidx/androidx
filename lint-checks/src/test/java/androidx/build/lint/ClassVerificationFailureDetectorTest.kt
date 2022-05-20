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

import androidx.build.lint.Stubs.Companion.RequiresApi
import androidx.build.lint.Stubs.Companion.IntRange
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ClassVerificationFailureDetectorTest : AbstractLintDetectorTest(
    useDetector = ClassVerificationFailureDetector(),
    useIssues = listOf(ClassVerificationFailureDetector.ISSUE),
    stubs = arrayOf(
        // AndroidManifest with minSdkVersion=14
        manifest().minSdk(14),
    ),
) {

    @Test
    fun `Detection of unsafe references in Java sources`() {
        val input = arrayOf(
            javaSample("androidx.ClassVerificationFailureFromJava"),
            RequiresApi,
            IntRange
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ClassVerificationFailureFromJava.java:37: Error: This call references a method added in API level 21; however, the containing class androidx.ClassVerificationFailureFromJava is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            view.setBackgroundTintList(tint);
                 ~~~~~~~~~~~~~~~~~~~~~
src/androidx/ClassVerificationFailureFromJava.java:46: Error: This call references a method added in API level 17; however, the containing class androidx.ClassVerificationFailureFromJava is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            return View.generateViewId();
                        ~~~~~~~~~~~~~~
src/androidx/ClassVerificationFailureFromJava.java:56: Error: This call references a method added in API level 23; however, the containing class androidx.ClassVerificationFailureFromJava is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return view.getAccessibilityClassName();
                    ~~~~~~~~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Detection and auto-fix of unsafe references in real-world Java sources`() {
        val input = arrayOf(
            javaSample("androidx.sample.core.widget.ListViewCompat"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/core/widget/ListViewCompat.java:39: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompat is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            listView.scrollListBy(y);
                     ~~~~~~~~~~~~
src/androidx/sample/core/widget/ListViewCompat.java:69: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompat is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            return listView.canScrollList(direction);
                            ~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()

        val expectedFix = """
Fix for src/androidx/sample/core/widget/ListViewCompat.java line 39: Extract to static inner class:
@@ -39 +39
-             listView.scrollListBy(y);
+             Api19Impl.scrollListBy(listView, y);
@@ -91 +91
+ @androidx.annotation.RequiresApi(19)
+ static class Api19Impl {
+     private Api19Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static void scrollListBy(android.widget.AbsListView absListView, int y) {
+         absListView.scrollListBy(y);
+     }
+
@@ -92 +103
+ }
Fix for src/androidx/sample/core/widget/ListViewCompat.java line 69: Extract to static inner class:
@@ -69 +69
-             return listView.canScrollList(direction);
+             return Api19Impl.canScrollList(listView, direction);
@@ -91 +91
+ @androidx.annotation.RequiresApi(19)
+ static class Api19Impl {
+     private Api19Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static boolean canScrollList(android.widget.AbsListView absListView, int direction) {
+         return absListView.canScrollList(direction);
+     }
+
@@ -92 +103
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Detection and auto-fix of unsafe references in real-world Kotlin sources`() {
        val input = arrayOf(
            ktSample("androidx.sample.core.widget.ListViewCompatKotlin"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/core/widget/ListViewCompatKotlin.kt:33: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompatKotlin is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            listView.scrollListBy(y)
                     ~~~~~~~~~~~~
src/androidx/sample/core/widget/ListViewCompatKotlin.kt:58: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompatKotlin is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            listView.canScrollList(direction)
                     ~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of RequiresApi annotation in outer class in Java source`() {
        val input = arrayOf(
            javaSample("androidx.RequiresApiJava"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of RequiresApi annotation in outer class in Kotlin source`() {
        val input = arrayOf(
            ktSample("androidx.RequiresApiKotlin"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/RequiresApiKotlinOuter19Passes.kt:67: Error: This call references a method added in API level 19; however, the containing class androidx.RequiresApiKotlinNoAnnotationFails.MyStaticClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            Character.isSurrogate(c)
                      ~~~~~~~~~~~
src/androidx/RequiresApiKotlinOuter19Passes.kt:77: Error: This call references a method added in API level 19; however, the containing class androidx.RequiresApiKotlinOuter16Fails.MyStaticClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            Character.isSurrogate(c)
                      ~~~~~~~~~~~
src/androidx/RequiresApiKotlinOuter19Passes.kt:87: Error: This call references a method added in API level 19; however, the containing class androidx.RequiresApiKotlinInner16Fails.MyStaticClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            Character.isSurrogate(c)
                      ~~~~~~~~~~~
src/androidx/RequiresApiKotlinOuter19Passes.kt:98: Error: This call references a method added in API level 19; however, the containing class androidx.RequiresApiKotlinInner16Outer16Fails.MyStaticClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            Character.isSurrogate(c)
                      ~~~~~~~~~~~
4 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Auto-fix unsafe void-type method reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeVoidMethodReferenceJava"),
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeVoidMethodReferenceJava.java line 34: Extract to static inner class:
@@ -34 +34
-             view.setBackgroundTintList(new ColorStateList(null, null));
+             Api21Impl.setBackgroundTintList(view, new ColorStateList(null, null));
@@ -37 +37
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
+
@@ -38 +49
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe constructor reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeConstructorReferenceJava"),
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeConstructorReferenceJava.java line 35: Extract to static inner class:
@@ -35 +35
-             AccessibilityNodeInfo node = new AccessibilityNodeInfo(new View(context), 1);
+             AccessibilityNodeInfo node = Api30Impl.createAccessibilityNodeInfo(new View(context), 1);
@@ -38 +38
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
+
@@ -39 +50
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe static method reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeStaticMethodReferenceJava"),
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeStaticMethodReferenceJava.java line 33: Extract to static inner class:
@@ -33 +33
-             return View.generateViewId();
+             return Api17Impl.generateViewId();
@@ -37 +37
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
+
@@ -38 +49
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe generic-type method reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeGenericMethodReferenceJava"),
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeGenericMethodReferenceJava.java line 34: Extract to static inner class:
@@ -34 +34
-             return context.getSystemService(serviceClass);
+             return Api23Impl.getSystemService(context, serviceClass);
@@ -38 +38
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
+
@@ -39 +50
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe reference in Java source with existing inner class`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeReferenceWithExistingClassJava"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeReferenceWithExistingClassJava.java line 36: Extract to static inner class:
@@ -36 +36
-             view.setBackgroundTintList(new ColorStateList(null, null));
+             Api21Impl.setBackgroundTintList(view, new ColorStateList(null, null));
@@ -46 +46
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
+
@@ -47 +58
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Detection and auto-fix for qualified expressions (issue 205026874)`() {
        val input = arrayOf(
            javaSample("androidx.sample.appcompat.widget.ActionBarBackgroundDrawable"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java:71: Error: This call references a method added in API level 21; however, the containing class androidx.sample.appcompat.widget.ActionBarBackgroundDrawable is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
                mContainer.mSplitBackground.getOutline(outline);
                                            ~~~~~~~~~~
src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java:76: Error: This call references a method added in API level 21; however, the containing class androidx.sample.appcompat.widget.ActionBarBackgroundDrawable is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
                mContainer.mBackground.getOutline(outline);
                                       ~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()

        val expectedFix = """
Fix for src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java line 71: Extract to static inner class:
@@ -71 +71
-                 mContainer.mSplitBackground.getOutline(outline);
+                 Api21Impl.getOutline(mContainer.mSplitBackground, outline);
@@ -90 +90
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static void getOutline(Drawable drawable, Outline outline) {
+         drawable.getOutline(outline);
+     }
+
@@ -91 +102
+ }
Fix for src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java line 76: Extract to static inner class:
@@ -76 +76
-                 mContainer.mBackground.getOutline(outline);
+                 Api21Impl.getOutline(mContainer.mBackground, outline);
@@ -90 +90
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static void getOutline(Drawable drawable, Outline outline) {
+         drawable.getOutline(outline);
+     }
+
@@ -91 +102
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix includes fully qualified class name (issue 205035683)`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeMethodWithQualifiedClass"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/AutofixUnsafeMethodWithQualifiedClass.java:35: Error: This call references a method added in API level 23; however, the containing class androidx.AutofixUnsafeMethodWithQualifiedClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return callback.onSearchRequested(searchEvent);
                        ~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/androidx/AutofixUnsafeMethodWithQualifiedClass.java line 35: Extract to static inner class:
@@ -35 +35
-         return callback.onSearchRequested(searchEvent);
+         return Api23Impl.onSearchRequested(callback, searchEvent);
@@ -37 +37
+ @RequiresApi(23)
+ static class Api23Impl {
+     private Api23Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static boolean onSearchRequested(Window.Callback callback, SearchEvent p) {
+         return callback.onSearchRequested(p);
+     }
+
@@ -38 +49
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }
}
