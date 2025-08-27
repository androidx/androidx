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

package androidx.compose.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.LocalBackgroundTextMeasurementExecutor
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.contentcapture.ContentCaptureManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random

class TextListActivity : ComponentActivity() {

    private var executor: ExecutorService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wordCount = intent.getIntExtra(BenchmarkConfig.WordCount, 8)
        val wordLength = intent.getIntExtra(BenchmarkConfig.WordLength, 4)
        val textCount = intent.getIntExtra(BenchmarkConfig.TextCount, 3)
        val styled = intent.getBooleanExtra(BenchmarkConfig.Styled, false)
        val prefetch = intent.getBooleanExtra(BenchmarkConfig.Prefetch, false)
        val enableContentCapture =
            intent.getBooleanExtra(BenchmarkConfig.EnableContentCapture, false)
        val randomTextGenerator = RandomTextGenerator()

        val items =
            (0 until ItemCount * textCount).map {
                if (styled) {
                    randomTextGenerator.nextAnnotatedString(
                        length =
                            (wordCount + 1) * wordLength - 1, // count for whitespace between words
                        wordLength = wordLength,
                        styleCount = wordCount,
                    )
                } else {
                    randomTextGenerator.nextParagraph(
                        length =
                            (wordCount + 1) * wordLength - 1, // count for whitespace between words
                        wordLength = wordLength,
                    )
                }
            }

        executor =
            if (prefetch) {
                Executors.newSingleThreadExecutor { r -> Thread(r, "BgText") }
            } else {
                null
            }

        if (!enableContentCapture) {
            @OptIn(ExperimentalComposeUiApi::class)
            ContentCaptureManager.isEnabled = false
        }

        setContent {
            CompositionLocalProvider(LocalBackgroundTextMeasurementExecutor provides executor) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(ItemCount) { i ->
                        val startIndex = i * textCount
                        FlowRow {
                            for (j in startIndex until startIndex + textCount) {
                                (items[j] as? AnnotatedString)?.let {
                                    Text(it, modifier = Modifier.padding(8.dp))
                                }
                                (items[j] as? String)?.let {
                                    Text(it, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor?.shutdown()
        @OptIn(ExperimentalComposeUiApi::class)
        ContentCaptureManager.isEnabled = false
    }

    companion object {
        const val ItemCount = 1000

        object BenchmarkConfig {
            val WordCount = "word_count" // Integer
            val WordLength = "word_length" // Integer
            val TextCount = "text_count" // Integer
            val Styled = "styled" // Boolean
            val Prefetch = "prefetch"
            val EnableContentCapture = "enableContentCapture" // Boolean
        }
    }
}

// Copied from TextBenchmarkTestRule
class RandomTextGenerator(
    private val alphabet: Alphabet = Alphabet.Latin,
    private val random: Random = Random(0),
) {
    // a set of predefined TextStyle's to add to styled text
    private val nonMetricAffectingTextStyles =
        arrayOf(
            SpanStyle(color = Color.Blue),
            SpanStyle(background = Color.Cyan),
            SpanStyle(textDecoration = TextDecoration.Underline),
            SpanStyle(shadow = Shadow(Color.Black, Offset(3f, 3f), 2.0f)),
        )

    private val metricAffectingTextStyles =
        arrayOf(
            SpanStyle(fontSize = 18.sp),
            SpanStyle(fontSize = 2.em),
            SpanStyle(fontWeight = FontWeight.Bold),
            SpanStyle(fontStyle = FontStyle.Italic),
            SpanStyle(letterSpacing = 0.2.em),
            SpanStyle(baselineShift = BaselineShift.Subscript),
            SpanStyle(textGeometricTransform = TextGeometricTransform(0.5f, 0.5f)),
            SpanStyle(localeList = LocaleList("it")),
        )

    private fun getSpanStyleList(hasMetricAffectingStyle: Boolean) =
        nonMetricAffectingTextStyles +
            if (hasMetricAffectingStyle) {
                metricAffectingTextStyles
            } else {
                arrayOf()
            }

    /** Creates a sequence of characters group of length [length]. */
    private fun nextWord(length: Int): String =
        List(length) { alphabet.charRanges.random(random).random(random).toChar() }
            .joinToString(separator = "")

    /**
     * Create a sequence of character groups separated by the [Alphabet.space]. Each character group
     * consists of [wordLength] characters. The total length of the returned string is [length].
     */
    fun nextParagraph(length: Int, wordLength: Int = 9): String {
        return if (length == 0) {
            ""
        } else {
            StringBuilder()
                .apply {
                    while (this.length < length) {
                        append(nextWord(wordLength))
                        append(alphabet.space)
                    }
                }
                .substring(0, length)
        }
    }

    /**
     * Given a [text] mark each character group with a predefined TextStyle. The order of TextStyles
     * is predefined, and not randomized on purpose in order to get a consistent result in our
     * benchmarks.
     *
     * @param text The text on which the markup is applied.
     * @param styleCount The number of the text styles applied on the [text].
     * @param hasMetricAffectingStyle Whether to apply metric affecting [TextStyle]s text, which
     *   increases the difficulty to measure text.
     */
    fun createStyles(
        text: String,
        styleCount: Int = text.split(alphabet.space).size,
        hasMetricAffectingStyle: Boolean = true,
    ): List<AnnotatedString.Range<SpanStyle>> {
        val spanStyleList = getSpanStyleList(hasMetricAffectingStyle)

        val words = text.split(alphabet.space)

        var index = 0
        var styleIndex = 0

        val stylePerWord = styleCount / words.size
        val remains = styleCount % words.size

        return words.withIndex().flatMap { (wordIndex, word) ->
            val start = index
            val end = start + word.length
            index += word.length + 1

            val styleCountOnWord = stylePerWord + if (wordIndex < remains) 1 else 0
            List(styleCountOnWord) {
                AnnotatedString.Range(
                    start = start,
                    end = end,
                    item = spanStyleList[styleIndex++ % spanStyleList.size],
                )
            }
        }
    }

    /**
     * Create an [AnnotatedString] with randomly generated text but predefined TextStyles.
     *
     * @see nextParagraph
     * @see createStyles
     */
    fun nextAnnotatedString(
        length: Int,
        wordLength: Int = 9,
        styleCount: Int,
        hasMetricAffectingStyle: Boolean = true,
    ): AnnotatedString {
        val text = nextParagraph(length, wordLength)
        return AnnotatedString(
            text = text,
            spanStyles = createStyles(text, styleCount, hasMetricAffectingStyle),
        )
    }
}

/** Defines the character ranges to be picked randomly for a script. */
class Alphabet(val charRanges: List<IntRange>, val space: Char, val name: String) {

    override fun toString(): String {
        return name
    }

    companion object {
        val Latin =
            Alphabet(
                charRanges = listOf(IntRange('a'.code, 'z'.code), IntRange('A'.code, 'Z'.code)),
                space = ' ',
                name = "Latin",
            )
    }
}
