/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Test for [LocalContextResourcesConfigurationReadDetector]. */
@RunWith(JUnit4::class)
class LocalContextResourcesConfigurationReadDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = LocalContextResourcesConfigurationReadDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            LocalContextResourcesConfigurationReadDetector.LocalContextConfigurationRead,
            LocalContextResourcesConfigurationReadDetector.LocalContextGetResourceValueCall,
            LocalContextResourcesConfigurationReadDetector.LocalContextResourcesRead,
        )

    val LocalContextStub =
        bytecodeStub(
            "AndroidCompositionLocals.kt",
            "androidx/compose/ui/platform",
            0xe66eae21,
            """
            package androidx.compose.ui.platform

            import android.content.Context
            import androidx.compose.runtime.staticCompositionLocalOf

            val LocalContext = staticCompositionLocalOf<Context>()
           """,
            """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijg0uOSSMxLKcrPTKnQS87PLcgvTtUr
            Ks0rycxNFRJyBgtklmTm5/nkJyfmeJdwWXHJYKgvzdQryEksScsvyhWScoTI
            omstBurl42IpSS0uEWILAZLeJUoMWgwAohQbypQAAAA=
            """,
            """
            androidx/compose/ui/platform/AndroidCompositionLocalsKt.class:
            H4sIAAAAAAAA/61TW0sbQRT+ZrPqZpuamF40alsbbRspuLG0FBoRRBBCUxUt
            vvg02d2EiZsZ2Z0NPvpb+gt6eSl9KNLH/qjSM5tIMeKLdGFnzjlzvu9c5szv
            Pz9+AniNVYa3XAaxEsGZ56v+qUpCLxXeacR1R8V9b2t4uJ0dCS2UbCmfR8l7
            PQXGUOrxAfciLrveXrsX+mTNMRS7oc7ctpXU4ZmmILXV1rU4cSq16IfefqwG
            IuDtKBwP02A4uB1y4xJEGMpBam+US2OTSJdbKu56vVC3Yy5k4nEpleYGnHi7
            Su+mkQlduFrDm1tVUEAebh4W7jA4G34kpNCbDLna6hHDyxsZx3lMv4sMS4lJ
            0x8/3eusBGGHpxGl2au1TpSmOF5v0Pc6qfSHde2MpHqj2Rq/tcbtbqeAGZRd
            lHCPYf//39LMZSEfQs0DrjnZrP4gR5NrmYWZBQzshOxnwmh1koJ1hrWL82n3
            4ty15qzh71jV2dLF+bxTtstW3aqzX58mHfKYt51cyTaoVwyVG9NhWLjpJayd
            UNPzh6IruU7jkDwPhnU35UAkgqre+jddDPa2Csip2BIy3E377TD+aDrD4B6q
            NPbDHWGUyojj6BoD1mmW7KzueTNapC2bhuABVmifJLuT6RVMkJbDM9IWyWo+
            +ysKnzPs85EvMDXCT13BO7iLaZINuoqs0XCZzb7j/jcUvlzjsPAiY6miRvs7
            sj6k+LPHyDUx10SlSdkukIjFJh7h8TFYgidYOsZEgnwCN8HTxMiTfwE44RI/
            mwQAAA==
            """,
        )

    val GlanceLocalContextStub =
        bytecodeStub(
            "CompositionLocals.kt",
            "androidx/glance",
            0x631c973f,
            """
            package androidx.glance

            import android.content.Context
            import androidx.compose.runtime.staticCompositionLocalOf

            val LocalContext = staticCompositionLocalOf<Context>()
           """,
            """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijg0uOSSMxLKcrPTKnQS87PLcgvTtUr
            Ks0rycxNFRJyBgtklmTm5/nkJyfmeJdwqXHxw9Wn5yTmJacKCaMrKwaq4+Ni
            KUktLhFiCwGS3iVKDFoMAKjR7e+AAAAA
            """,
            """
            androidx/glance/CompositionLocalsKt.class:
            H4sIAAAAAAAA/61TXU8TQRQ9s11gu1ZaEIUCKlLUEhO2GI2JJSSGhKSxAgHD
            C0/T3aGZ0s6Y3dmGR36Lv8CPF+ODIT76o4x3tkVDCS/ETXbm3rv3nPu5v35/
            /wHgBVYZKlxFsZbRadDuchWKYEv3PuhEGqlVU4e8m7w1E2AMpQ7v84B82sFu
            qyNCsuYYim1hMrctrYw4NQyvqqvNv5xhRiaCOFVG9kSwF+u+jHirK0bD1Bn2
            b4bcuAARhnJQJhjmUt8k0kpTx+2gI0wr5lIlAVdKG27BSbCjzU7ataELl2t4
            eaMKCsjDz8PBLQZvI+xKJc0mQ666esjw7FrGUR7b7yLDUmLTDEe/7h6vROKY
            p11Ks1NtnmhDcYJOvxccpyoc1LU9lGr1RnN0avWbTaeAKUz7KOEOw97/n9LU
            RSHvhOERN5xsTq+foy117MHsAQZ2QvZTabUaSdE6w9r52aR/fuY7c87g9Zzl
            2dL52bw37U47NafGfn4c98hj3vVyJdeinjOUr02HYebKL7B2Qt3OH8i24iaN
            BcPC/qDghurLRFK5b/6tFYO7pSNyKjalEjtpryXi97YlDP6BTuNQbEurlIcc
            h1cYsE5L5GYFz9udIq1iO4G7WKF7nOxeppcxRloOj0lbJKt93C8ofMqwT4a+
            wMQQP3EJ7+E2Jkm26GVkHYbPXPYNM19R+HyFw8HTjGUZVbpfk/UexZ89Qq6B
            uQbKDcp2gUQsNnAfD47AEjzE0hHGEuQT+AkeJVYe/wPYvCA4gAQAAA==
            """,
        )

    @Test
    fun error() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.platform.LocalContext

                @Composable
                fun Test() {
                    LocalContext.current.resources
                    LocalContext.current.getResources()
                    LocalContext.current.resources.configuration
                    LocalContext.current.getResources().getConfiguration()
                    LocalContext.current.getText(-1)
                    LocalContext.current.getString(-1)
                    LocalContext.current.getString(-1, Any())
                    LocalContext.current.getColor(-1)
                    LocalContext.current.getDrawable(-1)
                    LocalContext.current.getColorStateList(-1)
                }
            """
                ),
                LocalContextStub,
                Stubs.Composable,
                Stubs.CompositionLocal,
                AndroidStubs.Context,
                AndroidStubs.Resources,
                AndroidStubs.Configuration,
                AndroidStubs.Drawable,
                AndroidStubs.ColorStateList,
            )
            .run()
            .expect(
                """
