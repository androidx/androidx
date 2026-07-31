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

package androidx.emoji2.benchmark.text

import android.text.SpannableString
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.emoji2.text.EmojiCompat
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("CheckResult")
@RunWith(JUnit4::class)
@LargeTest
class EmojiProcessPerformanceBenchmark {

    @get:Rule val benchmarkRule = BenchmarkRule()

    @Before
    fun setup() {
        initializeEmojiCompatWithBundledForTest(replaceAll = false)
    }

    // 1. Full-miss (Latin only)
    private val latinText =
        "Hello, world! This is a typical English sentence with no emojis whatsoever. It should be skipped entirely by the gating check."

    // 2. Mostly Latin, One Simple Emoji at the end
    private val latinWithOneSimpleEmoji =
        "Hello, world! This is a typical English sentence with one simple emoji at the end 😀"

    // 3. Mostly Latin, One Complex Emoji at the end
    private val latinWithOneComplexEmoji =
        "Hello, world! This is a typical English sentence with one complex family emoji at the end 👨‍👩‍👧‍👦"

    // 4. Simple Emojis Only
    private val simpleEmojis = "😀😃😄😁😆😅😂🤣🙃😉😊😇🙂🙃😉😊😇🙂🙃😉😊😇"

    // 5. Complex Emojis Only (ZWJ sequences)
    private val complexEmojis =
        "👨‍👩‍👧‍👦👩‍👩‍👦‍👦👨‍👨‍👧‍👧👩‍❤️‍👨🧑‍🤝‍🧑👨‍👩‍👧‍👦👩‍👩‍👦‍👦👨‍👨‍👧‍👧👩‍❤️‍👨🧑‍🤝‍🧑"

    // 6. Mixed Latin and Simple Emojis
    private val mixedLatinSimple =
        "Hello 😀 world 😃 this 😄 is 😁 mixed 😆 with 😂 simple 🤣 emojis 🙃 everywhere 😉"

    // 7. Mixed Latin and Complex Emojis
    private val mixedLatinComplex =
        "Hello 👨‍👩‍👧‍👦 world 👩‍👩‍👦‍👦 this 👨‍👨‍👧‍👧 is mixed 👩‍❤️‍👨 with complex 🧑‍🤝‍🧑 emojis"

    // 8. Keycaps and Flags (Special triggers)
    private val keycapsAndFlags = "1️⃣ 2️⃣ 3️⃣ 🇺🇸 🇨🇦 🇲🇽 4️⃣ 5️⃣ 6️⃣ 🇬🇧 🇫🇷 🇩🇪"

    // 9. Skin Tone Modifiers
    private val skinTones = "👍🏻 👍🏼 👍🏽 👍🏾 👍🏿 🙋‍♂️🏻 🙋‍♀️🏼 🙋‍♂️🏽 🙋‍♀️🏾 🙋‍♂️🏿"

    @Test
    fun benchmark_1_pureLatin() {
        val ec = EmojiCompat.get()
        benchmarkRule.measureRepeated { ec.process(latinText) }
    }

    @Test
    fun benchmark_2_latinWithOneSimpleEmoji() {
        val ec = EmojiCompat.get()
        benchmarkRule.measureRepeated { ec.process(latinWithOneSimpleEmoji) }
    }

    @Test
    fun benchmark_3_latinWithOneComplexEmoji() {
        val ec = EmojiCompat.get()
        benchmarkRule.measureRepeated { ec.process(latinWithOneComplexEmoji) }
    }

    @Test
    fun benchmark_4_simpleEmojisOnly() {
        val ec = EmojiCompat.get()
        benchmarkRule.measureRepeated { ec.process(simpleEmojis) }
    }

    @Test
    fun benchmark_5_complexEmojisOnly() {
        val ec = EmojiCompat.get()
        benchmarkRule.measureRepeated { ec.process(complexEmojis) }
    }

    @Test
    fun benchmark_6_mixedLatinSimple() {
        val ec = EmojiCompat.get()
        benchmarkRule.measureRepeated { ec.process(mixedLatinSimple) }
    }

    @Test
    fun benchmark_7_mixedLatinComplex() {
        val ec = EmojiCompat.get()
        benchmarkRule.measureRepeated { ec.process(mixedLatinComplex) }
    }

    @Test
    fun benchmark_8_keycapsAndFlags() {
        val ec = EmojiCompat.get()
        benchmarkRule.measureRepeated { ec.process(keycapsAndFlags) }
    }

    @Test
    fun benchmark_9_skinTones() {
        val ec = EmojiCompat.get()
        benchmarkRule.measureRepeated { ec.process(skinTones) }
    }

    // 10. Already Processed Spannable (No-op check)
    @Test
    fun benchmark_10_alreadyProcessed() {
        val ec = EmojiCompat.get()
        // Pre-process a Spannable to add EmojiSpans
        val processedSpannable = SpannableString(mixedLatinSimple)
        ec.process(processedSpannable)

        benchmarkRule.measureRepeated { ec.process(processedSpannable) }
    }
}
