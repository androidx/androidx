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

import androidx.build.lint.Stubs.Companion.DoNotInline
import androidx.build.lint.Stubs.Companion.FlaggedApi
import androidx.build.lint.Stubs.Companion.Flags
import androidx.build.lint.Stubs.Companion.IntRange
import androidx.build.lint.Stubs.Companion.RequiresAconfigFlag
import androidx.build.lint.Stubs.Companion.RequiresApi
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ClassVerificationFailureDetectorTest :
    AbstractLintDetectorTest(
        useDetector = ClassVerificationFailureDetector(),
        useIssues = listOf(ClassVerificationFailureDetector.METHOD_CALL_ISSUE),
        stubs =
            arrayOf(
                // AndroidManifest with minSdkVersion=14
                manifest().minSdk(14)
            ),
    ) {

    @get:Rule var temporaryFolder = TemporaryFolder()

    override fun lint(): TestLintTask {
        return super.lint()
            .configureOption(ClassVerificationFailureDetector.METHOD_CALL_ALLUSAGES_OPTION, true)
    }

    @Test
    fun `Detection of unsafe references in Java sources`() {
        val input =
            arrayOf(javaSample("androidx.ClassVerificationFailureFromJava"), RequiresApi, IntRange)

        val expected =
            """
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
        """
                .trimIndent()

        check(*input).expect(expected)
    }

    @Test
    fun `Detection and auto-fix of unsafe references in real-world Java sources`() {
        val input = arrayOf(javaSample("androidx.sample.core.widget.ListViewCompat"))

        val expected =
            """
src/androidx/sample/core/widget/ListViewCompat.java:39: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompat is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            listView.scrollListBy(y);
                     ~~~~~~~~~~~~
src/androidx/sample/core/widget/ListViewCompat.java:69: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompat is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            return listView.canScrollList(direction);
                            ~~~~~~~~~~~~~
2 errors, 0 warnings
        """
                .trimIndent()

        val expectedFix =
            """
Fix for src/androidx/sample/core/widget/ListViewCompat.java line 39: Extract to static inner class:
@@ -21 +21
+ import android.widget.AbsListView;
@@ -23 +24
+ import androidx.annotation.DoNotInline;
@@ -24 +26
+ import androidx.annotation.RequiresApi;
@@ -39 +42
-             listView.scrollListBy(y);
+             Api19Impl.scrollListBy(listView, y);
@@ -91 +94
+
+ @RequiresApi(19)
+ static class Api19Impl {
+     private Api19Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static void scrollListBy(AbsListView absListView, int y) {
+         absListView.scrollListBy(y);
+     }
@@ -92 +105
+ }
Fix for src/androidx/sample/core/widget/ListViewCompat.java line 69: Extract to static inner class:
@@ -21 +21
+ import android.widget.AbsListView;
@@ -23 +24
+ import androidx.annotation.DoNotInline;
@@ -24 +26
+ import androidx.annotation.RequiresApi;
@@ -69 +72
-             return listView.canScrollList(direction);
+             return Api19Impl.canScrollList(listView, direction);
@@ -91 +94
+
+ @RequiresApi(19)
+ static class Api19Impl {
+     private Api19Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static boolean canScrollList(AbsListView absListView, int direction) {
+         return absListView.canScrollList(direction);
+     }
@@ -92 +105
+ }
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Detection and auto-fix of unsafe references in real-world Kotlin sources`() {
        val input = arrayOf(ktSample("androidx.sample.core.widget.ListViewCompatKotlin"))

        val expected =
            """
src/androidx/sample/core/widget/ListViewCompatKotlin.kt:33: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompatKotlin is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            listView.scrollListBy(y)
                     ~~~~~~~~~~~~
src/androidx/sample/core/widget/ListViewCompatKotlin.kt:56: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompatKotlin is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            listView.canScrollList(direction)
                     ~~~~~~~~~~~~~
2 errors, 0 warnings
        """
                .trimIndent()

        val expectedFix =
            """
Fix for src/androidx/sample/core/widget/ListViewCompatKotlin.kt line 33: Extract to static inner class:
@@ -20 +20
+ import android.widget.AbsListView
@@ -21 +22
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresApi
@@ -33 +36
-             listView.scrollListBy(y)
+             Api19Impl.scrollListBy(listView, y)
@@ -75 +78
+
+ @RequiresApi(19)
+ internal object Api19Impl {
+     @DoNotInline
+     @JvmStatic
+     fun scrollListBy(absListView: AbsListView, y: Int): Unit {
+         absListView.scrollListBy(y)
+     }
@@ -76 +87
+ }
Fix for src/androidx/sample/core/widget/ListViewCompatKotlin.kt line 56: Extract to static inner class:
@@ -20 +20
+ import android.widget.AbsListView
@@ -21 +22
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresApi
@@ -56 +59
-             listView.canScrollList(direction)
+             Api19Impl.canScrollList(listView, direction)
@@ -75 +78
+
+ @RequiresApi(19)
+ internal object Api19Impl {
+     @DoNotInline
+     @JvmStatic
+     fun canScrollList(absListView: AbsListView, direction: Int): Boolean {
+         return absListView.canScrollList(direction)
+     }
@@ -76 +87
+ }
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Detection of RequiresApi annotation in outer class in Java source`() {
        val input = arrayOf(javaSample("androidx.RequiresApiJava"), RequiresApi)

        val expected =
            """
No warnings.
        """
                .trimIndent()

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of RequiresApi annotation in outer class in Kotlin source`() {
        val input = arrayOf(ktSample("androidx.RequiresApiKotlin"), RequiresApi)

        val expected =
            """
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
        """
                .trimIndent()

        check(*input).expect(expected)
    }

    @Test
    fun `Auto-fix unsafe void-type method reference in Java source`() {
        val input = arrayOf(javaSample("androidx.AutofixUnsafeVoidMethodReferenceJava"))

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeVoidMethodReferenceJava.java line 34: Extract to static inner class:
@@ -22 +22
+ import androidx.annotation.DoNotInline;
+ import androidx.annotation.RequiresApi;
@@ -34 +36
-             view.setBackgroundTintList(new ColorStateList(null, null));
+             Api21Impl.setBackgroundTintList(view, new ColorStateList(null, null));
@@ -37 +39
+
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static void setBackgroundTintList(View view, ColorStateList tint) {
+         view.setBackgroundTintList(tint);
+     }
@@ -38 +50
+ }
        """
                .trimIndent()

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe void-type method reference in Kotlin source`() {
        val input = arrayOf(ktSample("androidx.AutofixUnsafeVoidMethodReferenceKotlin"))

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeVoidMethodReferenceKotlin.kt line 28: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresApi
@@ -28 +30
-             view.setBackgroundTintList(ColorStateList(null, null))
+             Api21Impl.setBackgroundTintList(view, ColorStateList(null, null))
@@ -31 +33
+
+ @RequiresApi(21)
+ internal object Api21Impl {
+     @DoNotInline
+     @JvmStatic
+     fun setBackgroundTintList(view: View, tint: ColorStateList): Unit {
+         view.setBackgroundTintList(tint)
+     }
@@ -32 +42
+ }
        """
                .trimIndent()

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe constructor reference in Java source`() {
        val input = arrayOf(javaSample("androidx.AutofixUnsafeConstructorReferenceJava"))

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeConstructorReferenceJava.java line 35: Extract to static inner class:
@@ -23 +23
+ import androidx.annotation.DoNotInline;
+ import androidx.annotation.RequiresApi;
@@ -35 +37
-             AccessibilityNodeInfo node = new AccessibilityNodeInfo(new View(context), 1);
+             AccessibilityNodeInfo node = Api30Impl.createAccessibilityNodeInfo(new View(context), 1);
@@ -38 +40
+
+ @RequiresApi(30)
+ static class Api30Impl {
+     private Api30Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static AccessibilityNodeInfo createAccessibilityNodeInfo(View root, int virtualDescendantId) {
+         return new AccessibilityNodeInfo(root, virtualDescendantId);
+     }
@@ -39 +51
+ }
        """
                .trimIndent()

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe constructor reference in Kotlin source`() {
        val input = arrayOf(ktSample("androidx.AutofixUnsafeConstructorReferenceKotlin"))

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeConstructorReferenceKotlin.kt line 29: Extract to static inner class:
@@ -22 +22
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresApi
@@ -29 +31
-             val node = AccessibilityNodeInfo(View(context), 1)
+             val node = Api30Impl.createAccessibilityNodeInfo(View(context), 1)
@@ -32 +34
+
+ @RequiresApi(30)
+ internal object Api30Impl {
+     @DoNotInline
+     @JvmStatic
+     fun createAccessibilityNodeInfo(root: View, virtualDescendantId: Int): AccessibilityNodeInfo {
+         return AccessibilityNodeInfo(root, virtualDescendantId)
+     }
@@ -33 +43
+ }
        """
                .trimIndent()

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe static method reference in Java source`() {
        val input = arrayOf(javaSample("androidx.AutofixUnsafeStaticMethodReferenceJava"))

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeStaticMethodReferenceJava.java line 33: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline;
+ import androidx.annotation.RequiresApi;
@@ -33 +35
-             return View.generateViewId();
+             return Api17Impl.generateViewId();
@@ -37 +39
+
+ @RequiresApi(17)
+ static class Api17Impl {
+     private Api17Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static int generateViewId() {
+         return View.generateViewId();
+     }
@@ -38 +50
+ }
        """
                .trimIndent()

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe static method reference in Kotlin source`() {
        val input = arrayOf(ktSample("androidx.AutofixUnsafeStaticMethodReferenceKotlin"))

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeStaticMethodReferenceKotlin.kt line 27: Extract to static inner class:
@@ -20 +20
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresApi
@@ -27 +29
-             return View.generateViewId()
+             return Api17Impl.generateViewId()
@@ -31 +33
+
+ @RequiresApi(17)
+ internal object Api17Impl {
+     @DoNotInline
+     @JvmStatic
+     fun generateViewId(): Int {
+         return View.generateViewId()
+     }
@@ -32 +42
+ }
        """
                .trimIndent()

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe generic-type method reference in Java source`() {
        val input = arrayOf(javaSample("androidx.AutofixUnsafeGenericMethodReferenceJava"))

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeGenericMethodReferenceJava.java line 34: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline;
+ import androidx.annotation.RequiresApi;
@@ -34 +36
-             return context.getSystemService(serviceClass);
+             return Api23Impl.getSystemService(context, serviceClass);
@@ -38 +40
+
+ @RequiresApi(23)
+ static class Api23Impl {
+     private Api23Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static <T> T getSystemService(Context context, Class<T> serviceClass) {
+         return context.getSystemService(serviceClass);
+     }
@@ -39 +51
+ }
        """
                .trimIndent()

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe generic-type method reference in Kotlin source`() {
        val input = arrayOf(ktSample("androidx.AutofixUnsafeGenericMethodReferenceKotlin"))

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeGenericMethodReferenceKotlin.kt line 30: Extract to static inner class:
@@ -20 +20
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresApi
@@ -30 +32
-             return context.getSystemService<T?>(serviceClass)
+             return Api23Impl.getSystemService(context, serviceClass)
@@ -34 +36
+
+ @RequiresApi(23)
+ internal object Api23Impl {
+     @DoNotInline
+     @JvmStatic
+     fun <T> getSystemService(context: Context, serviceClass: Class<T>): T {
+         return context.getSystemService(serviceClass)
+     }
@@ -35 +45
+ }
        """
                .trimIndent()

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe reference in Java source with existing inner class`() {
        val input =
            arrayOf(javaSample("androidx.AutofixUnsafeReferenceWithExistingClassJava"), RequiresApi)

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeReferenceWithExistingClassJava.java line 36: Extract to static inner class:
@@ -23 +23
+ import androidx.annotation.DoNotInline;
@@ -36 +37
-             view.setBackgroundTintList(new ColorStateList(null, null));
+             Api21Impl.setBackgroundTintList(view, new ColorStateList(null, null));
@@ -46 +47
+
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static void setBackgroundTintList(View view, ColorStateList tint) {
+         view.setBackgroundTintList(tint);
+     }
@@ -47 +58
+ }
        """
                .trimIndent()

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe reference in Kotlin source with existing inner class`() {
        val input =
            arrayOf(ktSample("androidx.AutofixUnsafeReferenceWithExistingClassKotlin"), RequiresApi)

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeReferenceWithExistingClassKotlin.kt line 29: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline
@@ -29 +30
-             view.setBackgroundTintList(ColorStateList(null, null))
+             Api21Impl.setBackgroundTintList(view, ColorStateList(null, null))
@@ -34 +35
+
+ @RequiresApi(21)
+ internal object Api21Impl {
+     @DoNotInline
+     @JvmStatic
+     fun setBackgroundTintList(view: View, tint: ColorStateList): Unit {
+         view.setBackgroundTintList(tint)
+     }
@@ -35 +44
+ }
        """
                .trimIndent()

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe reference in Kotlin source with existing top-level fix`() {
        val input =
            arrayOf(
                ktSample("androidx.AutofixUnsafeReferenceWithExistingTopLevelFixKotlin"),
                RequiresApi,
                DoNotInline,
            )

        val expectedFix =
            """
Fix for src/androidx/Api21Impl.kt line 31: Extract to static inner class:
@@ -31 +31
-     view.setBackgroundTintList(ColorStateList(null, null))
+     Api21Impl.setBackgroundTintList(view, ColorStateList(null, null))
Fix for src/androidx/Api21Impl.kt line 37: Extract to static inner class:
@@ -37 +37
-     val outline = Outline()
+     val outline = Api21Impl.createOutline()
@@ -49 +49
+ @DoNotInline
+ @JvmStatic
+ fun createOutline(): Outline {
+     return Outline()
@@ -50 +54
+ }
Fix for src/androidx/Api21Impl.kt line 38: Extract to static inner class:
@@ -38 +38
-     drawable.getOutline(outline)
+     Api21Impl.getOutline(drawable, outline)
@@ -49 +49
+ @DoNotInline
+ @JvmStatic
+ fun getOutline(drawable: Drawable, outline: Outline): Unit {
+     drawable.getOutline(outline)
@@ -50 +54
+ }
        """
                .trimIndent()

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe reference in Java source when the fix code already exists`() {
        val input =
            arrayOf(
                javaSample("androidx.AutofixUnsafeReferenceWithExistingFix"),
                RequiresApi,
                DoNotInline,
            )

        val expected =
            """
src/androidx/AutofixUnsafeReferenceWithExistingFix.java:37: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeReferenceWithExistingFix is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        view.setBackgroundTintList(new ColorStateList(null, null));
             ~~~~~~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeReferenceWithExistingFix.java:45: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeReferenceWithExistingFix is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        drawable.getOutline(null);
                 ~~~~~~~~~~
2 errors, 0 warnings
        """

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeReferenceWithExistingFix.java line 37: Extract to static inner class:
@@ -37 +37
-         view.setBackgroundTintList(new ColorStateList(null, null));
+         Api21Impl.setBackgroundTintList(view, new ColorStateList(null, null));
Fix for src/androidx/AutofixUnsafeReferenceWithExistingFix.java line 45: Extract to static inner class:
@@ -45 +45
-         drawable.getOutline(null);
+         Api21Impl.getOutline(drawable, null);
@@ -58 +58
-     }
+     @DoNotInline
+ static void getOutline(Drawable drawable, android.graphics.Outline outline) {
+     drawable.getOutline(outline);
@@ -60 +62
+ }
+ }
        """

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe reference in Kotlin source when the fix code already exists`() {
        val input =
            arrayOf(
                ktSample("androidx.AutofixUnsafeReferenceWithExistingFixKotlin"),
                RequiresApi,
                DoNotInline,
            )

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeReferenceWithExistingFixKotlin.kt line 31: Extract to static inner class:
@@ -31 +31
-         view.setBackgroundTintList(ColorStateList(null, null))
+         Api21Impl.setBackgroundTintList(view, ColorStateList(null, null))
Fix for src/androidx/AutofixUnsafeReferenceWithExistingFixKotlin.kt line 37: Extract to static inner class:
@@ -37 +37
-         val outline = Outline()
+         val outline = Api21Impl.createOutline()
@@ -48 +48
-     }
+     @DoNotInline
+ @JvmStatic
+ fun createOutline(): Outline {
+     return Outline()
@@ -50 +53
+ }
+ }
Fix for src/androidx/AutofixUnsafeReferenceWithExistingFixKotlin.kt line 38: Extract to static inner class:
@@ -38 +38
-         drawable.getOutline(outline)
+         Api21Impl.getOutline(drawable, outline)
@@ -48 +48
-     }
+     @DoNotInline
+ @JvmStatic
+ fun getOutline(drawable: Drawable, outline: Outline): Unit {
+     drawable.getOutline(outline)
@@ -50 +53
+ }
+ }
        """

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Detection and auto-fix for qualified expressions (issue 205026874)`() {
        val input =
            arrayOf(
                javaSample("androidx.sample.appcompat.widget.ActionBarBackgroundDrawable"),
                RequiresApi,
            )

        val expected =
            """
src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java:71: Error: This call references a method added in API level 21; however, the containing class androidx.sample.appcompat.widget.ActionBarBackgroundDrawable is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
                mContainer.mSplitBackground.getOutline(outline);
                                            ~~~~~~~~~~
src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java:76: Error: This call references a method added in API level 21; however, the containing class androidx.sample.appcompat.widget.ActionBarBackgroundDrawable is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
                mContainer.mBackground.getOutline(outline);
                                       ~~~~~~~~~~
2 errors, 0 warnings
        """
                .trimIndent()

        val expectedFix =
            """
Fix for src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java line 71: Extract to static inner class:
@@ -25 +25
+ import androidx.annotation.DoNotInline;
@@ -71 +72
-                 mContainer.mSplitBackground.getOutline(outline);
+                 Api21Impl.getOutline(mContainer.mSplitBackground, outline);
@@ -90 +91
+
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static void getOutline(Drawable drawable, Outline outline) {
+         drawable.getOutline(outline);
+     }
@@ -91 +102
+ }
Fix for src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java line 76: Extract to static inner class:
@@ -25 +25
+ import androidx.annotation.DoNotInline;
@@ -76 +77
-                 mContainer.mBackground.getOutline(outline);
+                 Api21Impl.getOutline(mContainer.mBackground, outline);
@@ -90 +91
+
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static void getOutline(Drawable drawable, Outline outline) {
+         drawable.getOutline(outline);
+     }
@@ -91 +102
+ }
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix includes fully qualified class name (issue 205035683, 236721202)`() {
        val input =
            arrayOf(javaSample("androidx.AutofixUnsafeMethodWithQualifiedClass"), RequiresApi)

        val expected =
            """
src/androidx/AutofixUnsafeMethodWithQualifiedClass.java:40: Error: This call references a method added in API level 19; however, the containing class androidx.AutofixUnsafeMethodWithQualifiedClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return builder.setMediaSize(mediaSize);
                       ~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFixDiffs =
            """
Fix for src/androidx/AutofixUnsafeMethodWithQualifiedClass.java line 40: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline;
@@ -40 +41
+         return Api19Impl.setMediaSize(builder, mediaSize);
+     }
+
+ @RequiresApi(19)
+ static class Api19Impl {
+     private Api19Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static PrintAttributes.Builder setMediaSize(PrintAttributes.Builder builder, PrintAttributes.MediaSize mediaSize) {
@@ -43 +54
+ }
        """

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Auto-fix includes fully qualified class name (issue 205035683, 236721202) in Kotlin`() {
        val input =
            arrayOf(ktSample("androidx.AutofixUnsafeMethodWithQualifiedClassKotlin"), RequiresApi)

        val expectedFixDiffs =
            """
Fix for src/androidx/AutofixUnsafeMethodWithQualifiedClassKotlin.kt line 36: Extract to static inner class:
@@ -19 +19
+ import androidx.annotation.DoNotInline
@@ -36 +37
+         return Api19Impl.setMediaSize(builder, mediaSize)
+     }
+
+ @RequiresApi(19)
+ internal object Api19Impl {
+     @DoNotInline
+     @JvmStatic
+     fun setMediaSize(builder: PrintAttributes.Builder, mediaSize: PrintAttributes.MediaSize): PrintAttributes.Builder {
@@ -39 +48
+ }
        """

        check(*input).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Auto-fix for unsafe method call on this`() {
        val input = arrayOf(javaSample("androidx.AutofixUnsafeCallToThis"))

        val expected =
            """
src/androidx/AutofixUnsafeCallToThis.java:39: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeCallToThis is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            getClipToPadding();
            ~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallToThis.java:48: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeCallToThis is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            this.getClipToPadding();
                 ~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallToThis.java:57: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeCallToThis is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            super.getClipToPadding();
                  ~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeCallToThis.java line 39: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline;
+ import androidx.annotation.RequiresApi;
@@ -39 +41
-             getClipToPadding();
+             Api21Impl.getClipToPadding(this);
@@ -60 +62
+
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static boolean getClipToPadding(ViewGroup viewGroup) {
+         return viewGroup.getClipToPadding();
+     }
@@ -61 +73
+ }
Fix for src/androidx/AutofixUnsafeCallToThis.java line 48: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline;
+ import androidx.annotation.RequiresApi;
@@ -48 +50
-             this.getClipToPadding();
+             Api21Impl.getClipToPadding(this);
@@ -60 +62
+
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static boolean getClipToPadding(ViewGroup viewGroup) {
+         return viewGroup.getClipToPadding();
+     }
@@ -61 +73
+ }
        """

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix for unsafe method call on this in Kotlin`() {
        val input = arrayOf(ktSample("androidx.AutofixUnsafeCallToThisKotlin"))

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeCallToThisKotlin.kt line 27: Extract to static inner class:
@@ -20 +20
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresApi
@@ -27 +29
-             getClipToPadding()
+             Api21Impl.getClipToPadding(this)
@@ -44 +46
+
+ @RequiresApi(21)
+ internal object Api21Impl {
+     @DoNotInline
+     @JvmStatic
+     fun getClipToPadding(viewGroup: ViewGroup): Boolean {
+         return viewGroup.getClipToPadding()
+     }
@@ -45 +55
+ }
Fix for src/androidx/AutofixUnsafeCallToThisKotlin.kt line 34: Extract to static inner class:
@@ -20 +20
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresApi
@@ -34 +36
-             this.getClipToPadding()
+             Api21Impl.getClipToPadding(this)
@@ -44 +46
+
+ @RequiresApi(21)
+ internal object Api21Impl {
+     @DoNotInline
+     @JvmStatic
+     fun getClipToPadding(viewGroup: ViewGroup): Boolean {
+         return viewGroup.getClipToPadding()
+     }
@@ -45 +55
+ }
        """

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix for unsafe method call on cast object (issue 206111383)`() {
        val input = arrayOf(javaSample("androidx.AutofixUnsafeCallOnCast"))

        val expected =
            """
src/androidx/AutofixUnsafeCallOnCast.java:32: Error: This call references a method added in API level 28; however, the containing class androidx.AutofixUnsafeCallOnCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            ((DisplayCutout) secretDisplayCutout).getSafeInsetTop();
                                                  ~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeCallOnCast.java line 32: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline;
+ import androidx.annotation.RequiresApi;
@@ -32 +34
-             ((DisplayCutout) secretDisplayCutout).getSafeInsetTop();
+             Api28Impl.getSafeInsetTop((DisplayCutout) secretDisplayCutout);
@@ -35 +37
+
+ @RequiresApi(28)
+ static class Api28Impl {
+     private Api28Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static int getSafeInsetTop(DisplayCutout displayCutout) {
+         return displayCutout.getSafeInsetTop();
+     }
@@ -36 +48
+ }
        """

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix for unsafe method call on cast object (issue 206111383) in Kotlin`() {
        val input = arrayOf(ktSample("androidx.AutofixUnsafeCallOnCastKotlin"))

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeCallOnCastKotlin.kt line 27: Extract to static inner class:
@@ -20 +20
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresApi
@@ -27 +29
-             (secretDisplayCutout as DisplayCutout).getSafeInsetTop()
+             Api28Impl.getSafeInsetTop(secretDisplayCutout as DisplayCutout)
@@ -30 +32
+
+ @RequiresApi(28)
+ internal object Api28Impl {
+     @DoNotInline
+     @JvmStatic
+     fun getSafeInsetTop(displayCutout: DisplayCutout): Int {
+         return displayCutout.getSafeInsetTop()
+     }
@@ -31 +41
+ }
        """

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix with implicit class cast from new return type (issue 214389795)`() {
        val input =
            arrayOf(javaSample("androidx.AutofixUnsafeCallWithImplicitReturnCast"), RequiresApi)

        val expected =
            """
src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java:36: Error: This call references a method added in API level 26; however, the containing class androidx.AutofixUnsafeCallWithImplicitReturnCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return new AdaptiveIconDrawable(null, null);
               ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java:44: Error: This call references a method added in API level 26; however, the containing class androidx.AutofixUnsafeCallWithImplicitReturnCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return new AdaptiveIconDrawable(null, null);
               ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java:52: Error: This call references a method added in API level 26; however, the containing class androidx.AutofixUnsafeCallWithImplicitReturnCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return Icon.createWithAdaptiveBitmap(null);
                    ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java:60: Error: This call references a method added in API level 26; however, the containing class androidx.AutofixUnsafeCallWithImplicitReturnCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return Icon.createWithAdaptiveBitmap(null);
                    ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java:68: Error: This call references a method added in API level 24; however, the containing class androidx.AutofixUnsafeCallWithImplicitReturnCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        useStyle(new Notification.DecoratedCustomViewStyle());
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
5 errors, 0 warnings
        """

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java line 36: Extract to static inner class:
@@ -24 +24
+ import androidx.annotation.DoNotInline;
@@ -36 +37
-         return new AdaptiveIconDrawable(null, null);
+         return Api26Impl.createAdaptiveIconDrawableReturnsDrawable(null, null);
@@ -77 +78
+
+ @RequiresApi(26)
+ static class Api26Impl {
+     private Api26Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static Drawable createAdaptiveIconDrawableReturnsDrawable(Drawable backgroundDrawable, Drawable foregroundDrawable) {
+         return new AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable);
+     }
@@ -78 +89
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java line 44: Extract to static inner class:
@@ -24 +24
+ import androidx.annotation.DoNotInline;
@@ -44 +45
-         return new AdaptiveIconDrawable(null, null);
+         return Api26Impl.createAdaptiveIconDrawable(null, null);
@@ -77 +78
+
+ @RequiresApi(26)
+ static class Api26Impl {
+     private Api26Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static AdaptiveIconDrawable createAdaptiveIconDrawable(Drawable backgroundDrawable, Drawable foregroundDrawable) {
+         return new AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable);
+     }
@@ -78 +89
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java line 52: Extract to static inner class:
@@ -20 +20
+ import android.graphics.Bitmap;
@@ -24 +25
+ import androidx.annotation.DoNotInline;
@@ -52 +54
-         return Icon.createWithAdaptiveBitmap(null);
+         return Api26Impl.createWithAdaptiveBitmapReturnsObject(null);
@@ -77 +79
+
+ @RequiresApi(26)
+ static class Api26Impl {
+     private Api26Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static Object createWithAdaptiveBitmapReturnsObject(Bitmap bits) {
+         return Icon.createWithAdaptiveBitmap(bits);
+     }
@@ -78 +90
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java line 60: Extract to static inner class:
@@ -20 +20
+ import android.graphics.Bitmap;
@@ -24 +25
+ import androidx.annotation.DoNotInline;
@@ -60 +62
-         return Icon.createWithAdaptiveBitmap(null);
+         return Api26Impl.createWithAdaptiveBitmap(null);
@@ -77 +79
+
+ @RequiresApi(26)
+ static class Api26Impl {
+     private Api26Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static Icon createWithAdaptiveBitmap(Bitmap bits) {
+         return Icon.createWithAdaptiveBitmap(bits);
+     }
@@ -78 +90
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCast.java line 68: Extract to static inner class:
@@ -24 +24
+ import androidx.annotation.DoNotInline;
@@ -68 +69
-         useStyle(new Notification.DecoratedCustomViewStyle());
+         useStyle(Api24Impl.createDecoratedCustomViewStyleReturnsStyle());
@@ -77 +78
+
+ @RequiresApi(24)
+ static class Api24Impl {
+     private Api24Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static Notification.Style createDecoratedCustomViewStyleReturnsStyle() {
+         return new Notification.DecoratedCustomViewStyle();
+     }
@@ -78 +89
+ }
        """

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix with implicit class cast from new return type (issue 214389795) in Kotlin`() {
        val input =
            arrayOf(ktSample("androidx.AutofixUnsafeCallWithImplicitReturnCastKotlin"), RequiresApi)

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCastKotlin.kt line 32: Extract to static inner class:
@@ -22 +22
+ import androidx.annotation.DoNotInline
@@ -32 +33
-         return AdaptiveIconDrawable(null, null)
+         return Api26Impl.createAdaptiveIconDrawableReturnsDrawable(null, null)
@@ -65 +66
+
+ @RequiresApi(26)
+ internal object Api26Impl {
+     @DoNotInline
+     @JvmStatic
+     fun createAdaptiveIconDrawableReturnsDrawable(backgroundDrawable: Drawable, foregroundDrawable: Drawable): Drawable {
+         return AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable)
+     }
@@ -66 +75
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCastKotlin.kt line 38: Extract to static inner class:
@@ -22 +22
+ import androidx.annotation.DoNotInline
@@ -38 +39
-         return AdaptiveIconDrawable(null, null)
+         return Api26Impl.createAdaptiveIconDrawable(null, null)
@@ -65 +66
+
+ @RequiresApi(26)
+ internal object Api26Impl {
+     @DoNotInline
+     @JvmStatic
+     fun createAdaptiveIconDrawable(backgroundDrawable: Drawable, foregroundDrawable: Drawable): AdaptiveIconDrawable {
+         return AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable)
+     }
@@ -66 +75
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCastKotlin.kt line 44: Extract to static inner class:
@@ -19 +19
+ import android.graphics.Bitmap
@@ -22 +23
+ import androidx.annotation.DoNotInline
@@ -44 +46
-         return Icon.createWithAdaptiveBitmap(null)
+         return Api26Impl.createWithAdaptiveBitmapReturnsObject(null)
@@ -65 +67
+
+ @RequiresApi(26)
+ internal object Api26Impl {
+     @DoNotInline
+     @JvmStatic
+     fun createWithAdaptiveBitmapReturnsObject(bits: Bitmap): Object {
+         return Icon.createWithAdaptiveBitmap(bits)
+     }
@@ -66 +76
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCastKotlin.kt line 50: Extract to static inner class:
@@ -19 +19
+ import android.graphics.Bitmap
@@ -22 +23
+ import androidx.annotation.DoNotInline
@@ -50 +52
-         return Icon.createWithAdaptiveBitmap(null)
+         return Api26Impl.createWithAdaptiveBitmap(null)
@@ -65 +67
+
+ @RequiresApi(26)
+ internal object Api26Impl {
+     @DoNotInline
+     @JvmStatic
+     fun createWithAdaptiveBitmap(bits: Bitmap): Icon {
+         return Icon.createWithAdaptiveBitmap(bits)
+     }
@@ -66 +76
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitReturnCastKotlin.kt line 56: Extract to static inner class:
@@ -22 +22
+ import androidx.annotation.DoNotInline
@@ -56 +57
-         useStyle(Notification.DecoratedCustomViewStyle())
+         useStyle(Api24Impl.createDecoratedCustomViewStyleReturnsStyle())
@@ -65 +66
+
+ @RequiresApi(24)
+ internal object Api24Impl {
+     @DoNotInline
+     @JvmStatic
+     fun createDecoratedCustomViewStyleReturnsStyle(): Notification.Style {
+         return Notification.DecoratedCustomViewStyle()
+     }
@@ -66 +75
+ }
        """

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix for constructor needs qualified class name (issue 244714253)`() {
        val input =
            arrayOf(javaSample("androidx.AutofixUnsafeConstructorQualifiedClass"), RequiresApi)

        val expected =
            """
src/androidx/AutofixUnsafeConstructorQualifiedClass.java:32: Error: This call references a method added in API level 24; however, the containing class androidx.AutofixUnsafeConstructorQualifiedClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return new Notification.DecoratedCustomViewStyle();
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeConstructorQualifiedClass.java line 32: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline;
@@ -32 +33
+         return Api24Impl.createDecoratedCustomViewStyle();
+     }
+
+ @RequiresApi(24)
+ static class Api24Impl {
+     private Api24Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static Notification.DecoratedCustomViewStyle createDecoratedCustomViewStyle() {
@@ -35 +46
+ }
        """

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix for constructor needs qualified class name (issue 244714253) in Kotlin`() {
        val input =
            arrayOf(ktSample("androidx.AutofixUnsafeConstructorQualifiedClassKotlin"), RequiresApi)

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeConstructorQualifiedClassKotlin.kt line 28: Extract to static inner class:
@@ -19 +19
+ import androidx.annotation.DoNotInline
@@ -28 +29
+         return Api24Impl.createDecoratedCustomViewStyle()
+     }
+
+ @RequiresApi(24)
+ internal object Api24Impl {
+     @DoNotInline
+     @JvmStatic
+     fun createDecoratedCustomViewStyle(): Notification.DecoratedCustomViewStyle {
@@ -31 +40
+ }
        """

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix with implicit class cast from new parameter type (issue 266845827)`() {
        val input =
            arrayOf(javaSample("androidx.AutofixUnsafeCallWithImplicitParamCast"), RequiresApi)

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeCallWithImplicitParamCast.java line 34: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline;
@@ -34 +35
-         style.setBuilder(builder);
+         Api16Impl.setBuilder(style, builder);
@@ -45 +46
+
+ @RequiresApi(16)
+ static class Api16Impl {
+     private Api16Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static void setBuilder(Notification.Style style, Notification.Builder builder) {
+         style.setBuilder(builder);
+     }
@@ -46 +57
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitParamCast.java line 43: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline;
@@ -43 +44
-         builder.extend(extender);
+         Api20Impl.extend(builder, extender);
@@ -45 +46
+
+ @RequiresApi(20)
+ static class Api20Impl {
+     private Api20Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static Notification.Builder extend(Notification.Builder builder, Notification.CarExtender extender) {
+         return builder.extend(extender);
+     }
@@ -46 +57
+ }
        """

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix with implicit class cast from new parameter type (issue 266845827) in Kotlin`() {
        val input =
            arrayOf(ktSample("androidx.AutofixUnsafeCallWithImplicitParamCastKotlin"), RequiresApi)

        val expectedFix =
            """
Fix for src/androidx/AutofixUnsafeCallWithImplicitParamCastKotlin.kt line 32: Extract to static inner class:
@@ -19 +19
+ import androidx.annotation.DoNotInline
@@ -32 +33
-         style.setBuilder(builder)
+         Api16Impl.setBuilder(style, builder)
@@ -43 +44
+
+ @RequiresApi(16)
+ internal object Api16Impl {
+     @DoNotInline
+     @JvmStatic
+     fun setBuilder(style: Notification.Style, builder: Notification.Builder): Unit {
+         style.setBuilder(builder)
+     }
@@ -44 +53
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitParamCastKotlin.kt line 41: Extract to static inner class:
@@ -19 +19
+ import androidx.annotation.DoNotInline
@@ -41 +42
-         builder.extend(extender)
+         Api20Impl.extend(builder, extender)
@@ -43 +44
+
+ @RequiresApi(20)
+ internal object Api20Impl {
+     @DoNotInline
+     @JvmStatic
+     fun extend(builder: Notification.Builder, extender: Notification.CarExtender): Notification.Builder {
+         return builder.extend(extender)
+     }
@@ -44 +53
+ }
        """

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix for method with varargs that are implicitly cast (issue 266845827)`() {
        val input =
            arrayOf(javaSample("androidx.AutofixOnUnsafeCallWithImplicitVarArgsCast"), RequiresApi)

        val expected =
            """
src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCast.java:35: Error: This call references a method added in API level 27; however, the containing class androidx.AutofixOnUnsafeCallWithImplicitVarArgsCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        adapter.setAutofillOptions();
                ~~~~~~~~~~~~~~~~~~
src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCast.java:43: Error: This call references a method added in API level 27; however, the containing class androidx.AutofixOnUnsafeCallWithImplicitVarArgsCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        adapter.setAutofillOptions(vararg);
                ~~~~~~~~~~~~~~~~~~
src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCast.java:52: Error: This call references a method added in API level 27; however, the containing class androidx.AutofixOnUnsafeCallWithImplicitVarArgsCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        adapter.setAutofillOptions(vararg1, vararg2, vararg3);
                ~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """

        val expectedFix =
            """
Fix for src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCast.java line 35: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline;
@@ -35 +36
-         adapter.setAutofillOptions();
+         Api27Impl.setAutofillOptions(adapter);
@@ -54 +55
+
+ @RequiresApi(27)
+ static class Api27Impl {
+     private Api27Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static void setAutofillOptions(BaseAdapter baseAdapter, CharSequence... options) {
+         baseAdapter.setAutofillOptions(options);
+     }
@@ -55 +66
+ }
Fix for src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCast.java line 43: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline;
@@ -43 +44
-         adapter.setAutofillOptions(vararg);
+         Api27Impl.setAutofillOptions(adapter, vararg);
@@ -54 +55
+
+ @RequiresApi(27)
+ static class Api27Impl {
+     private Api27Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static void setAutofillOptions(BaseAdapter baseAdapter, CharSequence... options) {
+         baseAdapter.setAutofillOptions(options);
+     }
@@ -55 +66
+ }
Fix for src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCast.java line 52: Extract to static inner class:
@@ -21 +21
+ import androidx.annotation.DoNotInline;
@@ -52 +53
-         adapter.setAutofillOptions(vararg1, vararg2, vararg3);
+         Api27Impl.setAutofillOptions(adapter, vararg1, vararg2, vararg3);
@@ -54 +55
+
+ @RequiresApi(27)
+ static class Api27Impl {
+     private Api27Impl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static void setAutofillOptions(BaseAdapter baseAdapter, CharSequence... options) {
+         baseAdapter.setAutofillOptions(options);
+     }
@@ -55 +66
+ }
        """

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix for method with varargs that are implicitly cast (issue 266845827) in Kotlin`() {
        val input =
            arrayOf(
                ktSample("androidx.AutofixOnUnsafeCallWithImplicitVarArgsCastKotlin"),
                RequiresApi,
            )

        val expectedFix =
            """
Fix for src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCastKotlin.kt line 30: Extract to static inner class:
@@ -19 +19
+ import androidx.annotation.DoNotInline
@@ -30 +31
-         adapter.setAutofillOptions()
+         Api27Impl.setAutofillOptions(adapter)
@@ -49 +50
+
+ @RequiresApi(27)
+ internal object Api27Impl {
+     @DoNotInline
+     @JvmStatic
+     fun setAutofillOptions(baseAdapter: BaseAdapter, vararg options: CharSequence): Unit {
+         baseAdapter.setAutofillOptions(options)
+     }
@@ -50 +59
+ }
Fix for src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCastKotlin.kt line 36: Extract to static inner class:
@@ -19 +19
+ import androidx.annotation.DoNotInline
@@ -36 +37
-         adapter.setAutofillOptions(vararg)
+         Api27Impl.setAutofillOptions(adapter, vararg)
@@ -49 +50
+
+ @RequiresApi(27)
+ internal object Api27Impl {
+     @DoNotInline
+     @JvmStatic
+     fun setAutofillOptions(baseAdapter: BaseAdapter, vararg options: CharSequence): Unit {
+         baseAdapter.setAutofillOptions(options)
+     }
@@ -50 +59
+ }
Fix for src/androidx/AutofixOnUnsafeCallWithImplicitVarArgsCastKotlin.kt line 47: Extract to static inner class:
@@ -19 +19
+ import androidx.annotation.DoNotInline
@@ -47 +48
-         adapter.setAutofillOptions(vararg1, vararg2, vararg3)
+         Api27Impl.setAutofillOptions(adapter, vararg1, vararg2, vararg3)
@@ -49 +50
+
+ @RequiresApi(27)
+ internal object Api27Impl {
+     @DoNotInline
+     @JvmStatic
+     fun setAutofillOptions(baseAdapter: BaseAdapter, vararg options: CharSequence): Unit {
+         baseAdapter.setAutofillOptions(options)
+     }
@@ -50 +59
+ }
        """

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Detection and auto-fix of flagged APIs in Kotlin sources`() {
        val input =
            arrayOf(
                kotlin(
                    """
                    package android.test

                    import android.annotation.FlaggedApi

                    object FlaggedApiContainer {
                        @FlaggedApi("android.test.myFlag")
                        fun flaggedApi() {
                        }
                    }
                    """
                        .trimIndent()
                ),
                kotlin(
                    """
                    package com.example

                    import android.test.FlaggedApiContainer

                    fun callFlaggedApi() {
                        FlaggedApiContainer.flaggedApi()
                    }
                    """
                        .trimIndent()
                ),
                FlaggedApi,
            )

        val expected =
            """
src/com/example/test.kt:6: Error: This call references a method guarded by Trunk Stable flag "android.test.myFlag"; however, the containing class com.example.TestKt is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
    FlaggedApiContainer.flaggedApi()
                        ~~~~~~~~~~
1 error
        """
                .trimIndent()

        val expectedFixDiffs =
            """
Fix for src/com/example/test.kt line 6: Extract to static inner class:
@@ -4 +4
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresAconfigFlag
@@ -5 +7
+
+ @RequiresAconfigFlag("android.test.myFlag")
+ internal object FlagMyFlagImpl {
+     @DoNotInline
+     @JvmStatic
+     fun flaggedApi(flaggedApiContainer: FlaggedApiContainer): Unit {
+         flaggedApiContainer.flaggedApi()
+     }
+ }
@@ -6 +17
-     FlaggedApiContainer.flaggedApi()
+     FlagMyFlagImpl.flaggedApi(FlaggedApiContainer)
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Detection and auto-fix of flagged APIs in Java sources`() {
        val input =
            arrayOf(
                java(
                    """
                    package android.test;

                    import android.annotation.FlaggedApi;

                    public class FlaggedApiContainer {
                        @FlaggedApi("test.pkg.myFlag")
                        public static void flaggedApi() {
                        }
                    }
                    """
                        .trimIndent()
                ),
                java(
                    """
                    package com.example;

                    import android.test.FlaggedApiContainer;

                    class MyClass {
                        void callFlaggedApi() {
                           FlaggedApiContainer.flaggedApi();
                        }
                    }
                    """
                        .trimIndent()
                ),
                FlaggedApi,
            )

        val expected =
            """
src/com/example/MyClass.java:7: Error: This call references a method guarded by Trunk Stable flag "test.pkg.myFlag"; however, the containing class com.example.MyClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
       FlaggedApiContainer.flaggedApi();
                           ~~~~~~~~~~
1 error
        """
                .trimIndent()

        val expectedFixDiffs =
            """
Fix for src/com/example/MyClass.java line 7: Extract to static inner class:
@@ -4 +4
+ import androidx.annotation.DoNotInline;
+ import androidx.annotation.RequiresAconfigFlag;
@@ -7 +9
-        FlaggedApiContainer.flaggedApi();
+        FlagMyFlagImpl.flaggedApi();
@@ -9 +11
+
+ @RequiresAconfigFlag("test.pkg.myFlag")
+ static class FlagMyFlagImpl {
+     private FlagMyFlagImpl() {
+         // This class is not instantiable.
+     }
+     @DoNotInline
+     static void flaggedApi() {
+         FlaggedApiContainer.flaggedApi();
+     }
+ }
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Detection and auto-fix of flagged APIs in Kotlin sources without existing outline`() {
        val input =
            arrayOf(
                javaSample("android.flagging.FlaggedApiContainer"),
                ktSample("flaggedapi.FlaggedUsageWithoutOutline"),
                FlaggedApi,
                RequiresAconfigFlag,
                Flags,
            )

        val expected =
            """
src/flaggedapi/FlaggedUsageWithoutOutline.kt:26: Error: This call references a method guarded by Trunk Stable flag "flaggedapi.myFlag"; however, the containing class flaggedapi.FlaggedUsageWithoutOutline is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            FlaggedApiContainer.innerApi()
                                ~~~~~~~~
src/flaggedapi/FlaggedUsageWithoutOutline.kt:31: Error: This call references a method guarded by Trunk Stable flag "flaggedapi.myFlag"; however, the containing class flaggedapi.FlaggedUsageWithoutOutline is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        FlaggedApiContainer.innerApi()
                            ~~~~~~~~
2 errors
        """
                .trimIndent()

        // Due to b/417243329 these do not reflect actual IDE auto-fix behavior. The flag string
        // used by the `@RequiresAconfigFlag` annotation is actually the full flag string, e.g.
        // "flaggedapi.myFlag", in practice.
        val expectedFixDiffs =
            """
Fix for src/flaggedapi/FlaggedUsageWithoutOutline.kt line 26: Extract to static inner class:
@@ -20 +20
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresAconfigFlag
@@ -26 +28
-             FlaggedApiContainer.innerApi()
+             FlagMyFlagImpl.innerApi()
@@ -33 +35
+
+ @RequiresAconfigFlag("flaggedapi.myFlag")
+ internal object FlagMyFlagImpl {
+     @DoNotInline
+     @JvmStatic
+     fun innerApi(): Boolean {
+         return FlaggedApiContainer.innerApi()
+     }
@@ -34 +44
+ }
Fix for src/flaggedapi/FlaggedUsageWithoutOutline.kt line 31: Extract to static inner class:
@@ -20 +20
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresAconfigFlag
@@ -31 +33
-         FlaggedApiContainer.innerApi()
+         FlagMyFlagImpl.innerApi()
@@ -33 +35
+
+ @RequiresAconfigFlag("flaggedapi.myFlag")
+ internal object FlagMyFlagImpl {
+     @DoNotInline
+     @JvmStatic
+     fun innerApi(): Boolean {
+         return FlaggedApiContainer.innerApi()
+     }
@@ -34 +44
+ }
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Detection and auto-fix of flagged APIs in Kotlin sources with existing outline`() {
        val input =
            arrayOf(
                javaSample("android.flagging.FlaggedApiContainer"),
                ktSample("flaggedapi.FlaggedUsageInOutline"),
                FlaggedApi,
                RequiresAconfigFlag,
                Flags,
            )

        val expected =
            """
No warnings.
        """
                .trimIndent()

        check(*input).expect(expected)
    }

    @Test
    fun `Detection and auto-fix of flagged APIs in Kotlin sources with type conversion`() {
        val input =
            arrayOf(
                javaSample("android.flagging.FlaggedApiContainer"),
                ktSample("flaggedapi.AutofixUnsafeUsageWithTypeConversion"),
                FlaggedApi,
                RequiresAconfigFlag,
                Flags,
            )

        // Due to b/417243329 these do not reflect actual IDE auto-fix behavior. The flag string
        // used by the `@RequiresAconfigFlag` annotation is actually the full flag string, e.g.
        // "flaggedapi.myFlag", in practice. Additionally, we don't handle adding generic types
        // to the generated call -- that's fine, the developer can easily fix that.
        val expectedFix =
            """
Fix for src/flaggedapi/AutofixUnsafeUsageWithTypeConversion.kt line 25: Extract to static inner class:
@@ -20 +20
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresAconfigFlag
@@ -25 +27
-             val resultA = FlaggedApiContainer.apiWithTypeArgument(null)
+             val resultA = FlagMyFlagImpl.apiWithTypeArgument(null)
@@ -30 +32
+
+ @RequiresAconfigFlag("flaggedapi.myFlag")
+ internal object FlagMyFlagImpl {
+     @DoNotInline
+     @JvmStatic
+     fun apiWithTypeArgument(param: BiConsumer<Integer, Float>): List<Array<Int>> {
+         return FlaggedApiContainer.apiWithTypeArgument(param)
+     }
@@ -31 +41
+ }
Fix for src/flaggedapi/AutofixUnsafeUsageWithTypeConversion.kt line 26: Extract to static inner class:
@@ -20 +20
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresAconfigFlag
@@ -26 +28
-             val resultB = FlaggedApiContainer.apiWithGenericType<Int?, Float>(null)
+             val resultB = FlagMyFlagImpl.apiWithGenericType(null)
@@ -30 +32
+
+ @RequiresAconfigFlag("flaggedapi.myFlag")
+ internal object FlagMyFlagImpl {
+     @DoNotInline
+     @JvmStatic
+     fun <T, R> apiWithGenericType(param: R): T {
+         return FlaggedApiContainer.apiWithGenericType(param)
+     }
@@ -31 +41
+ }
Fix for src/flaggedapi/AutofixUnsafeUsageWithTypeConversion.kt line 27: Extract to static inner class:
@@ -20 +20
+ import androidx.annotation.DoNotInline
+ import androidx.annotation.RequiresAconfigFlag
@@ -27 +29
-             val resultC = FlaggedApiContainer.apiWithTwoDimensionalArray(null)
+             val resultC = FlagMyFlagImpl.apiWithTwoDimensionalArray(null)
@@ -30 +32
+
+ @RequiresAconfigFlag("flaggedapi.myFlag")
+ internal object FlagMyFlagImpl {
+     @DoNotInline
+     @JvmStatic
+     fun apiWithTwoDimensionalArray(param: Array<Int>): Array<Array<Float>> {
+         return FlaggedApiContainer.apiWithTwoDimensionalArray(param)
+     }
@@ -31 +41
+ }
        """
                .trimIndent()

        check(*input).expectFixDiffs(expectedFix)
    }
}
