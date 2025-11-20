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

package androidx.appcompat.text

import android.content.Context
import android.graphics.Typeface
import android.text.TextPaint
import androidx.appcompat.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class TextAppearanceSpanCompatTest {
    private val PLACEHOLDER_TYPEFACE = Typeface.MONOSPACE
    private val COLOR_LIST_ID = -1

    // FontFetcher that emulates the requested font is already cached.
    private val CACHED_FONT_FETCHER =
        object : TextAppearanceSpanCompat.Companion.FontFetcher {
            override fun getCachedFont(context: Context, fontResId: Int): Typeface? {
                return PLACEHOLDER_TYPEFACE
            }

            override fun getFont(context: Context, fontResId: Int): Typeface? {
                return PLACEHOLDER_TYPEFACE
            }
        }

    // FontFetcher that emulates the requested font is not cached.
    private val NOT_CACHED_FONT_FETCHER =
        object : TextAppearanceSpanCompat.Companion.FontFetcher {
            override fun getCachedFont(context: Context, fontResId: Int): Typeface? {
                return null
            }

            override fun getFont(context: Context, fontResId: Int): Typeface? {
                return PLACEHOLDER_TYPEFACE
            }
        }

    // Mock executor that execute given runnable manually.
    private val mockExecutor =
        object : Executor {
            val commandList = mutableListOf<Runnable>()

            override fun execute(command: Runnable?) {
                if (command != null) {
                    commandList.add(command)
                }
            }

            fun runAllPendingCommand() {
                commandList.forEach { it.run() }
                commandList.clear()
            }

            val pendingCommandCount: Int
                get() = commandList.size
        }

    // Mock Runnable that records call count.
    private class MockRunnable : Runnable {
        var callCount = 0

        override fun run() {
            callCount++
        }
    }

    private fun TextAppearanceSpanCompat.extractUpdatedDrawState() =
        TextPaint().apply { updateDrawState(this) }

    private fun TextAppearanceSpanCompat.extractUpdatedMeasureState() =
        TextPaint().apply { updateMeasureState(this) }

    @Test
    fun create_Cached() {
        val span =
            TextAppearanceSpanCompat.createInternal(
                getInstrumentation().targetContext,
                R.style.TextAppearanceSpanCompat_fontFamilyOnly,
                COLOR_LIST_ID,
                mockExecutor,
                { fail("Should not be called") },
                CACHED_FONT_FETCHER,
            )

        assertThat(span).isNotNull()
        assertThat(mockExecutor.pendingCommandCount).isEqualTo(0)
        assertThat(span.appCompatTypeface.get()).isSameInstanceAs(PLACEHOLDER_TYPEFACE)
        assertThat(span.extractUpdatedDrawState().typeface).isSameInstanceAs(PLACEHOLDER_TYPEFACE)
        assertThat(span.extractUpdatedMeasureState().typeface)
            .isSameInstanceAs(PLACEHOLDER_TYPEFACE)
    }

    @Test
    fun create_NotCached() {
        val mockRunnable = MockRunnable()
        val span =
            TextAppearanceSpanCompat.createInternal(
                getInstrumentation().targetContext,
                R.style.TextAppearanceSpanCompat_fontFamilyOnly,
                COLOR_LIST_ID,
                mockExecutor,
                mockRunnable,
                NOT_CACHED_FONT_FETCHER,
            )

        assertThat(span).isNotNull()
        assertThat(mockExecutor.pendingCommandCount).isEqualTo(1)
        assertThat(span.appCompatTypeface.get()).isNull()
        assertThat(mockRunnable.callCount).isEqualTo(0)

        // execute the runnable to fetch the font
        mockExecutor.runAllPendingCommand()
        assertThat(mockRunnable.callCount).isEqualTo(1)
        assertThat(span.appCompatTypeface.get()).isNotNull()
        assertThat(span.extractUpdatedDrawState().typeface).isSameInstanceAs(PLACEHOLDER_TYPEFACE)
        assertThat(span.extractUpdatedMeasureState().typeface)
            .isSameInstanceAs(PLACEHOLDER_TYPEFACE)
    }

    @Test
    fun create_NotCached_withColor() {
        val mockRunnable = MockRunnable()
        val span =
            TextAppearanceSpanCompat.createInternal(
                getInstrumentation().targetContext,
                R.style.TextAppearanceSpanCompat_fontFamilyAndColorRed,
                COLOR_LIST_ID,
                mockExecutor,
                mockRunnable,
                NOT_CACHED_FONT_FETCHER,
            )

        assertThat(span).isNotNull()
        assertThat(mockExecutor.pendingCommandCount).isEqualTo(1)
        assertThat(span.appCompatTypeface.get()).isNull()
        assertThat(mockRunnable.callCount).isEqualTo(0)
        // Even with pending state, the text size should be applied.
        assertThat(span.extractUpdatedDrawState().textSize).isEqualTo(128f)
        assertThat(span.extractUpdatedMeasureState().textSize).isEqualTo(128f)

        // execute the runnable to fetch the font
        mockExecutor.runAllPendingCommand()
        assertThat(mockRunnable.callCount).isEqualTo(1)
        assertThat(span.appCompatTypeface.get()).isNotNull()
        assertThat(span.extractUpdatedDrawState().typeface).isSameInstanceAs(PLACEHOLDER_TYPEFACE)
        assertThat(span.extractUpdatedMeasureState().typeface)
            .isSameInstanceAs(PLACEHOLDER_TYPEFACE)
        assertThat(span.extractUpdatedDrawState().textSize).isEqualTo(128f)
        assertThat(span.extractUpdatedMeasureState().textSize).isEqualTo(128f)
    }

    @Test
    fun create_stringFontFamily() {
        val span =
            TextAppearanceSpanCompat.createInternal(
                getInstrumentation().targetContext,
                R.style.TextAppearanceSpanCompat_stringFontFamily,
                COLOR_LIST_ID,
                mockExecutor,
                { fail("Should not be called") },
                NOT_CACHED_FONT_FETCHER,
            )

        assertThat(span).isNotNull()
        assertThat(mockExecutor.pendingCommandCount).isEqualTo(0)
        assertThat(span.appCompatTypeface.get()).isNull()
        assertThat(span.extractUpdatedDrawState().typeface)
            .isSameInstanceAs(Typeface.create("sans-serif", Typeface.NORMAL))
        assertThat(span.extractUpdatedMeasureState().typeface)
            .isSameInstanceAs(Typeface.create("sans-serif", Typeface.NORMAL))
    }

    @Test
    fun create_typeface() {
        val span =
            TextAppearanceSpanCompat.createInternal(
                getInstrumentation().targetContext,
                R.style.TextAppearanceSpanCompat_typeface,
                COLOR_LIST_ID,
                mockExecutor,
                { fail("Should not be called") },
                NOT_CACHED_FONT_FETCHER,
            )

        assertThat(span).isNotNull()
        assertThat(mockExecutor.pendingCommandCount).isEqualTo(0)
        assertThat(span.appCompatTypeface.get()).isNull()
        assertThat(span.extractUpdatedDrawState().typeface)
            .isNotSameInstanceAs(PLACEHOLDER_TYPEFACE)
        assertThat(span.extractUpdatedMeasureState().typeface)
            .isNotSameInstanceAs(PLACEHOLDER_TYPEFACE)
    }
}