src/test/test.kt:11: Error: Reading Configuration using LocalContext.current.resources.configuration [LocalContextConfigurationRead]
                    LocalContext.current.resources.configuration
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:12: Error: Reading Configuration using LocalContext.current.resources.configuration [LocalContextConfigurationRead]
                    LocalContext.current.getResources().getConfiguration()
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:13: Error: Querying resource values using LocalContext.current [LocalContextGetResourceValueCall]
                    LocalContext.current.getText(-1)
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:14: Error: Querying resource values using LocalContext.current [LocalContextGetResourceValueCall]
                    LocalContext.current.getString(-1)
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:15: Error: Querying resource values using LocalContext.current [LocalContextGetResourceValueCall]
                    LocalContext.current.getString(-1, Any())
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:16: Error: Querying resource values using LocalContext.current [LocalContextGetResourceValueCall]
                    LocalContext.current.getColor(-1)
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:17: Error: Querying resource values using LocalContext.current [LocalContextGetResourceValueCall]
                    LocalContext.current.getDrawable(-1)
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:18: Error: Querying resource values using LocalContext.current [LocalContextGetResourceValueCall]
                    LocalContext.current.getColorStateList(-1)
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:9: Warning: Reading Resources using LocalContext.current.resources [LocalContextResourcesRead]
                    LocalContext.current.resources
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:10: Warning: Reading Resources using LocalContext.current.resources [LocalContextResourcesRead]
                    LocalContext.current.getResources()
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
8 errors, 2 warnings
            """
            )
            .expectFixDiffs(
                """
