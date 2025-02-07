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

/** Test for [ConfigurationScreenWidthHeightDetector]. */
@RunWith(JUnit4::class)
class ConfigurationScreenWidthHeightDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ConfigurationScreenWidthHeightDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ConfigurationScreenWidthHeightDetector.ConfigurationScreenWidthHeight)

    val LocalConfigurationStub =
        bytecodeStub(
            "AndroidCompositionLocals.kt",
            "androidx/compose/ui/platform",
            0x53d41b71,
            """
            package androidx.compose.ui.platform

            import android.content.res.Configuration
            import androidx.compose.runtime.compositionLocalOf

            val LocalConfiguration = compositionLocalOf<Configuration>()
            """,
            """
            META-INF/main.kotlin_module:
            H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijg0uOSSMxLKcrPTKnQS87PLcgvTtUr
            Ks0rycxNFRJyBgtklmTm5/nkJyfmeJdwWXHJYKgvzdQryEksScsvyhWScoTI
            omstBupVwGIXUG9pXmaJEItLAVAFHxdLSWpxiRBbCJD0LlFi0GIAAIyv63a2
            AAAA
            """,
            """
            androidx/compose/ui/platform/AndroidCompositionLocalsKt.class:
            H4sIAAAAAAAA/7VTW08TQRT+ZrvQ7VqhVBEoeAPUEhOmGI2JJSSEhKSxghHD
            g/gy3Z02U7YzZHe24ZHf4i/w8mJ8MMRHf5RxZltiKBITEjfZnXPOnO871/35
            69t3AE+xQvCcyTBWIjymgeodqYTTVNCjiOm2int0c3C5lV0JLZRsqoBFyUud
            ByEodVmf0YjJDt1tdXlgrDmC6Q7XmduWkm3RSWNmgSZUdaV5IVqcSi16nL6O
            VV+ErBXx0WB1gvdXQ66fgQxGai41jXlCz2VV3zD0S00Vd2iX61bMhEwok1Lp
            7DqhO0rvpJFNovy3mp5dqaIiCvALcHCNwFsPIiGF3iDIVVf2CR5fyjjKY6cw
            SVAJRuy77eWQt1kaaYJutXmotIlAu/0ebacyGNS1PZRq9UZzdIr1q82piCmU
            fZRwg+Dd/5zX1FlJr7hmIdPM2JxeP2d22rEfYj8gIIfGfiysVjNSuEawenoy
            4Z+e+M6sM3g9Z3GmdHpS8cpu2ak5NfLjw7hnPCqulyu5FvXELsi/EyOYv+xv
            WT00gyjsiY5kOo258Xwz6EVD9kUiTCc2/2wcgbulQuM02RSS76S9Fo/f2m4R
            +HsqjQO+LawyN+TYv8CANbNZbtaBil00oy3Z1mAay+YcN3Yv0+cwZrQcHhht
            wVjt435G8WOGfTj0BfJDfP4c3sN1TBjZoheRtRw+cclX3PyC4qcLHA4eZSyL
            qJrzhbHeMvFnDpBrYLaBuYbJdt6IWGjgNu4cgCS4i3sHGEtQSOAnuJ9Yefw3
            1TI9fr8EAAA=
            """
        )

    @Test
    fun error() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import android.content.res.Configuration
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.platform.LocalConfiguration
                import androidx.compose.ui.unit.dp

                @Composable
                fun Test() {
                    LocalConfiguration.current.screenWidthDp
                    LocalConfiguration.current.screenHeightDp

                    val configuration = LocalConfiguration.current
                    val width = configuration.screenWidthDp.dp
                    val height = configuration.screenHeightDp.dp

                    val someLambda = {
                        // Capture configuration value from composition, and use outside of
                        // composition
                        configuration.screenWidthDp
                        configuration.screenHeightDp
                    }
                }

                @Composable
                fun Test2(configuration: Configuration) {
                    val width = configuration.screenWidthDp.dp
                    val height = configuration.screenHeightDp.dp
                }
            """
                ),
                LocalConfigurationStub,
                Stubs.Composable,
                Stubs.CompositionLocal,
                Stubs.Dp,
                AndroidStubs.Configuration
            )
            .run()
            .expect(
                """
src/test/test.kt:11: Warning: Using Configuration.screenWidthDp instead of LocalWindowInfo.current.containerSize [ConfigurationScreenWidthHeight]
                    LocalConfiguration.current.screenWidthDp
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:12: Warning: Using Configuration.screenHeightDp instead of LocalWindowInfo.current.containerSize [ConfigurationScreenWidthHeight]
                    LocalConfiguration.current.screenHeightDp
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:15: Warning: Using Configuration.screenWidthDp instead of LocalWindowInfo.current.containerSize [ConfigurationScreenWidthHeight]
                    val width = configuration.screenWidthDp.dp
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:16: Warning: Using Configuration.screenHeightDp instead of LocalWindowInfo.current.containerSize [ConfigurationScreenWidthHeight]
                    val height = configuration.screenHeightDp.dp
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:21: Warning: Using Configuration.screenWidthDp instead of LocalWindowInfo.current.containerSize [ConfigurationScreenWidthHeight]
                        configuration.screenWidthDp
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:22: Warning: Using Configuration.screenHeightDp instead of LocalWindowInfo.current.containerSize [ConfigurationScreenWidthHeight]
                        configuration.screenHeightDp
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:28: Warning: Using Configuration.screenWidthDp instead of LocalWindowInfo.current.containerSize [ConfigurationScreenWidthHeight]
                    val width = configuration.screenWidthDp.dp
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:29: Warning: Using Configuration.screenHeightDp instead of LocalWindowInfo.current.containerSize [ConfigurationScreenWidthHeight]
                    val height = configuration.screenHeightDp.dp
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 8 warnings
            """
            )
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import android.content.res.Configuration
                import androidx.compose.runtime.Composable

                @Composable
                fun Test(configuration: Configuration) {
                    val someLambda = {
                        configuration.screenWidthDp
                        configuration.screenHeightDp
                    }
                }
            """
                ),
                LocalConfigurationStub,
                Stubs.Composable,
                Stubs.CompositionLocal,
                Stubs.Dp,
                AndroidStubs.Configuration
            )
            .run()
            .expectClean()
    }
}
