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

package androidx.compose.foundation.text

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class IntrinsicTextAsyncFontTest {

    @get:Rule val rule = createComposeRule()

    private class MyTypefaceLoader : AndroidFont.TypefaceLoader {
        val deferred = CompletableDeferred<Typeface>()

        override fun loadBlocking(context: Context, font: AndroidFont): Typeface? {
            error("Not blocking")
        }

        override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface? {
            return deferred.await()
        }
    }

    private class MyAsyncFont(val loader: MyTypefaceLoader) :
        AndroidFont(FontLoadingStrategy.Async, loader, FontVariation.Settings()) {
        override val weight: FontWeight = FontWeight.Normal
        override val style: FontStyle = FontStyle.Normal
    }

    @Test
    fun intrinsicRow_remeasures_whenAsyncFontLoads_string() {
        runIntrinsicRowRemeasuresTest(useAnnotatedString = false, minLines = 1)
    }

    @Test
    fun intrinsicRow_remeasures_whenAsyncFontLoads_annotatedString() {
        runIntrinsicRowRemeasuresTest(useAnnotatedString = true, minLines = 1)
    }

    @Test
    fun intrinsicRow_remeasures_whenAsyncFontLoads_withMinLines_string() {
        runIntrinsicRowRemeasuresTest(useAnnotatedString = false, minLines = 3)
    }

    @Test
    fun intrinsicRow_remeasures_whenAsyncFontLoads_withMinLines_annotatedString() {
        runIntrinsicRowRemeasuresTest(useAnnotatedString = true, minLines = 3)
    }

    private fun runIntrinsicRowRemeasuresTest(useAnnotatedString: Boolean, minLines: Int = 1) {
        val font = MyAsyncFont(MyTypefaceLoader())
        val fontFamily = font.toFontFamily()

        var rowSize = IntSize.Zero
        var textSize = IntSize.Zero

        val style = TextStyle(fontFamily = fontFamily, fontSize = 18.sp)

        rule.setContent {
            // Constrain width to force wrapping, which makes height sensitive to font changes
            Row(
                modifier =
                    Modifier.width(150.dp).height(IntrinsicSize.Min).onSizeChanged { rowSize = it }
            ) {
                val textModifier = Modifier.weight(1f).onSizeChanged { textSize = it }

                if (useAnnotatedString) {
                    BasicText(
                        text =
                            AnnotatedString("Text with async font that should wrap when it loads"),
                        style = style,
                        minLines = minLines,
                        modifier = textModifier,
                    )
                } else {
                    BasicText(
                        text = "Text with async font that should wrap when it loads",
                        style = style,
                        minLines = minLines,
                        modifier = textModifier,
                    )
                }
                // This box should fill the height determined by the text
                Box(Modifier.width(10.dp).fillMaxHeight())
            }
        }

        rule.waitForIdle()

        val initialRowSize = rowSize
        val initialTextSize = textSize
        assertNotEquals("Initial row size should not be Zero", IntSize.Zero, initialRowSize)
        assertNotEquals("Initial text size should not be Zero", IntSize.Zero, initialTextSize)

        // Now load the font. We use MONOSPACE BOLD which should be wider and cause more wrapping
        // (taller).
        font.loader.deferred.complete(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD))

        // Wait for recomposition/remeasure to automatically complete
        rule.waitForIdle()

        assertNotEquals("Text size should have changed", initialTextSize, textSize)
        assertNotEquals("Row size should have changed", initialRowSize, rowSize)
        assertEquals("Row height should match text height", textSize.height, rowSize.height)
    }
}
