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
            LocalContextResourcesConfigurationReadDetector.LocalContextResourcesRead
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
            """
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
                }
            """
                ),
                LocalContextStub,
                Stubs.Composable,
                Stubs.CompositionLocal,
                AndroidStubs.Context,
                AndroidStubs.Resources,
                AndroidStubs.Configuration
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
src/test/test.kt:9: Warning: Reading Resources using LocalContext.current.resources [LocalContextResourcesRead]
                    LocalContext.current.resources
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:10: Warning: Reading Resources using LocalContext.current.resources [LocalContextResourcesRead]
                    LocalContext.current.getResources()
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 2 warnings
            """
            )
            .expectFixDiffs(
                """
Autofix for src/test/test.kt line 11: Replace with LocalConfiguration.current:
@@ -5 +5
-                 import androidx.compose.ui.platform.LocalContext
+                 import androidx.compose.ui.platform.LocalConfiguration
+ import androidx.compose.ui.platform.LocalContext
@@ -11 +12
-                     LocalContext.current.resources.configuration
+                     LocalConfiguration.current
Autofix for src/test/test.kt line 9: Replace with LocalResources.current:
@@ -6 +6
+ import androidx.compose.ui.platform.LocalResources
@@ -9 +10
-                     LocalContext.current.resources
+                     LocalResources.current
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
                fun Test3() {
                    val context = LocalContext.current
                    val res = context.resources
                }
            """
                ),
                LocalContextStub,
                Stubs.Composable,
                Stubs.CompositionLocal,
                AndroidStubs.Context,
                AndroidStubs.Resources,
                AndroidStubs.Configuration
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
src/test/test.kt:29: Warning: Reading Resources using LocalContext.current.resources [LocalContextResourcesRead]
                    val res = context.resources
                              ~~~~~~~~~~~~~~~~~
3 errors, 1 warnings
            """
            )
    }
}
