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

package androidx.emoji2.benchmark.text

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannedString
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiSpan
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class EmojiProcessBenchmark(private val size: Int, private val replaceAll: Boolean) {

    @get:Rule val benchmarkRule = BenchmarkRule()

    companion object {
        @Parameterized.Parameters(name = "size={0},replaceAll={1}")
        @JvmStatic
        fun parameters() =
            mutableListOf<Array<Any>>().apply {
                listOf(1, 10, 20, 30, 40).forEach { size ->
                    listOf(true, false).forEach { replaceAll -> add(arrayOf(size, replaceAll)) }
                }
            }
    }

    @Test
    fun emojiSpannableStringBuilder_emptyHasGlyphCache() {
        doEmojiBenchmark {
            val text = SpannableStringBuilder(emojisString(size))
            emptyGlyphCache(text)
            text
        }
    }

    @Test
    fun emojiSpannableStringBuilder() {
        doEmojiBenchmark { SpannableStringBuilder(emojisString(size)) }
    }

    @Test
    fun emojiSpannedString() {
        doEmojiBenchmark { SpannedString(emojisString(size)) }
    }

    @Test
    fun emojiSpannedString_withExistingEmojiSpans() {
        doEmojiBenchmark {
            val spannedString = SpannedString(emojisString(size))
            val alreadySpanned = EmojiCompat.get().process(spannedString)!!
            alreadySpanned
        }
    }

    @Test
    fun emojiString() {

        // string is immutable
        val string = emojisString(size)
        doEmojiBenchmark { string }
    }

    @Test
    fun latin() {
        val string = "E".repeat(size)

        // string is immutable
        doEmojiBenchmark { string }
    }

    private fun doEmojiBenchmark(stepFactory: () -> CharSequence) {
        initializeEmojiCompatWithBundledForTest(replaceAll)
        val ec = EmojiCompat.get()
        benchmarkRule.measureRepeated {
            val text = runWithMeasurementDisabled(stepFactory)
            ec.process(text)
        }
    }

    private fun emptyGlyphCache(text: CharSequence) {
        if (replaceAll) return
        // reset hasGlyph cache on all metadata returned via replaceAll
        val allEmojiMetadata =
            EmojiCompat.get().process(text, 0, text.length, size, EmojiCompat.REPLACE_STRATEGY_ALL)
                as Spanned
        allEmojiMetadata.getSpans(0, text.length, EmojiSpan::class.java).forEach {
            it.typefaceRasterizer.resetHasGlyphCache()
        }
    }
}
