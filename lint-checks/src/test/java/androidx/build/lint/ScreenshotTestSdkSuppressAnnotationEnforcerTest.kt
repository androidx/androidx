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

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScreenshotTestSdkSuppressAnnotationEnforcerTest :
    AbstractLintDetectorTest(
        useDetector = ScreenshotTestSdkSuppressAnnotationEnforcer(),
        useIssues = listOf(ScreenshotTestSdkSuppressAnnotationEnforcer.ISSUE),
        stubs =
            arrayOf(
                Stubs.RunWith,
                Stubs.JUnit4Runner,
                Stubs.TestAnnotation,
                Stubs.RuleAnnotation,
                Stubs.AndroidXScreenshotTestRule,
                Stubs.SdkSuppressAnnotation,
                Stubs.AndroidBuild,
            ),
    ) {
    @Test
    fun `sdk suppress annotation missing in screenshot test class in kotlin sources`() {
        val input =
            arrayOf(
                kotlin(
                        """
                package androidx.foo

                import org.junit.runner.RunWith
                import org.junit.runners.JUnit4
                import org.junit.Rule
                import androidx.test.screenshot.AndroidXScreenshotTestRule

                @RunWith(JUnit4::class)
                class Test {
                    @get:Rule val screenshotRule = AndroidXScreenshotTestRule("SCREENSHOT_GOLDEN_PATH")
                    @Test
                    fun aTest() {}
                }
            """
                    )
                    .within("src/androidTest")
            )

        val expected =
            """
src/androidTest/androidx/foo/Test.kt:10: Error: Screenshot test class Test must be annotated with @SdkSuppress to run only on API 35. [ScreenshotTestSdkSuppress]
                class Test {
                      ~~~~
1 error
        """

        check(*input).expect(expected)
    }

    @Test
    fun `sdk suppress annotation present in screenshot test class in kotlin sources`() {
        val input =
            arrayOf(
                kotlin(
                        """
                package androidx.foo

                import org.junit.runner.RunWith
                import org.junit.runners.JUnit4
                import org.junit.Rule
                import androidx.test.filters.SdkSuppress
                import androidx.test.screenshot.AndroidXScreenshotTestRule

                @RunWith(JUnit4::class)
                @SdkSuppress(
                    minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM,
                    maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM,
                )
                class Test {
                    @get:Rule val screenshotRule = AndroidXScreenshotTestRule("SCREENSHOT_GOLDEN_PATH")
                    @Test
                    fun aTest() {}
                }
            """
                    )
                    .within("src/androidTest")
            )
        check(*input).expectClean()
    }

    @Test
    fun `sdk suppress annotation present but with different minSdkVersion in screenshot test class kotlin sources`() {
        val input =
            arrayOf(
                kotlin(
                        """
                package androidx.foo

                import org.junit.runner.RunWith
                import org.junit.runners.JUnit4
                import org.junit.Rule
                import androidx.test.filters.SdkSuppress
                import androidx.test.screenshot.AndroidXScreenshotTestRule

                @RunWith(JUnit4::class)
                @SdkSuppress(
                    minSdkVersion = Build.VERSION_CODES.TIRAMISU,
                    maxSdkVersion = Build.VERSION_CODES.TIRAMISU,
                )
                class Test {
                    @get:Rule val screenshotRule = AndroidXScreenshotTestRule("SCREENSHOT_GOLDEN_PATH")
                    @Test
                    fun aTest() {}
                }
            """
                    )
                    .within("src/androidTest")
            )
        val expected =
            """
src/androidTest/androidx/foo/Test.kt:11: Error: @SdkSuppress on screenshot test class Test must have minSdkVersion and maxSdkVersion set to 35. [ScreenshotTestSdkSuppress]
                @SdkSuppress(
                ^
1 error
        """

        lint().files(*stubs, *input).skipTestModes(TestMode.SUPPRESSIBLE).run().expect(expected)
    }

    @Test
    fun `sdk suppress annotation missing in screenshot test class in java sources`() {
        val input =
            arrayOf(
                java(
                        """
                package androidx.foo;

                import org.junit.runner.RunWith;
                import org.junit.runners.JUnit4;
                import org.junit.Rule;
                import androidx.test.screenshot.AndroidXScreenshotTestRule;
                import org.junit.Test;

                @RunWith(JUnit4.class)
                public class TestJava {
                    @Rule
                    public AndroidXScreenshotTestRule screenshotRule = new AndroidXScreenshotTestRule("SCREENSHOT_GOLDEN_PATH");

                    @Test
                    public void aTest() {}
                }
                """
                    )
                    .within("src/androidTest")
            )

        val expected =
            """
src/androidTest/androidx/foo/TestJava.java:11: Error: Screenshot test class TestJava must be annotated with @SdkSuppress to run only on API 35. [ScreenshotTestSdkSuppress]
                public class TestJava {
                             ~~~~~~~~
1 error
        """

        check(*input).expect(expected)
    }

    @Test
    fun `sdk suppress annotation present and correct in screenshot test class in java sources`() {
        val input =
            arrayOf(
                java(
                        """
                package androidx.foo;

                import org.junit.runner.RunWith;
                import org.junit.runners.JUnit4;
                import org.junit.Rule;
                import androidx.test.filters.SdkSuppress;
                import android.os.Build;
                import androidx.test.screenshot.AndroidXScreenshotTestRule;
                import org.junit.Test;

                @RunWith(JUnit4.class)
                @SdkSuppress(
                    minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM,
                    maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM
                )
                public class TestJava {
                    @Rule
                    public AndroidXScreenshotTestRule screenshotRule = new AndroidXScreenshotTestRule("SCREENSHOT_GOLDEN_PATH");

                    @Test
                    public void aTest() {}
                }
                """
                    )
                    .within("src/androidTest")
            )
        check(*input).expectClean()
    }

    @Test
    fun `sdk suppress annotation present but with different minSdkVersion in screenshot test class in java sources`() {
        val input =
            arrayOf(
                java(
                        """
                package androidx.foo;

                import org.junit.runner.RunWith;
                import org.junit.runners.JUnit4;
                import org.junit.Rule;
                import androidx.test.filters.SdkSuppress;
                import android.os.Build;
                import androidx.test.screenshot.AndroidXScreenshotTestRule;
                import org.junit.Test;

                @RunWith(JUnit4.class)
                @SdkSuppress(
                    minSdkVersion = Build.VERSION_CODES.TIRAMISU,
                    maxSdkVersion = Build.VERSION_CODES.TIRAMISU
                )
                public class TestJava {
                    @Rule
                    public AndroidXScreenshotTestRule screenshotRule = new AndroidXScreenshotTestRule("SCREENSHOT_GOLDEN_PATH");

                    @Test
                    public void aTest() {}
                }
                """
                    )
                    .within("src/androidTest")
            )

        val expected =
            """
src/androidTest/androidx/foo/TestJava.java:13: Error: @SdkSuppress on screenshot test class TestJava must have minSdkVersion and maxSdkVersion set to 35. [ScreenshotTestSdkSuppress]
                @SdkSuppress(
                ^
1 error
        """
        lint().files(*stubs, *input).skipTestModes(TestMode.SUPPRESSIBLE).run().expect(expected)
    }

    @Test
    fun `not a screenshot test class in kotlin sources`() {
        val input =
            arrayOf(
                kotlin(
                        """
                package androidx.foo

                import org.junit.runner.RunWith
                import org.junit.runners.JUnit4
                import org.junit.Test

                @RunWith(JUnit4::class)
                class NotAScreenshotTestKotlin {

                    @Test
                    fun someOtherTest() {}
                }
                """
                    )
                    .within("src/androidTest")
            )

        check(*input).expectClean()
    }

    @Test
    fun `sdk suppress annotation missing in screenshot test class in kotlin sources under androidInstrumentedTest dir`() {
        val input =
            arrayOf(
                kotlin(
                        """
                package androidx.foo

                import org.junit.runner.RunWith
                import org.junit.runners.JUnit4
                import org.junit.Rule
                import androidx.test.screenshot.AndroidXScreenshotTestRule
                import org.junit.Test

                @RunWith(JUnit4::class)
                class TestInInstrumentedTestDirKotlin {
                    @get:Rule
                    val screenshotRule = AndroidXScreenshotTestRule("SCREENSHOT_GOLDEN_PATH")

                    @Test
                    fun aTest() {}
                }
                """
                    )
                    .within("src/androidInstrumentedTest")
            )

        val expected =
            """
       src/androidInstrumentedTest/androidx/foo/TestInInstrumentedTestDirKotlin.kt:11: Error: Screenshot test class TestInInstrumentedTestDirKotlin must be annotated with @SdkSuppress to run only on API 35. [ScreenshotTestSdkSuppress]
                class TestInInstrumentedTestDirKotlin {
                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 error
        """
        check(*input).expect(expected)
    }

    @Test
    fun `sdk suppress annotation missing in screenshot test class in kotlin sources under regular test dir`() {
        val input =
            arrayOf(
                kotlin(
                        """
                package androidx.foo

                import org.junit.runner.RunWith
                import org.junit.runners.JUnit4
                import org.junit.Rule
                import androidx.test.screenshot.AndroidXScreenshotTestRule
                import org.junit.Test

                @RunWith(JUnit4::class)
                class TestInInstrumentedTestDirKotlin {
                    @get:Rule
                    val screenshotRule = AndroidXScreenshotTestRule("SCREENSHOT_GOLDEN_PATH")

                    @Test
                    fun aTest() {}
                }
                """
                    )
                    .within("src/test")
            )

        val expected =
            """
       src/androidInstrumentedTest/androidx/foo/TestInInstrumentedTestDirKotlin.kt:11: Error: Screenshot test class TestInInstrumentedTestDirKotlin must be annotated with @SdkSuppress to run only on API 35. [ScreenshotTestSdkSuppress]
                class TestInInstrumentedTestDirKotlin {
                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 error
        """

        check(*input).expectClean()
    }

    @Test
    fun `sdk suppress annotation present in screenshot test class with code name qualifier in kotlin sources`() {
        val input =
            arrayOf(
                kotlin(
                        """
                package androidx.foo

                import org.junit.runner.RunWith
                import org.junit.runners.JUnit4
                import org.junit.Rule
                import androidx.test.filters.SdkSuppress
                import androidx.test.screenshot.AndroidXScreenshotTestRule

                @RunWith(JUnit4::class)
                @SdkSuppress(
                    minSdkVersion = 35,
                    maxSdkVersion = 35,
                )
                class Test {
                    @get:Rule val screenshotRule = AndroidXScreenshotTestRule("SCREENSHOT_GOLDEN_PATH")
                    @Test
                    fun aTest() {}
                }
            """
                    )
                    .within("src/androidTest")
            )
        check(*input).expectClean()
    }
}
