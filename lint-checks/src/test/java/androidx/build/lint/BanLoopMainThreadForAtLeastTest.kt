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

package androidx.build.lint

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanLoopMainThreadForAtLeastTest :
    AbstractLintDetectorTest(
        useDetector = BanLoopMainThreadForAtLeast(),
        useIssues = listOf(BanLoopMainThreadForAtLeast.ISSUE),
        stubs = arrayOf(Stubs.EspressoUiController),
    ) {

    @Test
    fun `Detection of UiController#loopMainThreadForAtLeast in Java sources`() {
        val input =
            arrayOf(
                java(
                        """
                package androidx.foo;

                import androidx.test.espresso.UiController;

                class Test {
                    public void badMethod(UiController uiController) {
                        uiController.loopMainThreadForAtLeast(50);
                    }
                }
            """
                    )
                    .within("src/androidTest")
            )

        val expected =
            """
src/androidTest/androidx/foo/Test.java:8: Error: Uses loopMainThreadForAtLeast() [BanLoopMainThreadForAtLeast]
                        uiController.loopMainThreadForAtLeast(50);
                                     ~~~~~~~~~~~~~~~~~~~~~~~~
1 error
        """
                .trimIndent()

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of UiController#loopMainThreadForAtLeast in Kotlin sources`() {
        val input =
            arrayOf(
                kotlin(
                        """
                package androidx.foo

                import androidx.test.espresso.UiController

                class Test {
                    fun badMethod(uiController: UiController) {
                        uiController.loopMainThreadForAtLeast(50)
                    }
                }
            """
                    )
                    .within("src/androidTest")
            )

        val expected =
            """
src/androidTest/androidx/foo/Test.kt:8: Error: Uses loopMainThreadForAtLeast() [BanLoopMainThreadForAtLeast]
                        uiController.loopMainThreadForAtLeast(50)
                                     ~~~~~~~~~~~~~~~~~~~~~~~~
1 error
        """
                .trimIndent()

        check(*input).expect(expected)
    }
}
