/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.test.screenshot

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.screenshot.matchers.BitmapMatcher
import androidx.test.screenshot.matchers.PixelPerfectMatcher
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * Verifies the extension points that subclasses of [ScreenshotTestRule] rely on.
 *
 * These tests drive [ScreenshotTestRule.apply] directly instead of using `@Rule`, so they run on
 * any device rather than only on API 35 emulators.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class ScreenshotTestRuleSubclassTest {

    private val description =
        Description.createTestDescription(ScreenshotTestRuleSubclassTest::class.java, "test")

    @Test
    fun apply_callsAssumeSupportedDeviceBeforeTest() {
        val events = mutableListOf<String>()
        val rule =
            object : ScreenshotTestRule() {
                override fun assumeSupportedDevice() {
                    events += "assume"
                }
            }

        rule.evaluate { events += "test" }

        assertThat(events).containsExactly("assume", "test").inOrder()
    }

    @Test
    fun assertAgainstGolden_dispatchesToOverride() {
        val calls = mutableListOf<String>()
        val rule =
            object : ScreenshotTestRule() {
                override fun assumeSupportedDevice() {}

                override fun assertBitmapAgainstGolden(
                    actual: Bitmap,
                    goldenIdentifier: String,
                    matcher: BitmapMatcher,
                ) {
                    calls += goldenIdentifier
                }
            }
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        rule.evaluate {
            bitmap.assertAgainstGolden(rule, "golden_id", matcher = PixelPerfectMatcher())
        }

        assertThat(calls).containsExactly("golden_id")
    }

    @Test
    fun resolveGoldenPathInRepo_prefixesPathToGoldensInRepo() {
        val rule = PathExposingRule(ScreenshotTestRuleConfig("repo", "/module/dir/"))
        rule.setCustomGoldenIdResolver { "${it}.png" }

        var path: String? = null
        rule.evaluate { path = rule.goldenPath("golden_id") }

        assertThat(path).isEqualTo("module/dir/golden_id.png")
    }

    @Test
    fun resolveGoldenPathInRepo_withoutPathToGoldensInRepo_returnsGoldenName() {
        val rule = PathExposingRule(ScreenshotTestRuleConfig())
        rule.setCustomGoldenIdResolver { "${it}.png" }

        var path: String? = null
        rule.evaluate { path = rule.goldenPath("golden_id") }

        assertThat(path).isEqualTo("golden_id.png")
    }

    private fun ScreenshotTestRule.evaluate(block: () -> Unit) {
        apply(
                object : Statement() {
                    override fun evaluate() = block()
                },
                description,
            )
            .evaluate()
    }

    private class PathExposingRule(config: ScreenshotTestRuleConfig) : ScreenshotTestRule(config) {
        override fun assumeSupportedDevice() {}

        fun goldenPath(goldenIdentifier: String): String = resolveGoldenPathInRepo(goldenIdentifier)
    }
}
