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

import androidx.build.lint.Stubs.Companion.FlaggedApi
import androidx.build.lint.Stubs.Companion.Flags
import androidx.build.lint.Stubs.Companion.IntRange
import androidx.build.lint.Stubs.Companion.RequiresApi
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ClassVerificationFailureDetectorDefaultUsagesTest :
    AbstractLintDetectorTest(
        useDetector = ClassVerificationFailureDetector(),
        useIssues = listOf(ClassVerificationFailureDetector.METHOD_CALL_ISSUE),
        stubs =
            arrayOf(
                // AndroidManifest with minSdkVersion=14
                manifest().minSdk(14)
            ),
    ) {

    override fun lint(): TestLintTask {
        return super.lint().allowCompilationErrors(false)
    }

    @Test
    fun `Bypass detection of unsafe references in Java sources`() {
        val input =
            arrayOf(javaSample("androidx.ClassVerificationFailureFromJava"), RequiresApi, IntRange)

        // We're only checking flagged APIs and super calls, so this should be clean.
        check(*input).expectClean()
    }

    @Test
    fun `Bypass auto-fix for unsafe method call on this`() {
        val input = arrayOf(javaSample("androidx.AutofixUnsafeCallToThis"))

        // There are two additional failures when we're checking all usages, but we are only
        // checking flagged API calls and super calls.
        val expected =
            """
src/androidx/AutofixUnsafeCallToThis.java:57: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeCallToThis is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            super.getClipToPadding();
                  ~~~~~~~~~~~~~~~~
1 error
        """

        // Super calls cannot be outlined.
        val expectedFix = ""

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Detection of flagged APIs in Kotlin sources`() {
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

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of flagged APIs in Java sources`() {
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
    fun `Detection of unsafe references in real-world Kotlin sources`() {
        val input =
            arrayOf(
                javaSample("android.flagging.FlaggedApiContainer"),
                ktSample("flaggedapi.FlaggedUsageWithoutOutline"),
                FlaggedApi,
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

        check(*input).expect(expected)
    }
}
