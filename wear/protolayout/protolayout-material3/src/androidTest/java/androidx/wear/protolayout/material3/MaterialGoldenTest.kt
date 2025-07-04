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

package androidx.wear.protolayout.material3

import androidx.annotation.Dimension
import androidx.test.filters.LargeTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class MaterialGoldenTest(private val expected: String, private val testCase: RunnerUtils.TestCase) {
    @JvmField
    @Rule
    var mScreenshotRule: AndroidXScreenshotTestRule =
        AndroidXScreenshotTestRule("wear/protolayout/protolayout-material3")

    @Test
    fun testLtr() {
        // Skip test if it's not meant for LTR
        if (!testCase.isForLtr) return
        RunnerUtils.runSingleScreenshotTest(
            rule = mScreenshotRule,
            layout = testCase.layout,
            expected = expected,
            isRtlDirection = false,
        )
    }

    @Test
    fun testRtl() {
        // Skip test if it's not meant for RTL
        if (!testCase.isForRtl) return
        RunnerUtils.runSingleScreenshotTest(
            rule = mScreenshotRule,
            layout = testCase.layout,
            expected = expected + "_rtl",
            isRtlDirection = true,
        )
    }

    companion object {
        @Dimension(unit = Dimension.DP)
        fun pxToDp(px: Int, scale: Float): Int {
            return ((px - 0.5f) / scale).toInt()
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val testCaseList: MutableList<Array<Any>> = ArrayList()
            testCaseList.addAll(
                RunnerUtils.convertToTestParameters(
                    TestCasesGenerator.generateTestCases(),
                    isForRtr = true,
                    isForLtr = true,
                )
            )

            RunnerUtils.waitForNotificationToDisappears()

            return testCaseList
        }
    }
}
