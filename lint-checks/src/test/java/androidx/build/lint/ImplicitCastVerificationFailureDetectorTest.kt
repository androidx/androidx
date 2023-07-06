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

package androidx.build.lint

import androidx.build.lint.Stubs.Companion.DoNotInline
import androidx.build.lint.Stubs.Companion.RequiresApi
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ImplicitCastVerificationFailureDetectorTest : AbstractLintDetectorTest(
    useDetector = ClassVerificationFailureDetector(),
    useIssues = listOf(ClassVerificationFailureDetector.IMPLICIT_CAST_ISSUE),
    stubs = arrayOf(
        // AndroidManifest with minSdkVersion=14
        manifest().minSdk(14),
        RequiresApi,
        DoNotInline,
    ),
) {
    @Test
    fun `Unsafe implicit cast for method argument`() {
        val input = arrayOf(
            java("""
                package java.androidx;

                import android.app.Notification;
                import androidx.annotation.DoNotInline;
                import androidx.annotation.RequiresApi;

                public class UnsafeImplicitCastAsMethodArgumentJava {
                    @RequiresApi(24)
                    public void setBuilder(Notification.MessagingStyle style,
                            Notification.Builder builder) {
                        Api16Impl.setBuilder(style, builder);
                    }

                    @RequiresApi(16)
                    static class Api16Impl {
                        private Api16Impl() {}
                        @DoNotInline
                        static void setBuilder(Notification.Style style,
                                Notification.Builder builder) {
                            style.setBuilder(builder);
                        }
                    }
                }
            """.trimIndent()),
            kotlin("""
                package java.androidx

                import android.app.Notification
                import androidx.annotation.DoNotInline
                import androidx.annotation.RequiresApi

                class UnsafeImplicitCastAsMethodArgumentKotlin {
                    @RequiresApi(24)
                    fun setBuilder(style: Notification.MessagingStyle,
                            builder: Notification.Builder) {
                        Api16Impl.setBuilder(style, builder);
                    }

                    @RequiresApi(16)
                    private object Api16Impl {
                        @JvmStatic
                        @DoNotInline
                        fun setBuilder(style: Notification.Style, builder: Notification.Builder) {
                            style.setBuilder(builder)
                        }
                    }
                }
            """.trimIndent()),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/UnsafeImplicitCastAsMethodArgumentJava.java:11: Error: This expression has type android.app.Notification.MessagingStyle (introduced in API level 24) but it used as type android.app.Notification.Style (introduced in API level 16). Run-time class verification will not be able to validate this implicit cast on devices between these API levels. [ImplicitCastClassVerificationFailure]
        Api16Impl.setBuilder(style, builder);
                             ~~~~~
src/java/androidx/UnsafeImplicitCastAsMethodArgumentKotlin.kt:11: Error: This expression has type android.app.Notification.MessagingStyle (introduced in API level 24) but it used as type android.app.Notification.Style (introduced in API level 16). Run-time class verification will not be able to validate this implicit cast on devices between these API levels. [ImplicitCastClassVerificationFailure]
        Api16Impl.setBuilder(style, builder);
                             ~~~~~
2 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/UnsafeImplicitCastAsMethodArgumentJava.java line 11: Extract to static inner class:
@@ -11 +11
-         Api16Impl.setBuilder(style, builder);
+         Api16Impl.setBuilder(Api24Impl.castToStyle(style), builder);
@@ -23 +23
+ @RequiresApi(24)
+ static class Api24Impl {
+     private Api24Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static Notification.Style castToStyle(Notification.MessagingStyle messagingStyle) {
+         return messagingStyle;
+     }
+
@@ -24 +35
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Unsafe implicit cast within catch block`() {
        val input = arrayOf(
            java("""
                package java.androidx;

                import android.app.Presentation;
                import android.util.Log;
                import android.view.WindowManager;
                import androidx.annotation.DoNotInline;
                import androidx.annotation.RequiresApi;

                public class UnsafeImplicitCastInCatchBlockJava {
                    @RequiresApi(17)
                    public void tryShowPresentation(Presentation presentation) {
                        try {
                            Api17Impl.show(presentation);
                        } catch (WindowManager.InvalidDisplayException e) {
                            Log.w("Error", "Couldn't show presentation!", e);
                        }
                    }

                    @RequiresApi(17)
                    static class Api17Impl {
                        private Api16Impl() {}
                        @DoNotInline
                        static void show(Presentation presentation) {
                            presentation.show();
                        }
                    }
                }
            """.trimIndent()),
            kotlin("""
                package java.androidx

                import android.app.Presentation
                import android.util.Log
                import android.view.WindowManager
                import androidx.annotation.DoNotInline
                import androidx.annotation.RequiresApi

                class UnsafeImplicitCastInCatchBlockKotlin {
                    @RequiresApi(17)
                    fun tryShowPresentation(presentation: Presentation) {
                        try {
                            Api17Impl.show(presentation)
                        } catch (e: WindowManager.InvalidDisplayException) {
                            Log.w("Error", "Couldn't show presentation!", e)
                        }
                    }

                    @RequiresApi(17)
                    private object Api17Impl {
                        @JvmStatic
                        @DoNotInline
                        fun show(presentation: Presentation) {
                            presentation.show()
                        }
                    }
                }
            """.trimIndent()),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/UnsafeImplicitCastInCatchBlockJava.java:15: Error: This expression has type android.view.WindowManager.InvalidDisplayException (introduced in API level 17) but it used as type java.lang.Throwable (introduced in API level 1). Run-time class verification will not be able to validate this implicit cast on devices between these API levels. [ImplicitCastClassVerificationFailure]
            Log.w("Error", "Couldn't show presentation!", e);
                                                          ~
src/java/androidx/UnsafeImplicitCastInCatchBlockKotlin.kt:15: Error: This expression has type android.view.WindowManager.InvalidDisplayException (introduced in API level 17) but it used as type java.lang.Throwable (introduced in API level 1). Run-time class verification will not be able to validate this implicit cast on devices between these API levels. [ImplicitCastClassVerificationFailure]
            Log.w("Error", "Couldn't show presentation!", e)
                                                          ~
2 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/UnsafeImplicitCastInCatchBlockJava.java line 15: Extract to static inner class:
@@ -15 +15
-             Log.w("Error", "Couldn't show presentation!", e);
+             Log.w("Error", "Couldn't show presentation!", Api17Impl.castToThrowable(e));
@@ -26 +26
-     }
+     @DoNotInline
+ static java.lang.Throwable castToThrowable(WindowManager.InvalidDisplayException invalidDisplayException) {
+     return invalidDisplayException;
@@ -28 +30
+ }
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Unsafe implicit cast in assignment statement`() {
        val input = arrayOf(
            java("""
                package java.androidx;

                import android.app.Notification;
                import androidx.annotation.RequiresApi;

                public class UnsafeImplicitCastInAssignmentJava {
                    Notification.Style style;

                    @RequiresApi(24)
                    public void setNotificationStyle(Notification.MessagingStyle messagingStyle) {
                        style = messagingStyle;
                    }
                }
            """.trimIndent()),
            kotlin("""
                package java.androidx

                import android.app.Notification
                import androidx.annotation.RequiresApi

                class UnsafeImplicitCastInAssignmentKotlin {
                    lateinit var style: Notification.Style

                    @RequiresApi(24)
                    fun setNotificationStyle(messagingStyle: Notification.MessagingStyle) {
                        style = messagingStyle
                    }
                }
            """.trimIndent()),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/UnsafeImplicitCastInAssignmentJava.java:11: Error: This expression has type android.app.Notification.MessagingStyle (introduced in API level 24) but it used as type android.app.Notification.Style (introduced in API level 16). Run-time class verification will not be able to validate this implicit cast on devices between these API levels. [ImplicitCastClassVerificationFailure]
        style = messagingStyle;
                ~~~~~~~~~~~~~~
src/java/androidx/UnsafeImplicitCastInAssignmentKotlin.kt:11: Error: This expression has type android.app.Notification.MessagingStyle (introduced in API level 24) but it used as type android.app.Notification.Style (introduced in API level 16). Run-time class verification will not be able to validate this implicit cast on devices between these API levels. [ImplicitCastClassVerificationFailure]
        style = messagingStyle
                ~~~~~~~~~~~~~~
2 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/UnsafeImplicitCastInAssignmentJava.java line 11: Extract to static inner class:
@@ -11 +11
-         style = messagingStyle;
+         style = Api24Impl.castToStyle(messagingStyle);
@@ -13 +13
+ @RequiresApi(24)
+ static class Api24Impl {
+     private Api24Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static Notification.Style castToStyle(Notification.MessagingStyle messagingStyle) {
+         return messagingStyle;
+     }
+
@@ -14 +25
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Unsafe implicit cast on return`() {
        val input = arrayOf(
            java("""
                package java.androidx;

                import android.app.Notification;
                import androidx.annotation.RequiresApi;

                public class ImplicitCastOnReturnJava {
                    @RequiresApi(24)
                    public Notification.Style convertStyle(Notification.MessagingStyle style) {
                        return style;
                    }
                }
            """.trimIndent()),
            kotlin("""
                package java.androidx

                import android.app.Notification
                import androidx.annotation.RequiresApi

                class ImplicitCastOnReturnKotlin {
                    @RequiresApi(24)
                    fun convertStyle(style: Notification.MessagingStyle): Notification.Style {
                        return style
                    }
                }
            """.trimIndent()),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/ImplicitCastOnReturnJava.java:9: Error: This expression has type android.app.Notification.MessagingStyle (introduced in API level 24) but it used as type android.app.Notification.Style (introduced in API level 16). Run-time class verification will not be able to validate this implicit cast on devices between these API levels. [ImplicitCastClassVerificationFailure]
        return style;
               ~~~~~
src/java/androidx/ImplicitCastOnReturnKotlin.kt:9: Error: This expression has type android.app.Notification.MessagingStyle (introduced in API level 24) but it used as type android.app.Notification.Style (introduced in API level 16). Run-time class verification will not be able to validate this implicit cast on devices between these API levels. [ImplicitCastClassVerificationFailure]
        return style
               ~~~~~
2 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/ImplicitCastOnReturnJava.java line 9: Extract to static inner class:
@@ -9 +9
-         return style;
+         return Api24Impl.castToStyle(style);
@@ -11 +11
+ @RequiresApi(24)
+ static class Api24Impl {
+     private Api24Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static Notification.Style castToStyle(Notification.MessagingStyle messagingStyle) {
+         return messagingStyle;
+     }
+
@@ -12 +23
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Unsafe implicit cast of method call result`() {
        val input = arrayOf(
            java("""
                package java.androidx;

                import android.graphics.drawable.AdaptiveIconDrawable;
                import android.graphics.drawable.Drawable;
                import androidx.annotation.DoNotInline;
                import androidx.annotation.RequiresApi;

                public class ImplicitCastOfMethodCallResultJava {
                    @RequiresApi(26)
                    public Drawable createAdaptiveIconDrawable() {
                        return Api26Impl.createAdaptiveIconDrawable(null, null);
                    }

                    @RequiresApi(26)
                    static class Api26Impl {
                        private Api26Impl() {}
                        @DoNotInline
                        static AdaptiveIconDrawable createAdaptiveIconDrawable(
                                Drawable backgroundDrawable, Drawable foregroundDrawable) {
                            return new AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable);
                        }
                    }
                }
            """.trimIndent()),
            kotlin("""
                package java.androidx

                import android.graphics.drawable.AdaptiveIconDrawable
                import android.graphics.drawable.Drawable
                import androidx.annotation.DoNotInline
                import androidx.annotation.RequiresApi

                class ImplicitCastOfMethodCallResultKotlin {
                    @RequiresApi(26)
                    fun createAdaptiveIconDrawable(): Drawable =
                        Api26Impl.createAdaptiveIconDrawable(null, null)

                    @RequiresApi(26)
                    object Api26Impl {
                        @JvmStatic
                        @DoNotInline
                        fun createAdaptiveIconDrawable(backgroundDrawable: Drawable,
                                foregroundDrawable: Drawable): AdaptiveIconDrawable {
                            return AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable)
                        }
                    }
                }
            """.trimIndent()),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/ImplicitCastOfMethodCallResultJava.java:11: Error: This expression has type android.graphics.drawable.AdaptiveIconDrawable (introduced in API level 26) but it used as type android.graphics.drawable.Drawable (introduced in API level 1). Run-time class verification will not be able to validate this implicit cast on devices between these API levels. [ImplicitCastClassVerificationFailure]
        return Api26Impl.createAdaptiveIconDrawable(null, null);
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/java/androidx/ImplicitCastOfMethodCallResultKotlin.kt:11: Error: This expression has type android.graphics.drawable.AdaptiveIconDrawable (introduced in API level 26) but it used as type android.graphics.drawable.Drawable (introduced in API level 1). Run-time class verification will not be able to validate this implicit cast on devices between these API levels. [ImplicitCastClassVerificationFailure]
        Api26Impl.createAdaptiveIconDrawable(null, null)
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/java/androidx/ImplicitCastOfMethodCallResultJava.java line 11: Extract to static inner class:
@@ -11 +11
-         return Api26Impl.createAdaptiveIconDrawable(null, null);
+         return Api26Impl.castToDrawable(Api26Impl.createAdaptiveIconDrawable(null, null));
@@ -22 +22
-     }
+     @DoNotInline
+ static Drawable castToDrawable(AdaptiveIconDrawable adaptiveIconDrawable) {
+     return adaptiveIconDrawable;
@@ -24 +26
+ }
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Safe implicit cast to object`() {
        val input = arrayOf(
            java("""
                package java.androidx;

                import android.app.Notification;
                import androidx.annotation.RequiresApi;

                public class SafeImplicitCastToObjectJava {
                    Object style;

                    @RequiresApi(24)
                    public void setNotificationStyle(Notification.MessagingStyle messagingStyle) {
                        style = messagingStyle;
                    }
                }
            """.trimIndent()),
            kotlin("""
                package java.androidx

                import android.app.Notification
                import androidx.annotation.RequiresApi

                class SafeImplicitCastToObjectKotlin {
                    lateinit var style: Any

                    @RequiresApi(24)
                    fun setNotificationStyle(messagingStyle: Notification.MessagingStyle) {
                        style = messagingStyle
                    }
                }
            """.trimIndent()),
        )

        check(*input).expectClean()
    }

    @Test
    fun `Safe explicit cast`() {
        val input = arrayOf(
            java("""
                package java.androidx;

                import android.app.Notification;
                import androidx.annotation.RequiresApi;

                public class SafeExplicitCastJava {
                    Notification.Style style;

                    @RequiresApi(24)
                    public void setNotificationStyle(Notification.MessagingStyle messagingStyle) {
                        style = (Notification.Style) messagingStyle;
                    }
                }
            """.trimIndent()),
            kotlin("""
                package java.androidx

                import android.app.Notification
                import androidx.annotation.RequiresApi

                class SafeExplicitCastKotlin {
                    lateinit var style: Notification.Style

                    @RequiresApi(24)
                    fun setNotificationStyle(messagingStyle: Notification.MessagingStyle) {
                        style = messagingStyle as Notification.Style
                    }
                }
            """.trimIndent()),
        )

        check(*input).expectClean()
    }

    @Test
    fun `Safe implicit cast between classes from the same API level`() {
        val input = arrayOf(
            java("""
                package java.androidx;

                import android.media.tv.BroadcastInfoResponse;
                import android.media.tv.PesResponse;
                import androidx.annotation.RequiresApi;

                public class SafeImplicitCastSameApiLevelJava {
                    BroadcastInfoResponse response;

                    @RequiresApi(33)
                    public void setResponse(PesResponse pesResponse) {
                        response = pesResponse;
                    }
                }
            """.trimIndent()),
            kotlin("""
                package java.androidx

                import android.media.tv.BroadcastInfoResponse
                import android.media.tv.PesResponse
                import androidx.annotation.RequiresApi

                class SafeImplicitCastSameApiLevelKotlin {
                    lateinit var response: BroadcastInfoResponse

                    @RequiresApi(33)
                    fun setResponse(pesResponse: PesResponse) {
                        response = pesResponse
                    }
                }
            """.trimIndent()),
        )

        check(*input).expectClean()
    }

    @Test
    fun `Safe implicit cast within @RequiresApi class`() {
        val input = arrayOf(
            java("""
                package java.androidx;

                import android.app.Notification;

                import androidx.annotation.DoNotInline;
                import androidx.annotation.RequiresApi;

                @RequiresApi(24)
                static class SafeImplicitCastWithRequiresApiJava {
                    private SafeImplicitCastWithRequiresApi() {}
                    @DoNotInline
                    static void extend(Notification.Builder builder,
                            Notification.CarExtender extender) {
                        builder.extend(extender);
                    }
                }
            """.trimIndent()),
            kotlin("""
                package java.androidx

                import android.app.Notification
                import androidx.annotation.DoNotInline;
                import androidx.annotation.RequiresApi;

                @RequiresApi(24)
                object SafeImplicitCastWithRequiresApiKotlin {
                    @JvmStatic
                    @DoNotInline
                    fun extend(builder: Notification.Builder, extender: Notification.CarExtender) {
                        builder.extend(extender)
                    }
                }
            """.trimIndent()),
        )

        check(*input).expectClean()
    }

    @Test
    fun `Safe implicit cast from null value`() {
        val input = arrayOf(
            java("""
                package java.androidx;

                import android.app.Notification;
                import androidx.annotation.RequiresApi;

                public class SafeCastFromNullJava {
                    @RequiresApi(24)
                    public Notification.MessagingStyle getStyle() {
                        return null;
                    }
                }
            """.trimIndent()),
            kotlin("""
                package java.androidx

                import android.app.Notification
                import androidx.annotation.RequiresApi

                class SafeCastFromNullKotlin {
                    @RequiresApi(24)
                    fun getStyle(): Notification.MessagingStyle? {
                        return null
                    }
                }
            """.trimIndent()),
        )

        check(*input).expectClean()
    }

    @Test
    fun `Safe implicit cast from type introduced earlier than the minSdk`() {
        val input = arrayOf(
            java("""
                package java.androidx;

                import android.app.FragmentBreadCrumbs;
                import android.view.ViewGroup;

                public class SafeCastFromPreMinSdkClassJava {
                    ViewGroup viewGroup;

                    public void setViewGroup(FragmentBreadCrumbs breadCrumbs) {
                        // FragmentBreadCrumbs was introduced in API level 11
                        viewGroup = breadCrumbs;
                    }
                }
            """.trimIndent()),
            kotlin("""
                package java.androidx

                import android.app.FragmentBreadCrumbs
                import android.view.ViewGroup

                class SafeCastFromPreMinSdkClassKotlin {
                    lateinit var viewGroup: ViewGroup

                    fun setViewGroup(breadCrumbs: FragmentBreadCrumbs) {
                        // FragmentBreadCrumbs was introduced in API level 11
                        viewGroup = breadCrumbs
                    }
                }
            """.trimIndent()),
        )

        check(*input).expectClean()
    }

    @Test
    fun `Unsafe implicit cast to varargs method`() {
        val input = arrayOf(
            java("""
                package java.androidx;

                import android.icu.number.FormattedNumber;
                import android.widget.BaseAdapter;
                import androidx.annotation.DoNotInline;
                import androidx.annotation.RequiresApi;

                public class UnsafeCastToVarargs() {
                    @RequiresApi(30)
                    public void callVarArgsMethod(BaseAdapter adapter, FormattedNumber vararg1, FormattedNumber vararg2, FormattedNumber vararg3) {
                        Api27Impl.setAutofillOptions(adapter, vararg1, vararg2, vararg3);
                    }

                    @RequiresApi(27)
                    static class Api27Impl {
                        private Api27Impl() {}
                        @DoNotInline
                        static void setAutofillOptions(BaseAdapter baseAdapter, CharSequence... options) {
                            baseAdapter.setAutofillOptions(baseAdapter, options);
                        }
                    }
                }
            """.trimIndent())
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/java/androidx/UnsafeCastToVarargs.java:11: Error: This expression has type android.icu.number.FormattedNumber (introduced in API level 30) but it used as type java.lang.CharSequence (introduced in API level 1). Run-time class verification will not be able to validate this implicit cast on devices between these API levels. [ImplicitCastClassVerificationFailure]
        Api27Impl.setAutofillOptions(adapter, vararg1, vararg2, vararg3);
                                              ~~~~~~~
src/java/androidx/UnsafeCastToVarargs.java:11: Error: This expression has type android.icu.number.FormattedNumber (introduced in API level 30) but it used as type java.lang.CharSequence (introduced in API level 1). Run-time class verification will not be able to validate this implicit cast on devices between these API levels. [ImplicitCastClassVerificationFailure]
        Api27Impl.setAutofillOptions(adapter, vararg1, vararg2, vararg3);
                                                       ~~~~~~~
src/java/androidx/UnsafeCastToVarargs.java:11: Error: This expression has type android.icu.number.FormattedNumber (introduced in API level 30) but it used as type java.lang.CharSequence (introduced in API level 1). Run-time class verification will not be able to validate this implicit cast on devices between these API levels. [ImplicitCastClassVerificationFailure]
        Api27Impl.setAutofillOptions(adapter, vararg1, vararg2, vararg3);
                                                                ~~~~~~~
3 errors, 0 warnings
        """
        val expectedFixDiffs = """
Fix for src/java/androidx/UnsafeCastToVarargs.java line 11: Extract to static inner class:
@@ -11 +11
-         Api27Impl.setAutofillOptions(adapter, vararg1, vararg2, vararg3);
+         Api27Impl.setAutofillOptions(adapter, Api30Impl.castToCharSequence(vararg1), vararg2, vararg3);
@@ -22 +22
+ @RequiresApi(30)
+ static class Api30Impl {
+     private Api30Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static java.lang.CharSequence castToCharSequence(FormattedNumber formattedNumber) {
+         return formattedNumber;
+     }
+
@@ -23 +34
+ }
Fix for src/java/androidx/UnsafeCastToVarargs.java line 11: Extract to static inner class:
@@ -11 +11
-         Api27Impl.setAutofillOptions(adapter, vararg1, vararg2, vararg3);
+         Api27Impl.setAutofillOptions(adapter, vararg1, Api30Impl.castToCharSequence(vararg2), vararg3);
@@ -22 +22
+ @RequiresApi(30)
+ static class Api30Impl {
+     private Api30Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static java.lang.CharSequence castToCharSequence(FormattedNumber formattedNumber) {
+         return formattedNumber;
+     }
+
@@ -23 +34
+ }
Fix for src/java/androidx/UnsafeCastToVarargs.java line 11: Extract to static inner class:
@@ -11 +11
-         Api27Impl.setAutofillOptions(adapter, vararg1, vararg2, vararg3);
+         Api27Impl.setAutofillOptions(adapter, vararg1, vararg2, Api30Impl.castToCharSequence(vararg3));
@@ -22 +22
+ @RequiresApi(30)
+ static class Api30Impl {
+     private Api30Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static java.lang.CharSequence castToCharSequence(FormattedNumber formattedNumber) {
+         return formattedNumber;
+     }
+
@@ -23 +34
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }
}
