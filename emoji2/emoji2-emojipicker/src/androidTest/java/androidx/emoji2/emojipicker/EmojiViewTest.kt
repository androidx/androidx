/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.emoji2.emojipicker

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.graphics.applyCanvas
import androidx.core.text.toSpanned
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class EmojiViewTestActivity : Activity()

@RunWith(AndroidJUnit4::class)
@SmallTest
class EmojiViewTest {
    companion object {
        private const val GRINNING_FACE = "\uD83D\uDE00"
    }

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule("emoji2/emoji2-emojipicker")

    @get:Rule val activityRule = ActivityScenarioRule(EmojiViewTestActivity::class.java)

    private lateinit var emojiView: EmojiView

    @Before
    fun setUp() {
        activityRule.scenario.onActivity {
            emojiView = EmojiView(it)
            it.setContentView(emojiView)
        }
    }

    private fun setAndWait(cs: CharSequence?) {
        emojiView.emoji = cs
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun dumpAndAssertAgainstGolden(golden: String) {
        Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
            .applyCanvas { emojiView.draw(this) }
            .assertAgainstGolden(screenshotRule, golden)
    }

    @Test
    fun testDrawEmoji() {
        setAndWait(GRINNING_FACE)
        dumpAndAssertAgainstGolden("draw_grinning_face")
    }

    @Test
    fun testDrawSpannedString() {
        setAndWait(
            SpannableString("0")
                .apply {
                    setSpan(ForegroundColorSpan(Color.RED), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                .toSpanned()
        )

        dumpAndAssertAgainstGolden("draw_red_zero")
    }

    @Test
    fun testMultipleDraw() {
        setAndWait(GRINNING_FACE)
        setAndWait("M")

        dumpAndAssertAgainstGolden("multiple_draw")
    }

    @Ignore
    @Test
    fun testClear() {
        setAndWait(GRINNING_FACE)
        setAndWait(null)

        dumpAndAssertAgainstGolden("draw_and_clear")
    }
}