Autofix for src/test/test.kt line 11: Replace with LocalConfiguration.current:
@@ -5 +5,2 @@
-                import androidx.compose.ui.platform.LocalContext
+                import androidx.compose.ui.platform.LocalConfiguration
+import androidx.compose.ui.platform.LocalContext
@@ -11 +12 @@
-                    LocalContext.current.resources.configuration
+                    LocalConfiguration.current
Autofix for src/test/test.kt line 14: Replace with stringResource:
@@ -5,0 +6 @@
+import androidx.compose.ui.res.stringResource
@@ -14 +15 @@
-                    LocalContext.current.getString(-1)
+                    stringResource(-1)
Autofix for src/test/test.kt line 15: Replace with stringResource:
@@ -5,0 +6 @@
+import androidx.compose.ui.res.stringResource
@@ -15 +16 @@
-                    LocalContext.current.getString(-1, Any())
+                    stringResource(-1, Any())
Autofix for src/test/test.kt line 16: Replace with colorResource:
@@ -5,0 +6 @@
+import androidx.compose.ui.res.colorResource
@@ -16 +17 @@
-                    LocalContext.current.getColor(-1)
+                    colorResource(-1)
Autofix for src/test/test.kt line 17: Replace with painterResource:
@@ -5,0 +6 @@
+import androidx.compose.ui.res.painterResource
@@ -17 +18 @@
-                    LocalContext.current.getDrawable(-1)
+                    painterResource(-1)
Autofix for src/test/test.kt line 17: Replace with ImageBitmap.imageResource:
@@ -5,0 +6 @@
+import androidx.compose.ui.res.imageResource
@@ -17 +18 @@
-                    LocalContext.current.getDrawable(-1)
+                    ImageBitmap.imageResource(-1)
Autofix for src/test/test.kt line 17: Replace with ImageVector.vectorResource:
@@ -5,0 +6 @@
+import androidx.compose.ui.res.vectorResource
@@ -17 +18 @@
-                    LocalContext.current.getDrawable(-1)
+                    ImageVector.vectorResource(-1)
Autofix for src/test/test.kt line 9: Replace with LocalResources.current:
@@ -5,0 +6 @@
+import androidx.compose.ui.platform.LocalResources
@@ -9 +10 @@
-                    LocalContext.current.resources
+                    LocalResources.current
                """
            )
    }

    @Test
    fun errors_splitReferences() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.platform.LocalContext

                @Composable
                fun Test1() {
                    val resources = LocalContext.current.resources
                    resources.configuration
                }

                @Composable
                fun Test2() {
                    val context = LocalContext.current
                    context.resources.configuration
                }

                @Composable
                fun Test3() {
                    val context = LocalContext.current
                    val res = context.resources
                    res.configuration
                }

                @Composable
                fun Test4() {
                    val context = LocalContext.current
                    val res = context.resources
                }

                @Composable
                fun Test5() {
                    val context = LocalContext.current
                    context.getText(-1)
                    context.getString(-1)
                    context.getString(-1, Any())
                    context.getColor(-1)
                    context.getDrawable(-1)
                    context.getColorStateList(-1)
                }
            """
                ),
                LocalContextStub,
                Stubs.Composable,
                Stubs.CompositionLocal,
                AndroidStubs.Context,
                AndroidStubs.Resources,
                AndroidStubs.Configuration,
                AndroidStubs.Drawable,
                AndroidStubs.ColorStateList,
            )
            .run()
            .expect(
                """
src/test/test.kt:10: Error: Reading Configuration using LocalContext.current.resources.configuration [LocalContextConfigurationRead]
                    resources.configuration
                    ~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:16: Error: Reading Configuration using LocalContext.current.resources.configuration [LocalContextConfigurationRead]
                    context.resources.configuration
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:23: Error: Reading Configuration using LocalContext.current.resources.configuration [LocalContextConfigurationRead]
                    res.configuration
                    ~~~~~~~~~~~~~~~~~
src/test/test.kt:35: Error: Querying resource values using LocalContext.current [LocalContextGetResourceValueCall]
                    context.getText(-1)
                    ~~~~~~~~~~~~~~~~~~~
src/test/test.kt:36: Error: Querying resource values using LocalContext.current [LocalContextGetResourceValueCall]
                    context.getString(-1)
                    ~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:37: Error: Querying resource values using LocalContext.current [LocalContextGetResourceValueCall]
                    context.getString(-1, Any())
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:38: Error: Querying resource values using LocalContext.current [LocalContextGetResourceValueCall]
                    context.getColor(-1)
                    ~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:39: Error: Querying resource values using LocalContext.current [LocalContextGetResourceValueCall]
                    context.getDrawable(-1)
                    ~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:40: Error: Querying resource values using LocalContext.current [LocalContextGetResourceValueCall]
                    context.getColorStateList(-1)
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:29: Warning: Reading Resources using LocalContext.current.resources [LocalContextResourcesRead]
                    val res = context.resources
                              ~~~~~~~~~~~~~~~~~
9 errors, 1 warning
            """
            )
            .expectFixDiffs(
                """
Autofix for src/test/test.kt line 36: Replace with stringResource:
@@ -5,0 +6 @@
+import androidx.compose.ui.res.stringResource
@@ -36 +37 @@
-                    context.getString(-1)
+                    stringResource(-1)
Autofix for src/test/test.kt line 37: Replace with stringResource:
@@ -5,0 +6 @@
+import androidx.compose.ui.res.stringResource
@@ -37 +38 @@
-                    context.getString(-1, Any())
+                    stringResource(-1, Any())
Autofix for src/test/test.kt line 38: Replace with colorResource:
@@ -5,0 +6 @@
+import androidx.compose.ui.res.colorResource
@@ -38 +39 @@
-                    context.getColor(-1)
+                    colorResource(-1)
Autofix for src/test/test.kt line 39: Replace with painterResource:
@@ -5,0 +6 @@
+import androidx.compose.ui.res.painterResource
@@ -39 +40 @@
-                    context.getDrawable(-1)
+                    painterResource(-1)
Autofix for src/test/test.kt line 39: Replace with ImageBitmap.imageResource:
@@ -5,0 +6 @@
+import androidx.compose.ui.res.imageResource
@@ -39 +40 @@
-                    context.getDrawable(-1)
+                    ImageBitmap.imageResource(-1)
Autofix for src/test/test.kt line 39: Replace with ImageVector.vectorResource:
@@ -5,0 +6 @@
+import androidx.compose.ui.res.vectorResource
@@ -39 +40 @@
-                    context.getDrawable(-1)
+                    ImageVector.vectorResource(-1)
                """
            )
    }

    @Test
    fun ignoresOtherLocalContextDefinitions() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import android.content.Context
                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.staticCompositionLocalOf
                import androidx.glance.LocalContext

                val MyLocalContext = staticCompositionLocalOf<Context>()

                @Composable
                fun Test1() {
                    LocalContext.current.resources
                    LocalContext.current.getResources()
                    LocalContext.current.resources.configuration
                    LocalContext.current.getResources().getConfiguration()
                    LocalContext.current.getText(-1)
                    LocalContext.current.getString(-1)
                    LocalContext.current.getString(-1, Any())
                    LocalContext.current.getColor(-1)
                    LocalContext.current.getDrawable(-1)
                    LocalContext.current.getColorStateList(-1)
                    MyLocalContext.current.resources
                    MyLocalContext.current.getResources()
                    MyLocalContext.current.resources.configuration
                    MyLocalContext.current.getResources().getConfiguration()
                    MyLocalContext.current.getText(-1)
                    MyLocalContext.current.getString(-1)
                    MyLocalContext.current.getString(-1, Any())
                    MyLocalContext.current.getColor(-1)
                    MyLocalContext.current.getDrawable(-1)
                    MyLocalContext.current.getColorStateList(-1)
                }

                @Composable
                fun Test2() {
                    val resources = LocalContext.current.resources
                    resources.configuration
                    val resources2 = MyLocalContext.current.resources
                    resources2.configuration
                }

                @Composable
                fun Test3() {
                    val context = LocalContext.current
                    context.resources.configuration
                    val context2 = MyLocalContext.current
                    context2.resources.configuration
                }

                @Composable
                fun Test4() {
                    val context = LocalContext.current
                    val res = context.resources
                    res.configuration
                    val context2 = MyLocalContext.current
                    val res2 = context2.resources
                    res2.configuration
                }

                @Composable
                fun Test5() {
                    val context = LocalContext.current
                    val res = context.resources
                    val context2 = MyLocalContext.current
                    val res2 = context2.resources
                }
            """
                ),
                GlanceLocalContextStub,
                Stubs.Composable,
                Stubs.CompositionLocal,
                AndroidStubs.Context,
                AndroidStubs.Resources,
                AndroidStubs.Configuration,
                AndroidStubs.Drawable,
                AndroidStubs.ColorStateList,
            )
            .run()
            .expectClean()
    }
}
