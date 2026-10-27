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

package androidx.compose.remote.player.compose.embedded

import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerTextPropertiesTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun testCoreTextDataReflectiveReading() {
        val coreText =
            CoreText(
                null,
                1,
                -1,
                0f,
                0f,
                100f,
                50f,
                10,
                0xFF0000FF.toInt(),
                -1,
                18f,
                12f,
                36f,
                0,
                400f,
                -1,
                CoreText.TEXT_ALIGN_START,
                CoreText.OVERFLOW_START_ELLIPSIS,
                3,
                0.5f,
                2f,
                1.2f,
                CoreText.BREAK_STRATEGY_HIGH_QUALITY,
                1,
                CoreText.JUSTIFICATION_MODE_INTER_WORD,
                true,
                false,
                null,
                null,
                true,
                0,
                -1,
            )

        assertThat(coreText.autosize).isTrue()
        assertThat(coreText.minFontSize).isEqualTo(12f)
        assertThat(coreText.maxFontSize).isEqualTo(36f)
        assertThat(coreText.lineBreakStrategy).isEqualTo(CoreText.BREAK_STRATEGY_HIGH_QUALITY)
        assertThat(coreText.hyphenationFrequency).isEqualTo(1)
        assertThat(coreText.justificationMode).isEqualTo(CoreText.JUSTIFICATION_MODE_INTER_WORD)
        assertThat(coreText.overflow).isEqualTo(CoreText.OVERFLOW_START_ELLIPSIS)
        assertThat(coreText.letterSpacing).isEqualTo(0.5f)
        assertThat(coreText.lineHeightAdd).isEqualTo(2f)
        assertThat(coreText.lineHeightMultiplier).isEqualTo(1.2f)
        assertThat(coreText.underline).isTrue()
        assertThat(coreText.strikethrough).isFalse()
        assertThat(coreText.isDynamicColorEnabled).isFalse()
        assertThat(coreText.colorId).isEqualTo(-1)
    }

    @Test
    fun testCoreTextDataDynamicColorReflectiveReading() {
        val coreText =
            CoreText(
                null,
                1,
                -1,
                0f,
                0f,
                100f,
                50f,
                10,
                0xFF0000FF.toInt(),
                42, // colorId
                18f,
                -1f,
                -1f,
                0,
                400f,
                -1,
                CoreText.TEXT_ALIGN_START,
                CoreText.OVERFLOW_CLIP,
                1,
                0f,
                0f,
                1f,
                0,
                0,
                0,
                false,
                false,
                null,
                null,
                false,
                0,
                -1,
            )

        assertThat(coreText.isDynamicColorEnabled).isTrue()
        assertThat(coreText.colorId).isEqualTo(42)
    }

    @Test
    fun testTextLayoutDataDynamicColorReflectiveReading() {
        val textAlignWithDynamicFlag =
            (TextLayout.FLAG_IS_DYNAMIC_COLOR shl 16) or TextLayout.TEXT_ALIGN_START
        val textLayout =
            TextLayout(
                null,
                1,
                -1,
                0f,
                0f,
                100f,
                50f,
                10,
                42, // color is treated as colorId when dynamic color is enabled
                18f,
                0,
                400f,
                -1,
                textAlignWithDynamicFlag,
                TextLayout.OVERFLOW_CLIP,
                1,
            )

        assertThat(textLayout.isDynamicColorEnabled).isTrue()
        assertThat(textLayout.color).isEqualTo(42)
    }

    @Test
    fun testRemoteTextRendersThroughPlayer() {
        rule.setRemoteContent {
            RemoteText(
                text = "Hello Remote Properties".rs,
                style = RemoteTextStyle(fontSize = 18.rsp),
            )
        }

        rule.onNodeWithText("Hello Remote Properties").assertIsDisplayed()
    }

    @Test
    fun testTextAlignJustifyWithoutJustificationModeMapsToStart() {
        var currentDocument by
            mutableStateOf(
                coreTextDocument(
                    text = "Justify Mode Test",
                    textAlign = CoreText.TEXT_ALIGN_JUSTIFY,
                    justificationMode = CoreText.JUSTIFICATION_MODE_NONE,
                )
            )
        rule.setContent { Box(Modifier.size(200.dp)) { RcPlayer(document = currentDocument) } }
        rule.waitForIdle()

        assertThat(rule.textLayoutResultOf("Justify Mode Test").layoutInput.style.textAlign)
            .isEqualTo(TextAlign.Start)

        currentDocument =
            coreTextDocument(
                text = "Justify Mode Test",
                textAlign = CoreText.TEXT_ALIGN_JUSTIFY,
                justificationMode = CoreText.JUSTIFICATION_MODE_INTER_WORD,
            )
        rule.waitForIdle()

        assertThat(rule.textLayoutResultOf("Justify Mode Test").layoutInput.style.textAlign)
            .isEqualTo(TextAlign.Justify)
    }

    @Test
    fun testCustomTypefaceResolverPropagatesToGraphContextAndTextLayout() {
        var resolveCount = 0
        val customResolver = RcTypefaceResolver {
            resolveCount++
            Typeface.MONOSPACE
        }

        rule.setRemoteContent(typefaceResolver = customResolver) {
            RemoteText(
                text = "Custom Typeface Test".rs,
                style = RemoteTextStyle(fontSize = 18.rsp),
            )
        }

        rule.onNodeWithText("Custom Typeface Test").assertIsDisplayed()
        assertThat(resolveCount).isGreaterThan(0)
    }

    @Test
    fun testDynamicFontSizeReflectiveReadingAndEvaluation() {
        val dynamicFontSize = Utils.asNan(77)
        val coreText =
            CoreText(
                null,
                1,
                -1,
                0f,
                0f,
                100f,
                50f,
                10,
                0xFF0000FF.toInt(),
                -1,
                dynamicFontSize,
                -1f,
                -1f,
                0,
                400f,
                -1,
                CoreText.TEXT_ALIGN_START,
                CoreText.OVERFLOW_CLIP,
                1,
                0f,
                0f,
                1f,
                0,
                0,
                0,
                false,
                false,
                null,
                null,
                false,
                0,
                -1,
            )
        assertThat(coreText.fontSize.toRawBits()).isEqualTo(dynamicFontSize.toRawBits())

        val textLayout =
            TextLayout(
                null,
                1,
                -1,
                0f,
                0f,
                100f,
                50f,
                10,
                0xFF0000FF.toInt(),
                dynamicFontSize,
                0,
                400f,
                -1,
                TextLayout.TEXT_ALIGN_START,
                TextLayout.OVERFLOW_CLIP,
                1,
            )
        assertThat(textLayout.fontSize.toRawBits()).isEqualTo(dynamicFontSize.toRawBits())
    }

    @Test
    fun testResolveEffectiveMaxLinesMatchesAndroidPaintContext() {
        val cases =
            listOf(
                Triple(1, CoreText.OVERFLOW_CLIP, 1),
                Triple(1, CoreText.OVERFLOW_VISIBLE, 1),
                Triple(3, CoreText.OVERFLOW_CLIP, Int.MAX_VALUE),
                Triple(3, CoreText.OVERFLOW_VISIBLE, Int.MAX_VALUE),
                Triple(3, CoreText.OVERFLOW_ELLIPSIS, 3),
                Triple(3, CoreText.OVERFLOW_START_ELLIPSIS, 3),
                Triple(3, CoreText.OVERFLOW_MIDDLE_ELLIPSIS, 3),
            )

        var currentDocument by
            mutableStateOf(
                coreTextDocument(
                    text = "MaxLines Test",
                    maxLines = cases.first().first,
                    overflow = cases.first().second,
                )
            )
        rule.setContent { Box(Modifier.size(200.dp)) { RcPlayer(document = currentDocument) } }

        for ((maxLines, overflow, expected) in cases) {
            currentDocument =
                coreTextDocument(text = "MaxLines Test", maxLines = maxLines, overflow = overflow)
            rule.waitForIdle()
            assertThat(rule.textLayoutResultOf("MaxLines Test").layoutInput.maxLines)
                .isEqualTo(expected)
        }
    }

    private fun coreTextDocument(
        text: String,
        textAlign: Int = CoreText.TEXT_ALIGN_START,
        justificationMode: Int = CoreText.JUSTIFICATION_MODE_NONE,
        maxLines: Int = 1,
        overflow: Int = CoreText.OVERFLOW_CLIP,
    ): CoreDocument = buildDocument {
        val textId = addText(text)
        root {
            startTextComponent(
                RecordingModifier().width(200f).height(100f),
                textId,
                -1,
                0xFF000000.toInt(),
                -1,
                18f,
                -1f,
                -1f,
                0,
                400f,
                null,
                textAlign,
                overflow,
                maxLines,
                0f,
                0f,
                1f,
                0,
                0,
                justificationMode,
                false,
                false,
                null,
                null,
                false,
                0,
            )
            endTextComponent()
        }
    }
}
