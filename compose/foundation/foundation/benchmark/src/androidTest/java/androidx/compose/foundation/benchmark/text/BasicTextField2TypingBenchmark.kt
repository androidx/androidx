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

package androidx.compose.foundation.benchmark.text

import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.testutils.ComposeBenchmarkScope
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.benchmark.TextBenchmarkTestRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Measures one keystroke on a focused state-based [BasicTextField] through the real
 * InputConnection, followed by one full frame (recompose, measure, layout, draw). Each iteration
 * types one character and then deletes it, so the text length stays constant across iterations;
 * reported numbers therefore cover TWO edits and TWO frames.
 */
@LargeTest
@RunWith(Parameterized::class)
class BasicTextField2TypingBenchmark(private val textLength: Int, private val singleLine: Boolean) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0,number,#},singleLine={1}")
        fun parameters(): List<Array<Any>> =
            listOf(
                arrayOf(10, false),
                arrayOf(1000, false),
                arrayOf(1000, true),
            )
    }

    private val textBenchmarkRule = TextBenchmarkTestRule()
    private val benchmarkRule = ComposeBenchmarkRule()

    @get:Rule val testRule = RuleChain.outerRule(textBenchmarkRule).around(benchmarkRule)

    private val width = textBenchmarkRule.widthDp.dp
    private val fontSize = textBenchmarkRule.fontSizeSp.sp

    private val caseFactory = {
        textBenchmarkRule.generator { generator ->
            BasicTextField2TypingTestCase(
                initialText = generator.nextParagraph(length = textLength),
                width = width,
                fontSize = fontSize,
                singleLine = singleLine,
            )
        }
    }

    /** Focuses the field and checks that its input session has started. */
    private fun ComposeBenchmarkScope<BasicTextField2TypingTestCase>.focusAndAwaitInputConnection():
        Unit {
        benchmarkRule.runOnUiThread {
            doFramesUntilNoChangesPending()
            // LocalWindowInfo reports the window as focused; requestFocus() focuses the field
            // itself so that BasicTextField starts its input session.
            getTestCase().requestFocus()
            doFramesUntilNoChangesPending()
            checkNotNull(getTestCase().inputConnection) { "input session did not start" }
        }
    }

    /** commitText (as for a hardware-keyboard-like or non-composing IME) + backspace. */
    @Test
    fun commitText_thenDelete() {
        benchmarkRule.runBenchmarkFor(caseFactory) {
            focusAndAwaitInputConnection()
            benchmarkRule.measureRepeatedOnUiThread {
                val testCase = getTestCase()
                val ic = testCase.inputConnection!!
                ic.commitText("a", 1)
                doFrame()
                runWithMeasurementDisabled {
                    check(testCase.state.text.length == textLength + 1) {
                        "expected length ${textLength + 1}, got ${testCase.state.text.length}"
                    }
                }
                ic.deleteSurroundingText(1, 0)
                doFrame()
                runWithMeasurementDisabled {
                    check(testCase.state.text.length == textLength) {
                        "expected length $textLength, got ${testCase.state.text.length}"
                    }
                }
            }
        }
    }

    /** setComposingText the way Gboard types inside a word: the composing region grows/shrinks. */
    @Test
    fun setComposingText_growShrink() {
        benchmarkRule.runBenchmarkFor(caseFactory) {
            focusAndAwaitInputConnection()
            benchmarkRule.runOnUiThread {
                val ic = getTestCase().inputConnection!!
                ic.commitText(" ", 1)
                ic.setComposingText("ab", 1)
                doFramesUntilNoChangesPending()
            }
            benchmarkRule.measureRepeatedOnUiThread {
                val testCase = getTestCase()
                val ic = testCase.inputConnection!!
                ic.setComposingText("abc", 1)
                doFrame()
                runWithMeasurementDisabled {
                    check(testCase.state.text.length == textLength + 4) {
                        "expected length ${textLength + 4}, got ${testCase.state.text.length}"
                    }
                }
                ic.setComposingText("ab", 1)
                doFrame()
                runWithMeasurementDisabled {
                    check(testCase.state.text.length == textLength + 3) {
                        "expected length ${textLength + 3}, got ${testCase.state.text.length}"
                    }
                }
            }
        }
    }

    /** Selection-only change (tap to move the cursor by one character), no text change. */
    @Test
    fun setSelection_moveCursor() {
        benchmarkRule.runBenchmarkFor(caseFactory) {
            focusAndAwaitInputConnection()
            benchmarkRule.measureRepeatedOnUiThread {
                val testCase = getTestCase()
                val ic = testCase.inputConnection!!
                ic.setSelection(textLength - 1, textLength - 1)
                doFrame()
                runWithMeasurementDisabled {
                    check(testCase.state.selection == TextRange(textLength - 1)) {
                        "expected selection ${TextRange(textLength - 1)}, got ${testCase.state.selection}"
                    }
                }
                ic.setSelection(textLength, textLength)
                doFrame()
                runWithMeasurementDisabled {
                    check(testCase.state.selection == TextRange(textLength)) {
                        "expected selection ${TextRange(textLength)}, got ${testCase.state.selection}"
                    }
                }
            }
        }
    }

    /**
     * An IME call that changes nothing (setSelection to the current selection) while a composition
     * with IME-provided styling spans is active.
     */
    @Test
    fun noOpSetSelection_duringStyledComposition() {
        benchmarkRule.runBenchmarkFor(caseFactory) {
            focusAndAwaitInputConnection()
            benchmarkRule.runOnUiThread {
                val ic = getTestCase().inputConnection!!
                val composing = SpannableString("ab")
                composing.setSpan(UnderlineSpan(), 0, 2, Spanned.SPAN_COMPOSING)
                ic.commitText(" ", 1)
                ic.setComposingText(composing, 1)
                doFramesUntilNoChangesPending()
            }
            val cursor = textLength + 3
            benchmarkRule.measureRepeatedOnUiThread {
                val testCase = getTestCase()
                val ic = testCase.inputConnection!!
                ic.setSelection(cursor, cursor)
                doFrame()
                runWithMeasurementDisabled {
                    check(testCase.state.selection == TextRange(cursor)) {
                        "expected selection ${TextRange(cursor)}, got ${testCase.state.selection}"
                    }
                }
            }
        }
    }
}
