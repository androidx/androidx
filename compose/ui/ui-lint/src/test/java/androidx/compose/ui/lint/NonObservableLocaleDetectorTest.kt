/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFiles.bytecode
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NonObservableLocaleDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = NonObservableLocaleDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(NonObservableLocaleDetector.NonObservableLocale)

    @Test
    fun testJavaLocale_inComposable_showsWarning() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import java.util.Locale

                @Composable
                fun MyComposable() {
                    val locale = Locale.getDefault()
                }
                """
                ),
                Stubs.Composable,
            )
            .run()
            .expect(
                """
            src/test/test.kt:9: Error: Reading locale in a non-observable way in a composable function [NonObservableLocale]
                                val locale = Locale.getDefault()
                                             ~~~~~~~~~~~~~~~~~~~
            1 error
            """
            )
            .expectFixDiffs(
                """
                Autofix for src/test/test.kt line 9: Replace with LocalLocale.current.platformLocale:
                @@ -5 +5,2 @@
                -                import java.util.Locale
                +                import androidx.compose.ui.platform.LocalLocale
                +import java.util.Locale
                @@ -9 +10 @@
                -                    val locale = Locale.getDefault()
                +                    val locale = LocalLocale.current.platformLocale
                """
                    .trimIndent()
            )
    }

    @Test
    fun testJavaLocaleImportedDirectly_inComposable_showsWarning() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import java.util.Locale.getDefault

                @Composable
                fun MyComposable() {
                    val locale = getDefault()
                }
                """
                ),
                Stubs.Composable,
            )
            .run()
            .expect(
                """
            src/test/test.kt:9: Error: Reading locale in a non-observable way in a composable function [NonObservableLocale]
                                val locale = getDefault()
                                             ~~~~~~~~~~~~
            1 error
            """
            )
            .expectFixDiffs(
                """
                Autofix for src/test/test.kt line 9: Replace with LocalLocale.current.platformLocale:
                @@ -5 +5,2 @@
                -                import java.util.Locale.getDefault
                +                import androidx.compose.ui.platform.LocalLocale
                +import java.util.Locale.getDefault
                @@ -9 +10 @@
                -                    val locale = getDefault()
                +                    val locale = LocalLocale.current.platformLocale
                """
                    .trimIndent()
            )
    }

    @Test
    fun testAndroidLocaleList_inComposable_showsWarning() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import android.os.LocaleList
                import androidx.compose.runtime.Composable

                @Composable
                fun MyComposable() {
                    val localeList = LocaleList.getAdjustedDefault()
                }
                """
                ),
                Stubs.Composable,
            )
            .run()
            .expect(
                """
            src/test/test.kt:9: Error: Reading locale in a non-observable way in a composable function [NonObservableLocale]
                                val localeList = LocaleList.getAdjustedDefault()
                                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 error
            """
            )
            .expectFixDiffs(
                """
                Autofix for src/test/test.kt line 9: Replace with LocalLocaleList.current:
                @@ -5,0 +6 @@
                +import androidx.compose.ui.platform.LocalLocaleList
                @@ -9 +10 @@
                -                    val localeList = LocaleList.getAdjustedDefault()
                +                    val localeList = android.os.LocaleList(*LocalLocaleList.current.map { it.platformLocale }.toTypedArray())
                """
                    .trimIndent()
            )
    }

    @Test
    fun testAndroidLocaleListImportedDirectly_inComposable_showsWarning() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import android.os.LocaleList.getAdjustedDefault
                import androidx.compose.runtime.Composable

                @Composable
                fun MyComposable() {
                    val localeList = getAdjustedDefault()
                }
                """
                ),
                Stubs.Composable,
            )
            .run()
            .expect(
                """
            src/test/test.kt:9: Error: Reading locale in a non-observable way in a composable function [NonObservableLocale]
                                val localeList = getAdjustedDefault()
                                                 ~~~~~~~~~~~~~~~~~~~~
            1 error
            """
            )
            .expectFixDiffs(
                """
                Autofix for src/test/test.kt line 9: Replace with LocalLocaleList.current:
                @@ -5,0 +6 @@
                +import androidx.compose.ui.platform.LocalLocaleList
                @@ -9 +10 @@
                -                    val localeList = getAdjustedDefault()
                +                    val localeList = android.os.LocaleList(*LocalLocaleList.current.map { it.platformLocale }.toTypedArray())
                """
                    .trimIndent()
            )
    }

    @Test
    fun testAndroidXLocaleListCompat_inComposable_showsWarning() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.core.os.LocaleListCompat

                @Composable
                fun MyComposable() {
                    val localeList = LocaleListCompat.getAdjustedDefault()
                }
                """
                ),
                Stubs.Composable,
                LocalListCompatStub,
            )
            .run()
            .expect(
                """
            src/test/test.kt:9: Error: Reading locale in a non-observable way in a composable function [NonObservableLocale]
                                val localeList = LocaleListCompat.getAdjustedDefault()
                                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 error
            """
            )
            .expectFixDiffs(
                """
                Autofix for src/test/test.kt line 9: Replace with LocalLocaleList.current:
                @@ -5 +5,2 @@
                -                import androidx.core.os.LocaleListCompat
                +                import androidx.compose.ui.platform.LocalLocaleList
                +import androidx.core.os.LocaleListCompat
                @@ -9 +10 @@
                -                    val localeList = LocaleListCompat.getAdjustedDefault()
                +                    val localeList = LocaleListCompat.create(*LocalLocaleList.current.map { it.platformLocale }.toTypedArray())
                """
                    .trimIndent()
            )
    }

    @Test
    fun testAndroidXLocaleListCompatImportedDirectly_inComposable_showsWarning() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.core.os.LocaleListCompat.getAdjustedDefault

                @Composable
                fun MyComposable() {
                    val localeList = getAdjustedDefault()
                }
                """
                ),
                Stubs.Composable,
                LocalListCompatStub,
            )
            .run()
            .expect(
                """
            src/test/test.kt:9: Error: Reading locale in a non-observable way in a composable function [NonObservableLocale]
                                val localeList = getAdjustedDefault()
                                                 ~~~~~~~~~~~~~~~~~~~~
            1 error
            """
            )
            .expectFixDiffs(
                """
                Autofix for src/test/test.kt line 9: Replace with LocalLocaleList.current:
                @@ -5 +5,2 @@
                -                import androidx.core.os.LocaleListCompat.getAdjustedDefault
                +                import androidx.compose.ui.platform.LocalLocaleList
                +import androidx.core.os.LocaleListCompat.getAdjustedDefault
                @@ -9 +10 @@
                -                    val localeList = getAdjustedDefault()
                +                    val localeList = LocaleListCompat.create(*LocalLocaleList.current.map { it.platformLocale }.toTypedArray())
                """
                    .trimIndent()
            )
    }

    @Test
    fun testNoWarning_whenNotInComposable() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import java.util.Locale
                import android.os.LocaleList
                import androidx.core.os.LocaleListCompat

                fun myHelper() {
                    val locale = Locale.getDefault()
                    val localeList = LocaleList.getAdjustedDefault()
                    val localeListCompat = LocaleListCompat.getAdjustedDefault()
                }
                """
                ),
                Stubs.Composable,
                LocalListCompatStub,
            )
            .run()
            .expectClean()
    }
}

private val LocalListCompatStub =
    bytecode(
        "libs/localelistcompat.jar",
        java(
                """

package androidx.core.os;

public final class LocaleListCompat {
    private LocaleListCompat() {
    }

    public static LocaleListCompat getAdjustedDefault() {
        return new LocaleListCompat();
    }
}
                """
            )
            .indented(),
        0x92130b10,
        """
                androidx/core/os/LocaleListCompat.class:
                H4sIAAAAAAAA/4VPTUvDQBB9m88mVlsE7/YgtB4MnhWhVHoK9aAIHjfZsWxI
                s5JsxJ+lp4KCP8AfJU5C8eDFGZgv3nsz8/X9/glgjnEMB24IbwgfgcC4kM8y
                KWW1Tm6ygnIrEFzqStsrAXc6uw8xEJjIStVGq5ckNzUlpklSk8uSUt3Yhdk8
                SRsjhCvgLYwigVGqK1q1m4zqO5mVPDlck52rom0sqWt6lG3Ji06ms/Rf5QuB
                +Na0dU5L3Skd/QWcdR/gnA9w0JnDzq+BedxNOAvO/ukW4o0LgT2OcQ/24TEw
                whD7O/jxTmTwgfBhi+j1lxH0MhHHg37J6AfpWjmnUwEAAA==
                """,
    )
